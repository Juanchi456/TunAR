package com.example.tunar.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.tunar.domain.model.AudioCaptureException
import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.PitchSample
import com.example.tunar.domain.model.SampleRateMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Captura del micrófono con [AudioRecord] (PCM 16 bits, mono).
 *
 * Privacidad (RNF03): las muestras viven solo en buffers en memoria que se reutilizan;
 * nada se escribe a disco ni se envía por red. El micrófono se libera al cancelar el flujo.
 */
class MicrophoneDataSource(private val context: Context) {

    fun pitchSamples(config: DetectionConfig): Flow<PitchSample> = flow {
        val record = openRecord(config.sampleRate, config.hopSize)
        val analyzer = PitchAnalyzer(config)
        val shorts = ShortArray(config.hopSize)
        val block = FloatArray(config.hopSize)
        try {
            startOrThrow(record)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = record.read(shorts, 0, shorts.size, AudioRecord.READ_BLOCKING)
                if (read < 0) throw AudioCaptureException.ReadFailed(read)
                val capturedAt = SystemClock.elapsedRealtimeNanos()
                for (i in 0 until read) block[i] = shorts[i] / 32768f
                analyzer.push(block, read, capturedAt)?.let { emit(it) }
            }
        } finally {
            record.safeStopAndRelease()
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Mide la tasa de muestreo efectiva comparando cuadros capturados contra el reloj del sistema.
     * Usa [AudioRecord.getTimestamp] (marca de tiempo del hardware) si el dispositivo la ofrece;
     * si no, cuenta las muestras leídas entre dos instantes.
     */
    suspend fun measureSampleRate(nominalRate: Int, durationMillis: Long): SampleRateMeasurement =
        withContext(Dispatchers.IO) {
            val chunk = 1024
            val record = openRecord(nominalRate, chunk)
            val buffer = ShortArray(chunk)
            try {
                startOrThrow(record)
                // Descartar el arranque: los primeros bloques suelen llegar en ráfaga.
                readFor(record, buffer, WARM_UP_MILLIS)

                val ts1 = AudioTimestamp()
                val hasTimestamps =
                    record.getTimestamp(ts1, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS
                val startNanos = System.nanoTime()
                val framesRead = readFor(record, buffer, durationMillis)
                val endNanos = System.nanoTime()

                val ts2 = AudioTimestamp()
                if (hasTimestamps &&
                    record.getTimestamp(ts2, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS &&
                    ts2.nanoTime > ts1.nanoTime
                ) {
                    val rate = (ts2.framePosition - ts1.framePosition) * 1e9 / (ts2.nanoTime - ts1.nanoTime)
                    SampleRateMeasurement(nominalRate, rate, durationMillis, "AudioTimestamp")
                } else {
                    val rate = framesRead * 1e9 / (endNanos - startNanos)
                    SampleRateMeasurement(nominalRate, rate, durationMillis, "Conteo de muestras")
                }
            } finally {
                record.safeStopAndRelease()
            }
        }

    private suspend fun readFor(record: AudioRecord, buffer: ShortArray, millis: Long): Long {
        val end = System.nanoTime() + millis * 1_000_000
        var frames = 0L
        while (System.nanoTime() < end) {
            currentCoroutineContext().ensureActive()
            val read = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
            if (read < 0) throw AudioCaptureException.ReadFailed(read)
            frames += read
        }
        return frames
    }

    @SuppressLint("MissingPermission") // Se verifica explícitamente antes de crear el AudioRecord.
    private fun openRecord(sampleRate: Int, blockFrames: Int): AudioRecord {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) throw AudioCaptureException.PermissionMissing()

        val minBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING)
        if (minBytes <= 0) {
            throw AudioCaptureException.Unavailable("el dispositivo no admite $sampleRate Hz")
        }
        // Margen de varios bloques para que el sistema no descarte audio si un análisis se demora.
        val bufferBytes = maxOf(minBytes, blockFrames * BYTES_PER_FRAME * 4)
        val record = try {
            AudioRecord(audioSource(), sampleRate, CHANNEL, ENCODING, bufferBytes)
        } catch (e: SecurityException) {
            throw AudioCaptureException.PermissionMissing()
        } catch (e: IllegalArgumentException) {
            throw AudioCaptureException.Unavailable(e.message ?: "parámetros inválidos")
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw AudioCaptureException.Unavailable("no se pudo inicializar")
        }
        return record
    }

    private fun startOrThrow(record: AudioRecord) {
        record.startRecording()
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            throw AudioCaptureException.Unavailable("otra app está usando el micrófono")
        }
    }

    /**
     * UNPROCESSED evita el control automático de ganancia y la supresión de ruido, que alteran
     * la señal; si el dispositivo no lo soporta se usa VOICE_RECOGNITION, que también minimiza
     * el procesamiento.
     */
    private fun audioSource(): Int {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val unprocessed = audioManager
            ?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        return if (unprocessed) {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }.also { Log.d(TAG, "Fuente de audio: ${if (unprocessed) "UNPROCESSED" else "VOICE_RECOGNITION"}") }
    }

    private fun AudioRecord.safeStopAndRelease() {
        runCatching { if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop() }
        release()
    }

    private companion object {
        const val TAG = "TunAR-Audio"
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_FRAME = 2
        const val WARM_UP_MILLIS = 500L
    }
}

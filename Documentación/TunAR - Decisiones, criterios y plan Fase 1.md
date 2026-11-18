# TunAR – Decisiones, criterios de calidad y plan de la Fase 1

Última actualización: 2026-11-18

## 1. Resumen de la devolución del docente y su estado

| Observación | Estado / respuesta |
|---|---|
| Validar la detección de frecuencias en dispositivos reales, con distintas notas y ruido | Se define banco de calibración y pruebas en teléfono físico (secciones 3 y 5) |
| Precisar temperamentos alternativos y ajustes finos | Diferidos a la Fase 2 planificada; se documentan en el alcance (sección 2) |
| Completar el diseño: flujo principal de afinación y estados de error | Se rediseña en la Fase 1, paso 4 (sección 5) |
| Definir pruebas medibles (precisión, tiempo de respuesta, persistencia) | Criterios definidos en la sección 3 |
| Prioridad: afinador básico y calibración | Es el eje de la Fase 1 (sección 5) |

Nota: la última observación de la devolución parece quedar cortada («Sin una detección de frecuencias confiable. El tema es la calibración.»). Se interpreta como que la calibración es el eje de la próxima etapa.

## 2. Decisiones de alcance

**Fase 1 (esta entrega):** afinador funcional y calibrado, interfaz completa (Modo Básico y Modo Pro) y presets fijos o en memoria (Standard, Drop D, Open D). Se prueba la UI en el emulador y el afinador en un teléfono físico.

**Fase 1b:** persistencia local con Room (RF04) y código de perfil para compartir (nuevo RF05: exportar un perfil a un código y crear uno nuevo desde él).

**Fase 2 (planificada, no descartada):** temperamentos alternativos (justo, pitagórico) y ajuste fino de céntimos por nota.

**Fuera de alcance:** nube, registro o login, audio polifónico, metrónomo, diapasón y diccionario de acordes.

Otras decisiones:
- Arquitectura: Clean Architecture + MVVM. Interfaz: Material Design 3 (Compose Material3).
- Proyecto individual: la sección 17 se reemplaza por una nota que lo indique.
- La frecuencia de referencia (440, 432 u otra) es un parámetro del dominio, independiente del algoritmo de detección.

### Previsiones para la Fase 2 sin implementarla
- El perfil incluye `centsOffset` por cuerda (0 por defecto) y un tipo de sistema de afinación con un único valor (temperamento igual).
- El cálculo de céntimos recibe referencia y sistema de afinación como parámetros.
- Room con versión de esquema desde el inicio. El código de perfil lleva número de versión.

## 3. Criterios de calidad medibles
Los umbrales son propuestas, a ratificar tras la primera ronda de mediciones.

| Aspecto | Definición |
|---|---|
| Precisión | Tono puro generado: error ≤ ±2 céntimos en el 95 % de las lecturas estables. Instrumento real en silencio: ≤ ±5. Ruido moderado: ≤ ±8. Desviación estándar de un tono sostenido (1 s) ≤ 1,5 céntimos. Errores de octava ≤ 2 % |
| Rango | Obligatorio B0 (30,87 Hz) a E4 (329,63 Hz); deseable hasta E5. Se verifica con cuerdas al aire de guitarra estándar, bajo de 5 cuerdas y guitarra de 8 cuerdas (F#1) |
| Condiciones | Silencio (< 35 dB), ruido moderado (~55 dB) y ruido alto (~70 dB, informativo: la app debe mostrar «señal débil» y no un valor erróneo). Mínimo 1 teléfono físico, idealmente 2 de gamas distintas. El emulador no se usa para medir audio |
| Latencia | ≤ 300 ms extremo a extremo en el percentil 95, con al menos 30 mediciones por dispositivo. Registro de tiempos en la app, verificado con grabación de pantalla de un tono corto |
| Persistencia (Fase 1b) | Perfiles idénticos tras cierre forzado, reinicio y modo avión. Prueba con 20 perfiles. Guardado o lectura en menos de 100 ms |
| Código de perfil (Fase 1b) | Exportar e importar devuelve un perfil idéntico |

## 4. Consideraciones técnicas de la detección
- TarsosDSP ofrece YIN, FastYin y MPM. Se comparan los tres en el banco de calibración antes de elegir.
- YIN mide períodos hasta la mitad del buffer: para B0 a 44,1 kHz hace falta un buffer de al menos 4096 muestras (~93 ms). Cálculo propio, a confirmar.
- La tasa de muestreo real puede diferir de la nominal, lo que genera un error constante en céntimos. La calibración mide y corrige ese factor.
- Para Android, TarsosDSP se incorpora como JAR en `/app/libs/`; resolver en el paso 1.

## 5. Plan de la Fase 1
1. **Base del proyecto:** Android Studio, Compose con Material 3, Navigation, capas `presentation / domain / data` e integración de TarsosDSP.
2. **Dominio con tests unitarios:** nota, octava y céntimos con referencia configurable; modelos `TuningProfile`, `StringConfig` y `PitchResult`.
3. **Afinador y calibración:** permiso de micrófono, `AudioRecord` y detección en memoria sin guardar audio. Medir la tasa de muestreo real, barrer buffer, umbral y algoritmo, y elegir la configuración que cumpla los criterios.
4. **Interfaz en el emulador:** pantalla de afinación con estados (sin permiso, sin señal, afinado, desafinado, fuera de rango), selector de presets fijos, barra inferior Básico/Pro y edición de cuerdas en el Modo Pro (cambio de nota y octava, agregar o quitar cuerdas de 4 a 8).
5. **Cierre:** informe de pruebas con las mediciones; actualizar el documento (alcance por fases, RF05, nota de proyecto individual, diagrama de arquitectura visual, título de sección 15 duplicado) y el README.

## 6. Pendiente
- Confirmar o ajustar los umbrales de la sección 3 tras la primera medición.
- Rediseñar en Figma el flujo principal de afinación y sus estados de error (el diseño actual no es definitivo).

# 🎸 TunAR 

![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Room](https://img.shields.io/badge/Room_DB-00599C?style=for-the-badge&logo=sqlite&logoColor=white)

> **TunAR** es una aplicación móvil de afinación de instrumentos de cuerda diseñada para democratizar el acceso a herramientas de luthería de alta precisión. A diferencia de las alternativas comerciales que bloquean la personalización detrás de suscripciones, TunAR permite afinar en frecuencias no estándar, crear perfiles personalizados y explorar temperamentos alternativos de manera 100% gratuita y *Offline First*.

---

## ✨ Funcionalidades Principales

*   🎙️ **Análisis DSP en tiempo real:** Captura de audio de alta precisión mediante el micrófono del dispositivo, calculando la frecuencia fundamental (Hz) y la desviación en céntimos sin latencia perceptible.
*   🎛️ **Modos Escalares (Básico y Pro):** Interfaz simplificada para la afinación estándar (Ej: Guitarra E Standard 440Hz) y un modo avanzado para luthiers y músicos experimentados.
*   ⚙️ **Perfiles Altamente Personalizables:** Configuración libre de la cantidad de cuerdas (4 a 8), la nota, la octava y la frecuencia de referencia (ej. 432Hz) para cada instrumento.
*   💾 **Estrategia Offline First:** Todo el procesamiento matemático y la persistencia de los presets del usuario se realizan de manera 100% local. No requiere conexión a internet para funcionar.

---

## 📱 Capturas de Pantalla

En desarrollo...
| Modo Básico | Modo Pro (Edición) | Lista de Presets |
| :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Pantalla+Basico" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Pantalla+Pro" width="200"/> | <img src="https://via.placeholder.com/250x500.png?text=Pantalla+Presets" width="200"/> |

---

## 🏗️ Arquitectura y Tecnologías

El proyecto fue desarrollado aplicando estrictamente el patrón **MVVM** y principios de **Clean Architecture**, asegurando una alta separación de responsabilidades y un flujo de datos unidireccional (UDF).

### Stack Tecnológico:
*   **UI:** Jetpack Compose (Interfaces declarativas reactivas).
*   **Navegación:** Navigation Compose.
*   **Reactividad:** Kotlin Coroutines y StateFlow.
*   **Persistencia:** Room Database (ORM local para *presets*).
*   **Procesamiento Digital de Señales (DSP):** TarsosDSP (Implementación del algoritmo YIN para evitar el error de octava).
*   **Hardware:** Android AudioRecord API (Captura de buffer PCM continuo).

---

## 🚀 Instrucciones de Ejecución

Para ejecutar este proyecto en tu entorno local, sigue estos pasos:

1.  **Clonar el repositorio:**
    ```bash
    git clone [https://github.com/Juanchi456/TunAR.git](https://github.com/Juanchi456/TunAR.git)
    ```
2.  **Abrir en Android Studio:**
    Abre *Android Studio*, selecciona `File > Open...` y navega hasta la carpeta del proyecto clonado.
3.  **Sincronizar dependencias:**
    Espera a que Gradle sincronice todas las dependencias (necesitarás conexión a internet para este paso).
4.  **Ejecutar la app:**
    Conecta un dispositivo físico Android (recomendado para probar el micrófono) o inicia un emulador. Presiona `Run (Shift + F10)`. 
    *Asegúrate de otorgar los permisos de micrófono solicitados al abrir la aplicación por primera vez.*

---

## ⚠️ Limitaciones Conocidas
*   La aplicación está diseñada para detección **monofónica** (solo se debe tocar una cuerda a la vez). No procesa acordes ni rasgueos polifónicos.
*   El cálculo automático para escalas completas en temperamentos históricos no está integrado; cada cuerda debe afinarse por nota individual.

---

## 🎓 Contexto Académico

Este proyecto constituye el **Trabajo Práctico Obligatorio (TPO)** para la asignatura:

*   **Materia:** Desarrollo de Aplicaciones I (2° Cuatrimestre - 2026).
*   **Institución:** Universidad Argentina de la Empresa (UADE) - Facultad de Ingeniería y Ciencias Exactas.
*   **Alumno:** Juan Cruz Ordoñez (LU: 1140882).
*   **Docentes:** Alejandro Francisco Peña - Adrián Alberto Narducci.

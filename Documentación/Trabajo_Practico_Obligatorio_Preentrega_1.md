# UNIVERSIDAD ARGENTINA DE LA EMPRESA

## FACULTAD DE INGENIERÍA Y CIENCIAS EXACTAS

**DESARROLLO DE APLICACIONES I**

- 2° CUATRIMESTRE – 2026

**TRABAJO PRÁCTICO OBLIGATORIO**

**Diseño y Desarrollo de una Aplicación Móvil - Afinador de instrumentos de cuerda personalizable.**

**TunAR**

- **Alumno:** Juan Cruz Ordoñez – LU 1140882

- **Docentes:** Alejandro Francisco Peña - Adrián Alberto Narducci

## Tabla de Contenidos

1. Nombre provisorio de la aplicación
2. Descripción del problema
3. Usuario o usuarios principales
4. Contexto de uso
5. Propuesta de solución
6. Justificación de por qué la solución debe ser móvil
7. Caso de negocio o propuesta de valor
8. Alcance y fuera de alcance
9. Requisitos funcionales
10. Requisitos no funcionales
11. Diseño en Figma
12. Flujo de pantallas
13. Diagrama de arquitectura
14. Estrategia Offline First, cuando corresponda
15. Tecnologías previstas y justificación
16. Repositorio
17. Integrantes, roles y responsabilidades

# 1. Nombre provisorio de la aplicación

La aplicación se llamará TunAR, haciendo un juego de palabras entre la palabra en inglés “Tuner” y el “AR” de Argentina.

# 2. Definición del problema

Los músicos, particularmente los que se dedican a instrumentos de cuerda como la guitarra y el bajo, entre otros, constantemente tienen la necesidad de afinar sus instrumentos. Actualmente, los músicos que buscan afinar con frecuencias específicas (como 432Hz) o sistemas alternativos deben recurrir a afinadores físicos profesionales (como afinadores estroboscópicos de pedal) que son costosos y poco portátiles. La alternativa son aplicaciones móviles, pero estas presentan grandes dificultades: suelen estar diseñadas solo con la afinación estándar y bloquean la personalización de frecuencias y temperamentos detrás de costosas suscripciones de pago ( _paywalls_ ).

# 3. Usuario o usuarios principales

La aplicación está orientada a los siguientes perfiles:

- **Perfil:** Músicos profesionales, estudiantes de música y luthiers que buscan explorar sistemas de afinación no convencionales.

- **Necesidad principal:** Afinar instrumentos con alta precisión usando frecuencias no estándar y poder guardar esas configuraciones.

- **Frecuencia estimada de uso:** Alta (diaria o varias veces por semana durante prácticas, ensayos o grabaciones).

- **Posibles restricciones:** El usuario necesita utilizar la aplicación con una sola mano (la otra sostiene el instrumento) y debe poder interpretar la interfaz desde la distancia (dispositivo apoyado).

# 4. Contexto de uso

Los usuarios utilizarán la aplicación principalmente durante ensayos musicales, sesiones de grabación en estudio, prácticas individuales en el hogar o al aire libre, así como en reuniones y eventos. Se prevé que la aplicación sea utilizada habitualmente por períodos breves justo antes o durante la ejecución musical. El usuario típicamente tendrá el dispositivo apoyado sobre una superficie, atril o amplificador, requiriendo una interfaz visual clara que pueda leerse a cierta distancia mientras sostiene el instrumento con ambas manos. Es probable que la aplicación deba funcionar en entornos con conectividad limitada o nula, por lo que su funcionalidad de captación y afinación no debe depender del acceso a internet.

# 5. Propuesta de solución

La solución que se propone es una aplicación que permita una alta personalización sobre el esquema de afinación que utilizará el instrumento. Contará con dos modos, un modo “básico” el cual contará con las features más comunes y estándar de las aplicaciones de afinación, así como un modo “pro”, el cuál permitirá, principalmente:

- Agregar o remover cuerdas.

- Configurar qué nota se busca en cada cuerda.

- Afinar finamente los céntimos de cada nota.

- Cambiar a sistemas de afinación alternativos (temperamento justo, afinación pitagórica).

Además, la aplicación permitirá guardar estas configuraciones, y compartirlas entre los usuarios.

**Escenario principal de uso:** Un músico en una sala de ensayo necesita cambiar la afinación de su guitarra a un temperamento histórico para una canción específica, carga su preset guardado en la app y afina en minutos sin depender de internet.

**Beneficio esperado:** Democratizar el acceso a herramientas de luthería y optimizar drásticamente el tiempo de preparación del instrumento, reduciendo la carga cognitiva al momento de afinar.

# 6. ¿Por qué la solución debe ser móvil?

La naturaleza de la afinación de instrumentos musicales requiere una herramienta que sea altamente portable y de acceso inmediato en cualquier entorno. Los músicos no siempre disponen de una computadora de escritorio o portátil en salas de ensayo o escenarios, pero prácticamente siempre llevan consigo un teléfono inteligente. Una aplicación móvil aprovecha de forma nativa el micrófono integrado del dispositivo para captar las frecuencias de manera eficiente sin requerir hardware adicional. Además, el formato móvil permite al usuario llevar sus configuraciones y sistemas de afinación personalizados a cualquier lugar, solucionando el problema central de la indisponibilidad o extravío de herramientas físicas tradicionales y brindando una solución ubicua en el momento exacto de la necesidad.

# 7. Caso de negocio o propuesta de valor

El objetivo principal de la aplicación es proporcionar a la comunidad de músicos una alternativa libre, con un alto grado de personalización y sin funcionalidades bloqueadas detrás de paywalls.

A esto se le suma un fuerte valor operativo: al permitir guardar y cargar configuraciones ( _presets_ ), optimiza los tiempos de preparación en ensayos y estudios de grabación. Además, aporta un valor educativo y comunitario, ya que la posibilidad de explorar temperamentos históricos (como el pitagórico o justo) y de compartir esquemas de afinación fomenta el intercambio de conocimientos entre profesores, alumnos, luthiers y músicos profesionales.

En resumen, la propuesta se estructura de la siguiente manera:

- **Problema:** Las herramientas de afinación actuales son limitadas, estándar y restringen opciones avanzadas o no convencionales detrás de muros de pago.

- **Usuario:** Músicos, estudiantes de música y luthiers que buscan explorar sistemas de afinación no tradicionales o requieren alta precisión personalizable.

- **Solución:** Una aplicación móvil gratuita con modos escalables (básico y pro) que permite modificar cuerdas, frecuencias, céntimos y temperamentos, con capacidad de guardado y transferencia.

- **Valor generado:** Acceso democratizado a herramientas de afinación de nivel profesional, ahorro de tiempo en configuraciones repetitivas, y fomento de la exploración y la colaboración musical.

# 8. Alcance y fuera de alcance

Considerando el contexto del proyecto y priorizando una solución acotada, coherente y completamente funcional, el producto se limitará a las siguientes funcionalidades:

### Dentro del alcance:

- Captura y análisis en tiempo real: Uso del micrófono del dispositivo para captar frecuencias monofónicas.

- Modo de afinación estándar: Interfaz visual para afinación clásica (ej. Guitarra en Standard a 440Hz).

- Gestión de perfiles personalizados: Creación, edición y eliminación de _presets_ donde el usuario puede definir el número de cuerdas (de 4 a 8, la nota/octava específica de cada cuerda y modificar la frecuencia de referencia global (ej. 432Hz).

- Persistencia local: Almacenamiento de los perfiles en el dispositivo mediante una base de datos local.

- Funcionamiento 100% Offline: Toda la operativa de afinación y carga de perfiles funcionará sin dependencia de conexión a la red.

### Fuera de alcance:

- Sincronización en la nube (backend), sistemas de registro o _login_ de usuarios.

- Detección de audio polifónica (analizar el sonido de múltiples cuerdas tocadas al mismo tiempo).

- Herramientas musicales satélites como metrónomo, reproducción de tonos (diapasón) o diccionarios de acordes.

- Compartir perfiles a través de la red. Se podrán compartir perfiles a través de un código que se ingresará en la aplicación y se generará en cada perfil.

# 9. Requisitos funcionales

### RF01 - Capturar y analizar frecuencia de audio

- **Descripción:** El sistema deberá captar el sonido del instrumento a través del micrófono del dispositivo y mostrar en pantalla la nota musical detectada y su desviación (en céntimos) respecto a la frecuencia objetivo.

- **Usuario involucrado:** Músico / Luthier.

- **Criterio de aceptación:** Al emitir un sonido monofónico cerca del dispositivo, la interfaz gráfica debe reflejar la afinación detectada en tiempo real actualizando el indicador visual.

### RF02 - Configurar esquema de afinación personalizado

- **Descripción:** El usuario podrá crear un esquema de afinación a medida, indicando la cantidad total de cuerdas a afinar y asignando a cada una la nota musical exacta, su octava y la frecuencia de referencia en Hertz.

- **Usuario involucrado:** Músico / Luthier.

- **Criterio de aceptación:** Al guardar los cambios en el esquema, el motor de afinación de la pantalla principal debe actualizarse inmediatamente para utilizar las nuevas notas y frecuencias ingresadas como referencia.

### RF03 - Alternar entre modos de visualización (Básico / Pro)
- **Descripción:** El usuario podrá cambiar la interfaz entre un "Modo Básico" y un "Modo Pro". El Modo Básico utilizará exclusivamente la afinación estándar (sin permitir edición estructural), mientras que el Modo Pro habilitará la carga de esquemas personalizados.

- **Usuario involucrado:** Músico / Luthier.

- **Criterio de aceptación:** Al seleccionar el Modo Básico, la aplicación debe cargar la afinación de guitarra estándar por defecto y ocultar los botones de edición de cuerdas. Al volver al Modo Pro, debe restaurar el último esquema personalizado utilizado.

### RF04 - Persistir y gestionar perfiles de afinación (Presets)

- **Descripción:** El usuario podrá guardar su configuración actual de cuerdas y frecuencias bajo un nombre personalizado (perfil) para cargarla, editarla o eliminarla en el futuro.

- **Usuario involucrado:** Músico / Luthier.

- **Criterio de aceptación:** Un perfil guardado debe poder ser recuperado y aplicado correctamente desde la lista de perfiles incluso si la aplicación se cierra por completo, se reinicia el dispositivo o no hay conexión a internet.

# 10. Requisitos no funcionales

### RNF01 - Rendimiento y latencia:

La respuesta visual del indicador de afinación debe tener una latencia perceptiva mínima, actualizándose en menos de 300 milisegundos desde que el micrófono capta el sonido.

### RNF02 - Usabilidad y contexto visual:

La interfaz de la pantalla de afinación debe poseer un alto contraste cromático (por ejemplo, rojo para desfasado, verde para afinado) y un tamaño de tipografía que permita interpretar el estado de la afinación a una distancia mínima de 1 metro del dispositivo.

### RNF03 - Privacidad y permisos (Seguridad):

La aplicación debe solicitar el permiso de uso del micrófono de Android explícitamente en el primer uso, y el audio captado debe procesarse de manera efímera en la memoria RAM, sin almacenar ni transmitir ningún archivo de grabación de voz o sonido en el almacenamiento del dispositivo ni en la red.

### RNF04 - Comportamiento Offline:

De acuerdo con la estrategia de la aplicación, el 100% de las funcionalidades de la aplicación (captura de audio, cálculo de frecuencias y guardado/lectura de perfiles) deben poder ejecutarse sin ningún tipo de conexión a datos móviles o Wi-Fi.

# 11. Diseño en Figma

Enlace al Figma:

<https://www.figma.com/design/KFI7PfPh12DfvD9K346EHS/TunAR?node-id=0-1&t=Qe5n3tNdahN7AyRr-1>

En el Figma se diseñó, de manera provisoria, el flujo del modo “Pro”, en el que el usuario va a tener la posibilidad de crear y editar su propio preset. En este caso se presenta el flujo a la hora de cambiar una nota por otra en la aplicación.

# 12. Flujo de pantallas

*[Imagen: cuatro capturas del diseño en Figma, de izquierda a derecha: (1) pantalla principal del afinador en modo Pro con selector de presets desplegado; (2) menú desplegable para elegir la nota de la cuerda E2; (3) selección de la octava; (4) pantalla principal con el cambio reflejado. Se ven las cuerdas con sus notas, los botones de edición, guardar preset, “+” y “−”, y la barra de navegación inferior Basic / Pro.]*

En este flujo de pantallas, de izquierda a derecha, se puede observar el flujo de la aplicación donde el usuario presiona en la nota “E2”, se abre un menú desplegable donde selecciona la nota, y luego en qué octava de ella. Acto seguido, ve reflejado el cambio en la pantalla.

Para acceder a esta funcionalidad se debe clickear en el modo “edición”, debajo de las cuerdas con las notas. También, hay un botón para guardar el preset, y dos botones “+” y “-”, los cuales permitirían agregar y remover cuerdas.

Además, se puede cambiar de modos en la barra de navegación inferior, entre Basic y Pro. En la parte superior está la selección de presets, y debajo de ella la barra donde estaría la parte visual del afinador.

# 13. Diagrama de arquitectura

La aplicación se estructurará siguiendo los principios de **Clean Architecture** combinados con el patrón **MVVM** (Model-View-ViewModel), garantizando una alta separación de responsabilidades, testabilidad y la correcta abstracción de las fuentes de datos.

El flujo de datos será unidireccional (UDF) y se apoyará en el uso de _Coroutines_ y _StateFlow_ de Kotlin para manejar la reactividad, especialmente crítica para la actualización del afinador en tiempo real. El sistema se dividirá en tres capas principales:

### 1. Capa de Presentación (Presentation Layer):

- **UI (Jetpack Compose):** Interfaces declarativas que observan el estado emitido por el ViewModel y renderizan la pantalla en consecuencia (ej. dibujando el indicador de afinación o la lista de _presets_ ). No contienen lógica de negocio.

- **ViewModel:** Gestiona de forma explícita el estado de la pantalla ( UiState ). Recibe los eventos del usuario desde Compose, interactúa con los casos de uso y actualiza el StateFlow .

**2. Capa de Dominio (Domain Layer):** Es la capa central, independiente de cualquier librería de Android o de terceros (como TarsosDSP o Room).

- **Modelos:** Entidades de negocio puras, como TuningProfile o PitchResult (que contiene la nota detectada y los céntimos de desvío).

- **Casos de Uso (Use Cases):** Contienen las reglas del negocio. Ejemplos: AnalyzePitchUseCase (recibe la frecuencia fundamental y calcula la nota musical correspondiente) y SaveTuningProfileUseCase .

- **Interfaces:** Contratos que definen qué datos se necesitan de la capa exterior (ej. ITuningRepository , IAudioRepository ).

**3. Capa de Datos (Data Layer):** Responsable de implementar las interfaces del dominio y manejar la persistencia y el hardware.

- **Repositorios:** Actúan como única fuente de verdad, orquestando las fuentes de datos subyacentes.

- **Local Data Source (Persistencia):** Implementado con **Room** . Se encarga de las operaciones CRUD en la base de datos local para guardar y leer los _presets_ del usuario.

- **Hardware / DSP Data Source:** Un módulo especializado que utiliza la API AudioRecord de Android para capturar el buffer continuo de audio del micrófono y lo procesa utilizando la librería **TarsosDSP** (Algoritmo YIN) para devolver únicamente el valor numérico de la frecuencia (Hertz) al repositorio.

**Flujo de Datos del Afinador:** Micrófono -> AudioRecord -> TarsosDSP (Data Layer) -> AudioRepository -> AnalyzePitchUseCase (Domain Layer) -> TunerViewModel -> StateFlow -> Jetpack Compose (Presentation Layer).

# 14. Estrategia Offline First

La funcionalidad base de la aplicación no dependerá de ninguna manera de la conectividad del usuario. El procesamiento de la onda de sonido y los cálculos matemáticos para la afinación se realizarán 100% internamente en el hardware del dispositivo.

- **Qué información se almacenará localmente:** Se guardarán exclusivamente los perfiles de afinación personalizados (presets) creados por el usuario, utilizando una base de datos local mediante Room.

- **Cuál será la fuente remota:** Para la entrega de este MVP, no existirá una fuente remota. No se contemplarán integraciones para sincronizar usuarios en la nube (Backend) ni para compartir perfiles a través de la red.

- **Cuándo se actualizarán los datos:** Los datos (perfiles) se actualizarán únicamente de manera local, en el momento exacto en que el usuario guarde, edite o elimine una configuración desde la interfaz.

- **Qué verá el usuario sin conexión:** El usuario tendrá acceso total y sin restricciones a la interfaz de afinación y a todo el listado de sus perfiles guardados previamente.

- **Manejo de errores o conflictos:** Al no haber sincronización con un servidor externo, no existirán conflictos de sincronización. El único manejo de error contemplado será informar al usuario mediante la interfaz si intenta guardar un perfil sin haber completado los campos requeridos, o si intenta usar la aplicación sin otorgar permisos de micrófono.

# 15. Tecnologías previstas y justificación

El proyecto se desarrollará íntegramente en el ecosistema nativo de Android utilizando las siguientes tecnologías:

- **Kotlin:** Se utilizará como lenguaje de programación principal por su concisión, seguridad de tipos y soporte nativo para asincronismo mediante Coroutines, siendo el estándar actual para el desarrollo Android.

- **Jetpack Compose:** Se utilizará para la construcción de la interfaz gráfica. Su naturaleza declarativa permite crear la UI (como la aguja del afinador en tiempo real) de manera más eficiente y reactiva a los cambios de estado del modelo.

- **Navigation Compose:** Se implementará para gestionar el enrutamiento y la navegación entre la pantalla principal de afinación, la lista de perfiles y la pantalla de edición del Modo Pro.

- **ViewModel, Coroutines y StateFlow:** Constituyen el núcleo de la arquitectura MVVM. El _ViewModel_ mantendrá el estado de la UI separado de la lógica visual. Las _Coroutines_ permitirán procesar el audio en hilos secundarios sin bloquear la interfaz, y _StateFlow_ emitirá los cambios de frecuencia de manera reactiva hacia Jetpack Compose.

- **Room:** Se utilizará como ORM para la base de datos local. Es indispensable para cumplir con la estrategia _Offline First_ , permitiendo persistir, editar y eliminar los perfiles (presets) creados por el usuario de manera estructurada y segura.

- **Android AudioRecord API:** Se utilizará para capturar el flujo de audio puro (PCM) desde el micrófono del hardware del dispositivo de manera continua.

- **TarsosDSP (Algoritmo YIN / MPM):** Se integrará esta librería de procesamiento digital de señales (DSP) en la capa de datos. Su implementación del algoritmo YIN es ideal para instrumentos de cuerda y permite calcular la frecuencia fundamental con precisión fraccional en tiempo real y muy baja latencia, sin necesidad de conectividad.

# 16. Repositorio

Repositorio en github: <https://github.com/Juanchi456/TunAR>

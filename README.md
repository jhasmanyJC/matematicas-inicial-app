# 📘 matematicas-inicial-app

**Aplicación móvil educativa para Nivel Inicial con reconocimiento de voz (Vosk)**  
Diseñada para reforzar el aprendizaje de **matemáticas básicas** en **niños de 4 a 5 años**, fomentando la participación activa mediante comandos de voz.

## 🎯 Objetivo del proyecto

Apoyar el desarrollo del pensamiento lógico-matemático en el nivel inicial mediante el uso de herramientas tecnológicas interactivas, adaptadas al contexto educativo del **Colegio Caracas**.

## 🧩 Características principales

- 🗣️ **Reconocimiento de voz (Vosk)**: los niños responden hablando y la app evalúa su pronunciación y respuesta.  
- 🎮 **Actividades lúdicas**: incluye juegos como *Carrera de Números* o *Clasifiquemos juguetes*.  
- 👩‍🏫 **Panel del docente**: permite visualizar resultados individuales y globales.  
- 📊 **Sistema de seguimiento**: guarda el progreso, puntaje y actividades completadas.  
- 💾 **Base de datos local**: gestiona usuarios, actividades y puntajes.  
- 🎨 **Interfaz amigable y colorida**, adaptada para niños pequeños.

## 🛠️ Tecnologías utilizadas

- **Android Studio**  
- **Kotlin / Java**  
- **SQLite** (para almacenamiento local)  
- **Vosk API** (reconocimiento de voz offline)  
- **Material Design** (para la interfaz de usuario)
- **firebase**
- **Agente inteligente** 

## 🧮 Estructura del proyecto

```plaintext
app/
 ├── src/
 │   ├── main/
 │   │   ├── java/com/example/matematicas_inicial/
 │   │   ├── res/layout/
 │   │   ├── res/drawable/
 │   │   └── res/values/
 ├── gradle/
 ├── build.gradle
 └── settings.gradle

🧠 Ejemplos de actividades

🏁 Carrera de Números

El niño cuenta del 1 al 5 utilizando su voz.
Cada número dicho correctamente hace avanzar una casilla, simulando una carrera.
Cuando llega al número 5, ¡gana la partida!

🧸 Clasifiquemos juguetes

El niño clasifica objetos por color, forma o tamaño, diciendo las respuestas en voz alta.
El reconocimiento de voz identifica si la respuesta es correcta.

👨‍🏫 Panel docente

    📋 Visualización de resultados por niño y nivel.

    ⭐ Promedio general del grupo.

    📅 Historial de actividades y fechas de ejecución.

    📄 Generación de reportes en PDF.

📚 Institución educativa

Colegio Caracas
Proyecto educativo desarrollado para la asignatura de Matemáticas Inicial.

👨‍💻 Autor

Jhasmany Cano Gutierrez
📧 canojhasmany@gmail.com

Estado del proyecto

🔹 Versión predefensa: funcional, con módulos de actividades, reconocimiento de voz y registro docente.
🔹 Próximas mejoras:

Ampliar la base de datos con más actividades.
Optimizar interfaz y rendimiento general.

IMAGENES DE LAS INTERFACES DEL PROYECTO 

<img width="307" height="699" alt="image" src="https://github.com/user-attachments/assets/7eb0c864-79da-4412-925e-564452b248ad" />

Muestra la pantalla principal de la aplicación, en la cual se da la bienvenida a los usuarios y se permite elegir entre los perfiles de niño/a o profesor/a. El diseño utiliza colores vivos y elementos visuales amigables que promueven el interés y la identificación de los niños con el entorno educativo digital.

<img width="308" height="701" alt="image" src="https://github.com/user-attachments/assets/67db4633-6d5d-40e3-9608-f53669678852" />

Presenta la pantalla donde el niño o la niña ingresa su nombre antes de comenzar a utilizar la aplicación. El diseño emplea colores vivos y personajes animados que promueven un ambiente amigable, lúdico y motivador para los estudiantes de nivel inicial.

<img width="331" height="754" alt="image" src="https://github.com/user-attachments/assets/4af53a4a-74d3-43c7-b59c-667e9b28c7d9" />

Muestra la interfaz de inicio de sesión diseñada para los docentes, donde se ingresan las credenciales de acceso o se realiza el registro de un nuevo usuario. El diseño combina ilustraciones educativas y un esquema de colores armónico, transmitiendo claridad, orden y accesibilidad.

<img width="297" height="675" alt="image" src="https://github.com/user-attachments/assets/ba2e3343-f495-4fdb-bbd7-a9c6409d4c5c" />

Presenta el formulario de registro destinado al docente, donde se ingresan los datos personales y se selecciona el nivel educativo que enseña. La interfaz mantiene un diseño claro y ordenado, promoviendo una experiencia de usuario accesible y coherente con la temática educativa de la aplicación.

<img width="325" height="741" alt="image" src="https://github.com/user-attachments/assets/b996c7b2-e280-42ab-8c42-8c279c9f740c" />

Muestra la pantalla principal del usuario, desde donde el estudiante puede acceder a las distintas secciones de la aplicación. El diseño combina elementos visuales llamativos y accesibles, favoreciendo la autonomía y el aprendizaje activo en un entorno digital atractivo y educativo.

<img width="276" height="628" alt="image" src="https://github.com/user-attachments/assets/a089f05e-bbca-4aa6-aed5-5e80db494812" />

Presenta el juego “Carrera de Números Nivel 2”, en el cual los niños practican el conteo del 1 al 5 utilizando comandos de voz. Cada número dicho correctamente permite avanzar una casilla, simulando una competencia divertida que incentiva el aprendizaje oral y numérico de forma interactiva.

<img width="283" height="646" alt="image" src="https://github.com/user-attachments/assets/24c2ee45-3a4e-42fb-b953-39d1193a6bc5" />

Muestra la pantalla del menú del docente, donde se presentan sus datos personales y las opciones para acceder a los resultados por grado. El diseño combina funcionalidad y estética educativa, brindando una interfaz clara y organizada para la gestión del aprendizaje de los estudiantes.

<img width="290" height="661" alt="image" src="https://github.com/user-attachments/assets/7221123e-0c6a-467b-ad6a-84bab0acb437" />

Muestra la interfaz destinada al seguimiento del progreso estudiantil, donde el docente puede consultar y filtrar los resultados por alumno o visualizar reportes globales del grado. El diseño prioriza la claridad de la información y la funcionalidad, facilitando la evaluación del desempeño de los niños en las actividades de aprendizaje.

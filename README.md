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

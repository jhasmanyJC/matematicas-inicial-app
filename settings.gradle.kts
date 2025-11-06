pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()

        // ✅ Repositorio estable de Vosk (mirror activo)
        maven { url = uri("https://alphacephei.com/vosk/repo/") }

        // 🔹 Opcional (forks y ejemplos)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "com.nico.matinicial"
include(":app")
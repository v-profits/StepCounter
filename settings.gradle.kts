// Откуда Gradle берёт ПЛАГИНЫ (Android Gradle Plugin, Kotlin и т.д.)
pluginManagement {
    repositories {
        google()            // плагины Android и библиотеки Google
        mavenCentral()      // основной репозиторий библиотек
        gradlePluginPortal()
    }
}

// Откуда Gradle берёт БИБЛИОТЕКИ для модулей проекта
dependencyResolutionManagement {
    // Запрещаем модулям объявлять свои репозитории — всё централизованно здесь
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StepCounter"   // название проекта
include(":app")                    // единственный модуль — приложение

// Корневой build-файл: только объявляем версии плагинов.
// "apply false" — плагины подключаются в модуле app, а не здесь.
plugins {
    id("com.android.application") version "8.7.3" apply false          // сборка Android-приложения
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false    // поддержка Kotlin
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false // компилятор Jetpack Compose
    // KSP — обработчик аннотаций, он генерирует код для Room. Версия должна совпадать с версией Kotlin (2.0.21)
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
}

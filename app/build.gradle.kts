// Плагины, применяемые к модулю app
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") // генерация кода для Room
}

android {
    namespace = "com.example.stepcounter"  // пакет, в котором генерируется класс R
    compileSdk = 35                        // версия Android SDK, против которой компилируем

    defaultConfig {
        applicationId = "com.example.stepcounter" // уникальный ID приложения (в Google Play)
        minSdk = 26      // минимальная поддерживаемая версия — Android 8.0
        targetSdk = 35   // версия, под которую приложение оптимизировано
        versionCode = 1        // внутренний номер версии (растёт с каждым релизом)
        versionName = "1.0"    // версия, которую видит пользователь
    }

    buildTypes {
        release {
            isMinifyEnabled = false // без сжатия кода (R8) — проще отлаживать
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true // включаем Jetpack Compose
    }
}

dependencies {
    // BOM — набор согласованных версий всех библиотек Compose
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")                         // базовый UI
    implementation("androidx.compose.ui:ui-tooling-preview")         // @Preview в студии
    implementation("androidx.compose.material3:material3")           // компоненты Material 3
    implementation("androidx.compose.material:material-icons-extended") // расширенный набор иконок

    implementation("androidx.core:core-ktx:1.15.0")                  // Kotlin-расширения Android
    implementation("androidx.activity:activity-compose:1.9.3")       // Activity + Compose, запрос разрешений
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7") // ViewModel в Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")   // collectAsStateWithLifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0") // корутины
    implementation("com.google.android.material:material:1.12.0")    // нужна для XML-темы приложения

    // Room — база данных SQLite с удобным Kotlin-API
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")        // поддержка корутин и Flow
    ksp("androidx.room:room-compiler:2.6.1")              // генератор кода Room (через KSP)

    debugImplementation("androidx.compose.ui:ui-tooling")            // инструменты отладки UI
}

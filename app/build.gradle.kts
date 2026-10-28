import java.io.FileInputStream
import java.util.Properties

// Плагины, применяемые к модулю app
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") // генерация кода для Room
}

// Данные ключа подписи читаются из файла keystore.properties в корне проекта.
// Этот файл и сам ключ (.jks) НЕЛЬЗЯ отправлять в Git — они уже добавлены в .gitignore.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

android {
    namespace = "com.semenenko.stepcounter"  // пакет, в котором лежит код и генерируется класс R (НЕ меняйте)
    compileSdk = 36                        // Android 16: Google Play требует targetSdk 36 для новых приложений

    defaultConfig {
        // УНИКАЛЬНЫЙ идентификатор приложения в магазинах (совпадает с namespace). После первой публикации
        // его нельзя изменить. Для RuStore и Google Play он должен быть одинаковым.
        applicationId = "com.semenenko.stepcounter"
        minSdk = 26      // минимальная поддерживаемая версия — Android 8.0
        targetSdk = 36   // версия, под которую приложение оптимизировано (требование Google Play)
        versionCode = 1        // внутренний номер версии: с каждой загрузкой в магазин должен РАСТИ
        versionName = "1.0"    // версия, которую видит пользователь
    }

    // Подпись релизной сборки (данные берутся из keystore.properties)
    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    // Два варианта приложения: для Google Play и для RuStore.
    // Отличаются библиотекой окна оценки (звёздочки) и ссылкой на поддержку.
    flavorDimensions += "store"
    productFlavors {
        create("play") {
            dimension = "store"
            // Для Google Play ссылку на пожертвования оставляем пустой (правила магазина)
            buildConfigField("String", "SUPPORT_URL", "\"\"")
            // Правила Google Play запрещают задавать вопросы перед окном оценки и отбирать «довольных» пользователей,
            // поэтому здесь своё окно со звёздами выключено: в нужный момент показывается только окно оценки Google
            buildConfigField("boolean", "RATING_FUNNEL", "false")
            buildConfigField("String", "STORE_APP_URI", "\"market://details?id=\"")
            buildConfigField("String", "STORE_WEB_URL", "\"https://play.google.com/store/apps/details?id=\"")
        }
        create("rustore") {
            dimension = "store"
            // Для RuStore можно указать ссылку на страницу поддержки, например "https://ko-fi.com/ваш_ник"
            buildConfigField("String", "SUPPORT_URL", "\"\"")
            // В RuStore включено своё окно со звёздами: 4–5 звёзд ведут на страницу приложения в RuStore,
            // 1–3 звезды — в письмо разработчику
            buildConfigField("boolean", "RATING_FUNNEL", "true")
            buildConfigField("String", "STORE_APP_URI", "\"rustore://apps.rustore.ru/app/\"")
            buildConfigField("String", "STORE_WEB_URL", "\"https://www.rustore.ru/catalog/app/\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true      // сжатие и обфускация кода (R8): приложение становится заметно меньше
            isShrinkResources = true    // удаление неиспользуемых ресурсов
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
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
        compose = true      // включаем Jetpack Compose
        buildConfig = true  // нужен для полей SUPPORT_URL, RATING_FUNNEL и ссылок магазина
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

    // Окно оценки приложения (звёздочки): своя библиотека для каждого магазина
    "playImplementation"("com.google.android.play:review:2.0.2")        // Google Play In-App Review
    "rustoreImplementation"(platform("ru.rustore.sdk:bom:2026.10.01"))  // RuStore SDK (набор версий)
    "rustoreImplementation"("ru.rustore.sdk:review")                    // RuStore: оценки и отзывы

    debugImplementation("androidx.compose.ui:ui-tooling")            // инструменты отладки UI
}

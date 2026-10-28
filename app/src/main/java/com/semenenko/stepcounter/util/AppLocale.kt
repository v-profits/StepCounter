package com.semenenko.stepcounter.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Язык приложения, выбранный пользователем в настройках.
 * Если язык не выбран («Как в системе»), используется язык телефона.
 */
object AppLocale {
    /** Выбранный язык или null, если нужно следовать системе. */
    @Volatile
    var override: Locale? = null
        private set

    /** Язык, который нужно применять сейчас (для чисел, дат и текстов). */
    val current: Locale get() = override ?: Locale.getDefault()

    /** Запоминает выбор: "" — как в системе, иначе код языка ("en", "ru"). */
    fun set(languageTag: String) {
        override = if (languageTag.isBlank()) null else Locale.forLanguageTag(languageTag)
    }
}

/**
 * Возвращает контекст с выбранным языком приложения: строки из ресурсов (getString)
 * через него берутся на нужном языке. Если язык не выбран — возвращает тот же контекст.
 */
fun Context.withAppLocale(): Context {
    val locale = AppLocale.override ?: return this
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

package com.semenenko.stepcounter.util

import android.text.format.DateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// Текущий язык приложения (выбранный в настройках или язык телефона).
// Берём при каждом вызове, чтобы форматирование сразу следовало смене языка.
private val locale: Locale get() = AppLocale.current

/** 12345 -> "12 345" (рус.) или "12,345" (англ.) — с разделителем тысяч. */
fun Int.formatSteps(): String = String.format(locale, "%,d", this)

/** 5.43 -> "5,4" (рус.) или "5.4" (англ.) — один знак после запятой. */
fun Double.formatKm(): String = String.format(locale, "%.1f", this)

/** 278.6 -> "279" (округление до целого). */
fun Double.formatKcal(): String = String.format(locale, "%,d", this.roundToInt())

/** Дата -> "Среда, 14 октября" / "Wednesday, October 14" (порядок слов зависит от языка). */
fun LocalDate.fullTitle(): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    return DateTimeFormatter.ofPattern(pattern, locale).format(this).replaceFirstChar { it.uppercase() }
}

/** Дата -> "Ср" / "Wed" (короткое название дня недели). */
fun LocalDate.shortWeekday(): String =
    dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase() }

/** Дата -> "14 окт." / "Oct 14" (число и месяц). */
fun LocalDate.dayMonth(): String =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale).format(this)

/** Месяц -> "Окт" / "Oct". [names] — массив из ресурсов (months_short). */
fun YearMonth.shortName(names: Array<String>): String = names[monthValue - 1]

/** Месяц -> "Октябрь 2026" / "October 2026". [names] — массив из ресурсов (months_full). */
fun YearMonth.fullName(names: Array<String>): String = "${names[monthValue - 1]} $year"

/** Дата -> "16 окт. 2026 г." / "Oct 16, 2026" (дата с годом в формате выбранного языка). */
fun LocalDate.mediumDate(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(this)

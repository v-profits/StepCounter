package com.example.stepcounter.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// Русская локаль: даёт русские названия дней/месяцев и запятую в дробях
val RU: Locale = Locale.forLanguageTag("ru")

/** 12345 -> "12 345" (с разделителем тысяч). */
fun Int.formatSteps(): String = String.format(RU, "%,d", this)

/** 5.43 -> "5,4" (один знак после запятой). */
fun Double.formatKm(): String = String.format(RU, "%.1f", this)

/** 278.6 -> "279" (округление до целого). */
fun Double.formatKcal(): String = String.format(RU, "%,d", this.roundToInt())

/** Дата -> "Среда, 14 октября" (первая буква заглавная). */
fun LocalDate.fullTitle(): String =
    format(DateTimeFormatter.ofPattern("EEEE, d MMMM", RU)).replaceFirstChar { it.uppercase() }

/** Дата -> "Ср" (короткое название дня недели). */
fun LocalDate.shortWeekday(): String =
    dayOfWeek.getDisplayName(TextStyle.SHORT, RU).replaceFirstChar { it.uppercase() }

/** Дата -> "14 окт" (число и месяц). */
fun LocalDate.dayMonth(): String = format(DateTimeFormatter.ofPattern("d MMM", RU))

// Названия месяцев заданы вручную, чтобы выглядели одинаково на всех телефонах
private val MONTHS_SHORT = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
private val MONTHS_FULL = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

/** Месяц -> "Окт". */
fun YearMonth.shortName(): String = MONTHS_SHORT[monthValue - 1]

/** Месяц -> "Октябрь 2026". */
fun YearMonth.fullName(): String = "${MONTHS_FULL[monthValue - 1]} $year"

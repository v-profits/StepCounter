package com.example.stepcounter.data

import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

/** Шаги за один день. */
data class DaySteps(val date: LocalDate, val steps: Int)

/** Итоги за период (неделю, месяц или год) для экрана статистики. */
data class PeriodSummary(
    val total: Int,        // всего шагов
    val average: Int,      // среднее в день
    val best: DaySteps?,   // лучший день (null, если данных нет)
    val goalDays: Int,     // сколько дней цель выполнена
    val countedDays: Int,  // сколько дней учтено в расчёте
)

/**
 * Список из последних [length] дней, заканчивая сегодняшним (даты по возрастанию).
 * Для дней без записи подставляется 0 шагов.
 */
fun periodDays(history: Map<LocalDate, Int>, today: LocalDate, length: Int): List<DaySteps> =
    (length - 1 downTo 0).map {                      // it = сколько дней назад
        val d = today.minusDays(it.toLong())
        DaySteps(d, history[d] ?: 0)
    }

/** Все дни календарного месяца [ym] — от 1-го до последнего числа. */
fun daysOfMonth(history: Map<LocalDate, Int>, ym: YearMonth): List<DaySteps> =
    (1..ym.lengthOfMonth()).map {
        val d = ym.atDay(it)
        DaySteps(d, history[d] ?: 0)
    }

/** Все дни календарного года [year] — с 1 января по 31 декабря. */
fun daysOfYear(history: Map<LocalDate, Int>, year: Int): List<DaySteps> {
    val start = LocalDate.of(year, 1, 1)
    return (0 until Year.of(year).length()).map {
        val d = start.plusDays(it.toLong())
        DaySteps(d, history[d] ?: 0)
    }
}

/**
 * Считает итоги за период. Учитываются только дни от первого дня, за который есть запись,
 * и не позже сегодняшнего (будущие дни месяца или года в расчёт не входят).
 */
fun summarize(days: List<DaySteps>, firstRecorded: LocalDate?, today: LocalDate, goal: Int): PeriodSummary {
    val counted = if (firstRecorded == null) emptyList()
    else days.filter { !it.date.isBefore(firstRecorded) && !it.date.isAfter(today) }
    val total = counted.sumOf { it.steps }
    return PeriodSummary(
        total = total,
        average = if (counted.isEmpty()) 0 else total / counted.size,
        best = counted.maxByOrNull { it.steps }?.takeIf { it.steps > 0 }, // день с максимумом шагов
        goalDays = counted.count { it.steps >= goal },                    // дней с выполненной целью
        countedDays = counted.size,
    )
}

/**
 * «Серия»: сколько дней подряд выполнялась цель.
 * Если сегодня цель ещё не достигнута — серия считается со вчерашнего дня
 * (чтобы серия не «обнулялась» с утра).
 */
fun currentStreak(history: Map<LocalDate, Int>, today: LocalDate, goal: Int): Int {
    var d = if ((history[today] ?: 0) >= goal) today else today.minusDays(1)
    var n = 0
    // Идём назад по дням, пока цель выполнена
    while ((history[d] ?: 0) >= goal) {
        n++
        d = d.minusDays(1)
    }
    return n
}

/** Итоги за один календарный месяц (для годовой статистики). */
data class MonthSteps(
    val month: YearMonth,    // какой месяц
    val total: Int,          // всего шагов за месяц
    val daysCounted: Int,    // сколько дней месяца учтено (с первой записи и не позже сегодня)
) {
    /** Среднее число шагов в день за месяц (0, если дней нет). */
    val average: Int get() = if (daysCounted == 0) 0 else total / daysCounted
}

/**
 * Итоги месяца [ym]. В расчёт идут только дни с первой записи до сегодняшнего дня —
 * поэтому у нового пользователя средние значения не занижаются пустыми днями.
 */
fun monthSteps(history: Map<LocalDate, Int>, ym: YearMonth, today: LocalDate, firstRecorded: LocalDate?): MonthSteps {
    val lastDay = ym.atEndOfMonth().let { if (it.isAfter(today)) today else it } // не считаем будущие дни
    var total = 0
    var days = 0
    var d = ym.atDay(1)
    while (!d.isAfter(lastDay)) {
        if (firstRecorded != null && !d.isBefore(firstRecorded)) {
            total += history[d] ?: 0
            days++
        }
        d = d.plusDays(1)
    }
    return MonthSteps(ym, total, days)
}

/** Все 12 месяцев года [year] (январь … декабрь) — для годовой статистики. */
fun monthsOfYear(history: Map<LocalDate, Int>, year: Int, today: LocalDate, firstRecorded: LocalDate?): List<MonthSteps> =
    (1..12).map { monthSteps(history, YearMonth.of(year, it), today, firstRecorded) }

/** Месяцы, за которые есть данные (плюс текущий), от новых к старым — для выбора месяца. */
fun monthsWithData(history: Map<LocalDate, Int>, today: LocalDate): List<YearMonth> =
    (history.keys.map { YearMonth.from(it) } + YearMonth.from(today)).distinct().sortedDescending()

/** Годы, за которые есть данные (плюс текущий), от новых к старым — для выбора года. */
fun yearsWithData(history: Map<LocalDate, Int>, today: LocalDate): List<Int> =
    (history.keys.map { it.year } + today.year).distinct().sortedDescending()

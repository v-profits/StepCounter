package com.example.stepcounter.data

import java.time.LocalDate

/** Шаги за один день. */
data class DaySteps(val date: LocalDate, val steps: Int)

/** Итоги за период (неделю или месяц) для экрана статистики. */
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

/**
 * Считает итоги за период. Учитываются только дни, начиная с первого дня,
 * за который вообще есть запись (иначе у новичка среднее было бы заниженным).
 */
fun summarize(days: List<DaySteps>, firstRecorded: LocalDate?, goal: Int): PeriodSummary {
    val counted = if (firstRecorded == null) emptyList()
    else days.filter { !it.date.isBefore(firstRecorded) } // дни не раньше первой записи
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

package com.example.stepcounter.data

import java.time.LocalDate
import java.time.YearMonth

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
 * Последние [count] месяцев, заканчивая текущим (по возрастанию).
 * В расчёт идут только дни с первой записи до сегодняшнего дня —
 * поэтому у нового пользователя средние значения не занижаются пустыми днями.
 */
fun periodMonths(
    history: Map<LocalDate, Int>,
    today: LocalDate,
    firstRecorded: LocalDate?,
    count: Int = 12,
): List<MonthSteps> =
    (count - 1 downTo 0).map { back ->
        val ym = YearMonth.from(today).minusMonths(back.toLong())
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
        MonthSteps(ym, total, days)
    }

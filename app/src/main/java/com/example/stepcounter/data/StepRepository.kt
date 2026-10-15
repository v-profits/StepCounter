package com.example.stepcounter.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

/**
 * Настройки пользователя: цель на день, длина шага и вес.
 * data class — Kotlin сам создаёт copy(), equals(), toString() и т.д.
 */
data class UserSettings(
    val goal: Int = 10_000,      // дневная цель, шагов
    val stepLengthCm: Int = 75,  // длина шага, см
    val weightKg: Int = 70,      // вес, кг
) {
    /** Пройденное расстояние в километрах: шаги * длина шага (см) / 100 000 (см в км). */
    fun distanceKm(steps: Int): Double = steps * stepLengthCm / 100_000.0

    /** Приблизительный расход энергии при ходьбе: ~0.53 ккал на кг веса на км пути. */
    fun calories(steps: Int): Double = distanceKm(steps) * weightKg * 0.53
}

/**
 * Хранилище шагов по дням (SharedPreferences) + настройки пользователя.
 *
 * Датчик TYPE_STEP_COUNTER отдаёт общее число шагов с момента перезагрузки телефона,
 * поэтому мы запоминаем последнее показание и добавляем к текущему дню только РАЗНИЦУ.
 */
class StepRepository(context: Context) {

    // Файл настроек "steps" — простое key-value хранилище на диске телефона
    private val prefs = context.applicationContext
        .getSharedPreferences("steps", Context.MODE_PRIVATE)

    // Объект-замок: защищает данные, если к ним обратятся из нескольких потоков
    private val lock = Any()

    // История: дата -> количество шагов. StateFlow — «живое» значение:
    // как только оно меняется, интерфейс и уведомление автоматически обновляются.
    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<Map<LocalDate, Int>> = _history.asStateFlow() // наружу — только для чтения

    // Настройки (тоже «живые»)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    /** Сколько шагов за указанный день (по умолчанию — сегодня). Нет записи — 0. */
    fun stepsFor(date: LocalDate = LocalDate.now()): Int = _history.value[date] ?: 0

    /**
     * Вызывается сервисом при каждом новом показании датчика.
     * [value] — общее число шагов с момента загрузки телефона.
     */
    fun onCounterValue(value: Long) {
        synchronized(lock) {
            // Прошлое показание датчика (-1, если ещё ни разу не сохраняли)
            val last = prefs.getLong(KEY_LAST_COUNTER, -1L)
            val delta = when {
                last < 0 -> 0L            // первое показание — просто запоминаем как точку отсчёта
                value >= last -> value - last // обычный случай: прибавилось столько шагов
                else -> value             // значение уменьшилось => телефон перезагружался, счётчик начался с нуля
            }
            // Сохраняем текущее показание как «последнее»
            prefs.edit().putLong(KEY_LAST_COUNTER, value).apply()
            // Если шаги были — добавляем их к сегодняшнему дню
            if (delta > 0) addSteps(delta.toInt())
        }
    }

    /** Прибавляет [delta] шагов к указанному дню и сохраняет на диск. */
    private fun addSteps(delta: Int, date: LocalDate = LocalDate.now()) {
        val newValue = (_history.value[date] ?: 0) + delta
        // Ключ вида "d_2026-10-14" -> число шагов
        prefs.edit().putInt("$KEY_DAY$date", newValue).apply()
        // Обновляем «живое» значение (создаём новую карту — так Compose заметит изменение)
        _history.value = _history.value + (date to newValue)
    }

    /** Меняет настройки: [transform] получает старые и возвращает новые. Сохраняет на диск. */
    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val s = transform(_settings.value)
        prefs.edit()
            .putInt(KEY_GOAL, s.goal)
            .putInt(KEY_STEP_LENGTH, s.stepLengthCm)
            .putInt(KEY_WEIGHT, s.weightKg)
            .apply()
        _settings.value = s
    }

    /** Удаляет всю историю шагов (настройки остаются). */
    fun clearHistory() {
        synchronized(lock) {
            val editor = prefs.edit()
            // Удаляем только ключи дней (начинаются с "d_")
            prefs.all.keys.filter { it.startsWith(KEY_DAY) }.forEach { editor.remove(it) }
            editor.apply()
            _history.value = emptyMap()
        }
    }

    /** Заполняет прошлые 29 дней случайными данными — удобно, чтобы посмотреть интерфейс. */
    fun fillDemoData() {
        synchronized(lock) {
            val editor = prefs.edit()
            val map = _history.value.toMutableMap()
            val today = LocalDate.now()
            for (i in 1..29) {
                val date = today.minusDays(i.toLong())      // i дней назад
                val steps = Random.nextInt(2_500, 14_500)   // случайное число шагов
                editor.putInt("$KEY_DAY$date", steps)
                map[date] = steps
            }
            editor.apply()
            _history.value = map // сегодняшний день не трогаем
        }
    }

    /** Читает с диска всю историю: пары «дата -> шаги». Вызывается один раз при создании. */
    private fun loadHistory(): Map<LocalDate, Int> {
        val result = HashMap<LocalDate, Int>()
        for ((key, value) in prefs.all) {
            if (key.startsWith(KEY_DAY) && value is Int) {
                // Из ключа "d_2026-10-14" достаём дату; если формат сломан — пропускаем
                val date = runCatching { LocalDate.parse(key.removePrefix(KEY_DAY)) }.getOrNull()
                if (date != null) result[date] = value
            }
        }
        return result
    }

    /** Читает настройки с диска; если их нет — берём значения по умолчанию. */
    private fun loadSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            goal = prefs.getInt(KEY_GOAL, d.goal),
            stepLengthCm = prefs.getInt(KEY_STEP_LENGTH, d.stepLengthCm),
            weightKg = prefs.getInt(KEY_WEIGHT, d.weightKg),
        )
    }

    // Названия ключей в хранилище
    private companion object {
        const val KEY_DAY = "d_"                      // префикс ключей дней
        const val KEY_LAST_COUNTER = "last_counter"   // последнее показание датчика
        const val KEY_GOAL = "goal"
        const val KEY_STEP_LENGTH = "step_length_cm"
        const val KEY_WEIGHT = "weight_kg"
    }
}

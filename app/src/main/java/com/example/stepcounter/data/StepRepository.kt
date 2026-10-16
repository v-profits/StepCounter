package com.example.stepcounter.data

import android.content.Context
import com.example.stepcounter.widget.StepWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.random.Random

/** Варианты темы оформления. */
object ThemeMode {
    const val SYSTEM = 0  // как в системе
    const val LIGHT = 1   // светлая
    const val DARK = 2    // тёмная
}

/**
 * Настройки пользователя: цель на день, длина шага, вес, тема и язык.
 * data class — Kotlin сам создаёт copy(), equals(), toString() и т.д.
 */
data class UserSettings(
    val goal: Int = 10_000,      // дневная цель, шагов
    val stepLengthCm: Int = 75,  // длина шага, см
    val weightKg: Int = 70,      // вес, кг
    val themeMode: Int = ThemeMode.SYSTEM, // тема: 0 — как в системе, 1 — светлая, 2 — тёмная
    val language: String = "",             // язык: "" — как в системе, "en", "ru"
) {
    /** Пройденное расстояние в километрах: шаги * длина шага (см) / 100 000 (см в км). */
    fun distanceKm(steps: Int): Double = steps * stepLengthCm / 100_000.0

    /** Приблизительный расход энергии при ходьбе: ~0.53 ккал на кг веса на км пути. */
    fun calories(steps: Int): Double = distanceKm(steps) * weightKg * 0.53
}

/**
 * Хранилище данных приложения.
 *  - Шаги по дням лежат в базе Room (файл steps.db).
 *  - Настройки и «последнее показание датчика» — в SharedPreferences (это пара чисел, база не нужна).
 *
 * Датчик TYPE_STEP_COUNTER отдаёт общее число шагов с момента перезагрузки телефона,
 * поэтому мы запоминаем последнее показание и добавляем к текущему дню только РАЗНИЦУ.
 */
class StepRepository(context: Context) {

    private val appContext = context.applicationContext

    // Файл "steps": настройки, последнее показание датчика и (для переноса) старая история
    private val prefs = appContext.getSharedPreferences("steps", Context.MODE_PRIVATE)

    // DAO — доступ к таблице шагов
    private val dao = AppDatabase.get(appContext).stepDao()

    // Фоновая область корутин: все операции с базой выполняются не в главном потоке
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Замок для логики «последнего показания датчика»
    private val lock = Any()

    // Начальное значение истории. Читаем базу один раз при запуске (и переносим старые данные
    // из SharedPreferences, если они есть). runBlocking ждёт результата — это занимает
    // доли секунды и нужно, чтобы экран не мигал нулями при открытии.
    private val initialHistory: Map<LocalDate, Int> = runBlocking(Dispatchers.IO) {
        migrateLegacyPrefs()
        dao.getAll().toHistory()
    }

    // История: дата -> шаги. Room сам присылает новый список при любом изменении таблицы,
    // мы превращаем его в Map и отдаём как «живое» значение (StateFlow) для интерфейса.
    val history: StateFlow<Map<LocalDate, Int>> = dao.observeAll()
        .map { it.toHistory() }
        .stateIn(scope, SharingStarted.Eagerly, initialHistory)

    // Настройки (тоже «живые»)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    init {
        // Обновляем виджет на рабочем столе, когда меняются шаги сегодня или настройки.
        // После обновления делаем паузу — так виджет обновляется не чаще раза в несколько секунд
        // (conflate оставляет только самое свежее значение, пока мы ждём).
        scope.launch {
            combine(history, settings) { h, s -> (h[LocalDate.now()] ?: 0) to s }
                .distinctUntilChanged()
                .conflate()
                .collect { (steps, userSettings) ->
                    StepWidgetProvider.updateAll(appContext, steps, userSettings)
                    delay(WIDGET_MIN_INTERVAL_MS)
                }
        }
    }

    /** Сколько шагов за указанный день (по умолчанию — сегодня). Нет записи — 0. */
    fun stepsFor(date: LocalDate = LocalDate.now()): Int = history.value[date] ?: 0

    /**
     * Вызывается сервисом при каждом новом показании датчика.
     * [value] — общее число шагов с момента загрузки телефона.
     */
    fun onCounterValue(value: Long) {
        // Считаем, сколько шагов прибавилось с прошлого показания
        val delta = synchronized(lock) {
            val last = prefs.getLong(KEY_LAST_COUNTER, -1L) // -1, если ещё ни разу не сохраняли
            prefs.edit().putLong(KEY_LAST_COUNTER, value).apply()
            when {
                last < 0 -> 0L                // первое показание — просто точка отсчёта
                value >= last -> value - last // обычный случай: прибавилось столько шагов
                else -> value                 // значение уменьшилось => телефон перезагружался, счёт с нуля
            }
        }
        if (delta > 0) {
            // Дату берём сейчас (а не когда выполнится запись), чтобы шаги попали в правильный день
            val date = LocalDate.now().toString()
            scope.launch { dao.addSteps(date, delta.toInt()) }
        }
    }

    /** Меняет настройки: [transform] получает старые и возвращает новые. Сохраняет на диск. */
    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val s = transform(_settings.value)
        prefs.edit()
            .putInt(KEY_GOAL, s.goal)
            .putInt(KEY_STEP_LENGTH, s.stepLengthCm)
            .putInt(KEY_WEIGHT, s.weightKg)
            .putInt(KEY_THEME, s.themeMode)
            .putString(KEY_LANGUAGE, s.language)
            .apply()
        _settings.value = s
    }

    /** Удаляет всю историю шагов (настройки остаются). Интерфейс обновится сам через Flow. */
    fun clearHistory() {
        scope.launch { dao.clear() }
    }

    /** Заполняет прошлые 29 дней случайными данными — удобно, чтобы посмотреть интерфейс. */
    fun fillDemoData() {
        scope.launch {
            val today = LocalDate.now()
            dao.upsertAll(
                (1..29).map { i ->
                    DailyStepsEntity(
                        date = today.minusDays(i.toLong()).toString(), // i дней назад (сегодня не трогаем)
                        steps = Random.nextInt(2_500, 14_500),         // случайное число шагов
                    )
                }
            )
        }
    }

    /** Список строк таблицы -> карта «дата -> шаги». Строки с неверной датой пропускаются. */
    private fun List<DailyStepsEntity>.toHistory(): Map<LocalDate, Int> =
        mapNotNull { e ->
            runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { it to e.steps }
        }.toMap()

    /**
     * Однократный перенос истории из старого хранилища (SharedPreferences) в Room.
     * Нужен тем, кто пользовался прошлой версией приложения: их данные не потеряются.
     */
    private suspend fun migrateLegacyPrefs() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return // уже переносили

        // Читаем старые записи вида "d_2026-10-14" -> число
        val legacy = ArrayList<DailyStepsEntity>()
        for ((key, value) in prefs.all) {
            if (key.startsWith(KEY_DAY) && value is Int) {
                val date = key.removePrefix(KEY_DAY)
                if (runCatching { LocalDate.parse(date) }.isSuccess) {
                    legacy.add(DailyStepsEntity(date, value))
                }
            }
        }
        if (legacy.isNotEmpty()) dao.upsertAll(legacy)

        // Удаляем старые ключи и помечаем, что перенос выполнен
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(KEY_DAY) }.forEach { editor.remove(it) }
        editor.putBoolean(KEY_MIGRATED, true).apply()
    }

    /** Читает настройки с диска; если их нет — берём значения по умолчанию. */
    private fun loadSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            goal = prefs.getInt(KEY_GOAL, d.goal),
            stepLengthCm = prefs.getInt(KEY_STEP_LENGTH, d.stepLengthCm),
            weightKg = prefs.getInt(KEY_WEIGHT, d.weightKg),
            themeMode = prefs.getInt(KEY_THEME, d.themeMode),
            language = prefs.getString(KEY_LANGUAGE, d.language) ?: d.language,
        )
    }

    // Названия ключей в хранилище и константы
    private companion object {
        const val KEY_DAY = "d_"                      // префикс ключей дней в СТАРОМ хранилище
        const val KEY_MIGRATED = "migrated_to_room"   // флаг «перенос в Room выполнен»
        const val KEY_LAST_COUNTER = "last_counter"   // последнее показание датчика
        const val KEY_GOAL = "goal"
        const val KEY_STEP_LENGTH = "step_length_cm"
        const val KEY_WEIGHT = "weight_kg"
        const val KEY_THEME = "theme_mode"
        const val KEY_LANGUAGE = "language"
        const val WIDGET_MIN_INTERVAL_MS = 4_000L     // не чаще одного обновления виджета в 4 секунды
    }
}

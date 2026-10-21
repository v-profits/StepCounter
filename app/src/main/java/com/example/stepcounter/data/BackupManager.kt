package com.example.stepcounter.data

import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** Настройки, которые переносятся вместе со статистикой (тема и язык не переносятся). */
data class BackupSettings(
    val goal: Int,
    val heightCm: Int,
    val sex: Int,
    val weightKg: Int,
    val stepLengthCm: Int,
    val stepLengthManual: Boolean,
)

/** Содержимое файла резервной копии после чтения. */
data class BackupData(
    val days: Map<LocalDate, Int>,    // шаги по дням
    val settings: BackupSettings?,    // настройки (могут отсутствовать)
)

/**
 * Резервная копия статистики: один небольшой JSON-файл.
 * Его можно скопировать на другой телефон и загрузить в приложении.
 */
object BackupManager {
    private const val APP_ID = "StepCounter"   // метка: файл создан этим приложением
    private const val FORMAT_VERSION = 1       // версия формата файла
    const val MIME = "application/json"

    /** Типы файлов, которые показывает окно выбора при загрузке. На старых версиях Android .json иногда считается «неизвестным» типом. */
    val PICKER_MIME_TYPES = arrayOf(MIME, "application/octet-stream")

    /** Имя файла: StepCounter_backup_2026-10-16.json */
    fun fileName(today: LocalDate): String = "StepCounter_backup_$today.json"

    /** Собирает текст файла: метка приложения, версия, настройки и шаги по дням. */
    fun build(history: Map<LocalDate, Int>, settings: UserSettings): String {
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", LocalDateTime.now().toString())

        root.put(
            "settings",
            JSONObject()
                .put("goal", settings.goal)
                .put("heightCm", settings.heightCm)
                .put("sex", settings.sex)
                .put("weightKg", settings.weightKg)
                .put("stepLengthCm", settings.stepLengthCm)
                .put("stepLengthManual", settings.stepLengthManual)
        )

        val days = JSONObject()
        history.entries.sortedBy { it.key.toEpochDay() }.forEach { days.put(it.key.toString(), it.value) }
        root.put("days", days)
        return root.toString(2)
    }

    /**
     * Читает файл. Если это не резервная копия Шагомера — бросает IllegalArgumentException.
     * Некорректные записи отдельных дней пропускаются.
     */
    fun parse(text: String): BackupData {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Not a JSON file")
        }
        require(root.optString("app") == APP_ID) { "Not a StepCounter backup" }
        require(root.optInt("version", 0) in 1..FORMAT_VERSION) { "Unsupported version" }

        // Шаги по дням
        val days = HashMap<LocalDate, Int>()
        root.optJSONObject("days")?.let { obj ->
            obj.keys().forEach { key ->
                val date = runCatching { LocalDate.parse(key) }.getOrNull()
                val steps = obj.optInt(key, -1)
                if (date != null && steps >= 0) days[date] = steps
            }
        }

        // Настройки (значения ограничиваем разумными рамками)
        val settings = root.optJSONObject("settings")?.let { o ->
            val d = UserSettings()
            BackupSettings(
                goal = o.optInt("goal", d.goal).coerceIn(2_000, 30_000),
                heightCm = o.optInt("heightCm", d.heightCm).coerceIn(100, 230),
                sex = if (o.optInt("sex", d.sex) == Sex.FEMALE) Sex.FEMALE else Sex.MALE,
                weightKg = o.optInt("weightKg", d.weightKg).coerceIn(30, 250),
                stepLengthCm = o.optInt("stepLengthCm", d.stepLengthCm).coerceIn(30, 130),
                stepLengthManual = o.optBoolean("stepLengthManual", false),
            )
        }
        require(days.isNotEmpty() || settings != null) { "Empty backup" }
        return BackupData(days, settings)
    }
}

package com.example.stepcounter.data

import android.content.Context
import android.net.Uri
import com.example.stepcounter.util.RU
import java.io.IOException
import java.time.LocalDate

/** Экспорт статистики в файл CSV (таблица, которую открывают Excel и Google Таблицы). */
object CsvExporter {

    /**
     * Собирает текст CSV. Формат рассчитан на русский Excel:
     *  - разделитель столбцов — точка с запятой, дробные числа — через запятую;
     *  - в начало добавлен BOM (\uFEFF), чтобы Excel правильно прочитал кириллицу (UTF-8).
     * Колонка «Цель выполнена» считается по ТЕКУЩЕЙ цели из настроек.
     */
    fun build(history: Map<LocalDate, Int>, settings: UserSettings): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("Дата;Шаги;Дистанция (км);Калории (ккал);Цель выполнена\r\n")
        history.entries
            .sortedBy { it.key.toEpochDay() } // по возрастанию даты
            .forEach { (date, steps) ->
                sb.append(date).append(';')
                    .append(steps).append(';')
                    .append(String.format(RU, "%.2f", settings.distanceKm(steps))).append(';')
                    .append(String.format(RU, "%.0f", settings.calories(steps))).append(';')
                    .append(if (steps >= settings.goal) "да" else "нет")
                    .append("\r\n")
            }
        return sb.toString()
    }

    /** Записывает CSV в файл, который пользователь выбрал в системном окне сохранения ([uri]). */
    fun write(context: Context, uri: Uri, history: Map<LocalDate, Int>, settings: UserSettings) {
        // "wt" — открыть на запись и стереть прежнее содержимое
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(build(history, settings).toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Не удалось открыть файл для записи")
    }
}

package com.example.stepcounter.data

import android.content.Context
import android.net.Uri
import com.example.stepcounter.R
import com.example.stepcounter.util.AppLocale
import com.example.stepcounter.util.withAppLocale
import java.io.IOException
import java.text.DecimalFormatSymbols
import java.time.LocalDate

/** Экспорт статистики в файл CSV (таблица, которую открывают Excel и Google Таблицы). */
object CsvExporter {

    /**
     * Собирает текст CSV. Заголовки и «да/нет» — на языке приложения.
     *  - Разделитель столбцов: там, где дробную часть пишут через запятую (Россия и др.),
     *    Excel ждёт «;», в остальных случаях — «,».
     *  - В начало добавлен BOM (\uFEFF), чтобы Excel правильно прочитал кириллицу (UTF-8).
     * Колонка «Цель выполнена» считается по ТЕКУЩЕЙ цели из настроек.
     */
    fun build(context: Context, history: Map<LocalDate, Int>, settings: UserSettings): String {
        val locale = AppLocale.current
        val ctx = context.withAppLocale() // строки на выбранном языке
        val sep = if (DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',') ';' else ','
        val yes = ctx.getString(R.string.csv_yes)
        val no = ctx.getString(R.string.csv_no)

        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append(
            listOf(
                ctx.getString(R.string.csv_date),
                ctx.getString(R.string.csv_steps),
                ctx.getString(R.string.csv_distance),
                ctx.getString(R.string.csv_calories),
                ctx.getString(R.string.csv_goal),
            ).joinToString(sep.toString())
        ).append("\r\n")

        history.entries
            .sortedBy { it.key.toEpochDay() } // по возрастанию даты
            .forEach { (date, steps) ->
                sb.append(date).append(sep)
                    .append(steps).append(sep)
                    .append(String.format(locale, "%.2f", settings.distanceKm(steps))).append(sep)
                    .append(String.format(locale, "%.0f", settings.calories(steps))).append(sep)
                    .append(if (steps >= settings.goal) yes else no)
                    .append("\r\n")
            }
        return sb.toString()
    }

    /** Записывает CSV в файл, который пользователь выбрал в системном окне сохранения ([uri]). */
    fun write(context: Context, uri: Uri, history: Map<LocalDate, Int>, settings: UserSettings) {
        // "wt" — открыть на запись и стереть прежнее содержимое
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(build(context, history, settings).toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Could not open the file for writing")
    }
}

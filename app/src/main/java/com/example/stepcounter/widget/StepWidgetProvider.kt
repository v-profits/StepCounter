package com.example.stepcounter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.stepcounter.MainActivity
import com.example.stepcounter.R
import com.example.stepcounter.StepCounterApp
import com.example.stepcounter.data.UserSettings
import com.example.stepcounter.util.formatKcal
import com.example.stepcounter.util.formatKm
import com.example.stepcounter.util.ProgressLine
import com.example.stepcounter.util.formatSteps
import com.example.stepcounter.util.withAppLocale

/**
 * Виджет рабочего стола размером 3x1: по виду повторяет уведомление в шторке — шаги, калории
 * и дистанция за сегодня (с иконками) и линия прогресса к цели. Нажатие на виджет открывает приложение.
 *
 * Виджеты рисуются в другом процессе (на рабочем столе), поэтому используется RemoteViews —
 * упрощённый набор обычных (не Compose) View.
 */
class StepWidgetProvider : AppWidgetProvider() {

    /** Система просит обновить виджеты: при добавлении на экран и примерно раз в 30 минут. */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = (context.applicationContext as StepCounterApp).repository
        val views = buildViews(context, repo.stepsFor(), repo.settings.value)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    companion object {

        /** Обновляет все виджеты приложения на рабочем столе (если они там есть). */
        fun updateAll(context: Context, steps: Int, settings: UserSettings) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, StepWidgetProvider::class.java))
                if (ids.isNotEmpty()) {
                    manager.updateAppWidget(ids, buildViews(context, steps, settings))
                }
            } catch (_: Exception) {
                // Виджет — второстепенная функция: ошибка обновления не должна ронять приложение
            }
        }

        /** Собирает внешний вид виджета: значения, полоску прогресса и реакцию на нажатие. */
        private fun buildViews(context: Context, steps: Int, settings: UserSettings): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_steps)
            val ctx = context.withAppLocale() // контекст с выбранным языком — для единиц измерения

            // Значения — как в уведомлении (единицы берутся из строк, поэтому зависят от языка)
            views.setTextViewText(R.id.widget_steps, steps.formatSteps())
            views.setTextViewText(R.id.widget_kcal, ctx.getString(R.string.value_kcal, settings.calories(steps).formatKcal()))
            views.setTextViewText(R.id.widget_km, ctx.getString(R.string.value_km, settings.distanceKm(steps).formatKm()))

            // Линия прогресса под надписями: доля шагов от цели (рисуется картинкой)
            val progress = steps / settings.goal.coerceAtLeast(1).toFloat()
            views.setImageViewBitmap(R.id.widget_progress, ProgressLine.draw(progress))

            // Нажатие на виджет -> открыть приложение
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            return views
        }
    }
}

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
import com.example.stepcounter.util.formatSteps

/**
 * Виджет рабочего стола размером 2x1: шаги, калории и дистанция за сегодня (с иконками)
 * и полоска прогресса к цели. Нажатие на виджет открывает приложение.
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

            // Значения: шаги (и цель под ними), калории, километры
            views.setTextViewText(R.id.widget_steps, steps.formatSteps())
            views.setTextViewText(R.id.widget_goal, "из ${settings.goal.formatSteps()}")
            views.setTextViewText(R.id.widget_kcal, settings.calories(steps).formatKcal())
            views.setTextViewText(R.id.widget_km, settings.distanceKm(steps).formatKm())

            // Полоска прогресса: максимум — цель, текущее значение — шаги (не больше цели)
            views.setProgressBar(R.id.widget_progress, settings.goal, steps.coerceAtMost(settings.goal), false)

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

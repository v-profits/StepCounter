package com.example.stepcounter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.widget.RemoteViews
import com.example.stepcounter.MainActivity
import com.example.stepcounter.R
import com.example.stepcounter.StepCounterApp
import com.example.stepcounter.util.formatSteps

/**
 * Виджет рабочего стола: кольцо прогресса, число шагов за сегодня и цель.
 * Нажатие на виджет открывает приложение.
 *
 * Виджеты рисуются в другом процессе (на рабочем столе), поэтому используется RemoteViews —
 * упрощённый набор обычных (не Compose) View. Кольцо рисуем сами в виде картинки (Bitmap).
 */
class StepWidgetProvider : AppWidgetProvider() {

    /** Система просит обновить виджеты: при добавлении на экран и примерно раз в 30 минут. */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = (context.applicationContext as StepCounterApp).repository
        val views = buildViews(context, repo.stepsFor(), repo.settings.value.goal)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    companion object {

        /** Обновляет все виджеты приложения на рабочем столе (если они там есть). */
        fun updateAll(context: Context, steps: Int, goal: Int) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, StepWidgetProvider::class.java))
                if (ids.isNotEmpty()) {
                    manager.updateAppWidget(ids, buildViews(context, steps, goal))
                }
            } catch (_: Exception) {
                // Виджет — второстепенная функция: ошибка обновления не должна ронять приложение
            }
        }

        /** Собирает внешний вид виджета: кольцо, тексты и реакцию на нажатие. */
        private fun buildViews(context: Context, steps: Int, goal: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_steps)

            // Картинка кольца + тексты
            val progress = steps / goal.coerceAtLeast(1).toFloat()
            views.setImageViewBitmap(R.id.widget_ring, drawRing(progress))
            views.setTextViewText(R.id.widget_steps, steps.formatSteps())
            views.setTextViewText(R.id.widget_goal, "из ${goal.formatSteps()}")

            // Нажатие на виджет -> открыть приложение
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            return views
        }

        /** Рисует кольцо прогресса (0..1) в квадратную картинку с прозрачным фоном. */
        private fun drawRing(progress: Float): Bitmap {
            val size = 320                 // размер картинки в пикселях
            val stroke = 30f               // толщина кольца
            val center = size / 2f
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Прямоугольник, в который вписано кольцо (с отступом на толщину линии)
            val rect = RectF(stroke / 2, stroke / 2, size - stroke / 2, size - stroke / 2)

            // 1) Серая полупрозрачная дорожка — подходит и к светлому, и к тёмному фону
            val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = 0x33808080
            }
            canvas.drawArc(rect, 0f, 360f, false, track)

            // 2) Цветная дуга прогресса с градиентом (как в приложении)
            val p = progress.coerceIn(0f, 1f)
            if (p > 0.001f) {
                val gradient = SweepGradient(
                    center, center,
                    intArrayOf(Color.parseColor("#2FB4E0"), Color.parseColor("#0B8F6E"), Color.parseColor("#2FB4E0")),
                    null,
                ).apply {
                    // Градиент по умолчанию начинается справа; поворачиваем, чтобы начинался сверху
                    setLocalMatrix(Matrix().apply { postRotate(-90f, center, center) })
                }
                val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                    strokeCap = Paint.Cap.ROUND // скруглённые концы
                    shader = gradient
                }
                canvas.drawArc(rect, -90f, 360f * p, false, arc) // старт сверху (-90°)
            }
            return bitmap
        }
    }
}

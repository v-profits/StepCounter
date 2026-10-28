package com.semenenko.stepcounter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import com.semenenko.stepcounter.MainActivity
import com.semenenko.stepcounter.R
import com.semenenko.stepcounter.StepCounterApp
import com.semenenko.stepcounter.data.UserSettings
import com.semenenko.stepcounter.util.ProgressLine
import com.semenenko.stepcounter.util.formatSteps
import com.semenenko.stepcounter.util.withAppLocale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Виджет рабочего стола размером 2x2. Повторяет верхнюю карточку вкладки «Сегодня» в приложении:
 * кольцо прогресса, внутри — значок шагов, число шагов и надпись «из N шагов».
 * Все размеры (толщина кольца, значок, шрифты) берутся в тех же пропорциях, что в приложении,
 * и подстраиваются под фактический размер виджета.
 * Нажатие на виджет открывает приложение.
 *
 * Виджеты рисуются в другом процессе (на рабочем столе), поэтому используется RemoteViews —
 * упрощённый набор обычных (не Compose) View. Кольцо и значок рисуем сами в виде картинок.
 */
class StepWidgetProvider : AppWidgetProvider() {

    /** Система просит обновить виджеты: при добавлении на экран и примерно раз в 30 минут. */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = (context.applicationContext as StepCounterApp).repository
        val steps = repo.stepsFor()
        val goal = repo.settings.value.goal
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, appWidgetManager, id, steps, goal))
        }
    }

    /** Пользователь изменил размер виджета — перерисовываем под новый размер. */
    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle,
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {

        // Размеры на экране «Сегодня» (в dp / sp): по ним считаем пропорции виджета
        private const val APP_RING_DP = 264f    // диаметр кольца
        private const val APP_STROKE_DP = 22f   // толщина кольца
        private const val APP_ICON_DP = 32f     // значок шагов
        private const val APP_STEPS_SP = 50f    // число шагов
        private const val APP_GOAL_SP = 14f     // надпись «из N шагов»

        /** Диаметр кольца относительно меньшей стороны виджета (остальное — поля). */
        private const val RING_FRACTION = 0.88f

        /** Ограничение размера картинки кольца в пикселях (лимит памяти для виджетов). */
        private const val MAX_PIXELS = 700_000f

        /** Обновляет все виджеты приложения на рабочем столе (если они там есть). */
        fun updateAll(context: Context, steps: Int, settings: UserSettings) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, StepWidgetProvider::class.java))
                for (id in ids) {
                    manager.updateAppWidget(id, buildViews(context, manager, id, steps, settings.goal))
                }
            } catch (_: Exception) {
                // Виджет — второстепенная функция: ошибка обновления не должна ронять приложение
            }
        }

        /** Собирает внешний вид виджета: кольцо, значок, число шагов, цель и реакцию на нажатие. */
        private fun buildViews(context: Context, manager: AppWidgetManager, id: Int, steps: Int, goal: Int): RemoteViews {
            val ctx = context.withAppLocale() // контекст с выбранным языком — для надписи «из N шагов»
            val density = context.resources.displayMetrics.density
            val (widthDp, heightDp) = widgetSizeDp(context, manager, id)
            val sideDp = min(widthDp, heightDp) // кольцо вписываем в меньшую сторону виджета

            // Диаметр кольца и коэффициент k: во сколько раз виджет меньше «эталона» (кольцо 264dp в приложении)
            val ringDp = sideDp * RING_FRACTION
            val k = ringDp / APP_RING_DP

            val views = RemoteViews(context.packageName, R.layout.widget_steps)

            // Кольцо прогресса (квадратная картинка, ImageView вписывает её без искажений)
            val progress = steps / goal.coerceAtLeast(1).toFloat()
            views.setImageViewBitmap(R.id.widget_progress, drawRing(sideDp, ringDp, k, density, progress))

            // Значок шагов, число шагов и подпись с целью — в тех же пропорциях, что в приложении
            views.setImageViewBitmap(R.id.widget_icon, drawIcon(ctx, (APP_ICON_DP * k * density).roundToInt()))
            views.setTextViewText(R.id.widget_steps, steps.formatSteps())
            views.setTextViewTextSize(R.id.widget_steps, TypedValue.COMPLEX_UNIT_PX, APP_STEPS_SP * k * density)
            views.setTextViewText(R.id.widget_goal, ctx.getString(R.string.today_of_goal, goal.formatSteps()))
            views.setTextViewTextSize(R.id.widget_goal, TypedValue.COMPLEX_UNIT_PX, APP_GOAL_SP * k * density)

            // Нажатие на виджет -> открыть приложение
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            return views
        }

        /** Размер виджета в dp по данным лаунчера (в вертикальной и горизонтальной ориентации они разные). */
        private fun widgetSizeDp(context: Context, manager: AppWidgetManager, id: Int): Pair<Int, Int> {
            val o = manager.getAppWidgetOptions(id)
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val w = o.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            val h = o.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            // Если лаунчер размер не сообщил — берём типичный размер 2x2
            return (if (w > 0) w else 160) to (if (h > 0) h else 160)
        }

        /**
         * Рисует кольцо прогресса так же, как на экране «Сегодня»: серая дорожка по кругу и поверх неё
         * голубая дуга с закруглёнными концами, начинающаяся сверху и идущая по часовой стрелке.
         * Картинка квадратная со стороной [sideDp]; кольцо — по центру.
         */
        private fun drawRing(sideDp: Int, ringDp: Float, k: Float, density: Float, progress: Float): Bitmap {
            // Если картинка получается слишком большой — уменьшаем её (ImageView растянет обратно)
            var scale = 1f
            val area = sideDp * density * sideDp * density
            if (area > MAX_PIXELS) scale = sqrt(MAX_PIXELS / area)
            val unit = density * scale                          // пикселей в картинке на 1dp
            val sizePx = max(80, (sideDp * unit).roundToInt())

            val stroke = max(3f, APP_STROKE_DP * k * unit)      // толщина кольца (22dp в приложении)
            val diameter = ringDp * unit
            val center = sizePx / 2f
            // Кольцо рисуется по оси линии, поэтому отступаем на половину толщины
            val rect = RectF(
                center - diameter / 2f + stroke / 2f, center - diameter / 2f + stroke / 2f,
                center + diameter / 2f - stroke / 2f, center + diameter / 2f - stroke / 2f,
            )

            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1) Серая дорожка по всему кругу
            val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = ProgressLine.TRACK
            }
            canvas.drawArc(rect, 0f, 360f, false, track)

            // 2) Дуга прогресса сплошным голубым цветом (старт сверху: -90°)
            val p = progress.coerceIn(0f, 1f)
            if (p > 0.002f) {
                val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                    strokeCap = Paint.Cap.ROUND
                    color = ProgressLine.BLUE
                }
                canvas.drawArc(rect, -90f, 360f * p, false, arc)
            }
            return bitmap
        }

        /** Рисует значок шагов (тот же, что в приложении) голубым цветом в квадрат [sizePx] пикселей. */
        private fun drawIcon(context: Context, sizePx: Int): Bitmap {
            val px = max(8, sizePx)
            val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            ResourcesCompat.getDrawable(context.resources, R.drawable.ic_steps, null)?.mutate()?.let { icon ->
                icon.setTint(ProgressLine.BLUE)
                icon.setBounds(0, 0, px, px)
                icon.draw(Canvas(bitmap))
            }
            return bitmap
        }
    }
}

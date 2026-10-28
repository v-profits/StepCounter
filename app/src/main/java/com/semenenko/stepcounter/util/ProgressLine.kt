package com.semenenko.stepcounter.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max

/**
 * Рисует тонкую линию прогресса (скруглённую, одного цвета) в виде картинки.
 * Используется в уведомлении: обычный ProgressBar в таких «удалённых» View
 * на разных телефонах рисуется по-разному (или не виден), а картинка выглядит везде одинаково.
 */
object ProgressLine {

    /** Голубой цвет прогресса (одинаково хорошо виден на светлом и тёмном фоне). */
    val BLUE: Int = Color.parseColor("#2F9DEA")

    /** Серая полупрозрачная дорожка — подходит и к светлому, и к тёмному фону. */
    const val TRACK: Int = 0x33808080

    /**
     * [progress] — доля от 0 до 1 (больше 1 обрезается).
     * Серая дорожка рисуется всегда, цветная заливка — поверх неё.
     */
    fun draw(progress: Float, width: Int = 720, height: Int = 16): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = h / 2f // полностью скруглённые концы

        // 1) Дорожка
        canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TRACK })

        // 2) Заливка сплошным голубым цветом
        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            // Минимальная длина — «капля» шириной в высоту линии, чтобы малый прогресс был заметен
            val fillWidth = max(h, w * p)
            canvas.drawRoundRect(
                RectF(0f, 0f, fillWidth, h), radius, radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BLUE },
            )
        }
        return bitmap
    }
}

package com.example.stepcounter.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max

/**
 * Рисует тонкую линию прогресса (скруглённую, с градиентом) в виде картинки.
 * Используется и в уведомлении, и в виджете: обычный ProgressBar в таких «удалённых» View
 * на разных телефонах рисуется по-разному (или не виден), а картинка выглядит везде одинаково.
 */
object ProgressLine {

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

        // 1) Серая полупрозрачная дорожка — подходит и к светлому, и к тёмному фону
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33808080 }
        canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, track)

        // 2) Цветная заливка с градиентом (от голубого к зелёному)
        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, w, 0f,
                    Color.parseColor("#2FB4E0"), Color.parseColor("#1DB98F"),
                    Shader.TileMode.CLAMP,
                )
            }
            // Минимальная длина — «капля» шириной в высоту линии, чтобы малый прогресс был заметен
            val fillWidth = max(h, w * p)
            canvas.drawRoundRect(RectF(0f, 0f, fillWidth, h), radius, radius, fill)
        }
        return bitmap
    }
}

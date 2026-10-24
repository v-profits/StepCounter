package com.example.stepcounter.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepcounter.ui.theme.AppColors
import kotlin.math.max

/**
 * Кольцо прогресса с градиентом и плавной анимацией.
 * [progress] — от 0 до 1 (больше 1 обрезается), [content] — то, что рисуется внутри кольца.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    // Плавно «доезжаем» до нового значения прогресса за 0,9 секунды
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ring",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant // цвет серой дорожки
    val progressColor = AppColors.progress // сплошной голубой цвет прогресса

    Box(modifier, contentAlignment = Alignment.Center) {
        // Canvas — холст для ручного рисования
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx() // толщина линии в пикселях
            // Отступ, чтобы толстая линия не выходила за границы холста
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            // 1) Рисуем полное серое кольцо-дорожку
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke),
            )
            // 2) Поверх рисуем цветную дугу прогресса (если он не нулевой)
            if (animated > 0.001f) {
                // Рисуем дугу сплошным цветом; начинается сверху (-90° — это 12 часов)
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animated, // длина дуги пропорциональна прогрессу
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content() // текст и иконка в центре кольца
    }
}

/** Один столбец диаграммы. */
data class BarEntry(
    val label: String,                // подпись под столбцом
    val value: Int,                   // высота (шаги)
    val selected: Boolean = false,    // выделен ли столбец (сегодня / нажатый)
    val showLabel: Boolean = true,    // показывать ли подпись (в месяце — не у всех)
)

/**
 * Столбчатая диаграмма с пунктирной линией цели.
 * Столбцы, достигшие цели, закрашены градиентом, остальные — бледные.
 * [onBarClick] — вызывается с номером нажатого столбца (если null — столбцы не кликабельны).
 */
@Composable
fun BarChart(
    entries: List<BarEntry>,
    goal: Int,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    onBarClick: ((Int) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    // Верхняя граница шкалы: самый высокий столбец или цель + 15% запаса сверху
    val maxValue = max(entries.maxOfOrNull { it.value } ?: 0, goal).coerceAtLeast(1) * 1.15f
    val dense = entries.size > 11                  // «плотный» режим (месяц, год): тонкие столбцы
    val gap = if (dense) 2.dp else 8.dp            // промежуток между столбцами
    val radius = if (dense) 3.dp else 8.dp         // скругление верхушек
    // Градиент для столбцов, достигших цели
    val progressColor = AppColors.progress
    val dim = progressColor.copy(alpha = 0.28f)  // бледный цвет столбцов без достигнутой цели
    val lineColor = cs.secondary

    Column(modifier) {
        // Область графика: столбцы + поверх них линия цели
        Box(Modifier.fillMaxWidth().height(chartHeight)) {
            // Ряд столбцов; каждый занимает равную долю ширины (weight = 1)
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Bottom,
            ) {
                entries.forEachIndexed { i, e ->
                    // Анимированная высота столбца (доля от высоты графика)
                    val fraction by animateFloatAsState(
                        targetValue = e.value / maxValue,
                        animationSpec = tween(600),
                        label = "bar",
                    )
                    // Выбор цвета: цель достигнута -> градиент, выделен -> средний, иначе -> бледный
                    val brush: Brush = when {
                        e.value >= goal -> SolidColor(progressColor)
                        e.selected -> SolidColor(progressColor.copy(alpha = 0.6f))
                        else -> SolidColor(dim)
                    }
                    // Если задан обработчик — делаем столбец кликабельным (без ряби)
                    val clickMod = if (onBarClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onBarClick(i) }
                    } else Modifier
                    Box(
                        Modifier.weight(1f).fillMaxHeight().then(clickMod),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceIn(0.01f, 1f))
                                .clip(RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(brush)
                        )
                    }
                }
            }
            // Пунктирная горизонтальная линия на уровне цели
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - goal / maxValue) // координата Y линии цели
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Ряд подписей под столбцами (ширина совпадает со столбцами)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            entries.forEach { e ->
                Box(Modifier.weight(1f)) {
                    if (e.showLabel) {
                        Text(
                            text = e.label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            fontWeight = if (e.selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (e.selected) progressColor else cs.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center).wrapContentWidth(unbounded = true),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Небольшая цветная карточка с иконкой, значением и подписью
 * (например: иконка, «5,4», «км», «Дистанция»).
 */
@Composable
fun StatCard(
    painter: Painter,
    value: String,
    unit: String,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(painter, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text(unit, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.75f))
        }
    }
}

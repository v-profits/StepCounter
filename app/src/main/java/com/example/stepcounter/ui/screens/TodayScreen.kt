package com.example.stepcounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Map as MapIcon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepcounter.R
import com.example.stepcounter.data.UserSettings
import com.example.stepcounter.data.currentStreak
import com.example.stepcounter.data.periodDays
import com.example.stepcounter.ui.components.BarChart
import com.example.stepcounter.ui.components.BarEntry
import com.example.stepcounter.ui.components.ProgressRing
import com.example.stepcounter.ui.components.StatCard
import com.example.stepcounter.util.formatKcal
import com.example.stepcounter.util.formatKm
import com.example.stepcounter.util.formatSteps
import com.example.stepcounter.util.fullTitle
import com.example.stepcounter.util.shortWeekday
import java.time.LocalDate

/** Вкладка «Сегодня»: кольцо прогресса, карточки показателей и график за неделю. */
@Composable
fun TodayScreen(
    history: Map<LocalDate, Int>,
    settings: UserSettings,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val steps = history[today] ?: 0              // шаги сегодня
    val goal = settings.goal                     // дневная цель
    val progress = steps / goal.toFloat()        // прогресс от 0 до 1+ (кольцо само обрезает)
    // remember(ключи) — пересчитываем только когда ключи изменились, а не при каждой перерисовке
    val streak = remember(history, today, goal) { currentStreak(history, today, goal) }
    val week = remember(history, today) { periodDays(history, today, 7) } // последние 7 дней

    // Вся вкладка — вертикальная прокручиваемая колонка
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Заголовок и дата
        Column(Modifier.padding(top = 12.dp)) {
            Text("Сегодня", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(today.fullTitle(), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        }

        // Большая карточка с кольцом прогресса
        Card(
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Кольцо; внутри — иконка, число шагов и подпись «из N шагов»
                ProgressRing(progress, Modifier.size(264.dp), strokeWidth = 22.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painterResource(R.drawable.ic_steps),
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            steps.formatSteps(),
                            fontSize = 50.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                        Text(
                            "из ${goal.formatSteps()} шагов",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                // Плашка с процентом выполнения цели
                Surface(shape = CircleShape, color = cs.primaryContainer) {
                    Text(
                        "${(progress * 100).toInt()}% от цели",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(10.dp))
                // Мотивирующий текст: цель выполнена или сколько осталось
                Text(
                    text = if (steps >= goal) "Цель выполнена — так держать! 🎉"
                    else "Осталось ${(goal - steps).formatSteps()} шагов до цели",
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Ряд из трёх карточек: дистанция, калории, серия дней
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                painter = rememberVectorPainter(Icons.Rounded.MapIcon),
                value = settings.distanceKm(steps).formatKm(),
                unit = "км",
                label = "Дистанция",
                container = cs.primaryContainer,
                content = cs.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                painter = rememberVectorPainter(Icons.Rounded.LocalFireDepartment),
                value = settings.calories(steps).formatKcal(),
                unit = "ккал",
                label = "Калории",
                container = cs.secondaryContainer,
                content = cs.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                painter = rememberVectorPainter(Icons.Rounded.EmojiEvents),
                value = streak.toString(),
                unit = "дн.",
                label = "Серия",
                container = cs.tertiaryContainer,
                content = cs.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
        }

        // Карточка с графиком за последние 7 дней (сегодняшний столбец выделен)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Последние 7 дней", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Пунктир — ваша цель",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                BarChart(
                    entries = week.map {
                        BarEntry(label = it.date.shortWeekday(), value = it.steps, selected = it.date == today)
                    },
                    goal = goal,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

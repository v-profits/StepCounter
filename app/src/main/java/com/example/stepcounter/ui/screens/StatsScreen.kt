package com.example.stepcounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.stepcounter.data.UserSettings
import com.example.stepcounter.data.periodDays
import com.example.stepcounter.data.periodMonths
import com.example.stepcounter.data.summarize
import com.example.stepcounter.ui.components.BarChart
import com.example.stepcounter.ui.components.BarEntry
import com.example.stepcounter.util.dayMonth
import com.example.stepcounter.util.formatKcal
import com.example.stepcounter.util.formatKm
import com.example.stepcounter.util.formatSteps
import com.example.stepcounter.util.fullName
import com.example.stepcounter.util.fullTitle
import com.example.stepcounter.util.shortName
import com.example.stepcounter.util.shortWeekday
import java.time.LocalDate
import java.time.YearMonth

/**
 * Вкладка «Статистика»: неделя / месяц / год, график, итоги и история.
 * В режимах «Неделя» и «Месяц» столбцы — это дни, в режиме «Год» — месяцы (среднее число шагов в день).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    history: Map<LocalDate, Int>,
    settings: UserSettings,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val goal = settings.goal

    var period by rememberSaveable { mutableIntStateOf(0) } // 0 = неделя, 1 = месяц, 2 = год
    val length = when (period) { 0 -> 7; 1 -> 30; else -> 365 } // сколько дней входит в период
    val isYear = period == 2

    val days = remember(history, today, length) { periodDays(history, today, length) } // дни периода
    // Самая ранняя дата, за которую есть запись
    val first = remember(history) { history.keys.minByOrNull { it.toEpochDay() } }
    // Итоги за период (всего, среднее, лучший день...)
    val summary = remember(days, first, goal) { summarize(days, first, goal) }
    // Последние 12 месяцев — нужны для режима «Год»
    val months = remember(history, today, first) { periodMonths(history, today, first, 12) }

    // Выбранный нажатием день (неделя/месяц) и выбранный месяц (год). По умолчанию — сегодня / текущий месяц
    var selectedDay by remember(period, today) { mutableStateOf(today) }
    var selectedMonth by remember(today) { mutableStateOf(YearMonth.from(today)) }

    // Данные для карточки с подробностями выбранного дня или месяца
    val detailTitle: String
    val detailSteps: Int
    val detailSub: String?
    if (isYear) {
        val m = months.firstOrNull { it.month == selectedMonth } ?: months.last()
        detailTitle = m.month.fullName()
        detailSteps = m.total
        detailSub = "в среднем ${m.average.formatSteps()} в день"
    } else {
        detailTitle = selectedDay.fullTitle()
        detailSteps = history[selectedDay] ?: 0
        detailSub = null
    }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Статистика",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )

        // Переключатель «Неделя / Месяц / Год»
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Неделя", "Месяц", "Год").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = period == i,
                    onClick = { period = i },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 3),
                ) { Text(label) }
            }
        }

        // Карточка с подробностями выбранного дня/месяца: шаги, километры, калории
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = cs.primaryContainer,
                contentColor = cs.onPrimaryContainer,
            ),
        ) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(detailTitle, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${detailSteps.formatSteps()} шагов",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (detailSub != null) {
                        Text(detailSub, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${settings.distanceKm(detailSteps).formatKm()} км", style = MaterialTheme.typography.titleMedium)
                    Text("${settings.calories(detailSteps).formatKcal()} ккал", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Карточка с графиком; нажатие на столбец меняет выбранный день/месяц
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (isYear) "Среднее в день по месяцам" else "Шаги по дням",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Нажмите на столбец, чтобы увидеть подробности · пунктир — цель",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                // Превращаем данные в столбцы
                val entries = if (isYear) {
                    // Год: столбец = месяц, высота = среднее число шагов в день
                    months.map { BarEntry(label = it.month.shortName(), value = it.average, selected = it.month == selectedMonth) }
                } else {
                    // Неделя/месяц: столбец = день (в месяце подписываем каждый 5-й день)
                    days.mapIndexed { i, d ->
                        BarEntry(
                            label = if (length == 7) d.date.shortWeekday() else d.date.dayOfMonth.toString(),
                            value = d.steps,
                            selected = d.date == selectedDay,
                            showLabel = length == 7 || (length - 1 - i) % 5 == 0,
                        )
                    }
                }
                BarChart(
                    entries = entries,
                    goal = goal,
                    chartHeight = 200.dp,
                    onBarClick = { i ->
                        if (isYear) selectedMonth = months[i].month else selectedDay = days[i].date
                    },
                )
            }
        }

        // Итоги за период — две строки по две карточки одинаковой высоты
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                "Всего шагов", summary.total.formatSteps(),
                "≈ ${settings.distanceKm(summary.total).formatKm()} км",
                Modifier.weight(1f).fillMaxHeight(),
            )
            SummaryCard(
                "В среднем в день", summary.average.formatSteps(),
                "цель ${goal.formatSteps()}",
                Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                "Лучший день", summary.best?.steps?.formatSteps() ?: "—",
                summary.best?.date?.dayMonth() ?: "нет данных",
                Modifier.weight(1f).fillMaxHeight(),
            )
            SummaryCard(
                "Цель выполнена", "${summary.goalDays} дн.",
                if (summary.countedDays > 0) "из ${summary.countedDays}" else "нет данных",
                Modifier.weight(1f).fillMaxHeight(),
            )
        }

        // Список: по дням (неделя/месяц) или по месяцам (год); новые сверху
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    if (isYear) "По месяцам" else "История",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val hasData = first != null
                if (!hasData) {
                    Text(
                        "Данных пока нет — пройдитесь, и здесь появится статистика.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (isYear) {
                    // Строка на каждый месяц: полоска показывает среднее в день относительно цели
                    months.reversed().filter { it.daysCounted > 0 }.forEach { m ->
                        ProgressRow(
                            label = "${m.month.shortName()} ${m.month.year}",
                            labelWidth = 84.dp,
                            fraction = m.average / goal.toFloat(),
                            valueText = m.total.formatSteps(),
                        )
                    }
                } else {
                    // Строка на каждый день с начала записей
                    days.reversed().filter { first != null && !it.date.isBefore(first) }.forEach { d ->
                        ProgressRow(
                            label = "${d.date.shortWeekday()}, ${d.date.dayMonth()}",
                            labelWidth = 100.dp,
                            fraction = d.steps / goal.toFloat(),
                            valueText = d.steps.formatSteps(),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Строка списка: подпись слева, полоска прогресса посередине, значение справа. */
@Composable
private fun ProgressRow(label: String, labelWidth: Dp, fraction: Float, valueText: String) {
    val cs = MaterialTheme.colorScheme
    // Градиент заполненной части полоски
    val reached = remember(cs.primary, cs.tertiary) { Brush.horizontalGradient(listOf(cs.tertiary, cs.primary)) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(labelWidth))
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(cs.surfaceVariant)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f)) // доля от цели (не больше 100%)
                    .background(reached)
            )
        }
        Text(
            valueText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(72.dp),
        )
    }
}

/** Карточка итога: заголовок, крупное значение и подпись внизу. */
@Composable
private fun SummaryCard(title: String, value: String, sub: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = cs.primary)
        }
    }
}

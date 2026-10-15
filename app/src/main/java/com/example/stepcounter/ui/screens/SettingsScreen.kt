package com.example.stepcounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stepcounter.data.UserSettings
import com.example.stepcounter.util.formatSteps
import kotlin.math.roundToInt

/**
 * Вкладка «Настройки». Сама ничего не хранит — получает текущие [settings]
 * и сообщает наверх об изменениях через onGoal / onStepLength / onWeight и т.д.
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onGoal: (Int) -> Unit,
    onStepLength: (Int) -> Unit,
    onWeight: (Int) -> Unit,
    onDemo: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    var confirmReset by remember { mutableStateOf(false) } // показывать ли окно подтверждения удаления

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )

        // Карточка с тремя ползунками
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSlider("Дневная цель", "шагов", settings.goal, 2_000, 30_000, 500, onGoal)
                SettingSlider("Длина шага", "см", settings.stepLengthCm, 40, 110, 1, onStepLength)
                SettingSlider("Вес", "кг", settings.weightKg, 40, 150, 1, onWeight)
                Text(
                    "Длина шага и вес нужны для расчёта дистанции и калорий — это приблизительные оценки.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }

        // Информационная карточка «Как это работает»
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Как это работает", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Шаги считает встроенный датчик телефона. Приложение работает в фоне и показывает " +
                        "уведомление с прогрессом — так шаги правильно распределяются по дням, " +
                        "а после перезагрузки телефона подсчёт возобновляется сам.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }
        }

        // Карточка с действиями над данными: демо-данные и очистка
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Данные", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FilledTonalButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                    Text("Заполнить демо-данными")
                }
                Text(
                    "Добавит случайные шаги за прошлые 29 дней — удобно, чтобы посмотреть интерфейс на эмуляторе.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Удалить всю историю", color = cs.error)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // Окно подтверждения удаления истории (показывается, пока confirmReset = true)
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Удалить историю?") },
            text = { Text("Все сохранённые шаги будут удалены. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) {
                    Text("Удалить", color = cs.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }
}

/**
 * Ползунок с заголовком и текущим значением.
 * Пока пользователь двигает ползунок, значение хранится локально ([local]),
 * а в настройки записывается только когда он отпустил палец (onValueChangeFinished).
 * [step] — шаг округления (например, цель меняется по 500).
 */
@Composable
private fun SettingSlider(
    title: String,
    unit: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onCommit: (Int) -> Unit,
) {
    // Временное значение во время перетаскивания; сбрасывается, когда меняется сохранённое [value]
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    // Значение, округлённое до шага и ограниченное диапазоном — именно оно показывается и сохраняется
    val shown = ((local / step).roundToInt() * step).coerceIn(min, max)
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "${shown.formatSteps()} $unit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(shown) },
            valueRange = min.toFloat()..max.toFloat(),
        )
    }
}

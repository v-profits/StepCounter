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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stepcounter.AppConfig
import com.example.stepcounter.R
import com.example.stepcounter.data.UserSettings
import com.example.stepcounter.util.formatSteps
import kotlin.math.roundToInt

/**
 * Вкладка «Настройки». Сама ничего не хранит — получает текущие [settings]
 * и сообщает наверх об изменениях через колбэки (onGoal, onHeight, onSex и т.д.).
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onGoal: (Int) -> Unit,
    onHeight: (Int) -> Unit,
    onSex: (Int) -> Unit,
    onWeight: (Int) -> Unit,
    onStepLengthManual: (Boolean) -> Unit,
    onStepLength: (Int) -> Unit,
    onTheme: (Int) -> Unit,
    onLanguage: (String) -> Unit,
    onExport: () -> Unit,
    onSaveBackup: () -> Unit,
    onLoadBackup: () -> Unit,
    onResetToday: () -> Unit,
    onDemo: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    var confirmReset by remember { mutableStateOf(false) }       // окно подтверждения «удалить всю историю»
    var confirmResetToday by remember { mutableStateOf(false) }  // окно подтверждения «обнулить сегодня»
    var showGuide by remember { mutableStateOf(false) }          // окно с руководством пользователя
    val uriHandler = LocalUriHandler.current                     // умеет открывать ссылки в браузере

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )

        // Карточка «Цель и профиль»: цель, рост, пол, вес
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SettingSlider(stringResource(R.string.set_goal), stringResource(R.string.unit_steps), settings.goal, 2_000, 30_000, 500, onGoal)
                SettingSlider(stringResource(R.string.set_height), stringResource(R.string.unit_cm), settings.heightCm, 120, 220, 1, onHeight)
                // Пол: 0 — мужской, 1 — женский (совпадает с Sex)
                Text(stringResource(R.string.set_sex), style = MaterialTheme.typography.titleSmall)
                OptionRow(
                    options = listOf(stringResource(R.string.sex_male), stringResource(R.string.sex_female)),
                    selected = settings.sex,
                    onSelect = onSex,
                )
                SettingSlider(stringResource(R.string.set_weight), stringResource(R.string.unit_kg), settings.weightKg, 40, 150, 1, onWeight)
                Text(
                    stringResource(R.string.set_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }

        // Карточка «Длина шага»: здесь же виден расчёт по росту и полу и можно ввести значение вручную
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.set_step_length), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                // Результат автоматического расчёта (виден всегда)
                Text(
                    stringResource(R.string.step_auto_result, settings.autoStepLengthCm),
                    style = MaterialTheme.typography.bodyMedium,
                )
                // Какое значение используется сейчас
                Text(
                    stringResource(R.string.step_in_use, settings.effectiveStepLengthCm),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.primary,
                )
                // Переключатель ручного ввода
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.step_manual_switch), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = settings.stepLengthManual, onCheckedChange = onStepLengthManual)
                }
                // Ползунок ручного ввода (начинается с рассчитанного значения)
                if (settings.stepLengthManual) {
                    SettingSlider(stringResource(R.string.set_step_length), stringResource(R.string.unit_cm), settings.stepLengthCm, 30, 130, 1, onStepLength)
                    Text(
                        stringResource(R.string.step_manual_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }

        // Карточка «Оформление»: тема (светлая/тёмная) и язык приложения
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                // Тема: 0 — как в системе, 1 — светлая, 2 — тёмная (совпадает с ThemeMode)
                Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleSmall)
                OptionRow(
                    options = listOf(
                        stringResource(R.string.opt_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark),
                    ),
                    selected = settings.themeMode,
                    onSelect = onTheme,
                )

                // Язык: "" — как в системе, иначе код языка
                Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleSmall)
                val languageTags = listOf("", "en", "ru")
                OptionRow(
                    options = listOf(
                        stringResource(R.string.opt_system),
                        stringResource(R.string.lang_en),
                        stringResource(R.string.lang_ru),
                    ),
                    selected = languageTags.indexOf(settings.language).coerceAtLeast(0),
                    onSelect = { onLanguage(languageTags[it]) },
                )
            }
        }

        // Карточка «Поддержать разработчика» — показывается, только если в AppConfig указана ссылка
        if (AppConfig.SUPPORT_URL.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cs.primaryContainer,
                    contentColor = cs.onPrimaryContainer,
                ),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.support_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.support_text), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = {
                            // Открываем страницу в браузере; если браузера нет — просто ничего не происходит
                            runCatching { uriHandler.openUri(AppConfig.SUPPORT_URL) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.support_button)) }
                }
            }
        }

        // Карточка «Руководство»: открывает окно с описанием возможностей приложения
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.help_card_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.help_card_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
                FilledTonalButton(onClick = { showGuide = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.help_btn))
                }
            }
        }

        // Карточка с действиями над данными: резервная копия, CSV, демо-данные, очистка
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.data_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                // Сохранить статистику в файл (в выбранную папку) — для переноса на другой телефон
                FilledTonalButton(onClick = onSaveBackup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_save_backup))
                }
                Text(stringResource(R.string.save_hint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)

                // Загрузить статистику из ранее сохранённого файла
                FilledTonalButton(onClick = onLoadBackup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_load_backup))
                }
                Text(stringResource(R.string.load_hint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)

                // Экспорт в таблицу CSV (откроется в Excel / Google Таблицах)
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_export))
                }
                Text(stringResource(R.string.export_hint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)

                OutlinedButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_demo))
                }
                Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)

                // Обнулить шаги за сегодня (если датчик или подсчёт ошиблись)
                OutlinedButton(onClick = { confirmResetToday = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_reset_today))
                }
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_delete_all), color = cs.error)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // Окно с руководством: по разделам объясняет, что где находится и как работает
    if (showGuide) {
        val sections = listOf(
            R.string.help_1_title to R.string.help_1_body,
            R.string.help_2_title to R.string.help_2_body,
            R.string.help_3_title to R.string.help_3_body,
            R.string.help_4_title to R.string.help_4_body,
            R.string.help_5_title to R.string.help_5_body,
            R.string.help_6_title to R.string.help_6_body,
            R.string.help_7_title to R.string.help_7_body,
        )
        AlertDialog(
            onDismissRequest = { showGuide = false },
            title = { Text(stringResource(R.string.help_title)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    sections.forEach { (title, body) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.primary,
                            )
                            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGuide = false }) { Text(stringResource(R.string.help_close)) } },
        )
    }

    // Окно подтверждения «обнулить шаги за сегодня»
    if (confirmResetToday) {
        AlertDialog(
            onDismissRequest = { confirmResetToday = false },
            title = { Text(stringResource(R.string.dlg_reset_today_title)) },
            text = { Text(stringResource(R.string.dlg_reset_today_text)) },
            confirmButton = {
                TextButton(onClick = { confirmResetToday = false; onResetToday() }) {
                    Text(stringResource(R.string.dlg_reset_today_confirm), color = cs.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmResetToday = false }) { Text(stringResource(R.string.dlg_cancel)) } },
        )
    }

    // Окно подтверждения удаления истории (показывается, пока confirmReset = true)
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.dlg_delete_title)) },
            text = { Text(stringResource(R.string.dlg_delete_text)) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) {
                    Text(stringResource(R.string.dlg_delete_confirm), color = cs.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.dlg_cancel)) } },
        )
    }
}

/** Переключатель из нескольких вариантов в один ряд (например: Как в системе / Светлая / Тёмная). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = selected == i,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
            ) { Text(label, maxLines = 1) }
        }
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

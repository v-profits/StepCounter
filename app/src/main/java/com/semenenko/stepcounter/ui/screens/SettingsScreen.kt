package com.semenenko.stepcounter.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.semenenko.stepcounter.AppConfig
import com.semenenko.stepcounter.R
import com.semenenko.stepcounter.data.UserSettings
import com.semenenko.stepcounter.ui.components.ActionTile
import com.semenenko.stepcounter.ui.components.OptionTiles
import com.semenenko.stepcounter.ui.components.StepperTile
import com.semenenko.stepcounter.ui.theme.AppColors
import com.semenenko.stepcounter.util.formatSteps
import kotlin.math.roundToInt

/**
 * Вкладка «Настройки». Сама ничего не хранит — получает текущие [settings]
 * и сообщает наверх об изменениях через колбэки (onGoal, onHeight, onSex и т.д.).
 * Кнопки сделаны плитками со скруглёнными углами (см. ui/components/Tiles.kt).
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
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                // Цель: ползунок двигается по 500, кнопки «−/+» — по 100
                SettingSlider(stringResource(R.string.set_goal), stringResource(R.string.unit_steps), settings.goal, 2_000, 30_000, 500, 100, onGoal)
                SettingSlider(stringResource(R.string.set_height), stringResource(R.string.unit_cm), settings.heightCm, 120, 220, 1, 1, onHeight)
                // Пол: 0 — мужской, 1 — женский (совпадает с Sex)
                Text(stringResource(R.string.set_sex), style = MaterialTheme.typography.titleSmall)
                OptionTiles(
                    options = listOf(stringResource(R.string.sex_male), stringResource(R.string.sex_female)),
                    selected = settings.sex,
                    onSelect = onSex,
                )
                SettingSlider(stringResource(R.string.set_weight), stringResource(R.string.unit_kg), settings.weightKg, 40, 150, 1, 1, onWeight)
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
                    color = AppColors.progress,
                )
                // Переключатель ручного ввода
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.step_manual_switch), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = settings.stepLengthManual, onCheckedChange = onStepLengthManual)
                }
                // Ползунок ручного ввода (начинается с рассчитанного значения)
                if (settings.stepLengthManual) {
                    SettingSlider(stringResource(R.string.set_step_length), stringResource(R.string.unit_cm), settings.stepLengthCm, 30, 130, 1, 1, onStepLength)
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

                // Тема: 0 — системная, 1 — светлая, 2 — тёмная (совпадает с ThemeMode)
                Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleSmall)
                OptionTiles(
                    options = listOf(
                        stringResource(R.string.opt_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark),
                    ),
                    selected = settings.themeMode,
                    onSelect = onTheme,
                )

                // Язык: "" — системный, иначе код языка
                Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleSmall)
                val languageTags = listOf("", "en", "ru")
                OptionTiles(
                    options = listOf(
                        stringResource(R.string.opt_system_lang),
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
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.support_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.support_text), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    ActionTile(
                        icon = Icons.Rounded.Favorite,
                        title = stringResource(R.string.support_button),
                        onClick = {
                            // Открываем страницу в браузере; если браузера нет — просто ничего не происходит
                            runCatching { uriHandler.openUri(AppConfig.SUPPORT_URL) }
                        },
                    )
                }
            }
        }

        // Плитка «Руководство»: открывает окно с описанием возможностей приложения
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(12.dp)) {
                ActionTile(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.help_btn),
                    subtitle = stringResource(R.string.help_card_text),
                    onClick = { showGuide = true },
                )
            }
        }

        // Карточка с действиями над данными: резервная копия, CSV, обнуление, очистка (плитки)
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.data_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ActionTile(Icons.Rounded.Save, stringResource(R.string.btn_save_backup), onSaveBackup, subtitle = stringResource(R.string.save_hint))
                ActionTile(Icons.Rounded.FolderOpen, stringResource(R.string.btn_load_backup), onLoadBackup, subtitle = stringResource(R.string.load_hint))
                ActionTile(Icons.Rounded.TableChart, stringResource(R.string.btn_export), onExport, subtitle = stringResource(R.string.export_hint))
                ActionTile(Icons.Rounded.Refresh, stringResource(R.string.btn_reset_today), { confirmResetToday = true })
                ActionTile(Icons.Rounded.Delete, stringResource(R.string.btn_delete_all), { confirmReset = true }, danger = true)
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
                                color = AppColors.progress,
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

/**
 * Ползунок с заголовком, текущим значением и плитками «−» и «+» по краям.
 *  - Ползунок — для грубой установки: значение «прилипает» к кратным [step].
 *  - Плитки «−» / «+» — для точной подстройки: один шаг = [fineStep]; если держать палец, значение меняется непрерывно.
 * Значение записывается в настройки, когда пользователь отпустил ползунок или нажал плитку.
 */
@Composable
private fun SettingSlider(
    title: String,
    unit: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    fineStep: Int,
    onCommit: (Int) -> Unit,
) {
    // Текущее значение (во время перетаскивания и нажатий); сбрасывается, когда меняется сохранённое [value]
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val shown = local.roundToInt().coerceIn(min, max)

    // Сдвиг значения на [direction] точных шагов (−1 или +1)
    fun nudge(direction: Int) {
        val next = (local.roundToInt() + direction * fineStep).coerceIn(min, max)
        local = next.toFloat()
        onCommit(next)
    }

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "${shown.formatSteps()} $unit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.progress,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperTile("−", onStep = { nudge(-1) })
            Slider(
                value = local,
                onValueChange = { raw ->
                    // «Прилипание» к кратным step (например, цель двигается по 500)
                    local = ((raw / step).roundToInt() * step).coerceIn(min, max).toFloat()
                },
                onValueChangeFinished = { onCommit(local.roundToInt().coerceIn(min, max)) },
                valueRange = min.toFloat()..max.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.progress,
                    activeTrackColor = AppColors.progress,
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )
            StepperTile("+", onStep = { nudge(1) })
        }
    }
}

package com.example.stepcounter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepcounter.data.BackupData
import com.example.stepcounter.data.BackupManager
import com.example.stepcounter.data.ThemeMode
import com.example.stepcounter.service.StepService
import com.example.stepcounter.ui.MainViewModel
import com.example.stepcounter.ui.screens.SettingsScreen
import com.example.stepcounter.ui.screens.StatsScreen
import com.example.stepcounter.ui.screens.TodayScreen
import com.example.stepcounter.ui.theme.StepCounterTheme
import com.example.stepcounter.util.hasActivityRecognition
import com.example.stepcounter.util.mediumDate
import com.example.stepcounter.util.requiredPermissions
import com.example.stepcounter.util.withAppLocale

/** Единственная Activity приложения. Весь интерфейс рисуется через Compose. */
class MainActivity : ComponentActivity() {

    /** Подставляем язык, выбранный в настройках приложения (если не выбран — язык телефона). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // рисуем под системными панелями (современный вид)
        setContent {       // здесь начинается Compose-интерфейс
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()

            // Тёмная или светлая тема: по выбору пользователя либо как в системе
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            // Подстраиваем цвет значков в строке состояния и панели навигации под выбранную тему
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b),
                    ) { darkTheme },
                )
                onDispose { }
            }

            StepCounterTheme(darkTheme = darkTheme) {
                StepCounterRoot(vm)
            }
        }
    }
}

/**
 * Корневой экран: нижняя навигация, баннеры (нет датчика / нет разрешения)
 * и переключение между вкладками. Также отвечает за запрос разрешений и запуск сервиса.
 */
@Composable
fun StepCounterRoot(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current

    // Подписываемся на «живые» данные; при изменении экран перерисуется сам
    val history by vm.history.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) } // номер выбранной вкладки (сохраняется при повороте)
    var permissionGranted by remember { mutableStateOf(context.hasActivityRecognition()) } // есть ли разрешение
    var denied by rememberSaveable { mutableStateOf(false) } // пользователь отказал в разрешении

    // Окно запроса разрешений; в лямбде — что делать после ответа пользователя
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionGranted = context.hasActivityRecognition()
        denied = !permissionGranted
        if (permissionGranted && vm.sensorAvailable) StepService.start(context)
    }

    // Что именно сохраняем после выбора папки: 0 — ничего, 1 — таблицу CSV, 2 — резервную копию
    var pendingSave by rememberSaveable { mutableIntStateOf(0) }
    // Данные из выбранного файла резервной копии, ожидающие подтверждения восстановления
    var pendingRestore by remember { mutableStateOf<BackupData?>(null) }

    // Сообщение внизу экрана (Toast) по строке из ресурсов
    fun toast(resId: Int, long: Boolean = false) =
        Toast.makeText(context, context.getString(resId), if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    // Системное окно выбора ПАПКИ (файлы в нём не показываются); после выбора сохраняем файл в эту папку
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        val kind = pendingSave
        pendingSave = 0
        if (folder != null && kind != 0) { // null — пользователь отменил
            val done = { ok: Boolean -> toast(if (ok) R.string.toast_export_ok else R.string.toast_export_fail, long = true) }
            if (kind == 1) vm.saveCsv(folder, done) else vm.saveBackup(folder, done)
        }
    }

    // Окно выбора файла резервной копии: показывает только папки и файлы подходящего типа
    val backupFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.readBackup(uri) { result ->
                result.onSuccess { pendingRestore = it } // файл подошёл — спрашиваем подтверждение
                    .onFailure { toast(if (it is IllegalArgumentException) R.string.toast_restore_bad else R.string.toast_read_fail, long = true) }
            }
        }
    }

    // При первом показе экрана просим разрешения, если их ещё нет
    LaunchedEffect(Unit) {
        if (!permissionGranted) launcher.launch(requiredPermissions())
    }

    // Каждый раз, когда приложение возвращается на экран: перепроверяем разрешение и (пере)запускаем сервис
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = context.hasActivityRecognition()
        if (permissionGranted && vm.sensorAvailable) StepService.start(context)
    }

    // Три вкладки: название + иконка
    val tabs = listOf(
        stringResource(R.string.tab_today) to painterResource(R.drawable.ic_steps),
        stringResource(R.string.tab_stats) to rememberVectorPainter(Icons.Rounded.BarChart),
        stringResource(R.string.tab_settings) to rememberVectorPainter(Icons.Rounded.Settings),
    )

    // Scaffold — «каркас» экрана: нижняя панель + область содержимого
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (title, painter) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },                       // переключаем вкладку
                        icon = { Icon(painter, contentDescription = null) },
                        label = { Text(title) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Баннер с предупреждением — сверху над содержимым
            when {
                !vm.sensorAvailable -> Banner(
                    stringResource(R.string.banner_no_sensor),
                    null,
                ) {}
                !permissionGranted -> Banner(
                    stringResource(R.string.banner_permission),
                    if (denied) stringResource(R.string.btn_open_settings) else stringResource(R.string.btn_allow),
                ) {
                    if (denied) {
                        // Отказали навсегда — открываем системные настройки приложения
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null))
                        )
                    } else {
                        launcher.launch(requiredPermissions()) // просим разрешение ещё раз
                    }
                }
            }

            // Содержимое выбранной вкладки (с плавной сменой)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = tab, label = "tabs") { t ->
                    when (t) {
                        0 -> TodayScreen(history, settings, today, Modifier.fillMaxSize())
                        1 -> StatsScreen(history, settings, today, Modifier.fillMaxSize())
                        else -> SettingsScreen(
                            settings = settings,
                            onGoal = vm::setGoal,
                            onHeight = vm::setHeight,
                            onSex = vm::setSex,
                            onWeight = vm::setWeight,
                            onStepLengthManual = vm::setStepLengthManual,
                            onStepLength = vm::setStepLength,
                            onTheme = vm::setTheme,
                            onLanguage = { tag ->
                                vm.setLanguage(tag)
                                (context as? Activity)?.recreate() // перезапускаем экран, чтобы применить язык
                            },
                            // Сохранение CSV: сначала выбираем папку
                            onExport = {
                                if (history.isEmpty()) toast(R.string.toast_no_data)
                                else { pendingSave = 1; folderLauncher.launch(null) }
                            },
                            // Резервная копия: сначала выбираем папку
                            onSaveBackup = {
                                if (history.isEmpty()) toast(R.string.toast_no_data)
                                else { pendingSave = 2; folderLauncher.launch(null) }
                            },
                            // Загрузка: выбираем файл (только папки и файлы подходящего типа)
                            onLoadBackup = { backupFileLauncher.launch(BackupManager.PICKER_MIME_TYPES) },
                            onResetToday = vm::resetToday,
                            onDemo = vm::fillDemoData,
                            onReset = vm::clearHistory,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    // Окно подтверждения восстановления из резервной копии
    pendingRestore?.let { data ->
        RestoreDialog(
            data = data,
            onConfirm = {
                vm.restoreBackup(data)
                pendingRestore = null
                toast(R.string.toast_restore_ok)
            },
            onDismiss = { pendingRestore = null },
        )
    }
}

/** Окно подтверждения: что будет восстановлено из выбранного файла резервной копии. */
@Composable
private fun RestoreDialog(data: BackupData, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val dates = data.days.keys.sortedBy { it.toEpochDay() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dlg_restore_title)) },
        text = {
            Text(
                if (dates.isEmpty()) stringResource(R.string.dlg_restore_settings_only)
                else stringResource(R.string.dlg_restore_text, dates.size, dates.first().mediumDate(), dates.last().mediumDate())
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.dlg_restore_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) } },
    )
}

/** Цветная карточка-предупреждение с необязательной кнопкой. */
@Composable
private fun Banner(text: String, button: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (button != null) TextButton(onClick = onClick) { Text(button) }
        }
    }
}

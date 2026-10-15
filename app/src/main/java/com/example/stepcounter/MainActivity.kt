package com.example.stepcounter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepcounter.service.StepService
import com.example.stepcounter.ui.MainViewModel
import com.example.stepcounter.ui.screens.SettingsScreen
import com.example.stepcounter.ui.screens.StatsScreen
import com.example.stepcounter.ui.screens.TodayScreen
import com.example.stepcounter.ui.theme.StepCounterTheme
import com.example.stepcounter.util.hasActivityRecognition
import com.example.stepcounter.util.requiredPermissions

/** Единственная Activity приложения. Весь интерфейс рисуется через Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // рисуем под системными панелями (современный вид)
        setContent {       // здесь начинается Compose-интерфейс
            StepCounterTheme {
                StepCounterRoot()
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

    // Системное окно «Сохранить как…» для CSV; в лямбде — что делать после выбора файла
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) { // null — пользователь отменил
            vm.exportCsv(uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "Статистика сохранена" else "Не удалось сохранить файл",
                    Toast.LENGTH_LONG,
                ).show()
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
        "Сегодня" to painterResource(R.drawable.ic_steps),
        "Статистика" to rememberVectorPainter(Icons.Rounded.BarChart),
        "Настройки" to rememberVectorPainter(Icons.Rounded.Settings),
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
                    "На этом устройстве нет датчика шагов. Реальный подсчёт недоступен — " +
                        "но можно посмотреть интерфейс на демо-данных (Настройки).",
                    null,
                ) {}
                !permissionGranted -> Banner(
                    "Нужен доступ к физической активности, чтобы считать шаги.",
                    if (denied) "Настройки" else "Разрешить",
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
                            onStepLength = vm::setStepLength,
                            onWeight = vm::setWeight,
                            onExport = {
                                if (history.isEmpty()) {
                                    Toast.makeText(context, "Пока нет данных для экспорта", Toast.LENGTH_SHORT).show()
                                } else {
                                    exportLauncher.launch("steps_$today.csv") // предлагаемое имя файла
                                }
                            },
                            onDemo = vm::fillDemoData,
                            onReset = vm::clearHistory,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
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

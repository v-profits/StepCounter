package com.example.stepcounter.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.StepCounterApp
import com.example.stepcounter.data.BackupData
import com.example.stepcounter.data.BackupManager
import com.example.stepcounter.data.CsvExporter
import com.example.stepcounter.data.FileSaver
import com.example.stepcounter.util.AppLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * ViewModel — «посредник» между данными и экраном.
 * Переживает поворот экрана, поэтому состояние не теряется.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    // Берём общий репозиторий из класса приложения
    private val repo = (app as StepCounterApp).repository

    // Данные для экранов (пробрасываем из репозитория)
    val history = repo.history
    val settings = repo.settings

    private val _today = MutableStateFlow(LocalDate.now())
    /** Текущая дата; обновляется, если приложение открыто в полночь. */
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** Есть ли в телефоне датчик шагов (на эмуляторах обычно нет). */
    val sensorAvailable: Boolean =
        (app.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            .getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    init {
        // Каждые 15 секунд перепроверяем дату — чтобы в полночь экран «перешёл» на новый день
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                _today.value = LocalDate.now()
            }
        }
    }

    // Действия, которые вызываются с экрана настроек:
    fun setGoal(value: Int) = repo.updateSettings { it.copy(goal = value) }              // изменить цель
    fun setStepLength(value: Int) = repo.updateSettings { it.copy(stepLengthCm = value) } // длина шага (вручную)
    fun setHeight(value: Int) = repo.updateSettings { it.copy(heightCm = value) }         // рост
    fun setSex(value: Int) = repo.updateSettings { it.copy(sex = value) }                 // пол
    /** Включает/выключает ручной ввод длины шага; при включении начинаем с рассчитанного значения. */
    fun setStepLengthManual(enabled: Boolean) = repo.updateSettings {
        if (enabled) it.copy(stepLengthManual = true, stepLengthCm = it.autoStepLengthCm)
        else it.copy(stepLengthManual = false)
    }
    fun setWeight(value: Int) = repo.updateSettings { it.copy(weightKg = value) }        // вес
    fun setTheme(mode: Int) = repo.updateSettings { it.copy(themeMode = mode) }          // тема: система / светлая / тёмная
    fun setLanguage(tag: String) {                                                      // язык: "" = система, "en", "ru"
        AppLocale.set(tag)
        repo.updateSettings { it.copy(language = tag) }
    }
    fun fillDemoData() = repo.fillDemoData()                                             // демо-данные
    fun resetToday() = repo.resetToday()                                                 // обнулить шаги за сегодня
    fun clearHistory() = repo.clearHistory()                                             // стереть историю

    /** Сохраняет CSV-таблицу в выбранную пользователем папку. [onResult]: true — успешно. */
    fun saveCsv(folder: Uri, onResult: (Boolean) -> Unit) {
        val ctx = getApplication<Application>()
        runIo(onResult) {
            val text = CsvExporter.build(ctx, repo.history.value, repo.settings.value)
            FileSaver.saveToFolder(ctx, folder, "steps_${LocalDate.now()}.csv", "text/csv", text.toByteArray(Charsets.UTF_8))
        }
    }

    /** Сохраняет файл резервной копии в выбранную пользователем папку. */
    fun saveBackup(folder: Uri, onResult: (Boolean) -> Unit) {
        val ctx = getApplication<Application>()
        runIo(onResult) {
            val text = BackupManager.build(repo.history.value, repo.settings.value)
            FileSaver.saveToFolder(
                ctx, folder, BackupManager.fileName(LocalDate.now()), BackupManager.MIME,
                text.toByteArray(Charsets.UTF_8),
            )
        }
    }

    /**
     * Читает и проверяет файл резервной копии. В [onResult] приходит результат:
     * данные — если файл подходит, иначе ошибка (не тот файл или не удалось прочитать).
     */
    fun readBackup(uri: Uri, onResult: (Result<BackupData>) -> Unit) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { BackupManager.parse(FileSaver.readText(ctx, uri)) }
            }
            onResult(result)
        }
    }

    /** Применяет данные из резервной копии. */
    fun restoreBackup(data: BackupData) = repo.restoreBackup(data)

    // Выполняет [block] в фоновом потоке и сообщает, получилось ли
    private fun runIo(onResult: (Boolean) -> Unit, block: () -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching(block).isSuccess }
            onResult(ok)
        }
    }
}

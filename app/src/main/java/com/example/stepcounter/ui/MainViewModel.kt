package com.example.stepcounter.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.StepCounterApp
import com.example.stepcounter.data.CsvExporter
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
    fun setStepLength(value: Int) = repo.updateSettings { it.copy(stepLengthCm = value) } // длина шага
    fun setWeight(value: Int) = repo.updateSettings { it.copy(weightKg = value) }        // вес
    fun setTheme(mode: Int) = repo.updateSettings { it.copy(themeMode = mode) }          // тема: система / светлая / тёмная
    fun setLanguage(tag: String) {                                                      // язык: "" = система, "en", "ru"
        AppLocale.set(tag)
        repo.updateSettings { it.copy(language = tag) }
    }
    fun fillDemoData() = repo.fillDemoData()                                             // демо-данные
    fun clearHistory() = repo.clearHistory()                                             // стереть историю

    /**
     * Сохраняет статистику в CSV-файл [uri] (его выбрал пользователь в системном окне).
     * Запись идёт в фоновом потоке; по окончании вызывается [onResult] (true — успешно).
     */
    fun exportCsv(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    CsvExporter.write(getApplication<Application>(), uri, repo.history.value, repo.settings.value)
                }.isSuccess
            }
            onResult(ok)
        }
    }
}

package com.example.stepcounter.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.StepCounterApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    fun fillDemoData() = repo.fillDemoData()                                             // демо-данные
    fun clearHistory() = repo.clearHistory()                                             // стереть историю
}

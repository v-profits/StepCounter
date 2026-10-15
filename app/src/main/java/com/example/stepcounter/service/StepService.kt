package com.example.stepcounter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.stepcounter.MainActivity
import com.example.stepcounter.R
import com.example.stepcounter.StepCounterApp
import com.example.stepcounter.data.StepRepository
import com.example.stepcounter.data.UserSettings
import com.example.stepcounter.util.formatKcal
import com.example.stepcounter.util.formatKm
import com.example.stepcounter.util.formatSteps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Фоновый сервис: слушает датчик шагов и обновляет уведомление.
 * Работает как foreground-сервис — поэтому система его не «убивает» и в шторке
 * всегда висит уведомление (шаги, калории, дистанция и полоска прогресса).
 * SensorEventListener — интерфейс, через который датчик присылает показания.
 */
class StepService : Service(), SensorEventListener {

    // «Область» для корутин: нужна, чтобы в фоне следить за изменением данных
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sensorManager: SensorManager? = null
    private lateinit var repo: StepRepository

    /** Вызывается один раз при создании сервиса. */
    override fun onCreate() {
        super.onCreate()
        repo = (application as StepCounterApp).repository
        createChannel() // канал уведомлений (обязателен с Android 8)

        // ВАЖНО: foreground-сервис обязан сразу показать уведомление
        val initial = buildNotification(repo.stepsFor(), repo.settings.value)
        if (Build.VERSION.SDK_INT >= 34) {
            // На Android 14+ нужно указать тип сервиса — здесь «здоровье»
            startForeground(NOTIFICATION_ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, initial)
        }

        // Получаем датчик шагов; на некоторых устройствах его нет
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            stopSelf() // нет датчика — сервису делать нечего
            return
        }
        sensorManager = sm
        // Подписываемся на показания датчика (this = onSensorChanged ниже)
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        // Следим за данными: как только меняются шаги или настройки — обновляем уведомление
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        scope.launch {
            combine(repo.history, repo.settings) { history, settings ->
                (history[LocalDate.now()] ?: 0) to settings // пара (шаги сегодня, настройки)
            }.distinctUntilChanged() // реагируем, только если пара изменилась
                .collect { (steps, settings) ->
                    nm.notify(NOTIFICATION_ID, buildNotification(steps, settings))
                }
        }
    }

    /** START_STICKY — если система остановит сервис, она потом перезапустит его. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    /** Привязка не используется — сервис только запускается/останавливается. */
    override fun onBind(intent: Intent?): IBinder? = null

    /** Датчик прислал новое показание. */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            // values[0] — общее число шагов с момента загрузки телефона
            repo.onCounterValue(event.values[0].toLong())
        }
    }

    /** Изменилась точность датчика — нам это не важно. */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Сервис останавливается: отписываемся от датчика и отменяем корутины. */
    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        scope.cancel()
        super.onDestroy()
    }

    /** Создаёт канал уведомлений «Подсчёт шагов» с низкой важностью (без звука). */
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) } // без точки-значка на иконке
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    /**
     * Собирает уведомление. Внутри — свой макет (notification_steps.xml):
     * иконки + числа шагов, калорий и километров и полоска прогресса к цели.
     */
    private fun buildNotification(steps: Int, settings: UserSettings): Notification {
        val goal = settings.goal
        val kcal = settings.calories(steps).formatKcal()
        val km = settings.distanceKm(steps).formatKm()

        // Заполняем макет уведомления значениями
        val content = RemoteViews(packageName, R.layout.notification_steps).apply {
            setTextViewText(R.id.notif_steps, steps.formatSteps())
            setTextViewText(R.id.notif_kcal, "$kcal ккал")
            setTextViewText(R.id.notif_km, "$km км")
            setProgressBar(R.id.notif_progress, goal, steps.coerceAtMost(goal), false)
        }

        // Нажатие на уведомление открывает приложение
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_steps)                         // маленькая иконка в строке состояния
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())   // система рисует рамку, мы — содержимое
            .setCustomContentView(content)                             // наш макет с иконками
            .setContentTitle(steps.formatSteps())                      // запасной текст (там, где свой макет не показывается)
            .setContentText("$kcal ккал · $km км")
            .setOngoing(true)                                          // нельзя смахнуть (на старых версиях)
            .setOnlyAlertOnce(true)                                    // не пищать при обновлении
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "steps"   // id канала уведомлений
        private const val NOTIFICATION_ID = 1    // id самого уведомления

        /** Запускает сервис. Вызывается из экрана и из BootReceiver. */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, StepService::class.java))
            } catch (_: Exception) {
                // Система не разрешила запуск из фона — запустим при следующем открытии приложения.
            }
        }
    }
}

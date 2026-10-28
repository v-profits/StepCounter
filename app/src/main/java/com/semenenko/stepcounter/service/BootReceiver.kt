package com.semenenko.stepcounter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.semenenko.stepcounter.util.hasActivityRecognition

/**
 * Получает системный сигнал «телефон включился» (BOOT_COMPLETED)
 * и заново запускает сервис подсчёта шагов.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Запускаем только если разрешение на шаги уже выдано
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && context.hasActivityRecognition()) {
            StepService.start(context)
        }
    }
}

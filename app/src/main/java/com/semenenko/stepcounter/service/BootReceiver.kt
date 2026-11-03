package com.semenenko.stepcounter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.semenenko.stepcounter.util.hasActivityRecognition

/**
 * Получает системные сигналы «телефон включился» (BOOT_COMPLETED) и «приложение обновлено»
 * (MY_PACKAGE_REPLACED) и заново запускает сервис подсчёта шагов.
 * Без второго сигнала после каждого обновления приложения счёт стоял до ручного открытия программы.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Запускаем только если разрешение на шаги уже выдано
        val action = intent.action
        if ((action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) &&
            context.hasActivityRecognition()
        ) {
            StepService.start(context)
        }
    }
}

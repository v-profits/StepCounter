package com.semenenko.stepcounter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.semenenko.stepcounter.util.hasActivityRecognition

/**
 * «Сторож»: срабатывает по будильнику раз в ~15 минут (будильник переживает «убийство» процесса)
 * и заново запускает сервис подсчёта, если систему удалось остановить. Затем ставит следующий будильник.
 */
class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.hasActivityRecognition()) {
            StepService.start(context)          // если сервис жив — ничего не меняется
            StepService.scheduleWatchdog(context)
        }
    }
}

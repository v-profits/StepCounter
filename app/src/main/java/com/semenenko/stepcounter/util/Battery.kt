package com.semenenko.stepcounter.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.semenenko.stepcounter.AppConfig

/** Исключено ли приложение из оптимизации батареи (то есть система не «усыпляет» его в фоне). */
fun Context.isIgnoringBatteryOptimizations(): Boolean =
    (getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

/**
 * Просит разрешить работу без ограничений: системное окно «Разрешить» (одно нажатие).
 * Если окно недоступно (прошивка, вариант без разрешения) — открывает список настроек батареи.
 */
@SuppressLint("BatteryLife") // разрешение объявлено в AndroidManifest.xml
fun Context.requestBackgroundWork() {
    if (AppConfig.BATTERY_DIRECT) {
        val ok = runCatching {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }.isSuccess
        if (ok) return
    }
    openBatterySettings()
}

/** Intent для запуска с ожиданием результата: системное окно «Разрешить» (или список настроек батареи, если окно не используется). */
@SuppressLint("BatteryLife") // разрешение объявлено в AndroidManifest.xml
fun Context.backgroundWorkIntent(): Intent =
    if (AppConfig.BATTERY_DIRECT) Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
    else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

/** Открывает настройки батареи для ручной настройки: общий список, а если его нет — настройки приложения. */
fun Context.openBatterySettings() {
    val intents = listOf(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )
    for (i in intents) {
        if (runCatching { startActivity(i) }.isSuccess) return
    }
}

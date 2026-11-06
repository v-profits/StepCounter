package com.semenenko.stepcounter.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
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

/** Включены ли уведомления приложения (на Android 13+ зависит от разрешения POST_NOTIFICATIONS). */
fun Context.notificationsEnabled(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

/** Открывает настройки уведомлений этого приложения (а если не получилось — настройки приложения). */
fun Context.openNotificationSettings() {
    val intents = listOf(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )
    for (i in intents) {
        if (runCatching { startActivity(i) }.isSuccess) return
    }
}

/**
 * Производители, у которых есть собственная защита фона (отдельные выключатели «Работа в фоновом режиме»,
 * «Автозапуск»). Их нельзя включить из приложения и нельзя проверить — можно только отвести пользователя в настройки.
 */
fun isRestrictiveOem(): Boolean {
    val m = Build.MANUFACTURER.lowercase()
    return listOf(
        "xiaomi", "redmi", "poco", "oppo", "realme", "oneplus", "vivo", "iqoo",
        "huawei", "honor", "samsung", "meizu", "tecno", "infinix", "itel",
    ).any { m.contains(it) }
}

/** Открывает системную страницу этого приложения («О приложении»). */
fun Context.openAppSettings() {
    runCatching {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }
}

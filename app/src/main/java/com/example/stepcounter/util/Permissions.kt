package com.example.stepcounter.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Выдано ли приложению разрешение на распознавание активности (нужно для шагов).
 * До Android 10 (API 29) такого разрешения нет — считаем, что оно есть.
 */
fun Context.hasActivityRecognition(): Boolean =
    Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED

/** Список разрешений, которые нужно запросить у пользователя на этой версии Android. */
fun requiredPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION) // шаги
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)   // уведомления
}.toTypedArray()

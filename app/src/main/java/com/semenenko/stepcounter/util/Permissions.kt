package com.semenenko.stepcounter.util

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

/**
 * Разрешения, которые просим при первом запуске: только распознавание активности (шаги).
 * Уведомления и работа в фоне запрашиваются следом, по очереди (см. MainActivity).
 */
fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 29) arrayOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyArray()

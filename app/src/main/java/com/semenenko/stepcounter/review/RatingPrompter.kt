package com.semenenko.stepcounter.review

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.semenenko.stepcounter.AppConfig
import com.semenenko.stepcounter.R
import com.semenenko.stepcounter.util.withAppLocale

/**
 * Решает, когда просить оценку, и что делать с ответом.
 *
 * Когда: на AppConfig.RATING_FIRST_AT-м открытии приложения (по умолчанию 10-м), затем через каждые
 * AppConfig.RATING_EVERY открытий (60-е, 110-е, …) — пока пользователь не поставит оценку. После оценки больше не просим.
 *
 * Что делаем (вариант rustore, свои звёзды): 4–5 звёзд — открываем страницу приложения в магазине,
 * 1–3 звезды — открываем письмо разработчику. В варианте play показываем только окно оценки Google.
 */
object RatingPrompter {
    private const val PREFS = "review"
    private const val KEY_LAUNCHES = "launch_count"   // сколько раз пользователь открывал приложение
    private const val KEY_RATED = "rated"             // оценка уже поставлена (или окно магазина уже показано)

    @Volatile
    private var pendingThisSession = false // пора показать окно в этом запуске

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Считает открытие приложения и решает, нужно ли показать окно в этом запуске.
     * Вызывается один раз при каждом новом запуске экрана (не при смене языка).
     */
    fun countLaunch(context: Context) {
        val p = prefs(context)
        val launches = p.getInt(KEY_LAUNCHES, 0) + 1
        p.edit().putInt(KEY_LAUNCHES, launches).apply()
        pendingThisSession = !p.getBoolean(KEY_RATED, false) &&
            launches >= AppConfig.RATING_FIRST_AT &&
            (launches - AppConfig.RATING_FIRST_AT) % AppConfig.RATING_EVERY == 0
    }

    /** Нужно ли показать окно в этом запуске. */
    fun isPending(): Boolean = pendingThisSession

    /** Окно закрыто кнопкой «Позже»: в этом запуске больше не показываем, следующая попытка — через RATING_EVERY открытий. */
    fun clearPending() {
        pendingThisSession = false
    }

    private fun markRated(context: Context) {
        prefs(context).edit().putBoolean(KEY_RATED, true).apply()
    }

    /** Вариант play: показывает окно оценки самого магазина. Результат оценки магазин не сообщает, поэтому после показа больше не просим. */
    fun askNative(activity: Activity) {
        pendingThisSession = false
        createStoreReview().request(activity) { shown -> if (shown) markRated(activity) }
    }

    /**
     * Пользователь выбрал [stars] звёзд в своём окне. Запоминаем, что оценка поставлена, и направляем:
     * 4–5 — на страницу приложения в магазине, 1–3 — в письмо разработчику.
     * Возвращает false, если нужное приложение (магазин или почта) на телефоне не нашлось.
     */
    fun submit(activity: Activity, stars: Int): Boolean {
        markRated(activity)
        pendingThisSession = false
        return if (stars >= 4) openStorePage(activity) else writeToDeveloper(activity, stars)
    }

    /** Открывает страницу приложения в магазине: сначала в приложении магазина, если его нет — в браузере. */
    private fun openStorePage(activity: Activity): Boolean {
        val pkg = activity.packageName
        for (url in listOf(AppConfig.STORE_APP_URI + pkg, AppConfig.STORE_WEB_URL + pkg)) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return true
            } catch (_: ActivityNotFoundException) {
                // пробуем следующий адрес
            }
        }
        return false
    }

    /**
     * Открывает черновик письма разработчику: адрес, тема с оценкой и заготовка текста.
     * Внизу письма добавляются версия приложения и модель телефона — это помогает разобраться с проблемой.
     */
    private fun writeToDeveloper(activity: Activity, stars: Int): Boolean {
        val ctx = activity.withAppLocale() // текст заготовки — на выбранном языке
        val version = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
        } catch (_: Exception) {
            "?"
        }
        val body = ctx.getString(R.string.mail_body_intro, stars) +
            "\n\n---\nApp: $version\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n"
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConfig.FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.mail_subject, stars))
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return try {
            activity.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false // на телефоне нет почтового приложения
        }
    }
}

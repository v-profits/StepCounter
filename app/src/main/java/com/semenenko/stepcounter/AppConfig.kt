package com.semenenko.stepcounter

/** Настройки, которые владелец приложения меняет вручную. */
object AppConfig {
    /**
     * Ссылка на страницу добровольной поддержки (Ko-fi, Boosty, Buy Me a Coffee и т.п.).
     * Она задаётся в app/build.gradle.kts отдельно для каждого магазина (поле SUPPORT_URL у вариантов play и rustore).
     * Пока строка пустая, карточка «Поддержать разработчика» в настройках НЕ показывается.
     *
     * Важно: Google Play может отклонить приложение со ссылкой на пожертвования внутри программы —
     * поэтому для варианта play ссылка оставлена пустой.
     */
    val SUPPORT_URL: String get() = BuildConfig.SUPPORT_URL

    /** Почта разработчика: сюда уходят письма пользователей, поставивших 1–3 звезды. */
    const val FEEDBACK_EMAIL = "vl.profits@gmail.com"

    /** С какого по счёту открытия приложения предлагаем оценить его (10 = на десятом открытии). */
    const val RATING_FIRST_AT = 10

    /** Через сколько открытий повторяем предложение, пока оценка не поставлена. */
    const val RATING_EVERY = 50

    /**
     * true  — своё окно со звёздами: 4–5 звёзд ведут на страницу приложения в магазине, 1–3 — в письмо разработчику (вариант rustore);
     * false — показывается только окно оценки самого магазина (вариант play: правила Google не разрешают отбирать оценки).
     * Задаётся в app/build.gradle.kts для каждого варианта.
     */
    val RATING_FUNNEL: Boolean get() = BuildConfig.RATING_FUNNEL

    /** Адрес страницы приложения в магазине: сначала открываем приложение магазина, если его нет — сайт. */
    val STORE_APP_URI: String get() = BuildConfig.STORE_APP_URI
    val STORE_WEB_URL: String get() = BuildConfig.STORE_WEB_URL
}

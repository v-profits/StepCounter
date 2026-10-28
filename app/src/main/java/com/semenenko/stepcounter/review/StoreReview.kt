package com.semenenko.stepcounter.review

import android.app.Activity

/**
 * Окно оценки магазина. Реализация своя для каждого магазина:
 *  - вариант play    — Google Play In-App Review (папка app/src/play);
 *  - вариант rustore — RuStore In-app Review SDK (папка app/src/rustore).
 * Функция createStoreReview() объявлена в каждой из этих папок.
 * Сейчас окно магазина показывается в варианте play (см. RatingPrompter.askNative).
 */
interface StoreReview {
    /** Показывает окно оценки. [onDone] получает true, если магазин принял запрос, и false, если окно показать не удалось. */
    fun request(activity: Activity, onDone: (Boolean) -> Unit)
}

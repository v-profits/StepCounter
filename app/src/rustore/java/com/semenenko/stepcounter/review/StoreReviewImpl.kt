package com.semenenko.stepcounter.review

import android.app.Activity
import ru.rustore.sdk.review.RuStoreReviewManagerFactory

/**
 * Окно оценки RuStore (In-app Review SDK). Работает, если на телефоне установлен свежий RuStore,
 * пользователь в нём авторизован, а приложение опубликовано в RuStore с той же подписью.
 */
private class RuStoreReview : StoreReview {
    override fun request(activity: Activity, onDone: (Boolean) -> Unit) {
        val manager = RuStoreReviewManagerFactory.create(activity)
        // 1) Просим RuStore подготовить окно
        manager.requestReviewFlow()
            .addOnSuccessListener { reviewInfo ->
                // 2) Показываем окно
                manager.launchReviewFlow(reviewInfo)
                    .addOnSuccessListener { onDone(true) }
                    .addOnFailureListener { onDone(false) } // например, пользователь уже оценивал или лимит показов
            }
            .addOnFailureListener { onDone(false) } // RuStore не установлен, пользователь не вошёл и т.п.
    }
}

fun createStoreReview(): StoreReview = RuStoreReview()

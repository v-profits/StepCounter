package com.semenenko.stepcounter.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/** Окно оценки Google Play (In-App Review API): показывает звёздочки прямо внутри приложения. */
private class PlayStoreReview : StoreReview {
    override fun request(activity: Activity, onDone: (Boolean) -> Unit) {
        val manager = ReviewManagerFactory.create(activity)
        // 1) Просим Google Play подготовить окно
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) {
                onDone(false) // Google Play недоступен или слишком частые запросы
                return@addOnCompleteListener
            }
            // 2) Показываем окно; по его закрытии узнаём только, что процесс завершён (не оценку)
            manager.launchReviewFlow(activity, request.result).addOnCompleteListener { onDone(true) }
        }
    }
}

fun createStoreReview(): StoreReview = PlayStoreReview()

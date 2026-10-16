package com.example.stepcounter

import android.app.Application
import com.example.stepcounter.data.StepRepository
import com.example.stepcounter.util.AppLocale

/**
 * Класс приложения. Создаётся системой ОДИН раз при запуске процесса — раньше
 * любых Activity и сервисов. Здесь мы создаём единый репозиторий данных,
 * которым пользуются и сервис (пишет шаги), и интерфейс (показывает их).
 */
class StepCounterApp : Application() {

    // Единственный экземпляр хранилища шагов на всё приложение.
    // private set — присвоить значение можно только внутри этого класса.
    lateinit var repository: StepRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = StepRepository(this)
        // Применяем язык, выбранный пользователем (до создания экранов, сервиса и виджета)
        AppLocale.set(repository.settings.value.language)
    }
}

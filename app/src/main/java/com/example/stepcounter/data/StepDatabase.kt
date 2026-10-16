package com.example.stepcounter.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Строка таблицы daily_steps: один день = одна запись.
 * Дату храним строкой ISO ("2026-10-14") — она сортируется по алфавиту так же, как по времени,
 * и не требует конвертеров типов.
 */
@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String, // уникальный ключ — дата
    val steps: Int,               // шагов за день
)

/** DAO — набор запросов к таблице. Room сам напишет реализацию. */
@Dao
abstract class StepDao {

    /** «Живой» список всех дней: при любом изменении таблицы Room присылает новый список. */
    @Query("SELECT * FROM daily_steps")
    abstract fun observeAll(): Flow<List<DailyStepsEntity>>

    /** Разовое чтение всех дней. */
    @Query("SELECT * FROM daily_steps")
    abstract suspend fun getAll(): List<DailyStepsEntity>

    /** Создать запись дня, только если её ещё нет (IGNORE — при конфликте ничего не делать). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(entity: DailyStepsEntity)

    /** Записать список дней; существующие записи заменяются (REPLACE). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(entities: List<DailyStepsEntity>)

    /** Прибавить [delta] шагов к уже существующей записи дня. */
    @Query("UPDATE daily_steps SET steps = steps + :delta WHERE date = :date")
    abstract suspend fun addToRow(date: String, delta: Int)

    /** Удалить запись одного дня (используется для «Обнулить шаги за сегодня»). */
    @Query("DELETE FROM daily_steps WHERE date = :date")
    abstract suspend fun deleteDay(date: String)

    /** Удалить все записи. */
    @Query("DELETE FROM daily_steps")
    abstract suspend fun clear()

    /**
     * Добавить шаги к дню одной транзакцией: сначала создаём запись (если нет),
     * потом прибавляем. Транзакция гарантирует, что оба действия выполнятся вместе.
     */
    @Transaction
    open suspend fun addSteps(date: String, delta: Int) {
        insertIgnore(DailyStepsEntity(date, 0))
        addToRow(date, delta)
    }
}

/** Сама база данных (файл steps.db на телефоне). version — номер версии структуры таблиц. */
@Database(entities = [DailyStepsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun stepDao(): StepDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Возвращает единственный экземпляр базы (создаёт при первом обращении). */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "steps.db",
                ).build().also { instance = it }
            }
    }
}

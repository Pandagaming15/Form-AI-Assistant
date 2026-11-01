package com.example.formpilot.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "draft_sessions")
data class DraftEntity(
  @PrimaryKey val id: String,
  val formUrl: String,
  val formTitle: String,
  val status: String,
  val questionsJson: String,
  val updatedAt: Long
)

@Dao
interface DraftDao {
  @Query("SELECT * FROM draft_sessions ORDER BY updatedAt DESC")
  fun getAllDrafts(): Flow<List<DraftEntity>>

  @Query("SELECT * FROM draft_sessions WHERE id = :id LIMIT 1")
  suspend fun getDraftById(id: String): DraftEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(draft: DraftEntity)

  @Query("DELETE FROM draft_sessions WHERE id = :id")
  suspend fun deleteDraftById(id: String)

  @Query("DELETE FROM draft_sessions")
  suspend fun clearAllDrafts()
}

@Database(entities = [DraftEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun draftDao(): DraftDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "formpilot_local.db"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}

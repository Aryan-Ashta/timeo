package com.timeo.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeoDao {
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: Long): Category?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM categories")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE parentId = :parentId")
    suspend fun deleteChildren(parentId: Long)

    @Query("UPDATE categories SET parentId = NULL WHERE parentId = :parentId")
    suspend fun promoteChildren(parentId: Long)

    @Query("SELECT * FROM sessions WHERE endEpochMs IS NULL")
    fun observeRunning(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE endEpochMs IS NULL AND categoryId = :categoryId LIMIT 1")
    suspend fun runningFor(categoryId: Long): Session?

    /** Sessions overlapping [fromMs, toMs), including running ones. */
    @Query(
        "SELECT * FROM sessions WHERE startEpochMs < :toMs " +
            "AND (endEpochMs IS NULL OR endEpochMs > :fromMs) ORDER BY startEpochMs",
    )
    fun observeSessionsBetween(fromMs: Long, toMs: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY startEpochMs")
    fun observeAllSessions(): Flow<List<Session>>

    @Insert
    suspend fun insertSession(session: Session): Long

    @Insert
    suspend fun insertSessions(sessions: List<Session>)

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Transaction
    suspend fun deleteCategoryWithChildren(category: Category, deleteChildren: Boolean) {
        if (deleteChildren) deleteChildren(category.id) else promoteChildren(category.id)
        deleteCategory(category)
    }
}

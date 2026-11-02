package com.timeo.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A trackable thing in life. When [parentId] is set this is a subtimer of that category
 * (one level deep only) and its time also rolls up into the parent.
 */
@Entity(tableName = "categories", indices = [Index("parentId")])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long? = null,
    val name: String,
    val colorArgb: Int,
    val iconKey: String = "timer",
    val dailyGoalMinutes: Int? = null,
    val weeklyGoalMinutes: Int? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

/** One stopwatch run. [endEpochMs] is null while the stopwatch is running. */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId"), Index("startEpochMs")],
)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val startEpochMs: Long,
    val endEpochMs: Long? = null,
) {
    val isRunning: Boolean get() = endEpochMs == null
}

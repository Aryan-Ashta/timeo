package com.timeo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Category::class, Session::class], version = 1, exportSchema = true)
abstract class TimeoDatabase : RoomDatabase() {
    abstract fun dao(): TimeoDao

    companion object {
        fun build(context: Context): TimeoDatabase =
            Room.databaseBuilder(context, TimeoDatabase::class.java, "timeo.db").build()
    }
}

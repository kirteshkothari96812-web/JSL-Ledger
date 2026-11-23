package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WorkerEntity::class, AttendanceEntity::class, AdvanceEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(JslConverters::class)
abstract class JslDatabase : RoomDatabase() {
    abstract fun jslDao(): JslDao

    companion object {
        @Volatile
        private var INSTANCE: JslDatabase? = null

        fun getInstance(context: Context): JslDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JslDatabase::class.java,
                    "jsl_industries_ledger.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

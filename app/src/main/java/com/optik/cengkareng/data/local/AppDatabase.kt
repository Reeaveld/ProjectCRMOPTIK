package com.optik.cengkareng.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.local.dao.CustomerDao

@Database(
    entities = [CustomerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Fungsi ini menghubungkan Database dengan DAO
    abstract fun customerDao(): CustomerDao
}
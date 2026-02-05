package com.optik.cengkareng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: Int? = null, // ID dari MySQL Server
    val nama: String,
    val noHp: String,
    val jenisLensa: String, // Minus/Plus/Silinder
    val ukuranKiri: String,
    val ukuranKanan: String,
    val lastFollowUp: Long = 0,
    val isSynced: Boolean = false
)
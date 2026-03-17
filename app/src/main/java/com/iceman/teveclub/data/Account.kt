package com.iceman.teveclub.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usernameEncrypted: String,
    val passwordEncrypted: String?,
    val authTokenEncrypted: String?,
    val lastSync: Long = 0
)

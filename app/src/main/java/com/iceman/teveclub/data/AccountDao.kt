package com.iceman.teveclub.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: Account): Long

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<Account>

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)
}

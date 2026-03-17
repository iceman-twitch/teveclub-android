package com.iceman.teveclub.data

import android.content.Context

class AccountRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.accountDao()

    suspend fun getAllAccounts(): List<Account> {
        return dao.getAll()
    }

    suspend fun addAccount(account: Account): Long {
        return dao.insert(account)
    }

    suspend fun getById(id: Long): Account? = dao.getById(id)

    suspend fun update(account: Account) {
        dao.update(account)
    }

    suspend fun delete(account: Account) {
        dao.delete(account)
    }
}

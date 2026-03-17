package com.iceman.teveclub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iceman.teveclub.data.Account
import com.iceman.teveclub.data.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AccountRepository(application.applicationContext)

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    fun loadAccounts() {
        viewModelScope.launch {
            _accounts.value = repo.getAllAccounts()
        }
    }

    fun saveAccount(username: String, password: String) {
        viewModelScope.launch {
            // Check if account already exists, if so update it
            val existing = repo.getAllAccounts().find { it.usernameEncrypted == username }
            if (existing != null) {
                repo.update(existing.copy(passwordEncrypted = password))
            } else {
                repo.addAccount(Account(usernameEncrypted = username, passwordEncrypted = password, authTokenEncrypted = null))
            }
            _accounts.value = repo.getAllAccounts()
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repo.delete(account)
            _accounts.value = repo.getAllAccounts()
        }
    }
}

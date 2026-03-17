package com.iceman.teveclub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TeveViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = TeveApiRepository(application.applicationContext)

    private val _isLoggedIn = MutableStateFlow(AuthManager.isLoggedFlag(application.applicationContext))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _camelStatus = MutableStateFlow<TeveApiRepository.CamelStatus?>(null)
    val camelStatus: StateFlow<TeveApiRepository.CamelStatus?> = _camelStatus

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _learnPageHtml = MutableStateFlow<String?>(null)
    val learnPageHtml: StateFlow<String?> = _learnPageHtml

    private val _learnState = MutableStateFlow<TeveApiRepository.LearnPageState?>(null)
    val learnState: StateFlow<TeveApiRepository.LearnPageState?> = _learnState

    fun login(username: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.login(username, password)
            _isLoading.value = false
            if (res.isSuccess) {
                _isLoggedIn.value = true
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun logout(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.logout()
            _isLoading.value = false
            if (res.isSuccess) {
                _isLoggedIn.value = false
                _camelStatus.value = null
                onResult(true, null)
            } else onResult(false, res.exceptionOrNull()?.message)
        }
    }

    fun loadCamelStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.getCamelStatus()
            _isLoading.value = false
            if (res.isSuccess) _camelStatus.value = res.getOrNull()
            else _statusMessage.value = res.exceptionOrNull()?.message
        }
    }

    fun feedPet() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Etetés folyamatban..."
            val res = repo.feedUntilFull { progress ->
                _statusMessage.value = progress
            }
            _isLoading.value = false
            _statusMessage.value = if (res.isSuccess) res.getOrNull() else res.exceptionOrNull()?.message
            loadCamelStatus()
        }
    }

    fun setFood(foodId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repo.setFood(foodId)
            if (res.isSuccess) {
                onResult(true, "Kaja beállítva!")
                loadCamelStatus()
            } else onResult(false, res.exceptionOrNull()?.message)
        }
    }

    fun setDrink(drinkId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repo.setDrink(drinkId)
            if (res.isSuccess) {
                onResult(true, "Ital beállítva!")
                loadCamelStatus()
            } else onResult(false, res.exceptionOrNull()?.message)
        }
    }

    fun loadLearnPage(onResult: (TeveApiRepository.LearnPageState?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.getLearnPage()
            _isLoading.value = false
            if (res.isSuccess) {
                val state = res.getOrNull()
                _learnState.value = state
                onResult(state)
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message
                onResult(null)
            }
        }
    }

    fun submitLearn(lessonId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.submitLearn(lessonId)
            _isLoading.value = false
            if (res.isSuccess) {
                onResult(true, res.getOrNull())
                loadCamelStatus()
            } else onResult(false, res.exceptionOrNull()?.message)
        }
    }

    fun guessNumber(number: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.guessNumber(number)
            _isLoading.value = false
            if (res.isSuccess) onResult(true, res.getOrNull())
            else onResult(false, res.exceptionOrNull()?.message)
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}

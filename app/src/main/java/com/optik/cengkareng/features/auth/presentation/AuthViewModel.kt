package com.optik.cengkareng.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.response.LoginResponse
import com.optik.cengkareng.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginEvent = Channel<Resource<LoginResponse>>()
    val loginEvent = _loginEvent.receiveAsFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            authRepository.login(email.trim(), pass).collect {
                _loginEvent.send(it)
            }
        }
    }

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }

    fun getAdminName(): String = authRepository.getAdminName() ?: "Admin Optik"
    fun getAdminEmail(): String = authRepository.getAdminEmail() ?: "admin@optikcrm.com"
}

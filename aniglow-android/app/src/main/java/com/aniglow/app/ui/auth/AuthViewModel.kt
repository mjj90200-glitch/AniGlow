package com.aniglow.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.auth.AuthSession
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(
    val user: AuthUser? = AuthSession.user.value,
    val loading: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository(NetworkModule.authApi)
    private val mutableUiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            AuthSession.user.collect { user ->
                mutableUiState.value = mutableUiState.value.copy(user = user)
            }
        }
        if (!AuthSession.accessToken.isNullOrBlank()) restore()
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "请输入用户名和密码")
            return
        }
        submit(onSuccess) { repository.login(username, password) }
    }

    fun register(username: String, displayName: String, password: String, onSuccess: () -> Unit) {
        if (username.length !in 3..24) {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "用户名长度需要在 3 到 24 位之间")
            return
        }
        if (password.length < 8) {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "密码至少需要 8 位")
            return
        }
        submit(onSuccess) { repository.register(username, displayName, password) }
    }

    fun logout() {
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
            repository.logout()
            mutableUiState.value = AuthUiState()
        }
    }

    fun clearError() {
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
    }

    private fun restore() {
        viewModelScope.launch {
            repository.restore().onFailure {
                AuthSession.clear()
                mutableUiState.value = AuthUiState()
            }
        }
    }

    private fun submit(onSuccess: () -> Unit, request: suspend () -> Result<AuthUser>) {
        mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            request()
                .onSuccess {
                    mutableUiState.value = AuthUiState(user = it)
                    onSuccess()
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loading = false,
                        errorMessage = error.friendlyAuthMessage(),
                    )
                }
        }
    }
}

private fun Throwable.friendlyAuthMessage(): String = when (this) {
    is HttpException -> when (code()) {
        400 -> "提交的信息不符合要求，请检查后重试"
        401 -> "用户名或密码不正确"
        409 -> "这个用户名已经被使用"
        else -> "账号服务暂时不可用（${code()}）"
    }
    else -> message ?: "网络连接失败，请稍后重试"
}

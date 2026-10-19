package com.aniglow.app.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.AuthRepository
import com.aniglow.app.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val saving: Boolean = false,
    val selectedAvatar: Uri? = null,
    val removeAvatar: Boolean = false,
    val errorMessage: String? = null,
)

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(NetworkModule.authApi)
    private val communityRepository = CommunityRepository(NetworkModule.communityApi)
    private val mutableUiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = mutableUiState.asStateFlow()

    fun selectAvatar(uri: Uri?) {
        if (uri == null) return
        mutableUiState.value = mutableUiState.value.copy(
            selectedAvatar = uri,
            removeAvatar = false,
            errorMessage = null,
        )
    }

    fun removeAvatar() {
        mutableUiState.value = mutableUiState.value.copy(
            selectedAvatar = null,
            removeAvatar = true,
            errorMessage = null,
        )
    }

    fun save(displayName: String, currentAvatarUrl: String?, onSuccess: (AuthUser) -> Unit) {
        if (displayName.isBlank()) {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "请输入昵称")
            return
        }
        if (displayName.length > 80) {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "昵称不能超过 80 个字符")
            return
        }
        if (mutableUiState.value.saving) return
        mutableUiState.value = mutableUiState.value.copy(saving = true, errorMessage = null)
        viewModelScope.launch {
            val state = mutableUiState.value
            val avatarResult = when {
                state.selectedAvatar != null -> communityRepository.uploadImages(
                    getApplication<Application>().contentResolver,
                    listOf(state.selectedAvatar),
                ).map { it.firstOrNull().orEmpty() }
                state.removeAvatar -> Result.success("")
                else -> Result.success(currentAvatarUrl.orEmpty())
            }
            avatarResult.fold(
                onSuccess = { avatarUrl -> authRepository.updateProfile(displayName, avatarUrl) },
                onFailure = { Result.failure(it) },
            ).onSuccess { user ->
                mutableUiState.value = EditProfileUiState()
                onSuccess(user)
            }.onFailure { error ->
                mutableUiState.value = mutableUiState.value.copy(
                    saving = false,
                    errorMessage = error.message ?: "资料保存失败，请稍后重试",
                )
            }
        }
    }
}

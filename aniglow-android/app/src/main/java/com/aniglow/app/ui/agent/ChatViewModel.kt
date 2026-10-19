package com.aniglow.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.chat.ChatHistoryStore
import com.aniglow.app.data.chat.StoredChatMessage
import com.aniglow.app.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ChatMessage(val id: Long, val fromUser: Boolean, val content: String)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    val quotaRemaining: Int? = null,
    val errorMessage: String? = null,
)

class ChatViewModel(private val role: String, private val userId: Long) : ViewModel() {
    private val repository = DiscoveryRepository(NetworkModule.discoveryApi)
    private val mutableUiState = MutableStateFlow(
        ChatUiState(
            messages = ChatHistoryStore.load(userId, role).map { ChatMessage(it.id, it.fromUser, it.content) },
        ),
    )
    val uiState: StateFlow<ChatUiState> = mutableUiState.asStateFlow()

    fun send(message: String) {
        val content = message.trim()
        if (content.isBlank() || mutableUiState.value.sending) return
        val userMessage = ChatMessage(System.nanoTime(), true, content)
        mutableUiState.value = mutableUiState.value.copy(
            messages = mutableUiState.value.messages + userMessage,
            sending = true,
            errorMessage = null,
        )
        persistMessages()
        viewModelScope.launch {
            repository.chat(role, content)
                .onSuccess { response ->
                    mutableUiState.value = mutableUiState.value.copy(
                        messages = mutableUiState.value.messages + ChatMessage(System.nanoTime(), false, response.reply),
                        sending = false,
                        quotaRemaining = response.quotaRemaining,
                    )
                    persistMessages()
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        sending = false,
                        errorMessage = when (error) {
                            is HttpException -> if (error.code() == 429) "本时段对话次数已经用完" else "对话服务暂时不可用（${error.code()}）"
                            else -> error.message ?: "发送失败，请稍后重试"
                        },
                    )
                }
        }
    }

    fun clearError() {
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
    }

    fun clearHistory() {
        ChatHistoryStore.clear(userId, role)
        mutableUiState.value = mutableUiState.value.copy(messages = emptyList(), errorMessage = null)
    }

    private fun persistMessages() {
        ChatHistoryStore.save(
            userId,
            role,
            mutableUiState.value.messages.map { StoredChatMessage(it.id, it.fromUser, it.content) },
        )
    }

    class Factory(private val role: String, private val userId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(role, userId) as T
    }
}

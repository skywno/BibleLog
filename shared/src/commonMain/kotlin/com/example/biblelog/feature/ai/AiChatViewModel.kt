package com.example.biblelog.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.domain.model.AiConversationMode
import com.example.biblelog.domain.model.AiMessage
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiChatViewModel(
    private val repository: BibleLogRepository,
) : ViewModel() {
    val aiMessages: StateFlow<List<AiMessage>> = repository.aiMessages

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun sendMessage(content: String, mode: AiConversationMode) {
        if (content.isBlank() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            repository.sendAiMessage(content, mode)
            _isSending.value = false
        }
    }
}

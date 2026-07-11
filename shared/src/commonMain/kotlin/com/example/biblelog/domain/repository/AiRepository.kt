package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.AiConversationMode
import com.example.biblelog.domain.model.AiMessage
import kotlinx.coroutines.flow.StateFlow

interface AiRepository {
    val aiMessages: StateFlow<List<AiMessage>>

    suspend fun sendAiMessage(
        content: String,
        mode: AiConversationMode = AiConversationMode.CHAT,
    ): Result<AiMessage>
}

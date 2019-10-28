package com.r3.demo.chatapi.data

import com.r3.corda.lib.chat.contracts.ChatMessageContract
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import java.time.Instant

@BelongsToContract(ChatMessageContract::class)
data class ChatMessageData(
        val chatId: UniqueIdentifier,
        val participants: List<String>,
        val created: Instant,
        val content: String,
        val sender: String
) {
    companion object {
        fun fromState(chatMessageState: ChatMessage): ChatMessageData =
                ChatMessageData(
                        chatId = chatMessageState.chatId,
                        participants = chatMessageState.participants.map { it.nameOrNull()!!.organisation },
                        created = chatMessageState.created,
                        content = chatMessageState.content,
                        sender = chatMessageState.sender.name.organisation
                )

    }
}

package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatMessageContract
import com.r3.corda.lib.chat.contracts.internal.schemas.ChatMessageSchema
import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatMessage
import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant

@BelongsToContract(ChatMessageContract::class)
data class ChatMessage(
        val chatId: ChatID,
        override val participants: List<AbstractParty>,
        val created: Instant = Instant.now(),
        val content: String,
        val sender: Party
) : ChatBaseState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState =
            when (schema) {
                is ChatMessageSchema ->
                    PersistentChatMessage(
                            identifier = chatId.id,
                            created = created,
                            content = content,
                            sender = sender,
                            participants = participants
                    )
                else ->
                    throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
            }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ChatMessageSchema)
    override fun toString(): String {
        return "ChatMessage(chatId=$chatId, created=$created, participants=$participants, content='$content', sender=$sender)"
    }

}

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
        override val linearId: ChatID,
        override val created: Instant = Instant.now(),
        override val participants: List<AbstractParty>,
        val subject: String = "",
        val content: String = "",
        val sender: Party
) : ChatBaseState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState =
            when (schema) {
                is ChatMessageSchema ->
                    PersistentChatMessage(
                            identifier = linearId.id,
                            created = created,
                            subject = subject,
                            content = content,
                            sender = sender,
                            participants = participants
                    )
                else ->
                    throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
            }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ChatMessageSchema)
    override fun toString(): String {
        return "ChatMessage(linearId=$linearId, created=$created, participants=$participants, subject='$subject', content='$content', sender=$sender)"
    }

}

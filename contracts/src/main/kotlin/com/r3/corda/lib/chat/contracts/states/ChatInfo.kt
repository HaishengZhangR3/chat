package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatInfoContract
import com.r3.corda.lib.chat.contracts.internal.schemas.ChatSchema
import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatInfo
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

typealias ChatID = UniqueIdentifier

@CordaSerializable
sealed class ChatMessageType {
    object USER : ChatMessageType()     // user generated chat message
    object SYSTEM : ChatMessageType()   // system generated for facilitate usage, like add or remove participants
}

@BelongsToContract(ChatInfoContract::class)
data class ChatInfo(
        override val linearId: ChatID,
        override val created: Instant = Instant.now(),
        override val participants: List<AbstractParty>,
        val subject: String = "",
        val content: String = "",
        val attachment: SecureHash? = null,
        val sender: Party,
        val receivers: List<Party>,
        val chatMessageType: ChatMessageType = ChatMessageType.USER
) : ChatBaseState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState =
            when (schema) {
                is ChatSchema ->
                    PersistentChatInfo(
                            identifier = linearId.id,
                            created = created,
                            subject = subject,
                            content = content,
                            attachment = attachment?.toString(),
                            chatSender = sender,
                            chatReceiverList = receivers,
                            participants = participants
                    )
                else ->
                    throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
            }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ChatSchema)
    override fun toString(): String {
        return "ChatInfo(linearId=$linearId, created=$created, participants=$participants, subject='$subject', content='$content', attachment=$attachment, sender=$sender, receivers=$receivers, chatMessageType=$chatMessageType)"
    }

}

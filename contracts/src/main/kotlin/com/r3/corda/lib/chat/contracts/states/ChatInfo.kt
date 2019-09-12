package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatInfoContract
import com.r3.corda.lib.chat.contracts.internal.schemas.ChatSchema
import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatInfo
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant

/**
 * A state which records the chat subject, contents as well as the participants.
 * @TODO add a status indicating if the chat is closed or still opening
 */
@BelongsToContract(ChatInfoContract::class)
data class ChatInfo(
        val created: Instant = Instant.now(),
        val subject: String,
        val content: String,
        val attachment: SecureHash?,
        val from: Party,
        val to: List<Party>,
        override val linearId: UniqueIdentifier
) : LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = to + from

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        if (schema is ChatSchema) {
            return PersistentChatInfo(
                    created = created,
                    identifier = linearId.id,
                    subject = subject,
                    content = content,
                    attachment = attachment?.toString(),
                    chatFrom = from,
                    chatToList = to
            )
        } else {
            throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(ChatSchema)
    }

    override fun toString(): String {
        return "ChatInfo(subject='$subject', content='$content', attachment=$attachment, from=$from, to=$to, linearId=$linearId)"
    }

}

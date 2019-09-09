package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.AccountInfoContract
import com.r3.corda.lib.chat.contracts.internal.schemas.ChatSchema
import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatInfo
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

/**
 * A state which records the chat subject, contents as well as the participants.
 *
 */
@BelongsToContract(AccountInfoContract::class)
data class ChatInfo(
        val subject: String,
        val content: String,
        val attachment: Attachment?,
        val from: Party,
        val to: List<Party>,
        override val linearId: UniqueIdentifier = UniqueIdentifier.fromString(UUID.randomUUID().toString())
) : LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = to + from

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        if (schema is ChatSchema) {
            return PersistentChatInfo(
                    identifier = linearId.id,
                    subject = subject,
                    content = content,
                    attachment = attachment,
                    from = from,
                    to = to
            )
        } else {
            throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(ChatSchema)
    }
}

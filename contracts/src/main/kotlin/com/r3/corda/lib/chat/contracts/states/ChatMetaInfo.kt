package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatMetaInfoContract
import com.r3.corda.lib.chat.contracts.internal.schemas.ChatMetaInfoSchema
import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatMetaInfo
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

typealias ChatID = UniqueIdentifier

@CordaSerializable
sealed class ChatStatus {
    object ACTIVE : ChatStatus()
    object CLOSED : ChatStatus()
}

@BelongsToContract(ChatMetaInfoContract::class)
data class ChatMetaInfo(
        override val linearId: ChatID,
        override val created: Instant = Instant.now(),
        override val participants: List<AbstractParty>,
        val admin: Party,
        val receivers: List<Party>,
        val status: ChatStatus = ChatStatus.ACTIVE
) : ChatBaseState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState =
            when (schema) {
                is ChatMetaInfoSchema ->
                    PersistentChatMetaInfo(
                            identifier = linearId.id,
                            created = created,
                            admin = admin,
                            chatReceiverList = receivers,
                            status = status.toString(),
                            participants = participants
                    )
                else ->
                    throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
            }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ChatMetaInfoSchema)
    override fun toString(): String {
        return "ChatMetaInfo(linearId=$linearId, created=$created, participants=$participants, admin=$admin, receivers=$receivers, status=$status)"
    }

}

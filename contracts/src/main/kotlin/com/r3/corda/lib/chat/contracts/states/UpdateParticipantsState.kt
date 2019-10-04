package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatInfoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@BelongsToContract(ChatInfoContract::class)
data class UpdateParticipantsState(
        override val linearId: UniqueIdentifier,
        override val created: Instant = Instant.now(),
        override val participants: List<AbstractParty>,
        val from: Party,
        val toAdd: List<Party>,
        val toRemove: List<Party>,
        val includingHistoryChat: Boolean
) : ChatBaseState {

}

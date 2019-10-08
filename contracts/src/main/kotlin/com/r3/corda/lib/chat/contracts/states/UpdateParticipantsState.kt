package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatInfoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
sealed class UpdateParticipantsStatus {
    object PROPOSED : UpdateParticipantsStatus()
    object AGREED : UpdateParticipantsStatus()
    object REJECTED : UpdateParticipantsStatus()
}

// @todo: add a updating proposal ID to distinguish between several proposals. use linearId as its own key, add a new chatId.
//  also add argument in the flows
@BelongsToContract(ChatInfoContract::class)
data class UpdateParticipantsState(
        override val linearId: ChatID,
        override val created: Instant = Instant.now(),
        override val participants: List<AbstractParty>,
        val from: Party,
        val toAdd: List<Party>,
        val toRemove: List<Party>,
        val status: UpdateParticipantsStatus = UpdateParticipantsStatus.PROPOSED,
        val includingHistoryChat: Boolean
) : ChatBaseState {

}

package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.UpdateParticipantsContract
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

//  also add argument in the flows
@BelongsToContract(UpdateParticipantsContract::class)
data class UpdateParticipantsState(
        // update participants state does **not** need a unique ID, since we define the rule as:
        // if there is a updating propose, then no one else could propose again, instead, agree is needed.
        // reason is, if multiple parties proposed multiple ideas, we have to do merge logic, which is very easy to get wrong.
        // this is very similar with GIT file diff, which always merge your local version with the remote version,
        // and sometime it'll ask you to decide which version to take or neither or part of both, which is challenging.

        override val linearId: ChatID,
        override val created: Instant = Instant.now(),
        override val participants: List<AbstractParty>,
        val initiator: Party,
        val toAdd: List<Party>,
        val toRemove: List<Party>,
        val toAgreeParties: List<Party>,
        val agreedParties: MutableList<Party>,
        val status: UpdateParticipantsStatus = UpdateParticipantsStatus.PROPOSED,
        val includingHistoryChat: Boolean
) : ChatBaseState {
    override fun toString(): String {
        return "UpdateParticipantsState(linearId=$linearId, created=$created, participants=$participants, initiator=$initiator, toAdd=$toAdd, toRemove=$toRemove, toAgreeParties=$toAgreeParties, agreedParties=$agreedParties, status=$status, includingHistoryChat=$includingHistoryChat)"
    }
}

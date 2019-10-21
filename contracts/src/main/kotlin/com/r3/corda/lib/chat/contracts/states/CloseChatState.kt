package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.CloseChatContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
sealed class CloseChatStatus {
    object PROPOSED : CloseChatStatus()
    object AGREED : CloseChatStatus()
    object REJECTED : CloseChatStatus()
}

//  also add argument in the flows
@BelongsToContract(CloseChatContract::class)
data class CloseChatState(
        // close chat state does **not** need a unique ID, since we define the rule as:
        // if there is a close propose, then no one else could propose again, instead, agree is needed.
        override val linearId: ChatID,
        override val created: Instant = Instant.now(),
        override val participants: List<AbstractParty>,
        val initiator: Party,
        val toAgreeParties: List<Party>,
        val agreedParties: MutableList<Party>,
        val status: CloseChatStatus = CloseChatStatus.PROPOSED
) : ChatBaseState {
    override fun toString(): String {
        return "CloseChatState(linearId=$linearId, created=$created, participants=$participants, initiator=$initiator, toAgreeParties=$toAgreeParties, agreedParties=$agreedParties, status=$status)"
    }
}

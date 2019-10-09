package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatInfoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
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
@BelongsToContract(ChatInfoContract::class)
data class CloseChatState(
        // close chat state does **not** need a unique ID, since we define the rule as:
        // if there is a close propose, then no one else could propose again, instead, agree is needed.
        override val linearId: ChatID,
        override val created: Instant = Instant.now(),
        override val participants: List<AbstractParty>,
        val from: Party,
        val to: List<Party>,
        val status: CloseChatStatus = CloseChatStatus.PROPOSED
) : ChatBaseState {
    override fun toString(): String {
        return "CloseChatState(created=$created, from=$from, linearId=$linearId, participants=$participants)"
    }
}

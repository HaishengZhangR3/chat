package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatInfoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

/**
 * A state which records the participants update information: add or remove
 *
 */

@CordaSerializable
sealed class ParticipantsAction {
    object ADD : ParticipantsAction()
    object REMOVE : ParticipantsAction()
}

@BelongsToContract(ChatInfoContract::class)
data class ParticipantsUpdateState(
        val created: Instant = Instant.now(),
        val from: Party,
        val to: List<Party>,
        val action: ParticipantsAction,
        val includingHistoryChat: Boolean,
        val linearId: UniqueIdentifier
) : ContractState {
    override val participants: List<AbstractParty> get() = to + from
    override fun toString(): String {
        return "ParticipantsUpdateState(created=$created, from=$from, to=$to, action=$action, linearId=$linearId)"
    }
}

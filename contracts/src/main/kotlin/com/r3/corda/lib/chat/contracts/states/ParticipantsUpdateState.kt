package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatInfoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
sealed class ParticipantsAction {
    object ADD : ParticipantsAction()
    object REMOVE : ParticipantsAction()
}

@BelongsToContract(ChatInfoContract::class)
data class ParticipantsUpdateState(
        val created: Instant = Instant.now(),
        val from: Party,
        val toUpdate: List<Party>,
        val allParticipants: List<Party>,
        val action: ParticipantsAction,
        val includingHistoryChat: Boolean,
        override val linearId: UniqueIdentifier
) : LinearState {
    override val participants: List<AbstractParty> get() = allParticipants
    override fun toString(): String {
        return "ParticipantsUpdateState(created=$created, from=$from, toUpdate=$toUpdate, allParticipants=$allParticipants, action=$action, includingHistoryChat=$includingHistoryChat, linearId=$linearId)"
    }
}

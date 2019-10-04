package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsAgreeFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsProposeFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsRejectFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsProposeFlow(
        private val chatId: UniqueIdentifier,
        private val toAdd: List<Party>,
        private val includingHistoryChat: Boolean
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(UpdateParticipantsProposeFlow(
                chatId = chatId,
                toAdd = toAdd,
                includingHistoryChat = includingHistoryChat
        ))
    }
}

@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsAgreeFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(UpdateParticipantsAgreeFlow(
                chatId = chatId
        ))
    }
}


@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsRejectFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(UpdateParticipantsRejectFlow(
                chatId = chatId
        ))
    }
}

@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(UpdateParticipantsFlow(
                chatId = chatId,
                force = false
        ))
    }
}

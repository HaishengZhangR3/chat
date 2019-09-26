package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AddParticipants
import com.r3.corda.lib.chat.contracts.states.ParticipantsAction
import com.r3.corda.lib.chat.contracts.states.ParticipantsUpdateState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsProposeFlow(
        private val toAdd: List<Party>,
        private val includingHistoryChat: Boolean,
        private val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val inputChatInfo = ServiceUtils.getChatHead(serviceHub, linearId)

        val allParties = inputChatInfo.state.data.run { this.to + this.from + toAdd}
        val toParties = allParties - ourIdentity

        val participantsUpdate = ParticipantsUpdateState(
                from = ourIdentity,
                to = toAdd,
                action = ParticipantsAction.ADD,
                includingHistoryChat = includingHistoryChat,
                linearId = linearId
        )

        val txnBuilder = TransactionBuilder(notary = inputChatInfo.state.notary)
                .addInputState(inputChatInfo)
                .addOutputState(participantsUpdate)
                .addCommand(AddParticipants(), allParties.map { it.owningKey })
                .also {
                    it.verify(serviceHub)
                }

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = toParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))
        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(AddParticipantsProposeFlow::class)
class AddParticipantsProposeFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

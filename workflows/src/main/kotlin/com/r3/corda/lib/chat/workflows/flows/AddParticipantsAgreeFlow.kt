package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AddParticipants
import com.r3.corda.lib.chat.contracts.states.ChatID
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.ParticipantsUpdateState
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsAgreeFlow(
        private val txnId: SecureHash,
        private val chatId: ChatID
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val inputChatInfo = ServiceUtils.getChatHead(serviceHub, chatId)

        val txnToAgree = serviceHub.validatedTransactions.getTransaction(txnId) as SignedTransaction
        val stateToAgree = txnToAgree.coreTransaction.outputsOfType<ParticipantsUpdateState>().single()

        val allParties = inputChatInfo.state.data.run { this.to + this.from + stateToAgree.to}
        val toParties = allParties - stateToAgree.from

        val outputChatInfo = ChatInfo(
                from = stateToAgree.from,
                to = toParties,
                linearId = stateToAgree.linearId
        )

        val txnBuilder = TransactionBuilder(notary = inputChatInfo.state.notary)
                .addInputState(inputChatInfo)
                .addOutputState(outputChatInfo)
                .addCommand(AddParticipants(), listOf(ourIdentity, stateToAgree.from).map { it.owningKey })
                .also {
                    it.verify(serviceHub)
                }

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)

        val counterPartySession = initiateFlow(stateToAgree.from)
        counterPartySession.send(stateToAgree)
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, listOf(counterPartySession)))

        // finalise it
        return subFlow(FinalityFlow(collectSignTxn, counterPartySession))
    }
}

@InitiatedBy(AddParticipantsAgreeFlow::class)
class AddParticipantsAgreeFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val participantsUpdate = otherSession.receive<ParticipantsUpdateState>().unwrap{ it }

        // the proposer side is responsible of sending history chat to new added participants
        if (participantsUpdate.from == ourIdentity) {
            subFlow(SyncUpChatHistoryFlow(participantsUpdate.to, participantsUpdate.linearId))
        }

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

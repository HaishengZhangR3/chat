package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.states.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatFlow(
        private val linearId: UniqueIdentifier,
        private val force: Boolean = false
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // ask all parties to close the chats
        val allCloseStateRef = ServiceUtils.getActiveCloseChatStates(serviceHub, linearId)
        val proposedCloseStateRef = allCloseStateRef.filter { it.state.data.status == CloseChatStatus.PROPOSED }.first()
        val proposedCloseState = proposedCloseStateRef.state.data

        // need check whether all of the participants agreed already before close the chat
        requireThat { "Must the proposer are allowed to close the chat" using (proposedCloseState.from == ourIdentity) }
        if (!force) {
            CloseChatUtils.areAllCloseProposeAgreed(allCloseStateRef)
        }

        val allParties = (proposedCloseState.to + proposedCloseState.from + ourIdentity).distinct()
        val counterParties = allParties - ourIdentity

        // close by consuming ProposeClose state, need every parties to sign and close it
        val closeProposeTxn = TransactionBuilder(proposedCloseStateRef.state.notary)
                .addInputState(proposedCloseStateRef)
                .addCommand(Close(), allParties.map { it.owningKey })
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(closeProposeTxn)
        val counterPartySessions = counterParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        // consume all of the chat messages
        CloseChatUtils.closeChat(serviceHub, linearId, listOf(ourIdentity.owningKey))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

/**
 * This is the flow which responds to close chat.
 */
@InitiatedBy(CloseChatFlow::class)
class CloseChatFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {

                val closeChatState = serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data }.single() as CloseChatState
                CloseChatUtils.closeChat(serviceHub, closeChatState.linearId, listOf(ourIdentity.owningKey))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

object CloseChatUtils {

    fun closeChat(serviceHub: ServiceHub, linearId: ChatID, participants: List<PublicKey>): Unit {

        // get and consume all messages in vault
        val allMessagesStateRef = ServiceUtils.getActiveChats(serviceHub, linearId)
        requireThat { "There must be message in vault" using (allMessagesStateRef.isNotEmpty()) }

        val anyMessageStateRef = allMessagesStateRef.first()
        val txnBuilder = TransactionBuilder(notary = anyMessageStateRef.state.notary)
                .addCommand(Close(), participants)
        allMessagesStateRef.forEach { txnBuilder.addInputState(it) }
        txnBuilder.verify(serviceHub)

        // sign it
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
    }

    // check whether all of the participants agreed
    fun areAllCloseProposeAgreed(allCloseStateRef: List<StateAndRef<CloseChatState>>){

        allCloseStateRef.map { it.state.data }.let {
            val proposed  = it.filter { it.status == CloseChatStatus.PROPOSED }.size
            requireThat { "There should be at most 1 proposal." using (proposed == 1) }

            val rejected  = it.filter { it.status == CloseChatStatus.REJECTED }.size
            requireThat { "There should be not rejection." using (rejected == 0) }

            val proposedState = it.single { it.status == CloseChatStatus.PROPOSED }
            val partyAmount =  proposedState.to.size - 1 // proposer agreed by default
            val agreed = it.filter { it.status == CloseChatStatus.AGREED }.size
            requireThat { "Not all participants agreed." using (partyAmount == agreed) }
        }
    }
}
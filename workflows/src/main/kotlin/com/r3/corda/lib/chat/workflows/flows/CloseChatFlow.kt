package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

/**
 * A flow to close a chat.
 *
 */
@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatFlow(
        private val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val allMessagesStateRef = ServiceUtils.getActiveChats(serviceHub, linearId)
        val headMessageStateRef = allMessagesStateRef.sortedByDescending { it.state.data.created }.first()
        val headMessage = headMessageStateRef.state.data

        val allParties = (headMessage.to + headMessage.from + ourIdentity).distinct()
        val counterParties = allParties - ourIdentity

        val txnBuilder = TransactionBuilder(headMessageStateRef.state.notary)
                .addCommand(Close(), allParties.map { it.owningKey })
        allMessagesStateRef.forEach { txnBuilder.addInputState(it) }
        txnBuilder.verify(serviceHub)

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = counterParties.map { initiateFlow(it) }
        counterPartySessions.map { it.send(headMessage) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

/**
 * This is the flow which responds to close chat.
 */
@InitiatedBy(CloseChatFlow::class)
class CloseChatFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val otherChatInfo = otherSession.receive<ChatInfo>().unwrap{it}
        closeChat(serviceHub, otherChatInfo)

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {
            }
        }
        val signTxn = subFlow(transactionSigner)

        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }

    // consume all of the chat messages and close it
    private fun closeChat(serviceHub: ServiceHub, otherChatInfo: ChatInfo): Unit {

        // get and consume all messages in vault
        val allMessagesStateRef = ServiceUtils.getActiveChats(serviceHub, otherChatInfo.linearId)
        requireThat { "There must be message in vault" using (allMessagesStateRef.isNotEmpty()) }

        val anyMessageStateRef = allMessagesStateRef.first()
        val txnBuilder = TransactionBuilder(notary = anyMessageStateRef.state.notary)
                .addCommand(Close(), listOf(ourIdentity.owningKey))
        allMessagesStateRef.forEach { txnBuilder.addInputState(it) }
        txnBuilder.verify(serviceHub)

        // sign it
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
    }
}

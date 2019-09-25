package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
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
        private val from: Party,
        private val to: List<Party>,
        private val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val inputChatInfo = ServiceUtils.getChatHead(serviceHub, linearId)
        val allParties = (to + from).toSet()
        val toParties = (to - from).toSet()
        val txnBuilder = TransactionBuilder(notary = inputChatInfo.state.notary)
                .addCommand(Close(), allParties.map { it.owningKey })
                .addInputState(inputChatInfo)
                .also {
                    it.verify(serviceHub)
                }


        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)

        val counterPartySessions = toParties.map { initiateFlow(it) }

        counterPartySessions.map { it.send(inputChatInfo.state.data) }
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

        val othersideChatInfo = otherSession.receive<ChatInfo>().unwrap{it}
        closeChat(serviceHub, othersideChatInfo)

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {
            }
        }
        val signTxn = subFlow(transactionSigner)

        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }

    // close it with
    private fun closeChat(serviceHub: ServiceHub, othersideChatInfo: ChatInfo): Unit {
        val inputChatInfo = ServiceUtils.getChatHead(serviceHub, othersideChatInfo.linearId)
        val txnBuilder = TransactionBuilder(notary = inputChatInfo.state.notary)
                // no output
                .addInputState(inputChatInfo)
                .addCommand(Close(), listOf(ourIdentity.owningKey))
                .also {
                    it.verify(serviceHub)
                }

        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
    }
}

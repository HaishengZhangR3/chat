package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * A flow to close a chat.
 *
 */
@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatFlow(
        val from: Party,
        val to: List<Party>,
        val linearId: UniqueIdentifier
) : FlowLogic<StateAndRef<ChatInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatInfo> {

        val inputChatInfo = ServiceUtils.getChatHead(serviceHub, linearId)
        val txnBuilder = TransactionBuilder(notary = inputChatInfo.state.notary)
                .addCommand(Close(), from.owningKey)
                .addInputState(inputChatInfo)
                .also {
                    it.verify(serviceHub)
                }

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)

        val counterPartySessions = (to - from).toSet().map { initiateFlow(it) }

        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        val finalFlow = subFlow(FinalityFlow(collectSignTxn, counterPartySessions))

        return finalFlow.coreTransaction.outRefsOfType(ChatInfo::class.java).single()
    }
}

/**
 * This is the flow which responds to close chat.
 */
@InitiatedBy(CloseChatFlow::class)
class CloseChatFlowResponder(val otherSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat{
                val input = stx.inputs.size
                "Should be only 1 input" using (input == 1)
            }
        }

        val signTxn = subFlow(transactionSigner)
        subFlow(ReceiveFinalityFlow(
                otherSideSession =  otherSession,
                expectedTxId = signTxn.id,
                statesToRecord = StatesToRecord.ALL_VISIBLE
                ))
    }
}

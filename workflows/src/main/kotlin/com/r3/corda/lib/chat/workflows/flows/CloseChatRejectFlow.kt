package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.RejectClose
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.CloseChatUtils
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatRejectFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // @todo: code duplicat between all of the close flow?
        // @todo: close should not be allowed if there is no propose
        // @todo: reject the proposal from who? how to decide which proposal?
        val allCloseProposeStateRef = CloseChatUtils.getAllCloseStates(this, chatId)
        val headState = CloseChatUtils.getCloseProposeState(allCloseProposeStateRef)

        val allParties = (headState.state.data.toAgreeParties + headState.state.data.initiator).distinct()
        val counterParties = allParties - ourIdentity

        val txnBuilder = TransactionBuilder(headState.state.notary)
                .addCommand(RejectClose(), allParties.map { it.owningKey })
        allCloseProposeStateRef.map { txnBuilder.addInputState( it ) }
        txnBuilder.verify(serviceHub)

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = counterParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        // notify observers (including myself), if the app is listening
        subFlow(ChatNotifyFlow(info = headState.state.data, command = RejectClose()))
        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(CloseChatRejectFlow::class)
class CloseChatRejectFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction): Unit {
                val closeChatState = serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data }.first() as CloseChatState
                println("Close request is rejected: ${closeChatState}.")

                // notify observers (including myself), if the app is listening
                subFlow(ChatNotifyFlow(info = closeChatState, command = RejectClose()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

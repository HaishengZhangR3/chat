package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.RejectClose
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

        val allCloseProposeStateRef = CloseChatUtils.getAllCloseStates(serviceHub, chatId)
        val headState = CloseChatUtils.getCloseProposeState(allCloseProposeStateRef)

        val allParties = (headState.state.data.to + headState.state.data.from).distinct()
        val counterParties = allParties - ourIdentity

        val txnBuilder = TransactionBuilder(headState.state.notary)
                .addCommand(RejectClose(), allParties.map { it.owningKey })
        allCloseProposeStateRef.map { txnBuilder.addInputState( it ) }
        txnBuilder.verify(serviceHub)

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = counterParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(CloseChatRejectFlow::class)
class CloseChatRejectFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
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

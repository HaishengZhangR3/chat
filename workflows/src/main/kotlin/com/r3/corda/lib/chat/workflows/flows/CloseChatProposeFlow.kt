package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.ProposeClose
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatProposeFlow(
        private val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // @todo: if there has been a proposal on the chat, then no need to propose again
        val headMessageStateRef = ServiceUtils.getChatHead(serviceHub, linearId)
        val headMessage = headMessageStateRef.state.data

        val allParties = (headMessage.to + headMessage.from + ourIdentity).distinct()
        val counterParties = allParties - ourIdentity

        val proposeState = CloseChatState(
                linearId = linearId,
                from = ourIdentity,
                to = allParties,
                participants = allParties
        )
        val txnBuilder = TransactionBuilder(headMessageStateRef.state.notary)
                .addOutputState(proposeState)
                .addCommand(ProposeClose(), allParties.map { it.owningKey })
        txnBuilder.verify(serviceHub)

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = counterParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(CloseChatProposeFlow::class)
class CloseChatProposeFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
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

package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AgreeClose
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.contracts.states.CloseChatStatus
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatAgreeFlow(
        private val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val headMessageStateRef = ServiceUtils.getChatHead(serviceHub, linearId)
        val headMessage = headMessageStateRef.state.data

        val allParties = (headMessage.to + headMessage.from + ourIdentity).distinct()
        val counterParties = allParties - ourIdentity

        val proposeState = CloseChatState(
                linearId = linearId,
                from = ourIdentity,
                to = allParties,
                status = CloseChatStatus.AGREED,
                participants = allParties
        )
        val txnBuilder = TransactionBuilder(headMessageStateRef.state.notary)
                // no input
                .addOutputState(proposeState)
                .addCommand(AgreeClose(), allParties.map { it.owningKey })
        txnBuilder.verify(serviceHub)

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = counterParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(CloseChatAgreeFlow::class)
class CloseChatFlowAgreeResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {

                // @todo: how many "agree" we've got? if all agreed (including me),
                // @todo: then the proposer should close the chat calling CloseChatFlow

            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }

}

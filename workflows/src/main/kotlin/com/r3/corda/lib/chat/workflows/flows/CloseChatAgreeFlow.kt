package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AgreeClose
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.contracts.states.CloseChatStatus
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatAgreeFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val allCloseStateRef = CloseChatUtils.getAllCloseStates(serviceHub, chatId)
        val proposedCloseStateRef = CloseChatUtils.getCloseProposeState(allCloseStateRef)
        val proposedCloseState = proposedCloseStateRef.state.data
        requireThat { "Don't need to agree on the proposal you raised." using (proposedCloseState.from != ourIdentity) }

        val allParties = (proposedCloseState.to + proposedCloseState.from).distinct()
        val counterParties = allParties - ourIdentity

        val proposeState = CloseChatState(
                linearId = chatId,
                from = ourIdentity,
                to = allParties,
                status = CloseChatStatus.AGREED,
                participants = allParties
        )
        val txnBuilder = TransactionBuilder(proposedCloseStateRef.state.notary)
                // no input
                .addOutputState(proposeState)
                .addCommand(AgreeClose(), allParties.map { it.owningKey })
                .also { it.verify(serviceHub) }

        // need counter party to sign and close it
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
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }

}
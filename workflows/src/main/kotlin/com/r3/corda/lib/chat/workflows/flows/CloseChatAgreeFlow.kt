package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AgreeClose
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.contracts.states.CloseChatStatus
import com.r3.corda.lib.chat.workflows.flows.utils.CloseChatUtils
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

        // @todo: propose should fail if there is no chat in vault
        val allCloseStateRef = CloseChatUtils.getAllCloseStates(this, chatId)
        val proposedCloseStateRef = CloseChatUtils.getCloseProposeState(allCloseStateRef)
        val proposedCloseState = proposedCloseStateRef.state.data
        requireThat { "Don't need to agree on the proposal you raised." using (proposedCloseState.initiator != ourIdentity) }

        val allParties = (proposedCloseState.toAgreeParties + proposedCloseState.initiator).distinct()
        val counterParties = allParties - ourIdentity
        val agreedParties = proposedCloseState.agreedParties.toMutableList().also { it.add(ourIdentity) }

        val proposeState = CloseChatState(
                linearId = chatId,
                initiator = ourIdentity,
                toAgreeParties = allParties,
                agreedParties = agreedParties,
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
                val close = stx.tx.outputStates.single() as CloseChatState
                println("""
                    | Got close chat agreement: ${close}.
                    | If all agreed, please do final close.
                """.trimMargin())
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }

}

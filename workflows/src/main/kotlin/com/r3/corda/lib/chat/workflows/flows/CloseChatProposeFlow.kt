package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.ProposeClose
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatProposeFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // @todo: propose should fail if there is no chat in vault
        // @todo: if there is a proposal on the chat from anyone, then no need to propose again
        val headMessageStateRef = chatVaultService.getHeadMessage(chatId)
        val headMessage = headMessageStateRef.state.data

        val allParties = (headMessage.receivers + headMessage.sender + ourIdentity).distinct()
        val counterParties = allParties - ourIdentity

        val proposeState = CloseChatState(
                linearId = chatId,
                initiator = ourIdentity,
                toAgreeParties = allParties,
                agreedParties = mutableListOf(),
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

        // notify observers (including myself), if the app is listening
        subFlow(ChatNotifyFlow(info = proposeState, command = ProposeClose()))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(CloseChatProposeFlow::class)
class CloseChatProposeFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction): Unit {
                val close = stx.tx.outputStates.single() as CloseChatState
                println("""
                    | Got close chat proposal: ${close}.
                    | please agree.
                """.trimMargin())

                // notify observers (including myself), if the app is listening
                subFlow(ChatNotifyFlow(info = close, command = ProposeClose()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }

}

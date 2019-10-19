package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.RejectUpdateParticipants
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsStatus
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class UpdateParticipantsRejectFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // @todo: should be no reject if there is no propose
        val allUpdateStateRef = chatVaultService.getActiveParticipantsUpdateStates(chatId)
        val proposeState = allUpdateStateRef.first{it.state.data.status == UpdateParticipantsStatus.PROPOSED}
        val proposeData = proposeState.state.data

        val needSigns = proposeData.participants.map { it as Party }
        val counterParties = needSigns - ourIdentity

        val txnBuilder = TransactionBuilder(proposeState.state.notary)
                .addCommand(RejectUpdateParticipants(), needSigns.map { it.owningKey })
        allUpdateStateRef.map { txnBuilder.addInputState( it ) }
        txnBuilder.verify(serviceHub)

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = counterParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        // notify observers (including myself), if the app is listening
        subFlow(ChatNotifyFlow(info = proposeState, command = RejectUpdateParticipants()))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(UpdateParticipantsRejectFlow::class)
class UpdateParticipantsRejectFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction): Unit {
                val update = serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data }.first() as UpdateParticipantsState
                println("Update participants request is rejected: ${update}.")

                // notify observers (including myself), if the app is listening
                subFlow(ChatNotifyFlow(info = update, command = RejectUpdateParticipants()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AgreeUpdateParticipants
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsStatus
import com.r3.corda.lib.chat.workflows.flows.utils.ServiceUtils
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

@InitiatingFlow
@StartableByService
@StartableByRPC
class UpdateParticipantsAgreeFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val allUpdateStateRef = ServiceUtils.getActiveParticipantsUpdateStates(serviceHub, chatId)
        val proposeState = allUpdateStateRef.first{it.state.data.status == UpdateParticipantsStatus.PROPOSED}
        val proposeData = proposeState.state.data

        val needSigns = proposeData.participants.map { it as Party }
        val counterParties = needSigns - ourIdentity

        val participantsUpdate = proposeData.copy(
                created = Instant.now(),
                from = ourIdentity,
                status = UpdateParticipantsStatus.AGREED
        )

        val txnBuilder = TransactionBuilder(notary = proposeState.state.notary)
                .addOutputState(participantsUpdate)
                .addCommand(AgreeUpdateParticipants(), needSigns.map { it.owningKey })
                .also { it.verify(serviceHub) }

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = counterParties.map{ initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))
        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(UpdateParticipantsAgreeFlow::class)
class UpdateParticipantsAgreeFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction): Unit {
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}
package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.ProposeUpdateParticipants
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class UpdateParticipantsProposeFlow(
        private val chatId: UniqueIdentifier,
        private val toAdd: List<Party> = emptyList(),
        private val toRemove: List<Party> = emptyList(),
        private val includingHistoryChat: Boolean = false
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // @todo: there should not be more than one proposals for the chat
        val headChatStateRef = chatVaultService.getHeadMessage(chatId)
        val headChat = headChatStateRef.state.data

        // only the remaining parties (no new added, nor to remove) are needed to sign
        // val allParties = (headChat.to + headChat.from + toAdd - toRemove).distinct()
        val needSigns = (headChat.receivers + headChat.sender - toRemove).distinct()
        requireThat { "Cannot remove every participants." using needSigns.isNotEmpty() }
        // @todo: new added must not be in existing list
        // @todo: toRemove must be in existing list
        // @todo: can move myself? yes, don't block this logic

        val toParties = needSigns - ourIdentity

        val participantsUpdate = UpdateParticipantsState(
                linearId = chatId,
                participants = needSigns,
                initiator = ourIdentity,
                toAdd = toAdd,
                toRemove = toRemove,
                toAgreeParties = needSigns,
                agreedParties = mutableListOf(),
                includingHistoryChat = includingHistoryChat
        )

        val txnBuilder = TransactionBuilder(notary = headChatStateRef.state.notary)
                .addOutputState(participantsUpdate)
                .addCommand(ProposeUpdateParticipants(), needSigns.map { it.owningKey })
                .also {
                    it.verify(serviceHub)
                }

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySessions = toParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        // notify observers (including myself), if the app is listening
        subFlow(ChatNotifyFlow(info = participantsUpdate, command = ProposeUpdateParticipants()))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(UpdateParticipantsProposeFlow::class)
class UpdateParticipantsProposeFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction): Unit {
                val update = stx.tx.outputStates.single() as UpdateParticipantsState
                println("""
                    | Got update participants proposal: ${update}.
                    | toAdd = ${update.toAdd},
                    | toRemove = ${update.toRemove},
                    | please agree using add agree or remove agree properly.
                """.trimMargin())

                // notify observers (including myself), if the app is listening
                subFlow(ChatNotifyFlow(info = update, command = ProposeUpdateParticipants()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

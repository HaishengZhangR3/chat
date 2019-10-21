package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.UpdateParticipants
import com.r3.corda.lib.chat.contracts.states.ChatMessageType
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsStatus
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class UpdateParticipantsFlow(
        private val chatId: UniqueIdentifier,
        private val force: Boolean = false
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val allUpdateStateRef = chatVaultService.getActiveParticipantsUpdateStates(chatId)
        val proposeState = allUpdateStateRef.first{it.state.data.status == UpdateParticipantsStatus.PROPOSED}
        val proposeData = proposeState.state.data

        // @todo: updating should not be allowed if there is no propose
        requireThat { "Only the proposer are allowed to close the chat" using (proposeData.initiator == ourIdentity) }
        if (!force) {
            areAllUpdateProposeAgreed(allUpdateStateRef)
        }

        // steps:
        // 1. consume all propose and agree states for all parties who need sign, need all sign
        closeUpdateStates(allUpdateStateRef)

        // 2. counter party close the chat with consuming all of the history chats
        subFlow(ShutDownChatFlow(chatId = chatId, toList = proposeData.toRemove))

        // 3. share history messages to toAdd (subflow), no need sign
        subFlow(ShareChatHistoryFlow(chatId = proposeData.linearId, receivers = proposeData.toAdd))

        // 4. notice with a new txn output = chatInfo whose toList == old toList + toAdd - toRemove, need all sign
        return updateParticipants(proposeData)
    }

    @Suspendable
    private fun closeUpdateStates(allStates: List<StateAndRef<UpdateParticipantsState>>): SignedTransaction {
        val proposeState = allStates.first{it.state.data.status == UpdateParticipantsStatus.PROPOSED}
        val proposeData = proposeState.state.data

        val signParties = proposeData.participants.map { it as Party }
        val counterParties = signParties - ourIdentity
        val txnBuilder = TransactionBuilder(notary = proposeState.state.notary)
                .addCommand(UpdateParticipants(), signParties.map { it.owningKey })
        allStates.map{ txnBuilder.addInputState(it) }
        txnBuilder.also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySession = counterParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySession))

        // notify observers (including myself), if the app is listening
        subFlow(ChatNotifyFlow(info = proposeData, command = UpdateParticipants()))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySession))
    }

    @Suspendable
    private fun updateParticipants(proposeData: UpdateParticipantsState): SignedTransaction {
        val headChat = chatVaultService.getHeadMessage(chatId).state.data
        val toList = (headChat.receivers + headChat.sender + proposeData.toAdd - proposeData.toRemove - ourIdentity).distinct()

        return subFlow(SendMessageFlow(
                receivers = toList,
                chatId = chatId,
                chatMessageType = ChatMessageType.SYSTEM
        ))
    }

    private fun areAllUpdateProposeAgreed(allStates: List<StateAndRef<UpdateParticipantsState>>) {
        // @todo: same as mentioned before: how to differentiate between proposal from different participant?
        allStates.map { it.state.data }.let {
            val proposed = it.filter { it.status == UpdateParticipantsStatus.PROPOSED }.size
            requireThat { "There should be at most 1 proposal." using (proposed == 1) }

            val rejected = it.filter { it.status == UpdateParticipantsStatus.REJECTED }.size
            requireThat { "There should be not rejection." using (rejected == 0) }

            val proposedState = it.single { it.status == UpdateParticipantsStatus.PROPOSED }
            val partyAmount = proposedState.toAgreeParties.size - 1 // proposer agreed by default
            val agreed = it.filter { it.status == UpdateParticipantsStatus.AGREED }.size
            requireThat { "Not all participants agreed." using (partyAmount == agreed) }
        }
    }
}

@InitiatedBy(UpdateParticipantsFlow::class)
class UpdateParticipantsFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction): Unit {
                // no log, since the log is SendMessageResponder.
                val proposeData = serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data }.first() as UpdateParticipantsState

                // notify observers (including myself), if the app is listening
                subFlow(ChatNotifyFlow(info = proposeData, command = UpdateParticipants()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

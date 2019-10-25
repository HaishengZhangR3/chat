package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.RemoveParticipants
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByService
@StartableByRPC
class RemoveParticipantsFlow(
        private val chatId: UniqueIdentifier,
        private val toRemove: List<Party>
) : FlowLogic<StateAndRef<ChatMetaInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatMetaInfo> {

        // get and consume all messages in vault
        val metaInfoStateAndRef = chatVaultService.getMetaInfo(chatId)
        val metaInfo = metaInfoStateAndRef.state.data
        if (ourIdentity != metaInfo.admin) {
            throw FlowException("Only chat admin can remove participants to chat.")
        }

        // steps:
        // 1. close current ChatMetaInfo, need (all) sign
        subFlow(CloseMetaInfoFlow(chatId))

        // 2. create new ChatMetaInfo without removed parties, need (all - removed) sign
        val newReceivers = metaInfo.receivers - toRemove
        val txn = subFlow(CreateMetaInfoFlow(chatId, newReceivers))

        subFlow(ChatNotifyFlow(info = listOf(txn.state.data), command = RemoveParticipants()))
        return txn
    }
}

@InitiatedBy(RemoveParticipantsFlow::class)
class RemoveParticipantsFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) {
                val metaInfo = stx.coreTransaction.outputStates.single() as ChatMetaInfo

                // notify observers (including myself), if the app is listening
                subFlow(ChatNotifyFlow(info = listOf(metaInfo), command = RemoveParticipants()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

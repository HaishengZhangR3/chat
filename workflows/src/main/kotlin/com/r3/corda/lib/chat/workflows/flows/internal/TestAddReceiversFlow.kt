package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AddParticipants
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class TestAddReceiversFlow(
        private val chatId: UniqueIdentifier,
        private val toAdd: List<Party>
) : FlowLogic<StateAndRef<ChatMetaInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatMetaInfo> {

        // get and consume all messages in vault
        val metaInfoStateAndRef = chatVaultService.getMetaInfo(chatId)
        val metaInfo = metaInfoStateAndRef.state.data

        requireThat {
            "Only chat admin can add participants to chat." using (ourIdentity == metaInfo.admin)
            "List to be added should not be empty." using (toAdd.isNotEmpty())
            "There should not be duplicte in adding list." using (toAdd.distinct().size == toAdd.size)
            "Cannot add participants who have already been in chat." using (metaInfo.receivers.intersect(toAdd).isEmpty())
        }

        val allReceivers = metaInfo.receivers + toAdd
        val newMetaInfo = metaInfo.copy(
                receivers = allReceivers
        )
        val txnBuilder = TransactionBuilder(notary = metaInfoStateAndRef.state.notary)
            // @todo: if add this, it'll fail    .addInputState(metaInfoStateAndRef)
                .addOutputState(newMetaInfo)
                .addCommand(AddParticipants(), ourIdentity.owningKey)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(selfSignedTxn)
        allReceivers.map { initiateFlow(it) }
                .map { subFlow(SendTransactionFlow(it, selfSignedTxn)) }

        subFlow(ChatNotifyFlow(info = listOf(newMetaInfo), command = AddParticipants()))
        return selfSignedTxn.coreTransaction.outRefsOfType<ChatMetaInfo>().single()
    }
}

@InitiatedBy(TestAddReceiversFlow::class)
class TestAddReceiversFlowResponder(private val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val txn = subFlow(ReceiveTransactionFlow(
                otherSideSession =  otherSession,
                checkSufficientSignatures = true,
                statesToRecord = StatesToRecord.ALL_VISIBLE))
        val metaInfo = txn.coreTransaction.outputStates.single() as ChatMetaInfo
        subFlow(ChatNotifyFlow(info = listOf(metaInfo), command = AddParticipants()))
        return txn
    }
}

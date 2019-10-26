package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.CloseMessages
import com.r3.corda.lib.chat.contracts.states.ChatID
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseMessagesFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // get and consume all messages in vault
        val metaInfoStateAndRef = chatVaultService.getMetaInfo(chatId)
        if (ourIdentity != metaInfoStateAndRef.state.data.admin) {
            throw FlowException("Only chat admin can close the chat.")
        }

        val txn = CloseChatMessagesUtil.close(this, chatId)
        CloseChatMessagesUtil.closeCounterParties(this, chatId)
        return txn
    }
}

@InitiatedBy(CloseMessagesFlow::class)
class CloseMessagesFlowResponder(private val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val chatId = otherSession.receive<ChatID>().unwrap { it }
        return CloseChatMessagesUtil.close(this, chatId)
    }
}

private object CloseChatMessagesUtil {
    @Suspendable
    fun close(flow: FlowLogic<SignedTransaction>, chatId: UniqueIdentifier): SignedTransaction {

        // get and consume all messages in vault
        val metaInfoStateAndRef = flow.chatVaultService.getMetaInfo(chatId)
        val metaInfo = metaInfoStateAndRef.state.data

        val messagesStateRef = flow.chatVaultService.getActiveMessages(chatId)
        requireThat { "There must be message in vault" using (messagesStateRef.isNotEmpty()) }

        val txnBuilder = TransactionBuilder(notary = metaInfoStateAndRef.state.notary)
                .addCommand(CloseMessages(), flow.ourIdentity.owningKey)
                .addReferenceState(metaInfoStateAndRef.referenced())
        messagesStateRef.forEach { txnBuilder.addInputState(it) }
        txnBuilder.verify(flow.serviceHub)

        // sign it and save it
        val selfSignedTxn = flow.serviceHub.signInitialTransaction(txnBuilder)
        flow.serviceHub.recordTransactions(selfSignedTxn)

        // notify observers (including myself), if the app is listening
        flow.subFlow(ChatNotifyFlow(info = listOf(metaInfo), command = CloseMessages()))
        return selfSignedTxn
    }

    @Suspendable
    fun closeCounterParties(flow: FlowLogic<SignedTransaction>, chatId: UniqueIdentifier){
        val metaInfoStateAndRef = flow.chatVaultService.getMetaInfo(chatId)
        val metaInfo = metaInfoStateAndRef.state.data
        metaInfo.receivers.map { flow.initiateFlow(it).send(metaInfo.linearId) }
    }
}
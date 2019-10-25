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
        val txn = CloseChatMessagesUtil.close(this, chatId)
        CloseChatMessagesUtil.closeCounterParties(this, chatId)
        return txn
    }
}

@InitiatedBy(CloseMessagesFlow::class)
class CloseMessagesFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val chatId = otherSession.receive<ChatID>().unwrap { it }

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) {
                CloseChatMessagesUtil.close(this, chatId)
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

private object CloseChatMessagesUtil {
    @Suspendable
    fun close(flow: FlowLogic<SignedTransaction>, chatId: UniqueIdentifier): SignedTransaction {

        // get and consume all messages in vault
        val messagesStateRef = flow.chatVaultService.getActiveMessages(chatId)
        requireThat { "There must be message in vault" using (messagesStateRef.isNotEmpty()) }

        val metaInfoStateAndRef = flow.chatVaultService.getActiveMetaInfo(chatId)
        requireThat { "There must be message in vault" using (metaInfoStateAndRef != null) }
        metaInfoStateAndRef!!
        val messages = messagesStateRef.map { it.state.data }

        val txnBuilder = TransactionBuilder(notary = metaInfoStateAndRef.state.notary)
                .addCommand(CloseMessages(), messages.first().participants.map { it.owningKey })
                .addReferenceState(metaInfoStateAndRef.referenced())
        messagesStateRef.forEach { txnBuilder.addInputState(it) }
        txnBuilder.verify(flow.serviceHub)

        // sign it and save it
        val selfSignedTxn = flow.serviceHub.signInitialTransaction(txnBuilder)
        flow.serviceHub.recordTransactions(selfSignedTxn)

        // notify observers (including myself), if the app is listening
        flow.subFlow(ChatNotifyFlow(info = messages, command = CloseMessages()))
        return selfSignedTxn
    }

    @Suspendable
    fun closeCounterParties(flow: FlowLogic<SignedTransaction>, chatId: UniqueIdentifier){
        val metaInfoStateAndRef = flow.chatVaultService.getActiveMetaInfo(chatId)
        val metaInfo = metaInfoStateAndRef!!.state.data
        metaInfo.receivers.map { flow.initiateFlow(it).send(metaInfo.linearId) }
    }
}
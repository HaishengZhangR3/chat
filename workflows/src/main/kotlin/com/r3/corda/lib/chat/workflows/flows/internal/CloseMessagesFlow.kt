package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.CloseMessages
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseMessagesFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction  {

        // get and consume all messages in vault
        val metaInfoStateAndRef = chatVaultService.getMetaInfo(chatId)
        val messagesStateRef = chatVaultService.getChatActiveMessages(chatId)
        requireThat { "There must be message in vault" using (messagesStateRef.isNotEmpty()) }

        val txnBuilder = TransactionBuilder(notary = metaInfoStateAndRef.state.notary)
                .addReferenceState(metaInfoStateAndRef.referenced())
                .addCommand(CloseMessages(), ourIdentity.owningKey)
        messagesStateRef.forEach { txnBuilder.addInputState(it) }
        txnBuilder.verify(serviceHub)

        // sign it and save it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(selfSignedTxn)

        return selfSignedTxn
    }
}
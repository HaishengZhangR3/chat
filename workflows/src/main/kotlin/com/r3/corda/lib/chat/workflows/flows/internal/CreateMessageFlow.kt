package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.CreateMessage
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByService
@StartableByRPC
// @todo: go through all of the flows, some of them should not be exposed to RPC public
class CreateMessageFlow(
        private val chatId: UniqueIdentifier,
        private val content: String
) : FlowLogic<StateAndRef<ChatMessage>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatMessage> {
        val metaStateRef = chatVaultService.getMetaInfo(chatId)

        val chatMessage = ChatMessage(
                chatId = chatId,
                content = content,
                sender = ourIdentity,
                participants = listOf(ourIdentity)
        )

        val txnBuilder = TransactionBuilder(notary = chatVaultService.notary())
                .addReferenceState(metaStateRef.referenced())
                .addOutputState(chatMessage)
                .addCommand(CreateMessage(), ourIdentity.owningKey)
                .also {
                    it.verify(serviceHub)
                }

        // message will send to "to" list.
        val allReceivers = with (metaStateRef.state.data) { receivers + admin - ourIdentity }
        allReceivers.map { initiateFlow(it).send(chatMessage) }

        // save to vault
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)

        return signedTxn.coreTransaction.outRefsOfType<ChatMessage>().single()
    }
}

@InitiatedBy(CreateMessageFlow::class)
class CreateMessageFlowResponder(private val otherSession: FlowSession) : FlowLogic<StateAndRef<ChatMessage>>() {
    @Suspendable
    override fun call(): StateAndRef<ChatMessage> {

        // "receive" a message, then save to vault.
        val chatMessage = otherSession.receive<ChatMessage>().unwrap { it }
        val metaStateRef = chatVaultService.getMetaInfo(chatMessage.chatId)

        val newChatMessage = chatMessage.copy(participants = listOf(ourIdentity))
        val txnBuilder = TransactionBuilder(notary = metaStateRef.state.notary)
                .addReferenceState(metaStateRef.referenced())
                .addOutputState(newChatMessage)
                .addCommand(CreateMessage(), ourIdentity.owningKey)
                .also {
                    it.verify(serviceHub)
                }

        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
        return signedTxn.coreTransaction.outRefsOfType<ChatMessage>().single()
    }
}

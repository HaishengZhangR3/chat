package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Reply
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.utils.ServiceUtils
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

/**
 * A flow to reply a chat.
 *
 */
@InitiatingFlow
@StartableByService
@StartableByRPC
class SendMessageFlow(
        private val chatId: UniqueIdentifier,
        private val to: List<Party>,
        private val subject: String,
        private val content: String = "",
        private val attachment: SecureHash? = null
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val headMessageState = ServiceUtils.getChatHead(serviceHub, chatId)
        val outputChatInfo = ChatInfo(
                linearId = chatId,
                subject = subject,
                content = content,
                attachment = attachment,
                from = ourIdentity,
                to = to,
                participants = listOf(ourIdentity)
        )

        val txnBuilder = TransactionBuilder(notary = headMessageState.state.notary)
                // no any input
                .addOutputState(outputChatInfo)
                .addCommand(Reply(), ourIdentity.owningKey)
                .also {
                    it.verify(serviceHub)
                }

        // reply message will send to "to" list.
        to.map { initiateFlow(it).send(outputChatInfo) }

        // save to vault
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
        return signedTxn
    }
}

/**
 * This is the flow which responds to reply chat.
 */
@InitiatedBy(SendMessageFlow::class)
class SendMessageFlowResponder(private val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // "receive" a message, then save to vault.
        val chatInfo = flowSession.receive<ChatInfo>().unwrap { it }
        val headMessage = ServiceUtils.getChatHead(serviceHub, chatInfo.linearId)

        val txnBuilder = TransactionBuilder(notary = headMessage.state.notary)
                .addOutputState(chatInfo.copy(participants = listOf(ourIdentity)))
                .addCommand(Reply(), listOf(ourIdentity.owningKey))
                .also {
                    it.verify(serviceHub)
                }

        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
        return signedTxn
    }
}

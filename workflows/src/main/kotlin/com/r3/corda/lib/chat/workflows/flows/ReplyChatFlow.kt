package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Reply
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

/**
 * A flow to create a new chat.
 *
 */
@InitiatingFlow
@StartableByService
@StartableByRPC
class ReplyChatFlow(
        val subject: String,
        val content: String,
        val attachment: SecureHash?,
        val from: Party,
        val to: List<Party>,
        val linearId: UniqueIdentifier
) : FlowLogic<StateAndRef<ChatInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatInfo> {

        // reply which chat thread? should get the head of the chat thread from storage based on the linearId
        val inputChatInfo = ServiceUtils.getChatHead(serviceHub, linearId)
        val outputChatInfo = ChatInfo(
                subject = subject,
                content = content,
                attachment = attachment,
                from = from,
                to = to,
                linearId = linearId
        )

        // @TODO: should use the same notary as before in the chat thread
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val txnBuilder = TransactionBuilder(notary = notary)
                .addInputState(inputChatInfo)
                .addOutputState(outputChatInfo)
                .addCommand(Reply(), from.owningKey)
                .also {
                    it.verify(serviceHub)
                }

        // send to "to" list.
        to.map { initiateFlow(it).send(outputChatInfo) }

        // save to vault
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)

        return ServiceUtils.getChatHead(serviceHub, linearId)
    }
}

/**
 * This is the flow which responds to reply chat.
 */
@InitiatedBy(ReplyChatFlow::class)
class ReplyChatFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        // "receive" a message, then save to vault.
        // even when the node is off for a long time, still the chat will not be blocked by me
        val chatInfo = flowSession.receive<ChatInfo>().unwrap{ it }
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txnBuilder = TransactionBuilder(notary = notary)
                .addOutputState(chatInfo)
                .addCommand(Reply(), listOf(serviceHub.myInfo.legalIdentities.single().owningKey))
                .also {
                    it.verify(serviceHub)
                }

        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
    }
}

package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.internal.SendMessageFlow
import com.r3.corda.lib.chat.workflows.flows.utils.ServiceUtils
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByService
@StartableByRPC
class ReplyChatFlow(
        private val content: String,
        private val attachment: SecureHash?,
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // reply to which chat thread? should get the head of the chat thread based on the linearId
        val headMessageState = ServiceUtils.getChatHead(serviceHub, chatId)
        val headMessage = headMessageState.state.data

        val toList = (headMessage.to + headMessage.from - ourIdentity).distinct()

        return subFlow(SendMessageFlow(
                to = toList,
                subject = headMessage.subject,
                content = content,
                attachment = attachment,
                chatId = chatId
        ))
    }
}

/**
 * This is the flow which responds to reply chat.
 */
@InitiatedBy(ReplyChatFlow::class)
class ReplyChatFlowResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
    }
}

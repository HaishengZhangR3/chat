package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.workflows.flows.internal.CreateMessageFlow
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.observer.ReplyCommand
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByService
@StartableByRPC
class ReplyChatFlow(
        private val chatId: UniqueIdentifier,
        private val content: String
) : FlowLogic<StateAndRef<ChatMessage>>() {
    @Suspendable
    override fun call(): StateAndRef<ChatMessage> {
        val metaInfoStateRef = chatVaultService.getMetaInfoOrNull(chatId)
        require(metaInfoStateRef != null) { "ChatId must exist." }

        val metaInfo = metaInfoStateRef!!.state.data
        require((metaInfo.receivers + metaInfo.admin).contains(ourIdentity)) {"Replier must be in existing participants"}

        val messageStateRef = subFlow(CreateMessageFlow(
                chatId = chatId,
                content = content
        ))

        (metaInfo.receivers + metaInfo.admin).map { initiateFlow(it).send(messageStateRef.state.data) }
        return messageStateRef
    }
}

@InitiatedBy(ReplyChatFlow::class)
class ReplyChatFlowResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val chatMessage = otherSession.receive<ChatMessage>().unwrap { it }
        subFlow(ChatNotifyFlow(info = listOf(chatMessage), command = ReplyCommand()))
    }
}

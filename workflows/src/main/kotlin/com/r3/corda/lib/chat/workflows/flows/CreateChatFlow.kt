package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.workflows.flows.internal.CreateMessageFlow
import com.r3.corda.lib.chat.workflows.flows.internal.CreateMetaInfoFlow
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.observer.CreateCommand
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByService
@StartableByRPC
class CreateChatFlow(
        private val subject: String,
        private val content: String,
        private val receivers: List<Party>
) : FlowLogic<StateAndRef<ChatMessage>>() {
    @Suspendable
    override fun call(): StateAndRef<ChatMessage> {
        val metaInfo = subFlow(CreateMetaInfoFlow(receivers = receivers, subject = subject)).state.data
        val newMessageStateRef = subFlow(CreateMessageFlow(
                chatId = metaInfo.linearId,
                content = content
        ))

        (metaInfo.receivers + metaInfo.admin).map { initiateFlow(it).send(newMessageStateRef.state.data) }
        return newMessageStateRef
    }
}

@InitiatedBy(CreateChatFlow::class)
class CreateChatFlowResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val chatMessage = otherSession.receive<ChatMessage>().unwrap { it }
        subFlow(ChatNotifyFlow(info = listOf(chatMessage), command = CreateCommand()))
    }
}

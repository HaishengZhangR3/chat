package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.workflows.flows.internal.CreateMessageFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByService
@StartableByRPC
class ReplyChatFlow(
        private val chatId: UniqueIdentifier,
        private val subject: String,
        private val content: String
) : FlowLogic<StateAndRef<ChatMessage>>() {
    @Suspendable
    override fun call(): StateAndRef<ChatMessage> {
        val metaInfoStateAndRef = chatVaultService.getMetaInfo(chatId)

        return subFlow(CreateMessageFlow(
                chatId = metaInfoStateAndRef.state.data.linearId,
                receivers = metaInfoStateAndRef.state.data.receivers,
                subject = subject,
                content = content
        ))
    }
}

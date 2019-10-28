package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.workflows.flows.internal.CreateMessageFlow
import com.r3.corda.lib.chat.workflows.flows.internal.TestCreateMetaInfoFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party

@InitiatingFlow
@StartableByService
@StartableByRPC
class TestCreateChatFlow(
        private val subject: String,
        private val content: String,
        private val receivers: List<Party>
) : FlowLogic<StateAndRef<ChatMessage>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatMessage> {
        val metaStateRef = subFlow(TestCreateMetaInfoFlow(subject = subject, receivers = receivers))

        return subFlow(CreateMessageFlow(
                chatId = metaStateRef.state.data.linearId,
                content = content
        ))
    }
}


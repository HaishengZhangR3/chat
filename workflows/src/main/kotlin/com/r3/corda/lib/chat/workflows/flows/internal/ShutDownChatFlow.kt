package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatID
import com.r3.corda.lib.chat.workflows.flows.utils.CloseChatUtils
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

// @todo: review carefully for the annotation so that we don't expose too much through RPC or service
@InitiatingFlow
@StartableByService
@StartableByRPC
class ShutDownChatFlow(
        private val chatId: UniqueIdentifier,
        private val toList: List<Party>
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        toList.map { initiateFlow(it).send(chatId) }
    }
}

@InitiatedBy(ShutDownChatFlow::class)
class ShutDownChatFlowResponder(val otherSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        val chatId = otherSession.receive<ChatID>().unwrap { it }
        CloseChatUtils.closeChat(this, chatId, listOf(ourIdentity.owningKey))
    }
}

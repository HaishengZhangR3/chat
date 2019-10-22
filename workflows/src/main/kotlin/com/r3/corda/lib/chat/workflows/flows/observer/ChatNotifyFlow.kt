package com.r3.corda.lib.chat.workflows.flows.observer

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.ChatCommand
import net.corda.core.flows.*

@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatNotifyFlow(
        private val info: Any,
        private val command: ChatCommand
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        initiateFlow(ourIdentity).send(listOf(command, info))
    }
}

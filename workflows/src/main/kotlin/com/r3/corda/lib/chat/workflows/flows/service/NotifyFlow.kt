package com.r3.corda.lib.chat.workflows.flows.service

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.ChatCommand
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByService

@InitiatingFlow
@StartableByService
class NotifyFlow(
        private val chatInfo: ChatInfo,
        private val command: ChatCommand
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        initiateFlow(ourIdentity).send(listOf(chatInfo, command))
    }
}

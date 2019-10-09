package com.r3.corda.lib.chat.workflows.flows.service

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByService

@InitiatingFlow
@StartableByService
class NotifyFlow(
        private val chatId: UniqueIdentifier,
        private val command: String //@todo: "command" is what to send. to refine later
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        initiateFlow(ourIdentity).send(listOf<Any>(chatId, command))
    }
}

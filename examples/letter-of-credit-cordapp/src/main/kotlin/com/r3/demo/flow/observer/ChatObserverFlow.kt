package com.r3.demo.flow.observer

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.service.NotifyFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.unwrap

@InitiatedBy(NotifyFlow::class)
class ChatObserverFlow(private val otherSession: FlowSession): FlowLogic<Unit>() {

    companion object {
        private val log = contextLogger()
    }

    @Suspendable
    override fun call(): Unit {
        val (chatInfo, command) = otherSession.receive<List<Any>>().unwrap { it }

        // @todo: parse the command and notity through WebSocket API
        log.warn("${this.javaClass.name} got a notice from Chat SDK, ID: ${chatInfo}, cmd: $command")
    }
}

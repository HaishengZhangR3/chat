package com.r3.demo

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.service.NotifyFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@InitiatedBy(NotifyFlow::class)
class ChatObserverFlow(private val otherSession: FlowSession): FlowLogic<Unit>() {

    @Autowired
    private lateinit var wsServer: WSServer

    @Suspendable
    override fun call(): Unit {
        val (chatId, command) = otherSession.receive<List<Any>>().unwrap { it }

        // @todo: parse the command and notity through WebSocket API
        wsServer.getNotifyList().map{
            it.send("chatId: $chatId, command: $command")}
    }
}

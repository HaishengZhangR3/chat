package com.r3.demo.chatapi.observer

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.service.NotifyFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.unwrap
import java.io.File
import java.time.Instant

@InitiatedBy(NotifyFlow::class)
class ChatObserverFlow(private val otherSession: FlowSession): FlowLogic<Unit>() {

    companion object {
        private val log = contextLogger()
    }

    @Suspendable
    override fun call(): Unit {
        val (chatInfo, command) = otherSession.receive<List<Any>>().unwrap { it }

        // @todo: parse the command and notity through WebSocket API


        println("${ourIdentity.name} got a notice from Chat SDK, ID: ${chatInfo}, cmd: $command")
        log.warn("${ourIdentity.name} got a notice from Chat SDK, ID: ${chatInfo}, cmd: $command")

        val file = "/Users/haishengzhang/Documents/tmp/observer${Instant.now()}.log"
        File(file).appendText("command: $command")
        File(file).appendText("info: $chatInfo")

        // @todo: parse the command and notity through WebSocket API

        wsService.wsServer.getNotifyList().map{
            it.send("command: $command, message: ${chatInfo}")}
    }
}

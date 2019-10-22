package com.r3.demo.chatapi.observer

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.unwrap
import java.io.File
import java.time.Instant

@InitiatedBy(ChatNotifyFlow::class)
class ChatObserverFlow(private val otherSession: FlowSession): FlowLogic<Unit>() {

    companion object {
        private val log = contextLogger()
    }

    @Suspendable
    override fun call(): Unit {
        val (command, info) = otherSession.receive<List<Any>>().unwrap { it }

        println("${ourIdentity.name} got a notice from Chat SDK, ID: ${info}, cmd: $command")
        log.warn("${ourIdentity.name} got a notice from Chat SDK, ID: ${info}, cmd: $command")

        val file = "/Users/haishengzhang/Documents/tmp/observer${Instant.now()}.log"
        File(file).appendText("command: $command")
        File(file).appendText("info: $info")

        wsService.wsServer.getNotifyList().map{
            it.send("command: $command.\ninfo: ${info}.")}
    }
}

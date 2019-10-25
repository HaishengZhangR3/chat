package com.r3.demo.chatapi.observer

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.*
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.unwrap
import java.io.File
import java.time.Instant

@InitiatedBy(ChatNotifyFlow::class)
class ChatObserverFlow(private val otherSession: FlowSession) : FlowLogic<Unit>() {

    companion object {
        private val log = contextLogger()
    }

    @Suspendable
    override fun call() {
        val (command, info) = otherSession.receive<List<Any>>().unwrap { it }

        println("${ourIdentity.name} got a notice from Chat SDK, ID: ${info}, cmd: $command")
        log.warn("${ourIdentity.name} got a notice from Chat SDK, ID: ${info}, cmd: $command")

        val file = "/Users/haishengzhang/Documents/tmp/observer${Instant.now()}.log"
        File(file).appendText("command: $command")
        File(file).appendText("info: $info")

        wsService.wsServer.getNotifyList().map {
            it.send(parseData(command = command as ChatCommand, info = info as List<ContractState>))
        }
    }

    private fun parseData(command: ChatCommand, info: List<ContractState>): String =
            when (command) {
                // is CreateMeta: don't care, will update customers only after ChatMessage created
                is CreateMessage        -> {
                    val message = info.single() as ChatMessage
                    "New Message: " + chatInfoToString(message)
                }
                is CloseMeta            -> { info as ChatMetaInfo; "${info.linearId} is closed by ${info.admin}" }
                is AddParticipants      -> { info as ChatMetaInfo; "Added to chat ${info.linearId}." }
                is RemoveParticipants   -> { info as ChatMetaInfo; "Removed from chat ${info.linearId}." }
                else                    -> ""
            }

    private fun chatInfoToString(message: ChatMessage) =
            """
                ChatId: ${message.linearId},
                Sender: ${message.sender.name.organisation},
                Subject: ${message.subject},
                Content: ${message.content}
            """.trimIndent()
}

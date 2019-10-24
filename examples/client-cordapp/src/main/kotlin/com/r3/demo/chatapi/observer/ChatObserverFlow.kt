package com.r3.demo.chatapi.observer

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.*
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
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
    override fun call(): Unit {
        val (command, info) = otherSession.receive<List<Any>>().unwrap { it }

        println("${ourIdentity.name} got a notice from Chat SDK, ID: ${info}, cmd: $command")
        log.warn("${ourIdentity.name} got a notice from Chat SDK, ID: ${info}, cmd: $command")

        val file = "/Users/haishengzhang/Documents/tmp/observer${Instant.now()}.log"
        File(file).appendText("command: $command")
        File(file).appendText("info: $info")

        wsService.wsServer.getNotifyList().map {
            it.send(parseData(command = command as ChatCommand, info = info as ContractState))
        }
    }

    private fun parseData(command: ChatCommand, info: ContractState): String =
            when (command) {
                is Create               -> "New Message: " + chatInfoToString(info as ChatInfo)
                is Reply                -> "Replied Message: " + chatInfoToString(info as ChatInfo)
                is ShareHistory         -> "Shared Message: " + chatInfoToString(info as ChatInfo)
                is Close                -> { info as ChatInfo; "${info.linearId} is closed by ${info.sender}" }
                is UpdateParticipants   -> { info as UpdateParticipantsState; updateParticipantsInfoToString(info) }
                else                    -> ""
            }

    private fun chatInfoToString(info: ChatInfo) =
            """
                ChatId: ${info.linearId},
                Sender: ${info.sender.name.organisation},
                Subject: ${info.subject},
                Content: ${info.content}
            """.trimIndent()

    private fun updateParticipantsInfoToString(info: UpdateParticipantsState) =
            when {
                info.toAdd.isNotEmpty()     -> "${info.toAdd} have been added to chat ${info.linearId}."
                info.toRemove.isNotEmpty()  -> "${info.toRemove} have been removed to chat ${info.linearId}."
                else                        -> ""
            }
}

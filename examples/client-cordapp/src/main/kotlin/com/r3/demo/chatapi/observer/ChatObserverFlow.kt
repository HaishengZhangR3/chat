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

@Suppress("UNCHECKED_CAST")
@InitiatedBy(ChatNotifyFlow::class)
class ChatObserverFlow(private val otherSession: FlowSession) : FlowLogic<Unit>() {

    companion object {
        private val log = contextLogger()
    }

    @Suspendable
    override fun call() {
        val (command, info) = otherSession.receive<List<Any>>().unwrap { it }

        println("${ourIdentity.name.organisation} got a notice from Chat SDK, ID: ${info}, cmd: $command")

        val file = "/Users/haishengzhang/Documents/tmp/observer_${ourIdentity.name.organisation}.log"
        File(file).appendText("${ourIdentity.name.organisation} got a notice.\n")
        File(file).appendText("command: ${command}.\n")
        File(file).appendText("info: ${info}.\n")

        val data = parseData(command = command as ChatCommand, info = info as List<ContractState>)
        if (data.isNotEmpty()) {
            wsService.wsServer.getNotifyList().map { it.send(data) }
        }
    }

    private fun parseData(command: ChatCommand, info: List<ContractState>): String =
            when (command) {
                // is CreateMeta: don't care, will update customers only after ChatMessage created
                is CreateMessage        -> {
                    val message = info.single() as ChatMessage
                    "New Message: " + chatInfoToString(message)
                }
                is CloseMeta            -> {
                    val meta = info.single() as ChatMetaInfo
                    "${meta.linearId} is closed by ${meta.admin.name.organisation}"
                }
                // is CloseMessages: don't care, will update customers only after ChatMetaInfo closed
                is AddReceivers      -> {
                    val meta = info.single() as ChatMetaInfo
                    "Added to chat ${meta.linearId}."
                }
                is RemoveReceivers   -> {
                    val meta = info.single() as ChatMetaInfo
                    "Removed from chat ${meta.linearId}." }

                else                    -> ""
            }

    private fun chatInfoToString(message: ChatMessage) =
            """
                ChatId: ${message.chatId},
                Sender: ${message.sender.name.organisation},
                Content: ${message.content}
            """.trimIndent()
}

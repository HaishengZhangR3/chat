package com.r3.demo.chatapi.observer

import co.paralleluniverse.fibers.Suspendable
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.observer.*
import com.r3.demo.chatapi.data.ChatMessageData
import com.r3.demo.chatapi.data.ChatMetaInfoData
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

        val data = parseData(command = command as NotifyCommand, info = info as List<ContractState>)
        if (data.isNotEmpty()) {
            wsService.wsServer.getNotifyList().map { it.send(data) }
        }
    }

    private fun parseData(command: NotifyCommand, info: List<ContractState>): String =
            when (command) {
                // is CreateMeta: don't care, will update customers only after ChatMessage created
                is CreateCommand, is ReplyCommand -> {
                    val message = info.single() as ChatMessage
                    chatMessageToJson(command, message)
                }
                is CloseCommand -> {
                    val meta = info.single() as ChatMetaInfo
                    chatMetaInfoToJson(command, meta)
                }
                is AddParticipantsCommand -> {
                    val meta = info.single() as ChatMetaInfo
                    chatMetaInfoToJson(command, meta)
                }
                is RemoveParticipantsCommand -> {
                    val meta = info.single() as ChatMetaInfo
                    chatMetaInfoToJson(command, meta)
                }
                else -> ""
            }
    private fun chatMessageToJson(command: NotifyCommand, message: ChatMessage): String {
        val mapper = jacksonObjectMapper()
        return mapper.writeValueAsString(listOf(commandToString(command), ChatMessageData.fromState(message)))
    }

    private fun chatMetaInfoToJson(command: NotifyCommand, meta: ChatMetaInfo): String {
        val mapper = jacksonObjectMapper()
        return mapper.writeValueAsString(listOf(commandToString(command), ChatMetaInfoData.fromState(meta)))
    }

    private fun commandToString(command: NotifyCommand): String =
        when (command) {
            is  CreateCommand              -> "CreateCommand"
            is  ReplyCommand               -> "ReplyCommand"
            is  CloseCommand               -> "CloseCommand"
            is  AddParticipantsCommand     -> "AddParticipantsCommand"
            is  RemoveParticipantsCommand  -> "RemoveParticipantsCommand"
            else                           -> ""
        }

    private fun chatMessageToJson(message: ChatMessage) =
            """
                    ChatId: ${message.chatId},
                    Sender: ${message.sender.name.organisation},
                    Content: ${message.content}
                """.trimIndent()

}

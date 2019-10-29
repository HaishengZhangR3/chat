package net.corda.server.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.workflows.flows.service.ChatStatus
import com.r3.demo.chatapi.data.ChatMessageData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.server.NodeRPCConnection
import net.corda.server.service.ChatService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/loc")
class ChatController(rpc: NodeRPCConnection) {
    private val proxy = rpc.proxy

    ////////////////////  two test api: server aliveness and Chat CorDapp aliveness ////////////////////
    @GetMapping(value = ["/test"], produces = [MediaType.TEXT_PLAIN_VALUE])
    private fun status() = "from loc test web service"

    @GetMapping(value = ["/oneChat"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oneChat() =
            ChatService.api(proxy).getAllChats().first().state.data.toString()

    ////////////////////  ID from string to UniqueIdentifier ////////////////////
    fun toID(id: String) = UniqueIdentifier.fromString(id)

    ////////////////////  chat related api ////////////////////
    // data used between web service and UI
    data class APIChatMessage(
            val subject: String = "",
            val content: String = "",
            val receivers: List<String> = emptyList())

    @PostMapping(value = ["/chat"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun createChat(@RequestBody chatMessage: APIChatMessage) =
            ChatService.api(proxy).createChat(
                    subject = chatMessage.subject,
                    content = chatMessage.content,
                    receivers = chatMessage.receivers.map { proxy.partiesFromName(it, true).first() }
            ).toString()

    @PostMapping(value = ["/chat/{id}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun replyChat(@PathVariable("id") id: String,
                  @RequestBody chatMessage: APIChatMessage) =
            ChatService.api(proxy).replyChat(
                    chatId = toID(id),
                    content = chatMessage.content
            ).toString()

    ////////////////////  chat close api ////////////////////
    @PostMapping(value = ["/chat/{id}/close"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun closeChat(@PathVariable("id") id: String) {
        ChatService.api(proxy).closeChat(
                chatId = toID(id)
        ).toString()
    }

    ////////////////////  chat add participants api ////////////////////
    @PostMapping(value = ["/chat/{id}/participants/add"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun addParticipants(@PathVariable("id") id: String,
                        @RequestBody chatMessage: APIChatMessage) {
        ChatService.api(proxy).addParticipants(
                chatId = toID(id),
                toAdd = chatMessage.receivers.map { proxy.partiesFromName(it, true).first() }
        ).toString()
    }

    ////////////////////  chat remove participants api ////////////////////
    @PostMapping(value = ["/chat/{id}/participants/remove"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun removeParticipants(@PathVariable("id") id: String,
                           @RequestBody chatMessage: APIChatMessage) {
        ChatService.api(proxy).removeParticipants(
                chatId = toID(id),
                toRemove = chatMessage.receivers.map { proxy.partiesFromName(it, true).first() }
        ).toString()
    }

    ////////////////////  chat utility api ////////////////////
    @GetMapping(value = ["/chats/ids"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAllChatIDs() =
            ChatService.api(proxy).getAllChatIDs().toString()

    @GetMapping(value = ["/chats/messages"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAllChats() =
            chatInfoParser(ChatService.api(proxy).getAllChats().map { it.state.data })

    @GetMapping(value = ["/chat/{id}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatAllMessages(@PathVariable("id") id: String) =
            chatInfoParser(ChatService.api(proxy).getChatAllMessages(toID(id)).map { it.state.data })

    @GetMapping(value = ["/chat/{id}/status"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatCurrentStatus(@PathVariable("id") id: String) =
            ChatService.api(proxy).getChatCurrentStatus(toID(id)).toString()

    @GetMapping(value = ["/chat/{id}/participants"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatParticipants(@PathVariable("id") id: String) =
            ChatService.api(proxy).getChatParticipants(toID(id)).map { it.name.organisation }.toString()

    private fun chatInfoParser(infos: List<ChatMessage>): String {
        val activeChatIDs = ChatService.api(proxy).getActiveChatIDs()
        val newInfos = infos.map {
            if (!activeChatIDs.contains(it.chatId)) {
                ChatMessageData.fromState(it, ChatStatus.CLOSED)
            } else {
                ChatMessageData.fromState(it, ChatStatus.ACTIVE)
            }
        }

        val mapper = jacksonObjectMapper()
        return mapper.writeValueAsString(newInfos)
//
//        return infos.map { info ->
//            """
//                ChatId: ${info.chatId},
//                Sender: ${info.sender.name.organisation},
//                Content: ${info.content}.
//            """.trimIndent()
//        }.toString()
    }
}

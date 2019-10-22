package net.corda.server.controllers

import com.r3.corda.lib.chat.workflows.flows.service.ChatQuerySpec
import net.corda.core.contracts.UniqueIdentifier
import net.corda.server.NodeRPCConnection
import net.corda.server.service.ChatService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.time.Instant

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
            val subject: String,
            val content: String,
            val receivers: List<String>)

    @PostMapping(value = ["/chat"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun createChat(@RequestBody chatMessage: APIChatMessage) =
            ChatService.api(proxy).createChat(
                    subject = chatMessage.subject,
                    content = chatMessage.content,
                    attachment = null,
                    receivers = chatMessage.receivers.map { proxy.partiesFromName(it, true).first() }
            ).toString()

    @PostMapping(value = ["/chat/{id}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun replyChat(@PathVariable("id") id: String,
                  @RequestBody content: String) =
        ChatService.api(proxy).replyChat(
                chatId = toID(id),
                content = content,
                attachment = null
        ).toString()

    ////////////////////  chat close api ////////////////////
    @PostMapping(value = ["/chat/{id}/close/propose"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun proposeCloseChat(@PathVariable("id") id: String) =
            ChatService.api(proxy).proposeCloseChat(
                    chatId = toID(id)
            ).toString()

    @PostMapping(value = ["/chat/{id}/close/agree"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun agreeCloseChat(@PathVariable("id") id: String) =
            ChatService.api(proxy).agreeCloseChat(
                    chatId = toID(id)
            ).toString()

    @PostMapping(value = ["/chat/{id}/close/reject"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun rejectCloseChat(@PathVariable("id") id: String) =
            ChatService.api(proxy).rejectCloseChat(
                    chatId = toID(id)
            ).toString()

    @PostMapping(value = ["/chat/{id}/close"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun closeChat(@PathVariable("id") id: String) {
        ChatService.api(proxy).rejectCloseChat(
                chatId = toID(id)
        ).toString()
    }

    ////////////////////  chat add participants api ////////////////////
    @PostMapping(value = ["/chat/{id}/participants/add/propose"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun proposeAddParticipants(@PathVariable("id") id: String,
                               @RequestBody toAdd: List<String>) =
            ChatService.api(proxy).proposeAddParticipants(
                    chatId = toID(id),
                    toAdd = toAdd.map { proxy.partiesFromName(it, true).first() }
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/add/agree"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun agreeAddParticipants(@PathVariable("id") id: String) =
            ChatService.api(proxy).agreeAddParticipants(
                    chatId = toID(id)
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/add/reject"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun rejectAddParticipants(@PathVariable("id") id: String) =
            ChatService.api(proxy).rejectAddParticipants(
                    chatId = toID(id)
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/add"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun addParticipants(@PathVariable("id") id: String) {
        ChatService.api(proxy).addParticipants(
                chatId = toID(id)
        ).toString()
    }

    ////////////////////  chat remove participants api ////////////////////
    @PostMapping(value = ["/chat/{id}/participants/remove/propose"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun proposeRemoveParticipants(@PathVariable("id") id: String,
                                  @RequestBody toRemove: List<String>) =
            ChatService.api(proxy).proposeRemoveParticipants(
                    chatId = toID(id),
                    toRemove = toRemove.map { proxy.partiesFromName(it, true).first() }
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/remove/agree"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun agreeRemoveParticipants(@PathVariable("id") id: String) =
            ChatService.api(proxy).agreeRemoveParticipants(
                    chatId = toID(id)
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/remove/reject"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun rejectRemoveParticipants(@PathVariable("id") id: String) =
            ChatService.api(proxy).rejectRemoveParticipants(
                    chatId = toID(id)
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/remove"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun removeParticipants(@PathVariable("id") id: String) {
        ChatService.api(proxy).removeParticipants(
                chatId = toID(id)
        ).toString()
    }

    ////////////////////  chat utility api ////////////////////
    data class APIChatQuerySpec(
            val chatId: String? = null,
            val initiator: String? = null,
            val subject: String? = null,    // iLike subject, no wildcard
            val createdTimeFrom: Instant? = null,
            val createdTimeUntil: Instant? = null  // range = [fromTime, toTime)
    )

    @GetMapping(value = ["/chats/ids"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAllChatIDs() =
            ChatService.api(proxy).getAllChatIDs().toString()

    @GetMapping(value = ["/chats/messages"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAllChats() =
            ChatService.api(proxy).getAllChats().toString()

    @GetMapping(value = ["/chats/messages/by"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAllChatsBy(@RequestBody querySpec: APIChatQuerySpec) =
            ChatService.api(proxy).getAllChatsBy(
                    ChatQuerySpec(
                            chatId = toID(querySpec.chatId!!),
                            initiator = if (querySpec.initiator == null) null
                            else proxy.partiesFromName(querySpec.initiator, true).first(),
                            subject = querySpec.subject,
                            createdTimeFrom = querySpec.createdTimeFrom,
                            createdTimeUntil = querySpec.createdTimeUntil
                    )
            ).toString()

    @GetMapping(value = ["/chat/{id}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatAllMessages(@PathVariable("id") id: String) =
            ChatService.api(proxy).getChatAllMessages(toID(id)).toString()

    @GetMapping(value = ["/chat/{id}/by"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatMessagesBy(@PathVariable("id") id: String,
                          @RequestBody querySpec: APIChatQuerySpec) =
            ChatService.api(proxy).getChatMessagesBy(
                    ChatQuerySpec(
                            chatId = toID(id),
                            initiator = if (querySpec.initiator == null) null
                            else proxy.partiesFromName(querySpec.initiator, true).first(),
                            subject = querySpec.subject,
                            createdTimeFrom = querySpec.createdTimeFrom,
                            createdTimeUntil = querySpec.createdTimeUntil
                    )
            ).toString()

    @GetMapping(value = ["/chat/{id}/status"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatCurrentStatus(@PathVariable("id") id: String) =
            ChatService.api(proxy).getChatCurrentStatus(toID(id)).toString()

    @GetMapping(value = ["/chat/{id}/participants"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatParticipants(@PathVariable("id") id: String) =
            ChatService.api(proxy).getChatParticipants(toID(id)).map { it.name }.toString()

    @GetMapping(value = ["/chat/{id}/proposal/closeChat"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatCloseProposal(@PathVariable("id") id: String) =
            ChatService.api(proxy).getChatCloseProposal(toID(id)).toString()

    @GetMapping(value = ["/chat/{id}/proposal/updateParticipants"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatUpdateParticipantsProposal(@PathVariable("id") id: String) =
            ChatService.api(proxy).getChatUpdateParticipantsProposal(toID(id)).toString()

}

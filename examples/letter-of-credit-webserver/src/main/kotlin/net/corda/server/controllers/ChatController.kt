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
                receivers = chatMessage.receivers.map { proxy.partiesFromName( it, true).first() }
        ).toString()

    @PostMapping(value = ["/chat/{id}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun replyChat(@PathVariable("id") id: UniqueIdentifier,
                  @RequestBody content: String) =
            ChatService.api(proxy).replyChat(
                    chatId = id,
                    content = content,
                    attachment = null
            ).toString()

    ////////////////////  chat close api ////////////////////
    @PostMapping(value = ["/chat/{id}/close/propose"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun proposeCloseChat(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).proposeCloseChat(
                    chatId = id
            ).toString()

    @PostMapping(value = ["/chat/{id}/close/agree"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun agreeCloseChat(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).agreeCloseChat(
                    chatId = id
            ).toString()

    @PostMapping(value = ["/chat/{id}/close/reject"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun rejectCloseChat(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).rejectCloseChat(
                    chatId = id
            ).toString()

    @PostMapping(value = ["/chat/{id}/close"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun closeChat(@PathVariable("id") id: UniqueIdentifier) {
        ChatService.api(proxy).rejectCloseChat(
                chatId = id
        )
    }
    ////////////////////  chat add participants api ////////////////////
    @PostMapping(value = ["/chat/{id}/participants/add/propose"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun proposeAddParticipants(@PathVariable("id") id: UniqueIdentifier,
                               @RequestBody toAdd: List<String>) =
            ChatService.api(proxy).proposeAddParticipants(
                    chatId = id,
                    toAdd = toAdd.map { proxy.partiesFromName( it, true).first() }
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/add/agree"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun agreeAddParticipants(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).agreeAddParticipants(
                    chatId = id
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/add/reject"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun rejectAddParticipants(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).rejectAddParticipants(
                    chatId = id
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/add"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun addParticipants(@PathVariable("id") id: UniqueIdentifier) {
        ChatService.api(proxy).addParticipants(
                chatId = id
        )
    }

    ////////////////////  chat remove participants api ////////////////////
    @PostMapping(value = ["/chat/{id}/participants/remove/propose"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun proposeRemoveParticipants(@PathVariable("id") id: UniqueIdentifier,
                               @RequestBody toRemove: List<String>) =
            ChatService.api(proxy).proposeRemoveParticipants(
                    chatId = id,
                    toRemove = toRemove.map { proxy.partiesFromName( it, true).first() }
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/remove/agree"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun agreeRemoveParticipants(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).agreeRemoveParticipants(
                    chatId = id
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/remove/reject"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun rejectRemoveParticipants(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).rejectRemoveParticipants(
                    chatId = id
            ).toString()

    @PostMapping(value = ["/chat/{id}/participants/remove"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun removeParticipants(@PathVariable("id") id: UniqueIdentifier) {
        ChatService.api(proxy).removeParticipants(
                chatId = id
        )
    }

    ////////////////////  chat utility api ////////////////////
    data class APIChatQuerySpec(
            val chatId: UniqueIdentifier? = null,
            val initiator: String? = null,
            val subject: String? = null,    // iLike subject, no wildcard
            val createdTimeFrom: Instant? = null,
            val createdTimeUntil: Instant? = null  // range = [fromTime, toTime)
    )

    @GetMapping(value = ["/chats/ids"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAllChatIDs() =
        ChatService.api(proxy).getAllChatIDs()

    @GetMapping(value = ["/chats/messages"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAllChats() =
            ChatService.api(proxy).getAllChats()

    @GetMapping(value = ["/chats/messages/by"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAllChatsBy(@RequestBody querySpec: APIChatQuerySpec) =
            ChatService.api(proxy).getAllChatsBy(
                    ChatQuerySpec(
                            chatId = querySpec.chatId,
                            initiator = if (querySpec.initiator == null) null
                                    else proxy.partiesFromName(querySpec.initiator, true).first(),
                            subject = querySpec.subject,
                            createdTimeFrom = querySpec.createdTimeFrom,
                            createdTimeUntil = querySpec.createdTimeUntil
                    )
            )

    @GetMapping(value = ["/chat/{id}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatAllMessages(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).getChatAllMessages(id)

    @GetMapping(value = ["/chat/{id}/by"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatMessagesBy(@PathVariable("id") id: UniqueIdentifier,
                      @RequestBody querySpec: APIChatQuerySpec) =
            ChatService.api(proxy).getChatMessagesBy(
                    ChatQuerySpec(
                            chatId = querySpec.chatId,
                            initiator = if (querySpec.initiator == null) null
                            else proxy.partiesFromName(querySpec.initiator, true).first(),
                            subject = querySpec.subject,
                            createdTimeFrom = querySpec.createdTimeFrom,
                            createdTimeUntil = querySpec.createdTimeUntil
                    )
            )

    @GetMapping(value = ["/chat/{id}/status"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatCurrentStatus(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).getChatCurrentStatus(id)

    @GetMapping(value = ["/chat/{id}/participants"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatParticipants(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).getChatParticipants(id)

    @GetMapping(value = ["/chat/{id}/proposal/closeChat"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatCloseProposal(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).getChatCloseProposal(id)

    @GetMapping(value = ["/chat/{id}/proposal/updateParticipants"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getChatUpdateParticipantsProposal(@PathVariable("id") id: UniqueIdentifier) =
            ChatService.api(proxy).getChatUpdateParticipantsProposal(id)

}

package com.r3.demo.chatapi

import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.*
import com.r3.corda.lib.chat.workflows.flows.service.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow

class ChatApi() {

    companion object {
        private val log = contextLogger()
    }

    lateinit var proxy: CordaRPCOps

    constructor(newProxy: CordaRPCOps) : this() {
        proxy = newProxy
    }
    fun init(newProxy: CordaRPCOps) {
        proxy = newProxy
    }

    fun init(host: String, username: String, password: String, rpcPort: Int) {
        val nodeRPCConnection = NodeRPCConnection(host, username, password, rpcPort)
        proxy = nodeRPCConnection.proxy
    }

    fun createChat(subject: String, content: String, receivers: List<Party>): ChatMessage {

        log.warn("***** createChat *****")
        val createChat = proxy.startFlow(
                ::CreateChatFlow,
                subject,
                content,
                receivers
        ).returnValue.getOrThrow()
        return createChat.state.data
    }

    fun replyChat(chatId: UniqueIdentifier, content: String): ChatMessage {
        log.warn("***** replyChat *****")
        val replyChat = proxy.startFlow(
                ::ReplyChatFlow,
                chatId,
                content
        ).returnValue.getOrThrow()
        return replyChat.state.data
    }

    fun closeChat(chatId: UniqueIdentifier): SignedTransaction {
        log.warn("***** closeChat *****")
        val doIt = proxy.startFlow(
                ::CloseChatFlow,
                chatId
        ).returnValue.getOrThrow()

        return doIt
    }

    fun addParticipants(chatId: UniqueIdentifier, toAdd: List<Party>): ChatMetaInfo {
        log.warn("***** addParticipants *****")
        val doIt = proxy.startFlow(
                ::AddParticipantsFlow,
                chatId,
                toAdd
        ).returnValue.getOrThrow()
        return doIt.state.data
    }

    fun removeParticipants(chatId: UniqueIdentifier, toRemove: List<Party>): ChatMetaInfo {
        log.warn("***** removeParticipants *****")
        val doIt = proxy.startFlow(
                ::RemoveParticipantsFlow,
                chatId,
                toRemove
        ).returnValue.getOrThrow()
        return doIt.state.data
    }

    fun getAllChatIDs(): List<UniqueIdentifier> {
        log.warn("***** All chatIDs *****")
        val allChatIDsFromVault = proxy.startFlow(
                ::AllChatIDs
        ).returnValue.getOrThrow()
        return allChatIDsFromVault
    }

    fun getActiveChatIDs(): List<UniqueIdentifier> {
        log.warn("***** Active chatIDs *****")
        val allChatIDsFromVault = proxy.startFlow(
                ::ActiveChatIDs
        ).returnValue.getOrThrow()
        return allChatIDsFromVault
    }

    fun getAllChats(): List<StateAndRef<ChatMessage>> {
        log.warn("***** All chats and messages *****")
        val allChatsFromVault = proxy.startFlow(
                ::AllChats
        ).returnValue.getOrThrow()
        return allChatsFromVault
    }

    fun getChatAllMessages(chatId: UniqueIdentifier): List<StateAndRef<ChatMessage>> {
        log.warn("***** All messages for one single chat by ID: $chatId *****")
        val chatAllMessagesFromVault = proxy.startFlow(
                ::ChatAllMessages,
                chatId
        ).returnValue.getOrThrow()
        return chatAllMessagesFromVault
    }

    fun getChatCurrentStatus(chatId: UniqueIdentifier): ChatStatus {
        log.warn("***** chat status: active, close proposed, closed for one chat by ID *****")
        val chatStatusFromVault = proxy.startFlow(
                ::ChatCurrentStatus,
                chatId
        ).returnValue.getOrThrow()
        return chatStatusFromVault
    }

    fun getChatParticipants(chatId: UniqueIdentifier): List<Party> {
        log.warn("***** All participants for one chat by ID *****")
        val chatParticipantsFromVault = proxy.startFlow(
                ::ChatParticipants,
                chatId
        ).returnValue.getOrThrow()
        return chatParticipantsFromVault
    }
}

package com.r3.demo.chatapi

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.workflows.flows.*
import com.r3.corda.lib.chat.workflows.flows.service.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow

class ChatApi {

    companion object {
        private val log = contextLogger()
    }

    lateinit var proxy: CordaRPCOps

    fun init(newProxy: CordaRPCOps) {
        proxy = newProxy
    }

    fun init(host: String, username: String, password: String, rpcPort: Int) {
        val nodeRPCConnection = NodeRPCConnection(host, username, password, rpcPort)
        proxy = nodeRPCConnection.proxy
    }

    fun createChat(subject: String, content: String, attachment: SecureHash? = null, receivers: List<Party>): ChatInfo {

        log.warn("***** createChat *****")
        val createChat = proxy.startFlow(
                ::CreateChatFlow,
                subject,
                content,
                attachment,
                receivers
        ).returnValue.getOrThrow()
        return createChat.state.data
    }

    fun replyChat(chatId: UniqueIdentifier, content: String, attachment: SecureHash? = null): ChatInfo {
        log.warn("***** replyChat *****")
        val replyChat = proxy.startFlow(
                ::ReplyChatFlow,
                chatId,
                content,
                attachment
        ).returnValue.getOrThrow()
        return replyChat.coreTransaction.outputStates.single() as ChatInfo
    }

    fun proposeCloseChat(chatId: UniqueIdentifier): CloseChatState {
        log.warn("***** proposeCloseChat *****")
        val propose = proxy.startFlow(
                ::CloseChatProposeFlow,
                chatId
        ).returnValue.getOrThrow()
        return propose.coreTransaction.outputStates.single() as CloseChatState
    }

    fun agreeCloseChat(chatId: UniqueIdentifier): CloseChatState {
        log.warn("***** agreeCloseChat *****")
        val agree = proxy.startFlow(
                ::CloseChatAgreeFlow,
                chatId
        ).returnValue.getOrThrow()
        return agree.coreTransaction.outputStates.single() as CloseChatState
    }

    fun rejectCloseChat(chatId: UniqueIdentifier): SignedTransaction {
        log.warn("***** agreeCloseChat *****")
        val reject = proxy.startFlow(
                ::CloseChatRejectFlow,
                chatId
        ).returnValue.getOrThrow()
        return reject
    }

    fun closeChat(chatId: UniqueIdentifier): SignedTransaction {
        log.warn("***** closeChat *****")
        val doIt = proxy.startFlow(
                ::CloseChatFlow,
                chatId,
                false
        ).returnValue.getOrThrow()

        return doIt
    }

    fun proposeAddParticipants(toAdd: List<Party>, chatId: UniqueIdentifier): UpdateParticipantsState {
        log.warn("***** proposeAddParticipants *****")
        val propose = proxy.startFlow(
                ::AddParticipantsProposeFlow,
                chatId,
                toAdd,
                true
        ).returnValue.getOrThrow()
        return propose.coreTransaction.outputStates.single() as UpdateParticipantsState
    }

    fun agreeAddParticipants(toAdd: List<Party>, chatId: UniqueIdentifier): UpdateParticipantsState {
        log.warn("***** agreeAddParticipants *****")
        val agree = proxy.startFlow(
                    ::AddParticipantsAgreeFlow,
                    chatId
            ).returnValue.getOrThrow()
        return agree.coreTransaction.outputStates.single() as UpdateParticipantsState
    }

    fun rejectAddParticipants(toAdd: List<Party>, chatId: UniqueIdentifier): SignedTransaction {
        log.warn("***** rejectAddParticipants *****")
        val reject = proxy.startFlow(
                    ::AddParticipantsRejectFlow,
                    chatId
            ).returnValue.getOrThrow()
        return reject
    }

    fun addParticipants(toAdd: List<Party>, chatId: UniqueIdentifier): SignedTransaction {
        log.warn("***** addParticipants *****")
        val doIt = proxy.startFlow(
                ::AddParticipantsFlow,
                chatId
        ).returnValue.getOrThrow()
        return doIt
    }

    fun proposeRemoveParticipants(toRemove: List<Party>, chatId: UniqueIdentifier): UpdateParticipantsState {
        log.warn("***** proposeRemoveParticipants *****")
        val propose = proxy.startFlow(
                ::RemoveParticipantsProposeFlow,
                chatId,
                toRemove
        ).returnValue.getOrThrow()
        return propose.coreTransaction.outputStates.single() as UpdateParticipantsState
    }

    fun agreeRemoveParticipants(toRemove: List<Party>, chatId: UniqueIdentifier): UpdateParticipantsState {
        log.warn("***** agreeRemoveParticipants *****")
        val agree = proxy.startFlow(
                ::RemoveParticipantsAgreeFlow,
                chatId
        ).returnValue.getOrThrow()
        return agree.coreTransaction.outputStates.single() as UpdateParticipantsState
    }

    fun rejectRemoveParticipants(toRemove: List<Party>, chatId: UniqueIdentifier): SignedTransaction {
        log.warn("***** rejectRemoveParticipants *****")
        val reject = proxy.startFlow(
                ::RemoveParticipantsRejectFlow,
                chatId
        ).returnValue.getOrThrow()
        return reject
    }

    fun removeParticipants(toRemove: List<Party>, chatId: UniqueIdentifier): SignedTransaction {
        log.warn("***** removeParticipants *****")
        val doIt = proxy.startFlow(
                ::RemoveParticipantsFlow,
                chatId
        ).returnValue.getOrThrow()
        return doIt
    }

    fun getAllChatIDs(): List<UniqueIdentifier> {
        log.warn("***** All chatIDs *****")
        val allChatIDsFromVault = proxy.startFlow(
                ::AllChatIDs
        ).returnValue.getOrThrow()
        return allChatIDsFromVault
    }

    fun getAllChats(): List<StateAndRef<ChatInfo>> {
        log.warn("***** All chats and messages *****")
        val allChatsFromVault = proxy.startFlow(
                ::AllChats
        ).returnValue.getOrThrow()
        return allChatsFromVault
    }

    fun getAllChatsBy(querySpec: ChatQuerySpec): List<StateAndRef<ChatInfo>> {
        log.warn("***** All chats and messages with filter $querySpec applied *****")
        val allChatsFromVault = proxy.startFlow(
                ::AllChatsBy,
                querySpec
        ).returnValue.getOrThrow()
        return allChatsFromVault
    }

    fun getChatAllMessages(chatId: UniqueIdentifier): List<StateAndRef<ChatInfo>> {
        log.warn("***** All messages for one single chat by ID: $chatId *****")
        val chatAllMessagesFromVault = proxy.startFlow(
                ::ChatAllMessages,
                chatId
        ).returnValue.getOrThrow()
        return chatAllMessagesFromVault
    }

    fun getChatMessagesBy(querySpec: ChatQuerySpec): List<StateAndRef<ChatInfo>> {
        log.warn("***** All messages for one single chat with filter $querySpec applied *****")
        val chatAllMessagesFromVault = proxy.startFlow(
                ::ChatAllMessagesBy,
                querySpec
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

    fun getChatCloseProposal(chatId: UniqueIdentifier): CloseChatState {
        log.warn("***** getChatCloseProposal for one chat by ID *****")
        val closeProposalFromVault = proxy.startFlow(
                ::ChatCloseProposal,
                chatId
        ).returnValue.getOrThrow()
        return closeProposalFromVault
    }

    fun getChatUpdateParticipantsProposal(chatId: UniqueIdentifier): UpdateParticipantsState {
        log.warn("***** getChatUpdateParticipantsProposal for one chat by ID *****")
        val proposalFromVault = proxy.startFlow(
                ::ChatUpdateParticipantsProposal,
                chatId
        ).returnValue.getOrThrow()
        return proposalFromVault
    }
}

package com.r3.demo.chatapi

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.workflows.flows.*
import com.r3.corda.lib.chat.workflows.flows.service.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.concurrent.transpose
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.NodeParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import org.junit.Assert
import org.junit.Test
import java.time.Instant


class ChatApi {

    companion object {
        private val log = contextLogger()
    }
    lateinit var proxy: CordaRPCOps

    public fun connect(host: String,
                       username: String,
                       password: String,
                       rpcPort: Int) {
        val nodeRPCConnection = NodeRPCConnection(host, username, password, rpcPort)
        proxy = nodeRPCConnection.proxy

    }

    private fun createChat( toList: List<Party>, any: String): ChatInfo {
        val createChat = proxy.startFlow(
                ::CreateChatFlow,
                "Sample Topic $any",
                "Some sample content created $any",
                null,
                toList
        ).returnValue.getOrThrow()
        return createChat.state.data
    }

    private fun replyChat(chatId: UniqueIdentifier, any: String): ChatInfo {
        val replyChat = proxy.startFlow(
                ::ReplyChatFlow,
                chatId,
                "Some sample content replied $any",
                null
        ).returnValue.getOrThrow()

        return replyChat.coreTransaction.outputStates.single() as ChatInfo
    }

    private fun closeChat(agreeer: List<NodeHandle>, chatId: UniqueIdentifier): Any {
        log.warn("***** Propose close *****")
        val propose = proxy.startFlow(
                ::CloseChatProposeFlow,
                chatId
        ).returnValue.getOrThrow()

        // @todo: call ChatCloseProposal to get close proposal and check

        log.warn("***** Close proposal agreed *****")
        val agree = agreeer.map {
            it.rpc.startFlow(
                    ::CloseChatAgreeFlow,
                    chatId
            ).returnValue.getOrThrow()
        }

        log.warn("***** Do final close *****")
        val doIt = proposer.rpc.startFlow(
                ::CloseChatFlow,
                chatId,
                false
        ).returnValue.getOrThrow()

        return doIt
    }

    private fun addParticipantsToChat(proposer: NodeHandle, agreeer: List<NodeHandle>, toAdd: List<Party>, chatId: UniqueIdentifier): Any {
        log.warn("***** Propose add *****")
        val propose = proposer.rpc.startFlow(
                ::AddParticipantsProposeFlow,
                chatId,
                toAdd,
                true
        ).returnValue.getOrThrow()

        log.warn("***** Add proposal agreed *****")
        val agree = agreeer.map {
            it.rpc.startFlow(
                    ::AddParticipantsAgreeFlow,
                    chatId
            ).returnValue.getOrThrow()
        }

        log.warn("***** Do final add *****")
        val doIt = proposer.rpc.startFlow(
                ::AddParticipantsFlow,
                chatId
        ).returnValue.getOrThrow()

        return doIt
    }

    private fun getAllChatIDs(node: NodeHandle): List<UniqueIdentifier> {
        log.warn("***** All chatIDs *****")
        val allChatIDsFromVault = node.rpc.startFlow(
                ::AllChatIDs
        ).returnValue.getOrThrow()
        return allChatIDsFromVault
    }

    private fun getAllChats(node: NodeHandle): List<StateAndRef<ChatInfo>> {
        log.warn("***** All chats and messages *****")
        val allChatsFromVault = node.rpc.startFlow(
                ::AllChats
        ).returnValue.getOrThrow()
        return allChatsFromVault

    }

    private fun getAllChatsBy(node: NodeHandle, querySpec: ChatQuerySpec): List<StateAndRef<ChatInfo>> {
        log.warn("***** All chats and messages with filter $querySpec applied *****")
        val allChatsFromVault = node.rpc.startFlow(
                ::AllChatsBy,
                querySpec
        ).returnValue.getOrThrow()
        return allChatsFromVault
    }

    private fun getChatAllMessages(node: NodeHandle, chatId: UniqueIdentifier): List<StateAndRef<ChatInfo>> {
        log.warn("***** All messages for one single chat by ID: $chatId *****")
        val chatAllMessagesFromVault = node.rpc.startFlow(
                ::ChatAllMessages,
                chatId
        ).returnValue.getOrThrow()
        return chatAllMessagesFromVault
    }

    private fun getChatMessagesBy(node: NodeHandle, querySpec: ChatQuerySpec): List<StateAndRef<ChatInfo>> {
        log.warn("***** All messages for one single chat with filter $querySpec applied *****")
        val chatAllMessagesFromVault = node.rpc.startFlow(
                ::ChatAllMessagesBy,
                querySpec
        ).returnValue.getOrThrow()
        return chatAllMessagesFromVault
    }

    private fun getChatCurrentStatus(node: NodeHandle, chatId: UniqueIdentifier): ChatStatus {
        log.warn("***** chat status: active, close proposed, closed for one chat by ID *****")
        val chatStatusFromVault = node.rpc.startFlow(
                ::ChatCurrentStatus,
                chatId
        ).returnValue.getOrThrow()
        return chatStatusFromVault
    }

    private fun getChatParticipants(node: NodeHandle, chatId: UniqueIdentifier): List<Party> {
        log.warn("***** All participants for one chat by ID *****")
        val chatParticipantsFromVault = node.rpc.startFlow(
                ::ChatParticipants,
                chatId
        ).returnValue.getOrThrow()
        return chatParticipantsFromVault
    }

}

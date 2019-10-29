package com.r3.corda.lib.chat.workflows.flows.service

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault.*
import net.corda.core.serialization.CordaSerializable

// @todo: support page: PageSpecification

@CordaSerializable
sealed class ChatStatus {
    object ACTIVE : ChatStatus() {
        override fun toString() = "Active"
    }
    object CLOSED : ChatStatus(){
        override fun toString() = "Closed"
    }
}

// get ID of all chats
@InitiatingFlow
@StartableByService
@StartableByRPC
class AllChatIDs() : FlowLogic<List<UniqueIdentifier>>() {
    @Suspendable
    override fun call(): List<UniqueIdentifier> = chatVaultService.getAllChatIDs(status = StateStatus.ALL)
}

// get ID of all active chats
@InitiatingFlow
@StartableByService
@StartableByRPC
class ActiveChatIDs() : FlowLogic<List<UniqueIdentifier>>() {
    @Suspendable
    override fun call(): List<UniqueIdentifier> = chatVaultService.getAllChatIDs(status = StateStatus.UNCONSUMED)
}

// get all chats (no filter)
@InitiatingFlow
@StartableByService
@StartableByRPC
class AllChats : FlowLogic<List<StateAndRef<ChatMessage>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatMessage>> = chatVaultService.getAllMessages(status = StateStatus.ALL)
}

// get all messages for one single chat by ID
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatAllMessages(private val chatId: UniqueIdentifier) : FlowLogic<List<StateAndRef<ChatMessage>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatMessage>> = chatVaultService.getChatAllMessages(chatId)
}

// get chat status: active, close proposed, closed
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatCurrentStatus(private val chatId: UniqueIdentifier) : FlowLogic<ChatStatus>() {
    @Suspendable
    override fun call(): ChatStatus = chatVaultService.getChatStatus(chatId)
}


// get all participants for a chat
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatParticipants(private val chatId: UniqueIdentifier) : FlowLogic<List<Party>>() {
    @Suspendable
    override fun call(): List<Party> {
        val headMessage = chatVaultService.getMetaInfoOrNull(chatId)
        when (headMessage) {
            null -> return emptyList()
            else -> return headMessage.state.data.let { it.receivers + it.admin }.distinct()
        }
    }
}

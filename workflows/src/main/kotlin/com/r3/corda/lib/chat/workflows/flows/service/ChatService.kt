package com.r3.corda.lib.chat.workflows.flows.service

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.utils.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party

// @todo: support page: PageSpecification

// get all chats (no filter)
@InitiatingFlow
@StartableByService
@StartableByRPC
class AllChats : FlowLogic<List<StateAndRef<ChatInfo>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatInfo>> = chatVaultService.getAllChats()
}


// get all messages for one single chat by ID
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatAllMessages(private val chatId: UniqueIdentifier) : FlowLogic<List<StateAndRef<ChatInfo>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatInfo>> = chatVaultService.getAllMessages(chatId)
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
        val headMessage = chatVaultService.getHeadMessage(chatId)
        return headMessage.state.data.let { it.to + it.from }.distinct()
    }
}

// get all messages for a chat by:
//  from,
//  subject,
//  time range (newer than [datetimme], older than [another datetime]),
//  and/or combination of above
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatMessages(private val chatId: UniqueIdentifier, private val chatQuerySpec: ChatQuerySpec) : FlowLogic<List<StateAndRef<ChatInfo>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatInfo>> = chatVaultService.getChatMessages(chatId, chatQuerySpec)
}

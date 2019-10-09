package com.r3.corda.lib.chat.workflows.flows.service

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

// @todo: support page: PageSpecification

@CordaSerializable
sealed class ChatStatus {
    object ACTIVE : ChatStatus()
    object CLOSE_PROPOSED : ChatStatus()
    object CLOSE_AGREED : ChatStatus()
    object CLOSED : ChatStatus()
}

@CordaSerializable
data class ChatQuerySpec(
        val chatId: UniqueIdentifier? = null,
        val initiator: Party? = null,
        val subject: String? = null,    // iLike subject, no wildcard
        val createdTimeFrom: Instant? = null,
        val createdTimeUntil: Instant? = null  // range = [fromTime, toTime)
)

// get ID of all chats
@InitiatingFlow
@StartableByService
@StartableByRPC
class AllChatIDs() : FlowLogic<List<UniqueIdentifier>>() {
    @Suspendable
    override fun call(): List<UniqueIdentifier> = chatVaultService.getAllChatIDs()
}

// get all chats (no filter)
@InitiatingFlow
@StartableByService
@StartableByRPC
class AllChats : FlowLogic<List<StateAndRef<ChatInfo>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatInfo>> = chatVaultService.getAllChats()
}


// get all chats with filter applied
@InitiatingFlow
@StartableByService
@StartableByRPC
class AllChatsBy(private val chatQuerySpec: ChatQuerySpec) : FlowLogic<List<StateAndRef<ChatInfo>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatInfo>> = chatVaultService.getAllChatsBy(chatQuerySpec)
}

// get all messages for one single chat by ID
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatAllMessages(private val chatId: UniqueIdentifier) : FlowLogic<List<StateAndRef<ChatInfo>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatInfo>> = chatVaultService.getAllMessages(chatId)
}

// get all messages for a chat by:
//  from,
//  subject,
//  time range (newer than [datetime], older than [another datetime]),
//  and/or combination of above
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatAllMessagesBy(private val chatQuerySpec: ChatQuerySpec) : FlowLogic<List<StateAndRef<ChatInfo>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<ChatInfo>> = chatVaultService.getChatMessagesBy(chatQuerySpec)
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


// get close proposals for a chat
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatCloseProposal(private val chatId: UniqueIdentifier) : FlowLogic<CloseChatState>() {
    @Suspendable
    override fun call(): CloseChatState = chatVaultService.getCloseChatStateProposal(chatId)
}

// get participant updating proposals for a chat
@InitiatingFlow
@StartableByService
@StartableByRPC
class ChatUpdateParticipantsProposal(private val chatId: UniqueIdentifier) : FlowLogic<UpdateParticipantsState>() {
    @Suspendable
    override fun call(): UpdateParticipantsState = chatVaultService.getUpdateParticipantsProposal(chatId)
}

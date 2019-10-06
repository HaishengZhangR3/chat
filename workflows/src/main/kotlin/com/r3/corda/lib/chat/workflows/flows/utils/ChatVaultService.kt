package com.r3.corda.lib.chat.workflows.flows.utils

import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatInfo
import com.r3.corda.lib.chat.contracts.states.ChatBaseState
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.serialization.SingletonSerializeAsToken


sealed class ChatStatus {
    object ACTIVE: ChatStatus()
    object CLOSE_PROPOSED : ChatStatus()
    object CLOSE_AGREED : ChatStatus()
    object CLOSED : ChatStatus()
}

data class ChatQuerySpec (
        val chatId: UniqueIdentifier,
        val initiator: Party,
        val subject: String,
        val timeWindow: TimeWindow
)

@CordaService
class ChatVaultService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    /*  find notary */
    fun notary() =
            serviceHub.networkMapCache.notaryIdentities.first()

    fun notary(chatId: UniqueIdentifier) =
            getHeadMessage(chatId).state.notary

    /* get chat from vault */
    // get the chats thread from storage based on the chatId
    fun getAllChats(): List<StateAndRef<ChatInfo>> =
        serviceHub.vaultService.queryBy<ChatInfo>().states

    // get the chats thread from storage based on the chatId
    private fun getMessages(chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatInfo>> {
        val sorting = Sort(setOf(Sort.SortColumn(
                SortAttribute.Custom(PersistentChatInfo::class.java, "created"),
                Sort.Direction.DESC
        )))

        val stateAndRefs = serviceHub.vaultService.queryBy<ChatInfo>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(chatId), status = status),
                sorting = sorting)

        return when {
            stateAndRefs.states.isNotEmpty() -> stateAndRefs.states
            else -> throw FlowException("No such chat of chatId == $chatId found in vault.")
        }
    }

    fun getAllMessages(chatId: UniqueIdentifier) =
            getMessages(chatId, StateStatus.ALL)

    fun getActiveMessages(chatId: UniqueIdentifier) =
            getMessages(chatId)

    // any reply chat or finalize chat or add/remove participants should consume the head on-ledge state,
    // as a result, anytime, there s only one head of the chat chain.
    fun getHeadMessage(chatId: UniqueIdentifier): StateAndRef<ChatInfo> =
            getActiveMessages(chatId).first()

    /* get T: ChatBaseState from vault */
    inline fun <reified T: ChatBaseState> getVaultStates(chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<T>> {

        val stateAndRefs = serviceHub.vaultService.queryBy<T>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(chatId), status = status))

        return when {
            stateAndRefs.states.isNotEmpty() -> stateAndRefs.states
            else -> emptyList()
        }
    }

    inline fun <reified T: ChatBaseState> getHeadState(chatId: UniqueIdentifier): StateAndRef<T>? =
            getVaultStates<T>(chatId).sortedByDescending { it.state.data.created }.firstOrNull()

    inline fun <reified T: ChatBaseState> getActiveStates(chatId: UniqueIdentifier): Set<StateAndRef<T>> =
            getVaultStates<T>(chatId)
                    .filter { it.state.data.linearId == chatId}
                    .toSet()

    /* get ParticipantsUpdate head */
    fun getHeadParticipantsUpdateState(chatId: UniqueIdentifier) =
            getHeadState<UpdateParticipantsState>(chatId)

    fun getActiveParticipantsUpdateStates(chatId: UniqueIdentifier) =
            getVaultStates<UpdateParticipantsState>(chatId)

    /* get CloseChatState head */
    fun getHeadCloseChatState(chatId: UniqueIdentifier) =
            getHeadState<CloseChatState>(chatId)

    fun getActiveCloseChatStates(chatId: UniqueIdentifier) =
            getVaultStates<CloseChatState>(chatId)

    /* get chat information helper */
    // get current chat status
    fun getChatStatus(chatId: UniqueIdentifier): ChatStatus {
        // @todo: to implement
        return ChatStatus.ACTIVE
    }

    // query chat by id and filters
    fun getChatMessages(chatId: UniqueIdentifier, chatQuerySpec: ChatQuerySpec): List<StateAndRef<ChatInfo>> {
        // @todo: to implement
        return emptyList()
    }

}
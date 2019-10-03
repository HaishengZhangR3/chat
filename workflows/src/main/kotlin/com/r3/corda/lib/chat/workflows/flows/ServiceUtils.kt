package com.r3.corda.lib.chat.workflows.flows

import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatInfo
import com.r3.corda.lib.chat.contracts.states.ChatBaseState
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.contracts.states.ParticipantsUpdateState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute

object ServiceUtils {

    /*  find notary */
    fun notary(serviceHub: ServiceHub) =
            serviceHub.networkMapCache.notaryIdentities.first()

    fun notary(serviceHub: ServiceHub, chatId: UniqueIdentifier) =
            getChatHead(serviceHub, chatId).state.notary

    /* get chat from vault */
    // get the chats thread from storage based on the chatId
    private fun getChats(serviceHub: ServiceHub, chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatInfo>> {
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

    fun getAllChats(serviceHub: ServiceHub, chatId: UniqueIdentifier) =
            getChats(serviceHub, chatId, StateStatus.ALL)

    fun getActiveChats(serviceHub: ServiceHub, chatId: UniqueIdentifier) =
            getChats(serviceHub, chatId)

    // any reply chat or finalize chat or add/remove participants should consume the head on-ledge state,
    // as a result, anytime, there s only one head of the chat chain.
    fun getChatHead(serviceHub: ServiceHub, chatId: UniqueIdentifier): StateAndRef<ChatInfo> =
            getActiveChats(serviceHub, chatId).first()

    /* get T: ChatBaseState from vault */
    inline fun <reified T: ChatBaseState> getVaultStates(serviceHub: ServiceHub, chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<T>> {

        val stateAndRefs = serviceHub.vaultService.queryBy<T>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(chatId), status = status))

        return when {
            stateAndRefs.states.isNotEmpty() -> stateAndRefs.states
            else -> emptyList()
        }
    }

    inline fun <reified T: ChatBaseState> getHeadState(serviceHub: ServiceHub, chatId: UniqueIdentifier): StateAndRef<T>? =
            getVaultStates<T>(serviceHub, chatId).sortedByDescending { it.state.data.created }.firstOrNull()

    inline fun <reified T: ChatBaseState> getActiveStates(serviceHub: ServiceHub, chatId: UniqueIdentifier): Set<StateAndRef<T>> =
            getVaultStates<T>(serviceHub, chatId)
                    .filter { it.state.data.linearId == chatId}
                    .toSet()

    /* get ParticipantsUpdate head */
    fun getHeadParticipantsUpdateState(serviceHub: ServiceHub, chatId: UniqueIdentifier) =
            getHeadState<ParticipantsUpdateState>(serviceHub, chatId)

    fun getActiveParticipantsUpdateStates(serviceHub: ServiceHub, chatId: UniqueIdentifier) =
            getVaultStates<ParticipantsUpdateState>(serviceHub, chatId)

    /* get CloseChatState head */
    fun getHeadCloseChatState(serviceHub: ServiceHub, chatId: UniqueIdentifier) =
            getHeadState<CloseChatState>(serviceHub, chatId)

    fun getActiveCloseChatStates(serviceHub: ServiceHub, chatId: UniqueIdentifier) =
            getVaultStates<CloseChatState>(serviceHub, chatId)

}
package com.r3.corda.lib.chat.workflows.flows.utils

import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatMessage
import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatMetaInfo
import com.r3.corda.lib.chat.contracts.states.ChatBaseState
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.service.ChatStatus
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.node.services.vault.builder
import net.corda.core.serialization.SingletonSerializeAsToken
import java.util.*

// @todo: support paging: PageSpecification for every query

@CordaService
class ChatVaultService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    /*  find notary */
    fun notary() =
            serviceHub.networkMapCache.notaryIdentities.first()

    fun notary(chatId: UniqueIdentifier) =
            getMetaInfo(chatId).state.notary

    /* get all chats level information from vault */

    // get ID for all chats
    fun getAllChatIDs(status: StateStatus = StateStatus.UNCONSUMED): List<UniqueIdentifier> {

        val idGroup = builder { PersistentChatMetaInfo::created.min(groupByColumns = listOf(PersistentChatMetaInfo::identifier)) }
        val idGroupCriteria = QueryCriteria.VaultCustomQueryCriteria(idGroup)
        val chatInfos = serviceHub.vaultService.queryBy<ChatMetaInfo>(
                criteria = QueryCriteria.LinearStateQueryCriteria(status = status).and(idGroupCriteria)
        )

        return chatInfos.otherResults.filterIsInstance<UUID>().distinct().map { UniqueIdentifier(id=it) }
    }

    // get all chats
    fun getAllChats(status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatMessage>> =
            serviceHub.vaultService.queryBy<ChatMessage>(
                    criteria = QueryCriteria.LinearStateQueryCriteria(status = status)
            ).states

    /* get message level information from vault */
    // get the chats thread from storage based on the chatId
    private fun getMessages(chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatMessage>> {
        val stateAndRefs = serviceHub.vaultService.queryBy<ChatMessage>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(chatId), status = status))
        return stateAndRefs.states
    }

    fun getAllMessages(chatId: UniqueIdentifier) =
            getMessages(chatId, StateStatus.ALL)

    fun getActiveMessages(chatId: UniqueIdentifier) =
            getMessages(chatId)


    /* get T: ChatBaseState from vault */
    inline fun <reified T : ChatBaseState> getVaultStates(chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<T>> {

        val stateAndRefs = serviceHub.vaultService.queryBy<T>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(chatId), status = status))
        return stateAndRefs.states
    }

    inline fun <reified T : ChatBaseState> getHeadState(chatId: UniqueIdentifier): StateAndRef<T>? =
            getVaultStates<T>(chatId).sortedByDescending { it.state.data.created }.firstOrNull()

    inline fun <reified T : ChatBaseState> getActiveStates(chatId: UniqueIdentifier): Set<StateAndRef<T>> =
            getVaultStates<T>(chatId)
                    .filter { it.state.data.linearId == chatId }
                    .toSet()


    fun getMetaInfo(chatId: UniqueIdentifier): StateAndRef<ChatMetaInfo> =
            getVaultStates<ChatMetaInfo>(chatId).first()

    fun getActiveMetaInfo(chatId: UniqueIdentifier): StateAndRef<ChatMetaInfo>? =
            getActiveStates<ChatMetaInfo>(chatId).firstOrNull()

    // get current chat status
    fun getChatStatus(chatId: UniqueIdentifier): ChatStatus {
        val metaInfo = getActiveMetaInfo(chatId)
        when (metaInfo) {
            null -> return ChatStatus.CLOSED
            else -> return ChatStatus.ACTIVE
        }
    }
}

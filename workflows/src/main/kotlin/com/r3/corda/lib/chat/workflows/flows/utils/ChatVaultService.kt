package com.r3.corda.lib.chat.workflows.flows.utils

import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatMessage
import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatMetaInfo
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.service.ChatStatus
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.serialization.SingletonSerializeAsToken
import java.util.*

@CordaService
class ChatVaultService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    /*  find notary */
    fun notary() = serviceHub.networkMapCache.notaryIdentities.first()
    fun notary(chatId: UniqueIdentifier) = getMetaInfo(chatId).state.notary

    /* get all chats level information */
    // get ID for all chats
    fun getAllChatIDs(status: StateStatus = StateStatus.UNCONSUMED): List<UniqueIdentifier> {

        val idGroup = builder { PersistentChatMetaInfo::created.min(groupByColumns = listOf(PersistentChatMetaInfo::identifier)) }
        val idGroupCriteria = QueryCriteria.VaultCustomQueryCriteria(idGroup)
        val chatInfos = serviceHub.vaultService.queryBy<ChatMetaInfo>(
                criteria = QueryCriteria.LinearStateQueryCriteria(status = status).and(idGroupCriteria)
        )

        return chatInfos.otherResults.filterIsInstance<UUID>().distinct().map { UniqueIdentifier(id = it) }
    }

    // get all chat all messages
    fun getAllMessages(status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatMessage>> =
            serviceHub.vaultService.queryBy<ChatMessage>(
                    criteria = QueryCriteria.VaultQueryCriteria(status = status)
            ).states

    /* get chat level message information */
    private fun getChatMessages(chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatMessage>> {
        val criterial = QueryCriteria.VaultQueryCriteria(status = status)
                .and(builder {
                    QueryCriteria.VaultCustomQueryCriteria(PersistentChatMessage::identifier.equal(chatId.id))
                })
        val stateAndRefs = serviceHub.vaultService.queryBy<ChatMessage>(
                criteria = criterial)
        return stateAndRefs.states
    }
    fun getChatAllMessages(chatId: UniqueIdentifier) = getChatMessages(chatId, StateStatus.ALL)
    fun getChatActiveMessages(chatId: UniqueIdentifier) = getChatMessages(chatId)

    /* get chat level meta data information */
    inline fun <reified T : LinearState> getVaultStates(chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<T>> {
        val stateAndRefs = serviceHub.vaultService.queryBy<T>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(chatId), status = status))
        return stateAndRefs.states
    }
    fun getMetaInfo(chatId: UniqueIdentifier): StateAndRef<ChatMetaInfo> = getVaultStates<ChatMetaInfo>(chatId).first()
    fun getActiveMetaInfo(chatId: UniqueIdentifier): StateAndRef<ChatMetaInfo> = getVaultStates<ChatMetaInfo>(chatId).first()
    fun getMetaInfoOrNull(chatId: UniqueIdentifier): StateAndRef<ChatMetaInfo>? = getVaultStates<ChatMetaInfo>(chatId).firstOrNull()

    // get current chat status
    fun getChatStatus(chatId: UniqueIdentifier): ChatStatus {
        val metaInfo = getMetaInfoOrNull(chatId)
        when (metaInfo) {
            null -> return ChatStatus.CLOSED
            else -> return ChatStatus.ACTIVE
        }
    }
}

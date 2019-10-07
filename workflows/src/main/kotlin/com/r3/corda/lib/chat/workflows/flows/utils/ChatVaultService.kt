package com.r3.corda.lib.chat.workflows.flows.utils

import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatInfo
import com.r3.corda.lib.chat.contracts.states.*
import com.r3.corda.lib.chat.workflows.flows.service.ChatQuerySpec
import com.r3.corda.lib.chat.workflows.flows.service.ChatStatus
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.*
import net.corda.core.serialization.SingletonSerializeAsToken

// @todo: support paging: PageSpecification for every query

@CordaService
class ChatVaultService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    /*  find notary */
    fun notary() =
            serviceHub.networkMapCache.notaryIdentities.first()

    fun notary(chatId: UniqueIdentifier) =
            getHeadMessage(chatId).state.notary

    /* get all chats level information from vault */

    // get ID for all chats
    fun getAllChatIDs(status: StateStatus = StateStatus.UNCONSUMED): List<UniqueIdentifier> {

        val idGroup = builder { PersistentChatInfo::created.min(groupByColumns = listOf(PersistentChatInfo::identifier)) }
        val idGroupCriteria = QueryCriteria.VaultCustomQueryCriteria(idGroup)
        val chatInfos = serviceHub.vaultService.queryBy<ChatInfo>(
                criteria = QueryCriteria.LinearStateQueryCriteria(status = status).and(idGroupCriteria)
        )

        return chatInfos.states.map { it.state.data.linearId }
    }

    // get all chats
    fun getAllChats(status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatInfo>> =
            serviceHub.vaultService.queryBy<ChatInfo>(
                    criteria = QueryCriteria.LinearStateQueryCriteria(status = status)
            ).states

    // get all chats with filter
    fun getAllChatsBy(chatQuerySpec: ChatQuerySpec, status: StateStatus = StateStatus.UNCONSUMED) =
        getByChatQuerySpec(chatQuerySpec = chatQuerySpec, status = status)

    /* get message level information from vault */
    // get the chats thread from storage based on the chatId
    private fun getMessages(chatId: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatInfo>> {
        val sorting = Sort(setOf(Sort.SortColumn(
                SortAttribute.Custom(PersistentChatInfo::class.java, "created"),
                Sort.Direction.DESC
        )))

        val stateAndRefs = serviceHub.vaultService.queryBy<ChatInfo>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(chatId), status = status),
                sorting = sorting)
        return stateAndRefs.states
    }

    fun getAllMessages(chatId: UniqueIdentifier) =
            getMessages(chatId, StateStatus.ALL)

    fun getActiveMessages(chatId: UniqueIdentifier) =
            getMessages(chatId)

    // any reply chat or finalize chat or add/remove participants should consume the head on-ledge state,
    // as a result, anytime, there is only one head of the chat chain.
    fun getHeadMessage(chatId: UniqueIdentifier): StateAndRef<ChatInfo> =
            getActiveMessages(chatId).first()

    fun getHeadMessageOrNull(chatId: UniqueIdentifier): StateAndRef<ChatInfo>? =
            getActiveMessages(chatId).firstOrNull()

    /* get chat information helper */
    // get current chat status
    fun getChatStatus(chatId: UniqueIdentifier): ChatStatus {
        val headCloseState = getHeadCloseChatState(chatId)
        when (headCloseState) {
            null -> {
                val headChatInfo = getHeadMessageOrNull(chatId)
                when (headChatInfo) {
                    null -> return ChatStatus.CLOSED
                    else -> return ChatStatus.ACTIVE
                }
            }
            else -> {
                when (headCloseState.state.data.status) {
                    is CloseChatStatus.PROPOSED -> return ChatStatus.CLOSE_PROPOSED
                    is CloseChatStatus.REJECTED -> return ChatStatus.ACTIVE
                    is CloseChatStatus.AGREED -> return ChatStatus.CLOSE_AGREED
                }
            }
        }
    }

    // query chat by id and filters
    fun getChatMessagesBy(chatQuerySpec: ChatQuerySpec, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatInfo>> {
        if (chatQuerySpec.chatId == null) {
            throw FlowException("Chat ID must be provided to query by chat ID.")
        }
        return getByChatQuerySpec(chatQuerySpec = chatQuerySpec, status = status)
    }

    // get chats or messages with filter
    private fun getByChatQuerySpec(chatQuerySpec: ChatQuerySpec, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<ChatInfo>> {

        var criteria: QueryCriteria = QueryCriteria.LinearStateQueryCriteria(status = status)

        if (chatQuerySpec.createdTimeFrom != null && chatQuerySpec.createdTimeUntil != null) {
            val createdTimeExpression = builder {
                PersistentChatInfo::created.between(chatQuerySpec.createdTimeFrom, chatQuerySpec.createdTimeUntil)
            }
            criteria = criteria.and(QueryCriteria.VaultCustomQueryCriteria(createdTimeExpression))

//            criteria = criteria.and(QueryCriteria.VaultQueryCriteria(
//                    timeCondition = QueryCriteria.TimeCondition(
//                            QueryCriteria.TimeInstantType.RECORDED,
//                            ColumnPredicate.Between(chatQuerySpec.createdTimeFrom, chatQuerySpec.createdTimeUntil))
//            ))
        }

        if (chatQuerySpec.chatId != null) {
            criteria = criteria.and(QueryCriteria.LinearStateQueryCriteria(linearId = listOf(chatQuerySpec.chatId))
            )
        }

        if (chatQuerySpec.initiator != null) {
            val initiatorExpression = builder { PersistentChatInfo::chatFrom.equal(chatQuerySpec.initiator) }
            criteria = criteria.and(QueryCriteria.VaultCustomQueryCriteria(initiatorExpression))
        }

        if (chatQuerySpec.subject != null) {
            val subjectExpression = builder { PersistentChatInfo::subject.like(
                    string = chatQuerySpec.subject,
                    exactMatch = false) }
            criteria = criteria.and(QueryCriteria.VaultCustomQueryCriteria(subjectExpression))
        }

        return serviceHub.vaultService.queryBy<ChatInfo>(criteria = criteria).states
    }

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

}

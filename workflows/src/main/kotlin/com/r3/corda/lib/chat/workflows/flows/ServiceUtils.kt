package com.r3.corda.lib.chat.workflows.flows

import com.r3.corda.lib.chat.contracts.internal.schemas.PersistentChatInfo
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute

object ServiceUtils {

    // @todo: should any reply & finalize chat consume **all** of the on-ledge states or just the head one?
    fun getChats(serviceHub: ServiceHub, linearId: UniqueIdentifier): List<StateAndRef<ChatInfo>> {
        val sorting = Sort(setOf(Sort.SortColumn(
                SortAttribute.Custom(PersistentChatInfo::class.java, "created"),
                Sort.Direction.DESC
        )))

        val stateAndRefs = serviceHub.vaultService.queryBy<ChatInfo>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId)),
                sorting = sorting)

        return when {
            stateAndRefs.states.size > 0 -> stateAndRefs.states
            else -> throw FlowException("No such chat of linearId == $linearId found in vault.")
        }
    }

    // get the head of the chat thread from storage based on the linearId
    fun getChatHead(serviceHub: ServiceHub, linearId: UniqueIdentifier): StateAndRef<ChatInfo> =
        getChats(serviceHub, linearId).first()
}
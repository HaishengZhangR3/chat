package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AddReceivers
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByService
@StartableByRPC
class UpdateReceiversFlow(
        private val chatId: UniqueIdentifier,
        private val toAdd: List<Party> = emptyList(),
        private val toRemove: List<Party> = emptyList()
) : FlowLogic<StateAndRef<ChatMetaInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatMetaInfo> {

        val metaInfoStateAndRef = chatVaultService.getMetaInfo(chatId)
        val metaInfo = metaInfoStateAndRef.state.data

        // steps:
        // 1. close current ChatMetaInfo, need (all) sign
        subFlow(CloseMetaInfoFlow(chatId))

        // 2. create new ChatMetaInfo, with new added but without removed parties, need (all + added - removed) sign
        val newReceivers = metaInfo.receivers + toAdd - toRemove
        val txn = subFlow(CreateMetaInfoFlow(chatId, metaInfo.subject, newReceivers))

        subFlow(ChatNotifyFlow(info = listOf(txn.state.data), command = AddReceivers()))
        return txn
    }
}


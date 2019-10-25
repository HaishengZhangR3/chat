package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.CreateMeta
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.contracts.states.ChatStatus
import com.r3.corda.lib.chat.workflows.flows.internal.CreateMessageFlow
import com.r3.corda.lib.chat.workflows.flows.internal.CreateMetaInfoFlow
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByService
@StartableByRPC
class CreateChatFlow(
        private val subject: String,
        private val content: String,
        private val receivers: List<Party>
) : FlowLogic<StateAndRef<ChatMessage>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatMessage> {
        val metaStateRef = subFlow(CreateMetaInfoFlow(receivers = receivers))
        val chatId = metaStateRef.state.data.linearId

        return subFlow(CreateMessageFlow(chatId, subject, content))
    }
}


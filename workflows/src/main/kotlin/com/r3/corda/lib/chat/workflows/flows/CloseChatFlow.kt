package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.states.ChatID
import com.r3.corda.lib.chat.workflows.flows.internal.CloseMessagesFlow
import com.r3.corda.lib.chat.workflows.flows.internal.CloseMetaInfoFlow
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val metaInfo = chatVaultService.getMetaInfo(chatId).state.data
        requireThat {
            "Only chat admin can close chat." using (ourIdentity == metaInfo.admin)
        }

        // close all messages in our side
        subFlow(CloseMessagesFlow(chatId))

        // close all messages from other sides
        metaInfo.receivers.map { initiateFlow(it).send(chatId) }

        // close meta
        val txn = subFlow(CloseMetaInfoFlow(chatId))
        subFlow(ChatNotifyFlow(info = listOf(metaInfo), command = Close()))
        return txn
    }
}

@InitiatedBy(CloseChatFlow::class)
class CloseChatFlowResponder(private val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val chatId = otherSession.receive<ChatID>().unwrap { it }
        val metaInfo = chatVaultService.getMetaInfo(chatId).state.data

        val txn = subFlow(CloseMessagesFlow(chatId))
        subFlow(ChatNotifyFlow(info = listOf(metaInfo), command = Close()))
        return txn

    }
}

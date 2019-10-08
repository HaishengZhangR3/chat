package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.internal.SendMessageFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByService
@StartableByRPC
class ReplyChatFlow(
        private val chatId: UniqueIdentifier,
        private val content: String,
        private val attachment: SecureHash?
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // @todo: reply should fail if there is no chat in vault
        // @todo: in fact, everything should fail if there is no chat in vault

        // reply to which chat thread? should get the head of the chat thread based on the linearId
        val headMessageState = chatVaultService.getHeadMessage(chatId)
        val headMessage = headMessageState.state.data

        // @todo: check everywhere for:
        //        - distinct of toList and participants
        //        - "send to list" should ***not*** include "ourIdentity"
        val toList = (headMessage.to + headMessage.from - ourIdentity).distinct()

        return subFlow(SendMessageFlow(
                to = toList,
                subject = headMessage.subject,
                content = content,
                attachment = attachment,
                chatId = chatId
        ))
    }
}

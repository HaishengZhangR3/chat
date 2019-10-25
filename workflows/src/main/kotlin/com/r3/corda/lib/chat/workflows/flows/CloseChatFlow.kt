package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.internal.CloseMessagesFlow
import com.r3.corda.lib.chat.workflows.flows.internal.CloseMetaInfoFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseChatFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        subFlow(CloseMessagesFlow(chatId))
        return subFlow(CloseMetaInfoFlow(chatId))
    }
}

package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.SyncUpHistory
import com.r3.corda.lib.chat.contracts.states.ChatID
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByService
class SyncUpChatHistoryFlow(
        private val to: List<Party>,
        private val chatId: ChatID
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        val historyMessages = ServiceUtils.getAllChats(serviceHub, chatId).map { it.state.data }
        to.map { initiateFlow(it).send(historyMessages) }
    }
}

@InitiatedBy(SyncUpChatHistoryFlow::class)
class SyncUpChatHistoryFlowResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        val historyMessages = otherSession.receive<List<ChatInfo>>().unwrap { it }
        val signedTxns = historyMessages.map {
            val txnBuilder = TransactionBuilder(notary = ServiceUtils.notary(serviceHub))
                    // no input
                    .addOutputState( it.copy(participants = listOf(ourIdentity)) )
                    .addCommand(SyncUpHistory(), listOf(ourIdentity.owningKey))
            txnBuilder.verify(serviceHub)
            serviceHub.signInitialTransaction(txnBuilder)
        }
        serviceHub.recordTransactions(signedTxns)
    }
}

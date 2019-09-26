package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Create
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
        val historyChats = ServiceUtils.getActiveChats(serviceHub, chatId)
        val newChats = historyChats.map { it.state.data }
                .map { it.copy(to = it.to + to) }
        to.map { initiateFlow(it).send(newChats) }
    }
}

@InitiatedBy(SyncUpChatHistoryFlow::class)
class SyncUpChatHistoryFlowResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {

        val notary = ServiceUtils.notary(serviceHub)

        val historyChats = otherSession.receive<List<ChatInfo>>().unwrap { it }
        val txns = historyChats.map {
            val txnBuilder = TransactionBuilder(notary = notary)
                    // no input
                    .addOutputState(it)
                    .addCommand(Create(), listOf(ourIdentity.owningKey))
                    .also { it.verify(serviceHub) }
            serviceHub.signInitialTransaction(txnBuilder)
        }
        serviceHub.recordTransactions(txns)
    }
}

package com.r3.corda.lib.chat.workflows.flows.utils

import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.states.ChatID
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.contracts.states.CloseChatStatus
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

object CloseChatUtils {

    fun closeChat(serviceHub: ServiceHub, linearId: ChatID, participants: List<PublicKey>): Unit {

        // get and consume all messages in vault
        val allMessagesStateRef = ServiceUtils.getActiveChats(serviceHub, linearId)
        requireThat { "There must be message in vault" using (allMessagesStateRef.isNotEmpty()) }

        val anyMessageStateRef = allMessagesStateRef.first()
        val txnBuilder = TransactionBuilder(notary = anyMessageStateRef.state.notary)
                .addCommand(Close(), participants)
        allMessagesStateRef.forEach { txnBuilder.addInputState(it) }
        txnBuilder.verify(serviceHub)

        // sign it
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
    }

    fun getAllCloseStates(serviceHub: ServiceHub, linearId: ChatID): List<StateAndRef<CloseChatState>> {
        val allCloseStateRef = ServiceUtils.getActiveCloseChatStates(serviceHub, linearId)
        requireThat { "No close chat proposal." using (allCloseStateRef.isNotEmpty()) }
        requireThat { "Should not be more close chat proposal." using
                (allCloseStateRef.filter { it.state.data.status == CloseChatStatus.PROPOSED }.size == 1) }

        return allCloseStateRef
    }

    fun getCloseProposeState(allStates: List<StateAndRef<CloseChatState>>) = allStates.first { it.state.data.status == CloseChatStatus.PROPOSED }

    // check whether all of the participants agreed
    fun areAllCloseProposeAgreed(allCloseStateRef: List<StateAndRef<CloseChatState>>) {

        allCloseStateRef.map { it.state.data }.let {
            val proposed = it.filter { it.status == CloseChatStatus.PROPOSED }.size
            requireThat { "There should be at most 1 proposal." using (proposed == 1) }

            val rejected = it.filter { it.status == CloseChatStatus.REJECTED }.size
            requireThat { "There should be not rejection." using (rejected == 0) }

            val proposedState = it.single { it.status == CloseChatStatus.PROPOSED }
            val partyAmount = proposedState.to.size - 1 // proposer agreed by default
            val agreed = it.filter { it.status == CloseChatStatus.AGREED }.size
            requireThat { "Not all participants agreed." using (partyAmount == agreed) }
        }
    }
}
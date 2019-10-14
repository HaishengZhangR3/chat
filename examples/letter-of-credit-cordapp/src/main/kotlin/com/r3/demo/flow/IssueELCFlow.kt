package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.workflows.flows.utils.CloseChatUtils
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class IssueELCFlow(
        private val chatId: UniqueIdentifier,
        private val force: Boolean = false
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // @todo: close should fail if there is no chat in vault
        // @todo: close should fail if there is no close propose

        // ask all parties to close the chats
        val allCloseStateRef = CloseChatUtils.getAllCloseStates(this, chatId)
        val proposedCloseStateRef = CloseChatUtils.getCloseProposeState(allCloseStateRef)
        val proposedCloseState = proposedCloseStateRef.state.data

        // need check whether all of the participants agreed already before close the chat
        requireThat { "Only the proposer are allowed to close the chat" using (proposedCloseState.from == ourIdentity) }
        if (!force) {
            CloseChatUtils.areAllCloseProposeAgreed(allCloseStateRef)
        }

        val allParties = (proposedCloseState.to + proposedCloseState.from).distinct()
        val counterParties = allParties - ourIdentity

        // close by consuming 1). ProposeClose and CloseAgree state, need every parties to sign and close it
        val closeProposeTxn = TransactionBuilder(proposedCloseStateRef.state.notary)
                .addCommand(Close(), allParties.map { it.owningKey })
        allCloseStateRef.map { closeProposeTxn.addInputState( it ) }
        closeProposeTxn.verify(serviceHub)

        val selfSignedTxn = serviceHub.signInitialTransaction(closeProposeTxn)
        val counterPartySessions = counterParties.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        // 2). consume all of the chat messages
        CloseChatUtils.closeChat(this, chatId, listOf(ourIdentity.owningKey))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

/**
 * This is the flow which responds to close chat.
 */
@InitiatedBy(IssueELCFlow::class)
class IssueELCFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {

                val closeChatState = serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data }.first() as CloseChatState
                CloseChatUtils.closeChat(this, closeChatState.linearId, listOf(ourIdentity.owningKey))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

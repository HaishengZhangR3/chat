package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AgreeAddParticipants
import com.r3.corda.lib.chat.contracts.states.ChatID
import com.r3.corda.lib.chat.contracts.states.ParticipantsUpdateState
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsAgreeFlow(
        private val txnId: SecureHash,
        private val chatId: ChatID
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val txnToAgree = serviceHub.validatedTransactions.getTransaction(txnId) as SignedTransaction
        val stateAndRefToAgree = txnToAgree.toLedgerTransaction(serviceHub).outRefsOfType<ParticipantsUpdateState>().single()
        val stateToAgree = stateAndRefToAgree.state.data

        val allParties = listOf(stateToAgree.from, ourIdentity)
        val toParty = stateToAgree.from

        val txnBuilder = TransactionBuilder(notary = txnToAgree.notary)
                // @todo: input, output? think about it in detail......
//                .addInputState(stateAndRefToAgree)
                .addOutputState(stateToAgree.let { it.copy(allParticipants = it.toUpdate + it.from) })
                .addCommand(AgreeAddParticipants(), allParties.map { it.owningKey })
                .also { it.verify(serviceHub) }

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySession = initiateFlow(toParty)
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, listOf(counterPartySession)))
        return subFlow(FinalityFlow(collectSignTxn, counterPartySession))
    }
}

@InitiatedBy(AddParticipantsAgreeFlow::class)
class AddParticipantsAgreeFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {

            @Suspendable
            override fun checkTransaction(stx: SignedTransaction): Unit {

//                val stateToAgree = serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data }.single() as ParticipantsUpdateState
                val stateToAgree = stx.tx.outputStates.single() as ParticipantsUpdateState

                // the proposer side is responsible of sending history chat to new added participants,
                when (stateToAgree.from) {
                    ourIdentity -> {
                        // we proposed to >1 parties, but we'd only send once
                        val stateAndRef = ServiceUtils.getHeadParticipantsUpdateStates(serviceHub, stateToAgree.linearId)
                        when (stateAndRef){
                            null -> "Already send out, no any further action needed."
                            else -> {
                                subFlow(SyncUpChatHistoryFlow(stateToAgree.toUpdate, stateToAgree.linearId))

                                // @todo: consume local proposal and save to new txn
                                // if we collect all parties signature, then we'll consume the proposal state,
                                // together with all of the counterparts' agree states, and issue nothing
                                val allAgreeStates = ServiceUtils.getActiveParticipantsUpdateStates(
                                        serviceHub, stateToAgree.linearId, ourIdentity)
                                if (allAgreeStates.size == stateAndRef.state.data.allParticipants.size) {

                                    // consume everything, issue nothing
                                    val allParties = stateAndRef.state.data.allParticipants
                                    val txnBuilder = TransactionBuilder(notary = stateAndRef.state.notary)
                                            .withItems(allAgreeStates) // a lot to add a input
                                            .addCommand(AgreeAddParticipants(), allParties.map { it.owningKey })
                                            .also { it.verify(serviceHub) }


                                } else {
                                    //continue
                                }

                            }
                        }
                    }
                    else -> throw FlowException("I did not propose !!")
                }
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

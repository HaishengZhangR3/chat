package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AgreeUpdateParticipants
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.workflows.flows.ShareChatHistoryFlow
import com.r3.corda.lib.chat.workflows.flows.utils.ServiceUtils
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class UpdateParticipantsFlow(
        private val txnId: SecureHash,
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val txnToAgree = serviceHub.validatedTransactions.getTransaction(txnId) as SignedTransaction
        val stateAndRefToAgree = txnToAgree.toLedgerTransaction(serviceHub).outRefsOfType<UpdateParticipantsState>().single()
        val stateToAgree = stateAndRefToAgree.state.data

        val allParties = listOf(stateToAgree.from, ourIdentity)
        val toParty = stateToAgree.from

        val txnBuilder = TransactionBuilder(notary = txnToAgree.notary)
                // @todo: input, output? think about it in detail......
//                .addInputState(stateAndRefToAgree)
                .addOutputState(stateToAgree.let { it.copy(participants = it.toAdd + it.from) })
                .addCommand(AgreeUpdateParticipants(), allParties.map { it.owningKey })
                .also { it.verify(serviceHub) }

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySession = initiateFlow(toParty)
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, listOf(counterPartySession)))
        return subFlow(FinalityFlow(collectSignTxn, counterPartySession))
    }
}

@InitiatedBy(UpdateParticipantsFlow::class)
class UpdateParticipantsFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {

            @Suspendable
            override fun checkTransaction(stx: SignedTransaction): Unit {

//                val stateToAgree = serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data }.single() as ParticipantsUpdateState
                val stateToAgree = stx.tx.outputStates.single() as UpdateParticipantsState

                // the proposer side is responsible of sending history chat to new added participants,
                when (stateToAgree.from) {
                    ourIdentity -> {
                        // we proposed to >1 parties, but we'd only send once
                        val stateAndRef = ServiceUtils.getHeadParticipantsUpdateState(serviceHub, stateToAgree.linearId)
                        when (stateAndRef){
                            null -> "Already send out, no any further action needed."
                            else -> {
                                subFlow(ShareChatHistoryFlow(stateToAgree.toAdd, stateToAgree.linearId))

                                // @todo: consume local proposal and save to new txn
                                // if we collect all parties signature, then we'll consume the proposal state,
                                // together with all of the counterparts' agree states, and issue nothing
                                val allAgreeStates = ServiceUtils.getActiveParticipantsUpdateStates(
                                        serviceHub, stateToAgree.linearId)
                                if (allAgreeStates.size == stateAndRef.state.data.participants.size) {

                                    // consume everything, issue nothing
                                    val allParties = stateAndRef.state.data.participants
                                    val txnBuilder = TransactionBuilder(notary = stateAndRef.state.notary)
                                            .withItems(allAgreeStates) // a lot to add a input
                                            .addCommand(AgreeUpdateParticipants(), allParties.map { it.owningKey })
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

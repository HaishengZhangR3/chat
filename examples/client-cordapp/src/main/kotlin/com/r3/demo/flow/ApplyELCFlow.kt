package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.ApplyELC
import com.r3.demo.contracts.states.ElectronicLetterOfCredit
import com.r3.demo.contracts.states.LoCStatus
import com.r3.demo.vaultService
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

// step 2.1: application of electronic letter of credit
@InitiatingFlow
@StartableByService
@StartableByRPC
class ApplyELCFlow(
        private val issuer: Party,
        private val beneficiary: Party,
        private val advisingBank: Party,
        private val amountLimit: Amount<Currency>
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val state = ElectronicLetterOfCredit(
                issuer = issuer,
                applicant = ourIdentity,
                beneficiary = beneficiary,
                advisingBank = advisingBank,
                status = LoCStatus.APPLIED,
                amountLimit = amountLimit,
                participants = listOf(ourIdentity, issuer)
        )

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(ApplyELC(), state.participants.map { it.owningKey })
                .addOutputState(state)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = listOf(issuer).map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(ApplyELCFlow::class)
class ApplyELCFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

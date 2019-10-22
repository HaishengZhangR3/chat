package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.IssueELC
import com.r3.demo.contracts.states.ElectronicLetterOfCredit
import com.r3.demo.contracts.states.LoCStatus
import com.r3.demo.vaultService
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

// step 3: issue electronic letter of credit from issuing bank to advising bank
@InitiatingFlow
@StartableByService
@StartableByRPC
class IssueELCFlow : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // only issue "APPROVED" by "issuer" (me)
        val approved = vaultService.getVaultStates<ElectronicLetterOfCredit>()
                .first { it.state.data.status == LoCStatus.APPROVED
                        && it.state.data.issuer == ourIdentity
                }

        val output = approved.state.data.copy(
                timeIssued = Instant.now(),
                status = LoCStatus.ISSUED,
                participants = with (approved.state.data) { listOf(issuer, advisingBank) }
        )

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(IssueELC(), output.participants.map { it.owningKey })
                .addInputState(approved)
                .addOutputState(output)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = listOf(output.applicant).map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(IssueELCFlow::class)
class IssueELCFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
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

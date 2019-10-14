package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.AdviceELC
import com.r3.demo.contracts.states.ElectronicLetterOfCredit
import com.r3.demo.contracts.states.LoCStatus
import com.r3.demo.vaultService
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

// step 4: advice electronic letter of credit from advising bank to beneficiary
@InitiatingFlow
@StartableByService
@StartableByRPC
class AdviceELCFlow : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // only issue "ISSUED" by "advising bank" (me)
        val issued = vaultService.getVaultStates<ElectronicLetterOfCredit>()
                .first { it.state.data.status == LoCStatus.ISSUED
                        && it.state.data.advisingBank == ourIdentity
                }

        val output = issued.state.data.copy(
                timeIssued = Instant.now(),
                status = LoCStatus.ADVISED,
                participants = with (issued.state.data) { listOf(advisingBank, beneficiary) }
        )

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(AdviceELC(), output.participants.map { it.owningKey })
                .addInputState(issued)
                .addOutputState(output)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = listOf(output.applicant).map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(AdviceELCFlow::class)
class AdviceELCFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {}
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.AgreeELC
import com.r3.demo.contracts.states.ElectronicLetterOfCredit
import com.r3.demo.contracts.states.LoCStatus
import com.r3.demo.vaultService
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

// step 2.2: agree of electronic letter of credit
@InitiatingFlow
@StartableByService
@StartableByRPC
class AgreeELCFlow(
        private val applicant: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // only agree the APPLIED by "applicant"
        val applied = vaultService.getVaultStates<ElectronicLetterOfCredit>()
                .first { it.state.data.status == LoCStatus.APPLIED
                        && it.state.data.applicant == applicant
                }

        val output = applied.state.data.copy(
                timeIssued = Instant.now(),
                status = LoCStatus.APPROVED
        )

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(AgreeELC(), output.participants.map { it.owningKey })
                .addInputState(applied)
                .addOutputState(output)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = listOf(output.applicant).map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(AgreeELCFlow::class)
class AgreeELCFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
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

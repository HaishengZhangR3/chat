package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.DispatchPresentReleaseEDoc
import com.r3.demo.contracts.states.ElectronicLetterOfCredit
import com.r3.demo.contracts.states.LoCStatus
import com.r3.demo.contracts.states.Shipment
import com.r3.demo.contracts.states.ShipmentStatus
import com.r3.demo.vaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

// step 6 ~ 9: dispatch, present and release between: beneficiary, advising bank, issuing bank and applicant
@InitiatingFlow
@StartableByService
@StartableByRPC
class DispatchPresentReleaseEDocFlow : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // do I get "ADVISED" LoC and "RECEIVED" shipment?
        val advisedLoc = vaultService.getVaultStates<ElectronicLetterOfCredit>()
                .first {
                    it.state.data.status == LoCStatus.ADVISED
                            && it.state.data.beneficiary == ourIdentity
                }

        val shipment = vaultService.getVaultStates<Shipment>()
                .first {
                    it.state.data.status == ShipmentStatus.RECEIVED
                            && it.state.data.exporter == ourIdentity
                }

        val allParties = with(advisedLoc.state.data) {
            listOf(applicant, issuer, advisingBank, beneficiary)
        }
        val state = shipment.state.data.copy(
                status = ShipmentStatus.DOCUMENTED,
                participants = allParties
        )

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(DispatchPresentReleaseEDoc(), allParties.map { it.owningKey })
                .addOutputState(state)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = (allParties - ourIdentity).map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }

}

@InitiatedBy(DispatchPresentReleaseEDocFlow::class)
class DispatchPresentReleaseEDocFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {}
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

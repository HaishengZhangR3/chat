package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.Ship
import com.r3.demo.contracts.states.*
import com.r3.demo.vaultService
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

// step 5: ship from beneficiary to applicant
@InitiatingFlow
@StartableByService
@StartableByRPC
class ShipFlow(
        private val importer: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // get contract for details
        val contract = vaultService.getVaultStates<SaleContract>()
                .first { it.state.data.buyer == importer
                        && it.state.data.seller == ourIdentity
                }

        // do I get "ISSUED" LoC?
        val issuedLoc = vaultService.getVaultStates<ElectronicLetterOfCredit>()
                .first { it.state.data.status == LoCStatus.ISSUED
                        && it.state.data.beneficiary == ourIdentity
                }

        val state = Shipment(
                exporter = ourIdentity,
                importer = importer,
                productName = contract.state.data.productName,
                timeShipped = Instant.now(),
                timeReceived = null,
                status = ShipmentStatus.SHIPPED,
                participants = listOf(importer, ourIdentity)
        )

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(Ship(), state.participants.map { it.owningKey })
                .addOutputState(state)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = listOf(importer).map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))

    }
}

@InitiatedBy(ShipFlow::class)
class ShipFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {}
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.ReceiveShip
import com.r3.demo.contracts.states.Shipment
import com.r3.demo.contracts.states.ShipmentStatus
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
class ShipReceiveFlow(
        private val exporter: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // get shipment
        val ship = vaultService.getVaultStates<Shipment>()
                .first { it.state.data.exporter == exporter
                        && it.state.data.importer == ourIdentity
                }

        val state = ship.state.data.copy(
                timeReceived = Instant.now(),
                status = ShipmentStatus.RECEIVED
        )

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(ReceiveShip(), state.participants.map { it.owningKey })
                .addInputState(ship)
                .addOutputState(state)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = listOf(exporter).map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))

    }
}

@InitiatedBy(ShipReceiveFlow::class)
class ShipReceiveFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {}
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

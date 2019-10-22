package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.Pay
import com.r3.demo.contracts.states.*
import com.r3.demo.vaultService
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

// step 10: final pay with all of the other states consumed, including the chat!!!
@InitiatingFlow
@StartableByService
@StartableByRPC
class PayFlow(
        private val seller: Party
 ) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // paymend will check and consume:
        //  the only SaleContract,
        //  ADVISED ElectronicLetterOfCredit,
        //  DOCUMENTED Shipment,
        // and produce nothing, just to close.

        val contract = vaultService.getVaultStates<SaleContract>()
                .first { it.state.data.seller == seller
                        && it.state.data.buyer == ourIdentity
                }

        val advisedLoc = vaultService.getVaultStates<ElectronicLetterOfCredit>()
                .first { it.state.data.status == LoCStatus.ADVISED
                        && it.state.data.applicant == ourIdentity
                }

        val shipment = vaultService.getVaultStates<Shipment>()
                .first { it.state.data.status == ShipmentStatus.DOCUMENTED
                        && it.state.data.importer == ourIdentity
                }

        val allParties = with(advisedLoc.state.data) {
            kotlin.collections.listOf(applicant, issuer, advisingBank, beneficiary)
        }
        val counterParty = allParties - ourIdentity

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(Pay(), allParties.map { it.owningKey })
                .addInputState(contract)
                .addInputState(advisedLoc)
                .addInputState(shipment)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = counterParty.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))

    }
}

@InitiatedBy(PayFlow::class)
class PayFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {}
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

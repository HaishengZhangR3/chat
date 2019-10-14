package com.r3.demo.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.demo.contracts.commands.SignContract
import com.r3.demo.contracts.states.SaleContract
import com.r3.demo.vaultService
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

// step 1: create and sign contract
@InitiatingFlow
@StartableByService
@StartableByRPC
class SaleContractFlow(
        private val buyer: Party,
        private val seller: Party,
        private val productName: String,
        private val price: Amount<Currency>
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val saleContract = SaleContract(
                buyer = buyer,
                seller = seller,
                productName = productName,
                price = price
        )

        val txn = TransactionBuilder(vaultService.notary())
                .addCommand(SignContract(), saleContract.participants.map { it.owningKey })
                .addOutputState(saleContract)
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txn)
        val counterPartySessions = (listOf(buyer, seller) - ourIdentity).map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }
}

@InitiatedBy(SaleContractFlow::class)
class SaleContractFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
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

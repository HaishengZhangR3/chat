package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.CloseMeta
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class CloseMetaInfoFlow(
        private val chatId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // get and consume all messages in vault
        val metaInfoStateAndRef = chatVaultService.getMetaInfo(chatId)
        val metaInfo = metaInfoStateAndRef.state.data
        if (ourIdentity != metaInfo.admin) {
            throw FlowException("Only chat admin can close the chat.")
        }

        val txnBuilder = TransactionBuilder(notary = metaInfoStateAndRef.state.notary)
                .addCommand(CloseMeta(), metaInfo.participants.map { it.owningKey })
                .addInputState(metaInfoStateAndRef)
                .also { it.verify(serviceHub) }

        // sign it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySession = metaInfo.receivers.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySession))

        // notify observers (including myself), if the app is listening
        val txn = subFlow(FinalityFlow(collectSignTxn, counterPartySession))

        // notify observers (including myself), if the app is listening
        subFlow(ChatNotifyFlow(info = listOf(metaInfo), command = CloseMeta()))
        return txn
    }
}


/**
 * This is the flow which responds to close chat.
 */
@InitiatedBy(CloseMetaInfoFlow::class)
class CloseMetaInfoFlowResponder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) {
                val metaInfo = serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data }.first() as ChatMetaInfo
                return subFlow(ChatNotifyFlow(info = listOf(metaInfo), command = CloseMeta()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

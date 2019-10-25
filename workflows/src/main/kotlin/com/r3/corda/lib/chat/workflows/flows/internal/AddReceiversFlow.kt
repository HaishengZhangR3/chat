package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AddParticipants
import com.r3.corda.lib.chat.contracts.commands.CreateMeta
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByService
@StartableByRPC
class AddReceiversFlow(
        private val chatId: UniqueIdentifier,
        private val toAdd: List<Party>
) : FlowLogic<StateAndRef<ChatMetaInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatMetaInfo> {
        val metaStateRef = chatVaultService.getMetaInfo(chatId)
        val metaInfo = metaStateRef.state.data

        val participants = (metaInfo.participants + toAdd).distinct()
        val receivers = (metaInfo.receivers + toAdd - ourIdentity).distinct()
        val newMetaInfo = metaInfo.copy(
                participants = participants,
                receivers = receivers
        )

        val txnBuilder = TransactionBuilder(notary = metaStateRef.state.notary)
                .addInputState(metaStateRef)
                .addOutputState(newMetaInfo)
                .addCommand(AddParticipants(), receivers.map { it.owningKey })
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySession = receivers.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySession))
        val txn = subFlow(FinalityFlow(collectSignTxn, counterPartySession))

        // notify observers (including myself), if the app is listening
        subFlow(ChatNotifyFlow(info = newMetaInfo, command = AddParticipants()))

        return txn.coreTransaction.outRefsOfType<ChatMetaInfo>().single()
    }
}

/**
 * This is the flow which responds to create chat.
 */
@InitiatedBy(AddReceiversFlow::class)
class AddReceiversFlowResponder(private val otherSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) {
                val metaInfo = stx.coreTransaction.outputStates.single() as ChatMetaInfo
                return subFlow(ChatNotifyFlow(info = metaInfo, command = AddParticipants()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

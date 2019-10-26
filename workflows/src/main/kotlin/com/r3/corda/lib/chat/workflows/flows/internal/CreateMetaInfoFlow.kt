package com.r3.corda.lib.chat.workflows.flows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.CreateMeta
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.contracts.states.ChatStatus
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByService
@StartableByRPC
class CreateMetaInfoFlow(
        private val chatId: UniqueIdentifier = UniqueIdentifier.fromString(UUID.randomUUID().toString()),
        private val subject: String,
        private val receivers: List<Party>
) : FlowLogic<StateAndRef<ChatMetaInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatMetaInfo> {
        val notary = chatVaultService.notary()
        val chatMetaInfo = ChatMetaInfo(
                linearId = chatId,
                participants = receivers + ourIdentity,
                admin = ourIdentity,
                receivers = receivers,
                subject = subject,
                status = ChatStatus.ACTIVE
        )

        val txnBuilder = TransactionBuilder(notary = notary)
                .addOutputState(chatMetaInfo)
                .addCommand(CreateMeta(), (receivers + ourIdentity).map { it.owningKey })
                .also { it.verify(serviceHub) }

        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val counterPartySession = receivers.map { initiateFlow(it) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySession))
        val txn = subFlow(FinalityFlow(collectSignTxn, counterPartySession))

        // notify observers (including myself), if the app is listening
        subFlow(ChatNotifyFlow(info = listOf(chatMetaInfo), command = CreateMeta()))

        return txn.coreTransaction.outRefsOfType<ChatMetaInfo>().single()
    }
}

@InitiatedBy(CreateMetaInfoFlow::class)
class CreateMetaInfoFlowResponder(private val otherSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) {
                val metaInfo = stx.coreTransaction.outputStates.single() as ChatMetaInfo
                return subFlow(ChatNotifyFlow(info = listOf(metaInfo), command = CreateMeta()))
            }
        }
        val signTxn = subFlow(transactionSigner)
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

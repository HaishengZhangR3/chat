package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Create
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * A flow to create a new chat.
 *
 */
@InitiatingFlow
@StartableByService
@StartableByRPC
class CreateChatFlow(
        val subject: String,
        val content: String,
        val attachment: SecureHash?,
        val from: Party,
        val to: List<Party>
) : FlowLogic<StateAndRef<ChatInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatInfo> {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val newChatInfo = ChatInfo(
                subject = subject,
                content = content,
                attachment = attachment,
                from = from,
                to = to,
                linearId = UniqueIdentifier.fromString(UUID.randomUUID().toString())
        )

        val txnBuilder = TransactionBuilder(notary = notary)
                .addOutputState(newChatInfo)
                .addCommand(Create(), from.owningKey)
                .also {
                    it.verify(serviceHub)
                }

        // finalize it and save to vaults
        val counterpartySessions = to.map { initiateFlow(it) }.toSet()
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        val finalityFlow = subFlow(FinalityFlow(signedTxn, counterpartySessions))
        return finalityFlow.coreTransaction.outRefsOfType<ChatInfo>().single()
    }
}

/**
 * This is the flow which responds to create chat.
 */
@InitiatedBy(CreateChatFlow::class)
class CreateChatFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(flowSession))
    }
}

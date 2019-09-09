package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party

/**
 * A flow to create a new chat.
 *
 */
@StartableByService
@StartableByRPC
class CreateChatFlow(
        val subject: String,
        val content: String,
        val attachment: Attachment?,
        val from: Party,
        val to: List<Party>
) : FlowLogic<StateAndRef<ChatInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatInfo> {
//        require(accountService.accountInfo(subject) == null) {
//            "There is already an account registered with the specified name $subject."
//        }
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val newAccountInfo = AccountInfo(
//                name = name,
//                host = ourIdentity,
//                identifier = UniqueIdentifier(id = identifier)
//        )
//        val transactionBuilder = TransactionBuilder(notary = notary).apply {
//            addOutputState(newAccountInfo)
//            addCommand(Create(), ourIdentity.owningKey)
//        }
//        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
//        val finalisedTransaction = subFlow(FinalityFlow(signedTransaction, emptyList()))
//        return finalisedTransaction.coreTransaction.outRefsOfType<AccountInfo>().single()

    }
}
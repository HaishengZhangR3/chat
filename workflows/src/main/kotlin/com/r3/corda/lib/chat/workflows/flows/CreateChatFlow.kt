package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Create
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
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

        // send to "to" list.
        // why we chat with "send" instead of other way is becasue "send" is non-blockable,
        // meaning, any time, sender send message and save to vault, it'd leave no matter whether receiver get it or not.
        to.map { initiateFlow(it).send(newChatInfo) }

        // save to vault
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)

        return serviceHub.vaultService.queryBy<ChatInfo>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(newChatInfo.linearId))
        ).states.single()
    }
}

/**
 * This is the flow which responds to create chat.
 */
@InitiatedBy(CreateChatFlow::class)
class CreateChatFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        // "receive" a message, then save to vault.
        // even when the node is off for a long time, still the chat will not be blocked by me
        val chatInfo = flowSession.receive<ChatInfo>().unwrap{it}
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txnBuilder = TransactionBuilder(notary = notary)
                .addOutputState(chatInfo)
                .addCommand(Create(), listOf(serviceHub.myInfo.legalIdentities.single().owningKey))
                .also {
                    it.verify(serviceHub)
                }

        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
    }
}

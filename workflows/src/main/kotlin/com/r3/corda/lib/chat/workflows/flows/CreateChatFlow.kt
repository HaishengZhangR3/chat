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
                // no input
                .addOutputState(newChatInfo)
                .addCommand(Create(), from.owningKey)
                .also {
                    it.verify(serviceHub)
                }

        // send to "to" list.
        // why we chat with "send" instead of other way is because "send" is non-blockable,
        // meaning, any time, sender send message and save to vault, it'd leave no matter whether receiver get it or not.
        to.map { initiateFlow(it).send(newChatInfo) }

        // save to vault
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)

        return ServiceUtils.getChatHead(serviceHub, newChatInfo.linearId)
    }
}

/**
 * This is the flow which responds to create chat.
 */
@InitiatedBy(CreateChatFlow::class)
class CreateChatFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        // @TODO choose notary should be put to service utils because different parties may choose different notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // @TODO: use FlowLogic::receiveAllMap or ::receiveAll for better performance
        // "receive" a message, then save to vault.
        val chatInfo = flowSession.receive<ChatInfo>().unwrap{it}
        val txnBuilder = TransactionBuilder(notary = notary)
                // no input
                .addOutputState(chatInfo)
                .addCommand(Create(), listOf(ourIdentity.owningKey))
                .also {
                    it.verify(serviceHub)
                }

        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)
    }
}

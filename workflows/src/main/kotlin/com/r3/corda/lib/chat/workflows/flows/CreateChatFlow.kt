package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.Create
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.service.NotifyFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
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
        // @todo: who decides notary? pass in or Chat app?
        private val subject: String,
        private val content: String,
        private val attachment: SecureHash?,
        private val to: List<Party>
) : FlowLogic<StateAndRef<ChatInfo>>() {

    @Suspendable
    override fun call(): StateAndRef<ChatInfo> {
        val notary = chatVaultService.notary()
        val toList = (to - ourIdentity).distinct()
        val newChatInfo = ChatInfo(
                linearId = UniqueIdentifier.fromString(UUID.randomUUID().toString()),
                subject = subject,
                content = content,
                attachment = attachment,
                from = ourIdentity,
                to = toList,
                participants = listOf(ourIdentity)
        )

        val txnBuilder = TransactionBuilder(notary = notary)
                .addOutputState(newChatInfo)
                .addCommand(Create(), ourIdentity.owningKey)
                .also { it.verify(serviceHub) }

        // send to "to" list.
        // why we chat with "send" instead of other way is because "send" is non-blockable,
        // meaning, any time, sender send message and save to vault, it'd leave no matter whether receiver get it or not.
        toList.map { initiateFlow(it).send(newChatInfo) }

        // save to vault
        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)

        // @todo this is a very important step: to notice the caller CorDapp that we have a new msg
        //       doing this can be achieved by observer mode (notify to whom should know), refer to:
        //              https://github.com/roger3cev/observable-states
        //       the observer mode should apply every time when we save anything to vault, i.e.,
        //       a proposal, an agree, new message or reply message to audience.
        //       and the caller CorDapp should implement the @InitiatedBy flow to handle this.
        //       Typically, it'll setup a websocket to let UI knows the change and call API to get details
        // @todo meantime, the caller application who receives the notification, can do extend,
        //       i.e., parse SWIFT message:
        //          the subject is the SWIFT code,
        //          the message content is SWIFT body,
        //          caller application parses the chat message and do further action based on the SWIFT code
        return signedTxn.coreTransaction.outRefsOfType<ChatInfo>().single()
    }
}

/**
 * This is the flow which responds to create chat.
 */
@InitiatedBy(CreateChatFlow::class)
class CreateChatFlowResponder(private val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val notary = chatVaultService.notary()

        // "receive" a message, then save to vault.
        val chatInfo = flowSession.receive<ChatInfo>().unwrap{ it }
        val txnBuilder = TransactionBuilder(notary = notary)
                .addOutputState(chatInfo.copy(participants = listOf(ourIdentity)))
                .addCommand(Create(), listOf(ourIdentity.owningKey))
                .also { it.verify(serviceHub) }

        val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
        serviceHub.recordTransactions(signedTxn)

        // notify caller app of the event, if the app is listening
        subFlow(NotifyFlow(chatInfo = chatInfo, command = Create()))
        return signedTxn
    }
}

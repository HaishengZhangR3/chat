package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.commands.AddParticipants
import com.r3.corda.lib.chat.contracts.commands.Create
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

/**
 * A flow to add participants in a chat.
 *
 */
@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsFlow(
        private val toAdd: List<Party>,
        private val includingHistoryChat: Boolean,
        private val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // reply which chat thread? should get the head of the chat thread from storage based on the linearId
        val inputChatInfo = ServiceUtils.getChatHead(serviceHub, linearId)

        // final to list: existing one + new added
        val toParties = inputChatInfo.state.data.to + toAdd

        val outputChatInfo = ChatInfo(
                from = ourIdentity,
                to = toParties,
                linearId = linearId
        )

        val txnBuilder = TransactionBuilder(notary = inputChatInfo.state.notary)
                .addInputState(inputChatInfo)
                .addOutputState(outputChatInfo)
                .addCommand(AddParticipants(), toParties.map { it.owningKey })
                .also {
                    it.verify(serviceHub)
                }

        // need every parties to sign and close it
        val selfSignedTxn = serviceHub.signInitialTransaction(txnBuilder)

        val counterPartySessions = toParties.map { initiateFlow(it) }

        counterPartySessions.map { it.send(outputChatInfo) }
        val collectSignTxn = subFlow(CollectSignaturesFlow(selfSignedTxn, counterPartySessions))

        // other side accepted, then share the history chat to new added ones
        // should be in ReplyAgreeAddParticipantsFlowResponder
        if (includingHistoryChat) {
            sendHistoryChat(toAdd, linearId)
        }

        // finalise it
        return subFlow(FinalityFlow(collectSignTxn, counterPartySessions))
    }

    private fun sendHistoryChat(to: List<Party>, chatId: UniqueIdentifier) {
        val historyChats = ServiceUtils.getActiveChats(serviceHub, linearId)
        toAdd.map { initiateFlow(it).send(historyChats) }
    }
}

/**
 * This is the flow which responds to add participants flow.
 */
@InitiatedBy(AddParticipantsFlow::class)
class ReplyAddParticipantsFlowResponder(val otherSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // "receive" a message, then save to vault.
        // even when the node is off for a long time, still the chat will not be blocked by me
        val chatInfo = otherSession.receive<ChatInfo>().unwrap{ it }

        val headChatInfo = try { ServiceUtils.getChatHead(serviceHub, chatInfo.linearId) } catch (e: FlowException) { null}

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction): Unit {
            }
        }
        val signTxn = subFlow(transactionSigner)

        when (headChatInfo) {
            // new added party
            null -> {
                val notary = ServiceUtils.notary(serviceHub)

                val historyChatInfo = otherSession.receive<List<ChatInfo>>().unwrap { it }
                historyChatInfo.map {
                    val txnBuilder = TransactionBuilder(notary = notary)
                            // no input
                            .addOutputState(it)
                            .addCommand(Create(), it.from.owningKey)
                    txnBuilder.verify(serviceHub)

                    val signedTxn = serviceHub.signInitialTransaction(txnBuilder)
                    serviceHub.recordTransactions(signedTxn)
                }
            }

            // existing party
            else -> {
            }
        }
        return subFlow(ReceiveFinalityFlow(otherSession, signTxn.id))
    }
}

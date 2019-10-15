package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.*
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.ChatMessageType
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class ChatInfoContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        // @todo: carefully check every contract check

        when (command.value) {
            is Create -> {
                require(tx.inputStates.isEmpty()) { "There should be no input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                require(command.signers.size == 1) { "There should only be one required signer for a chat: sender." }

                val output = tx.outputStates.single() as ChatInfo
                require(output.receivers.isNotEmpty()) { "The receiver list should not be empty." }
                require(!output.receivers.contains(output.sender)) { "Sender should not be in receiver list."}
                require(output.receivers.distinct().size == output.receivers.size) { "Receiver list should not have duplicate."}

                require(output.chatMessageType == ChatMessageType.USER) {"User should not create ChatMessageType.SYSTEM message."}
            }
            is Reply -> {
                require(tx.inputStates.isEmpty()) { "There should only be one input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer for a chat: from." }
            }
            is ShareHistory -> {
                require(tx.inputStates.isEmpty()) { "There should only be one input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer for a chat: from." }
            }

            // @todo: Close should consume "ChatInfo" and "CLoseChatState",
            // @todo: add a ShutDown command to only consume "ChatInfo"
            is Close -> {
                require(tx.inputStates.size >= 1) { "There should be more than one input chat state." }
                require(tx.outputStates.size == 0) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should be more than one required signer for a chat: from and to list." }

            }

            else -> {
                throw NotSupportedException()
            }
        }
    }
}

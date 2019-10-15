package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.*
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class CloseChatContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        // @todo: carefully check every contract check

        when (command.value) {
            is ProposeClose -> {
                require(tx.inputStates.isEmpty()) { "There should be no input chat state." }
                require(tx.outputStates.size == 1) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should be more than one required signer for a chat: from and to list." }

            }
            is AgreeClose -> {
                require(tx.inputStates.isEmpty()) { "There should be no input chat state." }
                require(tx.outputStates.size == 1) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should be more than one required signer for a chat: from and to list." }

            }

            is RejectClose -> {
                require(tx.inputStates.isNotEmpty()) { "There should be more input state." }
                require(tx.outputStates.isEmpty()) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.isNotEmpty()) { "There should be more than one required signer for a chat: from and to list." }

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

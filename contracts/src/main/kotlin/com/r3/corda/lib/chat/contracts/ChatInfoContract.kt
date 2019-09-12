package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.Reply
import com.r3.corda.lib.chat.contracts.commands.ChatCommand
import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.commands.Create
import com.r3.corda.lib.chat.contracts.states.ChatInfo
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class ChatInfoContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        // common check
        // from must be me


        // per command check
        when (command.value) {
            is Create -> {
                require(tx.inputStates.size == 0) { "There should be no input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer for a chat: from." }

                // out.linearId must != null

            }
            is Reply -> {
                require(tx.inputStates.size == 1) { "There should only be one input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer for a chat: from." }

                // in.linearId must == out.linearId
                val input = tx.inputStates.single() as ChatInfo
                val output = tx.outputStates.single() as ChatInfo
                require(input.linearId == output.linearId) { "Both input and output should have the same ID so that they are in one chat thread." }
            }
            is Close -> {
                require(tx.inputStates.size == 1) { "There should only be one input chat state." }
                require(tx.outputStates.size == 0) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer for a chat: from." }

                // in.linearId must != null
            }
            else ->
                throw NotSupportedException()
        }
    }
}

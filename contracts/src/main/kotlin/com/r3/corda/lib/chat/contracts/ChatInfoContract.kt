package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.*
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
                // to list should not be empty
                // to list should not have duplicate

            }
            is Reply -> {
                require(tx.inputStates.size == 1) { "There should only be one input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer for a chat: from." }

                // toAdd list should not be empty

                // in.linearId must == out.linearId
                val input = tx.inputStates.single() as ChatInfo
                val output = tx.outputStates.single() as ChatInfo
                require(input.linearId == output.linearId) { "Both input and output should have the same ID so that they are in one chat thread." }
            }
            is Close -> {
                require(tx.inputStates.size == 1) { "There should only be one input chat state." }
                require(tx.outputStates.size == 0) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should more one required signer for a chat: from and to list." }

                // toAdd list should not be empty
                // in.linearId must != null
            }
            is AddParticipants -> {
                require(tx.inputStates.size == 1) { "There should only be one input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should more one required signer for a chat: from and to list." }

                // action must be add
                // toAdd list should not have duplicate
                // toAdd list should not be empty
                // in.linearId must != null

                // new added must not be in existing list
                // max amount of to?
            }
            is RemoveParticipants -> {
                require(tx.inputStates.size == 1) { "There should only be one input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size > 1) { "There should more one required signer for a chat: from and to list." }

                // toRemove list should not be empty
                // remainning to list should not be empty
                // in.linearId must != null

                // toRemove must be in existing list
                // toRemove list should not have duplicate
            }
            else -> {
                throw NotSupportedException()
            }
        }
    }
}

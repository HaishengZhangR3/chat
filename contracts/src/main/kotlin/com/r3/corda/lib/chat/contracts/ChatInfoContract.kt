package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.*
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class ChatInfoContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        // common check

        // per command check
        when (command.value) {
            is Create -> {
                require(tx.inputStates.isEmpty()) { "There should be no input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer for a chat: from." }

                // to list should not be empty
                // from should not be in to list
                // to list should not have duplicate
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
            is Close -> {
                require(tx.inputStates.size >= 1) { "There should be more than one input chat state." }
                require(tx.outputStates.size == 0) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should be more than one required signer for a chat: from and to list." }

            }
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

            is ProposeUpdateParticipants -> {
                require(tx.inputStates.isEmpty()) { "There should be no input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.isNotEmpty()) { "There should more one required signer for a chat: from and to list." }

                val outputState = tx.outputStates.single() as UpdateParticipantsState
                require(outputState.toAdd.isNotEmpty() || outputState.toRemove.isNotEmpty()) { "One of ToAdd or ToRemove should not be empty"}
                require(outputState.toAdd.distinct().size == outputState.toAdd.size) { "toAdd list should not have duplicate"}

                require(outputState.toRemove.distinct().size == outputState.toRemove.size) { "toAdd list should not have duplicate"}

                require(outputState.toAdd.intersect(outputState.toRemove).isEmpty()) { "ToAdd should not overlap with ToRemove"}

            }
            is RejectUpdateParticipants -> {
                require(tx.inputStates.isNotEmpty()) { "There should be more input chat state." }
                require(tx.outputStates.isEmpty()) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.isNotEmpty()) { "There should more one required signer for a chat: from and to list." }
            }
            is AgreeUpdateParticipants -> {
                require(tx.inputStates.isEmpty()) { "There should only be one input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.isNotEmpty()) { "There should more one required signer for a chat: from and to list." }
            }
            else -> {
                throw NotSupportedException()
            }
        }
    }
}

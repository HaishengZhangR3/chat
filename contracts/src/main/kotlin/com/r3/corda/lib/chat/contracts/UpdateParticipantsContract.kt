package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.*
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class UpdateParticipantsContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        // @todo: carefully check every contract check

        when (command.value) {
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
            is UpdateParticipants -> {
                val requiredSigners = command.signers
                require(requiredSigners.isNotEmpty()) { "There should be more than one required signer for a chat: from and to list." }

            }
            else -> {
                throw NotSupportedException()
            }
        }
    }
}

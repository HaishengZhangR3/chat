package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.*
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class ChatMetaInfoContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        when (command.value) {
            is CreateMeta -> {
                require(tx.inputStates.isEmpty()) { "There should be no input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                require(command.signers.size >= 1) { "There should only be one required signer for a chat: sender." }

                val output = tx.outputStates.single() as ChatMetaInfo
                require(output.receivers.isNotEmpty()) { "The receiver list should not be empty, or include only yourself." }
                require(!output.receivers.contains(output.admin)) { "Sender should not be in receiver list."}
                require(output.receivers.distinct().size == output.receivers.size) { "Receiver list should not have duplicate."}

            }
            is CloseMeta -> {
                require(tx.inputStates.size >= 1) { "There should be more than one input chat state." }
                require(tx.outputStates.size == 0) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should be more than one required signer for a chat: from and to list." }

            }
            is AddParticipants -> {
//                require(tx.inputStates.size == 1) { "There should be more than one input chat state." }
//                require(tx.outputStates.size == 1) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should be more than one required signer for a chat: from and to list." }

            }
            is RemoveParticipants -> {
                require(tx.inputStates.size == 1) { "There should be more than one input chat state." }
                require(tx.outputStates.size == 1) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should be more than one required signer for a chat: from and to list." }

            }
            else -> {
                throw NotSupportedException()
            }
        }
    }
}

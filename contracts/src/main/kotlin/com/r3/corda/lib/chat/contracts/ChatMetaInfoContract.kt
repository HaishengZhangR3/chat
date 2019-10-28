package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.*
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.contracts.states.ChatStatus
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
                require(tx.outputStates.size == 1) { "There should only be one output." }
                require(tx.outputStates.single() is ChatMetaInfo) { "The output should be ChatMetaInfo instance." }
                require(command.signers.size >= 1) { "There should be more than one required signer." }

                val output = tx.outputStates.single() as ChatMetaInfo
                require(output.receivers.isNotEmpty()) { "The receivers must not be empty." }
                require(!output.receivers.contains(output.admin)) { "Admin must not be in receivers."}
                require(output.receivers.distinct().size == output.receivers.size) { "Receiver list should not have duplicate."}
                require(output.status == ChatStatus.ACTIVE) { "The chat status must be Active." }

            }
            is CloseMeta -> {
                require(tx.inputStates.size >= 1) { "There should be more than one input chat state." }
                require(tx.outputStates.size == 0) { "There should be no output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size >= 1) { "There should be more than one required signer for a chat: from and to list." }

            }
            is AddReceivers -> {
            }
            is RemoveReceivers -> {
            }
            else -> {
                throw NotSupportedException()
            }
        }
    }
}

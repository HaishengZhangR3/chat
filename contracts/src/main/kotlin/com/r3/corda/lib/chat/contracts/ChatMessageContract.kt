package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.ChatCommand
import com.r3.corda.lib.chat.contracts.commands.CloseMessages
import com.r3.corda.lib.chat.contracts.commands.CreateMessage
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class ChatMessageContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        when (command.value) {
            is CreateMessage ->{
                require(tx.inputStates.isEmpty()) { "There should only be one input chat state." }
                require(tx.outputStates.size == 1) { "There should only be one output chat state." }
                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer for a chat: from." }
            }
            is CloseMessages -> {
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

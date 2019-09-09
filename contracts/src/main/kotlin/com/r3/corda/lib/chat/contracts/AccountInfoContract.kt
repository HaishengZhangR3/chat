package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.Reply
import com.r3.corda.lib.chat.contracts.commands.ChatCommand
import com.r3.corda.lib.chat.contracts.commands.Close
import com.r3.corda.lib.chat.contracts.commands.Create
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class AccountInfoContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        // common check

        // per command check
        if (command.value is Create) {
            require(tx.inputStates.size == 0) { "There should be no input chat state." }
            require(tx.outputStates.size == 1) { "There should only be one output chat state." }
            val requiredSigners = command.signers
            require(requiredSigners.size >= 2) { "There should be at least two required signer for a chat: from and to." }

        } else if (command.value is Reply) {
            require(tx.inputStates.size == 1) { "There should only be one input chat state." }
            require(tx.outputStates.size == 1) { "There should only be one output chat state." }
            val requiredSigners = command.signers
            require(requiredSigners.size >= 2) { "There should be at least two required signer for a chat: from and to." }
        } else if (command.value is Close) {
            require(tx.inputStates.size == 1) { "There should only be one input chat state." }
            require(tx.outputStates.size == 0) { "There should be no output chat state." }
            val requiredSigners = command.signers
            require(requiredSigners.size >= 2) { "There should be at least two required signer for a chat: from and to." }
        } else {
            throw NotSupportedException()
        }
    }
}

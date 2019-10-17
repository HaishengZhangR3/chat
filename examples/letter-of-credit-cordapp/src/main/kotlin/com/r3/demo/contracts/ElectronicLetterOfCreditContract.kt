package com.r3.demo.contracts

import com.r3.demo.contracts.commands.LoCCommand
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class ElectronicLetterOfCreditContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(LoCCommand::class.java)

        // per command check
        when (command.value) {

            else -> {
                throw NotSupportedException()
            }
        }
    }
}

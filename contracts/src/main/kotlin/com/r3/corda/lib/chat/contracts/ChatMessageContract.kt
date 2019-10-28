package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.ChatCommand
import com.r3.corda.lib.chat.contracts.commands.CloseMessages
import com.r3.corda.lib.chat.contracts.commands.CreateMessage
import com.r3.corda.lib.chat.contracts.states.ChatID
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import javax.transaction.NotSupportedException

class ChatMessageContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand(ChatCommand::class.java)

        when (command.value) {
            is CreateMessage ->{
                require(tx.referenceStates.size == 1) { "There should be a reference input." }
                require(tx.referenceStates.single() is ChatMetaInfo) { "The reference input should be ChatMetaInfo instance." }
                require(tx.inputStates.isEmpty()) { "There should be no input." }
                require(tx.outputStates.size == 1) { "There should only be one output." }
                require(tx.outputStates.single() is ChatMessage) { "The output should be ChatMessage instance." }

                val chatMeta = tx.referenceStates.single() as ChatMetaInfo
                val chatMessage = tx.outputStates.single() as ChatMessage
                require(chatMessage.chatId == chatMeta.linearId) {"chatId/linearId in two states must be same."}

                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should only be one required signer." }
            }
            is CloseMessages -> {
                require(tx.referenceStates.size == 1) { "There should be a reference input." }
                require(tx.referenceStates.single() is ChatMetaInfo) { "The reference input should be ChatMetaInfo instance." }
                require(tx.inputStates.size >= 1) { "There should be more than one input." }
                require(tx.outputStates.isEmpty()) { "There should be no output." }

                val chatMeta = tx.referenceStates.single() as ChatMetaInfo
                val chatIds = mutableSetOf<ChatID>()
                tx.inputStates.forEach {
                    require(it is ChatMessage) { "The output should be ChatMessage instance." }
                    it as ChatMessage
                    chatIds.add(it.chatId)
                }
                chatIds.add(chatMeta.linearId)
                require(chatIds.size == 1) { "All of the inputs should have same chat ID." }

                val requiredSigners = command.signers
                require(requiredSigners.size == 1) { "There should be only one required signer." }
            }
            else -> {
                throw NotSupportedException()
            }
        }
    }
}

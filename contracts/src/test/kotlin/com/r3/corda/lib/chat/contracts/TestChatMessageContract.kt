package com.r3.corda.lib.chat.contracts

import com.r3.corda.lib.chat.contracts.commands.CreateMessage
import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.contracts.states.ChatStatus
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant
import java.util.*

// @todo: add tests for contract and state
class TestChatMessageContract {

    class DummyCommand : TypeOnlyCommandData()
    private var ledgerServices = MockServices(listOf("com.r3.corda.lib.chat"))
    private val partyA = ledgerServices

    @Test
    fun mustIncludeIssueCommand() {
        val id = UniqueIdentifier.fromString(UUID.randomUUID().toString())
        val meta = ChatMetaInfo(
                linearId = id,
                created = Instant.now(),
                participants = listOf(ALICE.party, BOB.party),
                admin = ALICE.party,
                receivers = listOf( BOB.party),
                subject = "subject",
                status = ChatStatus.ACTIVE
        )
        val message = ChatMessage(
                chatId = id,
                participants = listOf(ALICE.party),
                created = Instant.now(),
                content = "content",
                sender = ALICE.party
        )

        // @TODO
        // MockServices only assume the version is 1, while reference is only supported in 4, so fails here
        ledgerServices.ledger {
            transaction {
                this.reference(ChatMetaInfoContract.CHAT_METAINFO_CONTRACT_ID, meta)
                output(ChatMessageContract.CHAT_MESSAGE_CONTRACT_ID, message)
                command(listOf(ALICE.publicKey), CreateMessage())

                this.verifies()
            }
        }
    }
}

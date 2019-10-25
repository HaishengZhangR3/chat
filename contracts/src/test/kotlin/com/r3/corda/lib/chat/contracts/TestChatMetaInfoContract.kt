package com.r3.corda.lib.chat.contracts

import net.corda.core.contracts.*
import net.corda.testing.contracts.DummyState
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.*

// @todo: add tests for contract and state
class TestChatMetaInfoContract {

    class DummyCommand : TypeOnlyCommandData()
    private var ledgerServices = MockServices(listOf("net.corda.training"))

    @Test
    fun mustIncludeIssueCommand() {

    }

}

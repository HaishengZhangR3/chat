package com.r3.corda.lib.chat.workflows.test.internal

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsProposeFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsRejectFlow
import com.r3.corda.lib.chat.workflows.test.observer.ObserverUtils
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class UpdateParticipantsRejectFlowTests {

    lateinit var network: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode
    lateinit var nodeC: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.r3.corda.lib.chat.contracts"),
                    TestCordapp.findCordapp("com.r3.corda.lib.chat.workflows")
                )
            )
        )
        nodeA = network.createPartyNode()
        nodeB = network.createPartyNode()
        nodeC = network.createPartyNode()
        ObserverUtils.registerObserver(listOf(nodeA, nodeB, nodeC))

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `should be possible to close a chat`() {

        // 1 create one
        val newChatFlow = nodeA.startFlow(CreateChatFlow(
                "subject",
                "content",
                null,
                listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        newChatFlow.getOrThrow()

        val newChatB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single().state.data

        // 2. add new participants
        val addParticipantsFlow = nodeB.startFlow(
                UpdateParticipantsProposeFlow(
                        toAdd = listOf(nodeC.info.legalIdentities.single()),
                        includingHistoryChat = true,
                        chatId = newChatB.linearId
                )
        )

        network.runNetwork()
        addParticipantsFlow.getOrThrow()

        // 4. propose close chat
        val reject = nodeB.startFlow(
                UpdateParticipantsRejectFlow(
                        newChatB.linearId
                )
        )
        network.runNetwork()
        reject.getOrThrow()

        // there are 0 chat on ledge in each node
        val updateA = nodeA.services.vaultService.queryBy(UpdateParticipantsState::class.java).states
        val updateB = nodeB.services.vaultService.queryBy(UpdateParticipantsState::class.java).states
        Assert.assertTrue(updateA.size == 0)
        Assert.assertTrue(updateB.size == 0)
    }
}

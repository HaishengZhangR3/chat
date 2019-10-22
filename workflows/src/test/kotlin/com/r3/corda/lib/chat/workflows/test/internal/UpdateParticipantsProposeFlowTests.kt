package com.r3.corda.lib.chat.workflows.test.internal

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsProposeFlow
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

class UpdateParticipantsProposeFlowTests {

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
    fun `should be possible to propose to add participants to a chat`() {

        // 1 create one
        val newChatFlow = nodeA.startFlow(CreateChatFlow(
                "subject",
                "content",
                null,
                listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        newChatFlow.getOrThrow()

        val newChatInfoInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single()

        // 2. add new participants
        val addParticipantsFlow = nodeB.startFlow(
                UpdateParticipantsProposeFlow(
                        toAdd = listOf(nodeC.info.legalIdentities.single()),
                        includingHistoryChat =  true,
                        chatId = newChatInfoInVaultB.state.data.linearId
                )
        )

        network.runNetwork()
        addParticipantsFlow.getOrThrow()

        // check whether the created one in node B is same as that in the DB of host node A
        val proposalA = nodeA.services.vaultService.queryBy(UpdateParticipantsState::class.java).states.single()
        val proposalB = nodeB.services.vaultService.queryBy(UpdateParticipantsState::class.java).states.single()
        Assert.assertTrue(proposalA.state == proposalB.state)
    }
}

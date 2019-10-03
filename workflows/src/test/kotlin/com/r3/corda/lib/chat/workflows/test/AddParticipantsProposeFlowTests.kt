package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.ParticipantsUpdateState
import com.r3.corda.lib.chat.workflows.flows.AddParticipantsProposeFlow
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
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

class AddParticipantsProposeFlowTests {

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
                nodeA.info.legalIdentities.single(),
                listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        val newChatInfo = newChatFlow.getOrThrow()

        val newChatInfoInVaultA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states.single()
        Assert.assertTrue(newChatInfo == newChatInfoInVaultA)

        // check whether the created one in node B is same as that in the DB of host node A
        val newChatInfoInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single()
        Assert.assertTrue(newChatInfoInVaultA.state.data.linearId == newChatInfoInVaultB.state.data.linearId)

        // 2. add new participants
        val addParticipantsFlow = nodeB.startFlow(
                AddParticipantsProposeFlow(
                        listOf(nodeC.info.legalIdentities.single()),
                        true,
                        newChatInfoInVaultB.state.data.linearId
                )
        )

        network.runNetwork()
        addParticipantsFlow.getOrThrow()

        // check whether the created one in node B is same as that in the DB of host node A
        val proposalB = nodeB.services.vaultService.queryBy(ParticipantsUpdateState::class.java).states.single()
        val proposalC = nodeC.services.vaultService.queryBy(ParticipantsUpdateState::class.java).states.single()
        val proposalA = nodeA.services.vaultService.queryBy(ParticipantsUpdateState::class.java).states.single()
        Assert.assertTrue(proposalA.state == proposalB.state)
        Assert.assertTrue(proposalC.state == proposalB.state)


    }
}

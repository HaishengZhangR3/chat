package com.r3.corda.lib.chat.workflows.test.internal

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsAgreeFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsProposeFlow
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

class UpdateParticipantsFlowTests {

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
                listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        newChatFlow.getOrThrow()

        val chatInB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single().state.data

        // 2. add new participants
        val addParticipantsFlow = nodeB.startFlow(
                UpdateParticipantsProposeFlow(
                        toAdd = listOf(nodeC.info.legalIdentities.single()),
                        includingHistoryChat = true,
                        chatId = chatInB.linearId
                )
        )

        network.runNetwork()
        addParticipantsFlow.getOrThrow()

        // 3. agree
        val agreeParticipantsFlowA = nodeA.startFlow(
                UpdateParticipantsAgreeFlow(
                        chatInB.linearId
                )
        )
        network.runNetwork()
        agreeParticipantsFlowA.getOrThrow()

        // 4. do update
        val updateParticipantsFlowB = nodeB.startFlow(
                UpdateParticipantsFlow(
                        chatId = chatInB.linearId,
                        force = true
                )
        )
        network.runNetwork()
        updateParticipantsFlowB.getOrThrow()

        // chatinfo should not be consumed in A,B, and new one should be in C
        val chatInfosInVaultA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states
        val chatInfosInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states
        val chatInfosInVaultC = nodeC.services.vaultService.queryBy(ChatInfo::class.java).states
        Assert.assertEquals(chatInfosInVaultA.size, 2)
        Assert.assertEquals(chatInfosInVaultB.size, 2)
        Assert.assertEquals(chatInfosInVaultC.size, 2)

        Assert.assertEquals(
                (chatInfosInVaultA.map { it.state.data.linearId }
                        + chatInfosInVaultA.map { it.state.data.linearId }
                        + chatInfosInVaultA.map { it.state.data.linearId })
                        .distinct().size,
                1
        )

        // no update state result any more
        val updateParticipantsA = nodeA.services.vaultService.queryBy(UpdateParticipantsState::class.java).states
        val updateParticipantsB = nodeB.services.vaultService.queryBy(UpdateParticipantsState::class.java).states
        val updateParticipantsC = nodeC.services.vaultService.queryBy(UpdateParticipantsState::class.java).states
        Assert.assertEquals(updateParticipantsA.size, 0)
        Assert.assertEquals(updateParticipantsB.size, 0)
        Assert.assertEquals(updateParticipantsC.size, 0)

    }
}

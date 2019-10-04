package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.ShareChatHistoryFlow
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

class ShareChatHistoryFlowTests {

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
    fun `should be possible to share chat history to new added participants`() {

        // 1 create one
        val newChatFlow = nodeA.startFlow(CreateChatFlow(
                "subject",
                "content",
                null,
                listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        newChatFlow.getOrThrow()

        val newChatInfoA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states.single().state.data
        val newChatInfoB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single().state.data

        // 2. share to C
        val shareChatHistoryFlow = nodeB.startFlow(
                ShareChatHistoryFlow(
                        listOf(nodeC.info.legalIdentities.single()),
                        newChatInfoB.linearId
                )
        )

        network.runNetwork()
        shareChatHistoryFlow.getOrThrow()

        // check whether the created one in node B is same as that in the DB of host node A
        val shareChatInfoInVaultC = nodeC.services.vaultService.queryBy(ChatInfo::class.java).states
        Assert.assertEquals(shareChatInfoInVaultC.size, 1)

        val newChatInfoC = shareChatInfoInVaultC.single().state.data
        Assert.assertTrue(newChatInfoC.linearId == newChatInfoB.linearId)

        // participants check
        val participantC = newChatInfoC.participants
        val participantA = newChatInfoA.participants
        Assert.assertEquals(participantC.size, 1)
        Assert.assertEquals(participantA.size, 1)
        Assert.assertFalse(participantA.single().toString().equals(participantC.single().toString()))
    }
}

package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
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


// @todo:
// . more detailed tests are needed.
// . simultaneously trigger of reply or add/remove or close not tested,
// . performance test not done,


class CreateChatFlowTests {

    lateinit var network: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode

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
        ObserverUtils.registerObserver(listOf(nodeA, nodeB))

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `should be possible to create a chat`() {

        val chatFlow = nodeA.startFlow(CreateChatFlow(
                subject = "subject",
                content = "content",
                receivers = listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        val txn = chatFlow.getOrThrow()
        val chatInfo = txn.state.data

        val chatInfoInVaultA = nodeA.services.vaultService.queryBy(ChatMessage::class.java).states.single()
        Assert.assertTrue(chatInfo == chatInfoInVaultA.state.data)

        //check whether the created one in node B is same as that in the DB of host node A
        val chatInfoInVaultB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single()
        Assert.assertTrue(chatInfoInVaultA.state.data.linearId == chatInfoInVaultB.state.data.linearId)

        // same chat in two nodes should have diff participants
        val participantsA = chatInfoInVaultA.state.data.participants
        val participantsB = chatInfoInVaultB.state.data.participants
        Assert.assertEquals(participantsA.size,1)
        Assert.assertEquals(participantsB.size,1)

        val participantA = participantsA.single()
        val participantB = participantsB.single()
        Assert.assertFalse(participantA.nameOrNull().toString().equals(participantB.nameOrNull().toString()))

    }
}

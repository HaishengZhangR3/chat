package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.TestCreateChatFlow
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

class TestCreateChatFlowTests {

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

        val chatFlow = nodeA.startFlow(TestCreateChatFlow(
                subject = "subject",
                content = "content",
                receivers = listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        val txn = chatFlow.getOrThrow()
        val chatInfo = txn.state.data

        val chatMessageA = nodeA.services.vaultService.queryBy(ChatMessage::class.java).states.single()
        val chatMessageB = nodeB.services.vaultService.queryBy(ChatMessage::class.java).states.single()
        Assert.assertTrue(chatInfo == chatMessageA.state.data)
        Assert.assertTrue(chatMessageB.state.data.linearId == chatMessageA.state.data.linearId)
        Assert.assertTrue(chatMessageB.state.data.content == chatMessageA.state.data.content)

        // same chat in two nodes should have diff participants
        val msgPartiesA = chatMessageA.state.data.participants
        val msgPartiesB = chatMessageB.state.data.participants
        Assert.assertEquals(msgPartiesA.size,1)
        Assert.assertEquals(msgPartiesB.size,1)

        val msgPartyA = msgPartiesA.single()
        val msgPartyB = msgPartiesB.single()
        Assert.assertFalse(msgPartyA.nameOrNull().toString().equals(msgPartyB.nameOrNull().toString()))

        //check whether the created one in node B is same as that in the DB of host node A
        val chatMetaA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single()
        val chatMetaB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single()
        Assert.assertTrue(chatMetaB.state.data.linearId == chatMetaA.state.data.linearId)

        // same chat meta in two nodes should have same participants: admin itself
        val metaPartiesA = chatMetaA.state.data.participants
        val metaPartiesB = chatMetaB.state.data.participants
        Assert.assertEquals(metaPartiesA.size,1)
        Assert.assertEquals(metaPartiesB.size,1)

        Assert.assertTrue(metaPartiesA.subtract(metaPartiesB).isEmpty())
        Assert.assertTrue(metaPartiesB.subtract(metaPartiesA).isEmpty())

    }
}

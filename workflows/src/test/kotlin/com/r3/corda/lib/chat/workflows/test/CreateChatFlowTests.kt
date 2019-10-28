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

        val chatMessageA = nodeA.services.vaultService.queryBy(ChatMessage::class.java).states.single()
        val chatMessageB = nodeB.services.vaultService.queryBy(ChatMessage::class.java).states.single()
        Assert.assertTrue(chatInfo == chatMessageA.state.data)
        Assert.assertTrue(chatMessageB.state.data.chatId == chatMessageA.state.data.chatId)
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
        Assert.assertEquals(metaPartiesA.size,2)
        Assert.assertEquals(metaPartiesB.size,2)

        Assert.assertTrue(metaPartiesA.subtract(metaPartiesB).isEmpty())
        Assert.assertTrue(metaPartiesB.subtract(metaPartiesA).isEmpty())

    }

    @Test
    fun `create chat should follow constrains`() {

        val chatFlow = nodeA.startFlow(CreateChatFlow(
                subject = "subject",
                content = "content",
                receivers = listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        chatFlow.getOrThrow()

        val chatMessageA = nodeA.services.vaultService.queryBy(ChatMessage::class.java).states.single()
        val chatMessageB = nodeB.services.vaultService.queryBy(ChatMessage::class.java).states.single()

        val chatMetaA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single()
        val chatMetaB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single()


        // the following tests are based on "state machine" constrains
        // chat admin must be the chat initiator
        Assert.assertTrue(chatMetaA.state.data.admin.equals(nodeA.info.legalIdentities.single()))
        Assert.assertTrue(chatMetaB.state.data.admin.equals(nodeA.info.legalIdentities.single()))

        // chatId/linearId must not exist
        // @todo: how to check? we're using UUID auto generated mechanism, so we're sure it'd be unique

        // chatId/linearId in two states must be same
        Assert.assertTrue(chatMessageA.state.data.chatId == chatMessageB.state.data.chatId)
        Assert.assertTrue(chatMetaA.state.data.linearId == chatMetaB.state.data.linearId)
        Assert.assertTrue(chatMessageA.state.data.chatId == chatMetaA.state.data.linearId)

        // sender must be the chat initiator
        Assert.assertTrue(chatMessageA.state.data.sender == nodeA.info.legalIdentities.single())
        Assert.assertTrue(chatMessageB.state.data.sender == nodeA.info.legalIdentities.single())

        // participants in ChatMessage only include the party receiving the message
        Assert.assertTrue(chatMessageA.state.data.participants.single() == nodeA.info.legalIdentities.single())
        Assert.assertTrue(chatMessageB.state.data.participants.single() == nodeB.info.legalIdentities.single())

        // participants in ChatMetaInfo participants include both admin and receivers
        val allParticipants = listOf(nodeA.info.legalIdentities.single(), nodeB.info.legalIdentities.single())
        val metaPartiesA = chatMetaA.state.data.participants
        val metaPartiesB = chatMetaB.state.data.participants
        Assert.assertTrue(metaPartiesA.subtract(allParticipants).isEmpty())
        Assert.assertTrue(metaPartiesB.subtract(allParticipants).isEmpty())


    }

}

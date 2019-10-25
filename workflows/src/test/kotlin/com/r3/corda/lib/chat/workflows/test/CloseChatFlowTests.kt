package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.CloseChatFlow
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.ReplyChatFlow
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

class CloseChatFlowTests {

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
                subject = "subject",
                content = "content",
                receivers = listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        newChatFlow.getOrThrow()

        val newChatInfoB = nodeB.services.vaultService.queryBy(ChatMessage::class.java).states.single().state.data

        // 2 reply the chat
        val replyFlow = nodeB.startFlow(
                ReplyChatFlow(
                        subject = "reply subject",
                        content = "reply content",
                        chatId = newChatInfoB.linearId
                )
        )

        network.runNetwork()
        replyFlow.getOrThrow()

        // 3. close chat
        val closeFlow = nodeA.startFlow(
                CloseChatFlow(
                        chatId = newChatInfoB.linearId
                )
        )
        network.runNetwork()
        closeFlow.getOrThrow()


        // there are 0 chat on ledge in each node
        val chatMetaA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        val chatMetaB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        val chatMetaC = nodeC.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        Assert.assertTrue(chatMetaA.isEmpty())
        Assert.assertTrue(chatMetaB.isEmpty())
        Assert.assertTrue(chatMetaC.isEmpty())

        val chatMessagesA = nodeA.services.vaultService.queryBy(ChatMessage::class.java).states
        val chatMessagesB = nodeB.services.vaultService.queryBy(ChatMessage::class.java).states
        val chatMessagesC = nodeC.services.vaultService.queryBy(ChatMessage::class.java).states
        Assert.assertTrue(chatMessagesA.isEmpty())
        Assert.assertTrue(chatMessagesB.isEmpty())
        Assert.assertTrue(chatMessagesC.isEmpty())

    }
}

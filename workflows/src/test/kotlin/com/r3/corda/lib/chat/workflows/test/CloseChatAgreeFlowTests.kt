package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.workflows.flows.CloseChatAgreeFlow
import com.r3.corda.lib.chat.workflows.flows.CloseChatProposeFlow
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

class CloseChatAgreeFlowTests {

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

        val newChatInfoB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single().state.data

        // 2 reply the chat
        val replyFlow = nodeB.startFlow(
                ReplyChatFlow(
                        content = "content",
                        attachment = null,
                        chatId = newChatInfoB.linearId
                )
        )

        network.runNetwork()
        replyFlow.getOrThrow()

        // 3. propose close chat
        val proposeClose = nodeA.startFlow(
                CloseChatProposeFlow(
                        newChatInfoB.linearId
                )
        )
        network.runNetwork()
        proposeClose.getOrThrow()

        // 4. agree close chat
        val agreeClose = nodeB.startFlow(
                CloseChatAgreeFlow(
                        newChatInfoB.linearId
                )
        )
        network.runNetwork()
        agreeClose.getOrThrow()

        // there are 0 chat on ledge in each node
        val closeChatsInVaultA = nodeA.services.vaultService.queryBy(CloseChatState::class.java).states
        val closeChatsInVaultB = nodeB.services.vaultService.queryBy(CloseChatState::class.java).states
        Assert.assertTrue(closeChatsInVaultA.size == 2)
        Assert.assertTrue(closeChatsInVaultB.size == 2)
    }
}

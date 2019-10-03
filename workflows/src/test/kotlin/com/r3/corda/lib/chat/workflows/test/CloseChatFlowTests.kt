package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.workflows.flows.*
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

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `should be possible to close a chat forcely`() {

        // 1 create one
        val newChatFlow = nodeA.startFlow(CreateChatFlow(
                "subject",
                "content",
                null,
                listOf(nodeB.info.legalIdentities.single(), nodeC.info.legalIdentities.single())
        ))
        network.runNetwork()
        newChatFlow.getOrThrow()

        val newChatInfoB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single().state.data

        // 2 reply the chat
        val replyFlow = nodeB.startFlow(
                ReplyChatFlow(
                        "content",
                        null,
                        newChatInfoB.linearId
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

        // 3. close chat
        val closeFlow = nodeA.startFlow(
                CloseChatFlow(
                        newChatInfoB.linearId,
                        true
                )
        )
        network.runNetwork()
        closeFlow.getOrThrow()

        // there are 0 chat on ledge in each node
        val closeChatsInVaultA = nodeA.services.vaultService.queryBy(CloseChatState::class.java).states
        val closeChatsInVaultB = nodeB.services.vaultService.queryBy(CloseChatState::class.java).states
        val closeChatsInVaultC = nodeC.services.vaultService.queryBy(CloseChatState::class.java).states
        Assert.assertTrue(closeChatsInVaultA.isEmpty())
        Assert.assertTrue(closeChatsInVaultB.isEmpty())
        Assert.assertTrue(closeChatsInVaultC.isEmpty())


        // there are 0 chat on ledge in each node
        val chatsInVaultA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states
        val chatsInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states
        val chatsInVaultC = nodeC.services.vaultService.queryBy(ChatInfo::class.java).states
        Assert.assertTrue(chatsInVaultA.isEmpty())
        Assert.assertTrue(chatsInVaultB.isEmpty())
        Assert.assertTrue(chatsInVaultC.isEmpty())

    }



    @Test
    fun `should be possible to close a chat when all agreed`() {

        // 1 create one
        val newChatFlow = nodeA.startFlow(CreateChatFlow(
                "subject",
                "content",
                null,
                listOf(nodeB.info.legalIdentities.single(), nodeC.info.legalIdentities.single())
        ))
        network.runNetwork()
        newChatFlow.getOrThrow()

        val newChatInfoB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single().state.data

        // 2 reply the chat
        val replyFlow = nodeB.startFlow(
                ReplyChatFlow(
                        "content",
                        null,
                        newChatInfoB.linearId
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

        val agreeB = nodeB.startFlow(
                CloseChatAgreeFlow(
                        newChatInfoB.linearId
                )
        )
        network.runNetwork()
        agreeB.getOrThrow()

        val agreeC = nodeC.startFlow(
                CloseChatAgreeFlow(
                        newChatInfoB.linearId
                )
        )
        network.runNetwork()
        agreeC.getOrThrow()

        // 3. close chat
        val closeFlow = nodeA.startFlow(
                CloseChatFlow(
                        newChatInfoB.linearId,
                        true
                )
        )
        network.runNetwork()
        closeFlow.getOrThrow()

        // there are 0 chat on ledge in each node
        val closeChatsInVaultA = nodeA.services.vaultService.queryBy(CloseChatState::class.java).states
        val closeChatsInVaultB = nodeB.services.vaultService.queryBy(CloseChatState::class.java).states
        val closeChatsInVaultC = nodeC.services.vaultService.queryBy(CloseChatState::class.java).states
        Assert.assertTrue(closeChatsInVaultA.isEmpty())
        Assert.assertTrue(closeChatsInVaultB.isEmpty())
        Assert.assertTrue(closeChatsInVaultC.isEmpty())


        // there are 0 chat on ledge in each node
        val chatsInVaultA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states
        val chatsInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states
        val chatsInVaultC = nodeC.services.vaultService.queryBy(ChatInfo::class.java).states
        Assert.assertTrue(chatsInVaultA.isEmpty())
        Assert.assertTrue(chatsInVaultB.isEmpty())
        Assert.assertTrue(chatsInVaultB.isEmpty())

    }

}

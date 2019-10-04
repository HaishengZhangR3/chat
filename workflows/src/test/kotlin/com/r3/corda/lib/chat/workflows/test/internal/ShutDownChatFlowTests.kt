package com.r3.corda.lib.chat.workflows.test.internal

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.ReplyChatFlow
import com.r3.corda.lib.chat.workflows.flows.internal.ShutDownChatFlow
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

class ShutDownChatFlowTests {

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

        // 2 reply the chat
        val replyFlow = nodeB.startFlow(
                ReplyChatFlow(
                        content ="content",
                        attachment = null,
                        chatId = chatInB.linearId
                )
        )

        network.runNetwork()
        replyFlow.getOrThrow()

        // 3. shut down B
        val shutdownA = nodeA.startFlow(
                ShutDownChatFlow(
                        chatId = chatInB.linearId,
                        toList = listOf(nodeB.info.legalIdentities.single())
                )
        )
        network.runNetwork()
        shutdownA.getOrThrow()

        // chatinfo should be consumed in B
        val chatInfosInVaultA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states
        val chatInfosInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states
        Assert.assertEquals(chatInfosInVaultA.size, 2)
        Assert.assertEquals(chatInfosInVaultB.size, 0)

    }
}

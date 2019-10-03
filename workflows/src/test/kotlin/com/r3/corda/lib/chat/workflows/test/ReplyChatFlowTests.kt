package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.ReplyChatFlow
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

class ReplyChatFlowTests {

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

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `should be possible to reply a chat`() {

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

        // 2 reply the chat
        val replyFlow = nodeB.startFlow(
                ReplyChatFlow(
                        "subject",
                        "content",
                        null,
                        nodeB.info.legalIdentities.single(),
                        listOf(nodeA.info.legalIdentities.single()),
                        newChatInfoInVaultB.state.data.linearId
                )
        )

        network.runNetwork()
        val replyChatInfo = replyFlow.getOrThrow()

        // the reply chat id === thread id
        Assert.assertTrue(replyChatInfo.state.data.linearId == newChatInfoInVaultB.state.data.linearId)

        // there are one chat on ledge in each node
        val replyChatsInVaultA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states
        val replyChatsInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states
        Assert.assertTrue(replyChatsInVaultA.size == 1)
        Assert.assertTrue(replyChatsInVaultB.size == 1)

        // replied chat should be newer than created chat
        val newChatDate = newChatInfo.state.data.created
        val replyChatDate = replyChatInfo.state.data.created
        Assert.assertTrue(newChatDate < replyChatDate)

        // all of them have same id
        Assert.assertTrue((replyChatsInVaultA + replyChatsInVaultB).map { it.state.data.linearId }.toSet().size == 1)

    }
}

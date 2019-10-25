package com.r3.corda.lib.chat.workflows.test.internal

import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.internal.CreateMessageFlow
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

// @todo: all tests to add details
class CreateMessageFlowTests {

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
    fun `should be possible to reply a chat`() {

        // 1 create one
        val newChatFlow = nodeA.startFlow(CreateChatFlow(
                subject = "subject",
                content = "content",
                receivers = listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        val txnNew = newChatFlow.getOrThrow()
        val newChatInfo = txnNew.state.data

        val newChatsInVaultA = nodeA.services.vaultService.queryBy(ChatMessage::class.java).states.single().state.data
        val newChatsInVaultB = nodeB.services.vaultService.queryBy(ChatMessage::class.java).states.single().state.data

        // 2 send a message to the chat
        val sendFlow = nodeB.startFlow(
                CreateMessageFlow(
                        subject = "subjectxxx",
                        content = "contentxxx",
                        chatId = newChatInfo.linearId
                )
        )

        network.runNetwork()
        sendFlow.getOrThrow()

        // there are two chat on ledge in each node
        val allChatsInVaultA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        val allChatsInVaultB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        Assert.assertTrue(allChatsInVaultA.size == 2)
        Assert.assertTrue(allChatsInVaultB.size == 2)

        val replyChatInfoA = allChatsInVaultA.sortedByDescending { it.state.data.created }.first().state.data
        val replyChatInfoB = allChatsInVaultB.sortedByDescending { it.state.data.created }.first().state.data

        // replied chat should be newer than created chat
        val newChatDate = newChatsInVaultA.created
        val replyChatDate = replyChatInfoA.created
        Assert.assertTrue(newChatDate < replyChatDate)

        // all of them have same id
        Assert.assertEquals(listOf(newChatInfo.linearId,
                newChatsInVaultA.linearId,
                newChatsInVaultB.linearId,
                newChatsInVaultA.linearId,
                replyChatInfoA.linearId,
                replyChatInfoB.linearId
                ).toSet().size,
                1)
    }
}

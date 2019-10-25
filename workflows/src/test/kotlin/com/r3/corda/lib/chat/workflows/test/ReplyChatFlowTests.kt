package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
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

        val newChatInfoA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single().state.data
        val newChatInfoB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single().state.data

        // 2 reply the chat
        val replyFlow = nodeB.startFlow(
                ReplyChatFlow(
                        subject = "reply subject",
                        content = "reply content",
                        chatId = newChatInfoB.linearId
                )
        )

        network.runNetwork()
        val chatMessageStateRef = replyFlow.getOrThrow()
        val replyChatMessage = chatMessageStateRef.state.data

        // the reply chat id === thread id
        Assert.assertTrue(replyChatMessage.linearId == newChatInfoB.linearId)

        // there are one chat on ledge in each node
        val allChatsInVaultA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        val allChatsInVaultB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        Assert.assertTrue(allChatsInVaultA.size == 2)
        Assert.assertTrue(allChatsInVaultB.size == 2)

        val replyChatInfoA = allChatsInVaultA.sortedByDescending { it.state.data.created }.first().state.data
        val replyChatInfoB = allChatsInVaultB.sortedByDescending { it.state.data.created }.first().state.data

        // replied chat should be newer than created chat
        val newChatDate = newChatInfo.created
        val replyChatDate = replyChatMessage.created
        Assert.assertTrue(newChatDate < replyChatDate)

        // all of them have same id
        Assert.assertEquals(listOf(newChatInfo.linearId,
                newChatInfoA.linearId,
                newChatInfoB.linearId,
                replyChatMessage.linearId,
                replyChatInfoA.linearId,
                replyChatInfoB.linearId
                ).toSet().size,
                1)


        // same chat in two nodes should have diff participants
        val participantsA = replyChatInfoA.participants
        val participantsB = replyChatInfoB.participants
        Assert.assertEquals(participantsA.size,1)
        Assert.assertEquals(participantsB.size,1)

        val participantA = participantsA.single()
        val participantB = participantsB.single()
        Assert.assertFalse(participantA.nameOrNull().toString().equals(participantB.nameOrNull().toString()))    }
}

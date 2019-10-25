package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatMessage
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

        val newChatMetaInfoA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single().state.data
        val newChatMetaInfoB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single().state.data

        // 2 reply the chat
        val replyFlow = nodeB.startFlow(
                ReplyChatFlow(
                        subject = "reply subject",
                        content = "reply content",
                        chatId = newChatMetaInfoA.linearId
                )
        )

        network.runNetwork()
        val chatMessageStateRef = replyFlow.getOrThrow()
        val replyChatMessage = chatMessageStateRef.state.data

        // the reply chat id === thread id
        Assert.assertTrue(replyChatMessage.linearId == newChatMetaInfoB.linearId)

        // there are one chat meta on ledge in each node
        val replyChatMetaStateRefA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        val replyChatMetaStateRefB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        Assert.assertTrue(replyChatMetaStateRefA.size == 1)
        Assert.assertTrue(replyChatMetaStateRefB.size == 1)

        val replyChatMetaA = replyChatMetaStateRefA.single().state.data
        val replyChatMetaB = replyChatMetaStateRefB.single().state.data

        // replied chat should be newer than created chat
        val newChatDate = newChatInfo.created
        val replyChatMetaDate = replyChatMessage.created
        Assert.assertTrue(newChatDate < replyChatMetaDate)

        // there are two chat messages on ledge in each node
        val chatMessageStateRefA = nodeA.services.vaultService.queryBy(ChatMessage::class.java).states
        val chatMessageStateRefB = nodeB.services.vaultService.queryBy(ChatMessage::class.java).states
        Assert.assertTrue(chatMessageStateRefA.size == 2)
        Assert.assertTrue(chatMessageStateRefB.size == 2)

        val chatMessageA = chatMessageStateRefA.sortedByDescending { it.state.data.created }.first().state.data
        val chatMessageB = chatMessageStateRefB.sortedByDescending { it.state.data.created }.first().state.data

        // replied chat should be newer than created chat
        val newChatMsgDate = newChatInfo.created
        val replyChatMsgDate = chatMessageA.created
        Assert.assertTrue(newChatMsgDate < replyChatMsgDate)

        // all of them have same id
        Assert.assertEquals(listOf(newChatInfo.linearId,
                newChatMetaInfoA.linearId,
                newChatMetaInfoB.linearId,
                replyChatMessage.linearId,
                replyChatMetaA.linearId,
                replyChatMetaB.linearId,
                chatMessageA.linearId,
                chatMessageB.linearId
                ).toSet().size,
                1)


        // same chat in two nodes should have diff participants
        val participantsA = chatMessageA.participants
        val participantsB = chatMessageB.participants
        Assert.assertEquals(participantsA.size,1)
        Assert.assertEquals(participantsB.size,1)

        val participantA = participantsA.single()
        val participantB = participantsB.single()
        Assert.assertFalse(participantA.nameOrNull().toString().equals(participantB.nameOrNull().toString()))    }
}

package com.r3.corda.lib.chat.workflows.test.internal

import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.AddParticipantsFlow
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

class AddParticipantsFlowTests {

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
    fun `should be possible to propose to add participants to a chat`() {

        // 1 create one
        val newChatFlow = nodeA.startFlow(CreateChatFlow(
                subject = "subject",
                content = "content",
                receivers = listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        newChatFlow.getOrThrow()

        val chatInB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states.single().state.data

        // 2. add new participants
        val addParticipantsFlow = nodeB.startFlow(
                AddParticipantsFlow(
                        toAdd = listOf(nodeC.info.legalIdentities.single()),
                        chatId = chatInB.linearId
                )
        )

        network.runNetwork()
        addParticipantsFlow.getOrThrow()



        // chatinfo should not be consumed in A,B, and new one should be in C
        val chatInfosInVaultA = nodeA.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        val chatInfosInVaultB = nodeB.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        val chatInfosInVaultC = nodeC.services.vaultService.queryBy(ChatMetaInfo::class.java).states
        Assert.assertEquals(chatInfosInVaultA.size, 1)
        Assert.assertEquals(chatInfosInVaultB.size, 1)
        Assert.assertEquals(chatInfosInVaultC.size, 1)

        Assert.assertEquals(
                (chatInfosInVaultA.map { it.state.data.linearId }
                        + chatInfosInVaultB.map { it.state.data.linearId }
                        + chatInfosInVaultB.map { it.state.data.linearId })
                        .distinct().size,
                1
        )

    }
}

package com.r3.corda.lib.chat.workflows.test

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
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

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    //host node will create an account
    @Test
    fun `should be possible to create a chat`() {

        //host node A will create an account
        val chatFlow = nodeA.startFlow(CreateChatFlow(
                "subject",
                "content",
                null,
                nodeA.info.legalIdentities.single(),
                listOf(nodeB.info.legalIdentities.single())
        ))
        network.runNetwork()
        val chatInfo = chatFlow.getOrThrow()

        val chatInfoInVaultA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states.single()

        //check whether the created one in node A is same as that in the DB of host node A
        Assert.assertTrue(chatInfo == chatInfoInVaultA)

        //check whether the created one in node B is same as that in the DB of host node A
        val chatInfoInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single()

        Assert.assertTrue(chatInfoInVaultA.state == chatInfoInVaultB.state)

    }
}

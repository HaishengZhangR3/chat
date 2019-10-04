package com.r3.corda.lib.chat.workflows.test.internal

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.UpdateParticipantsState
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsAgreeFlow
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateParticipantsProposeFlow
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

class UpdateParticipantsAgreeFlowTests {

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


    //private fun tt(){
    // keep here for reference
    // val txnToAgree = serviceHub.validatedTransactions.getTransaction(txnId) as SignedTransaction
    // val stateAndRefToAgree = txnToAgree.toLedgerTransaction(serviceHub).outRefsOfType<UpdateParticipantsState>().single()

    // val txns = nodeC.services.validatedTransactions.track().snapshot.filter {
    //     it.coreTransaction.outputsOfType<ParticipantsUpdateState>().isNotEmpty()
    // }
    //
    // Assert.assertTrue(txns.isNotEmpty())
    // val txnId = txns.single().id

    // val signedTxnId = signedTxn.coreTransaction.id
    //}

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

        // 2. add new participants
        val addParticipantsFlow = nodeB.startFlow(
                UpdateParticipantsProposeFlow(
                        toAdd = listOf(nodeC.info.legalIdentities.single()),
                        includingHistoryChat = true,
                        chatId = chatInB.linearId
                )
        )

        network.runNetwork()
        addParticipantsFlow.getOrThrow()

        // 3. agree
        val agreeParticipantsFlowA = nodeA.startFlow(
                UpdateParticipantsAgreeFlow(
                        chatInB.linearId
                )
        )
        network.runNetwork()
        agreeParticipantsFlowA.getOrThrow()

        // chatinfo should not be consumed in B
        val chatInfoInVaultA = nodeA.services.vaultService.queryBy(ChatInfo::class.java).states.single()
        val chatInfoInVaultB = nodeB.services.vaultService.queryBy(ChatInfo::class.java).states.single()
        Assert.assertTrue(chatInfoInVaultA.state.data.linearId == chatInfoInVaultB.state.data.linearId)

        // the agree result
        val updateParticipantsA = nodeA.services.vaultService.queryBy(UpdateParticipantsState::class.java).states
        val updateParticipantsB = nodeB.services.vaultService.queryBy(UpdateParticipantsState::class.java).states
        Assert.assertEquals(updateParticipantsA.size, 2)
        Assert.assertEquals(updateParticipantsB.size, 2)

    }

}

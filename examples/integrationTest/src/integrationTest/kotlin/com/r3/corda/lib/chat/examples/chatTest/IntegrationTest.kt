package com.r3.corda.lib.accounts.examples.tokensTest

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.workflows.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.concurrent.transpose
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.NodeParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import org.junit.Assert
import org.junit.Test

class IntegrationTest {

    companion object {
        private val log = contextLogger()
    }

    private val partyA = NodeParameters(
            providedName = CordaX500Name("PartyA", "Singapore", "SG"),
            additionalCordapps = listOf()
    )

    private val partyB = NodeParameters(
            providedName = CordaX500Name("PartyB", "NewYork", "US"),
            additionalCordapps = listOf()
    )

    private val partyC = NodeParameters(
            providedName = CordaX500Name("PartyC", "London", "GB"),
            additionalCordapps = listOf()
    )

    private val nodeParams = listOf(partyA, partyB, partyC)

    private val defaultCorDapps = listOf(
            TestCordapp.findCordapp("com.r3.corda.lib.chat.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.chat.contracts")
    )

    private val driverParameters = DriverParameters(
            startNodesInProcess = true,
            cordappsForAllNodes = defaultCorDapps,
            networkParameters = testNetworkParameters(notaries = emptyList(), minimumPlatformVersion = 4)
    )

    fun NodeHandle.legalIdentity() = nodeInfo.legalIdentities.single()

    @Test
    fun `basic chat flow test`() {
        driver(driverParameters) {
            log.warn("***** Chat integration test starting ....... *****")
            val (A, B, I) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
            log.warn("***** All nodes started up *****")

            log.warn("***** Creating chat on node A *****")
            val createChatOnA = listOf(
                    A.rpc.startFlow(
                            ::CreateChatFlow,
                            "Sample Topic",
                            "Some sample content created from A",
                            null,
                            listOf(B.legalIdentity())
                    ).returnValue
            ).transpose().getOrThrow()

            val chatOnA = createChatOnA.single().state.data
            log.warn("***** The chat created on A is: ${chatOnA.linearId} *****")

            log.warn("***** Reply chat from B *****")
            val replyChatFromB = listOf(
                    B.rpc.startFlow(
                            ::ReplyChatFlow,
                            chatOnA.linearId,
                            "Some sample content replied from B",
                            null
                    ).returnValue
            ).transpose().getOrThrow()

            val chatOnB = replyChatFromB.single().coreTransaction.outputStates.single() as ChatInfo
            log.warn("***** The chat replied from B: ${chatOnB.linearId} *****")
            log.warn("***** Reply chat from B *****")

            log.warn("***** Propose close from B *****")
            val proposeFromB = listOf(
                    B.rpc.startFlow(
                            ::CloseChatProposeFlow,
                            chatOnA.linearId
                    ).returnValue
            ).transpose().getOrThrow()

            log.warn("***** Close proposal agreed from A *****")
            val agreeFromA = listOf(
                    A.rpc.startFlow(
                            ::CloseChatAgreeFlow,
                            chatOnA.linearId
                    ).returnValue
            ).transpose().getOrThrow()

            log.warn("***** Do final close from B *****")
            val doCloseFromA = listOf(
                    B.rpc.startFlow(
                            ::CloseChatFlow,
                            chatOnA.linearId,
                            false
                    ).returnValue
            ).transpose().getOrThrow()

            log.warn("***** Chat ${chatOnA.linearId} closed *****")

            log.warn("**** Now let's check the closed chat *****")
            val chatsInA = A.rpc.vaultQuery(ChatInfo::class.java).states
            val chatsInB = B.rpc.vaultQuery(ChatInfo::class.java).states
            Assert.assertTrue("Should not be any chat in A", chatsInA.isEmpty())
            Assert.assertTrue("Should not be any chat in B", chatsInB.isEmpty())

            val closeStateA = A.rpc.vaultQuery(CloseChatState::class.java).states
            val closeStateB = B.rpc.vaultQuery(CloseChatState::class.java).states
            Assert.assertTrue("Should not be close states in A", closeStateA.isEmpty())
            Assert.assertTrue("Should not be close states in B", closeStateB.isEmpty())

            log.warn("**** All passed, happy *****")
        }
    }


//    @Test
//    fun `chat with participants updation test`() {
//        driver(driverParameters) {
//            val (A, B, I) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
//            log.info("All nodes started up.")
//
//            log.info("Creating two accounts on node A.")
//
//        }
//    }
}

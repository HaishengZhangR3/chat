package com.r3.corda.lib.accounts.examples.tokensTest

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.ReplyChatFlow
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
            val (A, B, I) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
            log.info("All nodes started up.")

            log.info("Creating chat on node A.")

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
            log.info("The chat ID created on A is: ${chatOnA.linearId}")


            val replyChatFromB = listOf(
                    B.rpc.startFlow(
                            ::ReplyChatFlow,
                            chatOnA.linearId,
                            "Some sample content replied from B",
                            null
                    ).returnValue
            ).transpose().getOrThrow()

            val chatOnB = replyChatFromB.single().coreTransaction.outputStates.single() as ChatInfo
            log.info("The chat ID replied from B is: ${chatOnB.linearId}")

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

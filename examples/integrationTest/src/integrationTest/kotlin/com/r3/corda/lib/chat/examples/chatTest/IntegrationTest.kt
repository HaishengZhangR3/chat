package com.r3.corda.lib.chat.examples.chatTest

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.contracts.states.CloseChatState
import com.r3.corda.lib.chat.workflows.flows.*
import com.r3.corda.lib.chat.workflows.flows.service.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
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

    private fun createChat(who: NodeHandle, toList: List<Party>, any: String): ChatInfo {
        val createChat = who.rpc.startFlow(
                ::CreateChatFlow,
                "Sample Topic $any",
                "Some sample content created $any",
                null,
                toList
        ).returnValue.getOrThrow()
        return createChat.state.data
    }

    private fun replyChat(who: NodeHandle, chatId: UniqueIdentifier, any: String): ChatInfo {
        val replyChat = who.rpc.startFlow(
                ::ReplyChatFlow,
                chatId,
                "Some sample content replied $any",
                null
        ).returnValue.getOrThrow()

        return replyChat.coreTransaction.outputStates.single() as ChatInfo
    }

    private fun closeChat(proposer: NodeHandle, agreeer: List<NodeHandle>, chatId: UniqueIdentifier): Any {
        log.warn("***** Propose close *****")
        val propose = proposer.rpc.startFlow(
                ::CloseChatProposeFlow,
                chatId
        ).returnValue.getOrThrow()

        log.warn("***** Close proposal agreed *****")
        val agree = agreeer.map {
            it.rpc.startFlow(
                    ::CloseChatAgreeFlow,
                    chatId
            ).returnValue.getOrThrow()
        }

        log.warn("***** Do final close *****")
        val doIt = proposer.rpc.startFlow(
                ::CloseChatFlow,
                chatId,
                false
        ).returnValue.getOrThrow()

        return doIt
    }

    private fun addParticipantsToChat(proposer: NodeHandle, agreeer: List<NodeHandle>, toAdd: List<Party>, chatId: UniqueIdentifier): Any {
        log.warn("***** Propose add *****")
        val propose = proposer.rpc.startFlow(
                ::AddParticipantsProposeFlow,
                chatId,
                toAdd,
                true
        ).returnValue.getOrThrow()

        log.warn("***** Add proposal agreed *****")
        val agree = agreeer.map {
            it.rpc.startFlow(
                    ::AddParticipantsAgreeFlow,
                    chatId
            ).returnValue.getOrThrow()
        }

        log.warn("***** Do final add *****")
        val doIt = proposer.rpc.startFlow(
                ::AddParticipantsFlow,
                chatId
        ).returnValue.getOrThrow()

        return doIt
    }

    private fun getAllChatIDs(node: NodeHandle): List<UniqueIdentifier> {
        log.warn("***** All chatIDs *****")
        val allChatIDsFromVault = node.rpc.startFlow(
                ::AllChatIDs
        ).returnValue.getOrThrow()
        return allChatIDsFromVault
    }
    private fun getAllChats(node: NodeHandle): List<StateAndRef<ChatInfo>> {
        log.warn("***** All chats and messages *****")
        val allChatsFromVault = node.rpc.startFlow(
                ::AllChats
        ).returnValue.getOrThrow()
        return allChatsFromVault

    }
    private fun getAllChatsBy(){
        // @todo: AllChatsBy filter
        log.warn("***** All chats and messages with filter applied *****")

    }

    private fun getChatAllMessages(node: NodeHandle, chatId: UniqueIdentifier): List<StateAndRef<ChatInfo>> {
        val chatAllMessagesFromVault = node.rpc.startFlow(
                ::ChatAllMessages,
                chatId
        ).returnValue.getOrThrow()
        return chatAllMessagesFromVault
    }
    private fun getChatMessagesBy(node: NodeHandle, chatId: UniqueIdentifier) {
        // @todo: ChatAllMessagesBy filter
        log.warn("***** All messages for one single chat with filter applied *****")

    }
    private fun getChatCurrentStatus(node: NodeHandle, chatId: UniqueIdentifier): ChatStatus {
        log.warn("***** chat status: active, close proposed, closed for one chat by ID *****")
        val chatStatusFromVault = node.rpc.startFlow(
                ::ChatCurrentStatus,
                chatId
        ).returnValue.getOrThrow()
        return chatStatusFromVault
    }

    private fun getChatParticipants(node: NodeHandle, chatId: UniqueIdentifier): List<Party> {
        log.warn("***** All participants for one chat by ID *****")
        val chatParticipantsFromVault = node.rpc.startFlow(
                ::ChatParticipants,
                chatId
        ).returnValue.getOrThrow()
        return chatParticipantsFromVault
    }

    @Test
    fun `Corda Chat supports create, reply, close proposal,agree,reject and finilise`() {
        driver(driverParameters) {
            log.warn("***** Chat integration test starting ....... *****")
            val (A, B, C) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
            log.warn("***** All nodes started up *****")

            log.warn("***** Creating chat on node A *****")
            val chatOnA = createChat(who = A, toList = listOf(B.legalIdentity()), any = "from A")
            log.warn("***** The chat created on A is: ${chatOnA.linearId} *****")

            log.warn("***** Reply chat from B *****")
            val chatOnB = replyChat(who = B, chatId = chatOnA.linearId, any = "from B")
            log.warn("***** The chat replied from B: ${chatOnB.linearId} *****")

            log.warn("***** Propose/agree/do close from B *****")
            closeChat(proposer = B, agreeer = listOf(A), chatId = chatOnA.linearId)

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


    @Test
    fun `Corda Chat supports participants updating proposal, agree, reject`() {

        driver(driverParameters) {
            log.warn("***** Chat integration test starting ....... *****")
            val (A, B, C) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
            log.warn("***** All nodes started up *****")

            log.warn("***** Creating chat on node A *****")
            val chatOnA = createChat(who = A, toList = listOf(B.legalIdentity()), any = "from A")
            log.warn("***** The chat created on A is: ${chatOnA.linearId} *****")

            log.warn("***** Reply chat from B *****")
            val chatOnB = replyChat(who = B, chatId = chatOnA.linearId, any = "from B")
            log.warn("***** The chat replied from B: ${chatOnB.linearId} *****")

            log.warn("***** Propose/agree/do add participants from B *****")
            addParticipantsToChat(proposer = B, agreeer = listOf(A), toAdd = listOf(C.legalIdentity()), chatId = chatOnA.linearId)

            log.warn("**** Now let's check the chat *****")
            val chatsInA = A.rpc.vaultQuery(ChatInfo::class.java).states
            val chatsInB = B.rpc.vaultQuery(ChatInfo::class.java).states
            val chatsInC = B.rpc.vaultQuery(ChatInfo::class.java).states
            Assert.assertTrue(chatsInA.isNotEmpty())
            Assert.assertTrue(chatsInB.isNotEmpty())
            Assert.assertTrue(chatsInC.isNotEmpty())

            // @todo: why 3? 1 new, 1 reply, 1 add participant. maybe need re-design
            Assert.assertTrue(chatsInA.size == 3)
            Assert.assertTrue(chatsInB.size == 3)
            Assert.assertTrue(chatsInC.size == 3)

            val allChatIds = (chatsInA + chatsInB + chatsInC).map { it.state.data.linearId }.distinct()
            Assert.assertTrue(allChatIds.size == 1)
            Assert.assertTrue(allChatIds.single() == chatOnA.linearId)

            log.warn("**** All passed, happy *****")
        }
    }

    @Test
    fun `Corda Chat supports admin utilities to list chats, chat messages and more filtering rules`() {

        driver(driverParameters) {

            log.warn("***** Chat integration test starting ....... *****")
            val (A, B, C) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
            log.warn("***** All nodes started up *****")

            // 5X3: A -> (B, C); 5X3: B -> (B, C); so chats amount: A: 5X3, B: 5X3X2, C: 5X3X2
            log.warn("***** Let's chat and reply for a while.... *****")

            val howManyChats = 5
            val chatIDs: MutableList<UniqueIdentifier> = mutableListOf()
            val fromNodes = listOf(A, B)
            val toNodes = listOf(B, C)
            val toList = listOf(B.legalIdentity(), C.legalIdentity())

            for (i in 0 until howManyChats) {
                fromNodes.map {
                    val fromNodeName = it.nodeInfo.legalIdentities.first().name
                    log.warn("***** Creating chat on node ${it.nodeInfo.legalIdentities.first().name} *****")
                    val chatOnA = createChat(who = it, toList = toList, any = "from $fromNodeName")
                    chatIDs.add(chatOnA.linearId)
                    log.warn("***** The chat created is: ${chatOnA.linearId} *****")

                    toNodes.map {
                        val toNodeName = it.nodeInfo.legalIdentities.first().name
                        log.warn("***** Reply chat from ${toNodeName} *****")
                        val chatOnB = replyChat(who = it, chatId = chatOnA.linearId, any = "from ${toNodeName}")
                        log.warn("***** The chat replied: ${chatOnB.linearId} *****")
                    }
                }
            }
            log.warn("***** No more chat, OK.... *****")

            log.warn("***** Let's check the result using admin utilities *****")

            log.warn("***** All chatIDs *****")
            val allChatIDsFromVault = getAllChatIDs(A)
            Assert.assertEquals(allChatIDsFromVault.size, chatIDs.size)
            Assert.assertTrue((allChatIDsFromVault - chatIDs).isEmpty())
            Assert.assertTrue((chatIDs - allChatIDsFromVault).isEmpty())

            log.warn("***** All chats and messages *****")
            val allChatsFromVault = getAllChats(A)
            val idsAllChatsFromVault = allChatsFromVault.map {
                it.state.data.linearId
            }.toList().distinct()

            Assert.assertEquals(idsAllChatsFromVault.size, chatIDs.size)
            Assert.assertTrue((idsAllChatsFromVault - chatIDs).isEmpty())
            Assert.assertTrue((chatIDs - idsAllChatsFromVault).isEmpty())

            // @todo: AllChatsBy filter
            log.warn("***** All chats and messages with filter applied *****")
            val idToChatsAmount = mapOf(
                    A to howManyChats * 3,
                    B to howManyChats * 3 * 2,
                    C to howManyChats * 3 * 2
            )

            val idToChatsParticipants = mapOf(
                    A to listOf(B.legalIdentity(), C.legalIdentity(), A.legalIdentity()),
                    B to listOf(B.legalIdentity(), C.legalIdentity()),
                    C to listOf(B.legalIdentity(), C.legalIdentity())
            )

            for (id in chatIDs) {

                log.warn("***** All messages for one single chat by ID: $id *****")
                for (node in listOf(A, B, C)) {
                    val chatAllMessagesFromVault = getChatAllMessages(node, id)
                    log.warn("***** Messages for ID: $id *****")
                    Assert.assertEquals(chatAllMessagesFromVault.size, idToChatsAmount[node])
                    chatAllMessagesFromVault.map {
                        log.warn("${it.state.data}")
                    }

                    // @todo: ChatAllMessagesBy filter
                    log.warn("***** All messages for one single chat with filter applied *****")

                    log.warn("***** chat status: active, close proposed, closed for one chat by ID *****")
                    val chatStatusFromVault = getChatCurrentStatus(node, id)
                    Assert.assertEquals(chatStatusFromVault, ChatStatus.ACTIVE)


                    log.warn("***** All participants for one chat by ID *****")
                    val chatParticipantsFromVault = getChatParticipants(node, id)
                    Assert.assertEquals(chatParticipantsFromVault.size, idToChatsParticipants.values.size)
                    Assert.assertEquals((chatParticipantsFromVault - idToChatsParticipants.values).size, 0)
                    Assert.assertEquals((idToChatsParticipants.values - chatParticipantsFromVault).size, 0)

                }
            }
            log.warn("**** All passed, happy *****")
        }
    }

    @Test
    fun `Corda Chat everything integration test`() {

        driver(driverParameters) {
            log.warn("***** Chat integration test starting ....... *****")
            val (A, B, C) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
            log.warn("***** All nodes started up *****")

            log.warn("***** Creating chat on node A *****")
            val chatOnA = createChat(who = A, toList = listOf(B.legalIdentity()), any = "from A")
            log.warn("***** The chat created on A is: ${chatOnA.linearId} *****")

            log.warn("***** Reply chat from B *****")
            val chatOnB = replyChat(who = B, chatId = chatOnA.linearId, any = "from B")
            log.warn("***** The chat replied from B: ${chatOnB.linearId} *****")

            log.warn("***** Propose/agree/do add participants from B *****")
            addParticipantsToChat(proposer = B, agreeer = listOf(A), toAdd = listOf(C.legalIdentity()), chatId = chatOnA.linearId)

            log.warn("***** All chatIDs *****")
            val allChatIDsFromVault = getAllChatIDs(A)

            log.warn("***** All chats and messages *****")
            val allChatsFromVault = getAllChats(A)

            log.warn("***** All messages for one single chat by ID: ${chatOnA.linearId} *****")
            val chatAllMessagesFromVault = getChatAllMessages(A, chatOnA.linearId)

            log.warn("***** chat status: active, close proposed, closed for one chat by ID *****")
            val chatStatusFromVault = getChatCurrentStatus(B, chatOnA.linearId)

            log.warn("***** All participants for one chat by ID *****")
            val chatParticipantsFromVault = getChatParticipants(C, chatOnA.linearId)

            log.warn("***** Propose/agree/do close from B *****")
            closeChat(proposer = B, agreeer = listOf(A, C), chatId = chatOnA.linearId)

            log.warn("***** Chat ${chatOnA.linearId} closed *****")

            log.warn("**** All passed, happy *****")
        }
    }


}

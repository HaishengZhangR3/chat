package com.r3.corda.lib.chat.examples.chatTest

import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.AddParticipantsFlow
import com.r3.corda.lib.chat.workflows.flows.CloseChatFlow
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import com.r3.corda.lib.chat.workflows.flows.ReplyChatFlow
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


// @todo: add remove participants cases
// @todo: add attachment test cases
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

    private fun createChat(who: NodeHandle, toList: List<Party>, any: String): ChatMessage {
        val createChat = who.rpc.startFlow(
                ::CreateChatFlow,
                "Sample Topic $any",
                "Some sample content created $any",
                toList
        ).returnValue.getOrThrow()
        return createChat.state.data
    }

    private fun replyChat(who: NodeHandle, chatId: UniqueIdentifier, any: String): ChatMessage {
        val replyChat = who.rpc.startFlow(
                ::ReplyChatFlow,
                chatId,
                "Some sample content replied $any"
        ).returnValue.getOrThrow()

        return replyChat.state.data
    }

    private fun closeChat(proposer: NodeHandle, chatId: UniqueIdentifier): Any {
        log.warn("***** Do final close *****")
        val doIt = proposer.rpc.startFlow(
                ::CloseChatFlow,
                chatId
        ).returnValue.getOrThrow()

        return doIt
    }

    private fun addParticipantsToChat(proposer: NodeHandle, toAdd: List<Party>, chatId: UniqueIdentifier): Any {
        log.warn("***** Do final add *****")
        val doIt = proposer.rpc.startFlow(
                ::AddParticipantsFlow,
                chatId,
                toAdd
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

    private fun getAllChats(node: NodeHandle): List<StateAndRef<ChatMessage>> {
        log.warn("***** All chats and messages *****")
        val allChatsFromVault = node.rpc.startFlow(
                ::AllChats
        ).returnValue.getOrThrow()
        return allChatsFromVault

    }

    private fun getChatAllMessages(node: NodeHandle, chatId: UniqueIdentifier): List<StateAndRef<ChatMessage>> {
        log.warn("***** All messages for one single chat by ID: $chatId *****")
        val chatAllMessagesFromVault = node.rpc.startFlow(
                ::ChatAllMessages,
                chatId
        ).returnValue.getOrThrow()
        return chatAllMessagesFromVault
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
    fun `Corda Chat supports create, reply, close`() {
        driver(driverParameters) {
            log.warn("***** Chat integration test starting ....... *****")
            val (A, B, _) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
            log.warn("***** All nodes started up *****")

            log.warn("***** Creating chat on node A *****")
            val chatOnA = createChat(who = A, toList = listOf(B.legalIdentity()), any = "from A")
            log.warn("***** The chat created on A is: ${chatOnA.linearId} *****")

            log.warn("***** Reply chat from B *****")
            val chatOnB = replyChat(who = B, chatId = chatOnA.linearId, any = "from B")
            log.warn("***** The chat replied from B: ${chatOnB.linearId} *****")

            log.warn("*****  close from B *****")
            closeChat(proposer = B, chatId = chatOnA.linearId)

            log.warn("***** Chat ${chatOnA.linearId} closed *****")

            log.warn("**** Now let's check the closed chat *****")
            val chatsInA = A.rpc.vaultQuery(ChatMetaInfo::class.java).states
            val chatsInB = B.rpc.vaultQuery(ChatMetaInfo::class.java).states
            Assert.assertTrue("Should not be any chat in A", chatsInA.isEmpty())
            Assert.assertTrue("Should not be any chat in B", chatsInB.isEmpty())

            log.warn("**** All passed, happy *****")
        }
    }


    @Test
    fun `Corda Chat supports participants updating`() {

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

            log.warn("***** add participants from B *****")
            addParticipantsToChat(proposer = B, toAdd = listOf(C.legalIdentity()), chatId = chatOnA.linearId)

            log.warn("**** Now let's check the chat *****")
            val chatsInA = A.rpc.vaultQuery(ChatMetaInfo::class.java).states
            val chatsInB = B.rpc.vaultQuery(ChatMetaInfo::class.java).states
            val chatsInC = B.rpc.vaultQuery(ChatMetaInfo::class.java).states
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

            val fromNodes = listOf(A, B)
            val toNodes = listOf(B, C)
            val toList = listOf(B.legalIdentity(), C.legalIdentity())

            val howManyChats = 5

            val howManyUniqueIdentifier: MutableMap<Party, MutableSet<UniqueIdentifier>> =
                    mutableMapOf(
                            A.nodeInfo.legalIdentities.first() to mutableSetOf(),
                            B.nodeInfo.legalIdentities.first() to mutableSetOf(),
                            C.nodeInfo.legalIdentities.first() to mutableSetOf()
                    )

            val howManyMessages = mapOf(
                    A to howManyChats * 3,
                    B to howManyChats * 3 * 2,
                    C to howManyChats * 3 * 2
            )

            val howManyChatsParticipants = mapOf(
                    A to listOf(B.legalIdentity(), C.legalIdentity(), A.legalIdentity()),
                    B to listOf(B.legalIdentity(), C.legalIdentity()),
                    C to listOf(B.legalIdentity(), C.legalIdentity())
            )

            for (i in 0 until howManyChats) {
                fromNodes.map { node ->
                    val fromNodeName = node.nodeInfo.legalIdentities.first().name
                    log.warn("***** Creating chat on node ${node.nodeInfo.legalIdentities.first().name} *****")
                    val chatOnNode = createChat(who = node, toList = toList, any = "from $fromNodeName")
                    (toList + node.nodeInfo.legalIdentities).distinct().map { party ->
                        howManyUniqueIdentifier[party]?.add(chatOnNode.linearId)
                    }

                    log.warn("***** The chat created is: ${chatOnNode.linearId} *****")

                    toNodes.map {
                        val toNodeName = it.nodeInfo.legalIdentities.first().name
                        log.warn("***** Reply chat from ${toNodeName} *****")
                        val chatOnReply = replyChat(who = it, chatId = chatOnNode.linearId, any = "from ${toNodeName}")

                        (toList + node.nodeInfo.legalIdentities).distinct().map { party ->
                            howManyUniqueIdentifier[party]?.add(chatOnReply.linearId)
                        }

                        log.warn("***** The chat replied: ${chatOnReply.linearId} *****")
                    }
                }
            }
            log.warn("***** No more chat, OK.... *****")

            log.warn("***** Let's check the result using admin utilities *****")

            log.warn("***** All chatIDs *****")
            val allChatIDsFromVault = getAllChatIDs(A)

            val howManyUniqueIdentifierA = howManyUniqueIdentifier.getOrDefault(A.nodeInfo.legalIdentities.first(), mutableSetOf())
            val howManyChatsMessagesA = howManyMessages.getOrDefault(A, 0)

            Assert.assertEquals(allChatIDsFromVault.size, howManyUniqueIdentifierA.size)
            Assert.assertTrue((allChatIDsFromVault - howManyUniqueIdentifierA).isEmpty())
            Assert.assertTrue((howManyUniqueIdentifierA - allChatIDsFromVault).isEmpty())

            log.warn("***** All chats and messages *****")
            val allChatsFromVault = getAllChats(A)
            Assert.assertEquals(howManyChatsMessagesA, allChatsFromVault.size)

            val idsAllChatsFromVault = allChatsFromVault.map {
                it.state.data.linearId
            }.toList().distinct()

            Assert.assertEquals(idsAllChatsFromVault.size, howManyUniqueIdentifierA.size)
            Assert.assertTrue((idsAllChatsFromVault - howManyUniqueIdentifierA).isEmpty())
            Assert.assertTrue((howManyUniqueIdentifierA - idsAllChatsFromVault).isEmpty())

            // check message level information, take C as example
            val node = A
            val idList = howManyUniqueIdentifier.getOrDefault(node.nodeInfo.legalIdentities.first(), mutableSetOf())
            val howManyChatsParticipantsA = howManyChatsParticipants.getOrDefault(A, emptyList())

            for (id in idList) {
                val chatAllMessagesFromVault = getChatAllMessages(node, id)
                log.warn("***** Messages for ID: $id *****")
                Assert.assertEquals(chatAllMessagesFromVault.size, 3)
                chatAllMessagesFromVault.map {
                    log.warn("${it.state.data}")
                }

                log.warn("***** chat status: active, close proposed, closed for one chat by ID *****")
                val chatStatusFromVault = getChatCurrentStatus(node, id)
                Assert.assertEquals(chatStatusFromVault, ChatStatus.ACTIVE)


                log.warn("***** All participants for one chat by ID *****")
                val chatParticipantsFromVault = getChatParticipants(node, id)
                Assert.assertEquals(chatParticipantsFromVault.size, howManyChatsParticipantsA.size)
                Assert.assertEquals((chatParticipantsFromVault - howManyChatsParticipantsA).size, 0)
                Assert.assertEquals((howManyChatsParticipantsA - chatParticipantsFromVault).size, 0)
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
            log.warn("***** The chat ${chatOnB.linearId} is replied from B *****")

            log.warn("***** add participants from B *****")
            addParticipantsToChat(proposer = B, toAdd = listOf(C.legalIdentity()), chatId = chatOnA.linearId)
            log.warn("***** Participant ${C.legalIdentity()} is add to chat ${chatOnB.linearId} *****")

            log.warn("***** All chatIDs *****")
            val allChatIDsFromVault = getAllChatIDs(A)
            log.warn("***** All chatIDs: $allChatIDsFromVault *****")

            log.warn("***** All chats and messages *****")
            val allChatsFromVault = getAllChats(A)
            log.warn("***** All chats: $allChatsFromVault *****")

            log.warn("***** All messages for one single chat by ID: ${chatOnA.linearId} *****")
            val chatAllMessagesFromVault = getChatAllMessages(A, chatOnA.linearId)
            log.warn("***** All messages for ${chatOnA.linearId} are: $chatAllMessagesFromVault *****")

            log.warn("***** Chat status: active, close proposed, closed for one chat by ID *****")
            val chatStatusFromVault = getChatCurrentStatus(B, chatOnA.linearId)
            log.warn("***** Chat status for ${chatOnA.linearId} is: $chatStatusFromVault *****")

            log.warn("***** All participants for one chat by ID *****")
            val chatParticipantsFromVault = getChatParticipants(C, chatOnA.linearId)
            log.warn("***** The participants for ${chatOnA.linearId} are: $chatParticipantsFromVault *****")

            log.warn("***** close from B *****")
            closeChat(proposer = B, chatId = chatOnA.linearId)
            log.warn("***** Chat ${chatOnA.linearId} is closed *****")

            log.warn("**** All passed, happy *****")
        }
    }
}

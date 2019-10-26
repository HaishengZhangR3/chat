// DOCSTART ChatRPCWrapper
/**
 * This is an exmple code showing you how to call Chat SDK API.
 * Purpose of this is so that you are able to:
 *      - build their own business logic with the help of Chat SDK, and/or
 *      - expose user-friendly service API instead of Corda specific API
 *
 * You can implement this logic in either an application or a CorDapp.
 * In order to compile your application, you'd include the followign jar file in the project settings:
 *
 *      cordaCompile "$corda_release_group:corda-core:$corda_release_version"
 *      cordaCompile "$corda_release_group:chat-flows:$corda_chat_release_version"
 *
 *  And import package and classes you may use.
 *
 */
class ChatRPCWrapper() {

    /**
     * Through [proxy], the node exposes RPC operations to clients.
     */
    lateinit var proxy: CordaRPCOps
    constructor(newProxy: CordaRPCOps) : this() {
        proxy = newProxy
    }

    /**
     * Here are some examples showing how you'd access Chat SDK.
     * Similarly you'd implement all of the other API calls.
     */
    fun createChat(subject: String, content: String, receivers: List<Party>): ChatInfo =
        proxy.startFlow(
                ::CreateChatFlow,
                subject,
                content,
                receivers
        ).returnValue.getOrThrow()

    fun replyChat(chatId: UniqueIdentifier, content: String): ChatInfo =
        val replyChat = proxy.startFlow(
                ::ReplyChatFlow,
                chatId,
                content
        ).returnValue.getOrThrow()
        return replyChat.coreTransaction.outputStates.single() as ChatInfo
    }

    fun getAllChats(): List<StateAndRef<ChatInfo>> =
        proxy.startFlow(
                ::AllChats
        ).returnValue.getOrThrow()

    fun getChatAllMessages(chatId: UniqueIdentifier): List<StateAndRef<ChatInfo>> =
        proxy.startFlow(
                ::ChatAllMessages,
                chatId
        ).returnValue.getOrThrow()
}
// DOCEND ChatRPCWrapper

package net.corda.server.controllers

import com.r3.corda.lib.chat.contracts.states.ChatMessage
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.server.NodeRPCConnection
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Define CorDapp-specific endpoints in a controller such as this.
 */
@RestController
@RequestMapping("/custom") // The paths for GET and POST requests are relative to this base path.
class CustomController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val rpcOps = rpc.proxy
    private val me = rpcOps.nodeInfo().legalIdentities.first()

    @GetMapping(value = ["/test"], produces = arrayOf("text/plain"))
    private fun status() = "from test web"

    @GetMapping(value = ["/createChat"], produces = arrayOf("text/plain"))
    private fun createchat() = createChat(listOf(me), "from test web").content

    private fun createChat( toList: List<Party>, any: String): ChatMessage {
        val createChat = rpcOps.startFlow(
                ::CreateChatFlow,
                "Sample Topic $any",
                "Some sample content created $any",
                toList
        ).returnValue.getOrThrow()
        return createChat.state.data
    }
}
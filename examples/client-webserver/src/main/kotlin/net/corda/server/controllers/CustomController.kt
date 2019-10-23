package net.corda.server.controllers

import com.r3.corda.lib.chat.contracts.states.ChatInfo
import com.r3.corda.lib.chat.workflows.flows.CreateChatFlow
import net.corda.core.identity.Party
import net.corda.core.internal.toX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.getOrThrow
import net.corda.server.NodeRPCConnection
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
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
    private fun createchat() = createChat(listOf(me), "from test web").subject

    private fun createChat( toList: List<Party>, any: String): ChatInfo {
        val createChat = rpcOps.startFlow(
                ::CreateChatFlow,
                "Sample Topic $any",
                "Some sample content created $any",
                null,
                toList
        ).returnValue.getOrThrow()
        return createChat.state.data
    }
}
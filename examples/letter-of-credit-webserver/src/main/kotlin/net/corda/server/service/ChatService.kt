package net.corda.server.service

import com.r3.demo.chatapi.ChatApi
import net.corda.core.messaging.CordaRPCOps
import org.springframework.stereotype.Service

@Service
class ChatService {
    companion object {
        fun api(proxy: CordaRPCOps) = ChatApi(proxy)
    }
}
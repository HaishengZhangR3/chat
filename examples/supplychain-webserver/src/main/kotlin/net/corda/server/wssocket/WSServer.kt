package net.corda.server.wssocket

import net.corda.core.utilities.loggerFor
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.net.InetSocketAddress

// refer code from:
// https://github.com/TooTallNate/Java-WebSocket/blob/master/src/main/example/ChatServer.java

@Service
class WSServer : WebSocketServer {

	companion object {
		private val logger: Logger = loggerFor<WSServer>()
	}

	constructor(address: InetSocketAddress) : super(address)
    constructor(port: Int) : this(InetSocketAddress(port))

	private val notifyList: MutableList<WebSocket> = mutableListOf()
	fun getNotifyList() = notifyList

	fun addNotify(client: WebSocket): MutableList<WebSocket> = notifyList.also {it.add(client)}

	override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
		notifyList.add(conn!!)
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        notifyList.remove(conn!!)
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        TODO("not implemented")
    }

    override fun onStart() {
        logger.info("Server started!");
		connectionLostTimeout = 0;
		connectionLostTimeout = 100;
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
		logger.error("Server error!");
		ex!!.printStackTrace()
	}
}
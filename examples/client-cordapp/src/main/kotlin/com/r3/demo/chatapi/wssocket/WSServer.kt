package com.r3.demo.chatapi.wssocket

import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.slf4j.Logger
import java.io.File
import java.net.InetSocketAddress
import java.time.Instant
import java.util.*


// refer code from:
// https://github.com/TooTallNate/Java-WebSocket/blob/master/src/main/example/ChatServer.java
@CordaService
class WSService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

	var wsServer: WSServer
	init {
		val org = serviceHub.myInfo.legalIdentitiesAndCerts.single().party.name.organisation
		val port: Int = loadPort("${org}.webSocketPort")
		wsServer = WSServer(port)
		wsServer.start()
	}

	private fun loadPort(item: String): Int {
		val file = WSService::class.java.classLoader.getResourceAsStream("application.properties")
		val prop = Properties()
		prop.load(file)
		return prop.getProperty(item).toInt()
	}
}

class WSServer : WebSocketServer {

	private val file = "/Users/haishengzhang/Documents/tmp/observer${Instant.now()}.log"

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
		logIt("${Instant.now()}: $conn openned.")
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        notifyList.remove(conn!!)
		logIt("${Instant.now()}: $conn closed.")
	}

    override fun onMessage(conn: WebSocket?, message: String?) {
		logIt("${Instant.now()}: $conn give me a message: ${message!!}.")
    }

    override fun onStart() {
		logIt("${Instant.now()}: web socket started")

		connectionLostTimeout = 0;
		connectionLostTimeout = 100;
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
		logIt("${Instant.now()}: web socket server error: ${ex!!.stackTrace}")
	}

	private fun logIt(message: String){
		File(file).appendText("web socket [${port}] log: $message")
	}
}
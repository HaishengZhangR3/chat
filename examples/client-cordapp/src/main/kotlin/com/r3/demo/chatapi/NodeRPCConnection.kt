package com.r3.demo.chatapi

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort

open class NodeRPCConnection(
        private val host: String,
        private val username: String,
        private val password: String,
        private val rpcPort: Int): AutoCloseable {

    lateinit var rpcConnection: CordaRPCConnection
        private set
    lateinit var proxy: CordaRPCOps
        private set

    fun initialiseNodeRPCConnection() {
            val rpcAddress = NetworkHostAndPort(host, rpcPort)
            val rpcClient = CordaRPCClient(rpcAddress)
            val rpcConnection = rpcClient.start(username, password)
            proxy = rpcConnection.proxy
    }

    override fun close() {
        rpcConnection.notifyServerAndClose()
    }
}
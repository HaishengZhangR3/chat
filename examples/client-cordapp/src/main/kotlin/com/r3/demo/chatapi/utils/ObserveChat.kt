package scripts

import com.r3.corda.lib.chat.contracts.states.*
import com.r3.demo.chatapi.ChatApi
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria

fun observe(proxy: CordaRPCOps?): Unit {

    val oldMessages: MutableList<StateAndRef<ChatInfo>> = mutableListOf()
    val oldUpdates: MutableList<StateAndRef<UpdateParticipantsState>> = mutableListOf()
    val oldCloses: MutableList<StateAndRef<CloseChatState>> = mutableListOf()

    if (proxy == null) {
        println("RPC connection is null, so quit.")
        return
    }

    val chatApi = ChatApi()
    chatApi.init(proxy)

    while (true) {
        val allMessages = chatApi.getAllChats()
        val newAddedMessages = allMessages - oldMessages
        if (newAddedMessages.isNotEmpty()) {
            newAddedMessages.forEach {
                println("~~~~~~~~~~ Get a new message ~~~~~~~~~~")
                println(it)
            }
            oldMessages.clear()
            oldMessages.addAll(allMessages)
        }



        val allUpdateEvents = proxy.vaultQueryByCriteria(QueryCriteria.LinearStateQueryCriteria(
                status = Vault.StateStatus.ALL),
                contractStateType = UpdateParticipantsState::class.java)
                .states

        val newUpdateEvents = allUpdateEvents - oldUpdates
        if (newUpdateEvents.isNotEmpty()) {
            newUpdateEvents.forEach {
                val state = it.state.data
                if (state.status == UpdateParticipantsStatus.PROPOSED) {
                    println("~~~~~~~~~~ Get a new proposal of updating participants ~~~~~~~~~~")
                } else if (state.status == UpdateParticipantsStatus.AGREED) {
                    println("~~~~~~~~~~ Get a new agreement of updating participants ~~~~~~~~~~")
                } else {
                    println("~~~~~~~~~~ Get an event of finishing updating participants ~~~~~~~~~~")
                }
                println(it)
            }
            oldUpdates.clear()
            oldUpdates.addAll(allUpdateEvents)
        }



        val allEvents = proxy.vaultQueryByCriteria(QueryCriteria.LinearStateQueryCriteria(
                status = Vault.StateStatus.ALL),
                contractStateType = CloseChatState::class.java)
                .states
        val newEvents = allEvents - oldCloses
        if (newEvents.isNotEmpty()) {
            newEvents.forEach {
                val state = it.state.data
                if (state.status == CloseChatStatus.PROPOSED) {
                    println("~~~~~~~~~~ Get a new proposal of closing chat ~~~~~~~~~~")
                } else if (state.status == CloseChatStatus.AGREED) {
                    println("~~~~~~~~~~ Get a new agreement of closing chat ~~~~~~~~~~")
                } else {
                    println("~~~~~~~~~~ Get an event of finishing close chat ~~~~~~~~~~")
                }
                println(it)
            }

            oldCloses.clear()
            oldCloses.addAll(allEvents)
        }


        Thread.sleep(1000)
    }
}

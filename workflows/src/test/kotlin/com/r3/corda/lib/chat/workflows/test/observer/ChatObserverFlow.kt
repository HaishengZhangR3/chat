package com.r3.corda.lib.chat.workflows.test.observer

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy

@InitiatedBy(ChatNotifyFlow::class)
class ChatObserverFlow(private val otherSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
    }
}

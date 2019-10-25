package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.internal.RemoveParticipantsFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party


@InitiatingFlow
@StartableByService
@StartableByRPC
class RemoveParticipantsFlow(
        private val chatId: UniqueIdentifier,
        private val toRemove: List<Party>
) : FlowLogic<StateAndRef<ChatMetaInfo>>() {
    @Suspendable
    override fun call(): StateAndRef<ChatMetaInfo> {
        return subFlow(RemoveParticipantsFlow(
                chatId = chatId,
                toRemove = toRemove
        ))
    }
}

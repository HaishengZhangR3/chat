package com.r3.corda.lib.chat.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import com.r3.corda.lib.chat.workflows.flows.internal.UpdateReceiversFlow
import com.r3.corda.lib.chat.workflows.flows.utils.chatVaultService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party

@InitiatingFlow
@StartableByService
@StartableByRPC
class AddParticipantsFlow(
        private val chatId: UniqueIdentifier,
        private val toAdd: List<Party>
) : FlowLogic<StateAndRef<ChatMetaInfo>>() {
    @Suspendable
    override fun call(): StateAndRef<ChatMetaInfo> {
        val metaInfoStateRef = chatVaultService.getMetaInfoOrNull(chatId)
        require(metaInfoStateRef != null) { "ChatId must exist." }

        val metaInfo = metaInfoStateRef!!.state.data
        requireThat {
            "Only chat admin can add participants to chat." using (ourIdentity == metaInfo.admin)
        }

        return subFlow(UpdateReceiversFlow(
                chatId = chatId,
                toAdd = toAdd
        ))
    }
}

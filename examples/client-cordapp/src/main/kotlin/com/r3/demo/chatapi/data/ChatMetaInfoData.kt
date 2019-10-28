package com.r3.demo.chatapi.data

import com.r3.corda.lib.chat.contracts.ChatMetaInfoContract
import com.r3.corda.lib.chat.contracts.states.ChatMetaInfo
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import java.time.Instant

@BelongsToContract(ChatMetaInfoContract::class)
data class ChatMetaInfoData(
        val linearId: UniqueIdentifier,
        val created: Instant,
        val participants: List<String>,
        val admin: String,
        val receivers: List<String>,
        val subject: String,
        val status: String
){
    companion object {
        fun fromState(chatMetaInfo: ChatMetaInfo): ChatMetaInfoData =
                ChatMetaInfoData(
                        linearId = chatMetaInfo.linearId,
                        created = chatMetaInfo.created,
                        participants = chatMetaInfo.participants.map { it.nameOrNull()!!.organisation },
                        admin = chatMetaInfo.admin.name.organisation,
                        receivers = chatMetaInfo.receivers.map { it.nameOrNull().organisation },
                        subject = chatMetaInfo.subject,
                        status = chatMetaInfo.status.toString()
                )
    }
}

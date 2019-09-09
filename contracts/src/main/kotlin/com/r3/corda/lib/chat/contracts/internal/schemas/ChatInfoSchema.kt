package com.r3.corda.lib.chat.contracts.internal.schemas

import net.corda.core.contracts.Attachment
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.*

object ChatSchema : MappedSchema(
        PersistentChatInfo::class.java,
        version = 1,
        mappedTypes = listOf(PersistentChatInfo::class.java)
)

@Entity
@Table(name = "chat", uniqueConstraints = [
    UniqueConstraint(name = "id_constraint", columnNames = ["identifier"])
], indexes = [
    Index(name = "chatId_idx", columnList = "identifier")
])
data class PersistentChatInfo(
        @Column(name = "identifier", unique = true, nullable = false)
        val identifier: UUID,
        @Column(name = "subject", unique = false, nullable = false)
        val subject: String,
        @Column(name = "content", unique = false, nullable = false)
        val content: String,
        @Column(name = "attachments", unique = false, nullable = true)
        val attachment: Attachment? = null,
        @Column(name = "from", unique = false, nullable = false)
        val from: Party,
        @Column(name = "to", unique = false, nullable = false)
        val to: List<Party>
) : PersistentState()

package com.r3.corda.lib.chat.contracts.internal.schemas

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.*

object ChatSchema : MappedSchema(
        PersistentChatInfo::class.java,
        version = 1,
        mappedTypes = listOf(PersistentChatInfo::class.java)
)

@Entity
@Table(
        name = "chat",
        uniqueConstraints = [
            UniqueConstraint(name = "id_constraint",
                    columnNames = ["identifier", "created", "output_index", "transaction_id"])
        ],
        indexes = [
            Index(name = "chatId_idx", columnList = "identifier", unique = false),
            Index(name = "chatCreated_idx", columnList = "created", unique = false)
        ]
)
data class PersistentChatInfo(
        // created time
        @Column(name = "created", unique = false, nullable = false)
        val created: Instant,
        // identifier is the linearId to indicate a chat thread
        @Column(name = "identifier", unique = false, nullable = false)
        val identifier: UUID,
        @Column(name = "subject", unique = false, nullable = false)
        val subject: String,
        @Column(name = "content", unique = false, nullable = false)
        val content: String,
        @Column(name = "attachments", unique = false, nullable = true)
        val attachment: String? = null,
        @Column(name = "chatFrom", unique = false, nullable = false)
        val chatFrom: Party,
        @ElementCollection
        @Column(name = "chatToList", unique = false, nullable = false)
        @CollectionTable(name = "chat_tos", joinColumns = [(JoinColumn(name = "output_index", referencedColumnName = "output_index")), (JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id"))])
        val chatToList: List<Party>

) : PersistentState()

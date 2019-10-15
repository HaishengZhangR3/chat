package com.r3.corda.lib.chat.contracts.internal.schemas

import net.corda.core.identity.AbstractParty
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
        // identifier is the linearId to indicate a chat thread
        @Column(name = "identifier", unique = false, nullable = false)
        val identifier: UUID,
        // created time
        @Column(name = "created", unique = false, nullable = false)
        val created: Instant,
        @Column(name = "subject", unique = false, nullable = false)
        val subject: String,
        @Column(name = "content", unique = false, nullable = false)
        val content: String,
        @Column(name = "attachments", unique = false, nullable = true)
        val attachment: String? = null,
        @Column(name = "chatSender", unique = false, nullable = false)
        val chatSender: Party,

        // @todo: toList are not saved in DB table, check why
        @ElementCollection
        @Column(name = "chatReceiverList", unique = false, nullable = false)
        @CollectionTable(name = "chat_receivers", joinColumns = [(JoinColumn(name = "output_index", referencedColumnName = "output_index")), (JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id"))])
        val chatReceiverList: List<Party>,

        @ElementCollection
        @Column(name = "participants", unique = false, nullable = false)
        @CollectionTable(name = "chat_participants", joinColumns = [(JoinColumn(name = "output_index", referencedColumnName = "output_index")), (JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id"))])
        val participants: List<AbstractParty>

) : PersistentState()

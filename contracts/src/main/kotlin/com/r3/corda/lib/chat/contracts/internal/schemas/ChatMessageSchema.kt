package com.r3.corda.lib.chat.contracts.internal.schemas

import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.*

object ChatMessageSchema : MappedSchema(
        PersistentChatMessage::class.java,
        version = 1,
        mappedTypes = listOf(PersistentChatMessage::class.java)
)

@Entity
@Table(
        name = "chat_messages",
        uniqueConstraints = [
            UniqueConstraint(name = "chat_messages_id_constraint",
                    columnNames = ["identifier", "created", "output_index", "transaction_id"])
        ],
        indexes = [
            Index(name = "chat_messages_id_idx", columnList = "identifier", unique = false),
            Index(name = "chat_messages_created_idx", columnList = "created", unique = false)
        ]
)
data class PersistentChatMessage(
        // identifier is the linearId to indicate a chat thread
        @Column(name = "identifier", unique = false, nullable = false)
        val identifier: UUID,
        // created time
        @Column(name = "created", unique = false, nullable = false)
        val created: Instant,
        @Column(name = "content", unique = false, nullable = false)
        val content: String,
        @Column(name = "sender", unique = false, nullable = false)
        val sender: Party,

        @ElementCollection
        @Column(name = "participants", unique = false, nullable = false)
        @CollectionTable(name = "chat_messages_participants", joinColumns = [(JoinColumn(name = "output_index", referencedColumnName = "output_index")), (JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id"))])
        val participants: List<AbstractParty>

) : PersistentState()

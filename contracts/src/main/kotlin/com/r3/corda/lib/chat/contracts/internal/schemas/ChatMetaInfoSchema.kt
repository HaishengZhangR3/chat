package com.r3.corda.lib.chat.contracts.internal.schemas

import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.*

object ChatMetaInfoSchema : MappedSchema(
        PersistentChatMetaInfo::class.java,
        version = 1,
        mappedTypes = listOf(PersistentChatMetaInfo::class.java)
)

@Entity
@Table(
        name = "chat_meta_info",
        uniqueConstraints = [
            UniqueConstraint(name = "chat_meta_id_constraint",
                    columnNames = ["identifier", "created", "output_index", "transaction_id"])
        ],
        indexes = [
            Index(name = "chat_meta_id_idx", columnList = "identifier", unique = false),
            Index(name = "chat_meta_created_idx", columnList = "created", unique = false)
        ]
)
data class PersistentChatMetaInfo(
        // identifier is the linearId to indicate a chat thread
        @Column(name = "identifier", unique = false, nullable = false)
        val identifier: UUID,
        // created time
        @Column(name = "created", unique = false, nullable = false)
        val created: Instant,
        @Column(name = "admin", unique = false, nullable = false)
        val admin: Party,
        @Column(name = "subject", unique = false, nullable = false)
        val subject: String,
        @Column(name = "status", unique = false, nullable = false)
        val status: String,

        @ElementCollection
        @Column(name = "chatReceiverList", unique = false, nullable = false)
        @CollectionTable(name = "chat_receivers", joinColumns = [(JoinColumn(name = "output_index", referencedColumnName = "output_index")), (JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id"))])
        val chatReceiverList: List<Party>,

        @ElementCollection
        @Column(name = "participants", unique = false, nullable = false)
        @CollectionTable(name = "chat_meta_participants", joinColumns = [(JoinColumn(name = "output_index", referencedColumnName = "output_index")), (JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id"))])
        val participants: List<AbstractParty>

) : PersistentState()

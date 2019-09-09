package com.r3.corda.lib.accounts.workflows

import com.r3.corda.lib.accounts.contracts.internal.schemas.PersistentAccountInfo
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountObservedQueryBy
import com.r3.corda.lib.accounts.workflows.internal.accountObservedTrackBy
import com.r3.corda.lib.accounts.workflows.internal.schemas.AllowedToSeeStateMapping
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.node.services.vault.VaultSchemaV1
import java.util.*

/** Helper for obtaining a [KeyManagementBackedAccountService]. */
val FlowLogic<*>.accountService get() = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

// TODO: Remove this and replace with a utility in a commons CorDapp.
val ServiceHub.ourIdentity get() = myInfo.legalIdentities.first()

// Query utilities.

/** Returns the base [AccountInfo] query criteria. */
val accountBaseCriteria = QueryCriteria.VaultQueryCriteria(
        contractStateTypes = setOf(AccountInfo::class.java),
        status = Vault.StateStatus.UNCONSUMED
)

/** To query [AccountInfo]s by host. */
fun accountHostCriteria(host: Party): QueryCriteria {
    return builder {
        val partySelector = PersistentAccountInfo::host.equal(host)
        QueryCriteria.VaultCustomQueryCriteria(partySelector)
    }
}

/** To query [AccountInfo]s by name. */
fun accountNameCriteria(name: String): QueryCriteria {
    return builder {
        val nameSelector = PersistentAccountInfo::name.equal(name)
        QueryCriteria.VaultCustomQueryCriteria(nameSelector)
    }
}

/** To query [AccountInfo]s by id. */
fun accountUUIDCriteria(id: UUID): QueryCriteria {
    return builder {
        val idSelector = PersistentAccountInfo::id.equal(id)
        QueryCriteria.VaultCustomQueryCriteria(idSelector)
    }
}

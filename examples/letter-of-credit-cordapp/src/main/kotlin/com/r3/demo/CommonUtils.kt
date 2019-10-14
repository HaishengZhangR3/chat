package com.r3.demo

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken

val FlowLogic<*>.vaultService get() = serviceHub.cordaService(VaultService::class.java)

@CordaService
class VaultService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    /*  find notary */
    fun notary() = serviceHub.networkMapCache.notaryIdentities.first()

    inline fun <reified T : ContractState> getVaultStates(status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED): List<StateAndRef<T>> {

        val stateAndRefs = serviceHub.vaultService.queryBy<T>(
                criteria = QueryCriteria.VaultQueryCriteria(status = status))
        return stateAndRefs.states
    }

}
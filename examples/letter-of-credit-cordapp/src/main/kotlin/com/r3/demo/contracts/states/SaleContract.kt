package com.r3.demo.contracts.states

import com.r3.demo.contracts.ElectronicLetterOfCreditContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(ElectronicLetterOfCreditContract::class)
data class SaleContract(
        val buyer: Party,
        val seller: Party,
        val productName: String,
        val price: Amount<Currency>,
        override val participants: List<AbstractParty> = listOf(buyer, seller)
) : ContractState

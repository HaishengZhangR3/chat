package com.r3.demo.contracts.states

import com.r3.demo.contracts.ElectronicLetterOfCreditContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

@BelongsToContract(ElectronicLetterOfCreditContract::class)
data class ElectronicDocument(
        val issuer: Party,
        val adviser: Party,
        val applicant: Party,
        val beneficiary: Party,
        val productName: String,
        val price: Amount<Currency>,
        val timeShipped: Instant,
        val timeReceived: Instant,
        override val participants: List<AbstractParty> = listOf(issuer, adviser, applicant, beneficiary)
) : ContractState

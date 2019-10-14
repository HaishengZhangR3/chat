package com.r3.demo.contracts.states

import com.r3.demo.contracts.ElectronicLetterOfCreditContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(ElectronicLetterOfCreditContract::class)
data class ElectronicLetterOfCredit(
        override val participants: List<AbstractParty>,
        val subject: String = "",
        val content: String = "",
        val attachment: SecureHash? = null,
        val from: Party,
        val to: List<Party>
) : ContractState

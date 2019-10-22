package com.r3.demo.contracts.states

import com.r3.demo.contracts.ElectronicLetterOfCreditContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.util.*


// the Letter of Credit state is created with reference of:
// https://images.sampletemplates.com/wp-content/uploads/2017/02/15234946/Commercial-Letter-of-Credit.pdf.zip

@CordaSerializable
sealed class LoCStatus {
    object APPLIED : LoCStatus()  // applicant applied
    object APPROVED : LoCStatus() // issuer (issuing bank) approved
    object ISSUED : LoCStatus()   // issuer issued to advising bank
    object ADVISED : LoCStatus()  // advising bank advised to beneficiary
}
@BelongsToContract(ElectronicLetterOfCreditContract::class)
data class ElectronicLetterOfCredit(
        val issuer: Party,
        val applicant: Party,
        val beneficiary: Party,
        val advisingBank: Party,
        val timeIssued: Instant? = null,
        val status: LoCStatus,
        val amountLimit: Amount<Currency>,
        override val participants: List<AbstractParty>
) : ContractState

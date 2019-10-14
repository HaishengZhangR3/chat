package com.r3.demo.contracts.states

import com.r3.demo.contracts.ElectronicLetterOfCreditContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant


@CordaSerializable
sealed class ShipmentStatus {
    object SHIPPED : ShipmentStatus()  // shipped by exporter
    object RECEIVED : ShipmentStatus() // received by importer
    object DOCUMENTED : ShipmentStatus()  // after shipment, the bill of shipment is treated as document to share among all 4 parties
}
@BelongsToContract(ElectronicLetterOfCreditContract::class)
data class Shipment(
        val exporter: Party,
        val importer: Party,
        val productName: String,
        val timeShipped: Instant,
        val timeReceived: Instant?,
        val status: ShipmentStatus,
        override val participants: List<AbstractParty> = listOf(exporter, importer)
) : ContractState

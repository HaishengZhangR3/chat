package com.r3.corda.lib.chat.contracts.states

import com.r3.corda.lib.chat.contracts.ChatInfoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import java.time.Instant


@BelongsToContract(ChatInfoContract::class)
interface ChatBaseState : LinearState {
    val created: Instant
}
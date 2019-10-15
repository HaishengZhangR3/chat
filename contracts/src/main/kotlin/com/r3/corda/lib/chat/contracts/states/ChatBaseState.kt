package com.r3.corda.lib.chat.contracts.states

import net.corda.core.contracts.LinearState
import java.time.Instant

interface ChatBaseState : LinearState {
    val created: Instant
}
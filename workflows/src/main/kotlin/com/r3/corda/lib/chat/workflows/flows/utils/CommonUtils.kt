package com.r3.corda.lib.chat.workflows.flows.utils

import net.corda.core.flows.FlowLogic


val FlowLogic<*>.chatVaultService get() = serviceHub.cordaService(ChatVaultService::class.java)

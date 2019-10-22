package com.r3.demo.chatapi.observer

import com.r3.demo.chatapi.wssocket.WSService
import net.corda.core.flows.FlowLogic


val FlowLogic<*>.wsService get() = serviceHub.cordaService(WSService::class.java)

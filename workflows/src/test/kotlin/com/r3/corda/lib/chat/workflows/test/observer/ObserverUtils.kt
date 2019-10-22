package com.r3.corda.lib.chat.workflows.test.observer

import net.corda.testing.node.StartedMockNode

object ObserverUtils {
    fun registerObserver(observers: List<StartedMockNode>) {
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (node in observers) {
            node.registerInitiatedFlow(ChatObserverFlow::class.java)
        }
    }
}
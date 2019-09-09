package com.r3.corda.lib.chat.contracts.commands

import net.corda.core.contracts.CommandData

/** Commands to be used in conjunction with [ChatInfo] states. */
interface ChatCommand : CommandData

/** For use when creating an [ChatInfo]. */
class Create : ChatCommand

/** For use when chatting an [ChatInfo]. */
class Reply : ChatCommand

/** For use when closing an [ChatInfo]. */
class Close : ChatCommand

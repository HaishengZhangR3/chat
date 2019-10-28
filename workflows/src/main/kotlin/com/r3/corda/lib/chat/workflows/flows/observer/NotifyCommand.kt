package com.r3.corda.lib.chat.workflows.flows.observer

import net.corda.core.contracts.CommandData

interface NotifyCommand : CommandData

/* Command below are used to notify client application */
class CreateCommand: NotifyCommand
class ReplyCommand: NotifyCommand
class CloseCommand: NotifyCommand
class AddParticipantsCommand : NotifyCommand
class RemoveParticipantsCommand : NotifyCommand

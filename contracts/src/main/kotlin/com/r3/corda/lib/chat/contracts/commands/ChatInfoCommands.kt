package com.r3.corda.lib.chat.contracts.commands

import net.corda.core.contracts.CommandData

interface ChatCommand : CommandData

/* Commands below are used in Chat SDK internal */
// Command for basic chat
class CreateMeta : ChatCommand
class CreateMessage : ChatCommand

class CloseMeta : ChatCommand
class CloseMessages : ChatCommand

// Command to update participants
class AddReceivers : ChatCommand
class RemoveReceivers : ChatCommand

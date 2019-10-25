package com.r3.corda.lib.chat.contracts.commands

import net.corda.core.contracts.CommandData

interface ChatCommand : CommandData

// Command for basic chat
class CreateMeta : ChatCommand
class CreateMessage : ChatCommand

class CloseMeta : ChatCommand
class CloseMessages : ChatCommand

// Command to update participants
class AddParticipants : ChatCommand
class RemoveParticipants : ChatCommand


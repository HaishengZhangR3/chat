package com.r3.corda.lib.chat.contracts.commands

import net.corda.core.contracts.CommandData

/** Commands to be used with [ChatInfo] states. */
interface ChatCommand : CommandData

class Create : ChatCommand
class Reply : ChatCommand
class Close : ChatCommand

class AddParticipants : ChatCommand
class RemoveParticipants : ChatCommand

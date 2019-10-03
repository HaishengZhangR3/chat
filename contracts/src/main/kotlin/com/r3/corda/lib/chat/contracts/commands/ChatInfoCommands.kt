package com.r3.corda.lib.chat.contracts.commands

import net.corda.core.contracts.CommandData

interface ChatCommand : CommandData

class Create : ChatCommand
class Reply : ChatCommand
class SyncUpHistory : ChatCommand

class Close : ChatCommand
class ProposeClose : ChatCommand
class AgreeClose : ChatCommand
class RejectClose : ChatCommand

class AddParticipants : ChatCommand
class AgreeAddParticipants : ChatCommand
class RejectAddParticipants : ChatCommand

class RemoveParticipants : ChatCommand
class AgreeRemoveParticipants : ChatCommand
class RejectRemoveParticipants : ChatCommand



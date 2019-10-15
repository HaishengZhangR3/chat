package com.r3.corda.lib.chat.contracts.commands

import net.corda.core.contracts.CommandData

interface ChatCommand : CommandData

// @todo: should create as much detailed command as possible so as that the contract can check
// Command for basic chat
class Create : ChatCommand
class Reply : ChatCommand
class ShareHistory : ChatCommand

// Command for close a chat
class Close : ChatCommand
class ProposeClose : ChatCommand
class AgreeClose : ChatCommand
class RejectClose : ChatCommand

// Command to update participants
class UpdateParticipants : ChatCommand
class ProposeUpdateParticipants : ChatCommand
class AgreeUpdateParticipants : ChatCommand
class RejectUpdateParticipants : ChatCommand

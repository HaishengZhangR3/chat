package com.r3.corda.lib.chat.contracts.commands

import net.corda.core.contracts.CommandData

interface ChatCommand : CommandData

class Create : ChatCommand
class Reply : ChatCommand
class ShareHistory : ChatCommand

class Close : ChatCommand
class ProposeClose : ChatCommand
class AgreeClose : ChatCommand
class RejectClose : ChatCommand

class UpdateParticipants : ChatCommand
class ProposeUpdateParticipants : ChatCommand
class AgreeUpdateParticipants : ChatCommand
class RejectUpdateParticipants : ChatCommand

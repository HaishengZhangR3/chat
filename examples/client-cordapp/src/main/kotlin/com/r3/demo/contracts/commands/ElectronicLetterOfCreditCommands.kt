package com.r3.demo.contracts.commands

import net.corda.core.contracts.CommandData

interface LoCCommand : CommandData

class SignContract : LoCCommand
class ApplyELC : LoCCommand
class AgreeELC : LoCCommand
class RejectELC : LoCCommand
class IssueELC : LoCCommand
class AdviceELC : LoCCommand
class Ship : LoCCommand
class ReceiveShip : LoCCommand
class DispatchPresentReleaseEDoc : LoCCommand
class Pay : LoCCommand

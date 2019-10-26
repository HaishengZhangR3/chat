// DOCSTART ChatObserverFlow
/**
 * This is an exmple observer code showing you how to respond to [ChatNotifyFlow] in Chat SDK.
 * [IMPORTANT] You must implement this observer flow in your CorDapp, otherwise you'd receiver runtime error:
 *
 *      com.r3.corda.lib.chat.workflows.flows.observer.ChatNotifyFlow is not registered
 */

/**
 * To implement your own observer flow, you must:
 *      - annotate your flow as: @InitiatedBy(ChatNotifyFlow::class), and
 *      - implement [FlowLogic] together with its abstract call() method
 */
@InitiatedBy(ChatNotifyFlow::class)
class ChatObserverFlow(private val otherSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call(): Unit {
        /**
         * You'd choose either do some extension logic, or discard the information received.
         * The received information send from Chat SDK is a [List] with two elements inside:
         * First one is a [ChatCommand] showing which command send you this message;
         * Second one is a [List], where ChatMetaInfo and ChatMessage instances are saved;
         * [todo] You'd refer to Chat SDK document to know which command map to which state in the message.
         */
        val (command, info) = otherSession.receive<List<Any>>().unwrap { it }

        /**
         * Here is an example to interprete the command and its state to a string.
         * You'd do more extension work, like:
         *      Write to your own DB for auditing purpose,
         *      Perform automation respond or other work,
         *      Achieve messages,
         *      ....
         */
        parseData(command = command as ChatCommand, info = info as List<ContractState>))
    }

    private fun parseData(command: ChatCommand, info: ContractState): String =
            when (command) {
                is CreateMessage -> "New Message: " + chatInfoToString(info.single() as ChatMessage)
                is Close         -> {
                    val meta = info.single() as ChatMetaInfo;
                    "${meta.linearId} is closed by ${meta.admin.name.organisation}"
                // is xxxx
                else             -> ""
            }

    private fun chatInfoToString(info: ChatMessage) =
            """
                ChatId: ${info.linearId},
                Sender: ${info.sender.name.organisation},
                Content: ${info.content}
            """.trimIndent()

}
// DOCEND ChatObserverFlow

package com.example.contract

import com.example.state.OrderState
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.LedgerTransaction


open class OrderContract : Contract{
    override fun verify (tx: LedgerTransaction){
        val command=tx.commands.requireSingleCommand<Commands>()
        when (command.value){
            is Commands.Issue -> {
                requireThat {
                    "No inputs should be consumed when issuing an order." using (tx.inputs.isEmpty())
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<OrderState>().single()
                    "The seller and buyer must be different." using (out.seller != out.buyer)
                    "The amount cannot be 0." using (out.order.totalAmount != 0.0)
                    "The seller and the buyer must be signers." using (command.signers.containsAll(listOf(out.seller.owningKey, out.buyer.owningKey)))
                }
            }
    is Commands.Move -> {
        requireThat {
            "Only one input should be conmumed when move an order." using (tx.inputs.size == 1)
            "Only one output state should be created." using (tx.outputs.size == 1)
            val input = tx.inputsOfType<OrderState>().single()
            val out = tx.outputsOfType<OrderState>().single()
            "Buyer must be changed when move an order." using (input.buyer != out.buyer)
            }
        }
    }
}

interface Commands : CommandData{
    class Issue : TypeOnlyCommandData(), Commands
    class Move: TypeOnlyCommandData(), Commands
}

    override val legalContractReference: SecureHash = SecureHash.sha256("contract template and params")
}
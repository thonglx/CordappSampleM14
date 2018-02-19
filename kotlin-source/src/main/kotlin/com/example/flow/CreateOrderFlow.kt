package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.OrderContract
import com.example.flow.CreateOrderFlow.Acceptor
import com.example.flow.CreateOrderFlow.Initiator
import com.example.state.OrderState
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionType
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


object CreateOrderFlow{
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val orderState: OrderState,
                    val otherParty: Party): FlowLogic<SignedTransaction>() {
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating Transaction based on new order.")
            object VERIFYING_TRANSACTION: ProgressTracker.Step("Veryfying Contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with private keys.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering counter parties' signatures.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
                }
            object FINALIZING_TRANSACTIONS : ProgressTracker.Step("Obtaining Notary Signatures and Recording transaction.")
            {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,VERIFYING_TRANSACTION,SIGNING_TRANSACTION,GATHERING_SIGS,FINALIZING_TRANSACTIONS
            )
            }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity

            //Stage1
            progressTracker.currentStep = GENERATING_TRANSACTION
            //generate an unsigned transaction
            val txCommand = Command(OrderContract.Commands.Issue(),listOf(orderState.seller.owningKey, orderState.buyer.owningKey))
            val txBuilder = TransactionBuilder(TransactionType.General, notary).withItems(orderState,txCommand)

            //Stage 2
            progressTracker.currentStep = VERIFYING_TRANSACTION
            //Verify Transaction is valid
            txBuilder.toWireTransaction().toLedgerTransaction(serviceHub).verify()

            //Stage 3
            progressTracker.currentStep = SIGNING_TRANSACTION
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            //stage4
            progressTracker.currentStep = GATHERING_SIGS
            //send state to counter party receive signed state back
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx,GATHERING_SIGS.childProgressTracker()))

            //Stage 5
            progressTracker.currentStep = FINALIZING_TRANSACTIONS
            //Notarised and record transaction in both parties vaults
            return subFlow(FinalityFlow(fullySignedTx,FINALIZING_TRANSACTIONS.childProgressTracker())).single()
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor (val otherParty: Party): FlowLogic<SignedTransaction> () {
        @Suspendable
        override fun call() : SignedTransaction {
            val signTransactionFLow = object : SignTransactionFlow(otherParty){
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                    "This must be an order transaction." using (output is OrderState)
                }
            }
            return subFlow(signTransactionFLow)
        }
    }

}
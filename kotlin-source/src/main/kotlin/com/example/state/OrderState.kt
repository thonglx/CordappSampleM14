package com.example.state

import com.example.contract.OrderContract
import com.example.model.Order
import com.example.schema.OrderSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.keys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.security.PublicKey

import java.time.Instant
import java.util.*

data class OrderState ( val order : Order,
        val seller : Party,
        val buyer : Party,
        val date : Instant =Instant.now(),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
        ): LinearState,
        QueryableState {

    override val contract get() = OrderContract()
    /** Public keys for involved parties. */
    override val participants: List<AbstractParty> get() = listOf(buyer)

    /** vault to tract a state if parties are involved*/
    override fun isRelevant(ourKeys: Set<PublicKey>) = ourKeys.intersect(participants.flatMap{it.owningKey.keys}).isNotEmpty()

    fun withNewOwner (newOwner: Party) = copy(buyer = newOwner)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when(schema){
         is OrderSchemaV1 -> OrderSchemaV1.PersistentBL(
                 sellerName = this.seller.name.toString(),
                 buyerName = this.buyer.name.toString(),
                 referenceNumber = this.order.referenceNumber,
                 totalAmount = this.order.totalAmount,
                 date = Instant.now()
         )
            else -> throw IllegalArgumentException("Unregconized schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(OrderSchemaV1)
}
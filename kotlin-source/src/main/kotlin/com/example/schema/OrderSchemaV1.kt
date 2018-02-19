package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object OrderSchema
object OrderSchemaV1 : MappedSchema(
        schemaFamily = OrderSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBL::class.java)){
        @Entity
        @Table(name = "order_states")
        class PersistentBL(
            @Column(name = "seller_name")
            var sellerName: String,

            @Column(name = "buyer_name")
            var buyerName: String,

            @Column(name = "amount")
            var totalAmount: Double,

            @Column(name = "reference_no")
            var referenceNumber: String,

            @Column(name = "date")
            var date: Instant
            ) : PersistentState()
        }

package com.example.model

import net.corda.core.serialization.CordaSerializable


@CordaSerializable
        data class Order(
        val referenceNumber: String,
        val totalAmount: Double)
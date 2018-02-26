package com.example.api

import com.example.flow.CreateOrderFlow
import com.example.flow.ExampleFlow
import com.example.flow.ShippingFlow
import com.example.model.BL
import com.example.model.Order
import com.example.state.BLState
import com.example.state.OrderState
import net.corda.client.rpc.notUsed
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.getOrThrow
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.loggerFor
import net.sourceforge.plantuml.project.Instant

import org.bouncycastle.asn1.x500.X500Name
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

val NOTARY_NAME = "CN=Controller,O=R3,L=London,C=UK"

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(val services: CordaRPCOps) {
    private val myLegalName: X500Name = services.nodeIdentity().legalIdentity.name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
    }

    @GET
    @Path("orders")
    @Produces(MediaType.APPLICATION_JSON)
    fun getOrders(): List<StateAndRef<OrderState>>{
        val vaultStates = services.vaultQueryBy<OrderState>()
        return vaultStates.states
    }

    @PUT
    @Path("{seller}/{buyer}/create-order")
    fun createOrder(order: Order,
                    @PathParam("seller") seller: X500Name,
                    @PathParam("buyer") buyer: X500Name) : Response{
        val seller =services.partyFromX500Name(seller)
        if (seller == null){
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
        val buyer = services.partyFromX500Name(buyer)
        if (buyer == null){
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
        val state = OrderState(
                order,
                seller,
                buyer,
                date = java.time.Instant.now()
        )
        val (status, msg) = try {
            val flowHandle = services.
                    startTrackedFlowDynamic(CreateOrderFlow.Initiator::class.java, state, seller)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            Response.Status.CREATED to "Transaction id ${result.id} committed to ledger."
        } catch (ex: Throwable){
            logger.error(ex.message, ex)
            Response.Status.BAD_REQUEST to "Transaction failed."
        }
        return Response.status(status).entity(msg).build()
    }


    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<X500Name>> {
        val (nodeInfo, nodeUpdates) = services.networkMapUpdates()
        nodeUpdates.notUsed()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentity.name }
                .filter { it != myLegalName && it.toString() != NOTARY_NAME })
    }


}
package io.cordacademy.webserver.areas.obligation

import io.cordacademy.obligation.contract.ObligationState
import io.cordacademy.obligation.workflow.ObligationExitFlow
import io.cordacademy.obligation.workflow.ObligationIssuanceFlow
import io.cordacademy.obligation.workflow.ObligationSettlementFlow
import io.cordacademy.obligation.workflow.ObligationTransferFlow
import io.cordacademy.webserver.ofCurrency
import io.cordacademy.webserver.resolveParty
import io.cordacademy.webserver.toDto
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow

private typealias IssuanceFlow = ObligationIssuanceFlow.Initiator
private typealias TransferFlow = ObligationTransferFlow.Initiator
private typealias SettlementFlow = ObligationSettlementFlow.Initiator
private typealias ExitFlow = ObligationExitFlow.Initiator

fun Route.obligationRoutes(rpc: CordaRPCOps) = route("/obligations") {

    get {
        val id = call.parameters["id"]

        val criteria = if (id != null) {
            val linearId = UniqueIdentifier.fromString(id)
            QueryCriteria.LinearStateQueryCriteria(
                linearId = listOf(linearId),
                status = Vault.StateStatus.ALL
            )
        } else {
            QueryCriteria.VaultQueryCriteria()
        }

        val obligations = rpc
            .vaultQueryByCriteria(criteria, ObligationState::class.java)
            .states
            .map { it.state.data.toDto() }

        call.respond(mapOf("obligations" to obligations))
    }

    post("/issue") {
        try {
            val dto = call.receive<ObligationIssuanceInputDto>()
            val obligor = rpc.resolveParty(dto.obligor)
            val borrowed = Amount.ofCurrency(dto.borrowed, dto.currency)
            val transaction = rpc.startTrackedFlow(::IssuanceFlow, obligor, borrowed).returnValue.getOrThrow()
            call.respond(ObligationTransactionOutputDto(transaction.id.toString()))
        } catch (ex: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("errorMessage" to ex.message))
        }
    }

    put("/transfer") {
        try {
            val dto = call.receive<ObligationTransferInputDto>()
            val linearId = UniqueIdentifier.fromString(dto.linearId!!)
            val obligee = rpc.resolveParty(dto.obligee)
            val transaction = rpc.startTrackedFlow(::TransferFlow, linearId, obligee).returnValue.getOrThrow()
            call.respond(ObligationTransactionOutputDto(transaction.id.toString()))
        } catch (ex: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("errorMessage" to ex.message))
        }
    }

    put("/settle") {
        try {
            val dto = call.receive<ObligationSettlementInputDto>()
            val linearId = UniqueIdentifier.fromString(dto.linearId!!)
            val settled = Amount.ofCurrency(dto.settled, dto.currency)
            val transaction = rpc.startTrackedFlow(::SettlementFlow, linearId, settled).returnValue.getOrThrow()
            call.respond(ObligationTransactionOutputDto(transaction.id.toString()))
        } catch (ex: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("errorMessage" to ex.message))
        }
    }

    delete("/exit") {
        try {
            val dto = call.receive<ObligationExitInputDto>()
            val linearId = UniqueIdentifier.fromString(dto.linearId!!)
            val transaction = rpc.startTrackedFlow(::ExitFlow, linearId).returnValue.getOrThrow()
            call.respond(ObligationTransactionOutputDto(transaction.id.toString()))
        } catch (ex: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("errorMessage" to ex.message))
        }
    }
}
package io.cordacademy.webserver

import io.cordacademy.obligation.contract.ObligationState
import io.cordacademy.webserver.areas.obligation.ObligationOutputDto
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import java.math.BigDecimal
import java.util.*

/**
 * Not the sort of thing you want in production code!
 * It's just here to remove the cruft so you can focus on more important stuff.
 */

fun Amount.Companion.ofCurrency(value: BigDecimal?, currency: String?) =
    Amount.fromDecimal(value!!, Currency.getInstance(currency))

fun CordaRPCOps.resolveParty(name: String?) = wellKnownPartyFromX500Name(CordaX500Name.parse(name!!))
    ?: throw IllegalArgumentException("Unable to resolve party from name: $name.")

fun ObligationState.toDto() = ObligationOutputDto(
    linearId = linearId.id.toString(),
    obligor = obligor.toString(),
    obligee = obligee.toString(),
    currency = borrowed.token.currencyCode,
    borrowed = borrowed.toDecimal(),
    settled = settled.toDecimal(),
    outstanding = outstanding.toDecimal()
)
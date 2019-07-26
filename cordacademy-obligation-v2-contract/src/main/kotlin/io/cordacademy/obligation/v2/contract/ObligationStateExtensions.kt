package io.cordacademy.obligation.v2.contract

import net.corda.core.contracts.Amount
import net.corda.core.identity.AbstractParty
import java.security.PublicKey
import java.util.*

/**
 * Gets a set of participant keys.
 */
val ObligationState.participantKeys: Set<PublicKey> get() = this.participants.map { it.owningKey }.toSet()

/**
 * Transfers an obligation to a new obligee.
 *
 * @param obligee The new obligee that the obligation will be transferred to.
 * @return Returns a new obligation containing the new obligee.
 */
fun ObligationState.transfer(obligee: AbstractParty) = copy(obligee = obligee)

/**
 * Partially or fully settles an obligation.
 *
 * @param amount The amount to settle against the obligation.
 * @return Returns a new obligation updating the settled amount.
 */
fun ObligationState.settle(amount: Amount<Currency>) = copy(settled = settled + amount)

/**
 * Defaults an obligation.
 *
 * @return Returns a new obligation updating the defaulted value to true.
 */
fun ObligationState.default() = copy(defaulted = true)

/**
 * Gets a hash code of the obligation excluding the obligee.
 *
 * @return Returns a hash code of the obligation excluding the obligee.
 */
internal fun ObligationState.hashWithoutObligee() = Objects.hash(obligor, borrowed, settled, linearId, defaulted)

/**
 * Gets a hash code of the obligation excluding the defaulted value.
 *
 * @return Returns a hash code of the obligation excluding the defaulted value.
 */
internal fun ObligationState.hashWithoutDefaulted() = Objects.hash(obligor, obligee, borrowed, settled, linearId)

/**
 * Gets a hash code of the obligation excluding the settled amount.
 *
 * @return Returns a hash code of the obligation excluding the settled amount.
 */
internal fun ObligationState.hashWithoutSettled() = Objects.hash(obligor, obligee, borrowed, linearId, defaulted)
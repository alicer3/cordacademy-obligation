package io.cordacademy.obligation.contract

import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

/**
 * Represents an obligation between an obligor and an obligee for an amount of currency.
 *
 * @param obligor The obligor (aka borrower or debtor) of the obligation.
 * @param obligee The obligee (aka lender or creditor) of the obligation.
 * @param borrowed The amount that was borrowed.
 * @param settled The amount that has been settled.
 * @param linearId The unique identifier of the obligation.
 */
@BelongsToContract(ObligationContract::class)
data class ObligationState(
    val obligor: AbstractParty,
    val obligee: AbstractParty,
    val borrowed: Amount<Currency>,
    val settled: Amount<Currency> = Amount.zero(borrowed.token),
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    init {
        check(borrowed.token == settled.token) {
            "Currency mismatch. Cannot settle ${borrowed.token} with ${settled.token}."
        }
    }

    /**
     * Gets the participants of this state.
     */
    override val participants: List<AbstractParty> get() = listOf(obligor, obligee)

    /**
     * Gets the amount that is outstanding on the obligation.
     */
    val outstanding: Amount<Currency> get() = borrowed - settled

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ObligationSchemaV1 -> ObligationSchemaV1.PersistentObligation(
                this.obligor.nameOrNull().toString(),
                this.obligee.nameOrNull().toString(),
                this.borrowed.toDecimal(),
                this.settled.toDecimal(),
                this.borrowed.token.currencyCode,
                this.linearId.id.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ObligationSchemaV1)

}
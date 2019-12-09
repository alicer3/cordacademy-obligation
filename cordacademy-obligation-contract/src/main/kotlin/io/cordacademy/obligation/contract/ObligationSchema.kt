package io.cordacademy.obligation.contract

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.math.BigDecimal
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object ObligationSchema

object ObligationSchemaV1: MappedSchema(
    schemaFamily = ObligationSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentObligation::class.java)) {
    @Entity
    @Table(name = "obligation_states")
    class PersistentObligation(
        @Column(name = "obligor")
        var obligor: String,
        @Column(name = "obligee")
        var obligee: String,
        @Column(name = "borrowed")
        var borrowed: BigDecimal,
        @Column(name = "settled")
        var settled: BigDecimal,
        @Column(name = "currency_code")
        var currency: String,
        @Column(name = "linear_id")
        var linearId: UUID
    ) : PersistentState()

}
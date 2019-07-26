package io.cordacademy.webserver.areas.obligation

import java.math.BigDecimal

data class ObligationOutputDto(
    val linearId: String,
    val obligor: String,
    val obligee: String,
    val currency: String,
    val borrowed: BigDecimal,
    val settled: BigDecimal,
    val outstanding: BigDecimal = borrowed - settled,
    val defaulted: Boolean?
)

data class ObligationTransactionOutputDto(
    val transactionId: String
)

data class ObligationIssuanceInputDto(
    val version: Int?,
    val obligor: String?,
    val borrowed: BigDecimal?,
    val currency: String?
)

data class ObligationTransferInputDto(
    val linearId: String?,
    val obligee: String?
)

data class ObligationSettlementInputDto(
    val linearId: String?,
    val settled: BigDecimal?,
    val currency: String?
)

data class ObligationExitInputDto(
    val linearId: String?
)

data class ObligationUpgradeDto(
    val linearId: String?
)

data class ObligationDefaultInputDto(
    val linearId: String?
)
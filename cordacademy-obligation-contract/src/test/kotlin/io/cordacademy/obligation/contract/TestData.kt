package io.cordacademy.obligation.contract

import io.cordacademy.test.IDENTITY_A
import io.cordacademy.test.IDENTITY_B
import io.cordacademy.test.IDENTITY_C
import net.corda.finance.POUNDS

val CORDAPPS = listOf("io.cordacademy.play.obligation.contract")
val CONTRACTS = listOf(ObligationContract.ID)

val OBLIGATION_STATE = ObligationState(IDENTITY_A.party, IDENTITY_B.party, 100.POUNDS)

val TRANSFERRED_OBLIGATION_STATE = OBLIGATION_STATE.transfer(IDENTITY_C.party)

val PARTIALLY_SETTLED_OBLIGATION_STATE = OBLIGATION_STATE.settle(50.POUNDS)

val FULLY_SETTLED_OBLIGATION_STATE = OBLIGATION_STATE.settle(100.POUNDS)

val DEFAULTED_OBLIGATION_STATE = OBLIGATION_STATE.default()
package io.cordacademy.obligation.v1.contract

import io.cordacademy.test.ContractTest
import io.cordacademy.test.IDENTITY_A
import io.cordacademy.test.IDENTITY_B
import net.corda.finance.POUNDS
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test

class ObligationSettlementContractTests : ContractTest(
    CORDAPPS,
    CONTRACTS
) {

    @Test
    fun `When settling an obligation, the transaction must include the Settle command`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    PARTIALLY_SETTLED_OBLIGATION_STATE
                )
                fails()
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Settle())
                verifies()
            }
        }
    }

    @Test
    fun `When settling an obligation, only one input state must be consumed`() {
        services.ledger {
            transaction {
                output(
                    ObligationContract.ID,
                    PARTIALLY_SETTLED_OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Settle())
                failsWith(ObligationContract.Settle.CONTRACT_RULE_INPUTS)
            }
        }
    }

    @Test
    fun `When settling an obligation, only one output state must be created`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Settle())
                failsWith(ObligationContract.Settle.CONTRACT_RULE_OUTPUTS)
            }
        }
    }

    @Test
    fun `When settling an obligation, the settled amount must not be greater than the borrowed amount`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(ObligationContract.ID, OBLIGATION_STATE.settle(200.POUNDS))
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Settle())
                failsWith(ObligationContract.Settle.CONTRACT_RULE_OUTPUTS_AMOUNT)
            }
        }
    }

    @Test
    fun `When settling an obligation, only the settled amount must change`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(ObligationContract.ID, TRANSFERRED_OBLIGATION_STATE.settle(50.POUNDS))
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Settle())
                failsWith(ObligationContract.Settle.CONTRACT_RULE_ONLY_SETTLED_CHANGED)
            }
        }
    }

    @Test
    fun `When settling an obligation, all participants must sign the transaction (obligor must sign)`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    PARTIALLY_SETTLED_OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_B), ObligationContract.Settle())
                failsWith(ObligationContract.Settle.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `When settling an obligation, all participants must sign the transaction (obligee must sign)`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    PARTIALLY_SETTLED_OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_A), ObligationContract.Settle())
                failsWith(ObligationContract.Settle.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
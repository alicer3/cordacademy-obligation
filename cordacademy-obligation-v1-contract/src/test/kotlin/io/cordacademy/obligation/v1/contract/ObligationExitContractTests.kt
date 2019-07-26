package io.cordacademy.obligation.v1.contract

import io.cordacademy.test.ContractTest
import io.cordacademy.test.IDENTITY_A
import io.cordacademy.test.IDENTITY_B
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test

class ObligationExitContractTests : ContractTest(
    CORDAPPS,
    CONTRACTS
) {

    @Test
    fun `When exiting an obligation, the transaction must include the Exit command`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    FULLY_SETTLED_OBLIGATION_STATE
                )
                fails()
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Exit())
                verifies()
            }
        }
    }

    @Test
    fun `When exiting an obligation, only one input state must be consumed`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    FULLY_SETTLED_OBLIGATION_STATE
                )
                input(
                    ObligationContract.ID,
                    FULLY_SETTLED_OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Exit())
                failsWith(ObligationContract.Exit.CONTRACT_RULE_INPUTS)
            }
        }
    }

    @Test
    fun `When exiting an obligation, zero output states must be created`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    FULLY_SETTLED_OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    FULLY_SETTLED_OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Exit())
                failsWith(ObligationContract.Exit.CONTRACT_RULE_OUTPUTS)
            }
        }
    }

    @Test
    fun `When exiting an obligation, the input state must be fully settled`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    PARTIALLY_SETTLED_OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Exit())
                failsWith(ObligationContract.Exit.CONTRACT_RULE_INPUT_SETTLED)
            }
        }
    }

    @Test
    fun `When exiting an obligation, all participants must sign the transaction (obligor must sign)`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    FULLY_SETTLED_OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_B), ObligationContract.Exit())
                failsWith(ObligationContract.Exit.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `When exiting an obligation, all participants must sign the transaction (obligee must sign)`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    FULLY_SETTLED_OBLIGATION_STATE
                )
                command(keysOf(IDENTITY_A), ObligationContract.Exit())
                failsWith(ObligationContract.Exit.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
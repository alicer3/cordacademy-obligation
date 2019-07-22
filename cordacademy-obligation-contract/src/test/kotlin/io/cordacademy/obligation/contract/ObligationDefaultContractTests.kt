package io.cordacademy.obligation.contract

import io.cordacademy.test.ContractTest
import io.cordacademy.test.IDENTITY_A
import io.cordacademy.test.IDENTITY_B
import net.corda.finance.POUNDS
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test

class ObligationDefaultContractTests : ContractTest(CORDAPPS, CONTRACTS) {

    @Test
    fun `When defaulting an obligation, the transaction must include the Default command`() {
        services.ledger {
            transaction {
                input(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, DEFAULTED_OBLIGATION_STATE)
                fails()
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Default())
                verifies()
            }
        }
    }

    @Test
    fun `When defaulting an obligation, only one input state must be consumed`() {
        services.ledger {
            transaction {
                input(ObligationContract.ID, OBLIGATION_STATE)
                input(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, DEFAULTED_OBLIGATION_STATE)
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Default())
                failsWith(ObligationContract.Default.CONTRACT_RULE_INPUTS)
            }
        }
    }

    @Test
    fun `When defaulting an obligation, only one output state must be created`() {
        services.ledger {
            transaction {
                input(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, DEFAULTED_OBLIGATION_STATE)
                output(ObligationContract.ID, DEFAULTED_OBLIGATION_STATE)
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Default())
                failsWith(ObligationContract.Default.CONTRACT_RULE_OUTPUTS)
            }
        }
    }

    @Test
    fun `When defaulting an obligation, the default value must be set to true`() {
        services.ledger {
            transaction {
                input(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, DEFAULTED_OBLIGATION_STATE.copy(defaulted = false))
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Default())
                failsWith(ObligationContract.Default.CONTRACT_RULE_DEFAULTED_CHANGED)
            }
        }
    }

    @Test
    fun `When defaulting an obligation, only the defaulted value must change`() {
        services.ledger {
            transaction {
                input(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, DEFAULTED_OBLIGATION_STATE.copy(borrowed = 1.POUNDS))
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Default())
                failsWith(ObligationContract.Default.CONTRACT_RULE_ONLY_DEFAULTED_CHANGED)
            }
        }
    }

    @Test
    fun `When defaulting an obligation, all participants must sign the transaction (obligor must sign)`() {
        services.ledger {
            transaction {
                input(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, DEFAULTED_OBLIGATION_STATE)
                command(keysOf(IDENTITY_B), ObligationContract.Default())
                failsWith(ObligationContract.Default.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `When defaulting an obligation, all participants must sign the transaction (obligee must sign)`() {
        services.ledger {
            transaction {
                input(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, DEFAULTED_OBLIGATION_STATE)
                command(keysOf(IDENTITY_A), ObligationContract.Default())
                failsWith(ObligationContract.Default.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
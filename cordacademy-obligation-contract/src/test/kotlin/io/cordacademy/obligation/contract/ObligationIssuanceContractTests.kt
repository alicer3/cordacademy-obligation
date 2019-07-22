package io.cordacademy.obligation.contract

import io.cordacademy.test.ContractTest
import io.cordacademy.test.IDENTITY_A
import io.cordacademy.test.IDENTITY_B
import net.corda.finance.POUNDS
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test

class ObligationIssuanceContractTests : ContractTest(CORDAPPS, CONTRACTS) {

    @Test
    fun `When issuing an obligation, the transaction must include the Issue command`() {
        services.ledger {
            transaction {
                output(ObligationContract.ID, OBLIGATION_STATE)
                fails()
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `When issuing an obligation, zero input states must be consumed`() {
        services.ledger {
            transaction {
                input(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, OBLIGATION_STATE)
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Issue())
                failsWith(ObligationContract.Issue.CONTRACT_RULE_INPUTS)
            }
        }
    }

    @Test
    fun `When issuing an obligation, only one output state must be created`() {
        services.ledger {
            transaction {
                output(ObligationContract.ID, OBLIGATION_STATE)
                output(ObligationContract.ID, OBLIGATION_STATE)
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Issue())
                failsWith(ObligationContract.Issue.CONTRACT_RULE_OUTPUTS)
            }
        }
    }

    @Test
    fun `When issuing an obligation, the output state must be issued with a positive amount`() {
        services.ledger {
            transaction {
                output(ObligationContract.ID, OBLIGATION_STATE.copy(borrowed = 0.POUNDS))
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Issue())
                failsWith(ObligationContract.Issue.CONTRACT_RULE_AMOUNT_IS_POSITIVE)
            }
        }
    }

    @Test
    fun `When issuing an obligation, the obligor and obligee must not be the same identity`() {
        services.ledger {
            transaction {
                output(ObligationContract.ID, OBLIGATION_STATE.copy(obligee = IDENTITY_A.party))
                command(keysOf(IDENTITY_A, IDENTITY_B), ObligationContract.Issue())
                failsWith(ObligationContract.Issue.CONTRACT_RULE_OBLIGEE_ISNT_OBLIGOR)
            }
        }
    }

    @Test
    fun `When issuing an obligation, all participants must sign the transaction (obligor must sign)`() {
        services.ledger {
            transaction {
                output(ObligationContract.ID, OBLIGATION_STATE)
                command(keysOf(IDENTITY_B), ObligationContract.Issue())
                failsWith(ObligationContract.Issue.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `When issuing an obligation, all participants must sign the transaction (obligee must sign)`() {
        services.ledger {
            transaction {
                output(ObligationContract.ID, OBLIGATION_STATE)
                command(keysOf(IDENTITY_A), ObligationContract.Issue())
                failsWith(ObligationContract.Issue.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
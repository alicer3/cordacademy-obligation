package io.cordacademy.obligation.contract

import io.cordacademy.test.ContractTest
import io.cordacademy.test.IDENTITY_A
import io.cordacademy.test.IDENTITY_B
import io.cordacademy.test.IDENTITY_C
import net.corda.finance.POUNDS
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test

class ObligationTransferContractTests : ContractTest(CORDAPPS, CONTRACTS) {

    @Test
    fun `When transferring an obligation, the transaction must include the Transfer command`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    TRANSFERRED_OBLIGATION_STATE
                )
                fails()
                command(
                    keysOf(IDENTITY_A, IDENTITY_B, IDENTITY_C),
                    ObligationContract.Transfer()
                )
                verifies()
            }
        }
    }

    @Test
    fun `When transferring an obligation, only one input state must be consumed`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    TRANSFERRED_OBLIGATION_STATE
                )
                command(
                    keysOf(IDENTITY_A, IDENTITY_B, IDENTITY_C),
                    ObligationContract.Transfer()
                )
                failsWith(ObligationContract.Transfer.CONTRACT_RULE_INPUTS)
            }
        }
    }

    @Test
    fun `When transferring an obligation, only one output state must be created`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    TRANSFERRED_OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    TRANSFERRED_OBLIGATION_STATE
                )
                command(
                    keysOf(IDENTITY_A, IDENTITY_B, IDENTITY_C),
                    ObligationContract.Transfer()
                )
                failsWith(ObligationContract.Transfer.CONTRACT_RULE_OUTPUTS)
            }
        }
    }

    @Test
    fun `When transferring an obligation, the obligee must change`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                command(
                    keysOf(IDENTITY_A, IDENTITY_B, IDENTITY_C),
                    ObligationContract.Transfer()
                )
                failsWith(ObligationContract.Transfer.CONTRACT_RULE_OBLIGEE_CHANGED)
            }
        }
    }

    @Test
    fun `When transferring an obligation, only the obligee must change`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(ObligationContract.ID, TRANSFERRED_OBLIGATION_STATE.copy(borrowed = 1.POUNDS))
                command(
                    keysOf(IDENTITY_A, IDENTITY_B, IDENTITY_C),
                    ObligationContract.Transfer()
                )
                failsWith(ObligationContract.Transfer.CONTRACT_RULE_ONLY_OBLIGEE_CHANGED)
            }
        }
    }

    @Test
    fun `When transferring an obligation, the obligor and obligee must not be the same identity`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(ObligationContract.ID, OBLIGATION_STATE.transfer(IDENTITY_A.party))
                command(
                    keysOf(IDENTITY_A, IDENTITY_B, IDENTITY_C),
                    ObligationContract.Transfer()
                )
                failsWith(ObligationContract.Transfer.CONTRACT_RULE_OBLIGEE_ISNT_OBLIGOR)
            }
        }
    }

    @Test
    fun `When transferring an obligation, the obligor, old obligee and new obligee must sign the transaction (obligor must sign)`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    TRANSFERRED_OBLIGATION_STATE
                )
                command(
                    keysOf(IDENTITY_B, IDENTITY_C),
                    ObligationContract.Transfer()
                )
                failsWith(ObligationContract.Transfer.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `When transferring an obligation, the obligor, old obligee and new obligee must sign the transaction (new obligee must sign)`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    TRANSFERRED_OBLIGATION_STATE
                )
                command(
                    keysOf(IDENTITY_A, IDENTITY_B),
                    ObligationContract.Transfer()
                )
                failsWith(ObligationContract.Transfer.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `When transferring an obligation, the obligor, old obligee and new obligee must sign the transaction (old obligee must sign)`() {
        services.ledger {
            transaction {
                input(
                    ObligationContract.ID,
                    OBLIGATION_STATE
                )
                output(
                    ObligationContract.ID,
                    TRANSFERRED_OBLIGATION_STATE
                )
                command(
                    keysOf(IDENTITY_A, IDENTITY_C),
                    ObligationContract.Transfer()
                )
                failsWith(ObligationContract.Transfer.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
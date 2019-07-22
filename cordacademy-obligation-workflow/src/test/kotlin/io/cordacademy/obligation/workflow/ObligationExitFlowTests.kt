package io.cordacademy.obligation.workflow

import io.cordacademy.obligation.contract.ObligationState
import io.cordacademy.test.FlowTest
import net.corda.finance.POUNDS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class ObligationExitFlowTests : FlowTest("io.cordacademy.obligation.contract") {

    override fun initialize() {
        listOf(nodeA, nodeB).forEach {
            it.registerInitiatedFlow<ObligationIssuanceFlow.Responder>()
            it.registerInitiatedFlow<ObligationSettlementFlow.Responder>()
            it.registerInitiatedFlow<ObligationExitFlow.Responder>()
        }
    }

    @Test
    fun `ObligationExitFlow should be signed by the initiator`() {

        // Arrange
        val initiator = nodeA
        val issuedLinearId = issue(nodeA, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId
        val linearId = settle(nodeB, issuedLinearId, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = exit(initiator, linearId)

        // Assert
        transaction.verifySignaturesExcept(partyB.owningKey)
    }

    @Test
    fun `ObligationExitFlow should be signed by the acceptor`() {

        // Arrange
        val initiator = nodeA
        val issuedLinearId = issue(nodeA, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId
        val linearId = settle(nodeB, issuedLinearId, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = exit(initiator, linearId)

        // Assert
        transaction.verifySignaturesExcept(partyA.owningKey)
    }

    @Test
    fun `ObligationExitFlow should record a transaction for all participants`() {

        // Arrange
        val initiator = nodeA
        val issuedLinearId = issue(nodeA, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId
        val linearId = settle(nodeB, issuedLinearId, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = exit(initiator, linearId)

        // Assert
        listOf(nodeA, nodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Could not find a recorded transaction with id ${transaction.id}.")

                assertEquals(transaction, recordedTransaction)
            }
        }
    }

    @Test
    fun `ObligationExitFlow should record a transaction with one input and zero outputs`() {

        // Arrange
        val initiator = nodeA
        val issuedLinearId = issue(nodeA, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId
        val linearId = settle(nodeB, issuedLinearId, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = exit(initiator, linearId)

        // Assert
        listOf(nodeA, nodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Could not find a recorded transaction with id ${transaction.id}.")

                assert(recordedTransaction.tx.inputs.size == 1)
                assert(recordedTransaction.tx.outputs.isEmpty())
            }
        }
    }
}
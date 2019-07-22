package io.cordacademy.obligation.workflow

import io.cordacademy.obligation.contract.ObligationState
import io.cordacademy.test.FlowTest
import net.corda.finance.POUNDS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class ObligationSettlementFlowTests : FlowTest("io.cordacademy.obligation.contract") {

    override fun initialize() {
        listOf(nodeA, nodeB, nodeC).forEach {
            it.registerInitiatedFlow<ObligationIssuanceFlow.Responder>()
            it.registerInitiatedFlow<ObligationSettlementFlow.Responder>()
        }
    }

    @Test
    fun `ObligationSettlementFlow should be signed by the initiator`() {

        // Arrange
        val initiator = nodeB
        val linearId = issue(nodeA, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = settle(initiator, linearId, 50.POUNDS)

        // Assert
        transaction.verifySignaturesExcept(partyA.owningKey)
    }

    @Test
    fun `ObligationSettlementFlow should be signed by the acceptor`() {

        // Arrange
        val initiator = nodeB
        val linearId = issue(nodeA, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = settle(initiator, linearId, 50.POUNDS)

        // Assert
        transaction.verifySignaturesExcept(partyB.owningKey)
    }

    @Test
    fun `ObligationSettlementFlow should record a transaction for all participants`() {

        // Arrange
        val initiator = nodeB
        val linearId = issue(nodeA, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = settle(initiator, linearId, 50.POUNDS)

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
    fun `ObligationSettlementFlow should record a transaction with one input and one output`() {

        // Arrange
        val initiator = nodeB
        val linearId = issue(nodeA, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = settle(initiator, linearId, 50.POUNDS)

        // Assert
        listOf(nodeA, nodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Could not find a recorded transaction with id ${transaction.id}.")

                assert(recordedTransaction.tx.inputs.size == 1)
                assert(recordedTransaction.tx.outputs.size == 1)
            }
        }
    }
}
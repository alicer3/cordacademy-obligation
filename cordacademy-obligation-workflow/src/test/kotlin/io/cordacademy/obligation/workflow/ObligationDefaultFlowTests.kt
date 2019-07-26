package io.cordacademy.obligation.workflow

import io.cordacademy.obligation.v2.contract.ObligationStateV2
import io.cordacademy.test.FlowTest
import net.corda.finance.POUNDS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class ObligationDefaultFlowTests : FlowTest(*CORDAPPS) {

    override fun initialize() {
        listOf(nodeA, nodeB, nodeC).forEach {
            it.registerInitiatedFlow<ObligationIssuanceFlow.Responder>()
            it.registerInitiatedFlow<ObligationDefaultFlow.Responder>()
        }
    }

    @Test
    fun `ObligationDefaultFlow should be signed by the initiator`() {

        // Arrange
        val initiator = nodeB
        val linearId = issue(nodeA, partyB, 100.POUNDS, version = 2)
            .tx.outputsOfType<ObligationStateV2>().single().linearId

        // Act
        val transaction = default(initiator, linearId)

        // Assert
        transaction.verifySignaturesExcept(partyA.owningKey)
    }

    @Test
    fun `ObligationDefaultFlow should be signed by the acceptor`() {

        // Arrange
        val initiator = nodeB
        val linearId = issue(nodeA, partyB, 100.POUNDS, version = 2)
            .tx.outputsOfType<ObligationStateV2>().single().linearId

        // Act
        val transaction = default(initiator, linearId)

        // Assert
        transaction.verifySignaturesExcept(partyB.owningKey)
    }

    @Test
    fun `ObligationDefaultFlow should record a transaction for all participants`() {

        // Arrange
        val initiator = nodeB
        val linearId = issue(nodeA, partyB, 100.POUNDS, version = 2)
            .tx.outputsOfType<ObligationStateV2>().single().linearId

        // Act
        val transaction = default(initiator, linearId)

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
    fun `ObligationDefaultFlow should record a transaction with one input and one output`() {

        // Arrange
        val initiator = nodeB
        val linearId = issue(nodeA, partyB, 100.POUNDS, version = 2)
            .tx.outputsOfType<ObligationStateV2>().single().linearId

        // Act
        val transaction = default(initiator, linearId)

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
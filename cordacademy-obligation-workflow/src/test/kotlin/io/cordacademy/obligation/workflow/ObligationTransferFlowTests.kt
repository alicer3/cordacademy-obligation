package io.cordacademy.obligation.workflow

import io.cordacademy.obligation.contract.ObligationState
import io.cordacademy.test.FlowTest
import net.corda.finance.POUNDS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class ObligationTransferFlowTests : FlowTest("io.cordacademy.obligation.contract") {

    override fun initialize() {
        listOf(nodeA, nodeB, nodeC).forEach {
            it.registerInitiatedFlow<ObligationIssuanceFlow.Responder>()
            it.registerInitiatedFlow<ObligationTransferFlow.Responder>()
        }
    }

    @Test
    fun `ObligationTransferFlow should be signed by the initiator`() {

        // Arrange
        val initiator = nodeA
        val obligee = partyC
        val linearId = issue(initiator, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = transfer(initiator, linearId, obligee)

        // Assert
        transaction.verifySignaturesExcept(partyA.owningKey, partyC.owningKey)
    }

    @Test
    fun `ObligationTransferFlow should be signed by the acceptor`() {

        // Arrange
        val initiator = nodeA
        val obligee = partyC
        val linearId = issue(initiator, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = transfer(initiator, linearId, obligee)

        // Assert
        transaction.verifySignaturesExcept(partyB.owningKey)
    }

    @Test
    fun `ObligationTransferFlow should record a transaction for all participants`() {

        // Arrange
        val initiator = nodeA
        val obligee = partyC
        val linearId = issue(initiator, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = transfer(initiator, linearId, obligee)

        // Assert
        listOf(nodeA, nodeB, nodeC).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Could not find a recorded transaction with id ${transaction.id}.")

                assertEquals(transaction, recordedTransaction)
            }
        }
    }

    @Test
    fun `ObligationTransferFlow should record a transaction with one input and one output`() {

        // Arrange
        val initiator = nodeA
        val obligee = partyC
        val linearId = issue(initiator, partyB, 100.POUNDS)
            .tx.outputsOfType<ObligationState>().single().linearId

        // Act
        val transaction = transfer(initiator, linearId, obligee)

        // Assert
        listOf(nodeA, nodeB, nodeC).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Could not find a recorded transaction with id ${transaction.id}.")

                assert(recordedTransaction.tx.inputs.size == 1)
                assert(recordedTransaction.tx.outputs.size == 1)
            }
        }
    }
}
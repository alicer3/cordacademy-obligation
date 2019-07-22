package io.cordacademy.obligation.workflow

import io.cordacademy.test.FlowTest
import net.corda.finance.POUNDS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class ObligationIssuanceFlowTests : FlowTest("io.cordacademy.obligation.contract") {

    override fun initialize() {
        nodeB.registerInitiatedFlow<ObligationIssuanceFlow.Responder>()
    }

    @Test
    fun `ObligationIssuanceFlow should be signed by the initiator`() {

        // Arrange
        val initiator = nodeA
        val obligor = partyB
        val borrowed = 100.POUNDS

        // Act
        val transaction = issue(initiator, obligor, borrowed)

        // Assert
        transaction.verifySignaturesExcept(partyB.owningKey)
    }

    @Test
    fun `ObligationIssuanceFlow should be signed by the acceptor`() {

        // Arrange
        val initiator = nodeA
        val obligor = partyB
        val borrowed = 100.POUNDS

        // Act
        val transaction = issue(initiator, obligor, borrowed)

        // Assert
        transaction.verifySignaturesExcept(partyA.owningKey)
    }

    @Test
    fun `ObligationIssuanceFlow should record a transaction for all participants`() {

        // Arrange
        val initiator = nodeA
        val obligor = partyB
        val borrowed = 100.POUNDS

        // Act
        val transaction = issue(initiator, obligor, borrowed)

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
    fun `ObligationIssuanceFlow should record a transaction with zero inputs and only one output`() {

        // Arrange
        val initiator = nodeA
        val obligor = partyB
        val borrowed = 100.POUNDS

        // Act
        val transaction = issue(initiator, obligor, borrowed)

        // Assert
        listOf(nodeA, nodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Could not find a recorded transaction with id ${transaction.id}.")

                assert(recordedTransaction.tx.inputs.isEmpty())
                assert(recordedTransaction.tx.outputs.size == 1)
            }
        }
    }
}
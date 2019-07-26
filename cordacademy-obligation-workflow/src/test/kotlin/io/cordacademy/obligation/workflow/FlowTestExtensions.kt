package io.cordacademy.obligation.workflow

import io.cordacademy.test.FlowTest
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.StartedMockNode
import java.time.Duration
import java.util.*

val CORDAPPS = arrayOf(
    "io.cordacademy.obligation.v1.contract",
    "io.cordacademy.obligation.v2.contract"
)

fun FlowTest.issue(
    initiator: StartedMockNode,
    obligor: Party,
    borrowed: Amount<Currency>,
    timeout: Duration = Duration.ofSeconds(30),
    version: Int = 1
): SignedTransaction {
    val flow = ObligationIssuanceFlow.Initiator(obligor, borrowed, version)
    return run { initiator.startFlow(flow) }.getOrThrow(timeout)
}

fun FlowTest.transfer(
    initiator: StartedMockNode,
    linearId: UniqueIdentifier,
    obligee: Party,
    timeout: Duration = Duration.ofSeconds(30)
): SignedTransaction {
    val flow = ObligationTransferFlow.Initiator(linearId, obligee)
    return run { initiator.startFlow(flow) }.getOrThrow(timeout)
}

fun FlowTest.settle(
    initiator: StartedMockNode,
    linearId: UniqueIdentifier,
    settled: Amount<Currency>,
    timeout: Duration = Duration.ofSeconds(30)
): SignedTransaction {
    val flow = ObligationSettlementFlow.Initiator(linearId, settled)
    return run { initiator.startFlow(flow) }.getOrThrow(timeout)
}

fun FlowTest.exit(
    initiator: StartedMockNode,
    linearId: UniqueIdentifier,
    timeout: Duration = Duration.ofSeconds(30)
): SignedTransaction {
    val flow = ObligationExitFlow.Initiator(linearId)
    return run { initiator.startFlow(flow) }.getOrThrow(timeout)
}

fun FlowTest.default(
    initiator: StartedMockNode,
    linearId: UniqueIdentifier,
    timeout: Duration = Duration.ofSeconds(30)
): SignedTransaction {
    val flow = ObligationDefaultFlow.Initiator(linearId)
    return run { initiator.startFlow(flow) }.getOrThrow(timeout)
}
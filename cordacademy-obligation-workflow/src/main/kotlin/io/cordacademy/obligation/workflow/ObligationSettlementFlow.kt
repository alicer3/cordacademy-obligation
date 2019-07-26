package io.cordacademy.obligation.workflow

import co.paralleluniverse.fibers.Suspendable
import io.cordacademy.obligation.v1.contract.ObligationContract
import io.cordacademy.obligation.v1.contract.participantKeys
import io.cordacademy.obligation.v1.contract.settle
import io.cordacademy.obligation.workflow.common.InitiatorFlowLogic
import io.cordacademy.obligation.workflow.common.ResponderFlowLogic
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * Represents an initiator/responder flow pair for obligation settlement.
 */
object ObligationSettlementFlow {

    /**
     * Specifies flow version 1
     */
    private const val FLOW_VERSION_1 = 1

    /**
     * Settles an obligation either partially or fully by the specified amount of currency.
     *
     * @param linearId The unique identifier of the obligation to settle.
     * @param settled The amount of currency by which to settle the obligation.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val linearId: UniqueIdentifier,
        private val settled: Amount<Currency>
    ) : InitiatorFlowLogic() {

        @Suspendable
        override fun call(): SignedTransaction {

            setStep(INITIALIZING_FLOW)
            val consumedObligation = findV1ObligationByLinearId(linearId)
            val settledObligation = consumedObligation.state.data.settle(settled)
            val sessions = flowSessionsFor(settledObligation.participants - serviceHub.myInfo.legalIdentities)
            val ourSigningKey = settledObligation.obligor.owningKey

            setStep(CREATING_TRANSACTION)
            val transaction = with(TransactionBuilder(firstNotary)) {
                addInputState(consumedObligation)
                addOutputState(settledObligation, ObligationContract.ID)
                addCommand(ObligationContract.Settle(), settledObligation.participantKeys.toList())
            }

            setStep(VERIFYING_TRANSACTION)
            transaction.verify(serviceHub)

            setStep(SIGNING_TRANSACTION)
            val partiallySignedTransaction = serviceHub.signInitialTransaction(transaction, ourSigningKey)

            setStep(COUNTERSIGNING_TRANSACTION)
            val fullySignedTransaction = subFlow(
                CollectSignaturesFlow(
                    partiallySignedTx = partiallySignedTransaction,
                    sessionsToCollectFrom = sessions,
                    myOptionalKeys = listOf(ourSigningKey),
                    progressTracker = COUNTERSIGNING_TRANSACTION.childProgressTracker()!!
                )
            )

            setStep(FINALIZING_TRANSACTION)
            return subFlow(
                FinalityFlow(
                    transaction = fullySignedTransaction,
                    sessions = sessions,
                    progressTracker = FINALIZING_TRANSACTION.childProgressTracker()!!
                )
            )
        }
    }

    /**
     * Responds to settlement of an obligation.
     *
     * @param session A session with the initiating counter-party.
     */
    @InitiatedBy(Initiator::class)
    class Responder(session: FlowSession) : ResponderFlowLogic(session)
}
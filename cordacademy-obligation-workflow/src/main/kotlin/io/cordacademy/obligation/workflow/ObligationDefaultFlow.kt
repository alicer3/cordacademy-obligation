package io.cordacademy.obligation.workflow

import co.paralleluniverse.fibers.Suspendable
import io.cordacademy.obligation.contract.ObligationContract
import io.cordacademy.obligation.contract.default
import io.cordacademy.obligation.workflow.common.InitiatorFlowLogic
import io.cordacademy.obligation.workflow.common.ResponderFlowLogic
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Represents an initiator/responder flow pair for obligation settlement.
 */
object ObligationDefaultFlow {

    /**
     * Specifies flow version 1
     */
    private const val FLOW_VERSION_1 = 1

    /**
     * Defaults on an obligation.
     *
     * @param linearId The unique identifier of the obligation to settle.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val linearId: UniqueIdentifier
    ) : InitiatorFlowLogic() {

        @Suspendable
        override fun call(): SignedTransaction {

            setStep(INITIALIZING_FLOW)
            val consumedObligation = findObligationByLinearId(linearId)
            val defaultedObligation = consumedObligation.state.data.default()
            val sessions = flowSessionsFor(defaultedObligation.participants - serviceHub.myInfo.legalIdentities)
            val ourSigningKey = consumedObligation.state.data.obligor.owningKey

            setStep(CREATING_TRANSACTION)
            val transaction = with(TransactionBuilder(firstNotary)) {
                addInputState(consumedObligation)
                addOutputState(defaultedObligation, ObligationContract.ID)
                addCommand(ObligationContract.Default(), defaultedObligation.participants.map { it.owningKey })
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
     * Responds to defaulting of an obligation.
     *
     * @param session A session with the initiating counter-party.
     */
    @InitiatedBy(Initiator::class)
    class Responder(session: FlowSession) : ResponderFlowLogic(session)
}
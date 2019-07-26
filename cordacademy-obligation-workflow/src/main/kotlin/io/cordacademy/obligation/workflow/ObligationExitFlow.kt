package io.cordacademy.obligation.workflow

import co.paralleluniverse.fibers.Suspendable
import io.cordacademy.obligation.v1.contract.ObligationContract
import io.cordacademy.obligation.workflow.common.InitiatorFlowLogic
import io.cordacademy.obligation.workflow.common.ResponderFlowLogic
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Represents an initiator/responder flow pair for obligation exit.
 */
object ObligationExitFlow {

    /**
     * Specifies flow version 1
     */
    private const val FLOW_VERSION_1 = 1


    /**
     * Exits a fully settled obligation.
     *
     * @param linearId The unique identifier of the obligation to exit.
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
            val sessions = flowSessionsFor(
                consumedObligation.state.data.participants - serviceHub.myInfo.legalIdentities
            )
            val ourSigningKey = serviceHub.myInfo.legalIdentities.first().owningKey

            setStep(CREATING_TRANSACTION)
            val transaction = with(TransactionBuilder(firstNotary)) {
                addInputState(consumedObligation)
                addCommand(ObligationContract.Exit(), consumedObligation.state.data.participants.map { it.owningKey })
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
     * Responds to exit of an obligation.
     *
     * @param session A session with the initiating counter-party.
     */
    @InitiatedBy(ObligationExitFlow.Initiator::class)
    class Responder(session: FlowSession) : ResponderFlowLogic(session)
}
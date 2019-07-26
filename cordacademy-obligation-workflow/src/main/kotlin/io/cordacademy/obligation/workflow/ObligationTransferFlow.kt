package io.cordacademy.obligation.workflow

import co.paralleluniverse.fibers.Suspendable
import io.cordacademy.obligation.v1.contract.ObligationContract
import io.cordacademy.obligation.v1.contract.transfer
import io.cordacademy.obligation.workflow.common.InitiatorFlowLogic
import io.cordacademy.obligation.workflow.common.ResponderFlowLogic
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Represents an initiator/responder flow pair for obligation transfer.
 */
object ObligationTransferFlow {

    /**
     * Specifies flow version 1
     */
    private const val FLOW_VERSION_1 = 1


    /**
     * Transfers an obligation to the specified obligee.
     *
     * @param linearId The unique identifier of the obligation to transfer.
     * @param obligee The specified obligee who will receive the transferred obligation.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val linearId: UniqueIdentifier,
        private val obligee: Party
    ) : InitiatorFlowLogic() {

        @Suspendable
        override fun call(): SignedTransaction {

            setStep(INITIALIZING_FLOW)
            val consumedObligation = findV1ObligationByLinearId(linearId)
            val transferredObligation = consumedObligation.state.data.transfer(obligee)
            val participants = consumedObligation.state.data.participants union transferredObligation.participants
            val sessions = flowSessionsFor(participants - serviceHub.myInfo.legalIdentities)
            val ourSigningKey = consumedObligation.state.data.obligee.owningKey

            setStep(CREATING_TRANSACTION)
            val transaction = with(TransactionBuilder(firstNotary)) {
                addInputState(consumedObligation)
                addOutputState(transferredObligation, ObligationContract.ID)
                addCommand(ObligationContract.Transfer(), participants.map { it.owningKey })
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
     * Responds to transfer of an obligation.
     *
     * @param session A session with the initiating counter-party.
     */
    @InitiatedBy(Initiator::class)
    class Responder(session: FlowSession) : ResponderFlowLogic(session)
}
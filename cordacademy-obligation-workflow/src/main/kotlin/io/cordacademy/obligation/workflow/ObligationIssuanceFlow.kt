package io.cordacademy.obligation.workflow

import co.paralleluniverse.fibers.Suspendable
import io.cordacademy.obligation.contract.ObligationContract
import io.cordacademy.obligation.contract.ObligationState
import io.cordacademy.obligation.contract.participantKeys
import io.cordacademy.obligation.workflow.common.InitiatorFlowLogic
import io.cordacademy.obligation.workflow.common.ResponderFlowLogic
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * Represents an initiator/responder flow pair for obligation issuance.
 */
object ObligationIssuanceFlow {

    /**
     * Specifies flow version 1
     */
    private const val FLOW_VERSION_1 = 1

    /**
     * Initiates issuance of an obligation.
     *
     * @param obligor The obligor with whom to issue an obligation.
     * @param borrowed The amount of currency for which the obligation is to be issued.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val obligor: Party,
        private val borrowed: Amount<Currency>
    ) : InitiatorFlowLogic() {

        @Suspendable
        override fun call(): SignedTransaction {

            setStep(INITIALIZING_FLOW)
            val obligee = serviceHub.myInfo.legalIdentities.first()
            val obligorFlowSession = initiateFlow(obligor)
            val obligation = ObligationState(obligor, obligee, borrowed)
            val ourSigningKey = obligation.obligee.owningKey

            setStep(CREATING_TRANSACTION)
            val transaction = with(TransactionBuilder(firstNotary)) {
                addOutputState(obligation, ObligationContract.ID)
                addCommand(ObligationContract.Issue(), obligation.participantKeys.toList())
            }

            setStep(VERIFYING_TRANSACTION)
            transaction.verify(serviceHub)

            setStep(SIGNING_TRANSACTION)
            val partiallySignedTransaction = serviceHub.signInitialTransaction(transaction, ourSigningKey)

            setStep(COUNTERSIGNING_TRANSACTION)
            val fullySignedTransaction = subFlow(
                CollectSignaturesFlow(
                    partiallySignedTx = partiallySignedTransaction,
                    sessionsToCollectFrom = setOf(obligorFlowSession),
                    myOptionalKeys = listOf(ourSigningKey),
                    progressTracker = COUNTERSIGNING_TRANSACTION.childProgressTracker()!!
                )
            )

            setStep(FINALIZING_TRANSACTION)
            return subFlow(
                FinalityFlow(
                    transaction = fullySignedTransaction,
                    sessions = setOf(obligorFlowSession),
                    progressTracker = FINALIZING_TRANSACTION.childProgressTracker()!!
                )
            )
        }
    }

    /**
     * Responds to issuance of an obligation.
     *
     * @param session A session with the initiating counter-party.
     */
    @InitiatedBy(ObligationIssuanceFlow.Initiator::class)
    class Responder(session: FlowSession) : ResponderFlowLogic(session)
}
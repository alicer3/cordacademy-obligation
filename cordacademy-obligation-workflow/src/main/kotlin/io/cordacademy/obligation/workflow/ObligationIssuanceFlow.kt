package io.cordacademy.obligation.workflow

import co.paralleluniverse.fibers.Suspendable
import io.cordacademy.obligation.contract.ObligationContract
import io.cordacademy.obligation.contract.ObligationState
import io.cordacademy.obligation.contract.participantKeys
import io.cordacademy.obligation.workflow.common.InitiatorFlowLogic
import io.cordacademy.obligation.workflow.common.ResponderFlowLogic
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
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
     * Specifies flow version 2
     */
    private const val FLOW_VERSION_2 = 2

    /**
     * Initiates issuance of an obligation.
     *
     * @param obligor The obligor with whom to issue an obligation.
     * @param borrowed The amount of currency for which the obligation is to be issued.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_2)
    class Initiator(
        private val obligor: Party,
        private val borrowed: Amount<Currency>,
        private val anonymous: Boolean = false
    ) : InitiatorFlowLogic() {

        @Suspendable
        override fun call(): SignedTransaction {

            setStep(INITIALIZING_FLOW)
            val obligee = serviceHub.myInfo.legalIdentities.first()
            val obligorFlowSession = initiateFlow(obligor)
            val obligorFlowVersion = obligorFlowSession.getCounterpartyFlowInfo().flowVersion

            when (obligorFlowVersion) {
                FLOW_VERSION_2 -> obligorFlowSession.send(anonymous)
                FLOW_VERSION_1 -> if (anonymous) throw FlowException(
                    "Counter-party is running flow version 1 which does not support anonymous obligations."
                )
                else -> throw FlowException(
                    "Counter-party is running flow version $obligorFlowSession which is not supported."
                )
            }

            val obligation = createObligation(obligorFlowSession, obligee)
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

        @Suspendable
        private fun createObligation(session: FlowSession, obligee: AbstractParty): ObligationState {
            return if (anonymous) {
                val swappedAnonymousIdentities = subFlow(SwapIdentitiesFlow(session))
                val anonymousObligor = swappedAnonymousIdentities[obligor]!!
                val anonymousObligee = swappedAnonymousIdentities[obligee]!!
                ObligationState(anonymousObligor, anonymousObligee, borrowed)
            } else {
                ObligationState(obligor, obligee, borrowed)
            }
        }
    }

    /**
     * Responds to issuance of an obligation.
     *
     * @param session A session with the initiating counter-party.
     */
    @InitiatedBy(ObligationIssuanceFlow.Initiator::class)
    class Responder(session: FlowSession) : ResponderFlowLogic(session) {

        @Suspendable
        override fun call(): SignedTransaction {
            if (session.receive<Boolean>().unwrap { it }) {
                subFlow(SwapIdentitiesFlow(session))
            }

            return super.call()
        }
    }
}
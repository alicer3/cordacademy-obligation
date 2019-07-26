package io.cordacademy.obligation.workflow

import co.paralleluniverse.fibers.Suspendable
import io.cordacademy.obligation.v1.contract.ObligationContract
import io.cordacademy.obligation.v2.contract.ObligationContractV1
import io.cordacademy.obligation.v2.contract.ObligationContractV2
import io.cordacademy.obligation.v2.contract.ObligationStateV1
import io.cordacademy.obligation.v2.contract.ObligationStateV2
import io.cordacademy.obligation.workflow.common.InitiatorFlowLogic
import io.cordacademy.obligation.workflow.common.ResponderFlowLogic
import net.corda.core.contracts.Amount
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
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
     * Specifies flow version 1
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
        private val version: Int = FLOW_VERSION_2
    ) : InitiatorFlowLogic() {

        @Suspendable
        override fun call(): SignedTransaction {

            setStep(INITIALIZING_FLOW)
            val obligee = serviceHub.myInfo.legalIdentities.first()
            val obligorFlowSession = initiateFlow(obligor)
            val obligorFlowVersion = obligorFlowSession.getCounterpartyFlowInfo().flowVersion
            val ourSigningKey = obligee.owningKey
            val (obligation, command, contractId) = create(obligee, obligorFlowVersion)

            setStep(CREATING_TRANSACTION)
            val transaction = with(TransactionBuilder(firstNotary)) {
                addOutputState(obligation, contractId)
                addCommand(command, obligation.participants.map { it.owningKey })
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
        private fun create(
            obligee: Party,
            obligorFlowVersion: Int
        ): Triple<ContractState, CommandData, ContractClassName> {

            if (obligorFlowVersion < version) {
                throw FlowException("Obligor with flow version $obligorFlowVersion cannot issue $version states.")
            }

            return when (version) {
                FLOW_VERSION_1 -> Triple(
                    ObligationStateV1(obligor, obligee, borrowed),
                    io.cordacademy.obligation.v1.contract.ObligationContract.Issue(),
                    ObligationContractV1.ID
                )
                FLOW_VERSION_2 -> Triple(
                    ObligationStateV2(obligor, obligee, borrowed),
                    io.cordacademy.obligation.v2.contract.ObligationContract.Issue(),
                    ObligationContractV2.ID
                )
                else -> throw FlowException("Flow version $obligorFlowVersion is not supported.")
            }
        }
    }

    /**
     * Responds to issuance of an obligation.
     *
     * @param session A session with the initiating counter-party.
     */
    @InitiatedBy(Initiator::class)
    class Responder(session: FlowSession) : ResponderFlowLogic(session)
}
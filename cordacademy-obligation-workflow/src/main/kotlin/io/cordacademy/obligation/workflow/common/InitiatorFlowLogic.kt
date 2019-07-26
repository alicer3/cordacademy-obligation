package io.cordacademy.obligation.workflow.common

import io.cordacademy.obligation.v1.contract.ObligationState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

/**
 * Represents the base flow for all initiating obligation flows.
 * This sets up all common flow steps, adds useful features and helps to reduce DRY violations.
 */
abstract class InitiatorFlowLogic : FlowLogic<SignedTransaction>() {

    protected companion object {
        val INITIALIZING_FLOW = object : Step("Initializing transaction.") {}
        val CREATING_TRANSACTION = object : Step("Creating unsigned transaction.") {}
        val VERIFYING_TRANSACTION = object : Step("Verifying unsigned transaction.") {}
        val SIGNING_TRANSACTION = object : Step("Signing unsigned transaction.") {}
        val COUNTERSIGNING_TRANSACTION = object : Step("Gathering counter-party signatures.") {
            override fun childProgressTracker(): ProgressTracker = CollectSignaturesFlow.tracker()
        }
        val FINALIZING_TRANSACTION = object : Step("Finalizing fully signed transaction.") {
            override fun childProgressTracker(): ProgressTracker = FinalityFlow.tracker()
        }
    }

    /**
     * Builds a progress tracker with the default steps for ledger update flows.
     */
    override val progressTracker: ProgressTracker
        get() = ProgressTracker(
            INITIALIZING_FLOW,
            CREATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            COUNTERSIGNING_TRANSACTION,
            FINALIZING_TRANSACTION
        )

    /**
     * Gets the first known notary identity.
     */
    protected val firstNotary: Party
        get() = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
            ?: throw FlowException("No available notary.")


    /**
     * Sets the current step of the progress tracker.
     *
     * @param step The step to set as the current step.
     */
    protected fun setStep(step: Step) {
        progressTracker.currentStep = step
    }

    /**
     * Finds an obligation by linear ID.
     *
     * @param linearId The linear ID of the obligation to find in the vault.
     * @return Returns an unconsumed obligation with the specified linear ID.
     * @throws FlowException if there is no unconsumed obligation in the vault with the specified linear ID.
     */
    protected fun findObligationByLinearId(linearId: UniqueIdentifier): StateAndRef<ObligationState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub
            .vaultService
            .queryBy<ObligationState>(criteria = queryCriteria)
            .states
            .singleOrNull()
            ?: throw FlowException("Cannot find obligation with id $linearId.")
    }

    /**
     * Creates flow sessions for the specified counter-parties.
     *
     * @param counterparties The counter-parties for which to create flow sessions.
     * @return Returns a set of flow sessions for each of the specified counter-parties.
     * @throws FlowException if a specified counter-party cannot be resolved to a well known party.
     */
    protected fun flowSessionsFor(counterparties: Iterable<AbstractParty>): Set<FlowSession> {
        return counterparties.map {
            serviceHub.identityService.wellKnownPartyFromAnonymous(it)
                ?: throw FlowException("Cannot resolve anonymous party.")
        }.map { initiateFlow(it) }.toSet()
    }
}
package io.cordacademy.obligation.workflow.common

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.transactions.SignedTransaction

/**
 * Represents the base flow for all accepting obligation flows.
 * This sets up all counter-party flow steps and helps to reduce DRY violations.
 * @param session A flow session with the counter-party.
 */
abstract class ResponderFlowLogic(protected val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val transaction = subFlow(object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) = Unit
        })

        return subFlow(ReceiveFinalityFlow(session, transaction.id))
    }
}
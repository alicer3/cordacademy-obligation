![Cordacademy logo](https://raw.githubusercontent.com/cordacademy/cordacademy.github.io/master/content/images/logo-combined.png)

# Obligation CorDapp

**Demonstrates the ability to issue, transfer, settle and exit obligation-like agreements.**

Obligations are simple states that represent an agreement for an amount of currency between an obligor (otherwise known as a debtor or borrower) and an obligee (otherwise known as a creditor or lender). Any participant can issue an obligation to any other participant on the network. Obligations can be settled partially, or in full. Once an obligation has been fully settled, it can be exited, at which point, the evolution of the obligation state ceases.

## [Contract (version 1)](https://github.com/cordacademy/cordacademy-obligation/tree/master/cordacademy-obligation-contract-v1/src/main/kotlin/io/cordacademy/obligation/contract/v1)

Defines the state and contract for the obligation CorDapp. The contract packages are versioned to allow for future upgrades.

### [ObligationState](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-contract-v1/src/main/kotlin/io/cordacademy/obligation/contract/v1/ObligationState.kt)

Represents an obligation between an obligor and an obligee for an amount of currency.

```kotlin
package io.cordacademy.obligation.contract.v1

@BelongsToContract(ObligationContract::class)
data class ObligationState(
    val obligor: AbstractParty,
    val obligee: AbstractParty,
    val borrowed: Amount<Currency>,
    val settled: Amount<Currency> = Amount.zero(borrowed.token),
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState { ... }
```

### [ObligationContract](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-contract-v1/src/main/kotlin/io/cordacademy/obligation/contract/v1/ObligationContract.kt)

Represents the obligation contract which governs how obligation states may evolve.

```kotlin
package io.cordacademy.obligation.v1.contract

class ObligationContract : Contract {
	interface ObligationContractCommand : CommandData
	class Issue : ObligationContractCommand { ... }
	class Transfer : ObligationContractCommand { ... }
	class Settle : ObligationContractCommand { ... }
	class Exit : ObligationContractCommand { ... }
}
```

## [Workflow](https://github.com/cordacademy/cordacademy-obligation/tree/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow)

Defines the issuance, transfer, settlement and exit flows which perform ledger updates. As there is some commonality between these flows, common logic is encapsulated into base flow implementations.

### [InitiatorFlowLogic](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/common/InitiatorFlowLogic.kt)

Represents the base class for all initiating flows.

```kotlin
package io.cordacademy.obligation.workflow.shared

abstract class InitiatorFlowLogic : FlowLogic<SignedTransaction>() { ... }
```

The following flow implementations extend this class:

- [ObligationIssuanceFlow.Initiator](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/ObligationIssuanceFlow.kt)
- [ObligationTransferFlow.Initiator](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/ObligationTransferFlow.kt)
- [ObligationSettlementFlow.Initiator](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/ObligationSettlementFlow.kt)
- [ObligationExitFlow.Initiator](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/ObligationExitFlow.kt)

### [ResponderFlowLogic](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/common/ResponderFlowLogic.kt)

Represents the base class for all responding flows.

```kotlin
package io.cordacademy.obligation.workflow.shared

abstract class ResponderFlowLogic : FlowLogic<SignedTransaction>() { ... }
```

The following flow implementations extend this class:

- [ObligationIssuanceFlow.Responder](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/ObligationIssuanceFlow.kt)
- [ObligationTransferFlow.Responder](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/ObligationTransferFlow.kt)
- [ObligationSettlementFlow.Responder](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/ObligationSettlementFlow.kt)
- [ObligationExitFlow.Responder](https://github.com/cordacademy/cordacademy-obligation/blob/master/cordacademy-obligation-workflow/src/main/kotlin/io/cordacademy/obligation/workflow/ObligationExitFlow.kt)
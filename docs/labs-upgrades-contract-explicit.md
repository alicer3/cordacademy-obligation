![Cordacademy logo](https://raw.githubusercontent.com/cordacademy/cordacademy.github.io/master/content/images/logo-combined.png)

# Explicit State/Contract Upgrades

Business requirements change over time, and business software solutions need to follow suite. CorDapps are no different. In this series we will be looking at how to upgrade CorDapps. This document illustrates how to upgrade states and contracts in a CorDapp using explicit upgrades and hash constraints.

## Understanding States

### R3's Definition

- States represent on-ledger facts
- States are evolved by marking the current state as historic and creating an updated state
- Each node has a vault where it stores any relevant states to itself

### Expanding The Definition

States are immutable data objects, composed of arbitrary information and can evolve over time. They provide a mechanism for nodes to store and share facts with one another and can be used to model all kinds of business entities and assets, such as financial instruments, obligations, currency, tokens, bookings, receipts, etc.

## Understanding Contracts

### R3's Definition

- A valid transaction must be accepted by the contract of each of its input and output states
- Contracts are written in a JVM programming language (e.g. Java or Kotlin)
- Contract execution id deterministic and its acceptance of a transaction is based on the transaction's contents alone.

### Expanding The Definition

Contracts represent constraints which govern how states are created, evolved and consumed. Typically, contracts are constructed of one or more commands, each of which defines a responsibility to verify a transaction that may create, and/or consume one or more states.

Contract commands are not limited to verification of states only. They can also verify other transaction information; for example, attachments.

Contract commands must always execute in a deterministic manner, so that what is true now, will always be true in future, and what is false now, will always be false in future. This is to ensure that transaction chains remain valid and there can be no disagreement over historic consensus.

## Laboratory

In this laboratory we will understand how to deploy a Corda network running version 1 of the obligation CorDapp. We will create an obligation between two participants on the network and then upgrade their nodes to use version 2 of the CorDapp. This demonstrates their ability to interact with each other using version 2 of the CorDapp, and how they can still communicate with other nodes running version 1.

### Getting Started

First you will need to clone the **cordacademy-obligation** repository and deploy some Corda nodes.

**NOTICE - YOU NEED TO CHECK OUT THE `master-versioned` BRANCH!**

The **master-versioned** branch is designed to illustrate how to use explicit upgrades and hash constraints, which we will need for this demo.

```
git clone https://github.com/cordacademy/cordacademy-obligation

cd cordacademy-obligation

git checkout --track origin/master-versioned

./gradlew deployNodes
```

Once the `deployNodes` task has completed, you can then start the nodes. 

### Starting The Nodes

From the project's root directory, navigate to where the nodes are deployed.

```
cd build/nodes
```

In this directory, you should find sub-directories for each deployed node: `PartyA`, `PartyB`, `PartyC` and `Notary`.

At this point create some new terminals/tabs from the directory you're in, so that you can navigate to and start each node independently. This will become really useful later as you'll need to stop Party A's node and Party B's node, whilst leaving the rest of the network running.

Assume that you're in **build/nodes** and you want to start each node...

#### Notary

```
cd Notary
java -jar corda.jar
```

#### PartyA

```
cd PartyA
java -jar corda.jar
```

#### PartyB

```
cd PartyB
java -jar corda.jar
```

#### PartyC

```
cd PartyC
java -jar corda.jar
```

Once all the nodes have started successfully, open the project in IntelliJ and import the grade project. 

### Running The Web Servers

Once everything is imported correctly, you should be able to start the web servers from the run configuration dropdown using the "Run All WebServers".

_The web-server implementation comes from the **cordacademy-template** repository. It is configured to work with either the in-memory network driver or deployNodes, without needing to change configuration. Each application based on the template includes additional endpoints that are relevant to that application. Please refer to the template readme for more information about the web server configuration._

### Configuring Postman

Each Cordacademy solution comes with a Postman configuration. You can find this in the root directory for the project, and is called `postman.json`.

In Postman, click the "Import" button and import the `postman.json` file. This will create a new collection, containing three groups: `PartyA`, `PartyB` and `PartyC`.

### Issuing An Obligation

From PartyA, select the POST **/obligations/issue** end-point. It's already configured with a body which will issue an obligation for 123.45 GBP to PartyB...

```json
{
	"obligor": "O=PartyB, L=New York, C=US",
	"borrowed": 123.45,
	"currency": "GBP"
}
```

Once this transaction completes, you should see a transaction ID. You can check this by querying the vault on PartyA's node and PartyB's node using the GET **/obligations/v1** endpoint.

### Upgrading the CorDapp

Version 2 of the CorDapp upgrades the state and contract to allow obligations to be defaulted by the obligor. It does this by introducing a new, version 2 module of the state/contract, which references version 1 of the contract, allowing it to be explicitly upgraded.

First, stop the web servers in IntelliJ. Then in all terminals type `exit` at their console to stop the node, and close down the terminals.

Next we need to check out the **labs/upgrade/contract-explicit** branch and deploy version 2 of the CorDapp.

```
git checkout --track origin/labs/upgrade/contract-explicit

./gradlew deployNodes
```

### Backup/Restore

There is an experimental feature built into v2 of the **deployNodes** script, which will perform the backup restore for you. It will:

- Copy `build/nodes` into `backup/nodes` before running **deployNodes**.
- Run **deployNodes** which will destroy everything in `build/nodes`
- Copy `backup/nodes/PartyC` into `build/nodes/partyC`
- Copy  `persistence.mv.db` and `persistence.trace.db` for PartyA and PartyB back into their respective directories in `build/nodes`.

_If you are running [Corda Enterprise](https://www.r3.com/platform/) nodes, you will unlikely need to run this backup/restore process as your data should be persisted in a high-performance SQL data store, rather than using the built-in H2 database. This step is still necessary when upgrading open source Corda nodes._

Once the backup/restore process is complete, you can restart each node and all web servers.

### Re-configuring Postman

Version 2 of this CorDapp contains updated endpoints. Remove version 1 of the postman configuration and import the postman configuration for version 2.

### Upgrading An Obligation

Explicit upgrades require counterparts authorisation before they can be upgraded. First you will need to obtain the ID of the obligation to upgrade. You can find this by querying the **/obligations/v1** end-point.

From PartyB's node, enter the ID into the body for **/obligations/upgrade/authorize**

```json
{
    "linearId": "ID_OF_YOUR_OBLIGATION"
}
```

You won't get much in the way of a response from this endpoint, but it should return HTTP 200 (OK).

Next you need to initiate the upgrade, which will consume version 1 of the state and produce version 2 of the state, which points to version 2 of the contract.

From PartyA's node, enter the ID into the body for **/obligations/upgrade/initiate**

```json
{
    "linearId": "ID_OF_YOUR_OBLIGATION"
}
```

You won't get much in the way of a response from this endpoint, but it should return HTTP 200 (OK).

### Versioned Vault Queries

Because the explicitly upgraded cordapp depends on both version 1 and version 2 of the states and contracts JARs, the endpoints have been designed to allow you to see both.

- To see version 1 states, query **/obligations/v1**
- To see version 2 states, query **/obligations/v2**

Provided that your upgrade succeeded, you should see a new state in the vault when you query for a version 2 state.

### Default On Obligations

Version 2 of the CorDapp allows the obligor of an obligation to default on the obligation. You can test this functionality using a version 2 obligation state. From _the obligor's_ node, enter the ID of the obligation that you want to default. You should get a transaction ID back. When querying the vault, you should see an obligation where `"defaulted": true` should appear in the response.

## Tasks

Version 2 of the CorDapp incomplete. There are various tasks that can be completed here impacting both flows and states & contracts.

1. The transfer, settle and exit flows are not compatible with version 2 of the CorDapp. These need to be fixed to allow compatibility with either a version 1 or a version 2 state.

2. As an additional task, there is a requirement that when an obligation is defaulted, a third party (credit default insurer) is added to the state. You can add this to version 2 of the state

   ```kotlin
   data class ObligationState(
       ...
       val insurer: AbstractParty? = null
   )
   ```

   The contract should check that 

   - `insurer != null` when `defaulted == true`
   - `insurer != obligee` 
   - `insurer != obligor`

   You can use **PartyC** as the insurer for this case. To perform this upgrade, you will need to remove the copy rules for PartyC from **deployNodes** so that PartyC does not revert back to version 1 of the CorDapp when you deploy the new JAR files.
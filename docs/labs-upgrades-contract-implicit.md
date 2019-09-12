![Cordacademy logo](https://raw.githubusercontent.com/cordacademy/cordacademy.github.io/master/content/images/logo-combined.png)

# Implicit State/Contract Upgrades

Business requirements change over time, and business software solutions need to follow suite. CorDapps are no different. In this series we will be looking at how to upgrade CorDapps. This document illustrates how to upgrade states and contracts in a CorDapp using implicit upgrades and signature constraints.

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

```
git clone https://github.com/cordacademy/cordacademy-obligation

cd cordacademy-obligation

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

Once this transaction completes, you should see a transaction ID. You can check this by querying the vault on PartyA's node and PartyB's node using the GET **/obligations** endpoint.

### Upgrading the CorDapp

Version 2 of the CorDapp upgrades the state and contract to allow obligations to be defaulted by the obligor. Upgrading is different from explicit upgrades; version 2 of the state/contract isn't dependent on version 1, and the state/contract modules don't need to be versioned at the code level as with explicit upgrades. Rather the existing state/contract module is upgraded in place, in such a way that it remains backwards compatible with the previous version. Provided that the jar file is signed with the same key as the previous version of the CorDapp, nodes should accept and use this as the new version.

_For development purposes, the jar files are signed with the development key._

First, stop the web servers in IntelliJ. Then in the terminals for `PartyA` and `PartyB` type `exit` at their console to stop the node, and close down the terminals.

Next we need to check out the **labs/upgrade/contract-implicit** branch and deploy version 2 of the CorDapp.

```
git checkout --track origin/labs/upgrade/contract-implicit

./gradlew upgradeNodes
```

There's no need to worry about copying persistence or JAR files into the node directories, the `upgradeNodes` task will do that for you. All you need to do once the task is complete is go and restart all of the nodes.

Once the nodes are all running you can restart the web servers as well.

### Re-configuring Postman

Version 2 of this CorDapp contains updated endpoints. Remove version 1 of the postman configuration and import the postman configuration for version 2.

### Upgrading An Obligation

Unlike with explicit upgrading, there is no longer any need to manually perform state/contract upgrades as the upgrade process is handled implicitly.

### Default On Obligations

From PartyB, select the GET **/obligations** end-point and copy the linear ID for the state to be defaulted, then select the PUT **/obligations/default** end-point, enter the linear ID into the body and execute the request. Assuming the transaction completes successfully, you should be able to re-query the participant nodes, which should now display the following:

```json
{
	"obligor": "O=PartyB, L=New York, C=US",
	"borrowed": 123.45,
	"currency": "GBP",
  "defaulted": true
}
```

### Pre-Deterministic JVM Notes

The major benefit of implicit upgrades is that it does not require versioned contract implementations, and since the contracts are designed for backwards compatibility, nodes running older versions of the CorDapp should still be compatible and interoperable with nodes running newer versions of the contract.

That said, interoperability with nodes running older versions of the CorDapp is not available until the DJVM has been implemented into Corda.
![Cordacademy logo](https://raw.githubusercontent.com/cordacademy/cordacademy.github.io/master/content/images/logo-combined.png)

# Schema Upgrades

Business requirements change over time, and business software solutions need to follow suite. CorDapps are no different. In this series we will be looking at how to upgrade CorDapps. This document illustrates how to upgrade schemas in a CorDapp.

## Understanding States

### R3's Definition

- States represent on-ledger facts
- States are evolved by marking the current state as historic and creating an updated state
- Each node has a vault where it stores any relevant states to itself

### Expanding The Definition

States are immutable data objects, composed of arbitrary information and can evolve over time. They provide a mechanism for nodes to store and share facts with one another and can be used to model all kinds of business entities and assets, such as financial instruments, obligations, currency, tokens, bookings, receipts, etc.

## Understanding Schemas

### R3's Definition

Corda offers developers the option to expose all or some parts of a contract state to an Object Relational Mapping (ORM) tool to be persisted in a Relational Database Management System (RDBMS).

The purpose of this, is to assist Vault development and allow for the persistence of state data to a custom database table. Persisted states held in the vault are indexed for the purposes of executing queries. This also allows for relational joins between Corda tables and the organization’s existing data.

## Laboratory

In this laboratory we will understand how to deploy a Corda network running the obligation CorDapp. We will start with seeing the difference between schema upgrade with Corda OS and CE, followed by how to initiate and upgrade schema with Corda CE.

### Getting Started

First you will need to clone the **cordacademy-obligation** repository and deploy some Corda nodes.

```
git clone https://github.com/cordacademy/cordacademy-obligation

cd cordacademy-obligation

```

### See the difference between OS and CE

Switch to **lab-upgrade-schema-init** branch. This branch will have the initial scripts of schema `ObligationStateSchema`.

Let's prepare bootstrapped network with node `PartyA`, `PartyB`, `PartyC` and notary `Notary`. By default, `deployNodes` task will bootstrap all three nodes with Corda OS. Task `installCE_A` will upgrade `PartyA` to Corda CE.

```
./gradlew clean deployNodes installCE_A
``` 

Spin up all the nodes and Notary.

```
./build/nodes/runnodes
```

`PartyA` node will fail to start up as expected, as the absence of migration script for `ObligationSchema`. Meanwhile `PartyB` and `PartyC` will start successfully with OS version. And the table `obligation_states` for `ObligationSchema` will be created.

### Start CE nodes with migration scripts

Let's start all over with clean nodes on Corda CE.

```
./gradlew clean deployNodes installCE_A installCE_B installCE_C
``` 

Include the migration scripts in cordapp. 

Go to **cordacademy-obligation-contracts/build.gradle** and uncomment the line `exclude '**/migration/**'`. It will include the migration scripts when packing cordapps.

Install cordapps (with migration scripts) on all nodes. The task `upgradeNodes` will build new cordapp jars and install them on all nodes.

```
./gradlew build upgradeNodes
```

Spin up all the nodes and Notary.

```
./build/nodes/runnodes
```

All nodes and notary will run successfully. And the table `obligation_states` for `ObligationSchema` will be created.

### Upgrade schema

Currently the `ObligationSchema` has column `linear_id` with wrong type `String`. We want to correct this column type to `UUID` instead. This scenario will demonstrate how to upgrade existing schema.

Switch to **lab/upgrade/schema/v1** branch. In this branch, the column type of `linear_id` in `ObligationSchema` has been updated and all corresponding adjustment have been made in code. The liquidbase script for this change `migration/obligation-schema-v1.changelog-v1.xml` has been created and appended to `migration/obligation-schema-v1.changelog-master.xml`.

Let's build the updated cordapps again.

```
./gradlew build upgradeNodes
```
Spin up all the nodes and Notary.

```
./build/nodes/runnodes
```

All nodes shall spin up successfully. And the table `obligation_states` will be updated accordingly. 

### Notes
Meanwhile you can always make any transaction anytime, based on the instruction below. It should not affect the upgrade.


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


### Upgrading An Obligation

Unlike with explicit upgrading, there is no longer any need to manually perform state/contract upgrades as the upgrade process is handled implicitly.

### Pre-Deterministic JVM Notes

The major benefit of implicit upgrades is that it does not require versioned contract implementations, and since the contracts are designed for backwards compatibility, nodes running older versions of the CorDapp should still be compatible and interoperable with nodes running newer versions of the contract.

That said, interoperability with nodes running older versions of the CorDapp is not available until the DJVM has been implemented into Corda.
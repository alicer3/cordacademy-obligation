![Cordacademy logo](https://raw.githubusercontent.com/cordacademy/cordacademy.github.io/master/content/images/logo-combined.png)

# Workflow Upgrades

Business requirements change over time, and business software solutions need to follow suite. CorDapps are no different. In this series we will be looking at how to upgrade CorDapps. This document illustrates how to upgrade flows in a CorDapp.



## Acknowledgements

### Henrik Carlström

**Associate Director, Solutions Engineering, R3**

Henrik's efforts have been instrumental in understanding the CorDapp upgrade process. Without his time and effort, this series on performing CorDapp upgrades would not have been possible. Thank you for your time and support.



## Prerequisites

There are a few applications that you should have installed on your machine before proceeding with the CorDapp upgrade laboratory:

- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [Git](https://git-scm.com/)
- [Postman](https://www.getpostman.com/)



## Understanding Flows

### R3's Definition

- Flows automate the process of agreeing ledger updates.

- Communication between nodes only occurs in the context of these flows, and is point-to-point.

- Built in flows are provided to automate common tasks.

### Expanding The Definition

Flows provide a mechanism for nodes to have conversations with each other. In the context of transactional flows, it's more than just a conversation it's a negotiation and an agreement from each node in order to reach consensus about what may be committed to the ledger.

Conversations between nodes follow a protocol and must remain synchronised with each other. What one node requests from a counter-party, the counter-party must respond with. Breaking that synchronisation between nodes would likely cause unexpected behaviour or an all-out flow failure.

Flows are _usually_ written in pairs, for example an **Initiator** flow and a **Responder** flow.



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

```
{
	"obligor": "O=PartyB, L=New York, C=US",
	"borrowed": 123.45,
	"currency": "GBP"
}
```

Once this transaction completes, you should see a transaction ID. You can check this by querying the vault on PartyA's node and PartyB's node using the GET **/obligations** endpoint.

### Upgrading the CorDapp

Version 2 of the CorDapp upgrades the issuance flow to allow anonymous obligations to be issued between participants on the network.

First, stop the web servers in IntelliJ. Then find the terminals for PartyA and PartyB and type `exit` at their console to stop the node.

Next we need to check out the **labs/upgrade/workflow** branch and build version 2 of the CorDapp.

```
git checkout --track origin/labs/upgrade/workflow

./gradlew build
```

Once the build has completed, you will need to copy the new workflow JAR file into the cordapps directories for PartyA and PartyB. Once the new JAR file has been installed, restart the nodes (see "Starting The Nodes") and remember you only need to restart nodes for PartyA and PartyB as the rest of the network is still running.

Once the nodes are all running you can restart the web servers as well.

### Issuing An Anonymous Obligation

From PartyA, select the POST **/obligations/issue** end-point. It's already configured with a body which will issue an obligation for 123.45 GBP to PartyB, however this time, we need to add a new property called **anonymous**.

```
{
	"obligor": "O=PartyB, L=New York, C=US",
	"borrowed": 123.45,
	"currency": "GBP",
	"anonymous": true
}
```

Once this transaction completes, you should see a transaction ID. You can check this by querying the vault on PartyA's node and PartyB's node using the GET **/obligations** endpoint. In the new obligation you should see that the obligee and obligor are anonymised.



## Tasks

Version 2 of the CorDapp incomplete. Whilst you can issue anonymous obligations, it's not possible to transfer, settle or exit them. Upgrade the remaining flows to support anonymous obligations.

Note - whilst upgrading the transfer, settle and exit flows, it will be easier to test against the in-memory network driver (since at this point, you're not testing upgrades). Once you feel sufficiently confident that the flows work, you can then test them using a network generated with the `deployNodes` task in order to ensure that it remains backwards compatible with version 1 of the CorDapp.
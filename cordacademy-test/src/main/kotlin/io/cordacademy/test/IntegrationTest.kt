package io.cordacademy.test

import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Provides utility for implementing Corda node driver based tests.
 *
 * @param cordapps A list of cordapps which should be loaded by the node driver network.
 */
abstract class IntegrationTest(private vararg val cordapps: String) : AutoCloseable {

    private companion object {
        val log = loggerFor<IntegrationTest>()
    }

    private lateinit var _nodeA: NodeHandle
    private lateinit var _nodeB: NodeHandle
    private lateinit var _nodeC: NodeHandle

    /**
     * Gets handle to party A's node.
     */
    protected val nodeA: NodeHandle get() = _nodeA

    /**
     * Gets handle to party B's node.
     */
    protected val nodeB: NodeHandle get() = _nodeB

    /**
     * Gets handle to party C's node.
     */
    protected val nodeC: NodeHandle get() = _nodeC

    /**
     * Creates and starts the in-memory network.
     */
    fun start() {
        val rpcUsers: List<User> = listOf(User("guest", "letmein", permissions = setOf("ALL")))

        val parameters = DriverParameters(
            isDebug = true,
            startNodesInProcess = true,
            waitForAllNodesToFinish = true,
            cordappsForAllNodes = cordapps.map { TestCordapp.findCordapp(it) }
        )

        driver(parameters) {
            _nodeA = startNode(providedName = IDENTITY_A.name, rpcUsers = rpcUsers).getOrThrow()
            _nodeB = startNode(providedName = IDENTITY_B.name, rpcUsers = rpcUsers).getOrThrow()
            _nodeC = startNode(providedName = IDENTITY_C.name, rpcUsers = rpcUsers).getOrThrow()

            listOf(_nodeA, _nodeB, _nodeC).forEach { logStartedNode(it) }
        }

        initialize()
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     */
    override fun close() = finalize()

    /**
     * Provides post startup test initialization.
     */
    protected open fun initialize() = Unit

    /**
     *Provides pre tear-down test finalization.
     */
    protected open fun finalize() = Unit

    /**
     * Initializes the test container.
     */
    @BeforeEach
    private fun setup() = start()

    /**
     * Finalizes the test container.
     */
    @AfterEach
    private fun tearDown() {
        log.info("Closing down integration test network.")
        close()
    }

    private fun logStartedNode(node: NodeHandle) = log.info(
        "Node registered with RPC address '${node.rpcAddress}' for '${node.nodeInfo.legalIdentities.first()}'"
    )
}
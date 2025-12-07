package io.github.psychoplasma.nonceq.queue

import io.github.psychoplasma.nonceq.queue.inmemory.InMemoryNonceQueueRepository
import io.github.psychoplasma.nonceq.utils.BlockNonceProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

class NonceQueueManagerTest {

    private lateinit var repository: InMemoryNonceQueueRepository
    private lateinit var nonceQueue: NonceQueue
    private lateinit var blockNonceProvider: MockBlockNonceProvider
    private lateinit var nonceQueueManager: NonceQueueManager

    @BeforeEach
    fun setUp() {
        repository = InMemoryNonceQueueRepository()
        // Capacity 10, expiry 10000
        nonceQueue = NonceQueue(repository, 10, 10000)
        blockNonceProvider = MockBlockNonceProvider()
        nonceQueueManager = NonceQueueManager(blockNonceProvider, nonceQueue)
    }

    @Test
    fun `getNextValidNonce should fetch from provider when queue is empty`() {
        val address = "0x123"
        val expectedNonce = BigInteger.TEN
        blockNonceProvider.setNextNonce(expectedNonce)

        val nonce = nonceQueueManager.getNextValidNonce(address)

        assertEquals(expectedNonce, nonce)
        // Verify it was inserted into the queue (head should be set)
        assertEquals(expectedNonce, repository.getHead(address.lowercase()))
    }

    @Test
    fun `getNextValidNonce should return next nonce from queue when not empty`() {
        val address = "0x123"
        val initialNonce = BigInteger.TEN
        blockNonceProvider.setNextNonce(initialNonce)

        // First call initializes the queue with 10
        val firstNonce = nonceQueueManager.getNextValidNonce(address)
        assertEquals(initialNonce, firstNonce)

        // Second call should return 11
        val secondNonce = nonceQueueManager.getNextValidNonce(address)
        assertEquals(initialNonce.add(BigInteger.ONE), secondNonce)
    }

    @Test
    fun `getNextValidNonce should handle case insensitivity`() {
        val address = "0xABC"
        val expectedNonce = BigInteger.TEN
        blockNonceProvider.setNextNonce(expectedNonce)

        val nonce = nonceQueueManager.getNextValidNonce(address)

        assertEquals(expectedNonce, nonce)
        // Verify it was inserted using lowercase address
        assertEquals(expectedNonce, repository.getHead(address.lowercase()))
    }

    @Test
    fun `useNonce should mark nonce as used`() {
        val address = "0x123"
        val nonceVal = BigInteger.TEN
        blockNonceProvider.setNextNonce(nonceVal)

        // Initialize and get nonce
        nonceQueueManager.getNextValidNonce(address)

        // Use it
        nonceQueueManager.useNonce(address, nonceVal, "tx1")

        // Verify it is marked used in repository
        val nonceRecord = repository.getNonce(address.lowercase(), nonceVal)
        assertTrue(nonceRecord!!.used)
    }

    @Test
    fun `discardNonce should remove nonce from queue`() {
        val address = "0x123"
        val nonceVal = BigInteger.TEN
        blockNonceProvider.setNextNonce(nonceVal)

        // Initialize and get nonce
        nonceQueueManager.getNextValidNonce(address)

        // Discard it
        nonceQueueManager.discardNonce(address, nonceVal, "some error")

        // Verify it is removed (getNonce returns null)
        val nonceRecord = repository.getNonce(address.lowercase(), nonceVal)
        assertEquals(null, nonceRecord)
    }

    @Test
    fun `discardNonce with nonce too low error should reset queue`() {
        val address = "0x123"
        val nonceVal = BigInteger.TEN
        blockNonceProvider.setNextNonce(nonceVal)

        // Initialize and get nonce
        nonceQueueManager.getNextValidNonce(address)

        // Discard with "nonce too low"
        nonceQueueManager.discardNonce(
            address,
            nonceVal,
            "Error: nonce too low"
        )

        // Verify queue is reset (empty)
        assertTrue(nonceQueue.isEmpty(address.lowercase()))
    }

    @Test
    fun `reset should reset the queue`() {
        val address = "0x123"
        val nonceVal = BigInteger.TEN
        blockNonceProvider.setNextNonce(nonceVal)

        // Initialize
        nonceQueueManager.getNextValidNonce(address)

        // Reset
        nonceQueueManager.reset(address)

        assertTrue(nonceQueue.isEmpty(address.lowercase()))
    }

    @Test
    fun `discardNonce with nonce too low should not reset if queue is empty`() {
        val address = "0x123"
        // Ensure queue is empty
        assertTrue(nonceQueue.isEmpty(address.lowercase()))

        nonceQueueManager.discardNonce(
            address,
            BigInteger.TEN,
            "nonce too low"
        )

        // Should remain empty, no exception
        assertTrue(nonceQueue.isEmpty(address.lowercase()))
    }
}

class MockBlockNonceProvider : BlockNonceProvider {
    private var nextNonce: BigInteger = BigInteger.ZERO

    fun setNextNonce(nonce: BigInteger) {
        this.nextNonce = nonce
    }

    override fun getBlockNonce(address: String): BigInteger {
        return nextNonce
    }
}

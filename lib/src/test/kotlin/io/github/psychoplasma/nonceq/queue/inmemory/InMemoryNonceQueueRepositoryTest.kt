package io.github.psychoplasma.nonceq.queue.inmemory

import io.github.psychoplasma.nonceq.queue.Nonce
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

class InMemoryNonceQueueRepositoryTest {

    private lateinit var repository: InMemoryNonceQueueRepository

    @BeforeEach
    fun setUp() {
        repository = InMemoryNonceQueueRepository()
    }

    @Test
    fun `head operations should work correctly`() {
        val address = "0x123"
        val initialHead = repository.getHead(address)
        assertEquals(BigInteger.ZERO, initialHead)


        val newHead = BigInteger.TEN
        repository.setHead(address, newHead)
        assertEquals(newHead, repository.getHead(address))

        // Set head to another value
        repository.setHead(address, BigInteger.TWO)
        assertEquals(BigInteger.TWO, repository.getHead(address))
    }

    @Test
    fun `tail operations should work correctly`() {
        val address = "0x123"
        val initialTail = repository.getTail(address)
        assertEquals(BigInteger.ZERO, initialTail)

        val newTail = BigInteger.TEN
        repository.setTail(address, newTail)
        assertEquals(newTail, repository.getTail(address))

        // Set tail to another value
        repository.setTail(address, BigInteger.TWO)
        assertEquals(BigInteger.TWO, repository.getTail(address))
    }

    @Test
    fun `nonce operations should work correctly`() {
        val address = "0x123"
        val nonceValue = BigInteger.ONE
        val nonce = Nonce("1", false, 1000L)

        // Put
        repository.putNonce(address, nonceValue, nonce)
        
        // Get
        val retrieved = repository.getNonce(address, nonceValue)
        assertEquals(nonce, retrieved)

        // Delete
        repository.deleteNonce(address, nonceValue)
        assertNull(repository.getNonce(address, nonceValue))
    }

    @Test
    fun `size should return correct number of elements`() {
        val address = "0x123"
        assertEquals(0L, repository.size(address))

        repository.putNonce(address, BigInteger.ONE, Nonce("1", false, 1000L))
        assertEquals(1L, repository.size(address))

        repository.putNonce(address, BigInteger.TWO, Nonce("2", false, 1000L))
        assertEquals(2L, repository.size(address))

        repository.deleteNonce(address, BigInteger.ONE)
        assertEquals(1L, repository.size(address))
    }

    @Test
    fun `clear should reset everything for address`() {
        val address = "0x123"
        repository.setHead(address, BigInteger.TEN)
        repository.setTail(address, BigInteger.ONE)
        repository.putNonce(address, BigInteger.ONE, Nonce("1", false, 1000L))

        repository.clear(address)

        assertEquals(BigInteger.ZERO, repository.getHead(address))
        assertEquals(BigInteger.ZERO, repository.getTail(address))
        assertEquals(0L, repository.size(address))
    }

    @Test
    fun `operations should be isolated by address`() {
        val address1 = "0x123"
        val address2 = "0x456"

        repository.setHead(address1, BigInteger.TEN)
        repository.setHead(address2, BigInteger.TWO)

        assertEquals(BigInteger.TEN, repository.getHead(address1))
        assertEquals(BigInteger.TWO, repository.getHead(address2))

        repository.putNonce(address1, BigInteger.ONE, Nonce("1", false, 1000L))
        assertEquals(1L, repository.size(address1))
        assertEquals(0L, repository.size(address2))
    }
}

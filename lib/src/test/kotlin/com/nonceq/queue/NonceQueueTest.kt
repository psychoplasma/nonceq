package com.nonceq.queue

import com.nonceq.queue.*
import com.nonceq.queue.inmemory.InMemoryNonceQueueRepository
import com.nonceq.Utils.generateAddress

import java.math.BigInteger

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class NonceQueueTest {
    private val capacity = 5L
    private val expiry = 250L
    private val repository = InMemoryNonceQueueRepository()
    private val queue: NonceQueue = NonceQueue(
        repository,
        capacity,
        expiry,
    )

    @Test
    fun `should insert an initial value`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)

        queue.insert(address, initialNonce)
        assertEquals(initialNonce.inc(), queue.next(address))
    }

    @Test
    fun `should not insert an initial value when it already exists`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)
        queue.insert(address, initialNonce)

        // Trying to insert again should have no effect
        queue.insert(address, initialNonce)
        assertEquals(initialNonce.inc(), queue.next(address))
    }

    @Test
    fun `should return next values in sequence`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)
        queue.insert(address, initialNonce)

        repeat((capacity - 1).toInt()) { i ->
            val expected = initialNonce + BigInteger.valueOf((i + 1).toLong())
            assertEquals(expected, queue.next(address))
        }
    }

    @Test
    fun `should throw QueueOverflowException when the queue reached its capacity`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)

        queue.insert(address, initialNonce)
        repeat(capacity.toInt() - 1) { queue.next(address) }

        assertThrows<NonceQueue.QueueOverflowException> { queue.next(address) }
    }

    @Test
    fun `should remove tail when it is used`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)
        queue.insert(address, initialNonce)

        queue.markUsed(address, initialNonce)

        queue.next(address)

        assertFalse(queue.has(address, initialNonce))
    }

    @Test
    fun `should remove value from tail when it is expired for later use`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)
        queue.insert(address, initialNonce)

        // Expire initial nonce
        runBlocking { delay(expiry + 1) }

        // Should return the initial nonce again
        assertEquals(initialNonce, queue.next(address))

        // Get the next nonce after initial
        queue.next(address)

        // Expire initial nonce and the next one
        runBlocking { delay(expiry + 1) }

        // Should return the initial nonce again
        assertEquals(initialNonce, queue.next(address))

        // Mark initial nonce as used
        queue.markUsed(address, initialNonce)

        // Should return the initial + 1, after removing used nonce(initial)
        // and expired nonce(initial + 1) from tail
        assertEquals(initialNonce.inc(), queue.next(address))
    }

    @Test
    fun `should return the lowest skipped nonce value`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)

        // Request some values
        queue.insert(address, initialNonce) // 666 skip
        val next1 = queue.next(address) // 667 use
        val next2 = queue.next(address) // 668 skip
        val next3 = queue.next(address) // 669 skip
        val next4 = queue.next(address) // 670 use

        // Use and remove some of them in random order
        // to simulate async response from the caller
        queue.markUsed(address, next4) // 670 used
        queue.remove(address, next3) // 669 skipped
        queue.markUsed(address, next1) // 667 used
        queue.remove(address, initialNonce) // 666 skipped
        queue.remove(address, next2) // 668 skipped

        assertEquals(initialNonce, queue.next(address))
        assertEquals(next2, queue.next(address))

        queue.markUsed(address, initialNonce)
        assertEquals(next3, queue.next(address))

        queue.remove(address, next2)
        assertEquals(next2, queue.next(address))

        queue.markUsed(address, next2)
        assertEquals(next4.inc(), queue.next(address))

        queue.remove(address, next4.inc())
        assertEquals(next4.inc(), queue.next(address))
    }

    @Test
    fun `should reset queue and cursors`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)
        queue.insert(address, initialNonce)
        queue.markUsed(address, initialNonce)
        queue.markUsed(address, queue.next(address))
        queue.markUsed(address, queue.next(address))

        queue.reset(address)

        assertTrue(queue.isEmpty(address))
        assertEquals(BigInteger.ONE, queue.next(address))

        queue.reset(address)
        queue.insert(address, BigInteger.valueOf(1001))

        assertEquals(BigInteger.valueOf(1002), queue.next(address))
    }

    @Test
    fun `should not remove a record if the record does not exist`() {
        val address = generateAddress()
        queue.insert(address, BigInteger.valueOf(666))

        queue.remove(address, BigInteger.TEN)
        assertNotEquals(BigInteger.TEN, queue.next(address))
    }

    @Test
    fun `should remove the initial record`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)
        queue.insert(address, initialNonce)

        queue.remove(address, initialNonce)
        assertEquals(initialNonce, queue.next(address))
    }

    @Test
    fun `should remove any record`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(666)
        queue.insert(address, initialNonce) // 1
        queue.next(address) // 2
        queue.next(address) // 3
        queue.next(address) // 4

        // Remove record number 2
        queue.remove(address, initialNonce.inc())
        // Record number 2 should be available for the next value
        assertEquals(initialNonce.inc(), queue.next(address))
    }

    @Test
    fun `should handle multiple addresses independently`() {
        val address1 = generateAddress()
        val address2 = generateAddress()
        val initialNonce1 = BigInteger.valueOf(100)
        val initialNonce2 = BigInteger.valueOf(200)

        queue.insert(address1, initialNonce1)
        queue.insert(address2, initialNonce2)

        val next1 = queue.next(address1)
        val next2 = queue.next(address2)

        assertEquals(initialNonce1.inc(), next1)
        assertEquals(initialNonce2.inc(), next2)
    }

    @Test
    fun `should maintain separate queue state for each address`() {
        val address1 = generateAddress()
        val address2 = generateAddress()
        val initialNonce = BigInteger.valueOf(500)

        queue.insert(address1, initialNonce)
        queue.insert(address2, initialNonce)

        // Use operations on address1 should not affect address2
        queue.markUsed(address1, initialNonce)
        assertTrue(queue.has(address1, initialNonce))
        assertTrue(queue.has(address2, initialNonce))

        // Remove from address1 should not affect address2
        queue.remove(address1, initialNonce)
        assertFalse(queue.has(address1, initialNonce))
        assertTrue(queue.has(address2, initialNonce))
    }

    @Test
    fun `should reset one address without affecting others`() {
        val address1 = generateAddress()
        val address2 = generateAddress()

        queue.insert(address1, BigInteger.valueOf(100))
        queue.insert(address2, BigInteger.valueOf(200))
        queue.next(address1)
        queue.next(address2)

        queue.reset(address1)

        assertTrue(queue.isEmpty(address1))
        assertFalse(queue.isEmpty(address2))
    }

    @Test
    fun `should handle remove of exact head value`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)
        val head = queue.next(address) // head should be 101

        // Remove the head value
        queue.remove(address, head)

        // Head should move to value-1 (100)
        val nextVal = queue.next(address)
        assertEquals(head, nextVal)
    }

    @Test
    fun `should handle remove of value greater than head`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)
        val val1 = queue.next(address) // 101
        val val2 = queue.next(address) // 102

        // Remove a value greater than head (102 > 101, but head was just moved to 102)
        // This should silently do nothing as per design
        queue.remove(address, val2.inc()) // 103 (future value)

        // Next call should return 103 as normal
        val val3 = queue.next(address)
        assertEquals(val2.inc(), val3)
    }

    @Test
    fun `should handle remove of head when it's the only record`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)

        // Remove the only record (head = tail = 100)
        queue.remove(address, initialNonce)

        // Head should move to 99
        val nextVal = queue.next(address)
        assertEquals(initialNonce, nextVal)
    }

    @Test
    fun `should handle remove before insert`() {
        val address = generateAddress()
        val nonce = BigInteger.valueOf(500)

        // Try to remove a nonce that was never inserted
        queue.remove(address, nonce)

        // Should have no effect
        assertEquals(BigInteger.ONE, queue.next(address))
    }

    @Test
    fun `should handle remove of same value multiple times`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)
        queue.next(address)

        queue.remove(address, initialNonce)
        val head1 = repository.getHead(address)

        queue.remove(address, initialNonce)
        val head2 = repository.getHead(address)

        // Removing the same value twice should not cause further changes
        assertEquals(head1, head2)
    }

    @Test
    fun `should handle multiple consecutive removes`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)
        val val1 = queue.next(address) // 101
        val val2 = queue.next(address) // 102
        val val3 = queue.next(address) // 103

        // Remove multiple values in sequence
        queue.remove(address, val3)
        queue.remove(address, val2)
        queue.remove(address, val1)

        // Head should be at initialNonce (100) after all removes
        assertEquals(initialNonce, repository.getHead(address))
    }

    @Test
    fun `should handle markUsed on non-existent nonce`() {
        val address = generateAddress()

        // Should not throw, should be idempotent
        queue.markUsed(address, BigInteger.valueOf(999))

        // Should still be able to use the queue normally
        queue.insert(address, BigInteger.valueOf(100))
        val val1 = queue.next(address)
        assertEquals(BigInteger.valueOf(101), val1)
    }

    @Test
    fun `should handle markUsed on same nonce multiple times`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)

        queue.markUsed(address, initialNonce)
        queue.markUsed(address, initialNonce)
        queue.markUsed(address, initialNonce)

        // Should not cause issues
        val nonce = repository.getNonce(address, initialNonce)
        assertTrue(nonce!!.used)
    }

    @Test
    fun `should handle markUsed before insert`() {
        val address = generateAddress()
        val nonce = BigInteger.valueOf(100)

        // Try to mark as used before it exists
        queue.markUsed(address, nonce)

        // Should have no effect and queue should work normally
        queue.insert(address, nonce)
        val next = queue.next(address)
        assertEquals(nonce.inc(), next)
    }

    @Test
    fun `should handle zero as initial nonce`() {
        val address = generateAddress()
        val initialNonce = BigInteger.ZERO

        queue.insert(address, initialNonce)
        val next = queue.next(address)

        assertEquals(BigInteger.ONE, next)
    }

    @Test
    fun `should handle large BigInteger values`() {
        val address = generateAddress()
        val largeNonce = BigInteger("18446744073709551615") // Max uint64

        queue.insert(address, largeNonce)
        val next = queue.next(address)

        assertEquals(largeNonce.inc(), next)
    }

    @Test
    fun `should handle capacity with all used records`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)

        // Fill the queue
        val nonces = mutableListOf(initialNonce)
        repeat(capacity.toInt() - 1) {
            nonces.add(queue.next(address))
        }

        // Mark all as used
        nonces.forEach { queue.markUsed(address, it) }

        // Queue should be full with all records used
        assertTrue(nonces.all { queue.has(address, it) && repository.getNonce(address, it)!!.used })

        // Should throw when trying to get next (queue is full)
        assertThrows<NonceQueue.QueueOverflowException> { queue.next(address) }
    }

    @Test
    fun `should remove used records from tail to allow new requests`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)

        // Fill the queue, so there is one slot left for the next call
        // Notice that by inserting initial nocnce, we have already used one slot
        val nonces = mutableListOf(initialNonce)
        repeat(capacity.toInt() - 2) {
            nonces.add(queue.next(address))
        }

        // Mark oldest (tail) as used
        queue.markUsed(address, nonces[0])

        // Try to get next as tail cleanup happens
        val nextNonce = queue.next(address)
        assertTrue(nextNonce > nonces.last())
        assertTrue(repository.size(address) == capacity - 1)
    }

    @Test
    fun `should handle isEmpty() after insert`() {
        val address = generateAddress()

        queue.insert(address, BigInteger.valueOf(100))
        assertFalse(queue.isEmpty(address))
    }

    @Test
    fun `should handle isEmpty() on fresh address`() {
        val address = generateAddress()

        assertTrue(queue.isEmpty(address))
    }

    @Test
    fun `should handle isEmpty() after reset with new insert`() {
        val address = generateAddress()

        queue.insert(address, BigInteger.valueOf(100))
        queue.reset(address)

        assertTrue(queue.isEmpty(address))

        queue.insert(address, BigInteger.valueOf(200))
        assertFalse(queue.isEmpty(address))
    }

    @Test
    fun `should handle next on empty queue`() {
        val address = generateAddress()

        // Should return BigInteger.ONE on first call
        val nonce = queue.next(address)
        assertEquals(BigInteger.ONE, nonce)
    }

    @Test
    fun `should reuse all expired nonces in FIFO order`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)
        val nonces = mutableListOf(initialNonce)
        repeat(3) {
            nonces.add(queue.next(address))
        }

        // Wait for expiry
        runBlocking { delay(expiry + 1) }

        // Request new nonces - should reuse expired ones in order
        val reused1 = queue.next(address)
        assertEquals(nonces[0], reused1)

        val reused2 = queue.next(address)
        assertEquals(nonces[1], reused2)
    }

    @Test
    fun `should not return a nonce as marked used even after expiry`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)
        val next1 = queue.next(address)
        queue.markUsed(address, initialNonce)

        // Wait for expiry
        runBlocking { delay(expiry + 1) }

        // Get next - expiry handling should work
        val next2 = queue.next(address)
        assertTrue(next2 == next1)
        assertTrue(next2 != initialNonce)
    }

    @Test
    fun `should handle remove and refill cycle`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)
        val nonces = mutableListOf(initialNonce)
        repeat((capacity - 1).toInt()) {
            nonces.add(queue.next(address))
        }

        // Remove half of them
        nonces.take(nonces.size / 2).forEach { queue.remove(address, it) }

        // Should be able to get more nonces (reusing removed ones)
        repeat(2) {
            val nonce = queue.next(address)
            assertTrue(nonce >= initialNonce)
        }
    }

    @Test
    fun `should handle tail position when records are selectively removed`() {
        val address = generateAddress()
        val initialNonce = BigInteger.valueOf(100)

        queue.insert(address, initialNonce)
        val nonces = mutableListOf(initialNonce)
        // By inserting initial nonce, we have already used one slot
        repeat(capacity.toInt() - 2) {
            nonces.add(queue.next(address))
        }

        // Remove first nonce
        queue.remove(address, nonces[0])
        val headAfterRemove = repository.getHead(address)

        // After removing nonce, head should adjust
        assertTrue(headAfterRemove == initialNonce.dec())
    }

    @Test
    fun `should return false for has() on non-existent nonce`() {
        val address = generateAddress()

        assertFalse(queue.has(address, BigInteger.valueOf(999)))
    }

    @Test
    fun `should return true for has() on inserted nonce`() {
        val address = generateAddress()
        val nonce = BigInteger.valueOf(100)

        queue.insert(address, nonce)
        assertTrue(queue.has(address, nonce))
    }

    @Test
    fun `should return false for has() after remove`() {
        val address = generateAddress()
        val nonce = BigInteger.valueOf(100)

        queue.insert(address, nonce)
        queue.remove(address, nonce)

        assertFalse(queue.has(address, nonce))
    }
}

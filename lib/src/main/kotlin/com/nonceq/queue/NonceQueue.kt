package com.nonceq.queue

import java.math.BigInteger
import java.time.Instant

import mu.KotlinLogging


/**
 * Circular Queue implementation for incremental nonce values
 *
 * Circular Queue structure contains three variables
 *  - `head`: cursor indicates the most recent record in the queue
 *  - `tail`: cursor indicates the oldest record in the queue
 *  - `queue`: mapping(nonce value => nonce record), keeps incremental nonce records
 *
 *  ```
 *  fifo queue with capacity n
 *  __________________________________
 *  |  k  | k+1 | .. | .. | .. |k+n-1|
 *  __________________________________
 *    ^                           ^
 *   tail                        head
 *  ```
 *
 * And there is only one queue for every different address space
 */
class NonceQueue(
    private val repository: NonceQueueRepository,
    private val capacity: Long,
    private val expiry: Long = 10_000L,
) {
    private val logger = KotlinLogging.logger {}

    class QueueOverflowException(message: String) : IllegalStateException(message)

    /**
     * Inserts [value], and sets head and tail to [value].
     *
     * This should be used only for initialization,
     * otherwise queue can behave unexpectedly!
     */
    fun insert(address: String, value: BigInteger) {
        if (!has(address, value)) {
            repository.setHead(address, value)
            repository.setTail(address, value)

            // Create a new nonce record
            repository.putNonce(
                address,
                value,
                Nonce(value.toString(), false, Instant.now().toEpochMilli()),
            )

            logger.debug { "Inserting initial nonce: $value for $address" }
        }
    }

    /**
     * Returns the next value from the queue.
     * Also, it will try to remove the tail in each iteration.
     *
     * ```
     *  Iteration works as follows;
     *
     *  Let's say there are skipped values in slots k+2 and k+4,
     *  and the head is at k+1.
     *  ______________________________________________
     *  |  k  | k+1 | empty | k+3 | empty | .. |k+n-1|
     *  ______________________________________________
     *    ^      ^
     *   tail   head
     *
     *  if we request the next value, k+2 will be returned
     *  and the head will move to k+2.
     *  ____________________________________________
     *  |  k  | k+1 | k+2 | k+3 | empty | .. |k+n-1|
     *  ____________________________________________
     *    ^            ^
     *   tail         head
     *
     *  However the next value is already in use and the one after
     *  the next value is available. And if we request a next value,
     *  k+3 will be skipped due to the fact that it is already in use.
     *  The head will move to k+3. And it will iterate to the next slot
     *  until it find an empty one.
     *  ____________________________________________
     *  |  k  | k+1 | k+2 | k+3 | empty | .. |k+n-1|
     *  ____________________________________________
     *    ^                 ^
     *   tail              head
     *
     *  And finally it will return the empty slot's value
     *  ____________________________________________
     *  |  k  | k+1 | k+2 | k+3 | empty | .. |k+n-1|
     *  ____________________________________________
     *    ^                        ^
     *   tail                     head
     * ```
     *
     * If the queue has already reached its capacity,
     * throws [QueueOverflowException].
     */
    fun next(address: String): BigInteger {
        // If the queue size reaches the max capacity,
        // do not move any forward (don't accept any request),
        // instead throw an exception which should be handled in the caller.
        while (!isFull(address)) {
            // Remove the tail if it is used which
            // prevents the queue from growing indefinitely.
            // OR remove expired record from the tail.
            removeTail(address)

            // Move cursor to the next value
            val head = repository.getHead(address).inc()
            repository.setHead(address, head)

            // If there is no record at the head, or it's expired, use it
            if (
                !has(address, head)
                || repository.getNonce(address, head)!!.isExpired(expiry)
            ) {
                // Create a new nonce record
                repository.putNonce(
                    address,
                    head,
                    Nonce(head.toString(), false, Instant.now().toEpochMilli()),
                )

                logger.debug { "Next nonce: $head for $address" }
                return head
            }

            // If there is a record at the head which is not expired, then iterate to the next value.
            // Iteration will find an unused value (with no record) or
            // it will throw a QueueOverflowException when it hits the capacity
        }

        throw QueueOverflowException(
            "nonce queue is full for address: $address with capacity: $capacity",
        )
    }

    /**
     * Removes [value] from the queue and sets head to the value
     * right before [value] so that the removed [value] can be used
     * again in the next iteration/s.
     *
     * ```
     *  __________________________________
     *  |  k  | k+1 | .. | .. | .. |k+n-1|
     *  __________________________________
     *    ^                           ^
     *   tail                        head
     *
     *  if the value k+2 is removed, then head moves to k+1.
     *  ____________________________________________
     *  |  k  | k+1 | empty | k+3 | k+4 | .. |k+n-1|
     *  ____________________________________________
     *    ^      ^
     *   tail   head
     *
     *  In the next iteration/s, k+2 will be available again.
     * ```
     *
     * Notice that if there is removed value/s with a lower number,
     * then it won't update the head so that the lower value/s can be used first.
     * ```
     *  For example, with the above case, if the value k+4, also, is removed,
     *  then head won't move to slot k+3, instead it will stay at k+1
     *  because there is already empty slot with a lower value which is k+2.
     *  ______________________________________________
     *  |  k  | k+1 | empty | k+3 | empty | .. |k+n-1|
     *  _____________________________________________
     *    ^      ^
     *   tail   head
     * ```
     */
    fun remove(address: String, value: BigInteger) {
        if (has(address, value)) {
            logger.debug { "Discarding nonce: $value for $address" }

            repository.deleteNonce(address, value)

            var head = repository.getHead(address)

            if (head >= value) {
                // Move the head to the value right before the removed value
                // so that, in the next iteration/s, the removed value can be used again
                head = value.dec()
                repository.setHead(address, head)

                logger.debug { "Moving head to: $head for $address" }
            }
            // If the head has already been moved to a lower value previously
            // (i.e. skipped nonce with lower value), then do not update the head.
        }
    }

    /**
     * Marks [value] as used for later removal from the tail.
     */
    fun markUsed(address: String, value: BigInteger) {
        if (has(address, value)) {
            val nonce = repository.getNonce(address, value)
            nonce!!.used = true
            repository.putNonce(address, value, nonce)
        }
    }

    /**
     * Checks if the queue is empty.
     *
     * Notice that size of zero doesn't always mean that the queue is empty.
     * Even though size is zero, head cursor can point a next value.
     * For example if a value is requested and then removed, assuming
     * that the tail is the previous value (requested value - 1) which
     * will be removed upon requesting the next value, then he head will
     * move to previous value, although there is no record in the queue.
     */
    fun isEmpty(address: String): Boolean {
        val head = repository.getHead(address)
        return repository.size(address) == 0L && head == BigInteger.ZERO
    }

    /**
     * Removes all the records from the queue and reset head and tail cursors
     */
    fun reset(address: String) {
        repository.clear(address)
        logger.debug { "Resetting nonce queue for $address" }
    }

    /**
     * Checks if there is a record with the given [value]
     */
    fun has(address: String, value: BigInteger): Boolean {
        return repository.getNonce(address, value) != null
    }

    /**
     * Removes the tail recursively if it is marked as used,
     * not to grow the queue indefinitely.
     * OR removes an expired record from the tail(not recursively).
     */
    private fun removeTail(address: String) {
        while (true) {
            val tail = repository.getTail(address)
            val tailNonce = repository.getNonce(address, tail) ?: return

            // Remove the tail if it's already used
            if (tailNonce.used) {
                logger.debug {
                    "Removing tail nonce: ${tailNonce.value} for $address"
                }
                repository.deleteNonce(address, tail)
                repository.setTail(address, tail.inc())

                // Remove tail till head
                if (tail < repository.getHead(address)) {
                    continue
                }
            }
            // If not used but expired, remove the record at the tail slot to use it again.
            // Notice that expired means the record is not used either discarded
            // in the given period of time. So we have to discard it by force
            // with the assumption that it's not submitted to blockchain.
            else if (tailNonce.isExpired(expiry)) {
                logger.debug {
                    "Expired nonce: ${tailNonce.value} for $address"
                }
                remove(address, tail)
                // Do not update tail cursor, because we are just
                // emptying a slot to use in the next iteration.
            }

            break
        }
    }

    private fun isFull(address: String): Boolean {
        return repository.size(address) == capacity
    }
}

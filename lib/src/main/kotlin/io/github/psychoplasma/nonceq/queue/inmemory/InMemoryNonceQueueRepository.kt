package io.github.psychoplasma.nonceq.queue.inmemory

import io.github.psychoplasma.nonceq.queue.Nonce
import io.github.psychoplasma.nonceq.queue.NonceQueueRepository

import java.math.BigInteger

/**
 * In-memory implementation of [NonceQueueRepository] using a mutable list
 */
public class InMemoryNonceQueueRepository : NonceQueueRepository {
    private data class Queue(
        var head: BigInteger,
        var tail: BigInteger,
        var queue: HashMap<String, Nonce>,
    )

    private val queues = hashMapOf<String, Queue>()

    override fun getHead(address: String): BigInteger {
        return queues[address]?.head ?: BigInteger.ZERO
    }

    override fun setHead(address: String, value: BigInteger) {
        if (queues[address] == null) {
            queues[address] = Queue(
                BigInteger.ZERO,
                BigInteger.ZERO,
                hashMapOf(),
            )
        }
        queues[address]?.head = value
    }

    override fun getTail(address: String): BigInteger {
        return queues[address]?.tail ?: BigInteger.ZERO
    }

    override fun setTail(address: String, value: BigInteger) {
        if (queues[address] == null) {
            queues[address] = Queue(
                BigInteger.ZERO,
                BigInteger.ZERO,
                hashMapOf(),
            )
        }
        queues[address]?.tail = value
    }

    override fun getNonce(address: String, value: BigInteger): Nonce? {
        return queues[address]?.queue?.get(value.toString())
    }

    override fun putNonce(address: String, value: BigInteger, nonce: Nonce) {
        if (queues[address] == null) {
            queues[address] = Queue(
                BigInteger.ZERO,
                BigInteger.ZERO,
                hashMapOf(),
            )
        }
        queues[address]?.queue?.put(value.toString(), nonce)
    }

    override fun deleteNonce(address: String, value: BigInteger) {
        queues[address]?.queue?.remove(value.toString())
    }

    override fun size(address: String): Long {
        return queues[address]?.queue?.size?.toLong() ?: 0L
    }

    override fun clear(address: String) {
        queues[address]?.head = BigInteger.ZERO
        queues[address]?.tail = BigInteger.ZERO
        queues[address]?.queue?.clear()
    }
}

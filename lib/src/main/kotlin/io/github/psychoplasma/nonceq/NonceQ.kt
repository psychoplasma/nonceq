package io.github.psychoplasma.nonceq

import io.github.psychoplasma.nonceq.queue.NonceQueue
import io.github.psychoplasma.nonceq.queue.NonceQueueManager
import io.github.psychoplasma.nonceq.queue.NonceQueueRepository
import io.github.psychoplasma.nonceq.queue.inmemory.InMemoryNonceQueueRepository
import io.github.psychoplasma.nonceq.utils.BlockNonceProvider

import java.math.BigInteger

/**
 * NonceQ is the main entry point for the nonce queue management library.
 *
 * It provides a simple API to create and manage nonce queues for different
 * storage backends (in-memory, Redis, etc.).
 *
 * Example usage:
 * ```kotlin
 * val nonceManager = NonceQ.builder()
 *     .withInMemoryRepository()
 *     .withBlockNonceProvider(web3jProvider)
 *     .build()
 *
 * val nonce = nonceManager.getNextValidNonce("0x1234...")
 * nonceManager.useNonce("0x1234...", nonce)
 * ```
 */
public object NonceQ {
    /**
     * Creates a new builder for configuring NonceQ
     */
    public fun builder(): NonceQBuilder = NonceQBuilder()
}

/**
 * Builder for creating configured [NonceManager] instances
 */
public class NonceQBuilder {
    private var repository: NonceQueueRepository? = null
    private var blockNonceProvider: BlockNonceProvider? = null
    private var queueCapacity: Long = 100L
    private var nonceExpiry: Long = 10_000L

    /**
     * Sets the repository to use in-memory storage
     */
    public fun withInMemoryRepository(): NonceQBuilder {
        this.repository = InMemoryNonceQueueRepository()
        return this
    }

    /**
     * Sets a custom repository implementation
     */
    public fun withRepository(repository: NonceQueueRepository): NonceQBuilder {
        this.repository = repository
        return this
    }

    /**
     * Sets the repository to use Redis storage
     */
    public fun withRedisRepository(jedisPool: redis.clients.jedis.JedisPool): NonceQBuilder {
        this.repository = io.github.psychoplasma.nonceq.queue.redis.RedisNonceQueueRepository(jedisPool)
        return this
    }

    /**
     * Sets the block nonce provider (required)
     */
    public fun withBlockNonceProvider(provider: BlockNonceProvider): NonceQBuilder {
        this.blockNonceProvider = provider
        return this
    }

    /**
     * Sets the maximum capacity of the nonce queue
     * Default: 100
     */
    public fun withQueueCapacity(capacity: Long): NonceQBuilder {
        this.queueCapacity = capacity
        return this
    }

    /**
     * Sets the expiry time for unused nonces in milliseconds
     * Default: 10,000 ms (10 seconds)
     */
    public fun withNonceExpiry(expiryMs: Long): NonceQBuilder {
        this.nonceExpiry = expiryMs
        return this
    }

    /**
     * Builds the NonceManager instance
     */
    public fun build(): NonceManager {
        val repo = repository ?:
            throw IllegalStateException("Repository must be configured")
        val provider = blockNonceProvider ?:
            throw IllegalStateException("BlockNonceProvider must be configured")

        val queue = NonceQueue(repo, queueCapacity, nonceExpiry)
        return NonceQueueManager(provider, queue)
    }
}

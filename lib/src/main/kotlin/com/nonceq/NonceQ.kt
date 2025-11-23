package com.nonceq

import com.nonceq.queue.NonceQueue
import com.nonceq.queue.NonceQueueManager
import com.nonceq.queue.NonceQueueRepository
import com.nonceq.queue.inmemory.InMemoryNonceQueueRepository
import com.nonceq.utils.BlockNonceProvider

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
object NonceQ {
    /**
     * Creates a new builder for configuring NonceQ
     */
    fun builder(): NonceQBuilder = NonceQBuilder()
}

/**
 * Builder for creating configured [NonceManager] instances
 */
class NonceQBuilder {
    private var repository: NonceQueueRepository? = null
    private var blockNonceProvider: BlockNonceProvider? = null
    private var queueCapacity: Long = 100L
    private var nonceExpiry: Long = 10_000L

    /**
     * Sets the repository to use in-memory storage
     */
    fun withInMemoryRepository(): NonceQBuilder {
        this.repository = InMemoryNonceQueueRepository()
        return this
    }

    /**
     * Sets a custom repository implementation
     */
    fun withRepository(repository: NonceQueueRepository): NonceQBuilder {
        this.repository = repository
        return this
    }

    /**
     * Sets the block nonce provider (required)
     */
    fun withBlockNonceProvider(provider: BlockNonceProvider): NonceQBuilder {
        this.blockNonceProvider = provider
        return this
    }

    /**
     * Sets the maximum capacity of the nonce queue
     * Default: 100
     */
    fun withQueueCapacity(capacity: Long): NonceQBuilder {
        this.queueCapacity = capacity
        return this
    }

    /**
     * Sets the expiry time for unused nonces in milliseconds
     * Default: 10,000 ms (10 seconds)
     */
    fun withNonceExpiry(expiryMs: Long): NonceQBuilder {
        this.nonceExpiry = expiryMs
        return this
    }

    /**
     * Builds the NonceManager instance
     */
    fun build(): NonceManager {
        val repo = repository ?:
            throw IllegalStateException("Repository must be configured")
        val provider = blockNonceProvider ?:
            throw IllegalStateException("BlockNonceProvider must be configured")

        val queue = NonceQueue(repo, queueCapacity, nonceExpiry)
        return NonceQueueManager(provider, queue)
    }
}

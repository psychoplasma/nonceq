/**
 * Public exports for library consumers
 *
 * Import from this package to access all public APIs
 */
package io.github.psychoplasma.nonceq

// Core entry point
public typealias NonceQLibrary = NonceQ

// Re-export all public interfaces and classes
public typealias IBlockNonceProvider = io.github.psychoplasma.nonceq.utils.BlockNonceProvider
public typealias INonceQueueRepository = io.github.psychoplasma.nonceq.queue.NonceQueueRepository
public typealias INonceManager = NonceManager

// Data classes
public typealias NonceRecord = io.github.psychoplasma.nonceq.queue.Nonce

// Implementations
public typealias InMemoryRepository = io.github.psychoplasma.nonceq.queue.inmemory.InMemoryNonceQueueRepository
public typealias RedisRepository = io.github.psychoplasma.nonceq.queue.redis.RedisNonceQueueRepository

// Builder
public typealias Manager = NonceQBuilder

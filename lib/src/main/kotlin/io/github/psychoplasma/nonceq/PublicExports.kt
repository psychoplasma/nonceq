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
public typealias INonceManager = io.github.psychoplasma.nonceq.NonceManager

// Data classes
public typealias NonceRecord = io.github.psychoplasma.nonceq.queue.Nonce
// public typealias isExpired = io.github.psychoplasma.nonceq.queue.Nonce.isExpired

// Implementations
public typealias InMemoryRepository = io.github.psychoplasma.nonceq.queue.inmemory.InMemoryNonceQueueRepository
public typealias RedisRepository = io.github.psychoplasma.nonceq.queue.redis.RedisNonceQueueRepository
public typealias QueueManager = io.github.psychoplasma.nonceq.queue.NonceQueueManager

// Builder
public typealias Manager = NonceQBuilder

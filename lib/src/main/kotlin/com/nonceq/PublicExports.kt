/**
 * Public exports for library consumers
 *
 * Import from this package to access all public APIs
 */
package com.nonceq

// Core entry point
public typealias NonceQLibrary = NonceQ

// Re-export all public interfaces and classes
public typealias IBlockNonceProvider = com.nonceq.utils.BlockNonceProvider
public typealias INonceQueueRepository = com.nonceq.queue.NonceQueueRepository
public typealias INonceManager = com.nonceq.NonceManager

// Data classes
public typealias NonceRecord = com.nonceq.queue.Nonce
// public typealias isExpired = com.nonceq.queue.Nonce.isExpired

// Implementations
public typealias InMemoryRepository = com.nonceq.queue.inmemory.InMemoryNonceQueueRepository
public typealias RedisRepository = com.nonceq.queue.redis.RedisNonceQueueRepository
public typealias QueueManager = com.nonceq.queue.NonceQueueManager

// Builder
public typealias Manager = NonceQBuilder

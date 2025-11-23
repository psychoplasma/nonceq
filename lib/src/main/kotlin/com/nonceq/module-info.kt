/**
 * NonceQ Library - Main Module
 *
 * This module provides a production-ready nonce queue management system for blockchain applications.
 *
 * ## Quick Start
 *
 * ```kotlin
 * // Initialize
 * val nonceManager = NonceQ.builder()
 *     .withInMemoryRepository()
 *     .withBlockNonceProvider(web3jProvider)
 *     .build()
 *
 * // Use
 * val nonce = nonceManager.getNextValidNonce("0x...")
 * nonceManager.useNonce("0x...", nonce)
 * ```
 *
 * ## Main Components
 *
 * ### Entry Points
 * - `NonceQ` - Factory for creating NonceManager instances
 * - `NonceQBuilder` - Builder for configuration
 *
 * ### Core Interfaces
 * - `NonceManager` - Main interface for nonce management
 * - `NonceQueueRepository` - Storage abstraction
 * - `BlockNonceProvider` - Blockchain nonce provider
 *
 * ### Implementations
 * - `InMemoryNonceQueueRepository` - In-memory storage backend
 * - `NonceQueueManager` - Main NonceManager implementation
 * - `Web3jBlockNonceProvider` - Web3j-based nonce provider
 *
 * ### Data Classes
 * - `Nonce` - Nonce record with metadata
 *
 * ## Features
 *
 * - Thread-safe nonce management
 * - Automatic nonce expiry and reuse
 * - Multiple storage backends (in-memory, Redis, etc.)
 * - Circular queue with capacity management
 * - Transaction tracking with error handling
 *
 * ## Documentation
 *
 * For detailed documentation, see `README.md`
 *
 * @since 1.0.0
 */
package com.nonceq

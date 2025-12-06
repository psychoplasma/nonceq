# NonceQ

Nonce queuing manager for nonce-based transactions (e.g., Ethereum). It provides a simple LRU queue with reuse and discarding features to manage nonces sequentially, even with concurrent requests.

## Features

- **Sequential Nonce Management**: Ensures nonces are used in order.
- **Concurrency Support**: Handles concurrent requests for the same address.
- **Backend Agnostic**: Comes with In-Memory and Redis implementations.
- **Recovery**: Automatically recovers from "nonce too low" errors by resetting the queue.
- **Expiry**: Automatically recycles unused nonces after a configurable timeout.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.psychoplasma.nonceq:nonceq:0.1.0")
    // If using Redis backend
    implementation("redis.clients:jedis:5.1.0")
}
```

## Usage

### Initialization

You need a `BlockNonceProvider` to fetch the current nonce from the blockchain when initializing the queue, also after reseting the queue.

Web3j-backed nonce provider comes out-of-the-box.

```kotlin
import org.web3j.protocol.Web3j

val web3j = Web3j.build(HttpService("https://..."))

NonceQ.builder()
    .withWeb3jBlockNonceProvider(web3j)
    ...
```

Also, you can bring your own nonce provider by implementing `io.github.psychoplasma.nonceq.utils.BlockNonceProvider` interface.

```kotlin
class CustomBlockNonceProvider : BlockNonceProvider {
    override fun getBlockNonce(address: String): BigInteger {
        // Fetch latest nonce for the given address with your custom logic
        ...
        return latestNonce
    }
}

...

NonceQ.builder()
    .withBlockNonceProvider(CustomBlockNonceProvider())
    ...
```

### In-Memory Backend

Suitable for single-instance applications.

```kotlin
val nonceManager = NonceQ.builder()
    .withInMemoryRepository()
    .withWeb3jBlockNonceProvider(web3j)
    .withQueueCapacity(100)
    .withNonceExpiry(10_000) // 10 seconds
    .build()
```

### Redis Backend

Suitable for distributed applications or when persistence is required.

```kotlin
val jedisPool = JedisPool("localhost", 6379)

val nonceManager = NonceQ.builder()
    .withRedisRepository(jedisPool)
    .withBlockNonceProvider(web3j)
    .build()
```

> **Note**: While Redis provides shared storage, strict sequentiality across multiple concurrent instances requires distributed locking, which is not currently built-in.

### Getting and Using Nonces

```kotlin
val address = "0x123..."

try {
    // Get the next valid nonce
    val nonce = nonceManager.getNextValidNonce(address)
    
    // Use it in a transaction...
    val txHash = sendTransaction(address, nonce)
    
    // Mark as used (so it can be cleared from the queue)
    nonceManager.useNonce(address, nonce, txHash)
    
} catch (e: Exception) {
    // If transaction failed(like not making to blockchain at all), discard the nonce so it can be reused
    nonceManager.discardNonce(address, nonce, e.message)
}
```

## Development

### Building

```bash
./gradlew build
```

### Testing

Run unit and integration tests:

```bash
./gradlew test
```

Redis tests use Testcontainers and require a Docker environment.

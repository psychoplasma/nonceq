package io.github.psychoplasma.nonceq

import io.github.psychoplasma.nonceq.queue.inmemory.InMemoryNonceQueueRepository
import io.github.psychoplasma.nonceq.utils.BlockNonceProvider
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.web3j.protocol.Web3j
import redis.clients.jedis.JedisPool
import java.math.BigInteger

class NonceQTest {

    @Test
    fun `builder should return a new builder instance`() {
        val builder = NonceQ.builder()
        assertNotNull(builder)
    }

    @Test
    fun `build should throw exception when repository is not configured`() {
        val builder = NonceQ.builder()
            .withBlockNonceProvider(object : BlockNonceProvider {
                override fun getBlockNonce(address: String): BigInteger = BigInteger.ZERO
            })

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `build should throw exception when provider is not configured`() {
        val builder = NonceQ.builder()
            .withInMemoryRepository()

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `build should create NonceManager with in-memory repository`() {
        val manager = NonceQ.builder()
            .withInMemoryRepository()
            .withBlockNonceProvider(object : BlockNonceProvider {
                override fun getBlockNonce(address: String): BigInteger = BigInteger.ZERO
            })
            .build()

        assertNotNull(manager)
    }

    @Test
    fun `build should create NonceManager with redis repository`() {
        val jedisPool = mockk<JedisPool>()
        val manager = NonceQ.builder()
            .withRedisRepository(jedisPool)
            .withBlockNonceProvider(object : BlockNonceProvider {
                override fun getBlockNonce(address: String): BigInteger = BigInteger.ZERO
            })
            .build()

        assertNotNull(manager)
    }

    @Test
    fun `build should create NonceManager with Web3j nonce provider`() {
        val web3j = mockk<Web3j>()
        val manager = NonceQ.builder()
            .withInMemoryRepository()
            .withWeb3jBlockNonceProvider(web3j)
            .build()

        assertNotNull(manager)
    }

    @Test
    fun `build should create NonceManager with custom repository and custom block nonce provider`() {
        val customRepo = InMemoryNonceQueueRepository()
        val manager = NonceQ.builder()
            .withRepository(customRepo)
            .withBlockNonceProvider(object : BlockNonceProvider {
                override fun getBlockNonce(address: String): BigInteger = BigInteger.ZERO
            })
            .build()

        assertNotNull(manager)
    }

    @Test
    fun `build should create NonceManager with custom capacity and expiry`() {
        val manager = NonceQ.builder()
            .withInMemoryRepository()
            .withBlockNonceProvider(object : BlockNonceProvider {
                override fun getBlockNonce(address: String): BigInteger = BigInteger.ZERO
            })
            .withQueueCapacity(50)
            .withNonceExpiry(5000)
            .build()

        assertNotNull(manager)
    }
}

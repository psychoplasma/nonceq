package com.nonceq.queue.redis

import com.nonceq.queue.Nonce
import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import redis.clients.jedis.JedisPool
import java.math.BigInteger

@Testcontainers
class RedisNonceQueueRepositoryTest {

    companion object {
        @Container
        val redis = RedisContainer(DockerImageName.parse("redis:7.2.4-alpine"))
    }

    private lateinit var jedisPool: JedisPool
    private lateinit var repository: RedisNonceQueueRepository

    @BeforeEach
    fun setUp() {
        jedisPool = JedisPool(redis.redisURI)
        repository = RedisNonceQueueRepository(jedisPool)
    }

    @AfterEach
    fun tearDown() {
        jedisPool.resource.use { it.flushAll() }
        jedisPool.close()
    }

    @Test
    fun `should set and get head`() {
        val address = "0x123"
        val head = BigInteger.valueOf(100)

        repository.setHead(address, head)
        assertEquals(head, repository.getHead(address))
    }

    @Test
    fun `should return zero for non-existent head`() {
        assertEquals(BigInteger.ZERO, repository.getHead("0x404"))
    }

    @Test
    fun `should set and get tail`() {
        val address = "0x123"
        val tail = BigInteger.valueOf(50)

        repository.setTail(address, tail)
        assertEquals(tail, repository.getTail(address))
    }

    @Test
    fun `should return zero for non-existent tail`() {
        assertEquals(BigInteger.ZERO, repository.getTail("0x404"))
    }

    @Test
    fun `should put, get and delete nonce`() {
        val address = "0x123"
        val value = BigInteger.valueOf(100)
        val nonce = Nonce("100", false, 123456789L)

        repository.putNonce(address, value, nonce)

        val retrieved = repository.getNonce(address, value)
        assertNotNull(retrieved)
        assertEquals(nonce.value, retrieved?.value)
        assertEquals(nonce.used, retrieved?.used)
        assertEquals(nonce.insertedAt, retrieved?.insertedAt)

        repository.deleteNonce(address, value)
        assertNull(repository.getNonce(address, value))
    }

    @Test
    fun `should return correct size`() {
        val address = "0x123"
        
        assertEquals(0L, repository.size(address))

        repository.putNonce(address, BigInteger.valueOf(1), Nonce("1", false, 0))
        repository.putNonce(address, BigInteger.valueOf(2), Nonce("2", false, 0))

        assertEquals(2L, repository.size(address))

        repository.deleteNonce(address, BigInteger.valueOf(1))
        assertEquals(1L, repository.size(address))
    }

    @Test
    fun `should clear all data for address`() {
        val address = "0x123"
        val otherAddress = "0x456"

        repository.setHead(address, BigInteger.TEN)
        repository.setTail(address, BigInteger.ONE)
        repository.putNonce(address, BigInteger.TEN, Nonce("10", false, 0))

        repository.setHead(otherAddress, BigInteger.TEN)

        repository.clear(address)

        assertEquals(BigInteger.ZERO, repository.getHead(address))
        assertEquals(BigInteger.ZERO, repository.getTail(address))
        assertEquals(0L, repository.size(address))
        assertNull(repository.getNonce(address, BigInteger.TEN))

        // Should not affect other address
        assertEquals(BigInteger.TEN, repository.getHead(otherAddress))
    }
}

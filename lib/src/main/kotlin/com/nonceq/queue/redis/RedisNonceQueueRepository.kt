package com.nonceq.queue.redis

import com.nonceq.queue.Nonce
import com.nonceq.queue.NonceQueueRepository
import redis.clients.jedis.JedisPool
import java.math.BigInteger

class RedisNonceQueueRepository(
    private val jedisPool: JedisPool,
    private val keyPrefix: String = "nonceq"
) : NonceQueueRepository {

    override fun getHead(address: String): BigInteger {
        jedisPool.resource.use { jedis ->
            val head = jedis.get(headKey(address))
            return if (head != null) BigInteger(head) else BigInteger.ZERO
        }
    }

    override fun setHead(address: String, value: BigInteger) {
        jedisPool.resource.use { jedis ->
            jedis.set(headKey(address), value.toString())
        }
    }

    override fun getTail(address: String): BigInteger {
        jedisPool.resource.use { jedis ->
            val tail = jedis.get(tailKey(address))
            return if (tail != null) BigInteger(tail) else BigInteger.ZERO
        }
    }

    override fun setTail(address: String, value: BigInteger) {
        jedisPool.resource.use { jedis ->
            jedis.set(tailKey(address), value.toString())
        }
    }

    override fun getNonce(address: String, value: BigInteger): Nonce? {
        jedisPool.resource.use { jedis ->
            val nonceStr = jedis.hget(queueKey(address), value.toString())
            return if (nonceStr != null) parseNonce(nonceStr) else null
        }
    }

    override fun putNonce(address: String, value: BigInteger, nonce: Nonce) {
        jedisPool.resource.use { jedis ->
            jedis.hset(queueKey(address), value.toString(), serializeNonce(nonce))
        }
    }

    override fun deleteNonce(address: String, value: BigInteger) {
        jedisPool.resource.use { jedis ->
            jedis.hdel(queueKey(address), value.toString())
        }
    }

    override fun size(address: String): Long {
        jedisPool.resource.use { jedis ->
            return jedis.hlen(queueKey(address))
        }
    }

    override fun clear(address: String) {
        jedisPool.resource.use { jedis ->
            val pipeline = jedis.pipelined()
            pipeline.del(headKey(address))
            pipeline.del(tailKey(address))
            pipeline.del(queueKey(address))
            pipeline.sync()
        }
    }

    private fun headKey(address: String) = "$keyPrefix:$address:head"
    private fun tailKey(address: String) = "$keyPrefix:$address:tail"
    private fun queueKey(address: String) = "$keyPrefix:$address:queue"

    // Simple serialization for Nonce object
    // Format: value:used:insertedAt
    private fun serializeNonce(nonce: Nonce): String {
        return "${nonce.value}:${nonce.used}:${nonce.insertedAt}"
    }

    private fun parseNonce(str: String): Nonce {
        val parts = str.split(":")
        return Nonce(parts[0], parts[1].toBoolean(), parts[2].toLong())
    }
}

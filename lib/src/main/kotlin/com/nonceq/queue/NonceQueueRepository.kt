package com.nonceq.queue

import java.math.BigInteger

interface NonceQueueRepository {
    fun getHead(address: String): BigInteger

    fun setHead(address: String, value: BigInteger)

    fun getTail(address: String): BigInteger

    fun setTail(address: String, value: BigInteger)

    fun getNonce(address: String, value: BigInteger): Nonce?

    fun putNonce(address: String, value: BigInteger, nonce: Nonce)

    fun deleteNonce(address: String, value: BigInteger)

    fun size(address: String): Long

    fun clear(address: String)
}

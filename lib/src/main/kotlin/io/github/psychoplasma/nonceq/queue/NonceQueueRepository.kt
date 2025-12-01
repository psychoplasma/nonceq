package io.github.psychoplasma.nonceq.queue

import java.math.BigInteger

public interface NonceQueueRepository {
    public fun getHead(address: String): BigInteger

    public fun setHead(address: String, value: BigInteger)

    public fun getTail(address: String): BigInteger

    public fun setTail(address: String, value: BigInteger)

    public fun getNonce(address: String, value: BigInteger): Nonce?

    public fun putNonce(address: String, value: BigInteger, nonce: Nonce)

    public fun deleteNonce(address: String, value: BigInteger)

    public fun size(address: String): Long

    public fun clear(address: String)
}

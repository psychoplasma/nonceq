package io.github.psychoplasma.nonceq

import java.math.BigInteger


public interface NonceManager {
    public fun getNextValidNonce(address: String): BigInteger

    public fun useNonce(address: String, nonce: BigInteger, txId: String? = null)

    public fun discardNonce(address: String, nonce: BigInteger, errorMessage: String? = null)
}

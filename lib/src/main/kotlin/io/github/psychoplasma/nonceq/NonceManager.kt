package io.github.psychoplasma.nonceq

import java.math.BigInteger


interface NonceManager {
    fun getNextValidNonce(address: String): BigInteger

    fun useNonce(address: String, nonce: BigInteger, txId: String? = null)

    fun discardNonce(address: String, nonce: BigInteger, errorMessage: String? = null)
}

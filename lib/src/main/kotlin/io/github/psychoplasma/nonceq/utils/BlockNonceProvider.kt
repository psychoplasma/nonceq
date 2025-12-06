package io.github.psychoplasma.nonceq.utils

import java.math.BigInteger


public interface BlockNonceProvider {
    /**
     * Gets the current block nonce for the given address
     */
    public fun getBlockNonce(address: String): BigInteger;
}

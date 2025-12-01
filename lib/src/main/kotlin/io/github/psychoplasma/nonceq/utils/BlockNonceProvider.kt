package io.github.psychoplasma.nonceq.utils

import java.math.BigInteger


public interface BlockNonceProvider {
    public fun getBlockNonce(address: String): BigInteger;
}

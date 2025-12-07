package io.github.psychoplasma.nonceq

import java.math.BigInteger


public interface NonceManager {
    /*
     * Gets the next valid nonce for the given address.
     * If the nonce queue is empty, it fetches the nonce from the BlockNonceProvider.
     * Thread-safe to call from multiple coroutines.
     */
    public fun getNextValidNonce(address: String): BigInteger

    /*
     * Marks the given nonce as used for the specified address.
     * Optionally, a transaction ID can be provided to associate with the nonce.
     * The nonce will be removed from the queue upon calling [NonceManager.getNextValidNonce].
     * Thread-safe to call from multiple coroutines.
     */
    public fun useNonce(address: String, nonce: BigInteger, txId: String? = null)

    /*
     * Discards the given nonce for the specified address, optionally providing an error message.
     */
    public fun discardNonce(address: String, nonce: BigInteger, errorMessage: String? = null)

    /*
     * Resets the nonce queue for the specified address.
     */
    public fun reset(address: String)
}

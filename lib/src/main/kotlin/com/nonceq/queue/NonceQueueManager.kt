package com.nonceq.queue

import com.nonceq.NonceManager
import com.nonceq.utils.BlockNonceProvider

import java.math.BigInteger
import java.util.concurrent.Semaphore


class NonceQueueManager(
    private val blockNonceProvider: BlockNonceProvider,
    private val nonceQueue: NonceQueue,
) : NonceManager {
    private val semaphore = Semaphore(1)

    override fun getNextValidNonce(address: String): BigInteger {
        semaphore.acquire()
        try {
            val neutralizedAddress = address.lowercase()

            // Initialize with block nonce if empty
            if (nonceQueue.isEmpty(neutralizedAddress)) {
                val blockNonce = blockNonceProvider.getBlockNonce(
                    neutralizedAddress,
                )
                nonceQueue.insert(neutralizedAddress, blockNonce)
                return blockNonce
            }

            return nonceQueue.next(neutralizedAddress)
        } finally {
            semaphore.release()
        }
    }

    override fun useNonce(address: String, nonce: BigInteger, txId: String?) {
        semaphore.acquire()
        try {
            nonceQueue.markUsed(address.lowercase(), nonce)
        } finally {
            semaphore.release()
        }
    }

    override fun discardNonce(
        address: String,
        nonce: BigInteger,
        errorMessage: String?,
    ) {
        semaphore.acquire()
        try {
            val neutralizedAddress = address.lowercase()
            // If somehow nonce manager falls behind blockchain nonce,
            // then reset the queue and start with the recent nonce value
            // in the next iteration.
            if (
                errorMessage?.contains("nonce too low", true) == true
                && !nonceQueue.isEmpty(neutralizedAddress)
            ) {
                nonceQueue.reset(neutralizedAddress)
            }
            else {
                nonceQueue.remove(neutralizedAddress, nonce)
            }
        } finally {
            semaphore.release()
        }
    }

    fun reset(address: String) {
        semaphore.acquire()
        try {
            nonceQueue.reset(address.lowercase())
        } finally {
            semaphore.release()
        }
    }
}

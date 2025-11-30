package io.github.psychoplasma.nonceq

import java.math.BigInteger

import kotlinx.coroutines.*
import kotlin.random.Random

import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Numeric


object Utils {
    val GAS_PRICE: BigInteger = BigInteger.valueOf(20_000_000_000L) // 20 Gwei
    val GAS_LIMIT: BigInteger = BigInteger.valueOf(21_000L)

    /**
     * Generates credentials with a randomly generated key-pair
     */
    fun generateCredentials(): Credentials {
        return Credentials.create(Keys.createEcKeyPair())
    }

    /**
     * Generates a random Ethereum-like address
     */
    fun generateAddress(): String {
        return Numeric.toHexString(Random.nextBytes(20))
    }

    /**
     * Encodes a raw transaction to byte array
     */
    fun encodeRawTransaction(rawTransaction: RawTransaction): ByteArray {
        return if (rawTransaction.type.isEip4844) {
            TransactionEncoder.encode4844(rawTransaction)
        } else {
            TransactionEncoder.encode(rawTransaction)
        }
    }

    /**
     * Builds a transaction transferring [amount] of Ether to [toAddress]
     *
     * @return Encoded raw transaction in hexadecimal string representation
     */
    fun transferEtherTx(
        toAddress: String,
        amount: BigInteger,
        nonce:BigInteger,
    ): String {
        val tx = RawTransaction.createEtherTransaction(
            nonce,
            GAS_PRICE,
            GAS_LIMIT,
            toAddress,
            amount,
        )
        return Numeric.toHexString(encodeRawTransaction(tx))
    }

    /**
     * @return Private key in hexadecimal string representation
     */
    fun Credentials.privateKey(): String = Numeric.toHexString(
        ecKeyPair.privateKey.toByteArray()
    )

    /**
     * @return Public key in hexadecimal string representation
     */
    fun Credentials.publicKey(): String = Numeric.toHexString(
        ecKeyPair.publicKey.toByteArray()
    )

    /**
     * Runs [function] the given [times] asynchronously.
     */
    fun runTimesAsync(
        times: Int,
        parallelism: Int = 2000,
        function: suspend (index: Int) -> Unit,
    ): List<Deferred<Unit>> {
        @OptIn(ExperimentalCoroutinesApi::class)
        val scope = CoroutineScope(SupervisorJob()
            + Dispatchers.Default.limitedParallelism(parallelism)
        )
        return (0..< times).map { scope.async { function(it) } }
    }

    /**
     * Runs [function] asynchronously at [rate] as evenly distributed [over] ms time.
     */
    suspend fun runAtRateAsync(
        rate: Int,
        over: Long = 1000L,
        parallelism: Int = 2000,
        function: suspend (index: Int) -> Unit,
    ): List<Deferred<Unit>> {
        val results = mutableListOf<Deferred<Unit>>()
        @OptIn(ExperimentalCoroutinesApi::class)
        val scope = CoroutineScope(SupervisorJob()
            + Dispatchers.Default.limitedParallelism(parallelism)
        )

        repeat(rate) {
            val result = scope.async { function(it) }
            results.add(result)
            delay(over / rate)
        }

        return results
    }

    /**
     * Runs [function] asynchronously at [rate] as uniformly distributed [over] ms time.
     */
    fun runAtRandomAsync(
        rate: Int,
        over: Long = 1000L,
        parallelism: Int = 2000,
        function: suspend (index: Int) -> Unit,
    ): List<Deferred<Unit>> {
        val results = mutableListOf<Deferred<Unit>>()
        @OptIn(ExperimentalCoroutinesApi::class)
        val scope = CoroutineScope(SupervisorJob()
            + Dispatchers.Default.limitedParallelism(parallelism)
        )

        repeat(rate) {
            val result = scope.async {
                delay(Random.nextLong(1, over))
                function(it)
            }
            results.add(result)
        }

        return results
    }
}

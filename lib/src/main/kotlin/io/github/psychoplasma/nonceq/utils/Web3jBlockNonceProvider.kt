package io.github.psychoplasma.nonceq.utils

import java.math.BigInteger
import java.util.*

import kotlinx.coroutines.delay

import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName


class Web3jBlockNonceProvider(private val web3j: Web3j) : BlockNonceProvider {
    override fun getBlockNonce(address: String): BigInteger {
        val response = web3j
            .ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
            .send()
        return response.transactionCount
    }
}
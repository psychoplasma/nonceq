package io.github.psychoplasma.nonceq.utils

import java.math.BigInteger

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName


public class Web3jBlockNonceProvider(private val web3j: Web3j) : BlockNonceProvider {
    override fun getBlockNonce(address: String): BigInteger {
        val response = web3j
            .ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
            .send()
        return response.transactionCount
    }
}
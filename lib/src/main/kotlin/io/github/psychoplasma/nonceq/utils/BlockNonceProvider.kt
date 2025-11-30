package io.github.psychoplasma.nonceq.utils

import java.math.BigInteger
import java.util.*

import kotlinx.coroutines.delay

import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName


interface BlockNonceProvider {
    fun getBlockNonce(address: String): BigInteger;
}

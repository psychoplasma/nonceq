package io.github.psychoplasma.nonceq.queue

import java.io.Serializable
import java.time.Instant


data class Nonce(
    var value: String,
    var used: Boolean,
    var insertedAt: Long = 0L,
) : Serializable

fun Nonce.isExpired(expiry: Long): Boolean {
    return !used && Instant.now().toEpochMilli() - insertedAt >= expiry
}

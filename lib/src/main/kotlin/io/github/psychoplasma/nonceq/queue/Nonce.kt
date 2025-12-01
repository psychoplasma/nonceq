package io.github.psychoplasma.nonceq.queue

import java.io.Serializable
import java.time.Instant


public data class Nonce(
    var value: String,
    var used: Boolean,
    var insertedAt: Long = 0L,
) : Serializable

public fun Nonce.isExpired(expiry: Long): Boolean {
    return !used && Instant.now().toEpochMilli() - insertedAt >= expiry
}

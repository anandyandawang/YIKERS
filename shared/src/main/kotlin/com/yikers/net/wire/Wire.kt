package com.yikers.net.wire

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

// Shared CBOR codec (compact binary, good for 60Hz snapshots).
@OptIn(ExperimentalSerializationApi::class)
object Wire {
    private val cbor = Cbor {
        ignoreUnknownKeys = true // forward-compat: an older peer skips new fields
    }

    fun encode(envelope: Envelope): ByteArray = cbor.encodeToByteArray(envelope)

    fun decode(bytes: ByteArray): Envelope = cbor.decodeFromByteArray(bytes)

    fun encodeAd(ad: ServerAd): ByteArray = cbor.encodeToByteArray(ad)

    fun decodeAd(bytes: ByteArray): ServerAd = cbor.decodeFromByteArray(bytes)
}

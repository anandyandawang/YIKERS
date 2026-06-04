package com.yikers.net.wire

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

// One shared CBOR codec for the whole protocol. CBOR (not JSON): compact binary,
// no float<->text precision loss, ideal for 60Hz snapshots over the socket. Switch
// to Json here in one place if a human-readable trace is ever needed.
@OptIn(ExperimentalSerializationApi::class)
object Wire {
    private val cbor = Cbor {
        ignoreUnknownKeys = true // forward-compat: an older peer skips new fields
    }

    fun encode(envelope: Envelope): ByteArray = cbor.encodeToByteArray(envelope)

    fun decode(bytes: ByteArray): Envelope = cbor.decodeFromByteArray(bytes)

    // Discovery rides UDP, not the TCP Envelope stream, so it gets its own helpers.
    fun encodeAd(ad: ServerAd): ByteArray = cbor.encodeToByteArray(ad)

    fun decodeAd(bytes: ByteArray): ServerAd = cbor.decodeFromByteArray(bytes)
}

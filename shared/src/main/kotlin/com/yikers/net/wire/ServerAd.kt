package com.yikers.net.wire

import kotlinx.serialization.Serializable

// A server's reply to a UDP discovery query: its display name, the TCP port to join
// on, and live room occupancy. Host is NOT carried here — the client reads it from
// the datagram's source address, which is always correct even behind multiple NICs.
@Serializable
data class ServerAd(
    val name: String,
    val tcpPort: Int,
    val players: Int,
    val maxPlayers: Int,
)

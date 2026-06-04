package com.yikers.net.wire

import kotlinx.serialization.Serializable

// Reply to a discovery query. No host: client reads it from the datagram source.
@Serializable
data class ServerAd(
    val name: String,
    val tcpPort: Int,
    val players: Int,
    val maxPlayers: Int,
)

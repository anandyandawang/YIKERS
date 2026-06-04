package com.yikers.net.discovery

import com.yikers.net.wire.ServerAd

// LAN discovery: client broadcasts on DISCOVERY_PORT; each server unicasts a ServerAd.
const val DISCOVERY_PORT = 54545

// Default TCP port; also the lobby's direct-connect fallback when UDP is blocked.
const val DEFAULT_TCP_PORT = 54000

// Magic so a server ignores stray datagrams on the port.
val DISCOVERY_QUERY: ByteArray = "YIKERS_Q1".toByteArray(Charsets.US_ASCII)

fun isDiscoveryQuery(packet: ByteArray, length: Int): Boolean =
    length == DISCOVERY_QUERY.size &&
        DISCOVERY_QUERY.indices.all { packet[it] == DISCOVERY_QUERY[it] }

data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
    val players: Int,
    val maxPlayers: Int,
) {
    val full: Boolean get() = players >= maxPlayers
    fun label(): String = "$name   $host:$port   $players/$maxPlayers"

    companion object {
        fun from(ad: ServerAd, host: String) =
            DiscoveredServer(ad.name, host, ad.tcpPort, ad.players, ad.maxPlayers)
    }
}

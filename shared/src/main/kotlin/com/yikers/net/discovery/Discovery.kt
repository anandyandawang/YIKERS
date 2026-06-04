package com.yikers.net.discovery

import com.yikers.net.wire.ServerAd

// LAN discovery constants + the discovered-server view model. A client broadcasts a
// tiny UDP query on DISCOVERY_PORT; every server's responder replies (unicast) with
// a CBOR ServerAd. The client reads the responder's host from the datagram source
// address, so the ad itself carries only the TCP port + room status.
const val DISCOVERY_PORT = 54545

// Default TCP port a server listens on when none is given — also the direct-connect
// fallback the lobby offers when UDP discovery is blocked (e.g. one-machine demo).
const val DEFAULT_TCP_PORT = 54000

// Magic in the broadcast query so a server ignores stray datagrams on the port.
val DISCOVERY_QUERY: ByteArray = "YIKERS_Q1".toByteArray(Charsets.US_ASCII)

fun isDiscoveryQuery(packet: ByteArray, length: Int): Boolean =
    length == DISCOVERY_QUERY.size &&
        DISCOVERY_QUERY.indices.all { packet[it] == DISCOVERY_QUERY[it] }

// A server found on the LAN, ready to show as a clickable lobby row + connect to.
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
        // Fold the server's ad + the host the datagram came from into a lobby row.
        fun from(ad: ServerAd, host: String) =
            DiscoveredServer(ad.name, host, ad.tcpPort, ad.players, ad.maxPlayers)
    }
}

package com.yikers.net

// Holds the LAN target PlayScreen connects to + any in-process server this client
// booted (solo or host). Stashed here instead of threading through constructors.
object Session {
    @Volatile
    var host: String = "127.0.0.1"
        private set

    @Volatile
    var port: Int = 0
        private set

    // In-process server from this client's "Host"; kept alive so others can join.
    @Volatile
    var hostedServer: DedicatedServer? = null
        private set

    fun network(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    fun setHosted(server: DedicatedServer) {
        hostedServer?.stop()
        hostedServer = server
    }

    fun shutdownHost() {
        hostedServer?.stop()
        hostedServer = null
    }
}

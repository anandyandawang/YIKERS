package com.yikers.net

// LAN target + this client's in-process server (solo or host).
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

    // Adopt an in-process server (stopping any previous) and aim the next join at it.
    fun hostAndJoin(server: DedicatedServer) {
        setHosted(server)
        server.start()
        network("127.0.0.1", server.port)
    }

    fun shutdownHost() {
        hostedServer?.stop()
        hostedServer = null
    }
}

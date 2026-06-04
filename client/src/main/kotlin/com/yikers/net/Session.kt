package com.yikers.net

// Routes PlayScreen to the local sim or a remote LAN server; the lobby stashes the
// chosen target here instead of threading it through constructors.
object Session {
    enum class Mode { LOCAL, NETWORK }

    @Volatile
    var mode: Mode = Mode.LOCAL
        private set

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

    fun local() {
        mode = Mode.LOCAL
    }

    fun network(host: String, port: Int) {
        mode = Mode.NETWORK
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

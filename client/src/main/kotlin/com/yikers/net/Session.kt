package com.yikers.net

// Routes PlayScreen to either the embedded local sim (singleplayer) or a remote LAN
// server (multiplayer). Screens are registered once in YikersGame.create(), so the
// lobby stashes the chosen target here instead of threading it through constructors.
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

    // The in-process server spun up when THIS client hit "Host". Kept alive across
    // the run so other clients can still join; stopped only when replaced or on exit.
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

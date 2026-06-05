package com.yikers.arch

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

// Locks the module boundaries: shared at the bottom (engine-free), launchers at
// the top, no upward deps, and the one sanctioned client -> server edge. Konsist
// reads each module's Kotlin source, so a stray cross-module import fails here.
//
// NOTE: com.yikers.net and com.yikers.control are SPLIT across modules, so the
// server-owned types in them are matched by EXACT name (serverOnlyExact), not by
// package prefix. Keep that list in sync if server gains public types there.
class ArchitectureTest {

    // Game engine packages. shared / client-shared / bot stay engine-free so one
    // side never drags the other's engine in.
    private val engine = listOf(
        "com.badlogic.gdx",
        "com.github.quillraven.fleks",
        "ktx.",
    )

    // server-exclusive packages (not split) -> safe to match by prefix.
    private val serverOnlyPrefixes = listOf(
        "com.yikers.sim",
        "com.yikers.ecs",
        "com.yikers.physics",
        "com.yikers.server",
    )

    // server-owned types in SPLIT packages. Sanctioned exceptions client MAY
    // import: com.yikers.net.DedicatedServer + com.yikers.control.BootConfig.
    private val serverOnlyExact = listOf(
        "com.yikers.control.Controller",
        "com.yikers.control.RelayController",
        "com.yikers.control.Palette",
        "com.yikers.net.ClientConn",
        "com.yikers.net.discovery.DiscoveryResponder",
    )

    // Client UI shell (not split). server + bot must never reach up into it.
    private val clientUi = listOf(
        "com.yikers.screen",
        "com.yikers.render",
        "com.yikers.YikersGame",
    )

    private val launchers = listOf(
        "com.yikers.lwjgl3",
        "com.yikers.android",
        "com.yikers.ios",
    )

    @Test
    fun `shared stays engine-free`() =
        Konsist.scopeFromModule("shared").assertNoImports(engine)

    @Test
    fun `client-shared stays engine-free and off the server`() =
        Konsist.scopeFromModule("client-shared")
            .assertNoImports(engine + serverOnlyPrefixes + serverOnlyExact)

    @Test
    fun `bot stays engine-free and out of client UI`() =
        Konsist.scopeFromModule("bot").assertNoImports(engine + clientUi)

    @Test
    fun `server never depends up on client UI or launchers`() =
        Konsist.scopeFromModule("server").assertNoImports(clientUi + launchers)

    // The one sanctioned edge: client may touch server ONLY via DedicatedServer
    // (in-process host) + BootConfig (boot seed). Any other server import fails.
    @Test
    fun `client touches server only via DedicatedServer and BootConfig`() =
        Konsist.scopeFromModule("client").assertNoImports(serverOnlyPrefixes + serverOnlyExact)

    @Test
    fun `nobody imports the platform launchers`() =
        Konsist.scopeFromProject().assertNoImports(launchers)

    // Fail if any file in the scope imports something under a forbidden prefix.
    private fun KoScope.assertNoImports(forbidden: List<String>) =
        files.assertFalse { file ->
            file.hasImport { import -> forbidden.any { import.name.startsWith(it) } }
        }
}

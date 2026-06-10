package com.yikers.arch

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

// Source-level rules only. The MODULE dependency graph (who may depend on whom) is
// enforced separately + more directly by the `checkModuleDeps` Gradle task in the
// root build. Konsist keeps just what gradle's project-dep graph can't see:
//   1. engine purity of the foundational layers (an external dep, not a project dep)
//   2. the symbol restriction on the one sanctioned client -> server edge
class ArchitectureTest {

    // Game engine packages. The pure layers must never import these. An import
    // check (not a dep check) catches the leak even if gdx arrives transitively.
    private val engine = listOf(
        "com.badlogic.gdx",
        "com.github.quillraven.fleks",
        "ktx.",
    )

    @Test
    fun `shared stays engine-free`() =
        Konsist.scopeFromModule("shared").assertNoImports(engine)

    @Test
    fun `client-shared stays engine-free`() =
        Konsist.scopeFromModule("client-shared").assertNoImports(engine)

    @Test
    fun `bot stays engine-free`() =
        Konsist.scopeFromModule("bot").assertNoImports(engine)

    // The one sanctioned edge: client depends on server (gradle), so the compiler
    // allows ANY server import. Restrict it to the single host touchpoint.
    // serverTypes is derived from server's source -> no hand-maintained list, and it
    // fails CLOSED: a new server type imported by client is caught with no edit.
    @Test
    fun `client touches server only via DedicatedServer`() {
        val sanctioned = setOf(
            "com.yikers.net.DedicatedServer",
        )
        val serverTypes = with(Konsist.scopeFromModule("server")) {
            (
                classes().mapNotNull { it.fullyQualifiedName } +
                    interfaces().mapNotNull { it.fullyQualifiedName } +
                    objects().mapNotNull { it.fullyQualifiedName }
                ).toSet()
        }
        Konsist.scopeFromModule("client").files.assertFalse { file ->
            file.imports.any { it.name in serverTypes && it.name !in sanctioned }
        }
    }

    // Fail if any file in the scope imports something under a forbidden prefix.
    private fun KoScope.assertNoImports(forbidden: List<String>) =
        files.assertFalse { file ->
            file.hasImport { import -> forbidden.any { import.name.startsWith(it) } }
        }
}

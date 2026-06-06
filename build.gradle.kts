// Root build. No code here. Subprojects pull shared repos and each applies its
// own Kotlin plugin — deliberately NOT declared apply-false here, so Kotlin
// stays off a shared root classpath. That lets :android load AGP + kotlin-android
// in one classloader; otherwise kotlin-android can't see AGP's BaseExtension.
// The RoboVM gradle plugin has no plugins{} marker, so the ios module
// applies it the legacy way; its classpath is declared here.
buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
    dependencies {
        // Kept as a literal: version-catalog (libs.*) accessors are not
        // available inside a buildscript{} block, since this classpath is
        // compiled before the catalog exists.
        classpath("com.mobidevelop.robovm:robovm-gradle-plugin:2.3.21")
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

// --- module dependency graph guard ---------------------------------------------
// The whole allowed inter-module graph in one place. `checkModuleDeps` fails if a
// subproject declares a project(...) dependency not on its allowlist -> catches a
// forbidden edge (e.g. :server -> :client) at the DECLARATION, before any import.
// Source-level purity + the sanctioned client->server symbol rule live in :arch.
val allowedModuleDeps = mapOf(
    "shared" to emptySet<String>(),
    "client-shared" to setOf(":shared"),
    "server" to setOf(":shared"),
    "client" to setOf(":shared", ":server", ":client-shared"),
    "bot" to setOf(":shared", ":client-shared"),
    "lwjgl3" to setOf(":client"),
    "ios" to setOf(":client"),
    "android" to setOf(":client"),
    "e2e" to setOf(":shared", ":client-shared", ":server", ":client", ":bot"),
    "arch" to emptySet(),
)

subprojects {
    val moduleName = name
    tasks.register("checkModuleDeps") {
        // Read deps at CONFIGURATION time (this subproject is already evaluated) and
        // capture into locals, so doLast touches no Task.project state at execution
        // -> stays configuration-cache friendly.
        val declared = configurations
            .flatMap { it.dependencies }
            .filterIsInstance<org.gradle.api.artifacts.ProjectDependency>()
            .map { it.path } // Gradle 8.11+: ProjectDependency.getPath()
            .toSet()
        val allowed = allowedModuleDeps[moduleName]
            ?: error("module :$moduleName missing from allowedModuleDeps — add it")
        doLast {
            val forbidden = declared - allowed
            require(forbidden.isEmpty()) {
                "module :$moduleName declares forbidden module deps: $forbidden " +
                    "(allowed: ${allowed.ifEmpty { setOf("(none)") }})"
            }
        }
    }
    // Local `./gradlew check` enforces the graph too; CI calls checkModuleDeps direct.
    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.named("check") { dependsOn("checkModuleDeps") }
    }
}

// Android launcher. libGDX Android backend -> installable APK.
// NOTE: this module needs the Android SDK (platforms;android-35,
// build-tools;35.0.0). CI installs them; locally set ANDROID_HOME or put
// sdk.dir=... in local.properties. Without the SDK, AGP fails to configure.
plugins {
    // Apply AGP and kotlin-android together here (both via the catalog) so they
    // share this module's plugin classloader. That's what lets kotlin-android see
    // AGP's classes (com.android.build.gradle.BaseExtension); loading them in
    // separate classloaders throws ClassNotFoundException for that type. The root
    // build keeps Kotlin off a shared classpath precisely so this works.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

repositories {
    // AGP build tooling (com.android.*) lives here. Everything else (gdx, ktx,
    // box2d natives) resolves from mavenCentral inherited from the root build.
    google()
}

android {
    namespace = "com.yikers"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yikers" // match iOS app.id
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        // Debug = signed with the local debug key, so it installs straight onto
        // a phone. Release stays unsigned (would need a keystore to install).
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    // gdx + box2d ship native .so per ABI; copyAndroidNatives drops them here
    // so AGP bundles them into the APK.
    sourceSets.getByName("main").jniLibs.srcDir(layout.buildDirectory.dir("generated/jniLibs"))

    // Asset-free game (shapes + built-in classpath font). Trim duplicate jar
    // metadata so packaging doesn't choke on it.
    packaging {
        resources {
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Native libs arrive as per-ABI jars on this configuration, off the compile path.
val natives: Configuration by configurations.creating

dependencies {
    implementation(project(":client"))
    // ktx-app: YikersGame extends KtxGame, so the launcher needs that type chain.
    implementation(libs.ktx.app)
    // gdx core: AndroidApplication's supertype (com.badlogic.gdx.Application) and
    // YikersGame's ApplicationListener are gdx-core types referenced at compile
    // time, but neither :core (implementation) nor gdx-backend-android exposes
    // them on the compile classpath — so depend on gdx directly.
    implementation(libs.gdx)
    implementation(libs.gdx.backend.android)

    // Per-ABI native .so jars, kept off the compile/runtime classpath on the
    // custom `natives` configuration; copyAndroidNatives extracts them below.
    "natives"(variantOf(libs.gdx.platform) { classifier("natives-armeabi-v7a") })
    "natives"(variantOf(libs.gdx.platform) { classifier("natives-arm64-v8a") })
    "natives"(variantOf(libs.gdx.platform) { classifier("natives-x86") })
    "natives"(variantOf(libs.gdx.platform) { classifier("natives-x86_64") })
    "natives"(variantOf(libs.gdx.box2d.platform) { classifier("natives-armeabi-v7a") })
    "natives"(variantOf(libs.gdx.box2d.platform) { classifier("natives-arm64-v8a") })
    "natives"(variantOf(libs.gdx.box2d.platform) { classifier("natives-x86") })
    "natives"(variantOf(libs.gdx.box2d.platform) { classifier("natives-x86_64") })
}

// Unpack each ABI's .so out of its natives jar into jniLibs so the APK bundles
// the right libgdx / libgdx-box2d for every device.
val nativesOut = layout.buildDirectory.dir("generated/jniLibs")
val abis = mapOf(
    "armeabi-v7a" to "natives-armeabi-v7a",
    "arm64-v8a" to "natives-arm64-v8a",
    "x86" to "natives-x86",
    "x86_64" to "natives-x86_64",
)
val nativeCopyTasks = abis.map { (abi, classifier) ->
    val suffix = abi.replace("-", "").replaceFirstChar { it.uppercase() }
    tasks.register<Copy>("copyNatives$suffix") {
        from({ natives.files.filter { it.name.endsWith("$classifier.jar") }.map { zipTree(it) } })
        include("**/*.so")
        into(nativesOut.map { it.dir(abi) })
    }
}
val copyAndroidNatives by tasks.registering {
    dependsOn(nativeCopyTasks)
}

// AGP merges jniLibs during packaging; extract natives before that runs.
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }
    .configureEach { dependsOn(copyAndroidNatives) }

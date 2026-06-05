// AGP (com.android.application) lives on google(), not the plugin portal, so
// the android module's plugins{} block can resolve it.
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "YIKERS"

// :android needs the Android SDK + AGP just to configure, so pull it in only
// when local.properties declares sdk.dir — i.e. an SDK is actually configured.
// Android Studio writes it, the APK workflow writes it, a local CLI build needs
// it for AGP anyway. Keeps desktop / iOS / headless-test builds — and the CI and
// iOS workflows — free of Android tooling. Keyed on sdk.dir (not mere file
// existence) so other uses of local.properties don't drag :android in.
val localProps = file("local.properties")
val buildAndroid = localProps.exists() &&
    java.util.Properties().apply { localProps.inputStream().use { load(it) } }.getProperty("sdk.dir") != null

include("shared", "client-shared", "server", "client", "lwjgl3", "ios", "bot", "e2e", "arch")
if (buildAndroid) include("android")

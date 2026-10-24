import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.portal.android.compose.feature)
}

extensions.configure<LibraryExtension> {
    namespace = "com.xxmrk888ytxx.mainscreen"
}

dependencies {
    implementation(libs.coil.compose)
}

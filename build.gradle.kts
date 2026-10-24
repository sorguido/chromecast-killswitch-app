plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.library") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

allprojects {
    layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("development/build/${if (path == ":") "root" else path.drop(1).replace(':', '/')}"))
}

# Byte Buddy Gradle plugin for Android

This plugin shares some similarities with the regular Byte Buddy Gradle plugin which is described in [its own readme](../README.md).

The Android version of Byte Buddy works similarly to [Android's annotation processors](https://developer.android.com/studio/build/dependencies#annotation_processor) (or `kapt` for Kotlin), as far as you need to define a *compiler project*, which will be a separate project containing the instrumentation code. Within this project, you use Byte Buddy's API to create a custom `Plugin` that runs during compilation of the actual Android project.

To add a compiler plugin to an Android project, you first apply the Byte Buddy Gradle plugin to the Android project, the same way as the plain Java Byte Buddy Gradle plugin is added. Then you
add your compiler plugin as a dependency of the Android project as a dependency of type `byteBuddy`. This custom type of dependency is used at compile time, but won't be present at runtime. So, if your compiler plugin needs to add classes that will be referenced at runtime, then those classes will have to be added as a separate, regular dependency.

For example, the following build file applies the `my.plugin:compiler` plugin with the shared dependency `my.plugin:library`:

###### build.gradle
```groovy
plugins {
    id 'com.android.application'
    id 'net.bytebuddy.byte-buddy-gradle-plugin' version byteBuddyVersion
}

dependencies {
    byteBuddy "my.plugin:compiler:1.0.0"
    implementation "my.plugin:library:1.0.0"
}
```

## Creating a plugin project

A plugin project is a Gradle project that can be either a regular `java-library` or `com.android.library` type. The advantage of defining it as an Android library project (`com.android.library`) is that you are able to reference other Android libraries from it, as well as Android SDK classes.

For your compiler plugin to be able of getting recognized as a Byte Buddy compiler project, it must contain its Byte Buddy plugins class names listed in the */META-INF/net.bytebuddy/build.plugins* resource file. Currently, this form of discovery is the only option for configuring plugins, but explicit configuration forms will be added in a future version.

## Special behaviour

This plugin can be used without restrictions for newer versions of Android. However, since Kotlin is used in many Android projects, the default `EntryPoint` is set to decoration without validation. The default would be to define the `EntryPoint` to `REBASE`. Kotlin, until very recent versions, issues erroneous type information which causes parsing errors when types are inflated what often fails the instrumentation. The default might be revised in the future, if this problem becomes less of an issue. To pin the `EntryPoint`, one should set it explicitly.

For older versions of Android, more restrictions apply. Previously, the plugin had to rely on Android's build APIs, the instrumentation of Android plugins differs in the following ways.

- You cannot instrument classes that belong to the [Android SDK](https://developer.android.com/reference/packages) or to the core JVM. However, you can instrument classes that are defined by libraries on Android application projects. However, you can instrument libraries only on Android application projects, not [Android libraries](https://developer.android.com/studio/projects/android-library) projects.
- You cannot add additional classes during an instrumentation as those cannot be added to a project using Android's current APIs. As a consequence, it is currently only possible to apply decorating transformations.
- As Byte Buddy does not control the lifecycle of the instrumentation, a `Plugin.WithPreprocessor` might be required to instrument a class before all classes of a project are preprocessed.

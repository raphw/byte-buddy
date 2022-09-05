# Byte buddy Android Gradle plugin

This plugin shares some similarities with the plain Java Byte Buddy Gradle plugin, so it's worth having a look at [its
readme](../README.md) first.

The Android version of Byte Buddy works similarly to
how [Android's annotation processors](https://developer.android.com/studio/build/dependencies#annotation_processor) (or
`kapt` for Kotlin) work in the sense that you'd need to define your "compiler" project, which will be a separate project
containing the instrumentation code, where the Byte Buddy API is used to create your own Byte Buddy Plugin that will
run at compile time of the host Android project.

In order to add your compiler plugin to an Android project, you'd first need to apply the Byte Buddy Gradle plugin to
said Android project, the same way as the plain Java Byte Buddy Gradle plugin is added, shown below. Then you need to
add your compiler plugin as a dependency of the Android project, but said dependency needs to be of type `bytebuddy`.
This custom type of dependency is used at compile time, but won't be present at runtime. So, if your compiler plugin
needs to add classes that will be referenced at runtime, then those classes will have to be added as a separate, regular
dependency, as shown below.

###### build.gradle

```groovy
plugins {
    id 'com.android.application'
    id 'net.bytebuddy.byte-buddy-gradle-plugin' version byteBuddyVersion
}

dependencies {
    bytebuddy "my.plugin:compiler:0.0.0"
    implementation "my.plugin:library:0.0.0"
}
```

## Creating a Plugin project

A plugin project is a Gradle project that can be either a regular `java-library` or `com.android.library` type of
project. The advantage of defining it as an Android Library project (`com.android.library`) is that you'd be able
to reference other Android libraries from it, and also Android SDK classes as well.

For your compiler plugin to be able of getting recognized as a Byte Buddy compiler project, it must contain its
Byte Buddy plugins class names listed in the *META-INF/net.bytebuddy/build.plugins* resource file (same way as for the
plain Java Byte Buddy Gradle plugin).

## Limitations

There are limitations to this tool in comparison to the plain Java Byte Buddy Gradle plugin, caused by the Android
environment, those are:

- You cannot instrument classes that belong to the [Android SDK](https://developer.android.com/reference/packages).
- You can instrument libraries only on Android application projects,
  not [Android libraries](https://developer.android.com/studio/projects/android-library).
- You cannot use `ByteBuddy.intercept(MethodDelegation)` since it requires the creation of additional classes, and the
  ASM API that this tools is based on, doesn't allow to create new classes.
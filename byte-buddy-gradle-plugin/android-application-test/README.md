How to run the tests
---

### Environment setup

* The environment var `ANDROID_HOME` must be set and pointing to the Android SDK dir.
* Java 11 is required, more info [here](https://developer.android.com/studio/releases/gradle-plugin#jdk-11).
* A Gradle wrapper version of 7.2+ is required in order to be compatible with the Android
  Gradle plugin used in this project, more info on it
  [here](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle).
* There must be an Android device connected through [ADB](https://developer.android.com/studio/command-line/adb), either
  a running Emulator or a real device plugged in with
  [USB debugging](https://developer.android.com/studio/command-line/adb#Enabling) enabled.
* Set environment var `ANDROID_TEST` to `"true"`.

This module is disabled by default to avoid issues with the Android Gradle Plugin and Intellij IDEA
version incompatibility, which causes the project analysis to fail when opened with said IDE, though it doesn't
cause any issues when running it from the command line. That's why the environment var `ANDROID_TEST` is needed
in order to switch this module on by setting it to `"true"`.

### Steps

* Build Byte Buddy alongside the Byte Buddy Gradle plugin.
* Run the following command from the Byte Buddy Gradle plugin
  dir: `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.bytebuddy.android.test.ByteBuddyInstrumentedTest`

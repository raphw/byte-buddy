How to run the tests
---

### Environment setup

* The environment var `ANDROID_HOME` must be set and pointing to the Android SDK dir.
* Java 11 is required, more
  info [here](https://developer.android.com/studio/releases/gradle-plugin#jdk-11).
* There must be an Android device connected
  through [ADB](https://developer.android.com/studio/command-line/adb), either
  a running Emulator or a real device plugged in with
  [USB debugging](https://developer.android.com/studio/command-line/adb#Enabling) enabled.

### Steps

* Build Byte Buddy alongside the Byte Buddy Gradle plugin.
* Located in the root dir of this test project, run the following command:
  dir: `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.bytebuddy.android.test.ByteBuddyInstrumentedTest`
* Optionally, you can also open up this test project with Android Studio, open up the test class and
  run it through the IDE.
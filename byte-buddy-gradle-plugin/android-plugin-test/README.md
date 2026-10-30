How to run the tests
---

### Environment setup

* The environment var `ANDROID_HOME` must be set and pointing to the Android SDK dir.
* Use a Java and Gradle version compatible with the Android Gradle plugin under
  test. The default is AGP 7.4.0; override it with `-PagpVersion=VERSION`.
  AGP 9.5.0 requires Java 17 or newer and Gradle 9.6.0 or newer.
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

### Published AAR plugin regression

The default test consumes the AAR plugin as a project dependency. To test its
packaged classes and discovery resource, publish it to the fixture's local
repository and consume that publication instead:

```shell
gradle -PagpVersion=9.5.0 :aar-bytebuddy-plugin:publishReleasePublicationToTestRepository
gradle -PagpVersion=9.5.0 -PpublishedAarPlugin -Pandroid.experimental.enableJavaResourceOptimizations=true :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.bytebuddy.android.test.ByteBuddyInstrumentedTest
```

Run these commands from this directory using a compatible Gradle installation.
Keep Java resource optimization enabled: AGP 9.5.0 produces separate class and
resource artifacts, and the published AAR plugin must load its classes from the
class artifact. The existing instrumentation and service-loading assertions
verify that both bytecode transformation and resources still work.
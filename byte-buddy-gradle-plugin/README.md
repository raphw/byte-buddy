# Byte Buddy Gradle Plugin

The **Byte Buddy Gradle Plugin** enables you to apply bytecode enhancements during the build process. If the *java* plugin 
is registered, the plugin registers an intermediate task for every source set for which at least one transformation is defined.
For the *main* source set, the task is named *byteBuddy*. For each other source set, the source set name is prefixed as in *[source set]ByteBuddy*. If the *java* plugin is not used in a Gradle build or subproject, the Byte Buddy plugin remains passive.

To apply a transformation, consider the following Gradle build file:

###### build.gradle
```groovy
plugins {
  id 'java'
  id 'net.bytebuddy.byte-buddy-gradle-plugin' version byteBuddyVersion
}

byteBuddy {
  transformation {
    plugin = net.bytebuddy.build.CachedReturnPlugin.class
  }
}
```

This example transformation uses a standard plugin that is shipped with Byte Buddy, which caches any method return value if annotated by `CachedReturnPlugin.Enhance`. The plugin in the example is applied onto the main source set. Custom plugins must implement Byte Buddy's `Plugin` interface, either inline in the build file, within the `buildSrc` directory or within an external artifact that is added as a dependency within the script's *buildscript* block. Note that Gradle also offers a `Plugin` interface which must not be confused with Byte Buddy's plugin API.

A custom can declare a constructor that can take arguments of type `File`, `BuildLogger` or a Gradle-specific `Logger` where the class file root directory or an appropriate logger is provided. It is also possible to supply an argument explicitly by specifying an argument in the plugin configuration.

A plugin can be applied automatically if the plugin's containing jar file declares the plugin's name in the *META-INF/net.bytebuddy/build.plugins* file.

The plugin offers the implementation of custom tasks, the `ByteBuddyTask` transforms classes within a folder and writes it to another folder while using Gradle's incremental build feature what requires Gradle 6 or later. The `ByteBuddySimpleTask` does not support incremental build but works from Gradle 2 on up whereas the `ByteBuddyJarTask` allows the transformation of a bundled jar file. Insight into the Byte Buddy plugins autoconfiguration can be found in the debug log.

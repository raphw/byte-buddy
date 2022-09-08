# Byte Buddy Gradle Plugin

> This info is focused on plain Java Gradle projects, if you need to use Byte Buddy on Android projects instead, you
> should take a look at the [Android's specific README](android-plugin/README.md) as well.

The **Byte Buddy Gradle Plugin** enables you to apply bytecode enhancements during the build process. If the *java* plugin is registered, the plugin registers an intermediate task for every source set for which at least one transformation is defined. For the *main* source set, the task is named *byteBuddy*. For each other source set, the source set name is prefixed as in *[source set]ByteBuddy*. If the *java* plugin is not used in a Gradle build or subproject, the Byte Buddy plugin remains passive.

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

A plugin can be applied automatically if the plugin's containing jar file declares the plugin's name in the *META-INF/net.bytebuddy/build.plugins* file. If these plugins should be loaded from a different location than the Gradle build context, it is possible to explicitly specify a target source set via the *discoverySet* option in the Byte Buddy extension.

Instead of specifying a *plugin* in the extension, it is also possible to specify a *pluginName* as a string. Doing so, the plugin is attempted to be loaded from the *disocerySet*. If no such set is defined, the plugin is rather loaded from the project's class path.

By default, the Byte Buddy plugin attempts an adjustment of the task dependency graph for all projects within a build. This might not always be possible when executing parallel builds or if a project's dependency graph is not previously resolved. If a different project depends on the compile task of another project, and if the adjustment of the dependency graph fails, the Byte Buddy task might not be applied when the dependant task gets executed. In such a case, a user must manually make sure that the Byte Buddy task gets executed when it is appropriate. Byte Buddy's adjustment behavior can be overridden by setting the `adjustment` property in the Byte Buddy extension. This way, Byte Buddy attempts to either resolve subprojects of a project (`Adjustement.SUB`), of only the project that applies the transformation (`Adjustement.SELF`) or it can disable the adjustment altogether (`Adjustement.NONE`). If a resolution error should fail the build instead of logging a warning, Byte Buddy's `adjustmentErrorHandler` property can be set to `Adjustement.ErrorHandler.FAIL`. To suppress the logging output, it can be set to `Adjustement.ErrorHandler.NONE`. To include manual resolutions after task registration, it is also possible to register an `adjustmentPostProcessor` that is executed after the automatic adjustment for each registered task. This way, it becomes possible to register task dependencies in a custom manner.

The plugin offers the implementation of custom tasks, the `ByteBuddyTask` transforms classes within a folder and writes it to another folder while using Gradle's incremental build feature what requires Gradle 6 or later. The `ByteBuddySimpleTask` does not support incremental build but works from Gradle 2 on up whereas the `ByteBuddyJarTask` allows the transformation of a bundled jar file. Insight into the Byte Buddy plugins autoconfiguration can be found in the debug log.

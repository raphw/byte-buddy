# Byte Buddy Maven Plugin

The **Byte Buddy Maven Plugin** enables you to apply bytecode enhancements during the build process. To activate this process, add the following sections to your project POM file:

###### pom.xml
```xml
  <build>
    <plugins>
      <plugin>
        <groupId>net.bytebuddy</groupId>
        <artifactId>byte-buddy-maven-plugin</artifactId>
        <version>LATEST</version>
        <executions>
          <execution>
            <goals>
              <goal>transform</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <transformations>
            <transformation>
              <groupId>net.bytebuddy</groupId>
              <artifactId>byte-buddy</artifactId>
              <version>LATEST</version>
              <plugin>net.bytebuddy.build.CachedReturnPlugin</plugin>
            </transformation>
          </transformations>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

This example transformation uses a standard plugin that is shipped with Byte Buddy, which caches any method return value if annotated by `CachedReturnPlugin.Enhance`. The plugin in the example is applied onto the main source set, test classes can be transformed by specifying the *transform-test* goal. Custom plugins must implement Byte Buddy's `Plugin` interface where the plugin's location is specified by Maven artifact coordinates as shown in the example. The coordinates can be dropped if the plugin is contained within the transformed artifact itself.

A custom can declare a constructor that can take arguments of type `File`, `BuildLogger` or a Gradle-specific `Logger` where the class file root directory or an appropriate logger is provided. It is also possible to supply an argument explicitly by specifying an argument in the plugin configuration.

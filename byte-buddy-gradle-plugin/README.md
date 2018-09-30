# Byte Buddy Maven Plugin

The **Byte Buddy Gradle Plugin** enables you to apply bytecode enhancements during the build process. To activate this process, add the following sections to your project Gradle build file:

###### build.gradle
```groovy
buildscript {
  repositories {
    jCenter()
  }
  dependencies {
    classpath "net.bytebuddy:byte-buddy-gradle-plugin:+"
  }
}
apply plugin: "net.bytebuddy.byte-buddy"

configurations {
  examplePlugin "foo:bar:1.0"
}

byteBuddy {
  transformation {
    plugin = "com.example.junit.HookInstallingPlugin"
    classPath = configurations.examplePlugin
  }
}
```

This configuration informs Gradle to transform all classes by the goal using the transformation specified by **HookInstallingPlugin** within the `foo:bar:1.0` artifact.

###### HookInstallingPlugin.java
```java
package com.example.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;

public class HookInstallingPlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return target.getName().endsWith("Test");
    }

    @Override
    public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription) {
        return builder.method(isAnnotatedWith(anyOf(Test.class, Before.class, After.class))
                .or(isStatic().and(isAnnotatedWith(anyOf(BeforeClass.class, AfterClass.class)))))
                .intercept(MethodDelegation.to(SampleInterceptor.class))
                .implement(Hooked.class);
    }
}
```

This example transformation specifies that Byte Buddy should install a method interceptor (defined by **SampleInterceptor**) on all test classes with a name ending with `Test`. The interceptor is added to all methods with the annotations **`@Test`**, **`@Before`**, **`@After`**, **`@BeforeClass`**, or **`@AfterClass`**. This transformation also adds a marker interface **Hooked** so that we can identify enhanced classes at runtime.

A plugin can declare a constructor that can take arguments of type `File`, `BuildLogger` or a Gradle-specific `Logger` where the class file root directory or an appropriate logger is provided. It is also possible to supply an argument explicitly by specifying an argument in the plugin configuration.
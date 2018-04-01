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
              <goal>transform-test</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <transformations>
            <transformation>
              <plugin>com.example.junit.HookInstallingPlugin</plugin>
            </transformation>
          </transformations>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

This `byte-buddy-maven-plugin` element informs Maven to transform all test classes by the `transform-test` goal using the transformation specified by **HookInstallingPlugin**. Alternatively, the plugin can instrument production classes using the `transform` goal.

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

This example transformation specifies that Byte Buddy should install a method interceptor (defined by **SampleInterceptor**) on all test classes with a name ending with `Test`. The intercetor is added to all methods with the annotations **`@Test`**, **`@Before`**, **`@After`**, **`@BeforeClass`**, or **`@AfterClass`**. This transformation also adds a marker interface **Hooked** so that we can identify enhanced classes at runtime.

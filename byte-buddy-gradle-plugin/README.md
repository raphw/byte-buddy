# Byte Buddy Gradle Plugin

The **Byte Buddy Gradle Plugin** enables you to apply bytecode enhancements during the build process. If the *java* plugin 
is registered, the plugin registers an intermediate task for every source set for which at least one transformation is defined.
For the *main* source set, the task is named *byteBuddy*. For each other source set, the source set name is suffixed.  

To apply a transformation, consider the following Gradle build file:

###### build.gradle
```groovy
plugins {
  id 'java'
  id 'net.bytebuddy.byte-buddy-gradle-plugin' version 'LATEST'
}

class HookInstallingPlugin implements Plugin {

    @Override
    boolean matches(TypeDescription target) {
        return target.getName().endsWith("Test");
    }

    @Override
    Builder<?> apply(Builder<?> builder, TypeDescription typeDescription) {
        return builder.method(isAnnotatedWith(anyOf(Test.class, Before.class, After.class))
                .or(isStatic().and(isAnnotatedWith(anyOf(BeforeClass.class, AfterClass.class)))))
                .intercept(MethodDelegation.to(SampleInterceptor.class))
                .implement(Hooked.class);
    }
    
    @Override void close() { }
}

testByteBuddy {
  transformation {
    plugin = com.example.junit.HookInstallingPlugin.class
  }
}
```

This example transformation specifies that Byte Buddy should install a method interceptor (defined by **SampleInterceptor**) on all test classes with a name ending with `Test`. The interceptor is added to all methods with the annotations **`@Test`**, **`@Before`**, **`@After`**, **`@BeforeClass`**, or **`@AfterClass`**. This transformation also adds a marker interface **Hooked** so that we can identify enhanced classes at runtime.

A plugin can declare a constructor that can take arguments of type `File`, `BuildLogger` or a Gradle-specific `Logger` where the class file root directory or an appropriate logger is provided. It is also possible to supply an argument explicitly by specifying an argument in the plugin configuration.

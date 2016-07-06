Byte Buddy
==========

<a href="http://bytebuddy.net">
<img src="https://raw.githubusercontent.com/raphw/byte-buddy/gh-pages/images/logo-bg.png" alt="Byte Buddy logo" height="180px" align="right" />
</a>

runtime code generation for the Java virtual machine

[![Build Status](https://travis-ci.org/raphw/byte-buddy.svg?branch=master)](https://travis-ci.org/raphw/byte-buddy)
[![Coverage Status](http://img.shields.io/coveralls/raphw/byte-buddy/master.svg)](https://coveralls.io/r/raphw/byte-buddy?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.bytebuddy/byte-buddy-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.bytebuddy/byte-buddy-parent)
[![Download from Bintray](https://api.bintray.com/packages/raphw/maven/ByteBuddy/images/download.svg) ](https://bintray.com/raphw/maven/ByteBuddy/_latestVersion)

Byte Buddy is a code generation library for creating Java classes during the runtime of a Java application and without
the help of a compiler. Other than the code generation utilities that
[ship with the Java Class Library](http://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Proxy.html),
Byte Buddy allows the creation of arbitrary classes and is not limited to implementing interfaces for the creation of
runtime proxies.

In order to use Byte Buddy, one does not require an understanding of Java byte code or the
[class file format](http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html). In contrast, Byte Buddy’s API aims
for code that is concise and easy to understand for everybody. Nevertheless, Byte Buddy remains fully customizable
down to the possibility of defining custom byte code. Furthermore, the API was designed to be as non-intrusive as
possible and as a result, Byte Buddy does not leave any trace in the classes that were created by it. For this reason,
the generated classes can exist without requiring Byte Buddy on the class path. Because of this feature, Byte Buddy’s
mascot was chosen to be a ghost.

Byte Buddy is written in Java 6 but supports the generation of classes for any Java version. Byte Buddy is a
light-weight library and only depends on the visitor API of the Java byte code parser library 
[ASM](http://asm.ow2.org/) which does itself 
[not require any further dependencies](https://repo1.maven.org/maven2/org/ow2/asm/asm/5.0.4/asm-5.0.4.pom).

At first sight, runtime code generation can appear to be some sort of black magic that should be avoided and only
few developers write applications that explicitly generate code during their runtime. However, this picture changes when
creating libraries that need to interact with arbitrary code and types that are unknown at compile time. In this
context, a library implementer must often choose between either requiring a user to implement library-proprietary
interfaces or to generate code at runtime when the user’s types becomes first known to the library. Many known libraries
such as for example *Spring* or *Hibernate* choose the latter approach which is popular among their users under the term
of using [*Plain Old Java Objects*](http://en.wikipedia.org/wiki/Plain_Old_Java_Object). As a result, code generation
has become an ubiquitous concept in the Java space. Byte Buddy is an attempt to innovate the runtime creation of Java
types in order to provide a better tool set to those relying on code generation.

___

<a href="http://bytebuddy.net">
<img src="https://raw.githubusercontent.com/raphw/byte-buddy/gh-pages/images/dukeschoice.jpg" alt="Duke's Choice award" height="110px" align="left" />
</a>

In October 2015, Byte Buddy was distinguished with a
[*Duke's Choice award*](https://www.oracle.com/corporate/pressrelease/dukes-award-102815.html) 
by Oracle. The award appreciates Byte Buddy for its "*tremendous amount of innovation in Java Technology*". 
We feel very honored for having received this award and want to thank all users and everybody else who helped 
making Byte Buddy the success it has become. We really appreciate it!

___

Byte Buddy offers excellent performance at production quality. It is stable and in use by distiguished frameworks and tools such as [Mockito](http://mockito.org), [Google's Bazel build system](http://bazel.io) and many others. Byte Buddy is also used by a large number of commercial products to great result. It is currently downloaded over a million times a year.

Hello World
-----------

Saying *Hello World* with Byte Buddy is as easy as it can get. Any creation of a Java class starts with an instance
of the `ByteBuddy` class which represents a configuration for creating new types:

```java
Class<?> dynamicType = new ByteBuddy()
  .subclass(Object.class)
  .method(ElementMatchers.named("toString"))
  .intercept(FixedValue.value("Hello World!"))
  .make()
  .load(getClass().getClassLoader())
  .getLoaded();
assertThat(dynamicType.newInstance().toString(), is("Hello World!"));
```

The default `ByteBuddy` configuration which is used in the above example creates a Java class in the newest version of
the class file format that is understood by the processing Java virtual machine. As hopefully obvious from
the example code, the created type will extend the `Object` class and overrides its `toString` method which should
return a fixed value of `Hello World!`. The method to be overridden is identified by a so-called `ElementMatcher`. In
the above example, a predefined element matcher `named(String)` is used which identifies methods by their exact names.
Byte Buddy comes with numerous predefined and well-tested matchers which are collected in the `ElementMatchers`
class and which can be easily composed. The creation of custom matchers is however as simple as implementing the
([functional](http://docs.oracle.com/javase/8/docs/api/java/lang/FunctionalInterface.html)) `ElementMatcher` interface.

For implementing the `toString` method, the `FixedValue` class defines a constant return value for the overridden
method. Defining a constant value is only one example of many method interceptors that ship with Byte Buddy. By
implementing the `Implementation` interface, a method could however even be defined by custom byte code.

Finally, the described Java class is created and then loaded into the Java virtual machine. For this purpose, a target
class loader is required. Eventually, we can convince ourselves of the result by calling the `toString` method on an 
instance of the created class and finding the return value to represent the constant value we expected.

A more complex example
----------------------

Of course, a *Hello World example* is a too simple use case for evaluating the quality of a code generation library.
In reality, a user of such a library wants to perform more complex manipulations, for example by introducing hooks
into the execution path of a Java program. Using Byte Buddy, doing so is however equally simple. The following example 
gives a taste of how method calls can be intercepted.

Byte Buddy expresses dynamically defined method implementations by instances of the `Implementation` interface. In the
previous example, `FixedValue` that implements this interface was already demonstrated. By implementing this interface, 
a user of Byte Buddy can go to the length of defining custom byte code for a method. Normally, it is however easier to 
use Byte Buddy's predefined implementations such as `MethodDelegation` which allows for implementing any method in 
plain Java. Using this implementation is straight forward as it operates by delegating the control flow to any POJO. As 
an example of such a POJO, Byte Buddy can for example redirect a call to the only method of the following class:

```java
public class GreetingInterceptor {
  public Object greet(Object argument) {
    return "Hello from " + argument;
  }
}
```

Note that the above `GreetingInterceptor` does not depend on any Byte Buddy type. This is good news because none of the classes
that by Byte Buddy generates require Byte Buddy on the class path! Given the above `GreetingInterceptor`, we can use Byte Buddy 
to implement the Java 8 `java.util.function.Function` interface and its abstract `apply` method:

```java
Class<? extends java.util.function.Function> dynamicType = new ByteBuddy()
  .subclass(java.util.function.Function.class)
  .method(ElementMatchers.named("apply"))
  .intercept(MethodDelegation.to(new GreetingInterceptor()))
  .make()
  .load(getClass().getClassLoader())
  .getLoaded();
assertThat((String) dynamicType.newInstance().apply("Byte Buddy"), is("Hello from Byte Buddy"));
```

Executing the above code, Byte Buddy implements Java's `Function` interface and implements the `apply` method
as a delegation to an instance of the `GreetingInterceptor` POJO that we defined before. Now, every time that the
`Function::apply` method is called, the control flow is dispatched to `GreetingInterceptor::greet` and the latter
method's return value is returned from the interface's method.

Interceptors can be defined to take with more generic inputs and outputs by annotating the interceptor's parameters. 
When Byte Buddy discovers an annotation, the library injects the dependency that the interceptor parameter requires. 
An example for a more general interceptor is the following class:

```java
public class GeneralInterceptor {
  @RuntimeType
  public Object intercept(@AllArguments Object[] allArguments,
                          @Origin Method method) {
    // intercept any method of any signature
  }
}
```

With the above interceptor, any intercepted method could be matched and processed. For example, when matching
`Function::apply`, the method's arguments would be passed as the single element of an array. Also, a `Method` 
reference to `Fuction::apply` would be passed as the interceptor's second argument due to the `@Origin` 
annotation. By declaring the `@RuntimeType` annotation on the method, Byte Buddy finally casts the returned 
value to the return value of the intercepted method if this is necessary. In doing so, Byte Buddy also applies
automatic boxing and unboxing.

Besides the annotations that were already mentioned there exist plenty of other predefined annotations. For 
example, when using the `@SuperCall` annotation on a `Runnable` or `Callable` type, Byte Buddy injects proxy 
instances that allow for an invocation of a non-abstract super method if such a method exists. And even if
Byte Buddy does not cover au use case, Byte Buddy offers an extension mechanism for defining custom annotations.

You might expect that using these annotations ties your code to Byte Buddy. However, Java ignores annotations in case
that they are not visible to a class loader. This way, generated code can still exist without Byte Buddy! You can
find more information on the `MethodDelegation` and on all of its predefined annotations in its *javadoc* and in
Byte Buddy's tutorial.

Where to go from here?
----------------------

Byte Buddy is a comprehensive library and we only scratched the surface of Byte Buddy's capabilities. However, Byte
Buddy aims for being easy to use by providing a domain-specific language for creating classes. Most runtime code
generation can be done by writing readable code and without any knowledge of Java's class file format. If you want
to learn more about Byte Buddy, you can find such a [tutorial on Byte Buddy's web page](http://bytebuddy.net/#/tutorial).
Furthermore, Byte Buddy comes with a [detailed in-code documentation](http://bytebuddy.net/javadoc/) and extensive
test case coverage which can also serve as example code. Finally, you can find an up-to-date list of articles and
presentations on Byte Buddy [in the wiki](https://github.com/raphw/byte-buddy/wiki/Web-Resources). When using Byte
Buddy, make also sure to read the following information on maintaining a project dependency.

Dependency and API evolution
----------------------------

Byte Buddy is written on top of [ASM](http://asm.ow2.org/), a mature and well-tested library for reading and writing
compiled Java classes. In order to allow for advanced type manipulations, Byte Buddy is intentionally exposing the
ASM API to its users. Of course, the direct use of ASM remains fully optional and most users will most likely never
require it. This choice was made such that a user of Byte Buddy is not restrained to its higher-level functionality
but can implement custom implementations without a fuzz when it is necessary.

However, this imposes one possible problem when relying onto Byte Buddy as a project dependency and making use of the
exposed ASM API. The authors of ASM require their users to
[repackage the ASM dependency into a different name space](http://asm.ow2.org/doc/faq.html#Q15). This is necessary
because one cannot anticipate changes in the Java class file format what can lead to API incompatibilities of future
versions of ASM. Because of this, each version of Byte Buddy is distributed in two different packaging formats:

- A no-dependency version that repackages the ASM dependency from its `org.objectweb.asm` into Byte Buddy's own
namespace `net.bytebuddy.jar.asm`. Doing so, the ASM dependency is also contained within Byte Buddy's *jar* file. By
using this version, you do not need to worry about possible ASM version clashes which might be caused by the use of
ASM by both Byte Buddy and other libraries. If you do not plan to use ASM, do not know what ASM is or what this
is all about, this is the version you want to use. The artifact ID of this packaging format is `byte-buddy`.
- A version with an explicit dependency on ASM in its original `org.objectweb.asm` namespace. **This version must only
be used for repackaging Byte Buddy and its ASM dependency into *your own* namespace**. Never distribute your
application while directly relying on this dependency. Otherwise, your users might experience version conflicts of
different ASM versions on their class path. The artifact ID of this packaging format is `byte-buddy-dep`.

Normally, you would use the first, no-dependency version. However, if you are using Byte Buddy and making use of the
exposed ASM API, you **must** use the second version of Byte Buddy **and repackage it** into your own name space as
suggested. This is in particularly true when you plan to redistribute your code for the use by others. Future versions
of Byte Buddy will update their ASM dependency to newer version what will then lead to version clashes between
different ASM versions that were repackaged by Byte Buddy, if you have not follow this recommendation!

There exist several tools that allow for an easy automation of the repacking of dependencies during your build
processes. You can for example use the
[Shade plugin](http://maven.apache.org/plugins/maven-shade-plugin/examples/class-relocation.html) for Maven. With
Gradle, a similar tool is the [Shadow plugin](https://github.com/johnrengelman/shadow). Another alternative is
<a href="http://code.google.com/p/jarjar/">jarjar</a>, a library that offers integration as an Ant task.

License and development
-----------------------

Byte Buddy is licensed under the liberal and business-friendly
[*Apache Licence, Version 2.0*](http://www.apache.org/licenses/LICENSE-2.0.html) and is freely available on
GitHub. Byte Buddy is further released to the repositories of Maven Central and on JCenter. The project is built
using <a href="http://maven.apache.org/">Maven</a>. From your shell, cloning and building the project would go
something like this:

```shell
git clone https://github.com/raphw/byte-buddy.git
cd byte-buddy
mvn package
```

On these commands, Byte Buddy is cloned from GitHub and built on your machine. Byte Buddy is currently tested for the
[*OpenJDK*](http://openjdk.java.net/) versions 6 and 7 and the *Oracle JDK* versions 7 and 8 using Travis CI.

Note that the above build is optimized for its runtime and does not create all artifacts. To build all artifacts,
including the zero-dependency jar of Byte Buddy, use the `extras` profile:

```shell
mvn package -Pextras
```

The zero-dependency jar file is then saved in *byte-buddy/target/byte-buddy-#.#-SNAPSHOT.jar*. The regular jar
file is saved in *byte-buddy-dep/target/byte-buddy-dep-#.#-SNAPSHOT.jar*. Additionally, source code and javadoc
artifacts are built when using this profile. See the [developer page](http://bytebuddy.net/#/develop) for further
information on building the project.

Please use GitHub's [issue tracker](https://github.com/raphw/byte-buddy/issues) for reporting bugs. When committing
code, please provide test cases that prove the functionality of your features or that demonstrate a bug fix.
Furthermore, make sure you are not breaking any existing test cases. If possible, please take the time to write
some documentation. For feature requests or general feedback, you can also use the
[issue tracker](https://github.com/raphw/byte-buddy/issues) or contact us on
[our mailing list](https://groups.google.com/forum/#!forum/byte-buddy).

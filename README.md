What is Byte Buddy?
-------------------

<a href="http://bytebuddy.net">
<img src="https://raw.githubusercontent.com/raphw/byte-buddy/gh-pages/images/logo-bg.png" alt="Byte Buddy logo" height="160px" align="right" />
</a>

Byte Buddy is a code generation library for creating Java classes during the runtime of a Java application and without
the help of a compiler. Other than the code generation utilities that
[ship with the Java Class Library](http://docs.oracle.com/javase/6/docs/api/java/lang/reflect/Proxy.html),
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
[not require any further dependencies](http://search.maven.org/remotecontent?filepath=org/ow2/asm/asm/4.2/asm-4.2.pom).

At first sight, runtime code generation can appear to be some sort of black magic that should be avoided and only
few developers write applications that explicitly generate code during their runtime. However, this picture changes when
creating libraries that need to interact with arbitrary code and types that are unknown at compile time. In this
context, a library implementer must often choose between either requiring a user to implement library-proprietary
interfaces or to generate code at runtime when the user’s types becomes first known to the library. Many known libraries
such as for example *Spring* or *Hibernate* choose the latter approach which is popular among their users under the term
of using [*Plain Old Java Objects*](http://en.wikipedia.org/wiki/Plain_Old_Java_Object). As a result, code generation
has become an ubiquitous concept in the Java space. Byte Buddy is an attempt to innovate the runtime creation of Java
types in order to provide a better tool set to those relying on code generation.

[![Download](https://api.bintray.com/packages/raphw/maven/ByteBuddy/images/download.png)](https://bintray.com/raphw/maven/ByteBuddy/_latestVersion)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.bytebuddy/byte-buddy-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.bytebuddy/byte-buddy-parent)

Hello World
-----------

Saying *Hello World* with Byte Buddy is as easy as it can get. Any creation of a Java class starts with an instance
of the `ByteBuddy` class which represents a configuration for creating new types:

```java
Class<?> dynamicType = new ByteBuddy()
  .subclass(Object.class) 
  .method(named("toString")).intercept(FixedValue.value("Hello World!"))
  .make()
  .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
  .getLoaded();
assertThat(dynamicType.newInstance().toString(), is("Hello World!"));
```

The default `ByteBuddy` configuration which is used in the above example creatse a Java class in the newest version of
the class file format that is understood by the processing Java virtual machine. As hopefully obvious from
the example code, the created type will extend the `Object` class and intercept its `toString` method which should
return a fixed value of `Hello World!`. The method to be intercepted is identified by a so-called method matcher. In 
the above example, a predefined method matcher `named(String)` is used which identifies a method by its exact name. 
Byte Buddy comes with numerous predefined and well-tested method matchers which are collected in the `MethodMatchers`
class. The creation of custom matchers is however as simple as implementing the
([functional](http://docs.oracle.com/javase/8/docs/api/java/lang/FunctionalInterface.html)) `MethodMatcher` interface.

For implementing the `toString` method, the `FixedValue` class defines a constant return value for the intercepted
method. Defining a constant value is only one example of many method interceptors that ship with Byte Buddy. By
implementing the `Instrumentation` interface, a method could however even be defined by custom byte code.

Finally, the described Java class is created and then loaded into the Java virtual machine. For this purpose, a target
class loader is required as well as a class loading strategy where we choose a wrapper strategy. The latter creates a
new child class loader which wraps the given class loader and only knows about the newly created dynamic type.
Eventually, we can convince ourselves of the result by calling the `toString` method on an instance of the created 
class and finding the return value to represent the constant value we expected.

A more complex example
----------------------

Of course, a *Hello World example* is a too simple use case for evaluating the quality of a code generation library.
In reality, a user of such a library wants to perform more complex manipulations such as introducing additional
logic to a compiled Java program. Using Byte Buddy, doing so is however not much harder and the following example
gives a taste of how method calls can be intercepted.

For this demonstration, we will make up a simple pseudo domain where `Account` objects can be used for transferring
money to a given recipient where the latter is represented by a simple string. Furthermore, we want to express that 
the direct transfer of money by calling the `transfer` method is somewhat unsafe which is why we annotate the 
method with `@Unsafe`.

```java
@Retention(RetentionPolicy.RUNTIME)
@interface Unsafe { }

@Retention(RetentionPolicy.RUNTIME)
@interface Secured { }

class Account {
  private int amount = 100;
  @Unsafe
  public String transfer(int amount, String recipient) {
    this.amount -= amount;
    return "transferred $" + amount + " to " + recipient;
  }
}
```

In order to make a transaction safe, we rather want to process it by some `Bank`. For this purpose, the bank
provides an obfuscation but logs the transaction details internally (and to your console). It will then conduct
the transaction for the customer. With the help of Byte Buddy, we will now create a subclass of `Account` that
processes all its transfers by using a `Bank`.

```java
class Bank {
  public static String obfuscate(@Argument(1) String recipient,
                                 @Argument(0) Integer amount,
                                 @Super Account zuper) {
    System.out.println("Transfer " + amount + " to " + recipient);
    return zuper.transfer(amount, recipient.substring(0, 3) + "XXX") + " (obfuscated)";
  }
}
```

Note the annotations on the `Bank`'s obfuscation method's parameters. The first both arguments are annotated
with `@Argument(n)` which will instruct Byte Buddy to inject the `n`-th argument of any intercepted method into
the annotated parameter. Further, note that the order of the parameters of the `Bank`'s obfuscation method
is opposite to the intercepted method in `Account`. Also note how Byte Buddy is capable of auto-boxing the `Integer`
value. The third parameter of the `Bank`'s obfuscation method is annotated with `@Super` which instructs
Byte Buddy to create a proxy that allows to call the non-intercepted (`super`) implementations of the extended type.

For the given implementation of a `Bank`, we can now create a `BankAccount` using `ByteBuddy`:

```java
Class<? extends Account> dynamicType = new ByteBuddy()
  .subclass(Account.class)
  .name("BankAccount")
  .method(isAnnotatedBy(Unsafe.class)).intercept(MethodDelegation.to(Bank.class))
  .annotateType(new Secured() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Secured.class;
    }
  })
  .implement(Serializable.class)
  .make()
  .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
  .getLoaded();
assertThat(dynamicType.getName(), is("BankAccount"));
assertThat(dynamicType.isAnnotationPresent(Secured.class), is(true));
assertThat(Serializable.class.isAssignableFrom(dynamicType), is(true));
assertThat(dynamicType.newInstance().transfer(26, "123456"),
    is("transferred $26 to 123XXX (obfuscated)"));
```

As obvious from the test results, the dynamically generated `BankAccount` class works as expected. We instructed
Byte Buddy to intercept any method call to methods that are annotated by the `Unsafe` annotation which only concerns
the `Account`'s `transfer` method. We then define to intercept the method call to be delegated by the
`MethodDelegation` instrumentation which will automatically detect the only `static` method of `Bank` as a possible
delegation target. The documentation of the `MethodDelegation` class gives details on how a method delegation is
discovered and how it can be specified in further detail.

Since all transactions will now be processed by a `Bank`, we can mark the new class as `Secured`. We can add this
annotation by simply handing over an instance of the annotation to add to the class. Since Java annotations are
nothing more than interfaces, this is straight-forward, even though it might appear a little strange at first. Finally,
we instruct the `Serializable` interface to be implemented by the created type.

The created type will be written directly in the Java class file format and in Java byte code and never exist as Java
source code or as the source code of another JVM language. However, if you had written a class like the one we just
created with Byte Buddy in Java, you would end up with the following Java source code:

```java
@Secured
class BankAccount extends Account implements Serializable {

  private class Proxy extends Account {

    @Override
    public String transfer(int amount, String recipientAccount) {
      return BankAccount.super.transfer(amount, recipientAccount);
    }

    // Omitted overridable methods of java.lang.Object which are also
    // implemented to call the super method implementations of the outer
    // BankAccount instance.
  }

  @Override
  public String transfer(int amount, String recipientAccount) {
    return Bank.obfuscate(recipientAccount, new Integer(amount), new Proxy());
  }
}
```

You can check out the documentation of the `MethodDelegation` class for more information. There are plenty of other
options for delegation such as delegating to a class or an instance member. And there are other instrumentations that
ship with ByteBuddy and were not yet mentioned. One of them allows the implementation of `StubMethod`s. The
`Exceptional` instrumentation allows to throw exceptions. One can conduct a `SuperMethodCall` or implement a
`FieldAccessor`. Or one can adapt Java Class Library proxies by using an `InvocationHandlerAdapter`. Just as for the
`MethodDelegation`, the Java documentation is a good place to getting started. Give Byte Buddy a try! You will like it.

Where to go from here?
----------------------

Byte Buddy is a comprehensive library and we only scratched the surface of Byte Buddy's capabilities. However, Byte
Buddy aims for being easy to use by providing a domain-specific language for creating classes. Most runtime code
generation can be done by writing readable code and without any knowledge of Java's class file format. If you want
to learn more about Byte Buddy, you can find such a [tutorial on Byte Buddy's web page](http://bytebuddy.net/#/tutorial).
Furthermore, Byte Buddy comes with a [detailed in-code documentation](http://bytebuddy.net/javadoc/) and extensive 
test case coverage which can also serve as example code. When using Byte Buddy, make also sure to read the
following information on maintaining a project dependency.

Dependency and API evolution
----------------------------

Byte Buddy is written on top of [ASM](http://asm.ow2.org/), a mature and well-tested library for reading and writing
compiled Java classes. In order to allow for advanced type manipulations, Byte Buddy is intentionally exposing the
ASM API to its users. Of course, the direct use of ASM remains fully optional and most users will most likely never
require it. This choice was made such that a user of Byte Buddy is not restrained to its higher-level functionality
but can implement custom instrumentations without a fuzz when it is necessary.

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
different ASM versions that were repackaged by Byte Buddy, if you have not follow this recommendation! In contrast, the
Byte Buddy API itself will only apply version compatible changes.

There exist several tools that allow for an easy automatization of the repacking of dependencies during your build
processes. You can for example use the
[Shade plugin](http://maven.apache.org/plugins/maven-shade-plugin/examples/class-relocation.html) for Maven. With
Gradle, a similar tool is the [Shadow plugin](https://github.com/johnrengelman/shadow). Another alternative is
<a href="http://code.google.com/p/jarjar/">jarjar</a>, a library that offers integration as an Ant task.

License and development
-----------------------

Byte Buddy is licensed under the liberal and business-friendly
[*Apache Licence, Version 2.0*](http://www.apache.org/licenses/LICENSE-2.0.html) and is freely available on this
GitHub page. Byte Buddy is further released on Maven Central. The project is built using
<a href="http://maven.apache.org/">Maven</a>. From your shell, building the project would look something like this:

```shell
git clone https://github.com/raphw/byte-buddy.git
cd byte-buddy
mvn package
```

On these commands, Byte Buddy is cloned from GitHub and built on your machine. Byte Buddy is currently tested for the
[*OpenJDK*](http://openjdk.java.net/) versions 6 and 7 and the *Oracle JDK* versions 7 and 8 using Travis CI. Please
use GitHub's [issue tracker](https://github.com/raphw/byte-buddy/issues) for reporting bugs. When committing code,
please provide test cases that prove the functionality of your features or that demonstrate a bug fix. Furthermore,
make sure you are not breaking any existing test cases. If possible, please take the time to write some documentation.
For feature requests or general feedback, you can also use the 
[issue tracker](https://github.com/raphw/byte-buddy/issues) or contact us on 
[our mailing list](https://groups.google.com/forum/#!forum/byte-buddy).

[![Build Status](https://travis-ci.org/raphw/byte-buddy.png)](https://travis-ci.org/raphw/byte-buddy) [![Coverage Status](https://coveralls.io/repos/raphw/byte-buddy/badge.png?branch=master)](https://coveralls.io/r/raphw/byte-buddy?branch=master)

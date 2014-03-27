What is Byte Buddy?
-------------------

Byte Buddy is a code generation library for creating Java classes without a compiler but during the runtime of a Java
application. Other than the code generation utilities that ship with the Java Class Library, Byte Buddy allows the
creation of arbitrary classes and is not limited to implementing interfaces for the creation of runtime proxies.

In order to use Byte Buddy, one does not require an understanding of Java byte code or the class file format. In
contrast, Byte Buddy’s API aims to allow for writing code that is concise and easy to understand to everybody.
Nevertheless, Byte Buddy remains fully customizable down to the possibility of defining custom byte code. Furthermore,
the API was designed to be as non-intrusive as possible and as a result, Byte Buddy does not leave any trace in the
classes that were created by it. Thus, the latter can exist without requiring Byte Buddy to be found on the class path.
For this feature, Byte Buddy’s mascot is a ghost.

Byte Buddy is written in Java 6 but supports the generation of classes for any Java version. Byte Buddy is a light-weight
library and only depends on the visitor API of the Java byte code parser library ASM which does itself not require any
further dependencies.

At first sight, runtime code generation can appear to be some sort of black magic that should be avoided. And not
many developers write applications that generate code during their runtime. However, this picture changes when
creating libraries that need to interact with their users’ code and their unknown type systems. In this context,
a library implementer must often choose between either requiring a user to implement library-proprietary interfaces
or to generate code at runtime when the user’s type system becomes first known to the library. Many known libraries
such as for example Spring or Hibernate choose the latter approach which is popular among their users under the term
of using “Plain Old Java Objects”. Byte Buddy is an attempt to innovate the runtime creation of Java types in order
to provide a better toolset to those needing such functionality.

Hello World
-----------

Saying “Hello World” with Byte Buddy is as easy as it can get. Any creation of a Java class starts with an instance
of the ByteBuddy class which represents a configuration for creating new types:

```java
Class<?> dynamicType = new ByteBuddy()
  .subclass(Object.class) 
  .method(named("toString")) .intercept(FixedValue.value("Hello World!"))
  .make()
  .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
  .getLoaded();
assertThat(dynamicType.newInstance().toString(), is("Hello World!"));
```

The default ByteBuddy configuration which is used in the above example will create a Java class in that version of
the class file format that is represented by the currently running Java virtual machine. As hopefully obvious from
the example code, the created type will extend the Object class and intercept its “toString” method which should
return a fixed value of “Hello World!”. The method to be intercepted is identified by a method matcher. In the
example, a predefined method matcher is used that identifies a method by its exact name. Byte Buddy comes with
numerous predefined and well-tested method matchers. The creation of custom matchers is however as simple as
implementing the MethodMatcher interface.

For implementing the toString method, the FixedValue class defines a fixed return type of the desired method result.
Defining a fixed value is only one example of many method interceptors that ship with Byte Buddy. By implementing the
Instrumentation interface, a method could be defined by custom byte code, if this was desired.

Finally, the described type is created and then loaded into the Java virtual machine. For this purpose, a target
class loader is required as well as a class loading strategy where we choose a wrapper strategy that creates a new
child class loader which wraps the given class loader and only knows about the newly created dynamic type. Now we
can now convince ourselves of the result by calling the toString method and finding the return value to represent
the result we expected.

A more complex example
----------------------

Of course, a Hello World example is a too simple use case for evaluating the quality of a code generation library.
In reality, the user of such a library wants to perform more complex manipulations such as introducing additional
logic to a compiled Java program. Using Byte Buddy, doing so is however not much harder and the following example
will give a taste of how method calls can be intercepted.

For this demonstration, let us make up a simple pseudo domain where `Account`s can be used for transferring money
to given recipient's account which is represented by a simple string. Furthermore, we want to express that the use
of the `transfer` method is somewhat unsafe which is why we annotate the method with `@Unsafe`.

```java
@Retention(RetentionPolicy.RUNTIME)
@interface Unsafe { }

@Retention(RetentionPolicy.RUNTIME)
@interface Secured { }

public static class Account {
  private int amount = 100;
  @Unsafe
  public String transfer(int amount, String recipientAccount) {
    this.amount -= amount;
    return "transferred $" + amount + " to " + recipientAccount;
  }
}
```

In order to make a transaction safe, we rather want to process it by some `Bank`. For this purpose, the bank
provides an obfuscation but logs the transaction details internally (and to your console). It will then conduct
the transaction for the customer. With the help of Byte Buddy, we will now create a subclass of `Account` that
processes all its annotations by using a `Bank`.

```java
public static class Bank {
  public static String obfuscate(@Argument(1) String recipientAccount,
                                 @Argument(0) Integer amount,
                                 @Super Account zuper) {
    System.out.println("Transfer " + amount + " to " + recipientAccount);
    return zuper.transfer(amount, recipientAccount.substring(0, 3) + "XXX") + " (obfuscated)";
  }
}
```

Note the annotations on the `Bank`'s obfuscation method's parameters. The first both arguments are annotated
by `@Argument(n)` which will instruct Byte Buddy to inject the `n`-th argument of any intercepted method into
the annotated parameter. Further, note that the order of the parameters of the `Bank`'s obfuscation method
is opposite to the intercepted method in `Account` and how Byte Buddy is capable of auto-boxing the `Integer`
value. The third parameter of the `Bank`'s obfuscation method is annotated with `@Super` which instructs
Byte Buddy to create a proxy which allows to call the non-intercepted methods of the subclassed instance.

With the given implementation of a `Bank`, we can now create a `BankAccount` by using `ByteBuddy`:

```java
Class<? extends Account> dynamicType = new ByteBuddy()
  .subclass(Account.class)
  .implement(Serializable.class)
  .name("BankAccount")
  .method(isAnnotatedBy(Unsafe.class)).intercept(MethodDelegation.to(Bank.class))
  .annotateType(new Secured() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Secured.class;
    }
  })
  .make()
  .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
  .getLoaded();
assertThat(dynamicType.getName(), is("BankAccount));
assertThat(dynamicType.isAnnotationPresent(Secured.class), is(true));
assertThat(dynamicType.newInstance().transfer(26, "123456"), is("transferred $26 to 123XXX (obfuscated)"));
```

As obvious from the test results, the dynamically generated `BankAccount` class works as expected. We instructed
Byte Buddy to intercept any method call to methods that are annotated by the `Unsafe` annotation which only concerns
the `Account`'s `transfer` method. We then define to intercept the method call to be delegated by the
`MethodDelegation` instrumentation which will automatically detect the only `static` method of `Bank` as a possible
delegation target. The documentation of the `MethodDelegation` class gives details advice on how a method delegation
can be specified in further detail.

Since all transactions will now be processed by a `Bank`, we can mark the new class as `Secured`. We can add such an
annotation by simply handing over an instance of the annotation to implement. Since Java annotations are simply
represented by interfaces, this is straight-forward, even though it might appear a little strange at first.

The dynamic type will be created directly in Java byte code. By the way, if you had created a class like the one we
just created at runtime with a Java compiler, you would write the following source code:

```java
@Secured
class BankAccount extends Account {

  private class Proxy extends Account {

    @Override
    public String transfer(int amount, String recipientAccount) {
      return BankAccount.super.transfer(amount, recipientAccount);
    }
  }

  @Override
  public String transfer(int amount, String recipientAccount) {
    return Bank.obfuscate(recipientAccount, new Integer(amount), new Proxy());
  }
}
```

Byte Buddy
==============

Byte Buddy is an effort to modernize run time class generation for Java. With ASM, there exists a fast and powerful library for processing byte code using a Java API but unfortunately, creating classes with ASM is verbose and difficult to apply. For this reason, libraries like cglib - which builds on ASM - were created. Cglib allows to express instrumentations without the need to know about the details of Java byte code. Unfortunately, cglib's implementation is quite messy and the project deserted and many modern Java features, such as annotations, did not find their way into cglib. Byte Buddy wants to fill this gap and allow library implementors access to easy, high speed class creation.

I have considered writing such a library for ever but I only started working on Byte Buddy in November 2013. Planned time schedule:

1. Initial architecture draft (til end of november 2013)
2. Experimenting with end user API (til end of december 2013)
3. General implementation (til end of feburary 2014)
4. Writing additional test cases and java doc (til end of march 2014)
5. Website / Documentation / Publication (til end of june 2014)

How does the API work?
------------------

A simple example:

```java
new ByteBuddy()
    .subclass(Object.class)
    .intercept(named("toString"), ConstantValue.of("Hello World!"))
    .make().load().getClass().newInstance().toString();
    // Will return 'Hello World'.
```

A more complex example:

```java
class MyDelegate {
  public static boolean equals(@Argument(0) Object arg, 
                               @SuperCall Callable<Boolean> superMethod) {
    return arg.getClass() == Object.class || superMethod.call();
  }
}

new ByteBuddy()
    .subclass(Object.class)
    .intercept(named("equals"), Delegate.to(MyDelegate.class))
    .make().load().getClass().newInstance().equals(new Object());
    // Will return true.
```

One target of the library is to work with annotations in order to avoid the array boxing that is enforced by existing libraries such as cglib or javassist.

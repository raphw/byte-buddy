# Byte Buddy Android

**Byte Buddy Android** allows you to generate classes on Android and to load them into the current Android VM process. To load classes on Android, the `AndroidClassLoadingStrategy` must be used when loading a dynamic type:

```java
ClassLoadingStrategy strategy = new AndroidClassLoadingStrategy.Wrapping(context.getDir(
  "generated", 
  Context.MODE_PRIVATE));

Class<?> dynamicType = new ByteBuddy()
  .subclass(Object.class)
  .method(ElementMatchers.named("toString"))
  .intercept(FixedValue.value("Hello World!"))
  .make()
  .load(getClass().getClassLoader(), strategy)
  .getLoaded();
assertThat(dynamicType.newInstance().toString(), is("Hello World!"))
```

Using the strategy requires Android with support for API version 21 or later. A wrapping and an injecting class loading strategy is offered, similar to Byte Buddy's standard strategies. On Android, it is not possible to transform loaded classes or to register a Java agent.

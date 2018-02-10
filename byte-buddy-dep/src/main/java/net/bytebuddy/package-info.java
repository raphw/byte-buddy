/**
 * Byte Buddy is a library for creating Java classes at runtime of a Java program. For this purpose, the
 * {@link net.bytebuddy.ByteBuddy} class serves as an entry point. The following example
 * <pre>
 * Class&#60;?&#62; dynamicType = new ByteBuddy()
 *    .subclass(Object.class)
 *    .implement(Serializable.class)
 *    .intercept(named("toString"), FixedValue.value("Hello World!"))
 *    .make()
 *    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
 *    .getLoaded();
 * dynamicType.newInstance().toString; // returns "Hello World!"</pre>
 * creates a subclass of the {@link java.lang.Object} class which implements the {@link java.io.Serializable}
 * interface. The {@link java.lang.Object#toString()} method is overridden to return {@code Hello World!}.
 */
package net.bytebuddy;

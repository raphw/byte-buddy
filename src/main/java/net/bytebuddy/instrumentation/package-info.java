/**
 * The instrumentation package contains any logic for intercepting method calls. The following instrumentations ship
 * with Byte Buddy:
 * <ol>
 * <li>{@link net.bytebuddy.instrumentation.Exceptional}: The exceptional interception allows to throw
 * {@link java.lang.Throwable} instances on a method call.</li>
 * <li>{@link net.bytebuddy.instrumentation.FieldAccessor}: A field accessor allows to read or write a class's field
 * value according to the Java bean specification, i.e. implements setter and getter methods.</li>
 * <li>{@link net.bytebuddy.instrumentation.InvocationHandlerAdapter}: An adapter for instrumenting methods by
 * delegating method calls to a {@link java.lang.reflect.InvocationHandler} which is already used for Java proxies.</li>
 * <li>{@link net.bytebuddy.instrumentation.MethodDelegation}: Allows to delegate a method call to either a {@code static}
 * or to an instance method. The method delegation is determined by annotations on the target method.</li>
 * <li>{@link net.bytebuddy.instrumentation.StubMethod}: A stub method overrides a method by an empty implementation
 * that only returns the method's return type's default value.</li>
 * <li>{@link net.bytebuddy.instrumentation.SuperMethodCall}: This instrumentation calls a method's super implementation.
 * This instrumentation is handy when annotations should be added to a method without changing the method's
 * implementation.</li>
 * </ol>
 */
package net.bytebuddy.instrumentation;

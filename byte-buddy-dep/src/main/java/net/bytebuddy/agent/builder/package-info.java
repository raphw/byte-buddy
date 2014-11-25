/**
 * An agent builder is used to easily implement load-time class-transformations using a Java agent. The API
 * builds on Java's {@link java.lang.instrument.ClassFileTransformer} and {@link java.lang.instrument.Instrumentation}
 * but offers higher-level APIs in order to allow for the implementation of very readable transformations using
 * {@link net.bytebuddy.ByteBuddy}.
 */
package net.bytebuddy.agent.builder;

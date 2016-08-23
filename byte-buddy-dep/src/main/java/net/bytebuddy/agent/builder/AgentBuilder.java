package net.bytebuddy.agent.builder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.*;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.NexusAccessor;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * <p>
 * An agent builder provides a convenience API for defining a
 * <a href="http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html">Java agent</a>. By default,
 * this transformation is applied by rebasing the type if not specified otherwise by setting a
 * {@link TypeStrategy}.
 * </p>
 * <p>
 * When defining several {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s, the agent builder always
 * applies the transformers that were supplied with the last applicable matcher. Therefore, more general transformers
 * should be defined first.
 * </p>
 * <p>
 * <b>Note</b>: Any transformation is performed using the {@link AccessControlContext} of an agent's creator.
 * </p>
 */
public interface AgentBuilder {

    /**
     * Defines the given {@link net.bytebuddy.ByteBuddy} instance to be used by the created agent.
     *
     * @param byteBuddy The Byte Buddy instance to be used.
     * @return A new instance of this agent builder which makes use of the given {@code byteBuddy} instance.
     */
    AgentBuilder with(ByteBuddy byteBuddy);

    /**
     * Defines the given {@link net.bytebuddy.agent.builder.AgentBuilder.Listener} to be notified by the created agent.
     * The given listener is notified after any other listener that is already registered. If a listener is registered
     * twice, it is also notified twice.
     *
     * @param listener The listener to be notified.
     * @return A new instance of this agent builder which creates an agent that informs the given listener about
     * events.
     */
    AgentBuilder with(Listener listener);

    /**
     * Defines the use of the given type locator for locating a {@link TypeDescription} for an instrumented type.
     *
     * @param poolStrategy The type locator to use.
     * @return A new instance of this agent builder which uses the given type locator for looking up class files.
     */
    AgentBuilder with(PoolStrategy poolStrategy);

    /**
     * Defines the use of the given location strategy for locating binary data to given class names.
     *
     * @param locationStrategy The location strategy to use.
     * @return A new instance of this agent builder which uses the given location strategy for looking up class files.
     */
    AgentBuilder with(LocationStrategy locationStrategy);

    /**
     * Defines how types should be transformed, e.g. if they should be rebased or redefined by the created agent.
     *
     * @param typeStrategy The type strategy to use.
     * @return A new instance of this agent builder which uses the given type strategy.
     */
    AgentBuilder with(TypeStrategy typeStrategy);

    /**
     * Defines a given initialization strategy to be applied to generated types. An initialization strategy is responsible
     * for setting up a type after it was loaded. This initialization must be performed after the transformation because
     * a Java agent is only invoked before loading a type. By default, the initialization logic is added to a class's type
     * initializer which queries a global object for any objects that are to be injected into the generated type.
     *
     * @param initializationStrategy The initialization strategy to use.
     * @return A new instance of this agent builder that applies the given initialization strategy.
     */
    AgentBuilder with(InitializationStrategy initializationStrategy);

    /**
     * Specifies a strategy for modifying types that were already loaded prior to the installation of this transformer.
     *
     * @param redefinitionStrategy The redefinition strategy to apply.
     * @return A new instance of this agent builder that applies the given redefinition strategy.
     */
    AgentBuilder with(RedefinitionStrategy redefinitionStrategy);

    /**
     * <p>
     * Enables or disables management of the JVM's {@code LambdaMetafactory} which is responsible for creating classes that
     * implement lambda expressions. Without this feature enabled, classes that are represented by lambda expressions are
     * not instrumented by the JVM such that Java agents have no effect on them when a lambda expression's class is loaded
     * for the first time.
     * </p>
     * <p>
     * When activating this feature, Byte Buddy instruments the {@code LambdaMetafactory} and takes over the responsibility
     * of creating classes that represent lambda expressions. In doing so, Byte Buddy has the opportunity to apply the built
     * class file transformer. If the current VM does not support lambda expressions, activating this feature has no effect.
     * </p>
     * <p>
     * <b>Important</b>: If this feature is active, it is important to release the built class file transformer when
     * deactivating it. Normally, it is sufficient to call {@link Instrumentation#removeTransformer(ClassFileTransformer)}.
     * When this feature is enabled, it is however also required to invoke
     * {@link LambdaInstrumentationStrategy#release(ClassFileTransformer, Instrumentation)}. Otherwise, the executing VMs class
     * loader retains a reference to the class file transformer what can cause a memory leak.
     * </p>
     *
     * @param lambdaInstrumentationStrategy {@code true} if this feature should be enabled.
     * @return A new instance of this agent builder where this feature is explicitly enabled or disabled.
     */
    AgentBuilder with(LambdaInstrumentationStrategy lambdaInstrumentationStrategy);

    /**
     * Specifies a strategy to be used for resolving {@link TypeDescription} for any type handled by the created transformer.
     *
     * @param descriptionStrategy The description strategy to use.
     * @return A new instance of this agent builder that applies the given description strategy.
     */
    AgentBuilder with(DescriptionStrategy descriptionStrategy);

    /**
     * Specifies an installation strategy that this agent builder applies upon installing an agent.
     *
     * @param installationStrategy The installation strategy to be used.
     * @return A new agent builder that applies the supplied installation strategy.
     */
    AgentBuilder with(InstallationStrategy installationStrategy);

    /**
     * Specifies a fallback strategy to that this agent builder applies upon installing an agent and during class file transformation.
     *
     * @param fallbackStrategy The fallback strategy to be used.
     * @return A new agent builder that applies the supplied fallback strategy.
     */
    AgentBuilder with(FallbackStrategy fallbackStrategy);

    /**
     * Enables class injection of auxiliary classes into the bootstrap class loader.
     *
     * @param instrumentation The instrumentation instance that is used for appending jar files to the
     *                        bootstrap class path.
     * @param folder          The folder in which jar files of the injected classes are to be stored.
     * @return An agent builder with bootstrap class loader class injection enabled.
     */
    AgentBuilder enableBootstrapInjection(Instrumentation instrumentation, File folder);

    /**
     * Enables the use of the given native method prefix for instrumented methods. Note that this prefix is also
     * applied when preserving non-native methods. The use of this prefix is also registered when installing the
     * final agent with an {@link java.lang.instrument.Instrumentation}.
     *
     * @param prefix The prefix to be used.
     * @return A new instance of this agent builder which uses the given native method prefix.
     */
    AgentBuilder enableNativeMethodPrefix(String prefix);

    /**
     * Disables the use of a native method prefix for instrumented methods.
     *
     * @return A new instance of this agent builder which does not use a native method prefix.
     */
    AgentBuilder disableNativeMethodPrefix();

    /**
     * Disables injection of auxiliary classes into the bootstrap class path.
     *
     * @return A new instance of this agent builder which does not apply bootstrap class loader injection.
     */
    AgentBuilder disableBootstrapInjection();

    /**
     * <p>
     * Disables all implicit changes on a class file that Byte Buddy would apply for certain instrumentations. When
     * using this option, it is no longer possible to rebase a method, i.e. intercepted methods are fully replaced. Furthermore,
     * it is no longer possible to implicitly apply loaded type initializers for explicitly initializing the generated type.
     * </p>
     * <p>
     * This is equivalent to setting {@link InitializationStrategy.NoOp} and {@link TypeStrategy.Default#REDEFINE_DECLARED_ONLY}
     * as well as configuring the underlying {@link ByteBuddy} instance to use a {@link net.bytebuddy.implementation.Implementation.Context.Disabled}.
     * </p>
     *
     * @return A new instance of this agent builder that does not apply any implicit changes to the received class file.
     */
    AgentBuilder disableClassFormatChanges();

    /**
     * Assures that all modules of the supplied types are read by the module of any instrumented type. If the current VM does not support
     * the Java module system, calling this method has no effect and this instance is returned.
     *
     * @param instrumentation The instrumentation instance that is used for adding a module read-dependency.
     * @param type            The types for which to assure their module-visibility from any instrumented class.
     * @return A new instance of this agent builder that assures the supplied types module visibility.
     * @see Listener.ModuleReadEdgeCompleting
     */
    AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Class<?>... type);

    /**
     * Assures that all supplied modules are read by the module of any instrumented type.
     *
     * @param instrumentation The instrumentation instance that is used for adding a module read-dependency.
     * @param module          The modules for which to assure their module-visibility from any instrumented class.
     * @return A new instance of this agent builder that assures the supplied types module visibility.
     * @see Listener.ModuleReadEdgeCompleting
     */
    AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, JavaModule... module);

    /**
     * Assures that all supplied modules are read by the module of any instrumented type.
     *
     * @param instrumentation The instrumentation instance that is used for adding a module read-dependency.
     * @param modules         The modules for which to assure their module-visibility from any instrumented class.
     * @return A new instance of this agent builder that assures the supplied types module visibility.
     * @see Listener.ModuleReadEdgeCompleting
     */
    AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules);

    /**
     * Assures that all modules of the supplied types are read by the module of any instrumented type and vice versa.
     * If the current VM does not support the Java module system, calling this method has no effect and this instance is returned.
     *
     * @param instrumentation The instrumentation instance that is used for adding a module read-dependency.
     * @param type            The types for which to assure their module-visibility from and to any instrumented class.
     * @return A new instance of this agent builder that assures the supplied types module visibility.
     * @see Listener.ModuleReadEdgeCompleting
     */
    AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Class<?>... type);

    /**
     * Assures that all supplied modules are read by the module of any instrumented type and vice versa.
     *
     * @param instrumentation The instrumentation instance that is used for adding a module read-dependency.
     * @param module          The modules for which to assure their module-visibility from and to any instrumented class.
     * @return A new instance of this agent builder that assures the supplied types module visibility.
     * @see Listener.ModuleReadEdgeCompleting
     */
    AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, JavaModule... module);

    /**
     * Assures that all supplied modules are read by the module of any instrumented type and vice versa.
     *
     * @param instrumentation The instrumentation instance that is used for adding a module read-dependency.
     * @param modules         The modules for which to assure their module-visibility from and to any instrumented class.
     * @return A new instance of this agent builder that assures the supplied types module visibility.
     * @see Listener.ModuleReadEdgeCompleting
     */
    AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules);

    /**
     * <p>
     * Matches a type being loaded in order to apply the supplied {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     * If several matchers positively match a type only the latest registered matcher is considered for transformation.
     * </p>
     * <p>
     * If this matcher is chained with additional subsequent matchers, this matcher is always executed first whereas the following matchers are
     * executed in the order of their execution. If any matcher indicates that a type is to be matched, none of the following matchers is still queried.
     * This behavior can be changed by {@link Identified.Extendable#asDecorator()} where subsequent type matchers are also applied.
     * </p>
     * <p>
     * <b>Note</b>: When applying a matcher, regard the performance implications by {@link AgentBuilder#ignore(ElementMatcher)}. The former
     * matcher is applied first such that it makes sense to ignore name spaces that are irrelevant to instrumentation. If possible, it is
     * also recommended, to exclude class loaders such as for example the bootstrap class loader by using
     * {@link AgentBuilder#type(ElementMatcher, ElementMatcher)} instead.
     * </p>
     *
     * @param typeMatcher An {@link net.bytebuddy.matcher.ElementMatcher} that is applied on the type being loaded that
     *                    decides if the entailed {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should
     *                    be applied for that type.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when the given {@code typeMatcher}
     * indicates a match.
     */
    Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher);

    /**
     * <p>
     * Matches a type being loaded in order to apply the supplied {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     * If several matchers positively match a type only the latest registered matcher is considered for transformation.
     * </p>
     * <p>
     * If this matcher is chained with additional subsequent matchers, this matcher is always executed first whereas the following matchers are
     * executed in the order of their execution. If any matcher indicates that a type is to be matched, none of the following matchers is still queried.
     * This behavior can be changed by {@link Identified.Extendable#asDecorator()} where subsequent type matchers are also applied.
     * </p>
     * <p>
     * <b>Note</b>: When applying a matcher, regard the performance implications by {@link AgentBuilder#ignore(ElementMatcher)}. The former
     * matcher is applied first such that it makes sense to ignore name spaces that are irrelevant to instrumentation. If possible, it
     * is also recommended, to exclude class loaders such as for example the bootstrap class loader.
     * </p>
     *
     * @param typeMatcher        An {@link net.bytebuddy.matcher.ElementMatcher} that is applied on the type being
     *                           loaded that decides if the entailed
     *                           {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be applied for
     *                           that type.
     * @param classLoaderMatcher An {@link net.bytebuddy.matcher.ElementMatcher} that is applied to the
     *                           {@link java.lang.ClassLoader} that is loading the type being loaded. This matcher
     *                           is always applied first where the type matcher is not applied in case that this
     *                           matcher does not indicate a match.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when both the given
     * {@code typeMatcher} and {@code classLoaderMatcher} indicate a match.
     */
    Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher);

    /**
     * <p>
     * Matches a type being loaded in order to apply the supplied {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     * If several matchers positively match a type only the latest registered matcher is considered for transformation.
     * </p>
     * <p>
     * If this matcher is chained with additional subsequent matchers, this matcher is always executed first whereas the following matchers are
     * executed in the order of their execution. If any matcher indicates that a type is to be matched, none of the following matchers is still queried.
     * This behavior can be changed by {@link Identified.Extendable#asDecorator()} where subsequent type matchers are also applied.
     * </p>
     * <p>
     * <b>Note</b>: When applying a matcher, regard the performance implications by {@link AgentBuilder#ignore(ElementMatcher)}. The former
     * matcher is applied first such that it makes sense to ignore name spaces that are irrelevant to instrumentation. If possible, it
     * is also recommended, to exclude class loaders such as for example the bootstrap class loader.
     * </p>
     *
     * @param typeMatcher        An {@link net.bytebuddy.matcher.ElementMatcher} that is applied on the type being
     *                           loaded that decides if the entailed
     *                           {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be applied for
     *                           that type.
     * @param classLoaderMatcher An {@link net.bytebuddy.matcher.ElementMatcher} that is applied to the
     *                           {@link java.lang.ClassLoader} that is loading the type being loaded. This matcher
     *                           is always applied second where the type matcher is not applied in case that this
     *                           matcher does not indicate a match.
     * @param moduleMatcher      An {@link net.bytebuddy.matcher.ElementMatcher} that is applied to the {@link JavaModule}
     *                           of the type being loaded. This matcher is always applied first where the class loader and
     *                           type matchers are not applied in case that this matcher does not indicate a match. On a JVM
     *                           that does not support the Java modules system, this matcher is not applied.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when both the given
     * {@code typeMatcher} and {@code classLoaderMatcher} indicate a match.
     */
    Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher,
                               ElementMatcher<? super ClassLoader> classLoaderMatcher,
                               ElementMatcher<? super JavaModule> moduleMatcher);

    /**
     * <p>
     * Matches a type being loaded in order to apply the supplied {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     * If several matchers positively match a type only the latest registered matcher is considered for transformation.
     * </p>
     * <p>
     * If this matcher is chained with additional subsequent matchers, this matcher is always executed first whereas the following matchers are
     * executed in the order of their execution. If any matcher indicates that a type is to be matched, none of the following matchers is still queried.
     * </p>
     * <p>
     * <b>Note</b>: When applying a matcher, regard the performance implications by {@link AgentBuilder#ignore(ElementMatcher)}. The former
     * matcher is applied first such that it makes sense to ignore name spaces that are irrelevant to instrumentation. If possible, it
     * is also recommended, to exclude class loaders such as for example the bootstrap class loader.
     * </p>
     *
     * @param matcher A matcher that decides if the entailed {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be
     *                applied for a type that is being loaded.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when the given {@code matcher}
     * indicates a match.
     */
    Identified.Narrowable type(RawMatcher matcher);

    /**
     * <p>
     * Excludes any type that is matched by the provided matcher from instrumentation and considers types by all {@link ClassLoader}s.
     * By default, Byte Buddy does not instrument synthetic types or types that are loaded by the bootstrap class loader.
     * </p>
     * <p>
     * When ignoring a type, any subsequently chained matcher is applied after this matcher in the order of their registration. Also, if
     * any matcher indicates that a type is to be ignored, none of the following chained matchers is executed.
     * </p>
     * <p>
     * <b>Note</b>: For performance reasons, it is recommended to always include a matcher that excludes as many namespaces
     * as possible. Byte Buddy can determine a type's name without parsing its class file and can therefore discard such
     * types with minimal overhead. When a different property of a type - such as for example its modifiers or its annotations
     * is accessed - Byte Buddy parses the class file lazily in order to allow for such a matching. Therefore, any exclusion
     * of a name should always be done as a first step and even if it does not influence the selection of what types are
     * matched. Without changing this property, the class file of every type is being parsed!
     * </p>
     * <p>
     * <b>Warning</b>: If a type is loaded during the instrumentation of the same type, this causes the original call site that loads the type
     * to remain unbound, causing a {@link LinkageError}. It is therefore important to not instrument types that may be loaded during the application
     * of a {@link Transformer}. For this reason, it is not recommended to instrument classes of the bootstrap class loader that Byte Buddy might
     * require for instrumenting a class or to instrument any of Byte Buddy's classes. If such instrumentation is desired, it is important to
     * assert for each class that they are not loaded during instrumentation.
     * </p>
     *
     * @param typeMatcher A matcher that identifies types that should not be instrumented.
     * @return A new instance of this agent builder that ignores all types that are matched by the provided matcher.
     * All previous matchers for ignored types are discarded.
     */
    Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher);

    /**
     * <p>
     * Excludes any type that is matched by the provided matcher and is loaded by a class loader matching the second matcher.
     * By default, Byte Buddy does not instrument synthetic types, types within a {@code net.bytebuddy.*} package or types that
     * are loaded by the bootstrap class loader.
     * </p>
     * <p>
     * When ignoring a type, any subsequently chained matcher is applied after this matcher in the order of their registration. Also, if
     * any matcher indicates that a type is to be ignored, none of the following chained matchers is executed.
     * </p>
     * <p>
     * <b>Note</b>: For performance reasons, it is recommended to always include a matcher that excludes as many namespaces
     * as possible. Byte Buddy can determine a type's name without parsing its class file and can therefore discard such
     * types with minimal overhead. When a different property of a type - such as for example its modifiers or its annotations
     * is accessed - Byte Buddy parses the class file lazily in order to allow for such a matching. Therefore, any exclusion
     * of a name should always be done as a first step and even if it does not influence the selection of what types are
     * matched. Without changing this property, the class file of every type is being parsed!
     * </p>
     * <p>
     * <b>Warning</b>: If a type is loaded during the instrumentation of the same type, this causes the original call site that loads the type
     * to remain unbound, causing a {@link LinkageError}. It is therefore important to not instrument types that may be loaded during the application
     * of a {@link Transformer}. For this reason, it is not recommended to instrument classes of the bootstrap class loader that Byte Buddy might
     * require for instrumenting a class or to instrument any of Byte Buddy's classes. If such instrumentation is desired, it is important to
     * assert for each class that they are not loaded during instrumentation.
     * </p>
     *
     * @param typeMatcher        A matcher that identifies types that should not be instrumented.
     * @param classLoaderMatcher A matcher that identifies a class loader that identifies classes that should not be instrumented.
     * @return A new instance of this agent builder that ignores all types that are matched by the provided matcher.
     * All previous matchers for ignored types are discarded.
     */
    Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher);

    /**
     * <p>
     * Excludes any type that is matched by the provided matcher and is loaded by a class loader matching the second matcher.
     * By default, Byte Buddy does not instrument synthetic types, types within a {@code net.bytebuddy.*} package or types that
     * are loaded by the bootstrap class loader.
     * </p>
     * <p>
     * When ignoring a type, any subsequently chained matcher is applied after this matcher in the order of their registration. Also, if
     * any matcher indicates that a type is to be ignored, none of the following chained matchers is executed.
     * </p>
     * <p>
     * <b>Note</b>: For performance reasons, it is recommended to always include a matcher that excludes as many namespaces
     * as possible. Byte Buddy can determine a type's name without parsing its class file and can therefore discard such
     * types with minimal overhead. When a different property of a type - such as for example its modifiers or its annotations
     * is accessed - Byte Buddy parses the class file lazily in order to allow for such a matching. Therefore, any exclusion
     * of a name should always be done as a first step and even if it does not influence the selection of what types are
     * matched. Without changing this property, the class file of every type is being parsed!
     * </p>
     * <p>
     * <b>Warning</b>: If a type is loaded during the instrumentation of the same type, this causes the original call site that loads the type
     * to remain unbound, causing a {@link LinkageError}. It is therefore important to not instrument types that may be loaded during the application
     * of a {@link Transformer}. For this reason, it is not recommended to instrument classes of the bootstrap class loader that Byte Buddy might
     * require for instrumenting a class or to instrument any of Byte Buddy's classes. If such instrumentation is desired, it is important to
     * assert for each class that they are not loaded during instrumentation.
     * </p>
     *
     * @param typeMatcher        A matcher that identifies types that should not be instrumented.
     * @param classLoaderMatcher A matcher that identifies a class loader that identifies classes that should not be instrumented.
     * @param moduleMatcher      A matcher that identifies a module that identifies classes that should not be instrumented. On a JVM
     *                           that does not support the Java modules system, this matcher is not applied.
     * @return A new instance of this agent builder that ignores all types that are matched by the provided matcher.
     * All previous matchers for ignored types are discarded.
     */
    Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher,
                   ElementMatcher<? super ClassLoader> classLoaderMatcher,
                   ElementMatcher<? super JavaModule> moduleMatcher);

    /**
     * <p>
     * Excludes any type that is matched by the raw matcher provided to this method. By default, Byte Buddy does not
     * instrument synthetic types, types within a {@code net.bytebuddy.*} package or types that are loaded by the bootstrap class loader.
     * </p>
     * <p>
     * When ignoring a type, any subsequently chained matcher is applied after this matcher in the order of their registration. Also, if
     * any matcher indicates that a type is to be ignored, none of the following chained matchers is executed.
     * </p>
     * <p>
     * <b>Note</b>: For performance reasons, it is recommended to always include a matcher that excludes as many namespaces
     * as possible. Byte Buddy can determine a type's name without parsing its class file and can therefore discard such
     * types with minimal overhead. When a different property of a type - such as for example its modifiers or its annotations
     * is accessed - Byte Buddy parses the class file lazily in order to allow for such a matching. Therefore, any exclusion
     * of a name should always be done as a first step and even if it does not influence the selection of what types are
     * matched. Without changing this property, the class file of every type is being parsed!
     * </p>
     * <p>
     * <b>Warning</b>: If a type is loaded during the instrumentation of the same type, this causes the original call site that loads the type
     * to remain unbound, causing a {@link LinkageError}. It is therefore important to not instrument types that may be loaded during the application
     * of a {@link Transformer}. For this reason, it is not recommended to instrument classes of the bootstrap class loader that Byte Buddy might
     * require for instrumenting a class or to instrument any of Byte Buddy's classes. If such instrumentation is desired, it is important to
     * assert for each class that they are not loaded during instrumentation.
     * </p>
     *
     * @param rawMatcher A raw matcher that identifies types that should not be instrumented.
     * @return A new instance of this agent builder that ignores all types that are matched by the provided matcher.
     * All previous matchers for ignored types are discarded.
     */
    Ignored ignore(RawMatcher rawMatcher);

    /**
     * Creates a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of this
     * agent builder.
     *
     * @return A class file transformer that implements the configuration of this agent builder.
     */
    ClassFileTransformer makeRaw();

    /**
     * <p>
     * Creates and installs a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of
     * this agent builder with a given {@link java.lang.instrument.Instrumentation}. If retransformation is enabled,
     * the installation also causes all loaded types to be retransformed.
     * </p>
     * <p>
     * If installing the created class file transformer causes an exception to be thrown, the consequences of this
     * exception are determined by the {@link InstallationStrategy} of this builder.
     * </p>
     *
     * @param instrumentation The instrumentation on which this agent builder's configuration is to be installed.
     * @return The installed class file transformer.
     */
    ClassFileTransformer installOn(Instrumentation instrumentation);

    /**
     * Creates and installs a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of
     * this agent builder with the Byte Buddy-agent which must be installed prior to calling this method.
     *
     * @return The installed class file transformer.
     * @see AgentBuilder#installOn(Instrumentation)
     */
    ClassFileTransformer installOnByteBuddyAgent();

    /**
     * An abstraction for extending a matcher.
     *
     * @param <T> The type that is produced by chaining a matcher.
     */
    interface Matchable<T extends Matchable<T>> {

        /**
         * Defines a matching that is positive if both the previous matcher and the supplied matcher are matched. When matching a
         * type, class loaders are not considered.
         *
         * @param typeMatcher A matcher for the type being matched.
         * @return A chained matcher.
         */
        T and(ElementMatcher<? super TypeDescription> typeMatcher);

        /**
         * Defines a matching that is positive if both the previous matcher and the supplied matcher are matched.
         *
         * @param typeMatcher        A matcher for the type being matched.
         * @param classLoaderMatcher A matcher for the type's class loader.
         * @return A chained matcher.
         */
        T and(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher);

        /**
         * Defines a matching that is positive if both the previous matcher and the supplied matcher are matched.
         *
         * @param typeMatcher        A matcher for the type being matched.
         * @param classLoaderMatcher A matcher for the type's class loader.
         * @param moduleMatcher      A matcher for the type's module. On a JVM that does not support modules, the Java module is represented by {@code null}.
         * @return A chained matcher.
         */
        T and(ElementMatcher<? super TypeDescription> typeMatcher,
              ElementMatcher<? super ClassLoader> classLoaderMatcher,
              ElementMatcher<? super JavaModule> moduleMatcher);

        /**
         * Defines a matching that is positive if both the previous matcher and the supplied matcher are matched.
         *
         * @param rawMatcher A raw matcher for the type being matched.
         * @return A chained matcher.
         */
        T and(RawMatcher rawMatcher);

        /**
         * Defines a matching that is positive if the previous matcher or the supplied matcher are matched. When matching a
         * type, the class loader is not considered.
         *
         * @param typeMatcher A matcher for the type being matched.
         * @return A chained matcher.
         */
        T or(ElementMatcher<? super TypeDescription> typeMatcher);

        /**
         * Defines a matching that is positive if the previous matcher or the supplied matcher are matched.
         *
         * @param typeMatcher        A matcher for the type being matched.
         * @param classLoaderMatcher A matcher for the type's class loader.
         * @return A chained matcher.
         */
        T or(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher);

        /**
         * Defines a matching that is positive if the previous matcher or the supplied matcher are matched.
         *
         * @param typeMatcher        A matcher for the type being matched.
         * @param classLoaderMatcher A matcher for the type's class loader.
         * @param moduleMatcher      A matcher for the type's module. On a JVM that does not support modules, the Java module is represented by {@code null}.
         * @return A chained matcher.
         */
        T or(ElementMatcher<? super TypeDescription> typeMatcher,
             ElementMatcher<? super ClassLoader> classLoaderMatcher,
             ElementMatcher<? super JavaModule> moduleMatcher);

        /**
         * Defines a matching that is positive if the previous matcher or the supplied matcher are matched.
         *
         * @param rawMatcher A raw matcher for the type being matched.
         * @return A chained matcher.
         */
        T or(RawMatcher rawMatcher);

        /**
         * An abstract base implementation of a matchable.
         *
         * @param <S> The type that is produced by chaining a matcher.
         */
        abstract class AbstractBase<S extends Matchable<S>> implements Matchable<S> {

            @Override
            public S and(ElementMatcher<? super TypeDescription> typeMatcher) {
                return and(typeMatcher, any());
            }

            @Override
            public S and(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return and(typeMatcher, classLoaderMatcher, any());
            }

            @Override
            public S and(ElementMatcher<? super TypeDescription> typeMatcher,
                         ElementMatcher<? super ClassLoader> classLoaderMatcher,
                         ElementMatcher<? super JavaModule> moduleMatcher) {
                return and(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher));
            }

            @Override
            public S or(ElementMatcher<? super TypeDescription> typeMatcher) {
                return or(typeMatcher, any());
            }

            @Override
            public S or(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return or(typeMatcher, classLoaderMatcher, any());
            }

            @Override
            public S or(ElementMatcher<? super TypeDescription> typeMatcher,
                        ElementMatcher<? super ClassLoader> classLoaderMatcher,
                        ElementMatcher<? super JavaModule> moduleMatcher) {
                return or(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher));
            }
        }
    }

    /**
     * Allows to further specify ignored types.
     */
    interface Ignored extends Matchable<Ignored>, AgentBuilder {
        /* this is merely a unionizing interface that does not declare methods */
    }

    /**
     * Describes an {@link net.bytebuddy.agent.builder.AgentBuilder} which was handed a matcher for identifying
     * types to instrumented in order to supply one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s.
     */
    interface Identified {

        /**
         * Applies the given transformer for the already supplied matcher.
         *
         * @param transformer The transformer to apply.
         * @return A new instance of this agent builder with the transformer being applied when the previously supplied matcher
         * identified a type for instrumentation which also allows for the registration of subsequent transformers.
         */
        Extendable transform(Transformer transformer);

        /**
         * Allows to specify a type matcher for a type to instrument.
         */
        interface Narrowable extends Matchable<Narrowable>, Identified {
            /* this is merely a unionizing interface that does not declare methods */
        }

        /**
         * This interface is used to allow for optionally providing several
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer} to applied when a matcher identifies a type
         * to be instrumented. Any subsequent transformers are applied in the order they are registered.
         */
        interface Extendable extends AgentBuilder, Identified {

            /**
             * <p>
             * Applies the specified transformation as a decorative transformation. For a decorative transformation, the supplied
             * transformer is prepended to any previous transformation that also matches the instrumented type, i.e. both transformations
             * are supplied. This procedure is repeated until a transformer is reached that matches the instrumented type but is not
             * defined as decorating after which no further transformations are considered. If all matching transformations are declared
             * as decorating, all matching transformers are applied.
             * </p>
             * <p>
             * <b>Note</b>: A decorating transformer is applied <b>after</b> previously registered transformers.
             * </p>
             *
             * @return A new instance of this agent builder with the specified transformation being applied as a decorator.
             */
            AgentBuilder asDecorator();
        }
    }

    /**
     * A matcher that allows to determine if a {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}
     * should be applied during the execution of a {@link java.lang.instrument.ClassFileTransformer} that was
     * generated by an {@link net.bytebuddy.agent.builder.AgentBuilder}.
     */
    interface RawMatcher {

        /**
         * Decides if the given {@code typeDescription} should be instrumented with the entailed
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s.
         *
         * @param typeDescription     A description of the type to be instrumented.
         * @param classLoader         The class loader of the instrumented type. Might be {@code null} if this class
         *                            loader represents the bootstrap class loader.
         * @param module              The transformed type's module or {@code null} if the current VM does not support modules.
         * @param classBeingRedefined The class being redefined which is only not {@code null} if a retransformation
         *                            is applied.
         * @param protectionDomain    The protection domain of the type being transformed.
         * @return {@code true} if the entailed {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should
         * be applied for the given {@code typeDescription}.
         */
        boolean matches(TypeDescription typeDescription,
                        ClassLoader classLoader,
                        JavaModule module,
                        Class<?> classBeingRedefined,
                        ProtectionDomain protectionDomain);

        /**
         * A conjunction of two raw matchers.
         */
        class Conjunction implements RawMatcher {

            /**
             * The left matcher which is applied first.
             */
            private final RawMatcher left;

            /**
             * The right matcher which is applied second.
             */
            private final RawMatcher right;

            /**
             * Creates a new conjunction of two raw matchers.
             *
             * @param left  The left matcher which is applied first.
             * @param right The right matcher which is applied second.
             */
            protected Conjunction(RawMatcher left, RawMatcher right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public boolean matches(TypeDescription typeDescription,
                                   ClassLoader classLoader,
                                   JavaModule module,
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain) {
                return left.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)
                        && right.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Conjunction that = (Conjunction) object;
                return left.equals(that.left) && right.equals(that.right);
            }

            @Override
            public int hashCode() {
                int result = left.hashCode();
                result = 31 * result + right.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.RawMatcher.Conjunction{" +
                        "left=" + left +
                        ", right=" + right +
                        '}';
            }
        }

        /**
         * A disjunction of two raw matchers.
         */
        class Disjunction implements RawMatcher {

            /**
             * The left matcher which is applied first.
             */
            private final RawMatcher left;

            /**
             * The right matcher which is applied second.
             */
            private final RawMatcher right;

            /**
             * Creates a new disjunction of two raw matchers.
             *
             * @param left  The left matcher which is applied first.
             * @param right The right matcher which is applied second.
             */
            protected Disjunction(RawMatcher left, RawMatcher right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public boolean matches(TypeDescription typeDescription,
                                   ClassLoader classLoader,
                                   JavaModule module,
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain) {
                return left.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)
                        || right.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Disjunction that = (Disjunction) object;
                return left.equals(that.left) && right.equals(that.right);
            }

            @Override
            public int hashCode() {
                int result = left.hashCode();
                result = 31 * result + right.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.RawMatcher.Disjunction{" +
                        "left=" + left +
                        ", right=" + right +
                        '}';
            }
        }

        /**
         * A raw matcher implementation that checks a {@link TypeDescription}
         * and its {@link java.lang.ClassLoader} against two suitable matchers in order to determine if the matched
         * type should be instrumented.
         */
        class ForElementMatchers implements RawMatcher {

            /**
             * The type matcher to apply to a {@link TypeDescription}.
             */
            private final ElementMatcher<? super TypeDescription> typeMatcher;

            /**
             * The class loader matcher to apply to a {@link java.lang.ClassLoader}.
             */
            private final ElementMatcher<? super ClassLoader> classLoaderMatcher;

            /**
             * A module matcher to apply to a {@code java.lang.reflect.Module}.
             */
            private final ElementMatcher<? super JavaModule> moduleMatcher;

            /**
             * Creates a new {@link net.bytebuddy.agent.builder.AgentBuilder.RawMatcher} that only matches the
             * supplied {@link TypeDescription} and its {@link java.lang.ClassLoader} against two matcher in order
             * to decided if an instrumentation should be conducted.
             *
             * @param typeMatcher        The type matcher to apply to a {@link TypeDescription}.
             * @param classLoaderMatcher The class loader matcher to apply to a {@link java.lang.ClassLoader}.
             * @param moduleMatcher      A module matcher to apply to a {@code java.lang.reflect.Module}.
             */
            public ForElementMatchers(ElementMatcher<? super TypeDescription> typeMatcher,
                                      ElementMatcher<? super ClassLoader> classLoaderMatcher,
                                      ElementMatcher<? super JavaModule> moduleMatcher) {
                this.typeMatcher = typeMatcher;
                this.classLoaderMatcher = classLoaderMatcher;
                this.moduleMatcher = moduleMatcher;
            }

            @Override
            public boolean matches(TypeDescription typeDescription,
                                   ClassLoader classLoader,
                                   JavaModule module,
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain) {
                return moduleMatcher.matches(module) && classLoaderMatcher.matches(classLoader) && typeMatcher.matches(typeDescription);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && classLoaderMatcher.equals(((ForElementMatchers) other).classLoaderMatcher)
                        && moduleMatcher.equals(((ForElementMatchers) other).moduleMatcher)
                        && typeMatcher.equals(((ForElementMatchers) other).typeMatcher);
            }

            @Override
            public int hashCode() {
                int result = typeMatcher.hashCode();
                result = 31 * result + classLoaderMatcher.hashCode();
                result = 31 * result + moduleMatcher.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.RawMatcher.ForElementMatchers{" +
                        "typeMatcher=" + typeMatcher +
                        ", classLoaderMatcher=" + classLoaderMatcher +
                        ", moduleMatcher=" + moduleMatcher +
                        '}';
            }
        }
    }

    /**
     * A listener that is informed about events that occur during an instrumentation process.
     */
    interface Listener {

        /**
         * Invoked right before a successful transformation is applied.
         *
         * @param typeDescription The type that is being transformed.
         * @param classLoader     The class loader which is loading this type.
         * @param module          The transformed type's module or {@code null} if the current VM does not support modules.
         * @param dynamicType     The dynamic type that was created.
         */
        void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType);

        /**
         * Invoked when a type is not transformed but ignored.
         *
         * @param typeDescription The type being ignored for transformation.
         * @param classLoader     The class loader which is loading this type.
         * @param module          The ignored type's module or {@code null} if the current VM does not support modules.
         */
        void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module);

        /**
         * Invoked when an error has occurred during transformation.
         *
         * @param typeName    The type name of the instrumented type.
         * @param classLoader The class loader which is loading this type.
         * @param module      The instrumented type's module or {@code null} if the current VM does not support modules.
         * @param throwable   The occurred error.
         */
        void onError(String typeName, ClassLoader classLoader, JavaModule module, Throwable throwable);

        /**
         * Invoked after a class was attempted to be loaded, independently of its treatment.
         *
         * @param typeName    The binary name of the instrumented type.
         * @param classLoader The class loader which is loading this type.
         * @param module      The instrumented type's module or {@code null} if the current VM does not support modules.
         */
        void onComplete(String typeName, ClassLoader classLoader, JavaModule module);

        /**
         * A no-op implementation of a {@link net.bytebuddy.agent.builder.AgentBuilder.Listener}.
         */
        enum NoOp implements Listener {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
                /* do nothing */
            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                /* do nothing */
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, Throwable throwable) {
                /* do nothing */
            }

            @Override
            public void onComplete(String typeName, ClassLoader classLoader, JavaModule module) {
                /* do nothing */
            }

            @Override
            public String toString() {
                return "AgentBuilder.Listener.NoOp." + name();
            }
        }

        /**
         * An adapter for a listener wher all methods are implemented as non-operational.
         */
        abstract class Adapter implements Listener {

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
                /* do nothing */
            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                /* do nothing */
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, Throwable throwable) {
                /* do nothing */
            }

            @Override
            public void onComplete(String typeName, ClassLoader classLoader, JavaModule module) {
                /* do nothing */
            }
        }

        /**
         * A listener that writes events to a {@link PrintStream}. This listener prints a line per event, including the event type and
         * the name of the type in question.
         */
        class StreamWriting implements Listener {

            /**
             * The prefix that is appended to all written messages.
             */
            protected static final String PREFIX = "[Byte Buddy]";

            /**
             * The print stream written to.
             */
            private final PrintStream printStream;

            /**
             * Creates a new stream writing listener.
             *
             * @param printStream The print stream written to.
             */
            public StreamWriting(PrintStream printStream) {
                this.printStream = printStream;
            }

            /**
             * Creates a new stream writing listener that writes to {@link System#out}.
             *
             * @return A listener writing events to the standard output stream.
             */
            public static Listener toSystemOut() {
                return new StreamWriting(System.out);
            }

            /**
             * Creates a new stream writing listener that writes to {@link System#err}.
             *
             * @return A listener writing events to the standad error stream.
             */
            public static Listener toSystemError() {
                return new StreamWriting(System.err);
            }

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
                printStream.println(PREFIX + " TRANSFORM " + typeDescription.getName() + "[" + classLoader + ", " + module + "]");
            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                printStream.println(PREFIX + " IGNORE " + typeDescription.getName() + "[" + classLoader + ", " + module + "]");
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, Throwable throwable) {
                synchronized (printStream) {
                    printStream.println(PREFIX + " ERROR " + typeName + "[" + classLoader + ", " + module + "]");
                    throwable.printStackTrace(printStream);
                }
            }

            @Override
            public void onComplete(String typeName, ClassLoader classLoader, JavaModule module) {
                printStream.println(PREFIX + " COMPLETE " + typeName + "[" + classLoader + ", " + module + "]");
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && printStream.equals(((StreamWriting) other).printStream);
            }

            @Override
            public int hashCode() {
                return printStream.hashCode();
            }

            @Override
            public String toString() {
                return "AgentBuilder.Listener.StreamWriting{" +
                        "printStream=" + printStream +
                        '}';
            }
        }

        /**
         * A listener that adds read-edges to any module of an instrumented class upon its transformation.
         */
        class ModuleReadEdgeCompleting extends Listener.Adapter {

            /**
             * The instrumentation instance used for adding read edges.
             */
            private final Instrumentation instrumentation;

            /**
             * {@code true} if the listener should also add a read-edge from the supplied modules to the instrumented type's module.
             */
            private final boolean addTargetEdge;

            /**
             * The modules to add as a read edge to any transformed class's module.
             */
            private final Set<? extends JavaModule> modules;

            /**
             * Creates a new module read-edge completing listener.
             *
             * @param instrumentation The instrumentation instance used for adding read edges.
             * @param addTargetEdge   {@code true} if the listener should also add a read-edge from the supplied modules
             *                        to the instrumented type's module.
             * @param modules         The modules to add as a read edge to any transformed class's module.
             */
            public ModuleReadEdgeCompleting(Instrumentation instrumentation, boolean addTargetEdge, Set<? extends JavaModule> modules) {
                this.instrumentation = instrumentation;
                this.addTargetEdge = addTargetEdge;
                this.modules = modules;
            }

            /**
             * Resolves a listener that adds module edges from and to the instrumented type's module.
             *
             * @param instrumentation The instrumentation instance used for adding read edges.
             * @param addTargetEdge   {@code true} if the listener should also add a read-edge from the supplied
             *                        modules to the instrumented type's module.
             * @param type            The types for which to extract the modules.
             * @return An appropriate listener.
             */
            protected static Listener of(Instrumentation instrumentation, boolean addTargetEdge, Class<?>... type) {
                Set<JavaModule> modules = new HashSet<JavaModule>();
                for (Class<?> aType : type) {
                    JavaModule module = JavaModule.ofType(aType);
                    if (module.isNamed()) {
                        modules.add(module);
                    }
                }
                return modules.isEmpty()
                        ? Listener.NoOp.INSTANCE
                        : new Listener.ModuleReadEdgeCompleting(instrumentation, addTargetEdge, modules);
            }

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
                if (module != null && module.isNamed()) {
                    for (JavaModule target : modules) {
                        if (!module.canRead(target)) {
                            module.addReads(instrumentation, target);
                        }
                        if (addTargetEdge && !target.canRead(module)) {
                            target.addReads(instrumentation, module);
                        }
                    }
                }
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ModuleReadEdgeCompleting that = (ModuleReadEdgeCompleting) object;
                return instrumentation.equals(that.instrumentation)
                        && addTargetEdge == that.addTargetEdge
                        && modules.equals(that.modules);
            }

            @Override
            public int hashCode() {
                int result = instrumentation.hashCode();
                result = 31 * result + modules.hashCode();
                result = 31 * result + (addTargetEdge ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Listener.ModuleReadEdgeCompleting{" +
                        "instrumentation=" + instrumentation +
                        ", addTargetEdge=" + addTargetEdge +
                        ", modules=" + modules +
                        '}';
            }
        }

        /**
         * A compound listener that allows to group several listeners in one instance.
         */
        class Compound implements Listener {

            /**
             * The listeners that are represented by this compound listener in their application order.
             */
            private final List<? extends Listener> listeners;

            /**
             * Creates a new compound listener.
             *
             * @param listener The listeners to apply in their application order.
             */
            public Compound(Listener... listener) {
                this(Arrays.asList(listener));
            }

            /**
             * Creates a new compound listener.
             *
             * @param listeners The listeners to apply in their application order.
             */
            public Compound(List<? extends Listener> listeners) {
                this.listeners = listeners;
            }

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
                for (Listener listener : listeners) {
                    listener.onTransformation(typeDescription, classLoader, module, dynamicType);
                }
            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                for (Listener listener : listeners) {
                    listener.onIgnored(typeDescription, classLoader, module);
                }
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, Throwable throwable) {
                for (Listener listener : listeners) {
                    listener.onError(typeName, classLoader, module, throwable);
                }
            }

            @Override
            public void onComplete(String typeName, ClassLoader classLoader, JavaModule module) {
                for (Listener listener : listeners) {
                    listener.onComplete(typeName, classLoader, module);
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && listeners.equals(((Compound) other).listeners);
            }

            @Override
            public int hashCode() {
                return listeners.hashCode();
            }

            @Override
            public String toString() {
                return "AgentBuilder.Listener.Compound{" +
                        "listeners=" + listeners +
                        '}';
            }
        }
    }

    /**
     * A type strategy is responsible for creating a type builder for a type that is being instrumented.
     */
    interface TypeStrategy {

        /**
         * Creates a type builder for a given type.
         *
         * @param typeDescription       The type being instrumented.
         * @param byteBuddy             The Byte Buddy configuration.
         * @param classFileLocator      The class file locator to use.
         * @param methodNameTransformer The method name transformer to use.
         * @return A type builder for the given arguments.
         */
        DynamicType.Builder<?> builder(TypeDescription typeDescription,
                                       ByteBuddy byteBuddy,
                                       ClassFileLocator classFileLocator,
                                       MethodNameTransformer methodNameTransformer);

        /**
         * Default implementations of type strategies.
         */
        enum Default implements TypeStrategy {

            /**
             * A definition handler that performs a rebasing for all types.
             */
            REBASE {
                @Override
                public DynamicType.Builder<?> builder(TypeDescription typeDescription,
                                                      ByteBuddy byteBuddy,
                                                      ClassFileLocator classFileLocator,
                                                      MethodNameTransformer methodNameTransformer) {
                    return byteBuddy.rebase(typeDescription, classFileLocator, methodNameTransformer);
                }
            },

            /**
             * <p>
             * A definition handler that performs a redefinition for all types.
             * </p>
             * <p>
             * Note that the default agent builder is configured to apply a self initialization where a static class initializer
             * is added to the redefined class. This can be disabled by for example using a {@link InitializationStrategy.Minimal} or
             * {@link InitializationStrategy.NoOp}. Also, consider the constraints implied by {@link ByteBuddy#redefine(TypeDescription, ClassFileLocator)}.
             * </p>
             * <p>
             * For prohibiting any changes on a class file, use {@link AgentBuilder#disableClassFormatChanges()}
             * </p>
             */
            REDEFINE {
                @Override
                public DynamicType.Builder<?> builder(TypeDescription typeDescription,
                                                      ByteBuddy byteBuddy,
                                                      ClassFileLocator classFileLocator,
                                                      MethodNameTransformer methodNameTransformer) {
                    return byteBuddy.redefine(typeDescription, classFileLocator);
                }
            },

            /**
             * <p>
             * A definition handler that performs a redefinition for all types and ignores all methods that were not declared by the instrumented type.
             * </p>
             * <p>
             * Note that the default agent builder is configured to apply a self initialization where a static class initializer
             * is added to the redefined class. This can be disabled by for example using a {@link InitializationStrategy.Minimal} or
             * {@link InitializationStrategy.NoOp}. Also, consider the constraints implied by {@link ByteBuddy#redefine(TypeDescription, ClassFileLocator)}.
             * </p>
             * <p>
             * For prohibiting any changes on a class file, use {@link AgentBuilder#disableClassFormatChanges()}
             * </p>
             */
            REDEFINE_DECLARED_ONLY {
                @Override
                public DynamicType.Builder<?> builder(TypeDescription typeDescription,
                                                      ByteBuddy byteBuddy,
                                                      ClassFileLocator classFileLocator,
                                                      MethodNameTransformer methodNameTransformer) {
                    return byteBuddy.redefine(typeDescription, classFileLocator).ignoreAlso(not(isDeclaredBy(typeDescription)));
                }
            };

            @Override
            public String toString() {
                return "AgentBuilder.TypeStrategy.Default." + name();
            }
        }
    }

    /**
     * A transformer allows to apply modifications to a {@link net.bytebuddy.dynamic.DynamicType}. Such a modification
     * is then applied to any instrumented type that was matched by the preceding matcher.
     */
    interface Transformer {

        /**
         * Allows for a transformation of a {@link net.bytebuddy.dynamic.DynamicType.Builder}.
         *
         * @param builder         The dynamic builder to transform.
         * @param typeDescription The description of the type currently being instrumented.
         * @param classLoader     The class loader of the instrumented class. Might be {@code null} to
         *                        represent the bootstrap class loader.
         * @return A transformed version of the supplied {@code builder}.
         */
        DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader);

        /**
         * A no-op implementation of a {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer} that does
         * not modify the supplied dynamic type.
         */
        enum NoOp implements Transformer {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                return builder;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Transformer.NoOp." + name();
            }
        }

        /**
         * A compound transformer that allows to group several
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s as a single transformer.
         */
        class Compound implements Transformer {

            /**
             * The transformers to apply in their application order.
             */
            private final Transformer[] transformer;

            /**
             * Creates a new compound transformer.
             *
             * @param transformer The transformers to apply in their application order.
             */
            public Compound(Transformer... transformer) {
                this.transformer = transformer;
            }

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                for (Transformer transformer : this.transformer) {
                    builder = transformer.transform(builder, typeDescription, classLoader);
                }
                return builder;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Arrays.equals(transformer, ((Compound) other).transformer);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(transformer);
            }

            @Override
            public String toString() {
                return "AgentBuilder.Transformer.Compound{" +
                        "transformer=" + Arrays.toString(transformer) +
                        '}';
            }
        }
    }

    /**
     * A type locator allows to specify how {@link TypeDescription}s are resolved by an {@link net.bytebuddy.agent.builder.AgentBuilder}.
     */
    interface PoolStrategy {

        /**
         * Creates a type pool for a given class file locator.
         *
         * @param classFileLocator The class file locator to use.
         * @param classLoader      The class loader for which the class file locator was created.
         * @return A type pool for the supplied class file locator.
         */
        TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader);

        /**
         * <p>
         * A default type locator that resolves types only if any property that is not the type's name is requested.
         * </p>
         * <p>
         * The returned type pool uses a {@link net.bytebuddy.pool.TypePool.CacheProvider.Simple} and the
         * {@link ClassFileLocator} that is provided by the builder's {@link LocationStrategy}.
         * </p>
         */
        enum Default implements PoolStrategy {

            /**
             * A type locator that parses the code segment of each method for extracting information about parameter
             * names even if they are not explicitly included in a class file.
             *
             * @see net.bytebuddy.pool.TypePool.Default.ReaderMode#EXTENDED
             */
            EXTENDED(TypePool.Default.ReaderMode.EXTENDED),

            /**
             * A type locator that skips the code segment of each method and does therefore not extract information
             * about parameter names. Parameter names are still included if they are explicitly included in a class file.
             *
             * @see net.bytebuddy.pool.TypePool.Default.ReaderMode#FAST
             */
            FAST(TypePool.Default.ReaderMode.FAST);

            /**
             * The reader mode to apply by this type locator.
             */
            private final TypePool.Default.ReaderMode readerMode;

            /**
             * Creates a new type locator.
             *
             * @param readerMode The reader mode to apply by this type locator.
             */
            Default(TypePool.Default.ReaderMode readerMode) {
                this.readerMode = readerMode;
            }

            @Override
            public TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader) {
                return new TypePool.Default.WithLazyResolution(TypePool.CacheProvider.Simple.withObjectType(), classFileLocator, readerMode);
            }

            @Override
            public String toString() {
                return "AgentBuilder.PoolStrategy.Default." + name();
            }
        }

        /**
         * <p>
         * A type locator that resolves all type descriptions eagerly.
         * </p>
         * <p>
         * The returned type pool uses a {@link net.bytebuddy.pool.TypePool.CacheProvider.Simple} and the
         * {@link ClassFileLocator} that is provided by the builder's {@link LocationStrategy}.
         * </p>
         */
        enum Eager implements PoolStrategy {

            /**
             * A type locator that parses the code segment of each method for extracting information about parameter
             * names even if they are not explicitly included in a class file.
             *
             * @see net.bytebuddy.pool.TypePool.Default.ReaderMode#EXTENDED
             */
            EXTENDED(TypePool.Default.ReaderMode.EXTENDED),

            /**
             * A type locator that skips the code segment of each method and does therefore not extract information
             * about parameter names. Parameter names are still included if they are explicitly included in a class file.
             *
             * @see net.bytebuddy.pool.TypePool.Default.ReaderMode#FAST
             */
            FAST(TypePool.Default.ReaderMode.FAST);

            /**
             * The reader mode to apply by this type locator.
             */
            private final TypePool.Default.ReaderMode readerMode;

            /**
             * Creates a new type locator.
             *
             * @param readerMode The reader mode to apply by this type locator.
             */
            Eager(TypePool.Default.ReaderMode readerMode) {
                this.readerMode = readerMode;
            }

            @Override
            public TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader) {
                return new TypePool.Default(TypePool.CacheProvider.Simple.withObjectType(), classFileLocator, readerMode);
            }

            @Override
            public String toString() {
                return "AgentBuilder.PoolStrategy.Eager." + name();
            }
        }

        /**
         * <p>
         * A type locator that attempts loading a type if it cannot be located by the underlying lazy type pool.
         * </p>
         * <p>
         * The returned type pool uses a {@link net.bytebuddy.pool.TypePool.CacheProvider.Simple} and the
         * {@link ClassFileLocator} that is provided by the builder's {@link LocationStrategy}. Any types
         * are loaded via the instrumented type's {@link ClassLoader}.
         * </p>
         */
        enum ClassLoading implements PoolStrategy {

            /**
             * A type locator that parses the code segment of each method for extracting information about parameter
             * names even if they are not explicitly included in a class file.
             *
             * @see net.bytebuddy.pool.TypePool.Default.ReaderMode#EXTENDED
             */
            EXTENDED(TypePool.Default.ReaderMode.EXTENDED),

            /**
             * A type locator that skips the code segment of each method and does therefore not extract information
             * about parameter names. Parameter names are still included if they are explicitly included in a class file.
             *
             * @see net.bytebuddy.pool.TypePool.Default.ReaderMode#FAST
             */
            FAST(TypePool.Default.ReaderMode.FAST);

            /**
             * The reader mode to apply by this type locator.
             */
            private final TypePool.Default.ReaderMode readerMode;

            /**
             * Creates a new type locator.
             *
             * @param readerMode The reader mode to apply by this type locator.
             */
            ClassLoading(TypePool.Default.ReaderMode readerMode) {
                this.readerMode = readerMode;
            }

            @Override
            public TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader) {
                return TypePool.ClassLoading.of(classLoader, new TypePool.Default.WithLazyResolution(TypePool.CacheProvider.Simple.withObjectType(), classFileLocator, readerMode));
            }

            @Override
            public String toString() {
                return "AgentBuilder.PoolStrategy.ClassLoading." + name();
            }
        }

        /**
         * <p>
         * A type locator that uses type pools but allows for the configuration of a custom cache provider by class loader. Note that a
         * {@link TypePool} can grow in size and that a static reference is kept to this pool by Byte Buddy's registration of a
         * {@link ClassFileTransformer} what can cause a memory leak if the supplied caches are not cleared on a regular basis. Also note
         * that a cache provider can be accessed concurrently by multiple {@link ClassLoader}s.
         * </p>
         * <p>
         * All types that are returned by the locator's type pool are resolved lazily.
         * </p>
         */
        abstract class WithTypePoolCache implements PoolStrategy {

            /**
             * The reader mode to use for parsing a class file.
             */
            protected final TypePool.Default.ReaderMode readerMode;

            /**
             * Creates a new type locator that creates {@link TypePool}s but provides a custom {@link net.bytebuddy.pool.TypePool.CacheProvider}.
             *
             * @param readerMode The reader mode to use for parsing a class file.
             */
            protected WithTypePoolCache(TypePool.Default.ReaderMode readerMode) {
                this.readerMode = readerMode;
            }

            @Override
            public TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader) {
                return new TypePool.Default.WithLazyResolution(locate(classLoader), classFileLocator, readerMode);
            }

            /**
             * Locates a cache provider for a given class loader.
             *
             * @param classLoader The class loader for which to locate a cache. This class loader might be {@code null} to represent the bootstrap loader.
             * @return The cache provider to use.
             */
            protected abstract TypePool.CacheProvider locate(ClassLoader classLoader);

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                WithTypePoolCache that = (WithTypePoolCache) object;
                return readerMode == that.readerMode;
            }

            @Override
            public int hashCode() {
                return readerMode.hashCode();
            }

            /**
             * An implementation of a type locator {@link WithTypePoolCache} (note documentation of the linked class) that is based on a
             * {@link ConcurrentMap}. It is the responsibility of the type locator's user to avoid the type locator from leaking memory.
             */
            public static class Simple extends WithTypePoolCache {

                /**
                 * The concurrent map that is used for storing a cache provider per class loader.
                 */
                private final ConcurrentMap<? super ClassLoader, TypePool.CacheProvider> cacheProviders;

                /**
                 * Creates a new type locator that caches a cache provider per class loader in a concurrent map. The type
                 * locator uses a fast {@link net.bytebuddy.pool.TypePool.Default.ReaderMode}.
                 *
                 * @param cacheProviders The concurrent map that is used for storing a cache provider per class loader.
                 */
                public Simple(ConcurrentMap<? super ClassLoader, TypePool.CacheProvider> cacheProviders) {
                    this(TypePool.Default.ReaderMode.FAST, cacheProviders);
                }

                /**
                 * Creates a new type locator that caches a cache provider per class loader in a concurrent map.
                 *
                 * @param readerMode     The reader mode to use for parsing a class file.
                 * @param cacheProviders The concurrent map that is used for storing a cache provider per class loader.
                 */
                public Simple(TypePool.Default.ReaderMode readerMode, ConcurrentMap<? super ClassLoader, TypePool.CacheProvider> cacheProviders) {
                    super(readerMode);
                    this.cacheProviders = cacheProviders;
                }

                @Override
                protected TypePool.CacheProvider locate(ClassLoader classLoader) {
                    classLoader = classLoader == null ? getBootstrapMarkerLoader() : classLoader;
                    TypePool.CacheProvider cacheProvider = cacheProviders.get(classLoader);
                    while (cacheProvider == null) {
                        cacheProvider = TypePool.CacheProvider.Simple.withObjectType();
                        TypePool.CacheProvider previous = cacheProviders.putIfAbsent(classLoader, cacheProvider);
                        if (previous != null) {
                            cacheProvider = previous;
                        }
                    }
                    return cacheProvider;
                }

                /**
                 * <p>
                 * Returns the class loader to serve as a cache key if a cache provider for the bootstrap class loader is requested.
                 * This class loader is represented by {@code null} in the JVM which is an invalid value for many {@link ConcurrentMap}
                 * implementations.
                 * </p>
                 * <p>
                 * By default, {@link ClassLoader#getSystemClassLoader()} is used as such a key as any resource location for the
                 * bootstrap class loader is performed via the system class loader within Byte Buddy as {@code null} cannot be queried
                 * for resources via method calls such that this does not make a difference.
                 * </p>
                 *
                 * @return A class loader to represent the bootstrap class loader.
                 */
                protected ClassLoader getBootstrapMarkerLoader() {
                    return ClassLoader.getSystemClassLoader();
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    if (!super.equals(object)) return false;
                    Simple simple = (Simple) object;
                    return cacheProviders.equals(simple.cacheProviders);
                }

                @Override
                public int hashCode() {
                    int result = super.hashCode();
                    result = 31 * result + cacheProviders.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.PoolStrategy.WithTypePoolCache.Simple{" +
                            "cacheProviders=" + cacheProviders +
                            '}';
                }
            }
        }
    }

    /**
     * An initialization strategy which determines the handling of {@link net.bytebuddy.implementation.LoadedTypeInitializer}s
     * and the loading of auxiliary types. The agent builder does not reuse the {@link TypeResolutionStrategy} as Javaagents cannot access
     * a loaded class after a transformation such that different initialization strategies become meaningful.
     */
    interface InitializationStrategy {

        /**
         * Creates a new dispatcher for injecting this initialization strategy during a transformation process.
         *
         * @return The dispatcher to be used.
         */
        Dispatcher dispatcher();

        /**
         * A dispatcher for changing a class file to adapt a self-initialization strategy.
         */
        interface Dispatcher {

            /**
             * Transforms the instrumented type to implement an appropriate initialization strategy.
             *
             * @param builder The builder which should implement the initialization strategy.
             * @return The given {@code builder} with the initialization strategy applied.
             */
            DynamicType.Builder<?> apply(DynamicType.Builder<?> builder);

            /**
             * Registers a dynamic type for initialization and/or begins the initialization process.
             *
             * @param dynamicType     The dynamic type that is created.
             * @param classLoader     The class loader of the dynamic type.
             * @param injectorFactory The injector factory
             */
            void register(DynamicType dynamicType, ClassLoader classLoader, InjectorFactory injectorFactory);

            /**
             * A factory for creating a {@link ClassInjector} only if it is required.
             */
            interface InjectorFactory {

                /**
                 * Resolves the class injector for this factory.
                 *
                 * @return The class injector for this factory.
                 */
                ClassInjector resolve();
            }
        }

        /**
         * A non-initializing initialization strategy.
         */
        enum NoOp implements InitializationStrategy, Dispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Dispatcher dispatcher() {
                return this;
            }

            @Override
            public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                return builder;
            }

            @Override
            public void register(DynamicType dynamicType, ClassLoader classLoader, InjectorFactory injectorFactory) {
                /* do nothing */
            }

            @Override
            public String toString() {
                return "AgentBuilder.InitializationStrategy.NoOp." + name();
            }
        }

        /**
         * An initialization strategy that adds a code block to an instrumented type's type initializer which
         * then calls a specific class that is responsible for the explicit initialization.
         */
        @SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE", justification = "Avoiding synchronization without security concerns")
        enum SelfInjection implements InitializationStrategy {

            /**
             * A form of self-injection where auxiliary types that are annotated by
             * {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType.SignatureRelevant} of the instrumented type are loaded lazily and
             * any other auxiliary type is loaded eagerly.
             */
            SPLIT {
                @Override
                public InitializationStrategy.Dispatcher dispatcher() {
                    return new SelfInjection.Dispatcher.Split(new Random().nextInt());
                }
            },

            /**
             * A form of self-injection where any auxiliary type is loaded lazily.
             */
            LAZY {
                @Override
                public InitializationStrategy.Dispatcher dispatcher() {
                    return new SelfInjection.Dispatcher.Lazy(new Random().nextInt());
                }
            },

            /**
             * A form of self-injection where any auxiliary type is loaded eagerly.
             */
            EAGER {
                @Override
                public InitializationStrategy.Dispatcher dispatcher() {
                    return new SelfInjection.Dispatcher.Eager(new Random().nextInt());
                }
            };

            @Override
            public String toString() {
                return "AgentBuilder.InitializationStrategy.SelfInjection." + name();
            }

            /**
             * A dispatcher for a self-initialization strategy.
             */
            protected abstract static class Dispatcher implements InitializationStrategy.Dispatcher {

                /**
                 * A random identification for the applied self-initialization.
                 */
                protected final int identification;

                /**
                 * Creates a new dispatcher.
                 *
                 * @param identification A random identification for the applied self-initialization.
                 */
                protected Dispatcher(int identification) {
                    this.identification = identification;
                }

                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                    return builder.initializer(new NexusAccessor.InitializationAppender(identification));
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && identification == ((Dispatcher) other).identification;
                }

                @Override
                public int hashCode() {
                    return identification;
                }

                /**
                 * A dispatcher for the {@link net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy.SelfInjection#SPLIT} strategy.
                 */
                protected static class Split extends Dispatcher {

                    /**
                     * Creates a new split dispatcher.
                     *
                     * @param identification A random identification for the applied self-initialization.
                     */
                    protected Split(int identification) {
                        super(identification);
                    }

                    @Override
                    public void register(DynamicType dynamicType, ClassLoader classLoader, InjectorFactory injectorFactory) {
                        Map<TypeDescription, byte[]> auxiliaryTypes = dynamicType.getAuxiliaryTypes();
                        LoadedTypeInitializer loadedTypeInitializer;
                        if (!auxiliaryTypes.isEmpty()) {
                            TypeDescription instrumentedType = dynamicType.getTypeDescription();
                            ClassInjector classInjector = injectorFactory.resolve();
                            Map<TypeDescription, byte[]> independentTypes = new LinkedHashMap<TypeDescription, byte[]>(auxiliaryTypes);
                            Map<TypeDescription, byte[]> dependentTypes = new LinkedHashMap<TypeDescription, byte[]>(auxiliaryTypes);
                            for (TypeDescription auxiliaryType : auxiliaryTypes.keySet()) {
                                (auxiliaryType.getDeclaredAnnotations().isAnnotationPresent(AuxiliaryType.SignatureRelevant.class)
                                        ? dependentTypes
                                        : independentTypes).remove(auxiliaryType);
                            }
                            Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = dynamicType.getLoadedTypeInitializers();
                            if (!independentTypes.isEmpty()) {
                                for (Map.Entry<TypeDescription, Class<?>> entry : classInjector.inject(independentTypes).entrySet()) {
                                    loadedTypeInitializers.get(entry.getKey()).onLoad(entry.getValue());
                                }
                            }
                            Map<TypeDescription, LoadedTypeInitializer> lazyInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>(loadedTypeInitializers);
                            loadedTypeInitializers.keySet().removeAll(independentTypes.keySet());
                            loadedTypeInitializer = lazyInitializers.size() > 1 // there exist auxiliary types that need lazy loading
                                    ? new InjectingInitializer(instrumentedType, dependentTypes, lazyInitializers, classInjector)
                                    : lazyInitializers.get(instrumentedType);
                        } else {
                            loadedTypeInitializer = dynamicType.getLoadedTypeInitializers().get(dynamicType.getTypeDescription());
                        }
                        NexusAccessor.INSTANCE.register(dynamicType.getTypeDescription().getName(), classLoader, identification, loadedTypeInitializer);
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.InitializationStrategy.SelfInjection.Dispatcher.Split{identification=" + identification + "}";
                    }
                }

                /**
                 * A dispatcher for the {@link net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy.SelfInjection#LAZY} strategy.
                 */
                protected static class Lazy extends Dispatcher {

                    /**
                     * Creates a new lazy dispatcher.
                     *
                     * @param identification A random identification for the applied self-initialization.
                     */
                    protected Lazy(int identification) {
                        super(identification);
                    }

                    @Override
                    public void register(DynamicType dynamicType, ClassLoader classLoader, InjectorFactory injectorFactory) {
                        Map<TypeDescription, byte[]> auxiliaryTypes = dynamicType.getAuxiliaryTypes();
                        LoadedTypeInitializer loadedTypeInitializer = auxiliaryTypes.isEmpty()
                                ? dynamicType.getLoadedTypeInitializers().get(dynamicType.getTypeDescription())
                                : new InjectingInitializer(dynamicType.getTypeDescription(), auxiliaryTypes, dynamicType.getLoadedTypeInitializers(), injectorFactory.resolve());
                        NexusAccessor.INSTANCE.register(dynamicType.getTypeDescription().getName(), classLoader, identification, loadedTypeInitializer);
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.InitializationStrategy.SelfInjection.Dispatcher.Lazy{identification=" + identification + "}";
                    }
                }

                /**
                 * A dispatcher for the {@link net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy.SelfInjection#EAGER} strategy.
                 */
                protected static class Eager extends Dispatcher {

                    /**
                     * Creates a new eager dispatcher.
                     *
                     * @param identification A random identification for the applied self-initialization.
                     */
                    protected Eager(int identification) {
                        super(identification);
                    }

                    @Override
                    public void register(DynamicType dynamicType, ClassLoader classLoader, InjectorFactory injectorFactory) {
                        Map<TypeDescription, byte[]> auxiliaryTypes = dynamicType.getAuxiliaryTypes();
                        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = dynamicType.getLoadedTypeInitializers();
                        if (!auxiliaryTypes.isEmpty()) {
                            for (Map.Entry<TypeDescription, Class<?>> entry : injectorFactory.resolve().inject(auxiliaryTypes).entrySet()) {
                                loadedTypeInitializers.get(entry.getKey()).onLoad(entry.getValue());
                            }
                        }
                        LoadedTypeInitializer loadedTypeInitializer = loadedTypeInitializers.get(dynamicType.getTypeDescription());
                        NexusAccessor.INSTANCE.register(dynamicType.getTypeDescription().getName(), classLoader, identification, loadedTypeInitializer);
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.InitializationStrategy.SelfInjection.Dispatcher.Eager{identification=" + identification + "}";
                    }
                }

                /**
                 * A type initializer that injects all auxiliary types of the instrumented type.
                 */
                protected static class InjectingInitializer implements LoadedTypeInitializer {

                    /**
                     * The instrumented type.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * The auxiliary types mapped to their class file representation.
                     */
                    private final Map<TypeDescription, byte[]> rawAuxiliaryTypes;

                    /**
                     * The instrumented types and auxiliary types mapped to their loaded type initializers.
                     * The instrumented types and auxiliary types mapped to their loaded type initializers.
                     */
                    private final Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers;

                    /**
                     * The class injector to use.
                     */
                    private final ClassInjector classInjector;

                    /**
                     * Creates a new injection initializer.
                     *
                     * @param instrumentedType       The instrumented type.
                     * @param rawAuxiliaryTypes      The auxiliary types mapped to their class file representation.
                     * @param loadedTypeInitializers The instrumented types and auxiliary types mapped to their loaded type initializers.
                     * @param classInjector          The class injector to use.
                     */
                    protected InjectingInitializer(TypeDescription instrumentedType,
                                                   Map<TypeDescription, byte[]> rawAuxiliaryTypes,
                                                   Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers,
                                                   ClassInjector classInjector) {
                        this.instrumentedType = instrumentedType;
                        this.rawAuxiliaryTypes = rawAuxiliaryTypes;
                        this.loadedTypeInitializers = loadedTypeInitializers;
                        this.classInjector = classInjector;
                    }

                    @Override
                    public void onLoad(Class<?> type) {
                        for (Map.Entry<TypeDescription, Class<?>> auxiliary : classInjector.inject(rawAuxiliaryTypes).entrySet()) {
                            loadedTypeInitializers.get(auxiliary.getKey()).onLoad(auxiliary.getValue());
                        }
                        loadedTypeInitializers.get(instrumentedType).onLoad(type);
                    }

                    @Override
                    public boolean isAlive() {
                        return true;
                    }

                    @Override
                    public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        InjectingInitializer that = (InjectingInitializer) o;
                        return classInjector.equals(that.classInjector)
                                && instrumentedType.equals(that.instrumentedType)
                                && rawAuxiliaryTypes.equals(that.rawAuxiliaryTypes)
                                && loadedTypeInitializers.equals(that.loadedTypeInitializers);
                    }

                    @Override
                    public int hashCode() {
                        int result = instrumentedType.hashCode();
                        result = 31 * result + rawAuxiliaryTypes.hashCode();
                        result = 31 * result + loadedTypeInitializers.hashCode();
                        result = 31 * result + classInjector.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.InitializationStrategy.SelfInjection.Dispatcher.InjectingInitializer{" +
                                "instrumentedType=" + instrumentedType +
                                ", rawAuxiliaryTypes=" + rawAuxiliaryTypes +
                                ", loadedTypeInitializers=" + loadedTypeInitializers +
                                ", classInjector=" + classInjector +
                                '}';
                    }
                }
            }
        }

        /**
         * An initialization strategy that loads auxiliary types before loading the instrumented type. This strategy skips all types
         * that are a subtype of the instrumented type which would cause a premature loading of the instrumented type and abort
         * the instrumentation process.
         */
        enum Minimal implements InitializationStrategy, Dispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Dispatcher dispatcher() {
                return this;
            }

            @Override
            public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                return builder;
            }

            @Override
            public void register(DynamicType dynamicType, ClassLoader classLoader, InjectorFactory injectorFactory) {
                Map<TypeDescription, byte[]> auxiliaryTypes = dynamicType.getAuxiliaryTypes();
                Map<TypeDescription, byte[]> independentTypes = new LinkedHashMap<TypeDescription, byte[]>(auxiliaryTypes);
                for (TypeDescription auxiliaryType : auxiliaryTypes.keySet()) {
                    if (!auxiliaryType.getDeclaredAnnotations().isAnnotationPresent(AuxiliaryType.SignatureRelevant.class)) {
                        independentTypes.remove(auxiliaryType);
                    }
                }
                if (!independentTypes.isEmpty()) {
                    ClassInjector classInjector = injectorFactory.resolve();
                    Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = dynamicType.getLoadedTypeInitializers();
                    for (Map.Entry<TypeDescription, Class<?>> entry : classInjector.inject(independentTypes).entrySet()) {
                        loadedTypeInitializers.get(entry.getKey()).onLoad(entry.getValue());
                    }
                }
            }

            @Override
            public String toString() {
                return "AgentBuilder.InitializationStrategy.Minimal." + name();
            }
        }
    }

    /**
     * A description strategy is responsible for resolving a {@link TypeDescription} when transforming or retransforming/-defining a type.
     */
    interface DescriptionStrategy {

        /**
         * Describes the given type.
         *
         * @param typeName The binary name of the type to describe.
         * @param type     The type that is being redefined, if a redefinition is applied or {@code null} if no redefined type is available.
         * @param typePool The type pool to use for locating a type if required.
         * @return An appropriate type description.
         */
        TypeDescription apply(String typeName, Class<?> type, TypePool typePool);

        /**
         * Indicates if this description strategy makes use of loaded type information and yields a different type description if no loaded type is available.
         *
         * @return {@code true} if this description strategy prefers loaded type information when describing a type and only uses a type pool
         * if loaded type information is not available.
         */
        boolean isLoadedFirst();

        /**
         * Default implementations of a {@link DescriptionStrategy}.
         */
        enum Default implements DescriptionStrategy {

            /**
             * A description type strategy represents a type as a {@link net.bytebuddy.description.type.TypeDescription.ForLoadedType} if a
             * retransformation or redefinition is applied on a type. Using a loaded type typically results in better performance as no
             * I/O is required for resolving type descriptions. However, any interaction with the type is carried out via the Java reflection
             * API. Using the reflection API triggers eager loading of any type that is part of a method or field signature. If any of these
             * types are missing from the class path, this eager loading will cause a {@link NoClassDefFoundError}. Some Java code declares
             * optional dependencies to other classes which are only realized if the optional dependency is present. Such code relies on the
             * Java reflection API not being used for types using optional dependencies.
             *
             * @see FallbackStrategy.Default#ENABLED
             * @see FallbackStrategy.ByThrowableType#ofOptionalTypes()
             */
            HYBRID(true) {
                @Override
                public TypeDescription apply(String typeName, Class<?> type, TypePool typePool) {
                    return type == null
                            ? typePool.describe(typeName).resolve()
                            : new TypeDescription.ForLoadedType(type);
                }
            },

            /**
             * <p>
             * A description strategy that always describes Java types using a {@link TypePool}. This requires that any type - even if it is already
             * loaded and a {@link Class} instance is available - is processed as a non-loaded type description. Doing so can cause overhead as processing
             * loaded types is supported very efficiently by a JVM.
             * </p>
             * <p>
             * Avoiding the usage of loaded types can improve robustness as this approach does not rely on the Java reflection API which triggers eager
             * validation of this loaded type which can fail an application if optional types are used by any types field or method signatures. Also, it
             * is possible to guarantee debugging meta data to be available also for retransformed or redefined types if a {@link TypeStrategy} specifies
             * the extraction of such meta data.
             * </p>
             */
            POOL_ONLY(false) {
                @Override
                public TypeDescription apply(String typeName, Class<?> type, TypePool typePool) {
                    return typePool.describe(typeName).resolve();
                }
            },

            /**
             * <p>
             * A description strategy that always describes Java types using a {@link TypePool} unless a type cannot be resolved by a pool and a loaded
             * {@link Class} instance  is available. Doing so can cause overhead as processing loaded types is supported very efficiently by a JVM.
             * </p>
             * <p>
             * Avoiding the usage of loaded types can improve robustness as this approach does not rely on the Java reflection API which triggers eager
             * validation of this loaded type which can fail an application if optional types are used by any types field or method signatures. Also, it
             * is possible to guarantee debugging meta data to be available also for retransformed or redefined types if a {@link TypeStrategy} specifies
             * the extraction of such meta data.
             * </p>
             */
            POOL_FIRST(false) {
                @Override
                public TypeDescription apply(String typeName, Class<?> type, TypePool typePool) {
                    TypePool.Resolution resolution = typePool.describe(typeName);
                    return resolution.isResolved() || type == null
                            ? resolution.resolve()
                            : new TypeDescription.ForLoadedType(type);
                }
            };

            /**
             * Indicates if loaded type information is preferred over using a type pool for describing a type.
             */
            private final boolean loadedFirst;

            /**
             * Indicates if loaded type information is preferred over using a type pool for describing a type.
             *
             * @param loadedFirst {@code true} if loaded type information is preferred over using a type pool for describing a type.
             */
            Default(boolean loadedFirst) {
                this.loadedFirst = loadedFirst;
            }

            @Override
            public boolean isLoadedFirst() {
                return loadedFirst;
            }

            @Override
            public String toString() {
                return "AgentBuilder.DescriptionStrategy.Default." + name();
            }
        }
    }

    /**
     * An installation strategy determines the reaction to a raised exception after the registration of a {@link ClassFileTransformer}.
     */
    interface InstallationStrategy {

        /**
         * Handles an error that occured after registering a class file transformer during installation.
         *
         * @param instrumentation      The instrumentation onto which the class file transformer was registered.
         * @param classFileTransformer The class file transformer that was registered.
         * @param throwable            The error that occurred.
         * @return The class file transformer to return when an error occurred.
         */
        ClassFileTransformer onError(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, Throwable throwable);

        /**
         * Default implementations of installation strategies.
         */
        enum Default implements InstallationStrategy {

            /**
             * An installation strategy that unregisters the transformer and propagates the exception. Using this strategy does not guarantee
             * that the registered transformer was not applied to any class, nor does it attempt to revert previous transformations. It only
             * guarantees that the class file transformer is unregistered and does no longer apply after this method returns.
             */
            ESCALATING {
                @Override
                public ClassFileTransformer onError(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, Throwable throwable) {
                    instrumentation.removeTransformer(classFileTransformer);
                    throw new IllegalStateException("Could not install class file transformer", throwable);
                }
            },

            /**
             * An installation strategy that retains the class file transformer and suppresses the error.
             */
            SUPPRESSING {
                @Override
                public ClassFileTransformer onError(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, Throwable throwable) {
                    return classFileTransformer;
                }
            };

            @Override
            public String toString() {
                return "AgentBuilder.InstallationStrategy.Default." + name();
            }
        }
    }

    /**
     * A strategy for creating a {@link ClassFileLocator} when instrumenting a type.
     */
    interface LocationStrategy {

        /**
         * Creates a class file locator for a given class loader and module combination.
         *
         * @param classLoader The class loader that is loading an instrumented type. Might be {@code null} to represent the bootstrap class loader.
         * @param module      The type's module or {@code null} if Java modules are not supported on the current VM.
         * @return The class file locator to use.
         */
        ClassFileLocator classFileLocator(ClassLoader classLoader, JavaModule module);

        /**
         * A location strategy that never locates any byte code.
         */
        enum NoOp implements LocationStrategy {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public ClassFileLocator classFileLocator(ClassLoader classLoader, JavaModule module) {
                return ClassFileLocator.NoOp.INSTANCE;
            }

            @Override
            public String toString() {
                return "AgentBuilder.LocationStrategy.NoOp." + name();
            }
        }

        /**
         * A location strategy that locates class files by querying an instrumented type's {@link ClassLoader}.
         */
        enum ForClassLoader implements LocationStrategy {

            /**
             * A location strategy that keeps a strong reference to the class loader the created class file locator represents.
             */
            STRONG {
                @Override
                public ClassFileLocator classFileLocator(ClassLoader classLoader, JavaModule module) {
                    return ClassFileLocator.ForClassLoader.of(classLoader);
                }
            },

            /**
             * A location strategy that keeps a weak reference to the class loader the created class file locator represents.
             * As a consequence, any returned class file locator stops working once the represented class loader is garbage collected.
             */
            WEAK {
                @Override
                public ClassFileLocator classFileLocator(ClassLoader classLoader, JavaModule module) {
                    return ClassFileLocator.ForClassLoader.WeaklyReferenced.of(classLoader);
                }
            };

            /**
             * Adds additional location strategies as fallbacks to this location strategy.
             *
             * @param classFileLocator The class file locators to query if this location strategy cannot locate a class file.
             * @return A compound location strategy that first applies this location strategy and then queries the supplied class file locators.
             */
            public LocationStrategy withFallbackTo(ClassFileLocator... classFileLocator) {
                return withFallbackTo(Arrays.asList(classFileLocator));
            }

            /**
             * Adds additional location strategies as fallbacks to this location strategy.
             *
             * @param classFileLocators The class file locators to query if this location strategy cannot locate a class file.
             * @return A compound location strategy that first applies this location strategy and then queries the supplied class file locators.
             */
            public LocationStrategy withFallbackTo(Collection<? extends ClassFileLocator> classFileLocators) {
                List<LocationStrategy> locationStrategies = new ArrayList<LocationStrategy>(classFileLocators.size());
                for (ClassFileLocator classFileLocator : classFileLocators) {
                    locationStrategies.add(new Simple(classFileLocator));
                }
                return withFallbackTo(locationStrategies);
            }

            /**
             * Adds additional location strategies as fallbacks to this location strategy.
             *
             * @param locationStrategy The fallback location strategies to use.
             * @return A compound location strategy that first applies this location strategy and then the supplied fallback location strategies
             * in the supplied order.
             */
            public LocationStrategy withFallbackTo(LocationStrategy... locationStrategy) {
                return withFallbackTo(Arrays.asList(locationStrategy));
            }

            /**
             * Adds additional location strategies as fallbacks to this location strategy.
             *
             * @param locationStrategies The fallback location strategies to use.
             * @return A compound location strategy that first applies this location strategy and then the supplied fallback location strategies
             * in the supplied order.
             */
            public LocationStrategy withFallbackTo(List<? extends LocationStrategy> locationStrategies) {
                List<LocationStrategy> allLocationStrategies = new ArrayList<LocationStrategy>(locationStrategies.size() + 1);
                allLocationStrategies.add(this);
                allLocationStrategies.addAll(locationStrategies);
                return new Compound(allLocationStrategies);
            }

            @Override
            public String toString() {
                return "AgentBuilder.LocationStrategy.ForClassLoader." + name();
            }
        }

        /**
         * A simple location strategy that queries a given class file locator.
         */
        class Simple implements LocationStrategy {

            /**
             * The class file locator to query.
             */
            private final ClassFileLocator classFileLocator;

            /**
             * A simple location strategy that queries a given class file locator.
             *
             * @param classFileLocator The class file locator to query.
             */
            public Simple(ClassFileLocator classFileLocator) {
                this.classFileLocator = classFileLocator;
            }

            @Override
            public ClassFileLocator classFileLocator(ClassLoader classLoader, JavaModule module) {
                return classFileLocator;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Simple simple = (Simple) object;
                return classFileLocator.equals(simple.classFileLocator);
            }

            @Override
            public int hashCode() {
                return classFileLocator.hashCode();
            }

            @Override
            public String toString() {
                return "AgentBuilder.LocationStrategy.Simple{" +
                        "classFileLocator=" + classFileLocator +
                        '}';
            }
        }

        /**
         * A compound location strategy that applies a list of location strategies.
         */
        class Compound implements LocationStrategy {

            /**
             * The location strategies in their application order.
             */
            private final List<? extends LocationStrategy> locationStrategies;

            /**
             * Creates a new compound location strategy.
             *
             * @param locationStrategy The location strategies in their application order.
             */
            public Compound(LocationStrategy... locationStrategy) {
                this(Arrays.asList(locationStrategy));
            }

            /**
             * Creates a new compound location strategy.
             *
             * @param locationStrategies The location strategies in their application order.
             */
            public Compound(List<? extends LocationStrategy> locationStrategies) {
                this.locationStrategies = locationStrategies;
            }

            @Override
            public ClassFileLocator classFileLocator(ClassLoader classLoader, JavaModule module) {
                List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(locationStrategies.size());
                for (LocationStrategy locationStrategy : locationStrategies) {
                    classFileLocators.add(locationStrategy.classFileLocator(classLoader, module));
                }
                return new ClassFileLocator.Compound(classFileLocators);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Compound compound = (Compound) object;
                return locationStrategies.equals(compound.locationStrategies);
            }

            @Override
            public int hashCode() {
                return locationStrategies.hashCode();
            }

            @Override
            public String toString() {
                return "AgentBuilder.LocationStrategy.Compound{" +
                        "locationStrategies=" + locationStrategies +
                        '}';
            }
        }
    }

    /**
     * A fallback strategy allows to reattempt a transformation or a consideration for redefinition/retransformation in case an exception
     * occurs. Doing so, it is possible to use a {@link TypePool} rather than using a loaded type description backed by a {@link Class}.
     * Loaded types can raise exceptions and errors if a {@link ClassLoader} cannot resolve all types that this class references. Using
     * a type pool, such errors can be avoided as type descriptions can be resolved lazily, avoiding such errors.
     */
    interface FallbackStrategy {

        /**
         * Returns {@code true} if the supplied type and throwable combination should result in a reattempt where the
         * loaded type is not used for querying information.
         *
         * @param type      The loaded type that was queried during the transformation attempt.
         * @param throwable The error or exception that was caused during the transformation.
         * @return {@code true} if the supplied type and throwable combination should
         */
        boolean isFallback(Class<?> type, Throwable throwable);

        /**
         * A default fallback strategy that either always reattempts a transformation or never does so.
         */
        enum Default implements FallbackStrategy {

            /**
             * An enabled fallback strategy that always attempts a new trial.
             */
            ENABLED(true),

            /**
             * A disabled fallback strategy that never attempts a new trial.
             */
            DISABLED(false);

            /**
             * {@code true} if this fallback strategy is enabled.
             */
            private final boolean enabled;

            /**
             * Creates a new default fallback strategy.
             *
             * @param enabled {@code true} if this fallback strategy is enabled.
             */
            Default(boolean enabled) {
                this.enabled = enabled;
            }

            @Override
            public boolean isFallback(Class<?> type, Throwable throwable) {
                return enabled;
            }

            @Override
            public String toString() {
                return "AgentBuilder.FallbackStrategy.Default." + name();
            }
        }

        /**
         * A fallback strategy that discriminates by the type of the {@link Throwable} that triggered a request.
         */
        class ByThrowableType implements FallbackStrategy {

            /**
             * A set of throwable types that should trigger a fallback attempt.
             */
            private final Set<? extends Class<? extends Throwable>> types;

            /**
             * Creates a new throwable type-discriminating fallback strategy.
             *
             * @param type The throwable types that should trigger a fallback.
             */
            @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
            public ByThrowableType(Class<? extends Throwable>... type) {
                this(new HashSet<Class<? extends Throwable>>(Arrays.asList(type)));
            }

            /**
             * Creates a new throwable type-discriminating fallback strategy.
             *
             * @param types The throwable types that should trigger a fallback.
             */
            public ByThrowableType(Set<? extends Class<? extends Throwable>> types) {
                this.types = types;
            }

            /**
             * Creates a fallback strategy that attempts a fallback if an error indicating a type error is the reason for requesting a reattempt.
             *
             * @return A fallback strategy that triggers a reattempt if a {@link LinkageError} or a {@link TypeNotPresentException} is raised.
             */
            @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
            public static FallbackStrategy ofOptionalTypes() {
                return new ByThrowableType(LinkageError.class, TypeNotPresentException.class);
            }

            @Override
            public boolean isFallback(Class<?> type, Throwable throwable) {
                for (Class<? extends Throwable> aType : types) {
                    if (aType.isInstance(throwable)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ByThrowableType byType = (ByThrowableType) object;
                return types.equals(byType.types);
            }

            @Override
            public int hashCode() {
                return types.hashCode();
            }

            @Override
            public String toString() {
                return "AgentBuilder.FallbackStrategy.ByThrowableType{" +
                        "types=" + types +
                        '}';
            }
        }
    }

    /**
     * A redefinition strategy regulates how already loaded classes are modified by a built agent.
     */
    enum RedefinitionStrategy {

        /**
         * Disables redefinition such that already loaded classes are not affected by the agent.
         */
        DISABLED {
            @Override
            protected boolean isRetransforming(Instrumentation instrumentation) {
                return false;
            }

            @Override
            protected Collector makeCollector(Default.Transformation transformation) {
                throw new IllegalStateException("A disabled redefinition strategy cannot create a collector");
            }
        },

        /**
         * <p>
         * Applies a <b>redefinition</b> to all classes that are already loaded and that would have been transformed if
         * the built agent was registered before they were loaded. The created {@link ClassFileTransformer} is <b>not</b>
         * registered for applying retransformations.
         * </p>
         * <p>
         * Using this strategy, a redefinition is applied as a single transformation request. This means that a single illegal
         * redefinition of a class causes the entire redefinition attempt to fail.
         * </p>
         * <p>
         * <b>Note</b>: When applying a redefinition, it is normally required to use a {@link TypeStrategy} that applies
         * a redefinition instead of rebasing classes such as {@link TypeStrategy.Default#REDEFINE}. Also, consider
         * the constrains given by this type strategy.
         * </p>
         */
        REDEFINITION {
            @Override
            protected boolean isRetransforming(Instrumentation instrumentation) {
                if (!instrumentation.isRedefineClassesSupported()) {
                    throw new IllegalArgumentException("Cannot redefine classes: " + instrumentation);
                }
                return false;
            }

            @Override
            protected Collector makeCollector(Default.Transformation transformation) {
                return new Collector.ForRedefinition.Cumulative(transformation);
            }
        },

        /**
         * <p>
         * Applies a <b>redefinition</b> to all classes that are already loaded and that would have been transformed if
         * the built agent was registered before they were loaded. The created {@link ClassFileTransformer} is <b>not</b>
         * registered for applying retransformations.
         * </p>
         * <p>
         * Using this strategy, a redefinition is applied in single class chunks. This means that a single illegal
         * redefinition of a class does not cause the failure of any other redefinition. Chunking the redefinition does
         * however imply a performance penalty. If at least one redefinition has failed, applying this strategy still causes an
         * exception to be thrown as a result of the application.
         * </p>
         * <p>
         * <b>Note</b>: When applying a redefinition, it is normally required to use a {@link TypeStrategy} that applies
         * a redefinition instead of rebasing classes such as {@link TypeStrategy.Default#REDEFINE}. Also, consider
         * the constrains given by this type strategy.
         * </p>
         */
        REDEFINITION_CHUNKED {
            @Override
            protected boolean isRetransforming(Instrumentation instrumentation) {
                return REDEFINITION.isRetransforming(instrumentation);
            }

            @Override
            protected Collector makeCollector(Default.Transformation transformation) {
                return new Collector.ForRedefinition.Chunked(transformation);
            }
        },

        /**
         * <p>
         * Applies a <b>retransformation</b> to all classes that are already loaded and that would have been transformed if
         * the built agent was registered before they were loaded. The created {@link ClassFileTransformer} is registered
         * for applying retransformations.
         * </p>
         * <p>
         * Using this strategy, a retransformation is applied as a single transformation request. This means that a single illegal
         * retransformation of a class causes the entire retransformation attempt to fail.
         * </p>
         * <p>
         * <b>Note</b>: When applying a redefinition, it is normally required to use a {@link TypeStrategy} that applies
         * a redefinition instead of rebasing classes such as {@link TypeStrategy.Default#REDEFINE}. Also, consider
         * the constrains given by this type strategy.
         * </p>
         */
        RETRANSFORMATION {
            @Override
            protected boolean isRetransforming(Instrumentation instrumentation) {
                if (!instrumentation.isRetransformClassesSupported()) {
                    throw new IllegalArgumentException("Cannot retransform classes: " + instrumentation);
                }
                return true;
            }

            @Override
            protected Collector makeCollector(Default.Transformation transformation) {
                return new Collector.ForRetransformation.Cumulative(transformation);
            }
        },

        /**
         * <p>
         * Applies a <b>retransformation</b> to all classes that are already loaded and that would have been transformed if
         * the built agent was registered before they were loaded. The created {@link ClassFileTransformer} is registered
         * for applying retransformations.
         * </p>
         * <p>
         * Using this strategy, a retransformation is applied in single class chunks. This means that a single illegal
         * retransformation of a class does not cause the failure of any other redefinition. Chunking the retransformation does
         * however imply a performance penalty. If at least one retransformation has failed, applying this strategy still causes an
         * exception to be thrown as a result of the application.
         * </p>
         * <p>
         * <b>Note</b>: When applying a redefinition, it is normally required to use a {@link TypeStrategy} that applies
         * a redefinition instead of rebasing classes such as {@link TypeStrategy.Default#REDEFINE}. Also, consider
         * the constrains given by this type strategy.
         * </p>
         */
        RETRANSFORMATION_CHUNKED {
            @Override
            protected boolean isRetransforming(Instrumentation instrumentation) {
                return RETRANSFORMATION.isRetransforming(instrumentation);
            }

            @Override
            protected Collector makeCollector(Default.Transformation transformation) {
                return new Collector.ForRetransformation.Chunked(transformation);
            }
        };

        /**
         * Indicates if this strategy requires a class file transformer to be registered with a hint to apply the
         * transformer for retransformation.
         *
         * @param instrumentation The instrumentation instance used.
         * @return {@code true} if a class file transformer must be registered with a hint for retransformation.
         */
        protected abstract boolean isRetransforming(Instrumentation instrumentation);

        /**
         * Indicates that this redefinition strategy applies a modification of already loaded classes.
         *
         * @return {@code true} if this redefinition strategy applies a modification of already loaded classes.
         */
        protected boolean isEnabled() {
            return this != DISABLED;
        }

        /**
         * Creates a collector instance that is responsible for collecting loaded classes for potential retransformation.
         *
         * @param transformation The transformation that is registered for the agent.
         * @return A new collector for collecting already loaded classes for transformation.
         */
        protected abstract Collector makeCollector(Default.Transformation transformation);

        @Override
        public String toString() {
            return "AgentBuilder.RedefinitionStrategy." + name();
        }

        /**
         * A collector is responsible for collecting classes that are to be considered for modification.
         */
        protected interface Collector {

            /**
             * Considers a loaded class for modification.
             *
             * @param typeDescription    The type description of the type that is to be considered.
             * @param type               The loaded representation of the type that is to be considered.
             * @param ignoredTypeMatcher Identifies types that should not be instrumented.
             * @return {@code true} if the class is considered to be redefined.
             */
            boolean consider(TypeDescription typeDescription, Class<?> type, Class<?> classBeingRedefined, RawMatcher ignoredTypeMatcher);

            /**
             * Applies this collector.
             *
             * @param instrumentation  The instrumentation instance to apply the transformation for.
             * @param poolStrategy     The type locator to use.
             * @param locationStrategy The location strategy to use.
             * @param listener         the listener to notify.
             * @throws UnmodifiableClassException If a class is not modifiable.
             * @throws ClassNotFoundException     If a class could not be found.
             */
            void apply(Instrumentation instrumentation,
                       PoolStrategy poolStrategy,
                       LocationStrategy locationStrategy,
                       Listener listener) throws UnmodifiableClassException, ClassNotFoundException;

            /**
             * A collector that applies a <b>redefinition</b> of already loaded classes.
             */
            abstract class ForRedefinition implements Collector {

                /**
                 * The transformation of the built agent.
                 */
                protected final Default.Transformation transformation;

                /**
                 * A list of already collected redefinitions.
                 */
                protected final List<Entry> entries;

                /**
                 * Creates a new collector for a redefinition.
                 *
                 * @param transformation The transformation of the built agent.
                 */
                protected ForRedefinition(Default.Transformation transformation) {
                    this.transformation = transformation;
                    entries = new ArrayList<Entry>();
                }

                @Override
                public boolean consider(TypeDescription typeDescription, Class<?> type, Class<?> classBeingRedefined, RawMatcher ignoredTypeMatcher) {
                    return transformation.isAlive(typeDescription,
                            type.getClassLoader(),
                            JavaModule.ofType(type),
                            classBeingRedefined,
                            type.getProtectionDomain(),
                            ignoredTypeMatcher) && entries.add(new Entry(type));
                }

                @Override
                public void apply(Instrumentation instrumentation,
                                  PoolStrategy poolStrategy,
                                  LocationStrategy locationStrategy,
                                  Listener listener) throws UnmodifiableClassException, ClassNotFoundException {
                    List<ClassDefinition> classDefinitions = new ArrayList<ClassDefinition>(entries.size());
                    for (Entry entry : entries) {
                        try {
                            classDefinitions.add(entry.resolve(locationStrategy));
                        } catch (Throwable throwable) {
                            JavaModule module = JavaModule.ofType(entry.getType());
                            try {
                                listener.onError(TypeDescription.ForLoadedType.getName(entry.getType()), entry.getType().getClassLoader(), module, throwable);
                            } finally {
                                listener.onComplete(TypeDescription.ForLoadedType.getName(entry.getType()), entry.getType().getClassLoader(), module);
                            }
                        }
                    }
                    doApply(instrumentation, classDefinitions);
                }

                /**
                 * Applies a redefinition.
                 *
                 * @param instrumentation  The instrumentation instance to use.
                 * @param classDefinitions The class definitions to apply.
                 * @throws UnmodifiableClassException If a class is not modifiable.
                 * @throws ClassNotFoundException     If a class could not be found.
                 */
                protected abstract void doApply(Instrumentation instrumentation, List<ClassDefinition> classDefinitions) throws UnmodifiableClassException, ClassNotFoundException;

                /**
                 * A collector that applies a redefinition and applies all redefinitions as a single transformation request.
                 */
                protected static class Cumulative extends ForRedefinition {

                    /**
                     * Creates a new cumulative redefinition collector.
                     *
                     * @param transformation The transformation of the built agent.
                     */
                    protected Cumulative(Default.Transformation transformation) {
                        super(transformation);
                    }

                    @Override
                    protected void doApply(Instrumentation instrumentation, List<ClassDefinition> classDefinitions) throws UnmodifiableClassException, ClassNotFoundException {
                        if (!classDefinitions.isEmpty()) {
                            instrumentation.redefineClasses(classDefinitions.toArray(new ClassDefinition[classDefinitions.size()]));
                        }
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.Cumulative{" +
                                "transformation=" + transformation +
                                ", entries=" + entries +
                                '}';
                    }
                }

                /**
                 * A collector that applies a redefinition and applies all redefinitions as a separate transformation request per class.
                 */
                protected static class Chunked extends ForRedefinition {

                    /**
                     * Creates a new chunked redefinition collector.
                     *
                     * @param transformation The transformation of the built agent.
                     */
                    protected Chunked(Default.Transformation transformation) {
                        super(transformation);
                    }

                    @Override
                    protected void doApply(Instrumentation instrumentation, List<ClassDefinition> classDefinitions) throws UnmodifiableClassException, ClassNotFoundException {
                        Map<Class<?>, Exception> exceptions = new HashMap<Class<?>, Exception>();
                        for (ClassDefinition classDefinition : classDefinitions) {
                            try {
                                instrumentation.redefineClasses(classDefinition);
                            } catch (Exception exception) {
                                exceptions.put(classDefinition.getDefinitionClass(), exception);
                            }
                        }
                        if (!exceptions.isEmpty()) {
                            throw new IllegalStateException("Could not retransform at least one class: " + exceptions);
                        }
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.Chunked{" +
                                "transformation=" + transformation +
                                ", entries=" + entries +
                                '}';
                    }
                }

                /**
                 * An entry describing a type redefinition.
                 */
                protected static class Entry {

                    /**
                     * The type to be redefined.
                     */
                    private final Class<?> type;

                    /**
                     * Creates a new entry for a given type.
                     *
                     * @param type The type to be redefined.
                     */
                    protected Entry(Class<?> type) {
                        this.type = type;
                    }

                    /**
                     * Returns the type that is being redefined.
                     *
                     * @return The type that is being redefined.
                     */
                    public Class<?> getType() {
                        return type;
                    }

                    /**
                     * Resolves the entry to a class definition.
                     *
                     * @param locationStrategy A strategy for creating a class file locator.
                     * @return A class definition representing the redefined class.
                     * @throws IOException If an I/O exception occurs.
                     */
                    protected ClassDefinition resolve(LocationStrategy locationStrategy) throws IOException {
                        return new ClassDefinition(type, locationStrategy
                                .classFileLocator(type.getClassLoader(), JavaModule.ofType(type))
                                .locate(TypeDescription.ForLoadedType.getName(type)).resolve());
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Entry entry = (Entry) other;
                        return type.equals(entry.type);
                    }

                    @Override
                    public int hashCode() {
                        return type.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.Entry{" +
                                "type=" + type +
                                '}';
                    }
                }
            }

            /**
             * A collector that applies a <b>retransformation</b> of already loaded classes.
             */
            abstract class ForRetransformation implements Collector {

                /**
                 * The transformation defined by the built agent.
                 */
                protected final Default.Transformation transformation;

                /**
                 * The types that were collected for retransformation.
                 */
                protected final List<Class<?>> types;

                /**
                 * Creates a new collector for a retransformation.
                 *
                 * @param transformation The transformation defined by the built agent.
                 */
                protected ForRetransformation(Default.Transformation transformation) {
                    this.transformation = transformation;
                    types = new ArrayList<Class<?>>();
                }

                @Override
                public boolean consider(TypeDescription typeDescription, Class<?> type, Class<?> classBeingRedefined, RawMatcher ignoredTypeMatcher) {
                    return transformation.isAlive(typeDescription,
                            type.getClassLoader(),
                            JavaModule.ofType(type),
                            classBeingRedefined,
                            type.getProtectionDomain(),
                            ignoredTypeMatcher) && types.add(type);
                }

                /**
                 * A collector that applies a retransformation and applies all redefinitions as a single transformation request.
                 */
                protected static class Cumulative extends ForRetransformation {

                    /**
                     * Creates a new cumulative retransformation collector.
                     *
                     * @param transformation The transformation of the built agent.
                     */
                    protected Cumulative(Default.Transformation transformation) {
                        super(transformation);
                    }

                    @Override
                    public void apply(Instrumentation instrumentation,
                                      PoolStrategy poolStrategy,
                                      LocationStrategy locationStrategy,
                                      Listener listener) throws UnmodifiableClassException {
                        if (!types.isEmpty()) {
                            instrumentation.retransformClasses(types.toArray(new Class<?>[types.size()]));
                        }
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.RedefinitionStrategy.Collector.ForRetransformation.Cumulative{" +
                                "transformation=" + transformation +
                                ", types=" + types +
                                '}';
                    }
                }

                /**
                 * A collector that applies a retransformation and applies all redefinitions as a chunked transformation request.
                 */
                protected static class Chunked extends ForRetransformation {

                    /**
                     * Creates a new chunked retransformation collector.
                     *
                     * @param transformation The transformation of the built agent.
                     */
                    protected Chunked(Default.Transformation transformation) {
                        super(transformation);
                    }

                    @Override
                    public void apply(Instrumentation instrumentation,
                                      PoolStrategy poolStrategy,
                                      LocationStrategy locationStrategy,
                                      Listener listener) throws UnmodifiableClassException {
                        Map<Class<?>, Exception> exceptions = new HashMap<Class<?>, Exception>();
                        for (Class<?> type : types) {
                            try {
                                instrumentation.retransformClasses(type);
                            } catch (Exception exception) {
                                exceptions.put(type, exception);
                            }
                        }
                        if (!exceptions.isEmpty()) {
                            throw new IllegalStateException("Could not retransform at least one class: " + exceptions);
                        }
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.RedefinitionStrategy.Collector.ForRetransformation.Chunked{" +
                                "transformation=" + transformation +
                                ", types=" + types +
                                '}';
                    }
                }
            }
        }
    }

    /**
     * Implements the instrumentation of the {@code LambdaMetafactory} if this feature is enabled.
     */
    enum LambdaInstrumentationStrategy {

        /**
         * A strategy that enables instrumentation of the {@code LambdaMetafactory} if such a factory exists on the current VM.
         * Classes representing lambda expressions that are created by Byte Buddy are fully compatible to those created by
         * the JVM and can be serialized or deserialized to one another. The classes do however show a few differences:
         * <ul>
         * <li>Byte Buddy's classes are public with a public executing transformer. Doing so, it is not necessary to instantiate a
         * non-capturing lambda expression by reflection. This is done because Byte Buddy is not necessarily capable
         * of using reflection due to an active security manager.</li>
         * <li>Byte Buddy's classes are not marked as synthetic as an agent builder does not instrument synthetic classes
         * by default.</li>
         * </ul>
         */
        ENABLED {
            @Override
            protected void apply(ByteBuddy byteBuddy,
                                 Instrumentation instrumentation,
                                 ClassFileTransformer classFileTransformer) {
                if (LambdaFactory.register(classFileTransformer, new LambdaInstanceFactory(byteBuddy), LambdaInjector.INSTANCE)) {
                    Class<?> lambdaMetaFactory;
                    try {
                        lambdaMetaFactory = Class.forName("java.lang.invoke.LambdaMetafactory");
                    } catch (ClassNotFoundException ignored) {
                        return;
                    }
                    byteBuddy.with(Implementation.Context.Disabled.Factory.INSTANCE)
                            .redefine(lambdaMetaFactory)
                            .visit(new AsmVisitorWrapper.ForDeclaredMethods()
                                    .method(named("metafactory"), MetaFactoryRedirection.INSTANCE)
                                    .method(named("altMetafactory"), AlternativeMetaFactoryRedirection.INSTANCE))
                            .make()
                            .load(lambdaMetaFactory.getClassLoader(), ClassReloadingStrategy.of(instrumentation));
                }
            }
        },

        /**
         * A strategy that does not instrument the {@code LambdaMetafactory}.
         */
        DISABLED {
            @Override
            protected void apply(ByteBuddy byteBuddy,
                                 Instrumentation instrumentation,
                                 ClassFileTransformer classFileTransformer) {
                    /* do nothing */
            }
        };

        /**
         * Indicates that an original implementation can be ignored when redefining a method.
         */
        protected static final MethodVisitor IGNORE_ORIGINAL = null;

        /**
         * Releases the supplied class file transformer when it was built with {@link AgentBuilder#with(LambdaInstrumentationStrategy)} enabled.
         * Subsequently, the class file transformer is no longer applied when a class that represents a lambda expression is created.
         *
         * @param classFileTransformer The class file transformer to release.
         * @param instrumentation      The instrumentation instance that is used to potentially rollback the instrumentation of the {@code LambdaMetafactory}.
         */
        public static void release(ClassFileTransformer classFileTransformer, Instrumentation instrumentation) {
            if (LambdaFactory.release(classFileTransformer)) {
                try {
                    ClassReloadingStrategy.of(instrumentation).reset(Class.forName("java.lang.invoke.LambdaMetafactory"));
                } catch (Exception exception) {
                    throw new IllegalStateException("Could not release lambda transformer", exception);
                }
            }
        }

        /**
         * Returns an enabled lambda instrumentation strategy for {@code true}.
         *
         * @param enabled If lambda instrumentation should be enabled.
         * @return {@code true} if the returned strategy should be enabled.
         */
        public static LambdaInstrumentationStrategy of(boolean enabled) {
            return enabled
                    ? ENABLED
                    : DISABLED;
        }

        /**
         * Applies a transformation to lambda instances if applicable.
         *
         * @param byteBuddy            The Byte Buddy instance to use.
         * @param instrumentation      The instrumentation instance for applying a redefinition.
         * @param classFileTransformer The class file transformer to apply.
         */
        protected abstract void apply(ByteBuddy byteBuddy, Instrumentation instrumentation, ClassFileTransformer classFileTransformer);

        /**
         * Indicates if this strategy enables instrumentation of the {@code LambdaMetafactory}.
         *
         * @return {@code true} if this strategy is enabled.
         */
        public boolean isEnabled() {
            return this == ENABLED;
        }

        @Override
        public String toString() {
            return "AgentBuilder.LambdaInstrumentationStrategy." + name();
        }

        /**
         * An injector for injecting the lambda class dispatcher to the system class path.
         */
        protected enum LambdaInjector implements Callable<Class<?>> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Class<?> call() throws Exception {
                TypeDescription lambdaFactory = new TypeDescription.ForLoadedType(LambdaFactory.class);
                return ClassInjector.UsingReflection.ofSystemClassLoader()
                        .inject(Collections.singletonMap(lambdaFactory, ClassFileLocator.ForClassLoader.read(LambdaFactory.class).resolve()))
                        .get(lambdaFactory);
            }

            @Override
            public String toString() {
                return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInjector." + name();
            }
        }

        /**
         * A factory that creates instances that represent lambda expressions.
         */
        protected static class LambdaInstanceFactory {

            /**
             * The name of a factory for a lambda expression.
             */
            private static final String LAMBDA_FACTORY = "get$Lambda";

            /**
             * A prefix for a field that represents a property of a lambda expression.
             */
            private static final String FIELD_PREFIX = "arg$";

            /**
             * The infix to use for naming classes that represent lambda expression. The additional prefix
             * is necessary because the subsequent counter is not sufficient to keep names unique compared
             * to the original factory.
             */
            private static final String LAMBDA_TYPE_INFIX = "$$Lambda$ByteBuddy$";

            /**
             * A type-safe constant to express that a class is not already loaded when applying a class file transformer.
             */
            private static final Class<?> NOT_PREVIOUSLY_DEFINED = null;

            /**
             * A counter for naming lambda expressions randomly.
             */
            private static final AtomicInteger LAMBDA_NAME_COUNTER = new AtomicInteger();

            /**
             * The Byte Buddy instance to use for creating lambda objects.
             */
            private final ByteBuddy byteBuddy;

            /**
             * Creates a new lambda instance factory.
             *
             * @param byteBuddy The Byte Buddy instance to use for creating lambda objects.
             */
            protected LambdaInstanceFactory(ByteBuddy byteBuddy) {
                this.byteBuddy = byteBuddy;
            }

            /**
             * Applies this lambda meta factory.
             *
             * @param targetTypeLookup            A lookup context representing the creating class of this lambda expression.
             * @param lambdaMethodName            The name of the lambda expression's represented method.
             * @param factoryMethodType           The type of the lambda expression's represented method.
             * @param lambdaMethodType            The type of the lambda expression's factory method.
             * @param targetMethodHandle          A handle representing the target of the lambda expression's method.
             * @param specializedLambdaMethodType A specialization of the type of the lambda expression's represented method.
             * @param serializable                {@code true} if the lambda expression should be serializable.
             * @param markerInterfaces            A list of interfaces for the lambda expression to represent.
             * @param additionalBridges           A list of additional bridge methods to be implemented by the lambda expression.
             * @param classFileTransformers       A collection of class file transformers to apply when creating the class.
             * @return A binary representation of the transformed class file.
             */
            public byte[] make(Object targetTypeLookup,
                               String lambdaMethodName,
                               Object factoryMethodType,
                               Object lambdaMethodType,
                               Object targetMethodHandle,
                               Object specializedLambdaMethodType,
                               boolean serializable,
                               List<Class<?>> markerInterfaces,
                               List<?> additionalBridges,
                               Collection<? extends ClassFileTransformer> classFileTransformers) {
                JavaConstant.MethodType factoryMethod = JavaConstant.MethodType.ofLoaded(factoryMethodType);
                JavaConstant.MethodType lambdaMethod = JavaConstant.MethodType.ofLoaded(lambdaMethodType);
                JavaConstant.MethodHandle targetMethod = JavaConstant.MethodHandle.ofLoaded(targetMethodHandle, targetTypeLookup);
                JavaConstant.MethodType specializedLambdaMethod = JavaConstant.MethodType.ofLoaded(specializedLambdaMethodType);
                Class<?> targetType = JavaConstant.MethodHandle.lookupType(targetTypeLookup);
                String lambdaClassName = targetType.getName() + LAMBDA_TYPE_INFIX + LAMBDA_NAME_COUNTER.incrementAndGet();
                DynamicType.Builder<?> builder = byteBuddy
                        .subclass(factoryMethod.getReturnType(), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .modifiers(TypeManifestation.FINAL, Visibility.PUBLIC)
                        .implement(markerInterfaces)
                        .name(lambdaClassName)
                        .defineConstructor(Visibility.PUBLIC)
                        .withParameters(factoryMethod.getParameterTypes())
                        .intercept(ConstructorImplementation.INSTANCE)
                        .method(named(lambdaMethodName)
                                .and(takesArguments(lambdaMethod.getParameterTypes()))
                                .and(returns(lambdaMethod.getReturnType())))
                        .intercept(new LambdaMethodImplementation(targetMethod, specializedLambdaMethod));
                int index = 0;
                for (TypeDescription capturedType : factoryMethod.getParameterTypes()) {
                    builder = builder.defineField(FIELD_PREFIX + ++index, capturedType, Visibility.PRIVATE, FieldManifestation.FINAL);
                }
                if (!factoryMethod.getParameterTypes().isEmpty()) {
                    builder = builder.defineMethod(LAMBDA_FACTORY, factoryMethod.getReturnType(), Visibility.PRIVATE, Ownership.STATIC)
                            .withParameters(factoryMethod.getParameterTypes())
                            .intercept(FactoryImplementation.INSTANCE);
                }
                if (serializable) {
                    if (!markerInterfaces.contains(Serializable.class)) {
                        builder = builder.implement(Serializable.class);
                    }
                    builder = builder.defineMethod("writeReplace", Object.class, Visibility.PRIVATE)
                            .intercept(new SerializationImplementation(new TypeDescription.ForLoadedType(targetType),
                                    factoryMethod.getReturnType(),
                                    lambdaMethodName,
                                    lambdaMethod,
                                    targetMethod,
                                    JavaConstant.MethodType.ofLoaded(specializedLambdaMethodType)));
                } else if (factoryMethod.getReturnType().isAssignableTo(Serializable.class)) {
                    builder = builder.defineMethod("readObject", void.class, Visibility.PRIVATE)
                            .withParameters(ObjectInputStream.class)
                            .throwing(NotSerializableException.class)
                            .intercept(ExceptionMethod.throwing(NotSerializableException.class, "Non-serializable lambda"))
                            .defineMethod("writeObject", void.class, Visibility.PRIVATE)
                            .withParameters(ObjectOutputStream.class)
                            .throwing(NotSerializableException.class)
                            .intercept(ExceptionMethod.throwing(NotSerializableException.class, "Non-serializable lambda"));
                }
                for (Object additionalBridgeType : additionalBridges) {
                    JavaConstant.MethodType additionalBridge = JavaConstant.MethodType.ofLoaded(additionalBridgeType);
                    builder = builder.defineMethod(lambdaMethodName, additionalBridge.getReturnType(), MethodManifestation.BRIDGE, Visibility.PUBLIC)
                            .withParameters(additionalBridge.getParameterTypes())
                            .intercept(new BridgeMethodImplementation(lambdaMethodName, lambdaMethod));
                }
                byte[] classFile = builder.make().getBytes();
                for (ClassFileTransformer classFileTransformer : classFileTransformers) {
                    try {
                        byte[] transformedClassFile = classFileTransformer.transform(targetType.getClassLoader(),
                                lambdaClassName.replace('.', '/'),
                                NOT_PREVIOUSLY_DEFINED,
                                targetType.getProtectionDomain(),
                                classFile);
                        classFile = transformedClassFile == null
                                ? classFile
                                : transformedClassFile;
                    } catch (Throwable ignored) {
                            /* do nothing */
                    }
                }
                return classFile;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && byteBuddy.equals(((LambdaInstanceFactory) other).byteBuddy);
            }

            @Override
            public int hashCode() {
                return byteBuddy.hashCode();
            }

            @Override
            public String toString() {
                return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory{" +
                        "byteBuddy=" + byteBuddy +
                        '}';
            }

            /**
             * Implements a lambda class's executing transformer.
             */
            @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "An enumeration does not serialize fields")
            protected enum ConstructorImplementation implements Implementation {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A reference to the {@link Object} class's default executing transformer.
                 */
                private final MethodDescription.InDefinedShape objectConstructor;

                /**
                 * Creates a new executing transformer implementation.
                 */
                ConstructorImplementation() {
                    objectConstructor = TypeDescription.OBJECT.getDeclaredMethods().filter(isConstructor()).getOnly();
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(implementationTarget.getInstrumentedType().getDeclaredFields());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.ConstructorImplementation." + name();
                }

                /**
                 * An appender to implement the executing transformer.
                 */
                protected static class Appender implements ByteCodeAppender {

                    /**
                     * The fields that are declared by the instrumented type.
                     */
                    private final List<FieldDescription.InDefinedShape> declaredFields;

                    /**
                     * Creates a new appender.
                     *
                     * @param declaredFields The fields that are declared by the instrumented type.
                     */
                    protected Appender(List<FieldDescription.InDefinedShape> declaredFields) {
                        this.declaredFields = declaredFields;
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                        List<StackManipulation> fieldAssignments = new ArrayList<StackManipulation>(declaredFields.size() * 3);
                        for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                            fieldAssignments.add(MethodVariableAccess.REFERENCE.loadOffset(0));
                            fieldAssignments.add(MethodVariableAccess.of(parameterDescription.getType()).loadOffset(parameterDescription.getOffset()));
                            fieldAssignments.add(FieldAccess.forField(declaredFields.get(parameterDescription.getIndex())).putter());
                        }
                        return new Size(new StackManipulation.Compound(
                                MethodVariableAccess.REFERENCE.loadOffset(0),
                                MethodInvocation.invoke(INSTANCE.objectConstructor),
                                new StackManipulation.Compound(fieldAssignments),
                                MethodReturn.VOID
                        ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && declaredFields.equals(((Appender) other).declaredFields);
                    }

                    @Override
                    public int hashCode() {
                        return declaredFields.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.ConstructorImplementation.Appender{" +
                                "declaredFields=" + declaredFields +
                                '}';
                    }
                }
            }

            /**
             * An implementation of a instance factory for a lambda expression's class.
             */
            protected enum FactoryImplementation implements Implementation {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(implementationTarget.getInstrumentedType());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.FactoryImplementation." + name();
                }

                /**
                 * An appender for a lambda expression factory.
                 */
                protected static class Appender implements ByteCodeAppender {

                    /**
                     * The instrumented type.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * Creates a new appender.
                     *
                     * @param instrumentedType The instrumented type.
                     */
                    protected Appender(TypeDescription instrumentedType) {
                        this.instrumentedType = instrumentedType;
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                        return new Size(new StackManipulation.Compound(
                                TypeCreation.of(instrumentedType),
                                Duplication.SINGLE,
                                MethodVariableAccess.allArgumentsOf(instrumentedMethod),
                                MethodInvocation.invoke(instrumentedType.getDeclaredMethods().filter(isConstructor()).getOnly()),
                                MethodReturn.REFERENCE
                        ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && instrumentedType.equals(((Appender) other).instrumentedType);
                    }

                    @Override
                    public int hashCode() {
                        return instrumentedType.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.FactoryImplementation.Appender{" +
                                "instrumentedType=" + instrumentedType +
                                '}';
                    }
                }
            }

            /**
             * Implements a lambda expression's functional method.
             */
            protected static class LambdaMethodImplementation implements Implementation {

                /**
                 * The handle of the target method of the lambda expression.
                 */
                private final JavaConstant.MethodHandle targetMethod;

                /**
                 * The specialized type of the lambda method.
                 */
                private final JavaConstant.MethodType specializedLambdaMethod;

                /**
                 * Creates a implementation of a lambda expression's functional method.
                 *
                 * @param targetMethod            The target method of the lambda expression.
                 * @param specializedLambdaMethod The specialized type of the lambda method.
                 */
                protected LambdaMethodImplementation(JavaConstant.MethodHandle targetMethod, JavaConstant.MethodType specializedLambdaMethod) {
                    this.targetMethod = targetMethod;
                    this.specializedLambdaMethod = specializedLambdaMethod;
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(targetMethod.getOwnerType()
                            .getDeclaredMethods()
                            .filter(named(targetMethod.getName())
                                    .and(returns(targetMethod.getReturnType()))
                                    .and(takesArguments(targetMethod.getParameterTypes())))
                            .getOnly(),
                            specializedLambdaMethod,
                            implementationTarget.getInstrumentedType().getDeclaredFields());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    LambdaMethodImplementation that = (LambdaMethodImplementation) other;
                    return targetMethod.equals(that.targetMethod)
                            && specializedLambdaMethod.equals(that.specializedLambdaMethod);
                }

                @Override
                public int hashCode() {
                    int result = targetMethod.hashCode();
                    result = 31 * result + specializedLambdaMethod.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.LambdaMethodImplementation{" +
                            "targetMethod=" + targetMethod +
                            ", specializedLambdaMethod=" + specializedLambdaMethod +
                            '}';
                }

                /**
                 * An appender for a lambda expression's functional method.
                 */
                protected static class Appender implements ByteCodeAppender {

                    /**
                     * The target method of the lambda expression.
                     */
                    private final MethodDescription targetMethod;

                    /**
                     * The specialized type of the lambda method.
                     */
                    private final JavaConstant.MethodType specializedLambdaMethod;

                    /**
                     * The instrumented type's declared fields.
                     */
                    private final List<FieldDescription.InDefinedShape> declaredFields;

                    /**
                     * Creates an appender of a lambda expression's functional method.
                     *
                     * @param targetMethod            The target method of the lambda expression.
                     * @param specializedLambdaMethod The specialized type of the lambda method.
                     * @param declaredFields          The instrumented type's declared fields.
                     */
                    protected Appender(MethodDescription targetMethod,
                                       JavaConstant.MethodType specializedLambdaMethod,
                                       List<FieldDescription.InDefinedShape> declaredFields) {
                        this.targetMethod = targetMethod;
                        this.specializedLambdaMethod = specializedLambdaMethod;
                        this.declaredFields = declaredFields;
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                        List<StackManipulation> fieldAccess = new ArrayList<StackManipulation>(declaredFields.size() * 2);
                        for (FieldDescription.InDefinedShape fieldDescription : declaredFields) {
                            fieldAccess.add(MethodVariableAccess.REFERENCE.loadOffset(0));
                            fieldAccess.add(FieldAccess.forField(fieldDescription).getter());
                        }
                        List<StackManipulation> parameterAccess = new ArrayList<StackManipulation>(instrumentedMethod.getParameters().size() * 2);
                        for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                            parameterAccess.add(MethodVariableAccess.of(parameterDescription.getType()).loadOffset(parameterDescription.getOffset()));
                            parameterAccess.add(Assigner.DEFAULT.assign(parameterDescription.getType(),
                                    specializedLambdaMethod.getParameterTypes().get(parameterDescription.getIndex()).asGenericType(),
                                    Assigner.Typing.DYNAMIC));
                        }
                        return new Size(new StackManipulation.Compound(
                                new StackManipulation.Compound(fieldAccess),
                                new StackManipulation.Compound(parameterAccess),
                                MethodInvocation.invoke(targetMethod),
                                MethodReturn.returning(targetMethod.getReturnType().asErasure())
                        ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Appender appender = (Appender) other;
                        return targetMethod.equals(appender.targetMethod)
                                && declaredFields.equals(appender.declaredFields)
                                && specializedLambdaMethod.equals(appender.specializedLambdaMethod);
                    }

                    @Override
                    public int hashCode() {
                        int result = targetMethod.hashCode();
                        result = 31 * result + declaredFields.hashCode();
                        result = 31 * result + specializedLambdaMethod.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.LambdaMethodImplementation.Appender{" +
                                "targetMethod=" + targetMethod +
                                ", specializedLambdaMethod=" + specializedLambdaMethod +
                                ", declaredFields=" + declaredFields +
                                '}';
                    }
                }
            }

            /**
             * Implements the {@code writeReplace} method for serializable lambda expressions.
             */
            protected static class SerializationImplementation implements Implementation {

                /**
                 * The lambda expression's declaring type.
                 */
                private final TypeDescription targetType;

                /**
                 * The lambda expression's functional type.
                 */
                private final TypeDescription lambdaType;

                /**
                 * The lambda expression's functional method name.
                 */
                private final String lambdaMethodName;

                /**
                 * The method type of the lambda expression's functional method.
                 */
                private final JavaConstant.MethodType lambdaMethod;

                /**
                 * A handle that references the lambda expressions invocation target.
                 */
                private final JavaConstant.MethodHandle targetMethod;

                /**
                 * The specialized method type of the lambda expression's functional method.
                 */
                private final JavaConstant.MethodType specializedMethod;

                /**
                 * Creates a new implementation for a serializable's lambda expression's {@code writeReplace} method.
                 *
                 * @param targetType        The lambda expression's declaring type.
                 * @param lambdaType        The lambda expression's functional type.
                 * @param lambdaMethodName  The lambda expression's functional method name.
                 * @param lambdaMethod      The method type of the lambda expression's functional method.
                 * @param targetMethod      A handle that references the lambda expressions invocation target.
                 * @param specializedMethod The specialized method type of the lambda expression's functional method.
                 */
                protected SerializationImplementation(TypeDescription targetType,
                                                      TypeDescription lambdaType,
                                                      String lambdaMethodName,
                                                      JavaConstant.MethodType lambdaMethod,
                                                      JavaConstant.MethodHandle targetMethod,
                                                      JavaConstant.MethodType specializedMethod) {
                    this.targetType = targetType;
                    this.lambdaType = lambdaType;
                    this.lambdaMethodName = lambdaMethodName;
                    this.lambdaMethod = lambdaMethod;
                    this.targetMethod = targetMethod;
                    this.specializedMethod = specializedMethod;
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    TypeDescription serializedLambda;
                    try {
                        serializedLambda = new TypeDescription.ForLoadedType(Class.forName("java.lang.invoke.SerializedLambda"));
                    } catch (ClassNotFoundException exception) {
                        throw new IllegalStateException("Cannot find class for lambda serialization", exception);
                    }
                    List<StackManipulation> lambdaArguments = new ArrayList<StackManipulation>(implementationTarget.getInstrumentedType().getDeclaredFields().size());
                    for (FieldDescription.InDefinedShape fieldDescription : implementationTarget.getInstrumentedType().getDeclaredFields()) {
                        lambdaArguments.add(new StackManipulation.Compound(MethodVariableAccess.REFERENCE.loadOffset(0),
                                FieldAccess.forField(fieldDescription).getter(),
                                Assigner.DEFAULT.assign(fieldDescription.getType(), TypeDescription.Generic.OBJECT, Assigner.Typing.STATIC)));
                    }
                    return new ByteCodeAppender.Simple(new StackManipulation.Compound(
                            TypeCreation.of(serializedLambda),
                            Duplication.SINGLE,
                            ClassConstant.of(targetType),
                            new TextConstant(lambdaType.getInternalName()),
                            new TextConstant(lambdaMethodName),
                            new TextConstant(lambdaMethod.getDescriptor()),
                            IntegerConstant.forValue(targetMethod.getHandleType().getIdentifier()),
                            new TextConstant(targetMethod.getOwnerType().getInternalName()),
                            new TextConstant(targetMethod.getName()),
                            new TextConstant(targetMethod.getDescriptor()),
                            new TextConstant(specializedMethod.getDescriptor()),
                            ArrayFactory.forType(TypeDescription.Generic.OBJECT).withValues(lambdaArguments),
                            MethodInvocation.invoke(serializedLambda.getDeclaredMethods().filter(isConstructor()).getOnly()),
                            MethodReturn.REFERENCE
                    ));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    SerializationImplementation that = (SerializationImplementation) other;
                    return targetType.equals(that.targetType)
                            && lambdaType.equals(that.lambdaType)
                            && lambdaMethodName.equals(that.lambdaMethodName)
                            && lambdaMethod.equals(that.lambdaMethod)
                            && targetMethod.equals(that.targetMethod)
                            && specializedMethod.equals(that.specializedMethod);
                }

                @Override
                public int hashCode() {
                    int result = targetType.hashCode();
                    result = 31 * result + lambdaType.hashCode();
                    result = 31 * result + lambdaMethodName.hashCode();
                    result = 31 * result + lambdaMethod.hashCode();
                    result = 31 * result + targetMethod.hashCode();
                    result = 31 * result + specializedMethod.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.SerializationImplementation{" +
                            "targetType=" + targetType +
                            ", lambdaType=" + lambdaType +
                            ", lambdaMethodName='" + lambdaMethodName + '\'' +
                            ", lambdaMethod=" + lambdaMethod +
                            ", targetMethod=" + targetMethod +
                            ", specializedMethod=" + specializedMethod +
                            '}';
                }
            }

            /**
             * Implements an explicit bridge method for a lambda expression.
             */
            protected static class BridgeMethodImplementation implements Implementation {

                /**
                 * The name of the lambda expression's functional method.
                 */
                private final String lambdaMethodName;

                /**
                 * The actual type of the lambda expression's functional method.
                 */
                private final JavaConstant.MethodType lambdaMethod;

                /**
                 * Creates a new bridge method implementation for a lambda expression.
                 *
                 * @param lambdaMethodName The name of the lambda expression's functional method.
                 * @param lambdaMethod     The actual type of the lambda expression's functional method.
                 */
                protected BridgeMethodImplementation(String lambdaMethodName, JavaConstant.MethodType lambdaMethod) {
                    this.lambdaMethodName = lambdaMethodName;
                    this.lambdaMethod = lambdaMethod;
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(implementationTarget.invokeSuper(new MethodDescription.SignatureToken(lambdaMethodName,
                            lambdaMethod.getReturnType(),
                            lambdaMethod.getParameterTypes())));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    BridgeMethodImplementation that = (BridgeMethodImplementation) other;
                    return lambdaMethodName.equals(that.lambdaMethodName) && lambdaMethod.equals(that.lambdaMethod);
                }

                @Override
                public int hashCode() {
                    int result = lambdaMethodName.hashCode();
                    result = 31 * result + lambdaMethod.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.BridgeMethodImplementation{" +
                            "lambdaMethodName='" + lambdaMethodName + '\'' +
                            ", lambdaMethod=" + lambdaMethod +
                            '}';
                }

                /**
                 * An appender for implementing a bridge method for a lambda expression.
                 */
                protected static class Appender implements ByteCodeAppender {

                    /**
                     * The invocation of the bridge's target method.
                     */
                    private final SpecialMethodInvocation bridgeTargetInvocation;

                    /**
                     * Creates a new appender for invoking a lambda expression's bridge method target.
                     *
                     * @param bridgeTargetInvocation The invocation of the bridge's target method.
                     */
                    protected Appender(SpecialMethodInvocation bridgeTargetInvocation) {
                        this.bridgeTargetInvocation = bridgeTargetInvocation;
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                        return new Compound(new Simple(
                                MethodVariableAccess.allArgumentsOf(instrumentedMethod)
                                        .asBridgeOf(bridgeTargetInvocation.getMethodDescription())
                                        .prependThisReference(),
                                bridgeTargetInvocation,
                                bridgeTargetInvocation.getMethodDescription().getReturnType().asErasure().isAssignableTo(instrumentedMethod.getReturnType().asErasure())
                                        ? StackManipulation.Trivial.INSTANCE
                                        : TypeCasting.to(instrumentedMethod.getReceiverType().asErasure()),
                                MethodReturn.returning(instrumentedMethod.getReturnType().asErasure())

                        )).apply(methodVisitor, implementationContext, instrumentedMethod);
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && bridgeTargetInvocation.equals(((Appender) other).bridgeTargetInvocation);
                    }

                    @Override
                    public int hashCode() {
                        return bridgeTargetInvocation.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.LambdaInstrumentationStrategy.LambdaInstanceFactory.BridgeMethodImplementation.Appender{" +
                                "bridgeTargetInvocation=" + bridgeTargetInvocation +
                                '}';
                    }
                }
            }
        }

        /**
         * Implements the regular lambda meta factory. The implementation represents the following code:
         * <blockquote><pre>
         * public static CallSite metafactory(MethodHandles.Lookup caller,
         *     String invokedName,
         *     MethodType invokedType,
         *     MethodType samMethodType,
         *     MethodHandle implMethod,
         *     MethodType instantiatedMethodType) throws Exception {
         *   Unsafe unsafe = Unsafe.getUnsafe();
         *   {@code Class<?>} lambdaClass = unsafe.defineAnonymousClass(caller.lookupClass(),
         *       (byte[]) ClassLoader.getSystemClassLoader().loadClass("net.bytebuddy.agent.builder.LambdaFactory").getDeclaredMethod("make",
         *           Object.class,
         *           String.class,
         *           Object.class,
         *           Object.class,
         *           Object.class,
         *           Object.class,
         *           boolean.class,
         *           List.class,
         *           List.class).invoke(null,
         *               caller,
         *               invokedName,
         *               invokedType,
         *               samMethodType,
         *               implMethod,
         *               instantiatedMethodType,
         *               false,
         *               Collections.emptyList(),
         *               Collections.emptyList()),
         *       null);
         *   unsafe.ensureClassInitialized(lambdaClass);
         *   return invokedType.parameterCount() == 0
         *     ? new ConstantCallSite(MethodHandles.constant(invokedType.returnType(), lambdaClass.getDeclaredConstructors()[0].newInstance()))
         *     : new ConstantCallSite(MethodHandles.Lookup.IMPL_LOOKUP.findStatic(lambdaClass, "get$Lambda", invokedType));
         * </pre></blockquote>
         */
        protected enum MetaFactoryRedirection implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodVisitor wrap(TypeDescription instrumentedType,
                                      MethodDescription.InDefinedShape methodDescription,
                                      MethodVisitor methodVisitor,
                                      ClassFileVersion classFileVersion,
                                      int writerFlags,
                                      int readerFlags) {
                methodVisitor.visitCode();
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "sun/misc/Unsafe", "getUnsafe", "()Lsun/misc/Unsafe;", false);
                methodVisitor.visitVarInsn(Opcodes.ASTORE, 6);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 6);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "lookupClass", "()Ljava/lang/Class;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
                methodVisitor.visitLdcInsn("net.bytebuddy.agent.builder.LambdaFactory");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                methodVisitor.visitLdcInsn("make");
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 9);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/String;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_2);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_3);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_4);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_5);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 6);
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 7);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/util/List;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 8);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/util/List;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 9);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_2);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_3);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_4);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 4);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_5);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 5);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 6);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 7);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 8);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, "[B");
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "sun/misc/Unsafe", "defineAnonymousClass", "(Ljava/lang/Class;[B[Ljava/lang/Object;)Ljava/lang/Class;", false);
                methodVisitor.visitVarInsn(Opcodes.ASTORE, 7);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 6);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 7);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "sun/misc/Unsafe", "ensureClassInitialized", "(Ljava/lang/Class;)V", false);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodType", "parameterCount", "()I", false);
                Label conditionalDefault = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFNE, conditionalDefault);
                methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodType", "returnType", "()Ljava/lang/Class;", false);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 7);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructors", "()[Ljava/lang/reflect/Constructor;", false);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitInsn(Opcodes.AALOAD);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
                Label conditionalAlternative = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, conditionalAlternative);
                methodVisitor.visitLabel(conditionalDefault);
                methodVisitor.visitFrame(Opcodes.F_APPEND, 2, new Object[]{"sun/misc/Unsafe", "java/lang/Class"}, 0, null);
                methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/invoke/MethodHandles$Lookup", "IMPL_LOOKUP", "Ljava/lang/invoke/MethodHandles$Lookup;");
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 7);
                methodVisitor.visitLdcInsn("get$Lambda");
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
                methodVisitor.visitLabel(conditionalAlternative);
                methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/invoke/CallSite"});
                methodVisitor.visitInsn(Opcodes.ARETURN);
                methodVisitor.visitMaxs(8, 8);
                methodVisitor.visitEnd();
                return IGNORE_ORIGINAL;
            }

            @Override
            public String toString() {
                return "AgentBuilder.LambdaInstrumentationStrategy.MetaFactoryRedirection." + name();
            }
        }

        /**
         * Implements the alternative lambda meta factory. The implementation represents the following code:
         * <blockquote><pre>
         * public static CallSite altMetafactory(MethodHandles.Lookup caller,
         *     String invokedName,
         *     MethodType invokedType,
         *     Object... args) throws Exception {
         *   int flags = (Integer) args[3];
         *   int argIndex = 4;
         *   {@code Class<?>[]} markerInterface;
         *   if ((flags {@code &} FLAG_MARKERS) != 0) {
         *     int markerCount = (Integer) args[argIndex++];
         *     markerInterface = new {@code Class<?>}[markerCount];
         *     System.arraycopy(args, argIndex, markerInterface, 0, markerCount);
         *     argIndex += markerCount;
         *   } else {
         *     markerInterface = new {@code Class<?>}[0];
         *   }
         *   MethodType[] additionalBridge;
         *   if ((flags {@code &} FLAG_BRIDGES) != 0) {
         *     int bridgeCount = (Integer) args[argIndex++];
         *     additionalBridge = new MethodType[bridgeCount];
         *     System.arraycopy(args, argIndex, additionalBridge, 0, bridgeCount);
         *     // argIndex += bridgeCount;
         *   } else {
         *     additionalBridge = new MethodType[0];
         *   }
         *   Unsafe unsafe = Unsafe.getUnsafe();
         *   {@code Class<?>} lambdaClass = unsafe.defineAnonymousClass(caller.lookupClass(),
         *       (byte[]) ClassLoader.getSystemClassLoader().loadClass("net.bytebuddy.agent.builder.LambdaFactory").getDeclaredMethod("make",
         *           Object.class,
         *           String.class,
         *           Object.class,
         *           Object.class,
         *           Object.class,
         *           Object.class,
         *           boolean.class,
         *           List.class,
         *           List.class).invoke(null,
         *               caller,
         *               invokedName,
         *               invokedType,
         *               args[0],
         *               args[1],
         *               args[2],
         *               (flags {@code &} FLAG_SERIALIZABLE) != 0,
         *               Arrays.asList(markerInterface),
         *               Arrays.asList(additionalBridge)),
         *       null);
         *   unsafe.ensureClassInitialized(lambdaClass);
         *   return invokedType.parameterCount() == 0
         *     ? new ConstantCallSite(MethodHandles.constant(invokedType.returnType(), lambdaClass.getDeclaredConstructors()[0].newInstance()))
         *     : new ConstantCallSite(MethodHandles.Lookup.IMPL_LOOKUP.findStatic(lambdaClass, "get$Lambda", invokedType));
         * }
         * </pre></blockquote>
         */
        protected enum AlternativeMetaFactoryRedirection implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodVisitor wrap(TypeDescription instrumentedType,
                                      MethodDescription.InDefinedShape methodDescription,
                                      MethodVisitor methodVisitor,
                                      ClassFileVersion classFileVersion,
                                      int writerFlags,
                                      int readerFlags) {
                methodVisitor.visitCode();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitInsn(Opcodes.ICONST_3);
                methodVisitor.visitInsn(Opcodes.AALOAD);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                methodVisitor.visitVarInsn(Opcodes.ISTORE, 4);
                methodVisitor.visitInsn(Opcodes.ICONST_4);
                methodVisitor.visitVarInsn(Opcodes.ISTORE, 5);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 4);
                methodVisitor.visitInsn(Opcodes.ICONST_2);
                methodVisitor.visitInsn(Opcodes.IAND);
                Label markerInterfaceLoop = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, markerInterfaceLoop);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 5);
                methodVisitor.visitIincInsn(5, 1);
                methodVisitor.visitInsn(Opcodes.AALOAD);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                methodVisitor.visitVarInsn(Opcodes.ISTORE, 7);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 7);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
                methodVisitor.visitVarInsn(Opcodes.ASTORE, 6);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 5);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 6);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 7);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 5);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 7);
                methodVisitor.visitInsn(Opcodes.IADD);
                methodVisitor.visitVarInsn(Opcodes.ISTORE, 5);
                Label markerInterfaceExit = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, markerInterfaceExit);
                methodVisitor.visitLabel(markerInterfaceLoop);
                methodVisitor.visitFrame(Opcodes.F_APPEND, 2, new Object[]{Opcodes.INTEGER, Opcodes.INTEGER}, 0, null);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
                methodVisitor.visitVarInsn(Opcodes.ASTORE, 6);
                methodVisitor.visitLabel(markerInterfaceExit);
                methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"[Ljava/lang/Class;"}, 0, null);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 4);
                methodVisitor.visitInsn(Opcodes.ICONST_4);
                methodVisitor.visitInsn(Opcodes.IAND);
                Label additionalBridgesLoop = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, additionalBridgesLoop);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 5);
                methodVisitor.visitIincInsn(5, 1);
                methodVisitor.visitInsn(Opcodes.AALOAD);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                methodVisitor.visitVarInsn(Opcodes.ISTORE, 8);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 8);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodType");
                methodVisitor.visitVarInsn(Opcodes.ASTORE, 7);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 5);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 7);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 8);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
                Label additionalBridgesExit = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, additionalBridgesExit);
                methodVisitor.visitLabel(additionalBridgesLoop);
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodType");
                methodVisitor.visitVarInsn(Opcodes.ASTORE, 7);
                methodVisitor.visitLabel(additionalBridgesExit);
                methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"[Ljava/lang/invoke/MethodType;"}, 0, null);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "sun/misc/Unsafe", "getUnsafe", "()Lsun/misc/Unsafe;", false);
                methodVisitor.visitVarInsn(Opcodes.ASTORE, 8);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 8);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "lookupClass", "()Ljava/lang/Class;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
                methodVisitor.visitLdcInsn("net.bytebuddy.agent.builder.LambdaFactory");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                methodVisitor.visitLdcInsn("make");
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 9);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/String;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_2);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_3);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_4);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_5);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 6);
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 7);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/util/List;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 8);
                methodVisitor.visitLdcInsn(Type.getType("Ljava/util/List;"));
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 9);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_2);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_3);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitInsn(Opcodes.AALOAD);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_4);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                methodVisitor.visitInsn(Opcodes.AALOAD);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ICONST_5);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
                methodVisitor.visitInsn(Opcodes.ICONST_2);
                methodVisitor.visitInsn(Opcodes.AALOAD);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 6);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 4);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                methodVisitor.visitInsn(Opcodes.IAND);
                Label callSiteConditional = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, callSiteConditional);
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                Label callSiteAlternative = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, callSiteAlternative);
                methodVisitor.visitLabel(callSiteConditional);
                methodVisitor.visitFrame(Opcodes.F_FULL, 9, new Object[]{"java/lang/invoke/MethodHandles$Lookup", "java/lang/String", "java/lang/invoke/MethodType", "[Ljava/lang/Object;", Opcodes.INTEGER, Opcodes.INTEGER, "[Ljava/lang/Class;", "[Ljava/lang/invoke/MethodType;", "sun/misc/Unsafe"}, 7, new Object[]{"sun/misc/Unsafe", "java/lang/Class", "java/lang/reflect/Method", Opcodes.NULL, "[Ljava/lang/Object;", "[Ljava/lang/Object;", Opcodes.INTEGER});
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitLabel(callSiteAlternative);
                methodVisitor.visitFrame(Opcodes.F_FULL, 9, new Object[]{"java/lang/invoke/MethodHandles$Lookup", "java/lang/String", "java/lang/invoke/MethodType", "[Ljava/lang/Object;", Opcodes.INTEGER, Opcodes.INTEGER, "[Ljava/lang/Class;", "[Ljava/lang/invoke/MethodType;", "sun/misc/Unsafe"}, 8, new Object[]{"sun/misc/Unsafe", "java/lang/Class", "java/lang/reflect/Method", Opcodes.NULL, "[Ljava/lang/Object;", "[Ljava/lang/Object;", Opcodes.INTEGER, Opcodes.INTEGER});
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 7);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 6);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitIntInsn(Opcodes.BIPUSH, 8);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 7);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, "[B");
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "sun/misc/Unsafe", "defineAnonymousClass", "(Ljava/lang/Class;[B[Ljava/lang/Object;)Ljava/lang/Class;", false);
                methodVisitor.visitVarInsn(Opcodes.ASTORE, 9);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 8);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 9);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "sun/misc/Unsafe", "ensureClassInitialized", "(Ljava/lang/Class;)V", false);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodType", "parameterCount", "()I", false);
                Label callSiteJump = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFNE, callSiteJump);
                methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodType", "returnType", "()Ljava/lang/Class;", false);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 9);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructors", "()[Ljava/lang/reflect/Constructor;", false);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitInsn(Opcodes.AALOAD);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
                Label callSiteExit = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, callSiteExit);
                methodVisitor.visitLabel(callSiteJump);
                methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Class"}, 0, null);
                methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/invoke/MethodHandles$Lookup", "IMPL_LOOKUP", "Ljava/lang/invoke/MethodHandles$Lookup;");
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 9);
                methodVisitor.visitLdcInsn("get$Lambda");
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
                methodVisitor.visitLabel(callSiteExit);
                methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/invoke/CallSite"});
                methodVisitor.visitInsn(Opcodes.ARETURN);
                methodVisitor.visitMaxs(9, 10);
                methodVisitor.visitEnd();
                return IGNORE_ORIGINAL;
            }

            @Override
            public String toString() {
                return "AgentBuilder.LambdaInstrumentationStrategy.AlternativeMetaFactoryRedirection." + name();
            }
        }
    }

    /**
     * <p>
     * The default implementation of an {@link net.bytebuddy.agent.builder.AgentBuilder}.
     * </p>
     * <p>
     * By default, Byte Buddy ignores any types loaded by the bootstrap class loader and
     * any synthetic type. Self-injection and rebasing is enabled. In order to avoid class format changes, set
     * {@link AgentBuilder#disableBootstrapInjection()}). All types are parsed without their debugging information ({@link PoolStrategy.Default#FAST}).
     * </p>
     */
    class Default implements AgentBuilder {

        /**
         * The name of the Byte Buddy {@code net.bytebuddy.agent.Installer} class.
         */
        private static final String INSTALLER_TYPE = "net.bytebuddy.agent.Installer";

        /**
         * The name of the {@code net.bytebuddy.agent.Installer} field containing an installed {@link Instrumentation}.
         */
        private static final String INSTRUMENTATION_FIELD = "instrumentation";

        /**
         * Indicator for access to a static member via reflection to make the code more readable.
         */
        private static final Object STATIC_FIELD = null;

        /**
         * The value that is to be returned from a {@link java.lang.instrument.ClassFileTransformer} to indicate
         * that no class file transformation is to be applied.
         */
        private static final byte[] NO_TRANSFORMATION = null;

        /**
         * Indicates that a loaded type should be considered as non-available.
         */
        private static final Class<?> NO_LOADED_TYPE = null;

        /**
         * The {@link net.bytebuddy.ByteBuddy} instance to be used.
         */
        private final ByteBuddy byteBuddy;

        /**
         * The listener to notify on transformations.
         */
        private final Listener listener;

        /**
         * The type locator to use.
         */
        private final PoolStrategy poolStrategy;

        /**
         * The definition handler to use.
         */
        private final TypeStrategy typeStrategy;

        /**
         * The location strategy to use.
         */
        private final LocationStrategy locationStrategy;

        /**
         * The native method strategy to use.
         */
        private final NativeMethodStrategy nativeMethodStrategy;

        /**
         * The initialization strategy to use for creating classes.
         */
        private final InitializationStrategy initializationStrategy;

        /**
         * The redefinition strategy to apply.
         */
        private final RedefinitionStrategy redefinitionStrategy;

        /**
         * The injection strategy for injecting classes into the bootstrap class loader.
         */
        private final BootstrapInjectionStrategy bootstrapInjectionStrategy;

        /**
         * A strategy to determine of the {@code LambdaMetafactory} should be instrumented to allow for the instrumentation
         * of classes that represent lambda expressions.
         */
        private final LambdaInstrumentationStrategy lambdaInstrumentationStrategy;

        /**
         * The description strategy for resolving type descriptions for types.
         */
        private final DescriptionStrategy descriptionStrategy;

        /**
         * The installation strategy to use.
         */
        private final InstallationStrategy installationStrategy;

        /**
         * The fallback strategy to apply.
         */
        private final FallbackStrategy fallbackStrategy;

        /**
         * Identifies types that should not be instrumented.
         */
        private final RawMatcher ignoredTypeMatcher;

        /**
         * The transformation object for handling type transformations.
         */
        private final Transformation transformation;

        /**
         * Creates a new default agent builder that uses a default {@link net.bytebuddy.ByteBuddy} instance for creating classes.
         *
         * @see AgentBuilder.Default#Default(ByteBuddy)
         */
        public Default() {
            this(new ByteBuddy());
        }

        /**
         * Creates a new agent builder with default settings. By default, Byte Buddy ignores any types loaded by the bootstrap class loader, any
         * type within a {@code net.bytebuddy} package and any synthetic type. Self-injection and rebasing is enabled. In order to avoid class format
         * changes, set {@link AgentBuilder#disableBootstrapInjection()}). All types are parsed without their debugging information
         * ({@link PoolStrategy.Default#FAST}).
         *
         * @param byteBuddy The Byte Buddy instance to be used.
         */
        public Default(ByteBuddy byteBuddy) {
            this(byteBuddy,
                    Listener.NoOp.INSTANCE,
                    PoolStrategy.Default.FAST,
                    TypeStrategy.Default.REBASE,
                    LocationStrategy.ForClassLoader.STRONG,
                    NativeMethodStrategy.Disabled.INSTANCE,
                    InitializationStrategy.SelfInjection.SPLIT,
                    RedefinitionStrategy.DISABLED,
                    BootstrapInjectionStrategy.Disabled.INSTANCE,
                    LambdaInstrumentationStrategy.DISABLED,
                    DescriptionStrategy.Default.HYBRID,
                    InstallationStrategy.Default.ESCALATING,
                    FallbackStrategy.ByThrowableType.ofOptionalTypes(),
                    new RawMatcher.Disjunction(new RawMatcher.ForElementMatchers(any(), isBootstrapClassLoader(), any()),
                            new RawMatcher.ForElementMatchers(nameStartsWith("net.bytebuddy.").or(nameStartsWith("sun.reflect.")).<TypeDescription>or(isSynthetic()), any(), any())),
                    Transformation.Ignored.INSTANCE);
        }

        /**
         * Creates a new default agent builder.
         *
         * @param byteBuddy                     The Byte Buddy instance to be used.
         * @param listener                      The listener to notify on transformations.
         * @param poolStrategy                  The type locator to use.
         * @param typeStrategy                  The definition handler to use.
         * @param locationStrategy              The location strategy to use.
         * @param nativeMethodStrategy          The native method strategy to apply.
         * @param initializationStrategy        The initialization strategy to use for transformed types.
         * @param redefinitionStrategy          The redefinition strategy to apply.
         * @param bootstrapInjectionStrategy    The injection strategy for injecting classes into the bootstrap class loader.
         * @param lambdaInstrumentationStrategy A strategy to determine of the {@code LambdaMetafactory} should be instrumented to allow for the
         *                                      instrumentation of classes that represent lambda expressions.
         * @param descriptionStrategy           The description strategy for resolving type descriptions for types.
         * @param installationStrategy          The installation strategy to use.
         * @param fallbackStrategy              The fallback strategy to apply.
         * @param ignoredTypeMatcher            Identifies types that should not be instrumented.
         * @param transformation                The transformation object for handling type transformations.
         */
        protected Default(ByteBuddy byteBuddy,
                          Listener listener,
                          PoolStrategy poolStrategy,
                          TypeStrategy typeStrategy,
                          LocationStrategy locationStrategy,
                          NativeMethodStrategy nativeMethodStrategy,
                          InitializationStrategy initializationStrategy,
                          RedefinitionStrategy redefinitionStrategy,
                          BootstrapInjectionStrategy bootstrapInjectionStrategy,
                          LambdaInstrumentationStrategy lambdaInstrumentationStrategy,
                          DescriptionStrategy descriptionStrategy,
                          InstallationStrategy installationStrategy,
                          FallbackStrategy fallbackStrategy,
                          RawMatcher ignoredTypeMatcher,
                          Transformation transformation) {
            this.byteBuddy = byteBuddy;
            this.poolStrategy = poolStrategy;
            this.typeStrategy = typeStrategy;
            this.locationStrategy = locationStrategy;
            this.listener = listener;
            this.nativeMethodStrategy = nativeMethodStrategy;
            this.initializationStrategy = initializationStrategy;
            this.redefinitionStrategy = redefinitionStrategy;
            this.bootstrapInjectionStrategy = bootstrapInjectionStrategy;
            this.lambdaInstrumentationStrategy = lambdaInstrumentationStrategy;
            this.descriptionStrategy = descriptionStrategy;
            this.installationStrategy = installationStrategy;
            this.fallbackStrategy = fallbackStrategy;
            this.ignoredTypeMatcher = ignoredTypeMatcher;
            this.transformation = transformation;
        }

        @Override
        public AgentBuilder with(ByteBuddy byteBuddy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(Listener listener) {
            return new Default(byteBuddy,
                    new Listener.Compound(this.listener, listener),
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(TypeStrategy typeStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(PoolStrategy poolStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(LocationStrategy locationStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder enableNativeMethodPrefix(String prefix) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    NativeMethodStrategy.ForPrefix.of(prefix),
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder disableNativeMethodPrefix() {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    NativeMethodStrategy.Disabled.INSTANCE,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(RedefinitionStrategy redefinitionStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(InitializationStrategy initializationStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(LambdaInstrumentationStrategy lambdaInstrumentationStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(DescriptionStrategy descriptionStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(InstallationStrategy installationStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder with(FallbackStrategy fallbackStrategy) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder enableBootstrapInjection(Instrumentation instrumentation, File folder) {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    new BootstrapInjectionStrategy.Enabled(folder, instrumentation),
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder disableBootstrapInjection() {
            return new Default(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    redefinitionStrategy,
                    BootstrapInjectionStrategy.Disabled.INSTANCE,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder disableClassFormatChanges() {
            return new Default(byteBuddy.with(Implementation.Context.Disabled.Factory.INSTANCE),
                    listener,
                    poolStrategy,
                    TypeStrategy.Default.REDEFINE_DECLARED_ONLY,
                    locationStrategy,
                    NativeMethodStrategy.Disabled.INSTANCE,
                    InitializationStrategy.NoOp.INSTANCE,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    installationStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Class<?>... type) {
            return JavaModule.isSupported()
                    ? with(Listener.ModuleReadEdgeCompleting.of(instrumentation, false, type))
                    : this;
        }

        @Override
        public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, JavaModule... module) {
            return assureReadEdgeTo(instrumentation, Arrays.asList(module));
        }

        @Override
        public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules) {
            return with(new Listener.ModuleReadEdgeCompleting(instrumentation, false, new HashSet<JavaModule>(modules)));
        }

        @Override
        public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Class<?>... type) {
            return JavaModule.isSupported()
                    ? with(Listener.ModuleReadEdgeCompleting.of(instrumentation, true, type))
                    : this;
        }

        @Override
        public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, JavaModule... module) {
            return assureReadEdgeFromAndTo(instrumentation, Arrays.asList(module));
        }

        @Override
        public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules) {
            return with(new Listener.ModuleReadEdgeCompleting(instrumentation, true, new HashSet<JavaModule>(modules)));
        }

        @Override
        public Identified.Narrowable type(RawMatcher matcher) {
            return new Transforming(matcher, Transformer.NoOp.INSTANCE, false);
        }

        @Override
        public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher) {
            return type(typeMatcher, any());
        }

        @Override
        public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
            return type(typeMatcher, classLoaderMatcher, any());
        }

        @Override
        public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher,
                                          ElementMatcher<? super ClassLoader> classLoaderMatcher,
                                          ElementMatcher<? super JavaModule> moduleMatcher) {
            return type(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, not(supportsModules()).or(moduleMatcher)));
        }

        @Override
        public Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher) {
            return ignore(typeMatcher, any());
        }

        @Override
        public Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
            return ignore(typeMatcher, classLoaderMatcher, any());
        }

        @Override
        public Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher,
                              ElementMatcher<? super ClassLoader> classLoaderMatcher,
                              ElementMatcher<? super JavaModule> moduleMatcher) {
            return ignore(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, not(supportsModules()).or(moduleMatcher)));
        }

        @Override
        public Ignored ignore(RawMatcher rawMatcher) {
            return new Ignoring(rawMatcher);
        }

        @Override
        public ClassFileTransformer makeRaw() {
            return ExecutingTransformer.FACTORY.make(byteBuddy,
                    listener,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    initializationStrategy,
                    bootstrapInjectionStrategy,
                    descriptionStrategy,
                    fallbackStrategy,
                    ignoredTypeMatcher,
                    transformation);
        }

        @Override
        public ClassFileTransformer installOn(Instrumentation instrumentation) {
            ClassFileTransformer classFileTransformer = makeRaw();
            instrumentation.addTransformer(classFileTransformer, redefinitionStrategy.isRetransforming(instrumentation));
            try {
                if (nativeMethodStrategy.isEnabled(instrumentation)) {
                    instrumentation.setNativeMethodPrefix(classFileTransformer, nativeMethodStrategy.getPrefix());
                }
                lambdaInstrumentationStrategy.apply(byteBuddy, instrumentation, classFileTransformer);
                if (redefinitionStrategy.isEnabled()) {
                    RedefinitionStrategy.Collector collector = redefinitionStrategy.makeCollector(transformation);
                    for (Class<?> type : instrumentation.getAllLoadedClasses()) {
                        JavaModule module = JavaModule.ofType(type);
                        try {
                            TypePool typePool = poolStrategy.typePool(locationStrategy.classFileLocator(type.getClassLoader(), module), type.getClassLoader());
                            try {
                                doConsider(descriptionStrategy.apply(TypeDescription.ForLoadedType.getName(type), type, typePool), type, type, module, collector, !instrumentation.isModifiableClass(type));
                            } catch (Throwable throwable) {
                                if (descriptionStrategy.isLoadedFirst() && fallbackStrategy.isFallback(type, throwable)) {
                                    doConsider(typePool.describe(TypeDescription.ForLoadedType.getName(type)).resolve(), type, NO_LOADED_TYPE, module, collector, false);
                                } else {
                                    throw throwable;
                                }
                            }
                        } catch (Throwable throwable) {
                            try {
                                try {
                                    listener.onError(TypeDescription.ForLoadedType.getName(type), type.getClassLoader(), module, throwable);
                                } finally {
                                    listener.onComplete(TypeDescription.ForLoadedType.getName(type), type.getClassLoader(), module);
                                }
                            } catch (Throwable ignored) {
                                // Ignore exceptions that are thrown by listeners to mimic the behavior of a transformation.
                            }
                        }
                    }
                    collector.apply(instrumentation, poolStrategy, locationStrategy, listener);
                }
                return classFileTransformer;
            } catch (Throwable throwable) {
                return installationStrategy.onError(instrumentation, classFileTransformer, throwable);
            }
        }

        /**
         * Does consider the retransformation or redefinition of a loaded type.
         *
         * @param typeDescription     The type description of the type being considered.
         * @param type                The loaded type being considered.
         * @param classBeingRedefined The loaded type being considered or {@code null} if it should be considered non-available.
         * @param module              The type's Java module or {@code null} if the current VM does not support modules.
         * @param collector           The collector to apply.
         * @param unmodifiable        {@code true} if the current type should be considered unmodifiable.
         */
        private void doConsider(TypeDescription typeDescription,
                                Class<?> type,
                                Class<?> classBeingRedefined,
                                JavaModule module,
                                RedefinitionStrategy.Collector collector,
                                boolean unmodifiable) {
            if (unmodifiable || !collector.consider(typeDescription, type, classBeingRedefined, ignoredTypeMatcher)) {
                try {
                    try {
                        listener.onIgnored(typeDescription, type.getClassLoader(), module);
                    } finally {
                        listener.onComplete(typeDescription.getName(), type.getClassLoader(), module);
                    }
                } catch (Throwable ignored) {
                    // Ignore exceptions that are thrown by listeners to mimic the behavior of a transformation.
                }
            }
        }

        @Override
        public ClassFileTransformer installOnByteBuddyAgent() {
            try {
                Instrumentation instrumentation = (Instrumentation) ClassLoader.getSystemClassLoader()
                        .loadClass(INSTALLER_TYPE)
                        .getDeclaredField(INSTRUMENTATION_FIELD)
                        .get(STATIC_FIELD);
                if (instrumentation == null) {
                    throw new IllegalStateException("The Byte Buddy agent is not installed");
                }
                return installOn(instrumentation);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Default aDefault = (Default) other;
            return byteBuddy.equals(aDefault.byteBuddy)
                    && listener.equals(aDefault.listener)
                    && poolStrategy.equals(aDefault.poolStrategy)
                    && nativeMethodStrategy.equals(aDefault.nativeMethodStrategy)
                    && typeStrategy.equals(aDefault.typeStrategy)
                    && locationStrategy.equals(aDefault.locationStrategy)
                    && initializationStrategy == aDefault.initializationStrategy
                    && redefinitionStrategy == aDefault.redefinitionStrategy
                    && bootstrapInjectionStrategy.equals(aDefault.bootstrapInjectionStrategy)
                    && lambdaInstrumentationStrategy.equals(aDefault.lambdaInstrumentationStrategy)
                    && descriptionStrategy.equals(aDefault.descriptionStrategy)
                    && installationStrategy.equals(aDefault.installationStrategy)
                    && fallbackStrategy.equals(aDefault.fallbackStrategy)
                    && ignoredTypeMatcher.equals(aDefault.ignoredTypeMatcher)
                    && transformation.equals(aDefault.transformation);
        }

        @Override
        public int hashCode() {
            int result = byteBuddy.hashCode();
            result = 31 * result + listener.hashCode();
            result = 31 * result + poolStrategy.hashCode();
            result = 31 * result + typeStrategy.hashCode();
            result = 31 * result + locationStrategy.hashCode();
            result = 31 * result + nativeMethodStrategy.hashCode();
            result = 31 * result + initializationStrategy.hashCode();
            result = 31 * result + redefinitionStrategy.hashCode();
            result = 31 * result + bootstrapInjectionStrategy.hashCode();
            result = 31 * result + lambdaInstrumentationStrategy.hashCode();
            result = 31 * result + descriptionStrategy.hashCode();
            result = 31 * result + installationStrategy.hashCode();
            result = 31 * result + fallbackStrategy.hashCode();
            result = 31 * result + ignoredTypeMatcher.hashCode();
            result = 31 * result + transformation.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AgentBuilder.Default{" +
                    "byteBuddy=" + byteBuddy +
                    ", listener=" + listener +
                    ", poolStrategy=" + poolStrategy +
                    ", typeStrategy=" + typeStrategy +
                    ", locationStrategy=" + locationStrategy +
                    ", nativeMethodStrategy=" + nativeMethodStrategy +
                    ", initializationStrategy=" + initializationStrategy +
                    ", redefinitionStrategy=" + redefinitionStrategy +
                    ", bootstrapInjectionStrategy=" + bootstrapInjectionStrategy +
                    ", lambdaInstrumentationStrategy=" + lambdaInstrumentationStrategy +
                    ", descriptionStrategy=" + descriptionStrategy +
                    ", installationStrategy=" + installationStrategy +
                    ", fallbackStrategy=" + fallbackStrategy +
                    ", ignoredTypeMatcher=" + ignoredTypeMatcher +
                    ", transformation=" + transformation +
                    '}';
        }

        /**
         * An injection strategy for injecting classes into the bootstrap class loader.
         */
        protected interface BootstrapInjectionStrategy {

            /**
             * Creates an injector for the bootstrap class loader.
             *
             * @param protectionDomain The protection domain to be used.
             * @return A class injector for the bootstrap class loader.
             */
            ClassInjector make(ProtectionDomain protectionDomain);

            /**
             * A disabled bootstrap injection strategy.
             */
            enum Disabled implements BootstrapInjectionStrategy {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public ClassInjector make(ProtectionDomain protectionDomain) {
                    throw new IllegalStateException("Injecting classes into the bootstrap class loader was not enabled");
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.BootstrapInjectionStrategy.Disabled." + name();
                }
            }

            /**
             * An enabled bootstrap injection strategy.
             */
            class Enabled implements BootstrapInjectionStrategy {

                /**
                 * The folder in which jar files are to be saved.
                 */
                private final File folder;

                /**
                 * The instrumentation to use for appending jar files.
                 */
                private final Instrumentation instrumentation;

                /**
                 * Creates a new enabled bootstrap class loader injection strategy.
                 *
                 * @param folder          The folder in which jar files are to be saved.
                 * @param instrumentation The instrumentation to use for appending jar files.
                 */
                public Enabled(File folder, Instrumentation instrumentation) {
                    this.folder = folder;
                    this.instrumentation = instrumentation;
                }

                @Override
                public ClassInjector make(ProtectionDomain protectionDomain) {
                    return ClassInjector.UsingInstrumentation.of(folder,
                            ClassInjector.UsingInstrumentation.Target.BOOTSTRAP,
                            instrumentation);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Enabled enabled = (Enabled) other;
                    return folder.equals(enabled.folder)
                            && instrumentation.equals(enabled.instrumentation);
                }

                @Override
                public int hashCode() {
                    int result = folder.hashCode();
                    result = 31 * result + instrumentation.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.BootstrapInjectionStrategy.Enabled{" +
                            "folder=" + folder +
                            ", instrumentation=" + instrumentation +
                            '}';
                }
            }
        }

        /**
         * A strategy for determining if a native method name prefix should be used when rebasing methods.
         */
        protected interface NativeMethodStrategy {

            /**
             * Determines if this strategy enables name prefixing for native methods.
             *
             * @param instrumentation The instrumentation used.
             * @return {@code true} if this strategy indicates that a native method prefix should be used.
             */
            boolean isEnabled(Instrumentation instrumentation);

            /**
             * Resolves the method name transformer for this strategy.
             *
             * @return A method name transformer for this strategy.
             */
            MethodNameTransformer resolve();

            /**
             * Returns the method prefix if the strategy is enabled. This method must only be called if this strategy enables prefixing.
             *
             * @return The method prefix.
             */
            String getPrefix();

            /**
             * A native method strategy that suffixes method names with a random suffix and disables native method rebasement.
             */
            enum Disabled implements NativeMethodStrategy {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public MethodNameTransformer resolve() {
                    return MethodNameTransformer.Suffixing.withRandomSuffix();
                }

                @Override
                public boolean isEnabled(Instrumentation instrumentation) {
                    return false;
                }

                @Override
                public String getPrefix() {
                    throw new IllegalStateException("A disabled native method strategy does not define a method name prefix");
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.NativeMethodStrategy.Disabled." + name();
                }
            }

            /**
             * A native method strategy that prefixes method names with a fixed value for supporting rebasing of native methods.
             */
            class ForPrefix implements NativeMethodStrategy {

                /**
                 * The method name prefix.
                 */
                private final String prefix;

                /**
                 * Creates a new name prefixing native method strategy.
                 *
                 * @param prefix The method name prefix.
                 */
                protected ForPrefix(String prefix) {
                    this.prefix = prefix;
                }

                /**
                 * Creates a new native method strategy for prefixing method names.
                 *
                 * @param prefix The method name prefix.
                 * @return An appropriate native method strategy.
                 */
                protected static NativeMethodStrategy of(String prefix) {
                    if (prefix.length() == 0) {
                        throw new IllegalArgumentException("A method name prefix must not be the empty string");
                    }
                    return new ForPrefix(prefix);
                }

                @Override
                public MethodNameTransformer resolve() {
                    return new MethodNameTransformer.Prefixing(prefix);
                }

                @Override
                public boolean isEnabled(Instrumentation instrumentation) {
                    if (!instrumentation.isNativeMethodPrefixSupported()) {
                        throw new IllegalArgumentException("A prefix for native methods is not supported: " + instrumentation);
                    }
                    return true;
                }

                @Override
                public String getPrefix() {
                    return prefix;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass()) && prefix.equals(((ForPrefix) other).prefix);
                }

                @Override
                public int hashCode() {
                    return prefix.hashCode();
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.NativeMethodStrategy.ForPrefix{" +
                            "prefix='" + prefix + '\'' +
                            '}';
                }
            }
        }

        /**
         * A transformation serves as a handler for modifying a class.
         */
        protected interface Transformation {

            /**
             * Checks if this transformation is alive.
             *
             * @param typeDescription     A description of the type that is to be transformed.
             * @param classLoader         The class loader of the type being transformed.
             * @param module              The transformed type's module or {@code null} if the current VM does not support modules.
             * @param classBeingRedefined In case of a type redefinition, the loaded type being transformed or {@code null} if that is not the case.
             * @param protectionDomain    The protection domain of the type being transformed.
             * @param ignoredTypeMatcher  Identifies types that should not be instrumented.
             * @return {@code true} if this transformation is alive.
             */
            boolean isAlive(TypeDescription typeDescription,
                            ClassLoader classLoader,
                            JavaModule module,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            RawMatcher ignoredTypeMatcher);

            /**
             * Resolves an attempted transformation to a specific transformation.
             *
             * @param typeDescription     A description of the type that is to be transformed.
             * @param classLoader         The class loader of the type being transformed.
             * @param module              The transformed type's module or {@code null} if the current VM does not support modules.
             * @param classBeingRedefined In case of a type redefinition, the loaded type being transformed or {@code null} if that is not the case.
             * @param protectionDomain    The protection domain of the type being transformed.
             * @param typePool            The type pool to apply during type creation.
             * @param ignoredTypeMatcher  Identifies types that should not be instrumented.
             * @return A resolution for the given type.
             */
            Resolution resolve(TypeDescription typeDescription,
                               ClassLoader classLoader,
                               JavaModule module,
                               Class<?> classBeingRedefined,
                               ProtectionDomain protectionDomain,
                               TypePool typePool,
                               RawMatcher ignoredTypeMatcher);

            /**
             * A resolution to a transformation.
             */
            interface Resolution {

                /**
                 * Returns the sort of this resolution.
                 *
                 * @return The sort of this resolution.
                 */
                Sort getSort();

                /**
                 * Resolves this resolution as a decorator of the supplied resolution.
                 *
                 * @param resolution The resolution for which this resolution should serve as a decorator.
                 * @return A resolution where this resolution is applied as a decorator if this resolution is alive.
                 */
                Resolution asDecoratorOf(Resolution resolution);

                /**
                 * Resolves this resolution as a decorator of the supplied resolution.
                 *
                 * @param resolution The resolution for which this resolution should serve as a decorator.
                 * @return A resolution where this resolution is applied as a decorator if this resolution is alive.
                 */
                Resolution prepend(Decoratable resolution);

                /**
                 * Transforms a type or returns {@code null} if a type is not to be transformed.
                 *
                 * @param initializationStrategy     The initialization strategy to use.
                 * @param classFileLocator           The class file locator to use.
                 * @param typeStrategy               The definition handler to use.
                 * @param byteBuddy                  The Byte Buddy instance to use.
                 * @param methodNameTransformer      The method name transformer to be used.
                 * @param bootstrapInjectionStrategy The bootstrap injection strategy to be used.
                 * @param accessControlContext       The access control context to be used.
                 * @param listener                   The listener to be invoked to inform about an applied or non-applied transformation.
                 * @return The class file of the transformed class or {@code null} if no transformation is attempted.
                 */
                byte[] apply(InitializationStrategy initializationStrategy,
                             ClassFileLocator classFileLocator,
                             TypeStrategy typeStrategy,
                             ByteBuddy byteBuddy,
                             NativeMethodStrategy methodNameTransformer,
                             BootstrapInjectionStrategy bootstrapInjectionStrategy,
                             AccessControlContext accessControlContext,
                             Listener listener);

                /**
                 * Describes a specific sort of a {@link Resolution}.
                 */
                enum Sort {

                    /**
                     * A terminal resolution. After discovering such a resolution, no further transformers are considered.
                     */
                    TERMINAL(true),

                    /**
                     * A resolution that can serve as a decorator for another resolution. After discovering such a resolution
                     * further transformations are considered where the represented resolution is prepended if applicable.
                     */
                    DECORATOR(true),

                    /**
                     * A non-resolved resolution.
                     */
                    UNDEFINED(false);

                    /**
                     * Indicates if this sort represents an active resolution.
                     */
                    private final boolean alive;

                    /**
                     * Creates a new resolution sort.
                     *
                     * @param alive Indicates if this sort represents an active resolution.
                     */
                    Sort(boolean alive) {
                        this.alive = alive;
                    }

                    /**
                     * Returns {@code true} if this resolution is alive.
                     *
                     * @return {@code true} if this resolution is alive.
                     */
                    protected boolean isAlive() {
                        return alive;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.Default.Transformation.Resolution.Sort." + name();
                    }
                }

                /**
                 * A resolution that can be decorated by a transformer.
                 */
                interface Decoratable extends Resolution {

                    /**
                     * Appends the supplied transformer to this resolution.
                     *
                     * @param transformer The transformer to append to the transformer that is represented bz this instance.
                     * @return A new resolution with the supplied transformer appended to this transformer.
                     */
                    Resolution append(Transformer transformer);
                }

                /**
                 * A canonical implementation of a non-resolved resolution.
                 */
                class Unresolved implements Resolution {

                    /**
                     * The type that is not transformed.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * The unresolved type's class loader.
                     */
                    private final ClassLoader classLoader;

                    /**
                     * The non-transformed type's module or {@code null} if the current VM does not support modules.
                     */
                    private final JavaModule module;

                    /**
                     * Creates a new unresolved resolution.
                     *
                     * @param typeDescription The type that is not transformed.
                     * @param classLoader     The unresolved type's class loader.
                     * @param module          The non-transformed type's module or {@code null} if the current VM does not support modules.
                     */
                    protected Unresolved(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                        this.typeDescription = typeDescription;
                        this.classLoader = classLoader;
                        this.module = module;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.UNDEFINED;
                    }

                    @Override
                    public Resolution asDecoratorOf(Resolution resolution) {
                        return resolution;
                    }

                    @Override
                    public Resolution prepend(Decoratable resolution) {
                        return resolution;
                    }

                    @Override
                    public byte[] apply(InitializationStrategy initializationStrategy,
                                        ClassFileLocator classFileLocator,
                                        TypeStrategy typeStrategy,
                                        ByteBuddy byteBuddy,
                                        NativeMethodStrategy methodNameTransformer,
                                        BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                        AccessControlContext accessControlContext,
                                        Listener listener) {
                        listener.onIgnored(typeDescription, classLoader, module);
                        return NO_TRANSFORMATION;
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        Unresolved that = (Unresolved) object;
                        return typeDescription.equals(that.typeDescription)
                                && (classLoader != null ? classLoader.equals(that.classLoader) : that.classLoader == null)
                                && (module != null ? module.equals(that.module) : that.module == null);
                    }

                    @Override
                    public int hashCode() {
                        int result = typeDescription.hashCode();
                        result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
                        result = 31 * result + (module != null ? module.hashCode() : 0);
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.Default.Transformation.Resolution.Unresolved{" +
                                "typeDescription=" + typeDescription +
                                ", classLoader=" + classLoader +
                                ", module=" + module +
                                '}';
                    }
                }
            }

            /**
             * A transformation that does not attempt to transform any type.
             */
            enum Ignored implements Transformation {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public boolean isAlive(TypeDescription typeDescription,
                                       ClassLoader classLoader,
                                       JavaModule module,
                                       Class<?> classBeingRedefined,
                                       ProtectionDomain protectionDomain,
                                       RawMatcher ignoredTypeMatcher) {
                    return false;
                }

                @Override
                public Resolution resolve(TypeDescription typeDescription,
                                          ClassLoader classLoader,
                                          JavaModule module,
                                          Class<?> classBeingRedefined,
                                          ProtectionDomain protectionDomain,
                                          TypePool typePool,
                                          RawMatcher ignoredTypeMatcher) {
                    return new Resolution.Unresolved(typeDescription, classLoader, module);
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.Transformation.Ignored." + name();
                }
            }

            /**
             * A simple, active transformation.
             */
            class Simple implements Transformation {

                /**
                 * The raw matcher that is represented by this transformation.
                 */
                private final RawMatcher rawMatcher;

                /**
                 * The transformer that is represented by this transformation.
                 */
                private final Transformer transformer;

                /**
                 * {@code true} if this transformer serves as a decorator.
                 */
                private final boolean decorator;

                /**
                 * Creates a new transformation.
                 *
                 * @param rawMatcher  The raw matcher that is represented by this transformation.
                 * @param transformer The transformer that is represented by this transformation.
                 * @param decorator   {@code true} if this transformer serves as a decorator.
                 */
                protected Simple(RawMatcher rawMatcher, Transformer transformer, boolean decorator) {
                    this.rawMatcher = rawMatcher;
                    this.transformer = transformer;
                    this.decorator = decorator;
                }

                @Override
                public boolean isAlive(TypeDescription typeDescription,
                                       ClassLoader classLoader,
                                       JavaModule module,
                                       Class<?> classBeingRedefined,
                                       ProtectionDomain protectionDomain,
                                       RawMatcher ignoredTypeMatcher) {
                    return !ignoredTypeMatcher.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)
                            && rawMatcher.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain);
                }

                @Override
                public Transformation.Resolution resolve(TypeDescription typeDescription,
                                                         ClassLoader classLoader,
                                                         JavaModule module,
                                                         Class<?> classBeingRedefined,
                                                         ProtectionDomain protectionDomain,
                                                         TypePool typePool,
                                                         RawMatcher ignoredTypeMatcher) {
                    return isAlive(typeDescription, classLoader, module, classBeingRedefined, protectionDomain, ignoredTypeMatcher)
                            ? new Resolution(typeDescription, classLoader, module, protectionDomain, typePool, transformer, decorator)
                            : new Transformation.Resolution.Unresolved(typeDescription, classLoader, module);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && decorator == ((Simple) other).decorator
                            && rawMatcher.equals(((Simple) other).rawMatcher)
                            && transformer.equals(((Simple) other).transformer);
                }

                @Override
                public int hashCode() {
                    int result = rawMatcher.hashCode();
                    result = 31 * result + (decorator ? 1 : 0);
                    result = 31 * result + transformer.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.Transformation.Simple{" +
                            "rawMatcher=" + rawMatcher +
                            ", transformer=" + transformer +
                            ", decorator=" + decorator +
                            '}';
                }

                /**
                 * A resolution that performs a type transformation.
                 */
                protected static class Resolution implements Transformation.Resolution.Decoratable {

                    /**
                     * A description of the transformed type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * The class loader of the transformed type.
                     */
                    private final ClassLoader classLoader;

                    /**
                     * The transformed type's module or {@code null} if the current VM does not support modules.
                     */
                    private final JavaModule module;

                    /**
                     * The protection domain of the transformed type.
                     */
                    private final ProtectionDomain protectionDomain;

                    /**
                     * The type pool to apply during type creation.
                     */
                    private final TypePool typePool;

                    /**
                     * The transformer to be applied.
                     */
                    private final Transformer transformer;

                    /**
                     * {@code true} if this transformer serves as a decorator.
                     */
                    private final boolean decorator;

                    /**
                     * Creates a new active transformation.
                     *
                     * @param typeDescription  A description of the transformed type.
                     * @param classLoader      The class loader of the transformed type.
                     * @param module           The transformed type's module or {@code null} if the current VM does not support modules.
                     * @param protectionDomain The protection domain of the transformed type.
                     * @param typePool         The type pool to apply during type creation.
                     * @param transformer      The transformer to be applied.
                     * @param decorator        {@code true} if this transformer serves as a decorator.
                     */
                    protected Resolution(TypeDescription typeDescription,
                                         ClassLoader classLoader,
                                         JavaModule module,
                                         ProtectionDomain protectionDomain,
                                         TypePool typePool,
                                         Transformer transformer,
                                         boolean decorator) {
                        this.typeDescription = typeDescription;
                        this.classLoader = classLoader;
                        this.module = module;
                        this.protectionDomain = protectionDomain;
                        this.typePool = typePool;
                        this.transformer = transformer;
                        this.decorator = decorator;
                    }

                    @Override
                    public Sort getSort() {
                        return decorator
                                ? Sort.DECORATOR
                                : Sort.TERMINAL;
                    }

                    @Override
                    public Transformation.Resolution asDecoratorOf(Transformation.Resolution resolution) {
                        return resolution.prepend(this);
                    }

                    @Override
                    public Transformation.Resolution prepend(Decoratable resolution) {
                        return resolution.append(transformer);
                    }

                    @Override
                    public Transformation.Resolution append(Transformer transformer) {
                        return new Resolution(typeDescription,
                                classLoader,
                                module,
                                protectionDomain,
                                typePool,
                                new Transformer.Compound(this.transformer, transformer),
                                decorator);
                    }

                    @Override
                    public byte[] apply(InitializationStrategy initializationStrategy,
                                        ClassFileLocator classFileLocator,
                                        TypeStrategy typeStrategy,
                                        ByteBuddy byteBuddy,
                                        NativeMethodStrategy methodNameTransformer,
                                        BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                        AccessControlContext accessControlContext,
                                        Listener listener) {
                        InitializationStrategy.Dispatcher dispatcher = initializationStrategy.dispatcher();
                        DynamicType.Unloaded<?> dynamicType = dispatcher.apply(transformer.transform(typeStrategy.builder(typeDescription,
                                byteBuddy,
                                classFileLocator,
                                methodNameTransformer.resolve()), typeDescription, classLoader)).make(TypeResolutionStrategy.Disabled.INSTANCE, typePool);
                        dispatcher.register(dynamicType, classLoader, new BootstrapClassLoaderCapableInjectorFactory(bootstrapInjectionStrategy,
                                classLoader,
                                protectionDomain));
                        listener.onTransformation(typeDescription, classLoader, module, dynamicType);
                        return dynamicType.getBytes();
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Resolution that = (Resolution) other;
                        return typeDescription.equals(that.typeDescription)
                                && decorator == that.decorator
                                && !(classLoader != null ? !classLoader.equals(that.classLoader) : that.classLoader != null)
                                && !(module != null ? !module.equals(that.module) : that.module != null)
                                && !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null)
                                && typePool.equals(that.typePool)
                                && transformer.equals(that.transformer);
                    }

                    @Override
                    public int hashCode() {
                        int result = typeDescription.hashCode();
                        result = 31 * result + (decorator ? 1 : 0);
                        result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
                        result = 31 * result + (module != null ? module.hashCode() : 0);
                        result = 31 * result + (protectionDomain != null ? protectionDomain.hashCode() : 0);
                        result = 31 * result + transformer.hashCode();
                        result = 31 * result + typePool.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.Default.Transformation.Simple.Resolution{" +
                                "typeDescription=" + typeDescription +
                                ", classLoader=" + classLoader +
                                ", module=" + module +
                                ", protectionDomain=" + protectionDomain +
                                ", typePool=" + typePool +
                                ", transformer=" + transformer +
                                ", decorator=" + decorator +
                                '}';
                    }

                    /**
                     * An injector factory that resolves to a bootstrap class loader injection if this is necessary and enabled.
                     */
                    protected static class BootstrapClassLoaderCapableInjectorFactory implements InitializationStrategy.Dispatcher.InjectorFactory {

                        /**
                         * The bootstrap injection strategy being used.
                         */
                        private final BootstrapInjectionStrategy bootstrapInjectionStrategy;

                        /**
                         * The class loader for which to create an injection factory.
                         */
                        private final ClassLoader classLoader;

                        /**
                         * The protection domain of the created classes.
                         */
                        private final ProtectionDomain protectionDomain;

                        /**
                         * Creates a new bootstrap class loader capable injector factory.
                         *
                         * @param bootstrapInjectionStrategy The bootstrap injection strategy being used.
                         * @param classLoader                The class loader for which to create an injection factory.
                         * @param protectionDomain           The protection domain of the created classes.
                         */
                        protected BootstrapClassLoaderCapableInjectorFactory(BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                                                             ClassLoader classLoader,
                                                                             ProtectionDomain protectionDomain) {
                            this.bootstrapInjectionStrategy = bootstrapInjectionStrategy;
                            this.classLoader = classLoader;
                            this.protectionDomain = protectionDomain;
                        }

                        @Override
                        public ClassInjector resolve() {
                            return classLoader == null
                                    ? bootstrapInjectionStrategy.make(protectionDomain)
                                    : new ClassInjector.UsingReflection(classLoader, protectionDomain);
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (this == other) return true;
                            if (other == null || getClass() != other.getClass()) return false;
                            BootstrapClassLoaderCapableInjectorFactory that = (BootstrapClassLoaderCapableInjectorFactory) other;
                            return bootstrapInjectionStrategy.equals(that.bootstrapInjectionStrategy)
                                    && !(classLoader != null ? !classLoader.equals(that.classLoader) : that.classLoader != null)
                                    && !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null);
                        }

                        @Override
                        public int hashCode() {
                            int result = bootstrapInjectionStrategy.hashCode();
                            result = 31 * result + (protectionDomain != null ? protectionDomain.hashCode() : 0);
                            result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
                            return result;
                        }

                        @Override
                        public String toString() {
                            return "AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory{" +
                                    "bootstrapInjectionStrategy=" + bootstrapInjectionStrategy +
                                    ", classLoader=" + classLoader +
                                    ", protectionDomain=" + protectionDomain +
                                    '}';
                        }
                    }
                }
            }

            /**
             * A compound transformation that applied several transformation in the given order and applies the first active transformation.
             */
            class Compound implements Transformation {

                /**
                 * The list of transformations to apply in their application order.
                 */
                private final List<? extends Transformation> transformations;

                /**
                 * Creates a new compound transformation.
                 *
                 * @param transformation An array of transformations to apply in their application order.
                 */
                protected Compound(Transformation... transformation) {
                    this(Arrays.asList(transformation));
                }

                /**
                 * Creates a new compound transformation.
                 *
                 * @param transformations A list of transformations to apply in their application order.
                 */
                protected Compound(List<? extends Transformation> transformations) {
                    this.transformations = transformations;
                }

                @Override
                public boolean isAlive(TypeDescription typeDescription,
                                       ClassLoader classLoader,
                                       JavaModule module,
                                       Class<?> classBeingRedefined,
                                       ProtectionDomain protectionDomain,
                                       RawMatcher ignoredTypeMatcher) {
                    for (Transformation transformation : transformations) {
                        if (transformation.isAlive(typeDescription, classLoader, module, classBeingRedefined, protectionDomain, ignoredTypeMatcher)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public Resolution resolve(TypeDescription typeDescription,
                                          ClassLoader classLoader,
                                          JavaModule module,
                                          Class<?> classBeingRedefined,
                                          ProtectionDomain protectionDomain,
                                          TypePool typePool,
                                          RawMatcher ignoredTypeMatcher) {
                    Resolution current = new Resolution.Unresolved(typeDescription, classLoader, module);
                    for (Transformation transformation : transformations) {
                        Resolution resolution = transformation.resolve(typeDescription,
                                classLoader,
                                module,
                                classBeingRedefined,
                                protectionDomain,
                                typePool,
                                ignoredTypeMatcher);
                        switch (resolution.getSort()) {
                            case TERMINAL:
                                return current.asDecoratorOf(resolution);
                            case DECORATOR:
                                current = current.asDecoratorOf(resolution);
                                break;
                            case UNDEFINED:
                                break;
                            default:
                                throw new IllegalStateException("Unexpected resolution type: " + resolution.getSort());
                        }
                    }
                    return current;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && transformations.equals(((Compound) other).transformations);
                }

                @Override
                public int hashCode() {
                    return transformations.hashCode();
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.Transformation.Compound{" +
                            "transformations=" + transformations +
                            '}';
                }
            }
        }

        /**
         * A {@link java.lang.instrument.ClassFileTransformer} that implements the enclosing agent builder's
         * configuration.
         */
        protected static class ExecutingTransformer implements ClassFileTransformer {

            /**
             * A factory for creating a {@link ClassFileTransformer} that supports the features of the current VM.
             */
            protected static final Factory FACTORY = AccessController.doPrivileged(FactoryCreationOption.INSTANCE);

            /**
             * The Byte Buddy instance to be used.
             */
            private final ByteBuddy byteBuddy;

            /**
             * The type locator to use.
             */
            private final PoolStrategy poolStrategy;

            /**
             * The definition handler to use.
             */
            private final TypeStrategy typeStrategy;

            /**
             * The listener to notify on transformations.
             */
            private final Listener listener;

            /**
             * The native method strategy to apply.
             */
            private final NativeMethodStrategy nativeMethodStrategy;

            /**
             * The initialization strategy to use for transformed types.
             */
            private final InitializationStrategy initializationStrategy;

            /**
             * The injection strategy for injecting classes into the bootstrap class loader.
             */
            private final BootstrapInjectionStrategy bootstrapInjectionStrategy;

            /**
             * The description strategy for resolving type descriptions for types.
             */
            private final DescriptionStrategy descriptionStrategy;

            /**
             * The location strategy to use.
             */
            private final LocationStrategy locationStrategy;

            /**
             * The fallback strategy to use.
             */
            private final FallbackStrategy fallbackStrategy;

            /**
             * Identifies types that should not be instrumented.
             */
            private final RawMatcher ignoredTypeMatcher;

            /**
             * The transformation object for handling type transformations.
             */
            private final Transformation transformation;

            /**
             * The access control context to use for loading classes.
             */
            private final AccessControlContext accessControlContext;

            /**
             * Creates a new class file transformer.
             *
             * @param byteBuddy                  The Byte Buddy instance to be used.
             * @param listener                   The listener to notify on transformations.
             * @param poolStrategy               The type locator to use.
             * @param typeStrategy               The definition handler to use.
             * @param locationStrategy           The location strategy to use.
             * @param nativeMethodStrategy       The native method strategy to apply.
             * @param initializationStrategy     The initialization strategy to use for transformed types.
             * @param bootstrapInjectionStrategy The injection strategy for injecting classes into the bootstrap class loader.
             * @param descriptionStrategy        The description strategy for resolving type descriptions for types.
             * @param fallbackStrategy           The fallback strategy to use.
             * @param ignoredTypeMatcher         Identifies types that should not be instrumented.
             * @param transformation             The transformation object for handling type transformations.
             */
            public ExecutingTransformer(ByteBuddy byteBuddy,
                                        Listener listener,
                                        PoolStrategy poolStrategy,
                                        TypeStrategy typeStrategy,
                                        LocationStrategy locationStrategy,
                                        NativeMethodStrategy nativeMethodStrategy,
                                        InitializationStrategy initializationStrategy,
                                        BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                        DescriptionStrategy descriptionStrategy,
                                        FallbackStrategy fallbackStrategy,
                                        RawMatcher ignoredTypeMatcher,
                                        Transformation transformation) {
                this.byteBuddy = byteBuddy;
                this.typeStrategy = typeStrategy;
                this.poolStrategy = poolStrategy;
                this.locationStrategy = locationStrategy;
                this.listener = listener;
                this.nativeMethodStrategy = nativeMethodStrategy;
                this.initializationStrategy = initializationStrategy;
                this.bootstrapInjectionStrategy = bootstrapInjectionStrategy;
                this.descriptionStrategy = descriptionStrategy;
                this.fallbackStrategy = fallbackStrategy;
                this.ignoredTypeMatcher = ignoredTypeMatcher;
                this.transformation = transformation;
                accessControlContext = AccessController.getContext();
            }

            @Override
            public byte[] transform(ClassLoader classLoader,
                                    String internalTypeName,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) {
                return AccessController.doPrivileged(new LegacyVmDispatcher(classLoader,
                        internalTypeName,
                        classBeingRedefined,
                        protectionDomain,
                        binaryRepresentation), accessControlContext);
            }

            /**
             * Applies a transformation for a class that was captured by this {@link ClassFileTransformer}. Invoking this method
             * allows to process module information which is available since Java 9.
             *
             * @param rawModule            The instrumented class's Java {@code java.lang.reflect.Module}.
             * @param internalTypeName     The internal name of the instrumented class.
             * @param classBeingRedefined  The loaded {@link Class} being redefined or {@code null} if no such class exists.
             * @param protectionDomain     The instrumented type's protection domain.
             * @param binaryRepresentation The class file of the instrumented class in its current state.
             * @return The transformed class file or an empty byte array if this transformer does not apply an instrumentation.
             */
            protected byte[] transform(Object rawModule,
                                       String internalTypeName,
                                       Class<?> classBeingRedefined,
                                       ProtectionDomain protectionDomain,
                                       byte[] binaryRepresentation) {
                return AccessController.doPrivileged(new Java9CapableVmDispatcher(rawModule,
                        internalTypeName,
                        classBeingRedefined,
                        protectionDomain,
                        binaryRepresentation), accessControlContext);
            }

            /**
             * Applies a transformation for a class that was captured by this {@link ClassFileTransformer}.
             *
             * @param module               The instrumented class's Java module in its wrapped form or {@code null} if the current VM does not support modules.
             * @param classLoader          The instrumented class's class loader.
             * @param internalTypeName     The internal name of the instrumented class.
             * @param classBeingRedefined  The loaded {@link Class} being redefined or {@code null} if no such class exists.
             * @param protectionDomain     The instrumented type's protection domain.
             * @param binaryRepresentation The class file of the instrumented class in its current state.
             * @return The transformed class file or an empty byte array if this transformer does not apply an instrumentation.
             */
            private byte[] transform(JavaModule module,
                                     ClassLoader classLoader,
                                     String internalTypeName,
                                     Class<?> classBeingRedefined,
                                     ProtectionDomain protectionDomain,
                                     byte[] binaryRepresentation) {
                if (internalTypeName == null) {
                    return NO_TRANSFORMATION;
                }
                String typeName = internalTypeName.replace('/', '.');
                try {
                    ClassFileLocator classFileLocator = ClassFileLocator.Simple.of(typeName,
                            binaryRepresentation,
                            locationStrategy.classFileLocator(classLoader, module));
                    TypePool typePool = poolStrategy.typePool(classFileLocator, classLoader);
                    try {
                        return doTransform(module, classLoader, typeName, classBeingRedefined, protectionDomain, typePool, classFileLocator);
                    } catch (Throwable throwable) {
                        if (classBeingRedefined != null && descriptionStrategy.isLoadedFirst() && fallbackStrategy.isFallback(classBeingRedefined, throwable)) {
                            return doTransform(module, classLoader, typeName, NO_LOADED_TYPE, protectionDomain, typePool, classFileLocator);
                        } else {
                            throw throwable;
                        }
                    }
                } catch (Throwable throwable) {
                    listener.onError(typeName, classLoader, module, throwable);
                    return NO_TRANSFORMATION;
                } finally {
                    listener.onComplete(typeName, classLoader, module);
                }
            }

            /**
             * Applies a transformation for a class that was captured by this {@link ClassFileTransformer}.
             *
             * @param module              The instrumented class's Java module in its wrapped form or {@code null} if the current VM does not support modules.
             * @param classLoader         The instrumented class's class loader.
             * @param typeName            The binary name of the instrumented class.
             * @param classBeingRedefined The loaded {@link Class} being redefined or {@code null} if no such class exists.
             * @param protectionDomain    The instrumented type's protection domain.
             * @param typePool            The type pool to use.
             * @param classFileLocator    The class file locator to use.
             * @return The transformed class file or an empty byte array if this transformer does not apply an instrumentation.
             */
            private byte[] doTransform(JavaModule module,
                                       ClassLoader classLoader,
                                       String typeName,
                                       Class<?> classBeingRedefined,
                                       ProtectionDomain protectionDomain,
                                       TypePool typePool,
                                       ClassFileLocator classFileLocator) {
                return transformation.resolve(descriptionStrategy.apply(typeName, classBeingRedefined, typePool),
                        classLoader,
                        module,
                        classBeingRedefined,
                        protectionDomain,
                        typePool,
                        ignoredTypeMatcher).apply(initializationStrategy,
                        classFileLocator,
                        typeStrategy,
                        byteBuddy,
                        nativeMethodStrategy,
                        bootstrapInjectionStrategy,
                        accessControlContext,
                        listener);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ExecutingTransformer that = (ExecutingTransformer) other;
                return byteBuddy.equals(that.byteBuddy)
                        && listener.equals(that.listener)
                        && poolStrategy.equals(that.poolStrategy)
                        && typeStrategy.equals(that.typeStrategy)
                        && locationStrategy.equals(that.locationStrategy)
                        && initializationStrategy.equals(that.initializationStrategy)
                        && nativeMethodStrategy.equals(that.nativeMethodStrategy)
                        && bootstrapInjectionStrategy.equals(that.bootstrapInjectionStrategy)
                        && descriptionStrategy.equals(that.descriptionStrategy)
                        && ignoredTypeMatcher.equals(that.ignoredTypeMatcher)
                        && fallbackStrategy.equals(that.fallbackStrategy)
                        && transformation.equals(that.transformation)
                        && accessControlContext.equals(that.accessControlContext);
            }

            @Override
            public int hashCode() {
                int result = byteBuddy.hashCode();
                result = 31 * result + listener.hashCode();
                result = 31 * result + poolStrategy.hashCode();
                result = 31 * result + typeStrategy.hashCode();
                result = 31 * result + locationStrategy.hashCode();
                result = 31 * result + initializationStrategy.hashCode();
                result = 31 * result + nativeMethodStrategy.hashCode();
                result = 31 * result + bootstrapInjectionStrategy.hashCode();
                result = 31 * result + descriptionStrategy.hashCode();
                result = 31 * result + fallbackStrategy.hashCode();
                result = 31 * result + ignoredTypeMatcher.hashCode();
                result = 31 * result + transformation.hashCode();
                result = 31 * result + accessControlContext.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Default." + getClass().getSimpleName() + "{" +
                        "byteBuddy=" + byteBuddy +
                        ", listener=" + listener +
                        ", poolStrategy=" + poolStrategy +
                        ", typeStrategy=" + typeStrategy +
                        ", locationStrategy=" + locationStrategy +
                        ", initializationStrategy=" + initializationStrategy +
                        ", nativeMethodStrategy=" + nativeMethodStrategy +
                        ", bootstrapInjectionStrategy=" + bootstrapInjectionStrategy +
                        ", descriptionStrategy=" + descriptionStrategy +
                        ", fallbackStrategy=" + fallbackStrategy +
                        ", ignoredTypeMatcher=" + ignoredTypeMatcher +
                        ", transformation=" + transformation +
                        ", accessControlContext=" + accessControlContext +
                        '}';
            }

            /**
             * A factory for creating a {@link ClassFileTransformer} for the current VM.
             */
            protected interface Factory {

                /**
                 * Creates a new class file transformer for the current VM.
                 *
                 * @param byteBuddy                  The Byte Buddy instance to be used.
                 * @param listener                   The listener to notify on transformations.
                 * @param poolStrategy               The type locator to use.
                 * @param typeStrategy               The definition handler to use.
                 * @param locationStrategy           The location strategy to use.
                 * @param nativeMethodStrategy       The native method strategy to apply.
                 * @param initializationStrategy     The initialization strategy to use for transformed types.
                 * @param bootstrapInjectionStrategy The injection strategy for injecting classes into the bootstrap class loader.
                 * @param descriptionStrategy        The description strategy for resolving type descriptions for types.
                 * @param fallbackStrategy           The fallback strategy to use.
                 * @param ignoredTypeMatcher         Identifies types that should not be instrumented.
                 * @param transformation             The transformation object for handling type transformations.
                 * @return A class file transformer for the current VM that supports the API of the current VM.
                 */
                ClassFileTransformer make(ByteBuddy byteBuddy,
                                          Listener listener,
                                          PoolStrategy poolStrategy,
                                          TypeStrategy typeStrategy,
                                          LocationStrategy locationStrategy,
                                          NativeMethodStrategy nativeMethodStrategy,
                                          InitializationStrategy initializationStrategy,
                                          BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                          DescriptionStrategy descriptionStrategy,
                                          FallbackStrategy fallbackStrategy,
                                          RawMatcher ignoredTypeMatcher,
                                          Transformation transformation);

                /**
                 * A factory for a class file transformer on a JVM that supports the {@code java.lang.reflect.Module} API to override
                 * the newly added method of the {@link ClassFileTransformer} to capture an instrumented class's module.
                 */
                class ForJava9CapableVm implements Factory {

                    /**
                     * A constructor for creating a {@link ClassFileTransformer} that overrides the newly added method for extracting
                     * the {@code java.lang.reflect.Module} of an instrumented class.
                     */
                    private final Constructor<? extends ClassFileTransformer> executingTransformer;

                    /**
                     * Creates a class file transformer factory for a Java 9 capable VM.
                     *
                     * @param executingTransformer A constructor for creating a {@link ClassFileTransformer} that overrides the newly added
                     *                             method for extracting the {@code java.lang.reflect.Module} of an instrumented class.
                     */
                    protected ForJava9CapableVm(Constructor<? extends ClassFileTransformer> executingTransformer) {
                        this.executingTransformer = executingTransformer;
                    }

                    @Override
                    public ClassFileTransformer make(ByteBuddy byteBuddy,
                                                     Listener listener,
                                                     PoolStrategy poolStrategy,
                                                     TypeStrategy typeStrategy,
                                                     LocationStrategy locationStrategy,
                                                     NativeMethodStrategy nativeMethodStrategy,
                                                     InitializationStrategy initializationStrategy,
                                                     BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                                     DescriptionStrategy descriptionStrategy,
                                                     FallbackStrategy fallbackStrategy,
                                                     RawMatcher ignoredTypeMatcher,
                                                     Transformation transformation) {
                        try {
                            return executingTransformer.newInstance(byteBuddy,
                                    listener,
                                    poolStrategy,
                                    typeStrategy,
                                    locationStrategy,
                                    nativeMethodStrategy,
                                    initializationStrategy,
                                    bootstrapInjectionStrategy,
                                    descriptionStrategy,
                                    fallbackStrategy,
                                    ignoredTypeMatcher,
                                    transformation);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Cannot access " + executingTransformer, exception);
                        } catch (InstantiationException exception) {
                            throw new IllegalStateException("Cannot instantiate " + executingTransformer.getDeclaringClass(), exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Cannot invoke " + executingTransformer, exception.getCause());
                        }
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        ForJava9CapableVm that = (ForJava9CapableVm) object;
                        return executingTransformer.equals(that.executingTransformer);
                    }

                    @Override
                    public int hashCode() {
                        return executingTransformer.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.Default.ExecutingTransformer.Factory.ForJava9CapableVm{" +
                                "executingTransformer=" + executingTransformer +
                                '}';
                    }
                }

                /**
                 * A factory for a {@link ClassFileTransformer} on a VM that does not support the {@code java.lang.reflect.Module} API.
                 */
                enum ForLegacyVm implements Factory {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public ClassFileTransformer make(ByteBuddy byteBuddy,
                                                     Listener listener,
                                                     PoolStrategy poolStrategy,
                                                     TypeStrategy typeStrategy,
                                                     LocationStrategy locationStrategy,
                                                     NativeMethodStrategy nativeMethodStrategy,
                                                     InitializationStrategy initializationStrategy,
                                                     BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                                     DescriptionStrategy descriptionStrategy,
                                                     FallbackStrategy fallbackStrategy,
                                                     RawMatcher ignoredTypeMatcher,
                                                     Transformation transformation) {
                        return new ExecutingTransformer(byteBuddy,
                                listener,
                                poolStrategy,
                                typeStrategy,
                                locationStrategy,
                                nativeMethodStrategy,
                                initializationStrategy,
                                bootstrapInjectionStrategy,
                                descriptionStrategy,
                                fallbackStrategy,
                                ignoredTypeMatcher,
                                transformation);
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.Default.ExecutingTransformer.Factory.ForLegacyVm." + name();
                    }
                }
            }

            /**
             * An action to create an implementation of {@link ExecutingTransformer} that support Java 9 modules.
             */
            protected enum FactoryCreationOption implements PrivilegedAction<Factory> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public Factory run() {
                    try {
                        return new Factory.ForJava9CapableVm(new ByteBuddy()
                                .subclass(ExecutingTransformer.class)
                                .name(ExecutingTransformer.class.getName() + "$ByteBuddy$ModuleSupport")
                                .method(named("transform").and(takesArgument(0, JavaType.MODULE.load())))
                                .intercept(MethodCall.invoke(ExecutingTransformer.class.getDeclaredMethod("transform",
                                        Object.class,
                                        String.class,
                                        Class.class,
                                        ProtectionDomain.class,
                                        byte[].class)).onSuper().withAllArguments())
                                .make()
                                .load(ExecutingTransformer.class.getClassLoader(),
                                        ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.with(ExecutingTransformer.class.getProtectionDomain()))
                                .getLoaded()
                                .getDeclaredConstructor(ByteBuddy.class,
                                        Listener.class,
                                        PoolStrategy.class,
                                        TypeStrategy.class,
                                        LocationStrategy.class,
                                        NativeMethodStrategy.class,
                                        InitializationStrategy.class,
                                        BootstrapInjectionStrategy.class,
                                        DescriptionStrategy.class,
                                        FallbackStrategy.class,
                                        RawMatcher.class,
                                        Transformation.class));
                    } catch (Exception ignored) {
                        return Factory.ForLegacyVm.INSTANCE;
                    }
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.ExecutingTransformer.InheritanceAction." + name();
                }
            }

            /**
             * A privileged action for transforming a class on a JVM prior to Java 9.
             */
            protected class LegacyVmDispatcher implements PrivilegedAction<byte[]> {

                /**
                 * The type's class loader or {@code null} if the bootstrap class loader is represented.
                 */
                private final ClassLoader classLoader;

                /**
                 * The type's internal name or {@code null} if no such name exists.
                 */
                private final String internalTypeName;

                /**
                 * The class being redefined or {@code null} if no such class exists.
                 */
                private final Class<?> classBeingRedefined;

                /**
                 * The type's protection domain.
                 */
                private final ProtectionDomain protectionDomain;

                /**
                 * The type's binary representation.
                 */
                private final byte[] binaryRepresentation;

                /**
                 * Creates a new type transformation dispatcher.
                 *
                 * @param classLoader          The type's class loader or {@code null} if the bootstrap class loader is represented.
                 * @param internalTypeName     The type's internal name or {@code null} if no such name exists.
                 * @param classBeingRedefined  The class being redefined or {@code null} if no such class exists.
                 * @param protectionDomain     The type's protection domain.
                 * @param binaryRepresentation The type's binary representation.
                 */
                protected LegacyVmDispatcher(ClassLoader classLoader,
                                             String internalTypeName,
                                             Class<?> classBeingRedefined,
                                             ProtectionDomain protectionDomain,
                                             byte[] binaryRepresentation) {
                    this.classLoader = classLoader;
                    this.internalTypeName = internalTypeName;
                    this.classBeingRedefined = classBeingRedefined;
                    this.protectionDomain = protectionDomain;
                    this.binaryRepresentation = binaryRepresentation;
                }

                @Override
                public byte[] run() {
                    return transform(JavaModule.UNSUPPORTED,
                            classLoader,
                            internalTypeName,
                            classBeingRedefined,
                            protectionDomain,
                            binaryRepresentation);
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private ExecutingTransformer getOuter() {
                    return ExecutingTransformer.this;
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    LegacyVmDispatcher that = (LegacyVmDispatcher) object;
                    return (classLoader != null ? classLoader.equals(that.classLoader) : that.classLoader == null)
                            && (internalTypeName != null ? internalTypeName.equals(that.internalTypeName) : that.internalTypeName == null)
                            && (classBeingRedefined != null ? classBeingRedefined.equals(that.classBeingRedefined) : that.classBeingRedefined == null)
                            && protectionDomain.equals(that.protectionDomain)
                            && ExecutingTransformer.this.equals(that.getOuter())
                            && Arrays.equals(binaryRepresentation, that.binaryRepresentation);
                }

                @Override
                public int hashCode() {
                    int result = classLoader != null ? classLoader.hashCode() : 0;
                    result = 31 * result + (internalTypeName != null ? internalTypeName.hashCode() : 0);
                    result = 31 * result + (classBeingRedefined != null ? classBeingRedefined.hashCode() : 0);
                    result = 31 * result + protectionDomain.hashCode();
                    result = 31 * result + ExecutingTransformer.this.hashCode();
                    result = 31 * result + Arrays.hashCode(binaryRepresentation);
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.ExecutingTransformer.LegacyVmDispatcher{" +
                            "outer=" + ExecutingTransformer.this +
                            ", classLoader=" + classLoader +
                            ", internalTypeName='" + internalTypeName + '\'' +
                            ", classBeingRedefined=" + classBeingRedefined +
                            ", protectionDomain=" + protectionDomain +
                            ", binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                            '}';
                }
            }

            /**
             * A privileged action for transforming a class on a JVM that supports modules.
             */
            protected class Java9CapableVmDispatcher implements PrivilegedAction<byte[]> {

                /**
                 * The type's {@code java.lang.reflect.Module}.
                 */
                private final Object rawModule;

                /**
                 * The type's internal name or {@code null} if no such name exists.
                 */
                private final String internalTypeName;

                /**
                 * The class being redefined or {@code null} if no such class exists.
                 */
                private final Class<?> classBeingRedefined;

                /**
                 * The type's protection domain.
                 */
                private final ProtectionDomain protectionDomain;

                /**
                 * The type's binary representation.
                 */
                private final byte[] binaryRepresentation;


                /**
                 * Creates a new legacy dispatcher.
                 *
                 * @param rawModule            The type's {@code java.lang.reflect.Module}.
                 * @param internalTypeName     The type's internal name or {@code null} if no such name exists.
                 * @param classBeingRedefined  The class being redefined or {@code null} if no such class exists.
                 * @param protectionDomain     The type's protection domain.
                 * @param binaryRepresentation The type's binary representation.
                 */
                protected Java9CapableVmDispatcher(Object rawModule,
                                                   String internalTypeName,
                                                   Class<?> classBeingRedefined,
                                                   ProtectionDomain protectionDomain,
                                                   byte[] binaryRepresentation) {
                    this.rawModule = rawModule;
                    this.internalTypeName = internalTypeName;
                    this.classBeingRedefined = classBeingRedefined;
                    this.protectionDomain = protectionDomain;
                    this.binaryRepresentation = binaryRepresentation;
                }

                @Override
                public byte[] run() {
                    JavaModule module = JavaModule.of(rawModule);
                    return transform(module,
                            module.getClassLoader(),
                            internalTypeName,
                            classBeingRedefined,
                            protectionDomain,
                            binaryRepresentation);
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private ExecutingTransformer getOuter() {
                    return ExecutingTransformer.this;
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    Java9CapableVmDispatcher that = (Java9CapableVmDispatcher) object;
                    return rawModule.equals(that.rawModule)
                            && (internalTypeName != null ? internalTypeName.equals(that.internalTypeName) : that.internalTypeName == null)
                            && (classBeingRedefined != null ? classBeingRedefined.equals(that.classBeingRedefined) : that.classBeingRedefined == null)
                            && protectionDomain.equals(that.protectionDomain)
                            && ExecutingTransformer.this.equals(that.getOuter())
                            && Arrays.equals(binaryRepresentation, that.binaryRepresentation);
                }

                @Override
                public int hashCode() {
                    int result = rawModule.hashCode();
                    result = 31 * result + (internalTypeName != null ? internalTypeName.hashCode() : 0);
                    result = 31 * result + (classBeingRedefined != null ? classBeingRedefined.hashCode() : 0);
                    result = 31 * result + protectionDomain.hashCode();
                    result = 31 * result + ExecutingTransformer.this.hashCode();
                    result = 31 * result + Arrays.hashCode(binaryRepresentation);
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.ExecutingTransformer.Java9CapableVmDispatcher{" +
                            "outer=" + ExecutingTransformer.this +
                            ", rawModule=" + rawModule +
                            ", internalTypeName='" + internalTypeName + '\'' +
                            ", classBeingRedefined=" + classBeingRedefined +
                            ", protectionDomain=" + protectionDomain +
                            ", binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                            '}';
                }
            }
        }

        /**
         * An abstract implementation of an agent builder that delegates all invocation to another instance.
         *
         * @param <T> The type that is produced by chaining a matcher.
         */
        protected abstract class Delegator<T extends Matchable<T>> extends Matchable.AbstractBase<T> implements AgentBuilder {

            /**
             * Materializes the currently described {@link net.bytebuddy.agent.builder.AgentBuilder}.
             *
             * @return An agent builder that represents the currently described entry of this instance.
             */
            protected abstract AgentBuilder materialize();

            @Override
            public AgentBuilder with(ByteBuddy byteBuddy) {
                return materialize().with(byteBuddy);
            }

            @Override
            public AgentBuilder with(Listener listener) {
                return materialize().with(listener);
            }

            @Override
            public AgentBuilder with(TypeStrategy typeStrategy) {
                return materialize().with(typeStrategy);
            }

            @Override
            public AgentBuilder with(PoolStrategy poolStrategy) {
                return materialize().with(poolStrategy);
            }

            @Override
            public AgentBuilder with(LocationStrategy locationStrategy) {
                return materialize().with(locationStrategy);
            }

            @Override
            public AgentBuilder with(InitializationStrategy initializationStrategy) {
                return materialize().with(initializationStrategy);
            }

            @Override
            public AgentBuilder with(RedefinitionStrategy redefinitionStrategy) {
                return materialize().with(redefinitionStrategy);
            }

            @Override
            public AgentBuilder with(LambdaInstrumentationStrategy lambdaInstrumentationStrategy) {
                return materialize().with(lambdaInstrumentationStrategy);
            }

            @Override
            public AgentBuilder with(DescriptionStrategy descriptionStrategy) {
                return materialize().with(descriptionStrategy);
            }

            @Override
            public AgentBuilder with(InstallationStrategy installationStrategy) {
                return materialize().with(installationStrategy);
            }

            @Override
            public AgentBuilder with(FallbackStrategy fallbackStrategy) {
                return materialize().with(fallbackStrategy);
            }

            @Override
            public AgentBuilder enableBootstrapInjection(Instrumentation instrumentation, File folder) {
                return materialize().enableBootstrapInjection(instrumentation, folder);
            }

            @Override
            public AgentBuilder disableBootstrapInjection() {
                return materialize().disableBootstrapInjection();
            }

            @Override
            public AgentBuilder enableNativeMethodPrefix(String prefix) {
                return materialize().enableNativeMethodPrefix(prefix);
            }

            @Override
            public AgentBuilder disableNativeMethodPrefix() {
                return materialize().disableNativeMethodPrefix();
            }

            @Override
            public AgentBuilder disableClassFormatChanges() {
                return materialize().disableClassFormatChanges();
            }

            @Override
            public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Class<?>... type) {
                return materialize().assureReadEdgeTo(instrumentation, type);
            }

            @Override
            public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, JavaModule... module) {
                return materialize().assureReadEdgeTo(instrumentation, module);
            }

            @Override
            public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules) {
                return materialize().assureReadEdgeTo(instrumentation, modules);
            }

            @Override
            public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Class<?>... type) {
                return materialize().assureReadEdgeFromAndTo(instrumentation, type);
            }

            @Override
            public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, JavaModule... module) {
                return materialize().assureReadEdgeFromAndTo(instrumentation, module);
            }

            @Override
            public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules) {
                return materialize().assureReadEdgeFromAndTo(instrumentation, modules);
            }

            @Override
            public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher) {
                return materialize().type(typeMatcher);
            }

            @Override
            public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return materialize().type(typeMatcher, classLoaderMatcher);
            }

            @Override
            public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher,
                                              ElementMatcher<? super ClassLoader> classLoaderMatcher,
                                              ElementMatcher<? super JavaModule> moduleMatcher) {
                return materialize().type(typeMatcher, classLoaderMatcher, moduleMatcher);
            }


            @Override
            public Identified.Narrowable type(RawMatcher matcher) {
                return materialize().type(matcher);
            }

            @Override
            public Ignored ignore(ElementMatcher<? super TypeDescription> ignoredTypes) {
                return materialize().ignore(ignoredTypes);
            }

            @Override
            public Ignored ignore(ElementMatcher<? super TypeDescription> ignoredTypes, ElementMatcher<? super ClassLoader> ignoredClassLoaders) {
                return materialize().ignore(ignoredTypes, ignoredClassLoaders);
            }

            @Override
            public Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher,
                                  ElementMatcher<? super ClassLoader> classLoaderMatcher,
                                  ElementMatcher<? super JavaModule> moduleMatcher) {
                return materialize().ignore(typeMatcher, classLoaderMatcher, moduleMatcher);
            }

            @Override
            public Ignored ignore(RawMatcher rawMatcher) {
                return materialize().ignore(rawMatcher);
            }

            @Override
            public ClassFileTransformer makeRaw() {
                return materialize().makeRaw();
            }

            @Override
            public ClassFileTransformer installOn(Instrumentation instrumentation) {
                return materialize().installOn(instrumentation);
            }

            @Override
            public ClassFileTransformer installOnByteBuddyAgent() {
                return materialize().installOnByteBuddyAgent();
            }
        }

        /**
         * A delegator transformer for further precising what types to ignore.
         */
        protected class Ignoring extends Delegator<Ignored> implements Ignored {

            /**
             * A matcher for identifying types that should not be instrumented.
             */
            private final RawMatcher rawMatcher;

            /**
             * Creates a new agent builder for further specifying what types to ignore.
             *
             * @param rawMatcher A matcher for identifying types that should not be instrumented.
             */
            protected Ignoring(RawMatcher rawMatcher) {
                this.rawMatcher = rawMatcher;
            }

            @Override
            protected AgentBuilder materialize() {
                return new Default(byteBuddy,
                        listener,
                        poolStrategy,
                        typeStrategy,
                        locationStrategy,
                        nativeMethodStrategy,
                        initializationStrategy,
                        redefinitionStrategy,
                        bootstrapInjectionStrategy,
                        lambdaInstrumentationStrategy,
                        descriptionStrategy,
                        installationStrategy,
                        fallbackStrategy,
                        rawMatcher,
                        transformation);
            }

            @Override
            public Ignored and(RawMatcher rawMatcher) {
                return new Ignoring(new RawMatcher.Conjunction(this.rawMatcher, rawMatcher));
            }

            @Override
            public Ignored or(RawMatcher rawMatcher) {
                return new Ignoring(new RawMatcher.Disjunction(this.rawMatcher, rawMatcher));
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private Default getOuter() {
                return Default.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && rawMatcher.equals(((Ignoring) other).rawMatcher)
                        && Default.this.equals(((Ignoring) other).getOuter());
            }

            @Override
            public int hashCode() {
                int result = rawMatcher.hashCode();
                result = 31 * result + Default.this.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Default.Ignoring{" +
                        "rawMatcher=" + rawMatcher +
                        ", agentBuilder=" + Default.this +
                        '}';
            }
        }

        /**
         * A helper class that describes a {@link net.bytebuddy.agent.builder.AgentBuilder.Default} after supplying
         * a {@link net.bytebuddy.agent.builder.AgentBuilder.RawMatcher} such that one or several
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s can be supplied.
         */
        protected class Transforming extends Delegator<Identified.Narrowable> implements Identified.Extendable, Identified.Narrowable {

            /**
             * The supplied raw matcher.
             */
            private final RawMatcher rawMatcher;

            /**
             * The supplied transformer.
             */
            private final Transformer transformer;

            /**
             * {@code true} if this transformer serves as a decorator.
             */
            private final boolean decorator;

            /**
             * Creates a new matched default agent builder.
             *
             * @param rawMatcher  The supplied raw matcher.
             * @param transformer The supplied transformer.
             * @param decorator   {@code true} if this transformer serves as a decorator.
             */
            protected Transforming(RawMatcher rawMatcher, Transformer transformer, boolean decorator) {
                this.rawMatcher = rawMatcher;
                this.transformer = transformer;
                this.decorator = decorator;
            }

            @Override
            protected AgentBuilder materialize() {
                return new Default(byteBuddy,
                        listener,
                        poolStrategy,
                        typeStrategy,
                        locationStrategy,
                        nativeMethodStrategy,
                        initializationStrategy,
                        redefinitionStrategy,
                        bootstrapInjectionStrategy,
                        lambdaInstrumentationStrategy,
                        descriptionStrategy,
                        installationStrategy,
                        fallbackStrategy,
                        ignoredTypeMatcher,
                        new Transformation.Compound(new Transformation.Simple(rawMatcher, transformer, decorator), transformation));
            }

            @Override
            public Identified.Extendable transform(Transformer transformer) {
                return new Transforming(rawMatcher, new Transformer.Compound(this.transformer, transformer), decorator);
            }

            @Override
            public AgentBuilder asDecorator() {
                return new Transforming(rawMatcher, transformer, true);
            }

            @Override
            public Narrowable and(RawMatcher rawMatcher) {
                return new Transforming(new RawMatcher.Conjunction(this.rawMatcher, rawMatcher), transformer, decorator);
            }

            @Override
            public Narrowable or(RawMatcher rawMatcher) {
                return new Transforming(new RawMatcher.Disjunction(this.rawMatcher, rawMatcher), transformer, decorator);
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private Default getOuter() {
                return Default.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && decorator == ((Transforming) other).decorator
                        && rawMatcher.equals(((Transforming) other).rawMatcher)
                        && transformer.equals(((Transforming) other).transformer)
                        && Default.this.equals(((Transforming) other).getOuter());
            }

            @Override
            public int hashCode() {
                int result = rawMatcher.hashCode();
                result = 31 * result + (decorator ? 1 : 0);
                result = 31 * result + transformer.hashCode();
                result = 31 * result + Default.this.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Default.Transforming{" +
                        "rawMatcher=" + rawMatcher +
                        ", transformer=" + transformer +
                        ", decorator=" + decorator +
                        ", agentBuilder=" + Default.this +
                        '}';
            }
        }
    }
}

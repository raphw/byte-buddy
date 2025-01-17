/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.asm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.build.RepeatedAnnotationPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.collection.ArrayAccess;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.*;
import net.bytebuddy.utility.nullability.AlwaysNull;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.visitor.ExceptionTableSensitiveMethodVisitor;
import net.bytebuddy.utility.visitor.LineNumberPrependingMethodVisitor;
import net.bytebuddy.utility.visitor.StackAwareMethodVisitor;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Advice wrappers copy the code of blueprint methods to be executed before and/or after a matched method. To achieve this, a {@code static}
 * method of a class is annotated by {@link OnMethodEnter} and/or {@link OnMethodExit} and provided to an instance of this class.
 * </p>
 * <p>
 * A method that is annotated with {@link OnMethodEnter} can annotate its parameters with {@link Argument} where field access to this parameter
 * is substituted with access to the specified argument of the instrumented method. Alternatively, a parameter can be annotated by {@link This}
 * where the {@code this} reference of the instrumented method is read when the parameter is accessed. This mechanism can also be used to assign a
 * new value to the {@code this} reference of an instrumented method. If no annotation is used on a parameter, it is assigned the {@code n}-th
 * parameter of the instrumented method for the {@code n}-th parameter of the advice method. All parameters must declare the exact same type as
 * the parameters of the instrumented type or the method's declaring type for the {@link This} reference respectively if they are not marked as
 * <i>read-only</i>. In the latter case, it suffices that a parameter type is a super type of the corresponding type of the instrumented method.
 * </p>
 * <p>
 * A method that is annotated with {@link OnMethodExit} can equally annotate its parameters with {@link Argument} and {@link This}. Additionally,
 * it can annotate a parameter with {@link Return} to receive the original method's return value. By reassigning the return value, it is possible
 * to replace the returned value. If an instrumented method does not return a value, this annotation must not be used. If a method returns
 * exceptionally, the parameter is set to its default value, i.e. to {@code 0} for primitive types and to {@code null} for reference types. The
 * parameter's type must be equal to the instrumented method's return type if it is not set to <i>read-only</i> where it suffices to declare the
 * parameter type to be of any super type to the instrumented method's return type. An exception can be read by annotating a parameter of type
 * {@link Throwable} annotated with {@link Thrown} which is assigned the thrown {@link Throwable} or {@code null} if a method returns normally.
 * Doing so, it is possible to exchange a thrown exception with any checked or unchecked exception.Finally, if a method annotated with
 * {@link OnMethodEnter} exists and this method returns a value, this value can be accessed by a parameter annotated with {@link Enter}.
 * This parameter must declare the same type as type being returned by the method annotated with {@link OnMethodEnter}. If the parameter is marked
 * to be <i>read-only</i>, it suffices that the annotated parameter is of a super type of the return type of the method annotated by
 * {@link OnMethodEnter}. If no such method exists or this method returns {@code void}, no such parameter must be declared. Any return value
 * of a method that is annotated by {@link OnMethodExit} is discarded.
 * </p>
 * <p>
 * If any advice method throws an exception, the method is terminated prematurely. If the method annotated by {@link OnMethodEnter} throws an exception,
 * the method annotated by {@link OnMethodExit} method is not invoked. If the instrumented method throws an exception, the method that is annotated by
 * {@link OnMethodExit} is only invoked if the {@link OnMethodExit#onThrowable()} property is set to {@code true} what is the default. If this property
 * is set to {@code false}, the {@link Thrown} annotation must not be used on any parameter.
 * </p>
 * <p>
 * Byte Buddy does not assert the visibility of any types that are referenced within an inlined advice method. It is the responsibility of
 * the user of this class to assure that all types referenced within the advice methods are visible to the instrumented class. Failing to
 * do so results in a {@link IllegalAccessError} at the instrumented class's runtime.
 * </p>
 * <p>
 * Advice can be used either as a {@link AsmVisitorWrapper} where any declared methods of the currently instrumented type are enhanced without
 * replacing an existing implementation. Alternatively, advice can function as an {@link Implementation} where, by default, the original super
 * or default method of the instrumented method is invoked. If this is not possible or undesired, the delegate implementation can be changed
 * by specifying a wrapped implementation explicitly by {@link Advice#wrap(Implementation)}.
 * </p>
 * <p>
 * When using an advice class as a visitor wrapper, native or abstract methods which are silently skipped when advice matches such a method.
 * </p>
 * <p>
 * <b>Important</b>: Since Java 6, class files contain <i>stack map frames</i> embedded into a method's byte code. When advice methods are compiled
 * with a class file version less then Java 6 but are used for a class file that was compiled to Java 6 or newer, these stack map frames must be
 * computed by ASM by using the {@link ClassWriter#COMPUTE_FRAMES} option. If the advice methods do not contain any branching instructions, this is
 * not required. No action is required if the advice methods are at least compiled with Java 6 but are used on classes older than Java 6. This
 * limitation only applies to advice methods that are inlined. Also, it is the responsibility of this class's user to assure that the advice method
 * does not contain byte code constructs that are not supported by the class containing the instrumented method. In particular, pre Java-5
 * try-finally blocks cannot be inlined into classes with newer byte code levels as the <i>jsr</i> instruction was deprecated. Also, classes prior
 * to Java 7 do not support the <i>invokedynamic</i> command which must not be contained by an advice method if the instrumented method targets an
 * older class file format version.
 * </p>
 * <p>
 * <b>Note</b>: For the purpose of inlining, Java 5 and Java 6 byte code can be seen as the best candidate for advice methods. These versions do
 * no longer allow subroutines, neither do they already allow invokedynamic instructions or method handles. This way, Java 5 and Java 6 byte
 * code is compatible to both older and newer versions. One exception for backwards-incompatible byte code is the possibility to load type references
 * from the constant pool onto the operand stack. These instructions can however easily be transformed for classes compiled to Java 4 and older
 * by registering a {@link TypeConstantAdjustment} <b>before</b> the advice visitor.
 * </p>
 * <p>
 * <b>Note</b>: It is not possible to trigger break points in inlined advice methods as the debugging information of the inlined advice is not
 * preserved. It is not possible in Java to reference more than one source file per class what makes translating such debugging information
 * impossible. It is however possible to set break points in advice methods when invoking the original advice target. This allows debugging
 * of advice code within unit tests that invoke the advice method without instrumentation. As a consequence of not transferring debugging information,
 * the names of the parameters of an advice method do not matter when inlining, neither does any meta information on the advice method's body
 * such as annotations or parameter modifiers.
 * </p>
 * <p>
 * <b>Note</b>: The behavior of this component is undefined if it is supplied with invalid byte code what might result in runtime exceptions.
 * </p>
 * <p>
 * <b>Note</b>: When using advice from a Java agent with an {@link net.bytebuddy.agent.builder.AgentBuilder}, it often makes sense to not include
 * any library-specific code in the agent's jar file. For being able to locate the advice code in the context of the library dependencies, Byte
 * Buddy offers an {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer.ForAdvice} implementation that allows registering the agent's
 * class file locators for assembly of the advice class's description at runtime and with respect to the specific user dependencies.
 * </p>
 *
 * @see OnMethodEnter
 * @see OnMethodExit
 */
@HashCodeAndEqualsPlugin.Enhance
public class Advice implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper, Implementation {

    /**
     * Indicates that no class reader is available to an advice method.
     */
    @AlwaysNull
    private static final AsmClassReader UNDEFINED = null;

    /**
     * A reference to the {@link OnMethodEnter#skipOn()} method.
     */
    private static final MethodDescription.InDefinedShape SKIP_ON;

    /**
     * A reference to the {@link OnMethodEnter#skipOnIndex()} method.
     */
    private static final MethodDescription.InDefinedShape SKIP_ON_INDEX;

    /**
     * A reference to the {@link OnMethodEnter#prependLineNumber()} method.
     */
    private static final MethodDescription.InDefinedShape PREPEND_LINE_NUMBER;

    /**
     * A reference to the {@link OnMethodEnter#inline()} method.
     */
    private static final MethodDescription.InDefinedShape INLINE_ENTER;

    /**
     * A reference to the {@link OnMethodEnter#suppress()} method.
     */
    private static final MethodDescription.InDefinedShape SUPPRESS_ENTER;

    /**
     * A reference to the {@link OnMethodExit#repeatOn()} method.
     */
    private static final MethodDescription.InDefinedShape REPEAT_ON;

    /**
     * A reference to the {@link OnMethodExit#repeatOnIndex()} method.
     */
    private static final MethodDescription.InDefinedShape REPEAT_ON_INDEX;

    /**
     * A reference to the {@link OnMethodExit#onThrowable()} method.
     */
    private static final MethodDescription.InDefinedShape ON_THROWABLE;

    /**
     * A reference to the {@link OnMethodExit#backupArguments()} method.
     */
    private static final MethodDescription.InDefinedShape BACKUP_ARGUMENTS;

    /**
     * A reference to the {@link OnMethodExit#inline()} method.
     */
    private static final MethodDescription.InDefinedShape INLINE_EXIT;

    /**
     * A reference to the {@link OnMethodExit#suppress()} method.
     */
    private static final MethodDescription.InDefinedShape SUPPRESS_EXIT;

    /*
     * Extracts the annotation values for the enter and exit advice annotations.
     */
    static {
        MethodList<MethodDescription.InDefinedShape> enter = TypeDescription.ForLoadedType.of(OnMethodEnter.class).getDeclaredMethods();
        SKIP_ON = enter.filter(named("skipOn")).getOnly();
        SKIP_ON_INDEX = enter.filter(named("skipOnIndex")).getOnly();
        PREPEND_LINE_NUMBER = enter.filter(named("prependLineNumber")).getOnly();
        INLINE_ENTER = enter.filter(named("inline")).getOnly();
        SUPPRESS_ENTER = enter.filter(named("suppress")).getOnly();
        MethodList<MethodDescription.InDefinedShape> exit = TypeDescription.ForLoadedType.of(OnMethodExit.class).getDeclaredMethods();
        REPEAT_ON = exit.filter(named("repeatOn")).getOnly();
        REPEAT_ON_INDEX = exit.filter(named("repeatOnIndex")).getOnly();
        ON_THROWABLE = exit.filter(named("onThrowable")).getOnly();
        BACKUP_ARGUMENTS = exit.filter(named("backupArguments")).getOnly();
        INLINE_EXIT = exit.filter(named("inline")).getOnly();
        SUPPRESS_EXIT = exit.filter(named("suppress")).getOnly();
    }

    /**
     * The dispatcher for instrumenting the instrumented method upon entering.
     */
    private final Dispatcher.Resolved.ForMethodEnter methodEnter;

    /**
     * The dispatcher for instrumenting the instrumented method upon exiting.
     */
    private final Dispatcher.Resolved.ForMethodExit methodExit;

    /**
     * The assigner to use.
     */
    private final Assigner assigner;

    /**
     * The exception handler to apply.
     */
    private final ExceptionHandler exceptionHandler;

    /**
     * The delegate implementation to apply if this advice is used as an instrumentation.
     */
    private final Implementation delegate;

    /**
     * Creates a new advice.
     *
     * @param methodEnter The dispatcher for instrumenting the instrumented method upon entering.
     * @param methodExit  The dispatcher for instrumenting the instrumented method upon exiting.
     */
    protected Advice(Dispatcher.Resolved.ForMethodEnter methodEnter, Dispatcher.Resolved.ForMethodExit methodExit) {
        this(methodEnter, methodExit, Assigner.DEFAULT, ExceptionHandler.Default.SUPPRESSING, SuperMethodCall.INSTANCE);
    }

    /**
     * Creates a new advice.
     *
     * @param methodEnter      The dispatcher for instrumenting the instrumented method upon entering.
     * @param methodExit       The dispatcher for instrumenting the instrumented method upon exiting.
     * @param assigner         The assigner to use.
     * @param exceptionHandler The exception handler to apply.
     * @param delegate         The delegate implementation to apply if this advice is used as an instrumentation.
     */
    private Advice(Dispatcher.Resolved.ForMethodEnter methodEnter,
                   Dispatcher.Resolved.ForMethodExit methodExit,
                   Assigner assigner,
                   ExceptionHandler exceptionHandler,
                   Implementation delegate) {
        this.methodEnter = methodEnter;
        this.methodExit = methodExit;
        this.assigner = assigner;
        this.exceptionHandler = exceptionHandler;
        this.delegate = delegate;
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods. The advices binary representation is
     * accessed by querying the class loader of the supplied class for a class file.
     *
     * @param advice The type declaring the advice.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(Class<?> advice) {
        return to(advice, ClassFileLocator.ForClassLoader.of(advice.getClassLoader()));
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods.
     *
     * @param advice           The type declaring the advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(Class<?> advice, ClassFileLocator classFileLocator) {
        return to(TypeDescription.ForLoadedType.of(advice), classFileLocator);
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods. Using this method, a non-operational
     * class file locator is specified for the advice target. This implies that only advice targets with the <i>inline</i> target set
     * to {@code false} are resolvable by the returned instance.
     *
     * @param advice The type declaring the advice.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(TypeDescription advice) {
        return to(advice, ClassFileLocator.NoOp.INSTANCE);
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods.
     *
     * @param advice           A description of the type declaring the advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(TypeDescription advice, ClassFileLocator classFileLocator) {
        return to(advice,
                PostProcessor.NoOp.INSTANCE,
                classFileLocator,
                Collections.<OffsetMapping.Factory<?>>emptyList(),
                Delegator.ForRegularInvocation.Factory.INSTANCE,
                AsmClassReader.Factory.Default.IMPLICIT);
    }

    /**
     * Creates a new advice.
     *
     * @param advice               A description of the type declaring the advice.
     * @param postProcessorFactory The post processor factory to use.
     * @param classFileLocator     The class file locator for locating the advisory class's class file.
     * @param userFactories        A list of custom factories for user generated offset mappings.
     * @param delegatorFactory     The delegator factory to use.
     * @param classReaderFactory   The class reader factory to use.
     * @return A method visitor wrapper representing the supplied advice.
     */
    protected static Advice to(TypeDescription advice,
                               PostProcessor.Factory postProcessorFactory,
                               ClassFileLocator classFileLocator,
                               List<? extends OffsetMapping.Factory<?>> userFactories,
                               Delegator.Factory delegatorFactory,
                               AsmClassReader.Factory classReaderFactory) {
        Dispatcher.Unresolved methodEnter = Dispatcher.Inactive.INSTANCE, methodExit = Dispatcher.Inactive.INSTANCE;
        for (MethodDescription.InDefinedShape methodDescription : advice.getDeclaredMethods()) {
            methodEnter = locate(OnMethodEnter.class, INLINE_ENTER, methodEnter, methodDescription, delegatorFactory);
            methodExit = locate(OnMethodExit.class, INLINE_EXIT, methodExit, methodDescription, delegatorFactory);
        }
        if (!methodEnter.isAlive() && !methodExit.isAlive()) {
            throw new IllegalArgumentException("No advice defined by " + advice);
        }
        try {
            AsmClassReader classReader = methodEnter.isBinary() || methodExit.isBinary()
                    ? classReaderFactory.make(classFileLocator.locate(advice.getName()).resolve())
                    : UNDEFINED;
            return new Advice(methodEnter.asMethodEnter(userFactories, classReader, methodExit, postProcessorFactory),
                    methodExit.asMethodExit(userFactories, classReader, methodEnter, postProcessorFactory));
        } catch (IOException exception) {
            throw new IllegalStateException("Error reading class file of " + advice, exception);
        }
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods. The advices binary representation is
     * accessed by querying the class loader of the supplied class for a class file.
     *
     * @param enterAdvice The type declaring the enter advice.
     * @param exitAdvice  The type declaring the exit advice.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(Class<?> enterAdvice, Class<?> exitAdvice) {
        ClassLoader enterLoader = enterAdvice.getClassLoader(), exitLoader = exitAdvice.getClassLoader();
        return to(enterAdvice, exitAdvice, enterLoader == exitLoader
                ? ClassFileLocator.ForClassLoader.of(enterLoader)
                : new ClassFileLocator.Compound(ClassFileLocator.ForClassLoader.of(enterLoader), ClassFileLocator.ForClassLoader.of(exitLoader)));
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods.
     *
     * @param enterAdvice      The type declaring the enter advice.
     * @param exitAdvice       The type declaring the exit advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(Class<?> enterAdvice, Class<?> exitAdvice, ClassFileLocator classFileLocator) {
        return to(TypeDescription.ForLoadedType.of(enterAdvice), TypeDescription.ForLoadedType.of(exitAdvice), classFileLocator);
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods. Using this method, a non-operational
     * class file locator is specified for the advice target. This implies that only advice targets with the <i>inline</i> target set
     * to {@code false} are resolvable by the returned instance.
     *
     * @param enterAdvice The type declaring the enter advice.
     * @param exitAdvice  The type declaring the exit advice.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(TypeDescription enterAdvice, TypeDescription exitAdvice) {
        return to(enterAdvice, exitAdvice, ClassFileLocator.NoOp.INSTANCE);
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods.
     *
     * @param enterAdvice      The type declaring the enter advice.
     * @param exitAdvice       The type declaring the exit advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(TypeDescription enterAdvice, TypeDescription exitAdvice, ClassFileLocator classFileLocator) {
        return to(enterAdvice,
                exitAdvice,
                PostProcessor.NoOp.INSTANCE,
                classFileLocator,
                Collections.<OffsetMapping.Factory<?>>emptyList(),
                Delegator.ForRegularInvocation.Factory.INSTANCE,
                AsmClassReader.Factory.Default.IMPLICIT);
    }

    /**
     * Creates a new advice.
     *
     * @param enterAdvice          The type declaring the enter advice.
     * @param exitAdvice           The type declaring the exit advice.
     * @param postProcessorFactory The post processor factory to use.
     * @param classFileLocator     The class file locator for locating the advisory class's class file.
     * @param userFactories        A list of custom factories for user generated offset mappings.
     * @param delegatorFactory     The delegator factory to use.
     * @param classReaderFactory   The class reader factory to use.
     * @return A method visitor wrapper representing the supplied advice.
     */
    protected static Advice to(TypeDescription enterAdvice,
                               TypeDescription exitAdvice,
                               PostProcessor.Factory postProcessorFactory,
                               ClassFileLocator classFileLocator,
                               List<? extends OffsetMapping.Factory<?>> userFactories,
                               Delegator.Factory delegatorFactory,
                               AsmClassReader.Factory classReaderFactory) {
        Dispatcher.Unresolved methodEnter = Dispatcher.Inactive.INSTANCE, methodExit = Dispatcher.Inactive.INSTANCE;
        for (MethodDescription.InDefinedShape methodDescription : enterAdvice.getDeclaredMethods()) {
            methodEnter = locate(OnMethodEnter.class, INLINE_ENTER, methodEnter, methodDescription, delegatorFactory);
        }
        if (!methodEnter.isAlive()) {
            throw new IllegalArgumentException("No enter advice defined by " + enterAdvice);
        }
        for (MethodDescription.InDefinedShape methodDescription : exitAdvice.getDeclaredMethods()) {
            methodExit = locate(OnMethodExit.class, INLINE_EXIT, methodExit, methodDescription, delegatorFactory);
        }
        if (!methodExit.isAlive()) {
            throw new IllegalArgumentException("No exit advice defined by " + exitAdvice);
        }
        try {
            return new Advice(methodEnter.asMethodEnter(userFactories, methodEnter.isBinary()
                    ? classReaderFactory.make(classFileLocator.locate(enterAdvice.getName()).resolve())
                    : UNDEFINED, methodExit, postProcessorFactory), methodExit.asMethodExit(userFactories, methodExit.isBinary()
                    ? classReaderFactory.make(classFileLocator.locate(exitAdvice.getName()).resolve())
                    : UNDEFINED, methodEnter, postProcessorFactory));
        } catch (IOException exception) {
            throw new IllegalStateException("Error reading class file of " + enterAdvice + " or " + exitAdvice, exception);
        }
    }

    /**
     * Locates a dispatcher for the method if available.
     *
     * @param type              The annotation type that indicates a given form of advice that is currently resolved.
     * @param property          An annotation property that indicates if the advice method should be inlined.
     * @param dispatcher        Any previous dispatcher that was discovered or the previous dispatcher if found.
     * @param methodDescription The method description that is considered as an advice method.
     * @param delegatorFactory  The delegator factory to use.
     * @return A resolved dispatcher or the previous dispatcher if none was found.
     */
    private static Dispatcher.Unresolved locate(Class<? extends Annotation> type,
                                                MethodDescription.InDefinedShape property,
                                                Dispatcher.Unresolved dispatcher,
                                                MethodDescription.InDefinedShape methodDescription,
                                                Delegator.Factory delegatorFactory) {
        AnnotationDescription annotation = methodDescription.getDeclaredAnnotations().ofType(type);
        if (annotation == null) {
            return dispatcher;
        } else if (dispatcher.isAlive()) {
            throw new IllegalStateException("Duplicate advice for " + dispatcher + " and " + methodDescription);
        } else if (!methodDescription.isStatic()) {
            throw new IllegalStateException("Advice for " + methodDescription + " is not static");
        } else {
            return annotation.getValue(property).resolve(Boolean.class)
                    ? new Dispatcher.Inlining(methodDescription)
                    : new Dispatcher.Delegating(methodDescription, delegatorFactory);
        }
    }

    /**
     * Allows for the configuration of custom annotations that are then bound to a dynamically computed, constant value.
     *
     * @return A builder for an {@link Advice} instrumentation with custom values.
     * @see OffsetMapping.Factory
     */
    public static WithCustomMapping withCustomMapping() {
        return new WithCustomMapping();
    }

    /**
     * Returns an ASM visitor wrapper that matches the given matcher and applies this advice to the matched methods.
     *
     * @param matcher The matcher identifying methods to apply the advice to.
     * @return A suitable ASM visitor wrapper with the <i>compute frames</i> option enabled.
     */
    public AsmVisitorWrapper.ForDeclaredMethods on(ElementMatcher<? super MethodDescription> matcher) {
        return new AsmVisitorWrapper.ForDeclaredMethods().invokable(matcher, this);
    }

    /**
     * {@inheritDoc}
     */
    public MethodVisitor wrap(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              MethodVisitor methodVisitor,
                              Implementation.Context implementationContext,
                              TypePool typePool,
                              int writerFlags,
                              int readerFlags) {
        return instrumentedMethod.isAbstract() || instrumentedMethod.isNative()
                ? methodVisitor
                : doWrap(instrumentedType, instrumentedMethod, methodVisitor, implementationContext, writerFlags, readerFlags);
    }

    /**
     * Wraps the method visitor to implement this advice.
     *
     * @param instrumentedType      The instrumented type.
     * @param instrumentedMethod    The instrumented method.
     * @param methodVisitor         The method visitor to write to.
     * @param implementationContext The implementation context to use.
     * @param writerFlags           The ASM writer flags to use.
     * @param readerFlags           The ASM reader flags to use.
     * @return A method visitor that applies this advice.
     */
    protected MethodVisitor doWrap(TypeDescription instrumentedType,
                                   MethodDescription instrumentedMethod,
                                   MethodVisitor methodVisitor,
                                   Implementation.Context implementationContext,
                                   int writerFlags,
                                   int readerFlags) {
        if (methodEnter.isPrependLineNumber()) {
            methodVisitor = new LineNumberPrependingMethodVisitor(methodVisitor);
        }
        if (!methodExit.isAlive()) {
            return new AdviceVisitor.WithoutExitAdvice(methodVisitor,
                    implementationContext,
                    assigner,
                    exceptionHandler.resolve(instrumentedMethod, instrumentedType),
                    instrumentedType,
                    instrumentedMethod,
                    methodEnter,
                    writerFlags,
                    readerFlags);
        } else if (methodExit.getThrowable().represents(NoExceptionHandler.class)) {
            return new AdviceVisitor.WithExitAdvice.WithoutExceptionHandling(methodVisitor,
                    implementationContext,
                    assigner,
                    exceptionHandler.resolve(instrumentedMethod, instrumentedType),
                    instrumentedType,
                    instrumentedMethod,
                    methodEnter,
                    methodExit,
                    writerFlags,
                    readerFlags);
        } else if (instrumentedMethod.isConstructor()) {
            throw new IllegalStateException("Cannot catch exception during constructor call for " + instrumentedMethod);
        } else {
            return new AdviceVisitor.WithExitAdvice.WithExceptionHandling(methodVisitor,
                    implementationContext,
                    assigner,
                    exceptionHandler.resolve(instrumentedMethod, instrumentedType),
                    instrumentedType,
                    instrumentedMethod,
                    methodEnter,
                    methodExit,
                    writerFlags,
                    readerFlags,
                    methodExit.getThrowable());
        }
    }

    /**
     * {@inheritDoc}
     */
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return delegate.prepare(instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(this, implementationTarget, delegate.appender(implementationTarget));
    }

    /**
     * Configures this advice to use the specified assigner. Any previous or default assigner is replaced.
     *
     * @param assigner The assigner to use,
     * @return A version of this advice that uses the specified assigner.
     */
    public Advice withAssigner(Assigner assigner) {
        return new Advice(methodEnter, methodExit, assigner, exceptionHandler, delegate);
    }

    /**
     * Configures this advice to call {@link Throwable#printStackTrace()} upon a suppressed exception.
     *
     * @return A version of this advice that prints any suppressed exception.
     */
    public Advice withExceptionPrinting() {
        return withExceptionHandler(ExceptionHandler.Default.PRINTING);
    }

    /**
     * Configures this advice to execute the given stack manipulation upon a suppressed exception. The stack manipulation is executed with a
     * {@link Throwable} instance on the operand stack. The stack must be empty upon completing the exception handler.
     *
     * @param exceptionHandler The exception handler to apply.
     * @return A version of this advice that applies the supplied exception handler.
     */
    public Advice withExceptionHandler(StackManipulation exceptionHandler) {
        return withExceptionHandler(new ExceptionHandler.Simple(exceptionHandler));
    }

    /**
     * Configures this advice to execute the given exception handler upon a suppressed exception. The stack manipulation is executed with a
     * {@link Throwable} instance on the operand stack. The stack must be empty upon completing the exception handler.
     *
     * @param exceptionHandler The exception handler to apply.
     * @return A version of this advice that applies the supplied exception handler.
     */
    public Advice withExceptionHandler(ExceptionHandler exceptionHandler) {
        return new Advice(methodEnter, methodExit, assigner, exceptionHandler, delegate);
    }

    /**
     * Wraps the supplied implementation to have this advice applied around it.
     *
     * @param implementation The implementation to wrap.
     * @return An implementation that applies the supplied implementation and wraps it with this advice.
     */
    public Implementation wrap(Implementation implementation) {
        return new Advice(methodEnter, methodExit, assigner, exceptionHandler, implementation);
    }

    /**
     * Represents an offset mapping for an advice method to an alternative offset.
     */
    public interface OffsetMapping {

        /**
         * Resolves an offset mapping to a given target offset.
         *
         * @param instrumentedType   The instrumented type.
         * @param instrumentedMethod The instrumented method for which the mapping is to be resolved.
         * @param assigner           The assigner to use.
         * @param argumentHandler    The argument handler to use for resolving offsets of the local variable array of the instrumented method.
         * @param sort               The sort of the advice method being resolved.
         * @return A suitable target mapping.
         */
        Target resolve(TypeDescription instrumentedType,
                       MethodDescription instrumentedMethod,
                       Assigner assigner,
                       ArgumentHandler argumentHandler,
                       Sort sort);

        /**
         * A target offset of an offset mapping.
         */
        interface Target {

            /**
             * Resolves a read instruction.
             *
             * @return A stack manipulation that represents a reading of an advice parameter.
             */
            StackManipulation resolveRead();

            /**
             * Resolves a write instruction.
             *
             * @return A stack manipulation that represents a writing to an advice parameter.
             */
            StackManipulation resolveWrite();

            /**
             * Resolves an increment instruction.
             *
             * @param value The incrementation value.
             * @return A stack manipulation that represents a writing to an advice parameter.
             */
            StackManipulation resolveIncrement(int value);

            /**
             * An adapter class for a target that only can be read.
             */
            abstract class AbstractReadOnlyAdapter implements Target {

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveWrite() {
                    throw new IllegalStateException("Cannot write to read-only value");
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveIncrement(int value) {
                    throw new IllegalStateException("Cannot write to read-only value");
                }
            }

            /**
             * A target for an offset mapping that represents a non-operational value. All writes are discarded and a value's
             * default value is returned upon every read.
             */
            @HashCodeAndEqualsPlugin.Enhance
            abstract class ForDefaultValue implements Target {

                /**
                 * The represented type.
                 */
                protected final TypeDefinition typeDefinition;

                /**
                 * A stack manipulation to apply after a read instruction.
                 */
                protected final StackManipulation readAssignment;

                /**
                 * Creates a new target for a default value.
                 *
                 * @param typeDefinition The represented type.
                 * @param readAssignment A stack manipulation to apply after a read instruction.
                 */
                protected ForDefaultValue(TypeDefinition typeDefinition, StackManipulation readAssignment) {
                    this.typeDefinition = typeDefinition;
                    this.readAssignment = readAssignment;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveRead() {
                    return new StackManipulation.Compound(DefaultValue.of(typeDefinition), readAssignment);
                }

                /**
                 * A read-only target for a default value.
                 */
                public static class ReadOnly extends ForDefaultValue {

                    /**
                     * Creates a new writable target for a default value.
                     *
                     * @param typeDefinition The represented type.
                     */
                    public ReadOnly(TypeDefinition typeDefinition) {
                        this(typeDefinition, StackManipulation.Trivial.INSTANCE);
                    }

                    /**
                     * Creates a new -writable target for a default value.
                     *
                     * @param typeDefinition The represented type.
                     * @param readAssignment A stack manipulation to apply after a read instruction.
                     */
                    public ReadOnly(TypeDefinition typeDefinition, StackManipulation readAssignment) {
                        super(typeDefinition, readAssignment);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        throw new IllegalStateException("Cannot write to read-only default value");
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveIncrement(int value) {
                        throw new IllegalStateException("Cannot write to read-only default value");
                    }
                }

                /**
                 * A read-write target for a default value.
                 */
                public static class ReadWrite extends ForDefaultValue {

                    /**
                     * Creates a new read-only target for a default value.
                     *
                     * @param typeDefinition The represented type.
                     */
                    public ReadWrite(TypeDefinition typeDefinition) {
                        this(typeDefinition, StackManipulation.Trivial.INSTANCE);
                    }

                    /**
                     * Creates a new read-only target for a default value.
                     *
                     * @param typeDefinition The represented type.
                     * @param readAssignment A stack manipulation to apply after a read instruction.
                     */
                    public ReadWrite(TypeDefinition typeDefinition, StackManipulation readAssignment) {
                        super(typeDefinition, readAssignment);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        return Removal.of(typeDefinition);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveIncrement(int value) {
                        return StackManipulation.Trivial.INSTANCE;
                    }
                }
            }

            /**
             * A target for an offset mapping that represents a local variable.
             */
            @HashCodeAndEqualsPlugin.Enhance
            abstract class ForVariable implements Target {

                /**
                 * The represented type.
                 */
                protected final TypeDefinition typeDefinition;

                /**
                 * The value's offset.
                 */
                protected final int offset;

                /**
                 * An assignment to execute upon reading a value.
                 */
                protected final StackManipulation readAssignment;

                /**
                 * Creates a new target for a local variable mapping.
                 *
                 * @param typeDefinition The represented type.
                 * @param offset         The value's offset.
                 * @param readAssignment An assignment to execute upon reading a value.
                 */
                protected ForVariable(TypeDefinition typeDefinition, int offset, StackManipulation readAssignment) {
                    this.typeDefinition = typeDefinition;
                    this.offset = offset;
                    this.readAssignment = readAssignment;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveRead() {
                    return new StackManipulation.Compound(MethodVariableAccess.of(typeDefinition).loadFrom(offset), readAssignment);
                }

                /**
                 * A target for a read-only mapping of a local variable.
                 */
                public static class ReadOnly extends ForVariable {

                    /**
                     * Creates a read-only mapping for a local variable.
                     *
                     * @param typeDefinition The represented type.
                     * @param offset         The value's offset.
                     */
                    public ReadOnly(TypeDefinition typeDefinition, int offset) {
                        this(typeDefinition, offset, StackManipulation.Trivial.INSTANCE);
                    }

                    /**
                     * Creates a read-only mapping for a local variable.
                     *
                     * @param typeDefinition The represented type.
                     * @param offset         The value's offset.
                     * @param readAssignment An assignment to execute upon reading a value.
                     */
                    public ReadOnly(TypeDefinition typeDefinition, int offset, StackManipulation readAssignment) {
                        super(typeDefinition, offset, readAssignment);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        throw new IllegalStateException("Cannot write to read-only parameter " + typeDefinition + " at " + offset);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveIncrement(int value) {
                        throw new IllegalStateException("Cannot write to read-only variable " + typeDefinition + " at " + offset);
                    }
                }

                /**
                 * A target for a writable mapping of a local variable.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class ReadWrite extends ForVariable {

                    /**
                     * A stack manipulation to apply upon a write to the variable.
                     */
                    private final StackManipulation writeAssignment;

                    /**
                     * Creates a new target mapping for a writable local variable.
                     *
                     * @param typeDefinition The represented type.
                     * @param offset         The value's offset.
                     */
                    public ReadWrite(TypeDefinition typeDefinition, int offset) {
                        this(typeDefinition, offset, StackManipulation.Trivial.INSTANCE, StackManipulation.Trivial.INSTANCE);
                    }

                    /**
                     * Creates a new target mapping for a writable local variable.
                     *
                     * @param typeDefinition  The represented type.
                     * @param offset          The value's offset.
                     * @param readAssignment  An assignment to execute upon reading a value.
                     * @param writeAssignment A stack manipulation to apply upon a write to the variable.
                     */
                    public ReadWrite(TypeDefinition typeDefinition, int offset, StackManipulation readAssignment, StackManipulation writeAssignment) {
                        super(typeDefinition, offset, readAssignment);
                        this.writeAssignment = writeAssignment;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        return new StackManipulation.Compound(writeAssignment, MethodVariableAccess.of(typeDefinition).storeAt(offset));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveIncrement(int value) {
                        return typeDefinition.represents(int.class)
                                ? MethodVariableAccess.of(typeDefinition).increment(offset, value)
                                : new StackManipulation.Compound(resolveRead(), IntegerConstant.forValue(1), Addition.INTEGER, resolveWrite());
                    }
                }
            }

            /**
             * A target mapping for an array of all local variables.
             */
            @HashCodeAndEqualsPlugin.Enhance
            abstract class ForArray implements Target {

                /**
                 * The compound target type.
                 */
                protected final TypeDescription.Generic target;

                /**
                 * The stack manipulations to apply upon reading a variable array.
                 */
                protected final List<? extends StackManipulation> valueReads;

                /**
                 * Creates a new target mapping for an array of all local variables.
                 *
                 * @param target     The compound target type.
                 * @param valueReads The stack manipulations to apply upon reading a variable array.
                 */
                protected ForArray(TypeDescription.Generic target, List<? extends StackManipulation> valueReads) {
                    this.target = target;
                    this.valueReads = valueReads;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveRead() {
                    return ArrayFactory.forType(target).withValues(valueReads);
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveIncrement(int value) {
                    throw new IllegalStateException("Cannot increment read-only array value");
                }

                /**
                 * A target mapping for a read-only target mapping for an array of local variables.
                 */
                public static class ReadOnly extends ForArray {

                    /**
                     * Creates a read-only target mapping for an array of all local variables.
                     *
                     * @param target     The compound target type.
                     * @param valueReads The stack manipulations to apply upon reading a variable array.
                     */
                    public ReadOnly(TypeDescription.Generic target, List<? extends StackManipulation> valueReads) {
                        super(target, valueReads);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        throw new IllegalStateException("Cannot write to read-only array value");
                    }
                }

                /**
                 * A target mapping for a writable target mapping for an array of local variables.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class ReadWrite extends ForArray {

                    /**
                     * The stack manipulations to apply upon writing to a variable array.
                     */
                    private final List<? extends StackManipulation> valueWrites;

                    /**
                     * Creates a writable target mapping for an array of all local variables.
                     *
                     * @param target      The compound target type.
                     * @param valueReads  The stack manipulations to apply upon reading a variable array.
                     * @param valueWrites The stack manipulations to apply upon writing to a variable array.
                     */
                    public ReadWrite(TypeDescription.Generic target,
                                     List<? extends StackManipulation> valueReads,
                                     List<? extends StackManipulation> valueWrites) {
                        super(target, valueReads);
                        this.valueWrites = valueWrites;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        return new StackManipulation.Compound(ArrayAccess.of(target).forEach(valueWrites), Removal.SINGLE);
                    }
                }
            }

            /**
             * A target for an offset mapping that loads a field value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            abstract class ForField implements Target {

                /**
                 * The field value to load.
                 */
                protected final FieldDescription fieldDescription;

                /**
                 * The stack manipulation to apply upon a read.
                 */
                protected final StackManipulation readAssignment;

                /**
                 * Creates a new target for a field value mapping.
                 *
                 * @param fieldDescription The field value to load.
                 * @param readAssignment   The stack manipulation to apply upon a read.
                 */
                protected ForField(FieldDescription fieldDescription, StackManipulation readAssignment) {
                    this.fieldDescription = fieldDescription;
                    this.readAssignment = readAssignment;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveRead() {
                    return new StackManipulation.Compound(fieldDescription.isStatic()
                            ? StackManipulation.Trivial.INSTANCE
                            : MethodVariableAccess.loadThis(), FieldAccess.forField(fieldDescription).read(), readAssignment);
                }

                /**
                 * A read-only mapping for a field value.
                 */
                public static class ReadOnly extends ForField {

                    /**
                     * Creates a new read-only mapping for a field.
                     *
                     * @param fieldDescription The field value to load.
                     */
                    public ReadOnly(FieldDescription fieldDescription) {
                        this(fieldDescription, StackManipulation.Trivial.INSTANCE);
                    }

                    /**
                     * Creates a new read-only mapping for a field.
                     *
                     * @param fieldDescription The field value to load.
                     * @param readAssignment   The stack manipulation to apply upon a read.
                     */
                    public ReadOnly(FieldDescription fieldDescription, StackManipulation readAssignment) {
                        super(fieldDescription, readAssignment);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        throw new IllegalStateException("Cannot write to read-only field value");
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveIncrement(int value) {
                        throw new IllegalStateException("Cannot write to read-only field value");
                    }
                }

                /**
                 * A write-only mapping for a field value, typically to be used for constructors prior to invoking the super-constructor.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class WriteOnly implements Target {

                    /**
                     * The field value to load.
                     */
                    private final FieldDescription fieldDescription;

                    /**
                     * An assignment to apply prior to a field write.
                     */
                    private final StackManipulation writeAssignment;

                    /**
                     * Creates a write-only mapping for a field value.
                     *
                     * @param fieldDescription The field value to load.
                     * @param writeAssignment  An assignment to apply prior to a field write.
                     */
                    protected WriteOnly(FieldDescription fieldDescription, StackManipulation writeAssignment) {
                        this.fieldDescription = fieldDescription;
                        this.writeAssignment = writeAssignment;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveRead() {
                        throw new IllegalStateException("Cannot read write-only field value");
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        StackManipulation preparation;
                        if (fieldDescription.isStatic()) {
                            preparation = StackManipulation.Trivial.INSTANCE;
                        } else {
                            preparation = new StackManipulation.Compound(
                                    MethodVariableAccess.loadThis(),
                                    Duplication.SINGLE.flipOver(fieldDescription.getType()),
                                    Removal.SINGLE
                            );
                        }
                        return new StackManipulation.Compound(writeAssignment, preparation, FieldAccess.forField(fieldDescription).write());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveIncrement(int value) {
                        throw new IllegalStateException("Cannot increment write-only field value");
                    }
                }

                /**
                 * A mapping for a writable field.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class ReadWrite extends ForField {

                    /**
                     * An assignment to apply prior to a field write.
                     */
                    private final StackManipulation writeAssignment;

                    /**
                     * Creates a new target for a writable field.
                     *
                     * @param fieldDescription The field value to load.
                     */
                    public ReadWrite(FieldDescription fieldDescription) {
                        this(fieldDescription, StackManipulation.Trivial.INSTANCE, StackManipulation.Trivial.INSTANCE);
                    }

                    /**
                     * Creates a new target for a writable field.
                     *
                     * @param fieldDescription The field value to load.
                     * @param readAssignment   The stack manipulation to apply upon a read.
                     * @param writeAssignment  An assignment to apply prior to a field write.
                     */
                    public ReadWrite(FieldDescription fieldDescription, StackManipulation readAssignment, StackManipulation writeAssignment) {
                        super(fieldDescription, readAssignment);
                        this.writeAssignment = writeAssignment;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        StackManipulation preparation;
                        if (fieldDescription.isStatic()) {
                            preparation = StackManipulation.Trivial.INSTANCE;
                        } else {
                            preparation = new StackManipulation.Compound(
                                    MethodVariableAccess.loadThis(),
                                    Duplication.SINGLE.flipOver(fieldDescription.getType()),
                                    Removal.SINGLE
                            );
                        }
                        return new StackManipulation.Compound(writeAssignment, preparation, FieldAccess.forField(fieldDescription).write());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveIncrement(int value) {
                        return new StackManipulation.Compound(
                                resolveRead(),
                                IntegerConstant.forValue(value),
                                Addition.INTEGER,
                                resolveWrite()
                        );
                    }
                }
            }

            /**
             * A target for an offset mapping that represents a read-only stack manipulation.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForStackManipulation implements Target {

                /**
                 * The represented stack manipulation.
                 */
                private final StackManipulation stackManipulation;

                /**
                 * Creates a new target for an offset mapping for a stack manipulation.
                 *
                 * @param stackManipulation The represented stack manipulation.
                 */
                public ForStackManipulation(StackManipulation stackManipulation) {
                    this.stackManipulation = stackManipulation;
                }

                /**
                 * Creates a target for a {@link Method} or {@link Constructor} constant.
                 *
                 * @param methodDescription The method or constructor to represent.
                 * @return A mapping for a method or constructor constant.
                 */
                public static Target of(MethodDescription.InDefinedShape methodDescription) {
                    return new ForStackManipulation(MethodConstant.of(methodDescription));
                }

                /**
                 * Creates a target for an offset mapping for a type constant.
                 *
                 * @param typeDescription The type constant to represent.
                 * @return A mapping for a type constant.
                 */
                public static Target of(TypeDescription typeDescription) {
                    return new ForStackManipulation(ClassConstant.of(typeDescription));
                }

                /**
                 * Creates a target for an offset mapping for a constant value or {@code null}.
                 *
                 * @param value The constant value to represent or {@code null}.
                 * @return An appropriate target for an offset mapping.
                 */
                public static Target of(@MaybeNull Object value) {
                    return new ForStackManipulation(value == null
                            ? NullConstant.INSTANCE
                            : ConstantValue.Simple.wrap(value).toStackManipulation());
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveRead() {
                    return stackManipulation;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveWrite() {
                    throw new IllegalStateException("Cannot write to constant value: " + stackManipulation);
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolveIncrement(int value) {
                    throw new IllegalStateException("Cannot write to constant value: " + stackManipulation);
                }

                /**
                 * A constant value that can be written to.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class Writable implements Target {

                    /**
                     * The reading stack manipulation.
                     */
                    private final StackManipulation read;

                    /**
                     * The writing stack manipulation.
                     */
                    private final StackManipulation write;

                    /**
                     * Creates a writable target.
                     *
                     * @param read  The reading stack manipulation.
                     * @param write The writing stack manipulation.
                     */
                    public Writable(StackManipulation read, StackManipulation write) {
                        this.read = read;
                        this.write = write;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveRead() {
                        return read;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveWrite() {
                        return write;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation resolveIncrement(int value) {
                        throw new IllegalStateException("Cannot increment mutable constant value: " + write);
                    }
                }
            }
        }

        /**
         * Represents a factory for creating a {@link OffsetMapping} for a given parameter for a given annotation.
         *
         * @param <T> The annotation type that triggers this factory.
         */
        interface Factory<T extends Annotation> {

            /**
             * Returns the annotation type of this factory.
             *
             * @return The factory's annotation type.
             */
            Class<T> getAnnotationType();

            /**
             * Creates a new offset mapping for the supplied parameter if possible.
             *
             * @param target     The parameter description for which to resolve an offset mapping.
             * @param annotation The annotation that triggered this factory.
             * @param adviceType {@code true} if the binding is applied using advice method delegation.
             * @return A resolved offset mapping or {@code null} if no mapping can be resolved for this parameter.
             */
            OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType);

            /**
             * Describes the type of advice being applied.
             */
            enum AdviceType {

                /**
                 * Indicates advice where the invocation is delegated.
                 */
                DELEGATION(true),

                /**
                 * Indicates advice where the invocation's code is copied into the target method.
                 */
                INLINING(false);

                /**
                 * {@code true} if delegation is used.
                 */
                private final boolean delegation;

                /**
                 * Creates a new advice type.
                 *
                 * @param delegation {@code true} if delegation is used.
                 */
                AdviceType(boolean delegation) {
                    this.delegation = delegation;
                }

                /**
                 * Returns {@code true} if delegation is used.
                 *
                 * @return {@code true} if delegation is used.
                 */
                public boolean isDelegation() {
                    return delegation;
                }
            }

            /**
             * A simple factory that binds a constant offset mapping.
             *
             * @param <T> The annotation type that represents this offset mapping.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Simple<T extends Annotation> implements Factory<T> {

                /**
                 * The annotation type being bound.
                 */
                private final Class<T> annotationType;

                /**
                 * The fixed offset mapping.
                 */
                private final OffsetMapping offsetMapping;

                /**
                 * Creates a simple factory for a simple binding for an offset mapping.
                 *
                 * @param annotationType The annotation type being bound.
                 * @param offsetMapping  The fixed offset mapping.
                 */
                public Simple(Class<T> annotationType, OffsetMapping offsetMapping) {
                    this.annotationType = annotationType;
                    this.offsetMapping = offsetMapping;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    return offsetMapping;
                }
            }

            /**
             * A factory for an annotation whose use is not permitted.
             *
             * @param <T> The annotation type this factory binds.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Illegal<T extends Annotation> implements Factory<T> {

                /**
                 * The annotation type.
                 */
                private final Class<T> annotationType;

                /**
                 * Creates a factory that does not permit the usage of the represented annotation.
                 *
                 * @param annotationType The annotation type.
                 */
                public Illegal(Class<T> annotationType) {
                    this.annotationType = annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    throw new IllegalStateException("Usage of " + annotationType + " is not allowed on " + target);
                }
            }
        }

        /**
         * Describes the sort of the executed advice.
         */
        enum Sort {

            /**
             * Indicates that an offset is mapped for an enter advice.
             */
            ENTER {
                @Override
                public boolean isPremature(MethodDescription methodDescription) {
                    return methodDescription.isConstructor();
                }
            },

            /**
             * Indicates that an offset is mapped for an exit advice.
             */
            EXIT {
                @Override
                public boolean isPremature(MethodDescription methodDescription) {
                    return false;
                }
            };

            /**
             * Checks if an advice is executed in a premature state, i.e. the instrumented method is a constructor where the super constructor is not
             * yet invoked. In this case, the {@code this} reference is not yet initialized and therefore not available.
             *
             * @param methodDescription The instrumented method.
             * @return {@code true} if the advice is executed premature for the instrumented method.
             */
            public abstract boolean isPremature(MethodDescription methodDescription);
        }

        /**
         * An offset mapping for a given parameter of the instrumented method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        abstract class ForArgument implements OffsetMapping {

            /**
             * The type expected by the advice method.
             */
            protected final TypeDescription.Generic target;

            /**
             * Determines if the parameter is to be treated as read-only.
             */
            protected final boolean readOnly;

            /**
             * The typing to apply when assigning values.
             */
            private final Assigner.Typing typing;

            /**
             * Creates a new offset mapping for a parameter of the instrumented method.
             *
             * @param target   The type expected by the advice method.
             * @param readOnly Determines if the parameter is to be treated as read-only.
             * @param typing   The typing to apply.
             */
            protected ForArgument(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing) {
                this.target = target;
                this.readOnly = readOnly;
                this.typing = typing;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                ParameterDescription parameterDescription = resolve(instrumentedMethod);
                StackManipulation readAssignment = assigner.assign(parameterDescription.getType(), target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + parameterDescription + " to " + target);
                } else if (readOnly) {
                    return new Target.ForVariable.ReadOnly(parameterDescription.getType(), argumentHandler.argument(parameterDescription.getOffset()), readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, parameterDescription.getType(), typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + parameterDescription + " to " + target);
                    }
                    return new Target.ForVariable.ReadWrite(parameterDescription.getType(), argumentHandler.argument(parameterDescription.getOffset()), readAssignment,
                            writeAssignment);
                }
            }

            /**
             * Resolves the bound parameter.
             *
             * @param instrumentedMethod The instrumented method.
             * @return The bound parameter.
             */
            protected abstract ParameterDescription resolve(MethodDescription instrumentedMethod);

            /**
             * An offset mapping for a parameter of the instrumented method with a specific index.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class Unresolved extends ForArgument {

                /**
                 * The index of the parameter.
                 */
                private final int index;

                /**
                 * {@code true} if the parameter binding is optional.
                 */
                private final boolean optional;

                /**
                 * Creates a new offset binding for a parameter with a given index.
                 *
                 * @param target     The target type.
                 * @param annotation The annotation that triggers this binding.
                 */
                protected Unresolved(TypeDescription.Generic target, AnnotationDescription.Loadable<Argument> annotation) {
                    this(target, annotation.getValue(Factory.ARGUMENT_READ_ONLY).resolve(Boolean.class),
                            annotation.getValue(Factory.ARGUMENT_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                            annotation.getValue(Factory.ARGUMENT_VALUE).resolve(Integer.class),
                            annotation.getValue(Factory.ARGUMENT_OPTIONAL).resolve(Boolean.class));
                }

                /**
                 * Creates a new offset binding for a parameter with a given index.
                 *
                 * @param parameterDescription The parameter triggering this binding.
                 */
                protected Unresolved(ParameterDescription parameterDescription) {
                    this(parameterDescription.getType(), true, Assigner.Typing.STATIC, parameterDescription.getIndex());
                }

                /**
                 * Creates a non-optional offset binding for a parameter with a given index.
                 *
                 * @param target   The type expected by the advice method.
                 * @param readOnly Determines if the parameter is to be treated as read-only.
                 * @param typing   The typing to apply.
                 * @param index    The index of the parameter.
                 */
                public Unresolved(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, int index) {
                    this(target, readOnly, typing, index, false);
                }

                /**
                 * Creates a new offset binding for a parameter with a given index.
                 *
                 * @param target   The type expected by the advice method.
                 * @param readOnly Determines if the parameter is to be treated as read-only.
                 * @param typing   The typing to apply.
                 * @param index    The index of the parameter.
                 * @param optional {@code true} if the parameter binding is optional.
                 */
                public Unresolved(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, int index, boolean optional) {
                    super(target, readOnly, typing);
                    this.index = index;
                    this.optional = optional;
                }

                @Override
                protected ParameterDescription resolve(MethodDescription instrumentedMethod) {
                    ParameterList<?> parameters = instrumentedMethod.getParameters();
                    if (parameters.size() <= index) {
                        throw new IllegalStateException(instrumentedMethod + " does not define an index " + index);
                    } else {
                        return parameters.get(index);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Target resolve(TypeDescription instrumentedType,
                                      MethodDescription instrumentedMethod,
                                      Assigner assigner,
                                      ArgumentHandler argumentHandler,
                                      Sort sort) {
                    if (optional && instrumentedMethod.getParameters().size() <= index) {
                        return readOnly
                                ? new Target.ForDefaultValue.ReadOnly(target)
                                : new Target.ForDefaultValue.ReadWrite(target);
                    }
                    return super.resolve(instrumentedType, instrumentedMethod, assigner, argumentHandler, sort);
                }

                /**
                 * A factory for a mapping of a parameter of the instrumented method.
                 */
                protected enum Factory implements OffsetMapping.Factory<Argument> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * A description of the {@link Argument#value()} method.
                     */
                    private static final MethodDescription.InDefinedShape ARGUMENT_VALUE;

                    /**
                     * A description of the {@link Argument#readOnly()} method.
                     */
                    private static final MethodDescription.InDefinedShape ARGUMENT_READ_ONLY;

                    /**
                     * A description of the {@link Argument#typing()} method.
                     */
                    private static final MethodDescription.InDefinedShape ARGUMENT_TYPING;

                    /**
                     * A description of the {@link Argument#optional()} method.
                     */
                    private static final MethodDescription.InDefinedShape ARGUMENT_OPTIONAL;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(Argument.class).getDeclaredMethods();
                        ARGUMENT_VALUE = methods.filter(named("value")).getOnly();
                        ARGUMENT_READ_ONLY = methods.filter(named("readOnly")).getOnly();
                        ARGUMENT_TYPING = methods.filter(named("typing")).getOnly();
                        ARGUMENT_OPTIONAL = methods.filter(named("optional")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<Argument> getAnnotationType() {
                        return Argument.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<Argument> annotation,
                                              AdviceType adviceType) {
                        if (adviceType.isDelegation() && !annotation.getValue(ARGUMENT_READ_ONLY).resolve(Boolean.class)) {
                            throw new IllegalStateException("Cannot define writable field access for " + target + " when using delegation");
                        } else {
                            return new ForArgument.Unresolved(target.getType(), annotation);
                        }
                    }
                }
            }

            /**
             * An offset mapping for a specific parameter of the instrumented method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class Resolved extends ForArgument {

                /**
                 * The parameter being bound.
                 */
                private final ParameterDescription parameterDescription;

                /**
                 * Creates an offset mapping that binds a parameter of the instrumented method.
                 *
                 * @param target               The type expected by the advice method.
                 * @param readOnly             Determines if the parameter is to be treated as read-only.
                 * @param typing               The typing to apply.
                 * @param parameterDescription The parameter being bound.
                 */
                public Resolved(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, ParameterDescription parameterDescription) {
                    super(target, readOnly, typing);
                    this.parameterDescription = parameterDescription;
                }

                @Override
                protected ParameterDescription resolve(MethodDescription instrumentedMethod) {
                    if (!parameterDescription.getDeclaringMethod().equals(instrumentedMethod)) {
                        throw new IllegalStateException(parameterDescription + " is not a parameter of " + instrumentedMethod);
                    }
                    return parameterDescription;
                }

                /**
                 * A factory for a parameter argument of the instrumented method.
                 *
                 * @param <T> The type of the bound annotation.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class Factory<T extends Annotation> implements OffsetMapping.Factory<T> {

                    /**
                     * The annotation type.
                     */
                    private final Class<T> annotationType;

                    /**
                     * The bound parameter.
                     */
                    private final ParameterDescription parameterDescription;

                    /**
                     * {@code true} if the factory should create a read-only binding.
                     */
                    private final boolean readOnly;

                    /**
                     * The typing to use.
                     */
                    private final Assigner.Typing typing;

                    /**
                     * Creates a new factory for binding a parameter of the instrumented method with read-only semantics and static typing.
                     *
                     * @param annotationType       The annotation type.
                     * @param parameterDescription The bound parameter.
                     */
                    public Factory(Class<T> annotationType, ParameterDescription parameterDescription) {
                        this(annotationType, parameterDescription, true, Assigner.Typing.STATIC);
                    }

                    /**
                     * Creates a new factory for binding a parameter of the instrumented method.
                     *
                     * @param annotationType       The annotation type.
                     * @param parameterDescription The bound parameter.
                     * @param readOnly             {@code true} if the factory should create a read-only binding.
                     * @param typing               The typing to use.
                     */
                    public Factory(Class<T> annotationType, ParameterDescription parameterDescription, boolean readOnly, Assigner.Typing typing) {
                        this.annotationType = annotationType;
                        this.parameterDescription = parameterDescription;
                        this.readOnly = readOnly;
                        this.typing = typing;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<T> getAnnotationType() {
                        return annotationType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<T> annotation,
                                              AdviceType adviceType) {
                        return new Resolved(target.getType(), readOnly, typing, parameterDescription);
                    }
                }
            }
        }

        /**
         * An offset mapping that provides access to the {@code this} reference of the instrumented method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForThisReference implements OffsetMapping {

            /**
             * The type that the advice method expects for the {@code this} reference.
             */
            private final TypeDescription.Generic target;

            /**
             * Determines if the parameter is to be treated as read-only.
             */
            private final boolean readOnly;

            /**
             * The typing to apply.
             */
            private final Assigner.Typing typing;

            /**
             * {@code true} if the parameter should be bound to {@code null} if the instrumented method is static.
             */
            private final boolean optional;

            /**
             * Creates a new offset mapping for a {@code this} reference.
             *
             * @param target     The type that the advice method expects for the {@code this} reference.
             * @param annotation The mapped annotation.
             */
            protected ForThisReference(TypeDescription.Generic target, AnnotationDescription.Loadable<This> annotation) {
                this(target,
                        annotation.getValue(Factory.THIS_READ_ONLY).resolve(Boolean.class),
                        annotation.getValue(Factory.THIS_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                        annotation.getValue(Factory.THIS_OPTIONAL).resolve(Boolean.class));
            }

            /**
             * Creates a new offset mapping for a {@code this} reference.
             *
             * @param target   The type that the advice method expects for the {@code this} reference.
             * @param readOnly Determines if the parameter is to be treated as read-only.
             * @param typing   The typing to apply.
             * @param optional {@code true} if the parameter should be bound to {@code null} if the instrumented method is static.
             */
            public ForThisReference(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, boolean optional) {
                this.target = target;
                this.readOnly = readOnly;
                this.typing = typing;
                this.optional = optional;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                if (instrumentedMethod.isStatic() || sort.isPremature(instrumentedMethod)) {
                    if (optional) {
                        return readOnly
                                ? new Target.ForDefaultValue.ReadOnly(instrumentedType)
                                : new Target.ForDefaultValue.ReadWrite(instrumentedType);
                    } else {
                        throw new IllegalStateException("Cannot map this reference for static method or constructor start: " + instrumentedMethod);
                    }
                }
                StackManipulation readAssignment = assigner.assign(instrumentedType.asGenericType(), target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + instrumentedType + " to " + target);
                } else if (readOnly) {
                    return new Target.ForVariable.ReadOnly(instrumentedType.asGenericType(), argumentHandler.argument(ArgumentHandler.THIS_REFERENCE), readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, instrumentedType.asGenericType(), typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to " + instrumentedType);
                    }
                    return new Target.ForVariable.ReadWrite(instrumentedType.asGenericType(), argumentHandler.argument(ArgumentHandler.THIS_REFERENCE), readAssignment,
                            writeAssignment);
                }
            }

            /**
             * A factory for creating a {@link ForThisReference} offset mapping.
             */
            protected enum Factory implements OffsetMapping.Factory<This> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A description of the {@link This#readOnly()} method.
                 */
                private static final MethodDescription.InDefinedShape THIS_READ_ONLY;

                /**
                 * A description of the {@link This#typing()} method.
                 */
                private static final MethodDescription.InDefinedShape THIS_TYPING;

                /**
                 * A description of the {@link This#optional()} method.
                 */
                private static final MethodDescription.InDefinedShape THIS_OPTIONAL;

                /*
                 * Resolves annotation properties.
                 */
                static {
                    MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(This.class).getDeclaredMethods();
                    THIS_READ_ONLY = methods.filter(named("readOnly")).getOnly();
                    THIS_TYPING = methods.filter(named("typing")).getOnly();
                    THIS_OPTIONAL = methods.filter(named("optional")).getOnly();
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<This> getAnnotationType() {
                    return This.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<This> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.getValue(THIS_READ_ONLY).resolve(Boolean.class)) {
                        throw new IllegalStateException("Cannot write to this reference for " + target + " in read-only context");
                    } else {
                        return new ForThisReference(target.getType(), annotation);
                    }
                }
            }
        }

        /**
         * An offset mapping that maps an array containing all arguments of the instrumented method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForAllArguments implements OffsetMapping {

            /**
             * The component target type.
             */
            private final TypeDescription.Generic target;

            /**
             * {@code true} if the array is read-only.
             */
            private final boolean readOnly;

            /**
             * The typing to apply.
             */
            private final Assigner.Typing typing;

            /**
             * {@code true} if the method's target instance should be considered an element of the produced array.
             */
            private final boolean includeSelf;

            /**
             * {@code true} if a {@code null} value should be assigned if the
             * instrumented method does not declare any parameters.
             */
            private final boolean nullIfEmpty;

            /**
             * Creates a new offset mapping for an array containing all arguments.
             *
             * @param target     The component target type.
             * @param annotation The mapped annotation.
             */
            protected ForAllArguments(TypeDescription.Generic target, AnnotationDescription.Loadable<AllArguments> annotation) {
                this(target, annotation.getValue(Factory.ALL_ARGUMENTS_READ_ONLY).resolve(Boolean.class),
                        annotation.getValue(Factory.ALL_ARGUMENTS_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                        annotation.getValue(Factory.ALL_ARGUMENTS_INCLUDE_SELF).resolve(Boolean.class),
                        annotation.getValue(Factory.ALL_ARGUMENTS_NULL_IF_EMPTY).resolve(Boolean.class));
            }

            /**
             * Creates a new offset mapping for an array containing all arguments.
             *
             * @param target      The component target type.
             * @param readOnly    {@code true} if the array is read-only.
             * @param typing      The typing to apply.
             * @param includeSelf {@code true} if the method's target instance should be considered
             *                    an element of the produced array.
             * @param nullIfEmpty {@code true} if a {@code null} value should be assigned if the
             *                    instrumented method does not declare any parameters.
             */
            public ForAllArguments(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, boolean includeSelf, boolean nullIfEmpty) {
                this.target = target;
                this.readOnly = readOnly;
                this.typing = typing;
                this.includeSelf = includeSelf;
                this.nullIfEmpty = nullIfEmpty;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                if (nullIfEmpty && instrumentedMethod.getParameters().isEmpty() && (!includeSelf || instrumentedMethod.isStatic())) {
                    return readOnly
                            ? new Target.ForStackManipulation(NullConstant.INSTANCE)
                            : new Target.ForStackManipulation.Writable(NullConstant.INSTANCE, Removal.SINGLE);
                }
                List<StackManipulation> reads = new ArrayList<StackManipulation>((includeSelf && !instrumentedMethod.isStatic()
                        ? 1
                        : 0) + instrumentedMethod.getParameters().size());
                if (includeSelf && !instrumentedMethod.isStatic()) {
                    if (sort.isPremature(instrumentedMethod) && instrumentedMethod.isConstructor()) {
                        throw new IllegalStateException("Cannot include self in all arguments array from " + instrumentedMethod);
                    }
                    StackManipulation assignment = assigner.assign(instrumentedMethod.getDeclaringType().asGenericType(), target, typing);
                    if (!assignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + instrumentedMethod.getDeclaringType() + " to " + target);
                    }
                    reads.add(new StackManipulation.Compound(
                            MethodVariableAccess.REFERENCE.loadFrom(argumentHandler.argument(ArgumentHandler.THIS_REFERENCE)),
                            assignment));
                }
                for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                    StackManipulation assignment = assigner.assign(parameterDescription.getType(), target, typing);
                    if (!assignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + parameterDescription + " to " + target);
                    }
                    reads.add(new StackManipulation.Compound(
                            MethodVariableAccess.of(parameterDescription.getType()).loadFrom(argumentHandler.argument(parameterDescription.getOffset())),
                            assignment));
                }
                if (readOnly) {
                    return new Target.ForArray.ReadOnly(target, reads);
                } else {
                    List<StackManipulation> writes = new ArrayList<StackManipulation>(2 * ((includeSelf && !instrumentedMethod.isStatic()
                            ? 1
                            : 0) + instrumentedMethod.getParameters().size()));
                    if (includeSelf && !instrumentedMethod.isStatic()) {
                        StackManipulation assignment = assigner.assign(target, instrumentedMethod.getDeclaringType().asGenericType(), typing);
                        if (!assignment.isValid()) {
                            throw new IllegalStateException("Cannot assign " + target + " to " + instrumentedMethod.getDeclaringType());
                        }
                        writes.add(new StackManipulation.Compound(
                                assignment,
                                MethodVariableAccess.REFERENCE.storeAt(argumentHandler.argument(ArgumentHandler.THIS_REFERENCE))));
                    }
                    for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                        StackManipulation assignment = assigner.assign(target, parameterDescription.getType(), typing);
                        if (!assignment.isValid()) {
                            throw new IllegalStateException("Cannot assign " + target + " to " + parameterDescription);
                        }
                        writes.add(new StackManipulation.Compound(
                                assignment,
                                MethodVariableAccess.of(parameterDescription.getType()).storeAt(argumentHandler.argument(parameterDescription.getOffset()))));
                    }
                    return new Target.ForArray.ReadWrite(target, reads, writes);
                }
            }

            /**
             * A factory for an offset mapping that maps all arguments values of the instrumented method.
             */
            protected enum Factory implements OffsetMapping.Factory<AllArguments> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A description of the {@link AllArguments#readOnly()} method.
                 */
                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_READ_ONLY;

                /**
                 * A description of the {@link AllArguments#typing()} method.
                 */
                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_TYPING;

                /**
                 * A description of the {@link AllArguments#includeSelf()} method.
                 */
                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_INCLUDE_SELF;

                /**
                 * A description of the {@link AllArguments#nullIfEmpty()} method.
                 */
                private static final MethodDescription.InDefinedShape ALL_ARGUMENTS_NULL_IF_EMPTY;

                /*
                 * Resolves annotation properties.
                 */
                static {
                    MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(AllArguments.class).getDeclaredMethods();
                    ALL_ARGUMENTS_READ_ONLY = methods.filter(named("readOnly")).getOnly();
                    ALL_ARGUMENTS_TYPING = methods.filter(named("typing")).getOnly();
                    ALL_ARGUMENTS_INCLUDE_SELF = methods.filter(named("includeSelf")).getOnly();
                    ALL_ARGUMENTS_NULL_IF_EMPTY = methods.filter(named("nullIfEmpty")).getOnly();
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<AllArguments> getAnnotationType() {
                    return AllArguments.class;
                }

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<AllArguments> annotation,
                                          AdviceType adviceType) {
                    if (!target.getType().represents(Object.class) && !target.getType().isArray()) {
                        throw new IllegalStateException("Cannot use AllArguments annotation on a non-array type");
                    } else if (adviceType.isDelegation() && !annotation.getValue(ALL_ARGUMENTS_READ_ONLY).resolve(Boolean.class)) {
                        throw new IllegalStateException("Cannot define writable field access for " + target);
                    } else {
                        return new ForAllArguments(target.getType().represents(Object.class)
                                ? TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)
                                : target.getType().getComponentType(), annotation);
                    }
                }
            }
        }

        /**
         * Maps the declaring type of the instrumented method.
         */
        enum ForInstrumentedType implements OffsetMapping {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                return Target.ForStackManipulation.of(instrumentedType);
            }
        }

        /**
         * Maps a constant representing the instrumented method.
         */
        enum ForInstrumentedMethod implements OffsetMapping {

            /**
             * A constant that must be a {@link Method} instance.
             */
            METHOD {
                @Override
                protected boolean isRepresentable(MethodDescription instrumentedMethod) {
                    return instrumentedMethod.isMethod();
                }

                @Override
                protected Target resolve(MethodDescription.InDefinedShape methodDescription) {
                    return Target.ForStackManipulation.of(methodDescription);
                }
            },

            /**
             * A constant that must be a {@link Constructor} instance.
             */
            CONSTRUCTOR {
                @Override
                protected boolean isRepresentable(MethodDescription instrumentedMethod) {
                    return instrumentedMethod.isConstructor();
                }

                @Override
                protected Target resolve(MethodDescription.InDefinedShape methodDescription) {
                    return Target.ForStackManipulation.of(methodDescription);
                }
            },

            /**
             * A constant that must be a {@code java.lang.reflect.Executable} instance.
             */
            EXECUTABLE {
                @Override
                protected boolean isRepresentable(MethodDescription instrumentedMethod) {
                    return true;
                }

                @Override
                protected Target resolve(MethodDescription.InDefinedShape methodDescription) {
                    return Target.ForStackManipulation.of(methodDescription);
                }
            },


            /**
             * A constant that must be a {@code java.lang.invoke.MethodHandle} instance.
             */
            METHOD_HANDLE {
                @Override
                protected boolean isRepresentable(MethodDescription instrumentedMethod) {
                    return true;
                }

                @Override
                protected Target resolve(MethodDescription.InDefinedShape methodDescription) {
                    return new Target.ForStackManipulation(JavaConstant.MethodHandle.of(methodDescription).toStackManipulation());
                }
            },

            /**
             * A constant that must be a {@code java.lang.invoke.MethodType} instance.
             */
            METHOD_TYPE {
                @Override
                protected boolean isRepresentable(MethodDescription instrumentedMethod) {
                    return true;
                }

                @Override
                protected Target resolve(MethodDescription.InDefinedShape methodDescription) {
                    return new Target.ForStackManipulation(JavaConstant.MethodType.of(methodDescription).toStackManipulation());
                }
            };

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                if (!isRepresentable(instrumentedMethod)) {
                    throw new IllegalStateException("Cannot represent " + instrumentedMethod + " as the specified constant");
                }
                return resolve(instrumentedMethod.asDefined());
            }

            /**
             * Checks if the supplied method is representable for the assigned offset mapping.
             *
             * @param instrumentedMethod The instrumented method to represent.
             * @return {@code true} if this method is representable.
             */
            protected abstract boolean isRepresentable(MethodDescription instrumentedMethod);

            /**
             * Resolves the target for a given method description.
             *
             * @param methodDescription The method description for which to resolve a constant.
             * @return A suitable target.
             */
            protected abstract Target resolve(MethodDescription.InDefinedShape methodDescription);
        }

        /**
         * An offset mapping for a field.
         */
        @HashCodeAndEqualsPlugin.Enhance
        abstract class ForField implements OffsetMapping {

            /**
             * The {@link FieldValue#value()} method.
             */
            private static final MethodDescription.InDefinedShape FIELD_VALUE;

            /**
             * The {@link FieldValue#declaringType()}} method.
             */
            private static final MethodDescription.InDefinedShape FIELD_DECLARING_TYPE;

            /**
             * The {@link FieldValue#readOnly()}} method.
             */
            private static final MethodDescription.InDefinedShape FIELD_READ_ONLY;

            /**
             * The {@link FieldValue#typing()}} method.
             */
            private static final MethodDescription.InDefinedShape FIELD_TYPING;

            /*
             * Looks up all annotation properties to avoid loading of the declaring field type.
             */
            static {
                MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(FieldValue.class).getDeclaredMethods();
                FIELD_VALUE = methods.filter(named("value")).getOnly();
                FIELD_DECLARING_TYPE = methods.filter(named("declaringType")).getOnly();
                FIELD_READ_ONLY = methods.filter(named("readOnly")).getOnly();
                FIELD_TYPING = methods.filter(named("typing")).getOnly();
            }

            /**
             * The expected type that the field can be assigned to.
             */
            private final TypeDescription.Generic target;

            /**
             * {@code true} if this mapping is read-only.
             */
            private final boolean readOnly;

            /**
             * The typing to apply.
             */
            private final Assigner.Typing typing;

            /**
             * Creates an offset mapping for a field.
             *
             * @param target   The target type.
             * @param readOnly {@code true} if this mapping is read-only.
             * @param typing   The typing to apply.
             */
            protected ForField(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing) {
                this.target = target;
                this.readOnly = readOnly;
                this.typing = typing;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                FieldDescription fieldDescription = resolve(instrumentedType, instrumentedMethod);
                if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot access non-static field " + fieldDescription + " from static method " + instrumentedMethod);
                }
                if (sort.isPremature(instrumentedMethod) && !fieldDescription.isStatic()) {
                    if (readOnly) {
                        throw new IllegalStateException("Cannot read " + fieldDescription + " before super constructor call");
                    } else {
                        StackManipulation writeAssignment = assigner.assign(target, fieldDescription.getType(), typing);
                        if (!writeAssignment.isValid()) {
                            throw new IllegalStateException("Cannot assign " + target + " to " + fieldDescription);
                        }
                        return new Target.ForField.WriteOnly(fieldDescription.asDefined(), writeAssignment);
                    }
                } else {
                    StackManipulation readAssignment = assigner.assign(fieldDescription.getType(), target, typing);
                    if (!readAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + fieldDescription + " to " + target);
                    } else if (readOnly) {
                        return new Target.ForField.ReadOnly(fieldDescription, readAssignment);
                    } else {
                        StackManipulation writeAssignment = assigner.assign(target, fieldDescription.getType(), typing);
                        if (!writeAssignment.isValid()) {
                            throw new IllegalStateException("Cannot assign " + target + " to " + fieldDescription);
                        }
                        return new Target.ForField.ReadWrite(fieldDescription.asDefined(), readAssignment, writeAssignment);
                    }
                }
            }

            /**
             * Resolves the field being bound.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @return The field being bound.
             */
            protected abstract FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

            /**
             * An offset mapping for a field that is resolved from the instrumented type by its name.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public abstract static class Unresolved extends ForField {

                /**
                 * Indicates that a name should be extracted from an accessor method.
                 */
                protected static final String BEAN_PROPERTY = "";

                /**
                 * The name of the field.
                 */
                private final String name;

                /**
                 * Creates an offset mapping for a field that is not yet resolved.
                 *
                 * @param target   The target type.
                 * @param readOnly {@code true} if this mapping is read-only.
                 * @param typing   The typing to apply.
                 * @param name     The name of the field.
                 */
                protected Unresolved(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, String name) {
                    super(target, readOnly, typing);
                    this.name = name;
                }

                @Override
                protected FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    FieldLocator locator = fieldLocator(instrumentedType);
                    FieldLocator.Resolution resolution = name.equals(BEAN_PROPERTY)
                            ? FieldLocator.Resolution.Simple.ofBeanAccessor(locator, instrumentedMethod)
                            : locator.locate(name);
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Cannot locate field named " + name + " for " + instrumentedType);
                    } else {
                        return resolution.getField();
                    }
                }

                /**
                 * Returns a field locator for this instance.
                 *
                 * @param instrumentedType The instrumented type.
                 * @return An appropriate field locator.
                 */
                protected abstract FieldLocator fieldLocator(TypeDescription instrumentedType);

                /**
                 * An offset mapping for a field with an implicit declaring type.
                 */
                public static class WithImplicitType extends Unresolved {

                    /**
                     * Creates an offset mapping for a field with an implicit declaring type.
                     *
                     * @param target     The target type.
                     * @param annotation The annotation to represent.
                     */
                    protected WithImplicitType(TypeDescription.Generic target, AnnotationDescription.Loadable<FieldValue> annotation) {
                        this(target,
                                annotation.getValue(FIELD_READ_ONLY).resolve(Boolean.class),
                                annotation.getValue(FIELD_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                annotation.getValue(FIELD_VALUE).resolve(String.class));
                    }

                    /**
                     * Creates an offset mapping for a field with an implicit declaring type.
                     *
                     * @param target   The target type.
                     * @param name     The name of the field.
                     * @param readOnly {@code true} if the field is read-only.
                     * @param typing   The typing to apply.
                     */
                    public WithImplicitType(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, String name) {
                        super(target, readOnly, typing, name);
                    }

                    @Override
                    protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                        return new FieldLocator.ForClassHierarchy(instrumentedType);
                    }
                }

                /**
                 * An offset mapping for a field with an explicit declaring type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class WithExplicitType extends Unresolved {

                    /**
                     * The type declaring the field.
                     */
                    private final TypeDescription declaringType;

                    /**
                     * Creates an offset mapping for a field with an explicit declaring type.
                     *
                     * @param target        The target type.
                     * @param annotation    The annotation to represent.
                     * @param declaringType The field's declaring type.
                     */
                    protected WithExplicitType(TypeDescription.Generic target,
                                               AnnotationDescription.Loadable<FieldValue> annotation,
                                               TypeDescription declaringType) {
                        this(target,
                                annotation.getValue(FIELD_READ_ONLY).resolve(Boolean.class),
                                annotation.getValue(FIELD_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                annotation.getValue(FIELD_VALUE).resolve(String.class),
                                declaringType);
                    }

                    /**
                     * Creates an offset mapping for a field with an explicit declaring type.
                     *
                     * @param target        The target type.
                     * @param name          The name of the field.
                     * @param readOnly      {@code true} if the field is read-only.
                     * @param typing        The typing to apply.
                     * @param declaringType The field's declaring type.
                     */
                    public WithExplicitType(TypeDescription.Generic target,
                                            boolean readOnly,
                                            Assigner.Typing typing,
                                            String name,
                                            TypeDescription declaringType) {
                        super(target, readOnly, typing, name);
                        this.declaringType = declaringType;
                    }

                    @Override
                    protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                        if (!declaringType.represents(TargetType.class) && !instrumentedType.isAssignableTo(declaringType)) {
                            throw new IllegalStateException(declaringType + " is no super type of " + instrumentedType);
                        }
                        return new FieldLocator.ForExactType(TargetType.resolve(declaringType, instrumentedType));
                    }
                }

                /**
                 * A factory for a {@link Unresolved} offset mapping.
                 */
                protected enum Factory implements OffsetMapping.Factory<FieldValue> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public Class<FieldValue> getAnnotationType() {
                        return FieldValue.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<FieldValue> annotation,
                                              AdviceType adviceType) {
                        if (adviceType.isDelegation() && !annotation.getValue(ForField.FIELD_READ_ONLY).resolve(Boolean.class)) {
                            throw new IllegalStateException("Cannot write to field for " + target + " in read-only context");
                        } else {
                            TypeDescription declaringType = annotation.getValue(FIELD_DECLARING_TYPE).resolve(TypeDescription.class);
                            return declaringType.represents(void.class)
                                    ? new WithImplicitType(target.getType(), annotation)
                                    : new WithExplicitType(target.getType(), annotation, declaringType);
                        }
                    }
                }
            }

            /**
             * A binding for an offset mapping that represents a specific field.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class Resolved extends ForField {

                /**
                 * The accessed field.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a resolved offset mapping for a field.
                 *
                 * @param target           The target type.
                 * @param readOnly         {@code true} if this mapping is read-only.
                 * @param typing           The typing to apply.
                 * @param fieldDescription The accessed field.
                 */
                public Resolved(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, FieldDescription fieldDescription) {
                    super(target, readOnly, typing);
                    this.fieldDescription = fieldDescription;
                }

                @Override
                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming declaring type for type member.")
                protected FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    if (!fieldDescription.isStatic() && !fieldDescription.getDeclaringType().asErasure().isAssignableFrom(instrumentedType)) {
                        throw new IllegalStateException(fieldDescription + " is no member of " + instrumentedType);
                    } else if (!fieldDescription.isVisibleTo(instrumentedType)) {
                        throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                    }
                    return fieldDescription;
                }

                /**
                 * A factory that binds a field.
                 *
                 * @param <T> The annotation type this factory binds.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class Factory<T extends Annotation> implements OffsetMapping.Factory<T> {

                    /**
                     * The annotation type.
                     */
                    private final Class<T> annotationType;

                    /**
                     * The field to be bound.
                     */
                    private final FieldDescription fieldDescription;

                    /**
                     * {@code true} if this factory should create a read-only binding.
                     */
                    private final boolean readOnly;

                    /**
                     * The typing to use.
                     */
                    private final Assigner.Typing typing;

                    /**
                     * Creates a new factory for binding a specific field with read-only semantics and static typing.
                     *
                     * @param annotationType   The annotation type.
                     * @param fieldDescription The field to bind.
                     */
                    public Factory(Class<T> annotationType, FieldDescription fieldDescription) {
                        this(annotationType, fieldDescription, true, Assigner.Typing.STATIC);
                    }

                    /**
                     * Creates a new factory for binding a specific field.
                     *
                     * @param annotationType   The annotation type.
                     * @param fieldDescription The field to bind.
                     * @param readOnly         {@code true} if this factory should create a read-only binding.
                     * @param typing           The typing to use.
                     */
                    public Factory(Class<T> annotationType, FieldDescription fieldDescription, boolean readOnly, Assigner.Typing typing) {
                        this.annotationType = annotationType;
                        this.fieldDescription = fieldDescription;
                        this.readOnly = readOnly;
                        this.typing = typing;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<T> getAnnotationType() {
                        return annotationType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<T> annotation,
                                              AdviceType adviceType) {
                        return new Resolved(target.getType(), readOnly, typing, fieldDescription);
                    }
                }
            }
        }

        /**
         * An offset mapping for a field handle.
         */
        @HashCodeAndEqualsPlugin.Enhance
        abstract class ForFieldHandle implements OffsetMapping {

            /**
             * The access type of the represented handle.
             */
            private final Access access;

            /**
             * Creates an offset mapping for a field handle.
             *
             * @param access The access type of the represented handle.
             */
            protected ForFieldHandle(Access access) {
                this.access = access;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                FieldDescription fieldDescription = resolve(instrumentedType, instrumentedMethod);
                if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot access non-static field " + fieldDescription + " from static method " + instrumentedMethod);
                }
                if (sort.isPremature(instrumentedMethod) && !fieldDescription.isStatic()) {
                    throw new IllegalStateException("Cannot access " + fieldDescription + " before super constructor call");
                } else if (fieldDescription.isStatic()) {
                    return new Target.ForStackManipulation(access.resolve(fieldDescription.asDefined()).toStackManipulation());
                } else {
                    return new Target.ForStackManipulation(new StackManipulation.Compound(
                            access.resolve(fieldDescription.asDefined()).toStackManipulation(),
                            MethodVariableAccess.REFERENCE.loadFrom(argumentHandler.argument(ArgumentHandler.THIS_REFERENCE)),
                            MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLE.getTypeStub(), new MethodDescription.Token("bindTo",
                                    Opcodes.ACC_PUBLIC,
                                    JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                                    new TypeList.Generic.Explicit(TypeDefinition.Sort.describe(Object.class)))))));
                }
            }

            /**
             * Resolves the field being bound.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @return The field being bound.
             */
            protected abstract FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

            /**
             * A description of the field handle's access type.
             */
            public enum Access {

                /**
                 * Determines the resolution of a getter for the method handle.
                 */
                GETTER {
                    @Override
                    protected JavaConstant.MethodHandle resolve(FieldDescription.InDefinedShape fieldDescription) {
                        return JavaConstant.MethodHandle.ofGetter(fieldDescription);
                    }
                },

                /**
                 * Determines the resolution of a setter for the method handle.
                 */
                SETTER {
                    @Override
                    protected JavaConstant.MethodHandle resolve(FieldDescription.InDefinedShape fieldDescription) {
                        return JavaConstant.MethodHandle.ofSetter(fieldDescription);
                    }
                };

                /**
                 * Returns the appropriate method handle.
                 *
                 * @param fieldDescription The field description to resolve the handle for.
                 * @return The appropriate method handle.
                 */
                protected abstract JavaConstant.MethodHandle resolve(FieldDescription.InDefinedShape fieldDescription);
            }

            /**
             * An offset mapping for a field handle that is resolved from the instrumented type by its name.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public abstract static class Unresolved extends ForFieldHandle {

                /**
                 * Indicates that a name should be extracted from an accessor method.
                 */
                protected static final String BEAN_PROPERTY = "";

                /**
                 * The name of the field.
                 */
                private final String name;

                /**
                 * Creates an offset mapping for a field that is not yet resolved.
                 *
                 * @param access The access type of the represented handle.
                 * @param name   The name of the field.
                 */
                public Unresolved(Access access, String name) {
                    super(access);
                    this.name = name;
                }

                @Override
                protected FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    FieldLocator locator = fieldLocator(instrumentedType);
                    FieldLocator.Resolution resolution = name.equals(BEAN_PROPERTY)
                            ? FieldLocator.Resolution.Simple.ofBeanAccessor(locator, instrumentedMethod)
                            : locator.locate(name);
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Cannot locate field named " + name + " for " + instrumentedType);
                    } else {
                        return resolution.getField();
                    }
                }

                /**
                 * Returns a field locator for this instance.
                 *
                 * @param instrumentedType The instrumented type.
                 * @return An appropriate field locator.
                 */
                protected abstract FieldLocator fieldLocator(TypeDescription instrumentedType);

                /**
                 * An offset mapping for a field handle with an implicit declaring type.
                 */
                public static class WithImplicitType extends Unresolved {

                    /**
                     * Creates an offset mapping for a field handle with an implicit declaring type.
                     *
                     * @param access The access type of the represented handle.
                     * @param name   The name of the field.
                     */
                    public WithImplicitType(Access access, String name) {
                        super(access, name);
                    }

                    @Override
                    protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                        return new FieldLocator.ForClassHierarchy(instrumentedType);
                    }
                }

                /**
                 * An offset mapping for a field handle with an explicit declaring type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class WithExplicitType extends Unresolved {

                    /**
                     * The type declaring the field.
                     */
                    private final TypeDescription declaringType;

                    /**
                     * Creates an offset mapping for a field handle with an explicit declaring type.
                     *
                     * @param access        The access type of the represented handle.
                     * @param name          The name of the field.
                     * @param declaringType The type declaring the field.
                     */
                    public WithExplicitType(Access access, String name, TypeDescription declaringType) {
                        super(access, name);
                        this.declaringType = declaringType;
                    }

                    @Override
                    protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                        if (!declaringType.represents(TargetType.class) && !instrumentedType.isAssignableTo(declaringType)) {
                            throw new IllegalStateException(declaringType + " is no super type of " + instrumentedType);
                        }
                        return new FieldLocator.ForExactType(TargetType.resolve(declaringType, instrumentedType));
                    }
                }

                /**
                 * A factory for a {@link ForFieldHandle.Unresolved} offset mapping representing a getter.
                 */
                protected enum ReaderFactory implements OffsetMapping.Factory<FieldGetterHandle> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The {@link FieldGetterHandle#value()} method.
                     */
                    private static final MethodDescription.InDefinedShape FIELD_GETTER_HANDLE_VALUE;

                    /**
                     * The {@link FieldGetterHandle#declaringType()} method.
                     */
                    private static final MethodDescription.InDefinedShape FIELD_GETTER_HANDLE_DECLARING_TYPE;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(FieldGetterHandle.class).getDeclaredMethods();
                        FIELD_GETTER_HANDLE_VALUE = methods.filter(named("value")).getOnly();
                        FIELD_GETTER_HANDLE_DECLARING_TYPE = methods.filter(named("declaringType")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<FieldGetterHandle> getAnnotationType() {
                        return FieldGetterHandle.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<FieldGetterHandle> annotation,
                                              AdviceType adviceType) {
                        if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                            throw new IllegalStateException("Cannot assign method handle to " + target);
                        }
                        TypeDescription declaringType = annotation.getValue(FIELD_GETTER_HANDLE_DECLARING_TYPE).resolve(TypeDescription.class);
                        return declaringType.represents(void.class)
                                ? new ForFieldHandle.Unresolved.WithImplicitType(Access.GETTER, annotation.getValue(FIELD_GETTER_HANDLE_VALUE).resolve(String.class))
                                : new ForFieldHandle.Unresolved.WithExplicitType(Access.GETTER, annotation.getValue(FIELD_GETTER_HANDLE_VALUE).resolve(String.class), declaringType);
                    }
                }

                /**
                 * A factory for a {@link ForFieldHandle.Unresolved} offset mapping representing a setter.
                 */
                protected enum WriterFactory implements OffsetMapping.Factory<FieldSetterHandle> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The {@link FieldSetterHandle#value()} method.
                     */
                    private static final MethodDescription.InDefinedShape FIELD_SETTER_HANDLE_VALUE;

                    /**
                     * The {@link FieldSetterHandle#declaringType()} method.
                     */
                    private static final MethodDescription.InDefinedShape FIELD_SETTER_HANDLE_DECLARING_TYPE;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(FieldSetterHandle.class).getDeclaredMethods();
                        FIELD_SETTER_HANDLE_VALUE = methods.filter(named("value")).getOnly();
                        FIELD_SETTER_HANDLE_DECLARING_TYPE = methods.filter(named("declaringType")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<FieldSetterHandle> getAnnotationType() {
                        return FieldSetterHandle.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<FieldSetterHandle> annotation,
                                              AdviceType adviceType) {
                        if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                            throw new IllegalStateException("Cannot assign method handle to " + target);
                        }
                        TypeDescription declaringType = annotation.getValue(FIELD_SETTER_HANDLE_DECLARING_TYPE).resolve(TypeDescription.class);
                        return declaringType.represents(void.class)
                                ? new ForFieldHandle.Unresolved.WithImplicitType(Access.SETTER, annotation.getValue(FIELD_SETTER_HANDLE_VALUE).resolve(String.class))
                                : new ForFieldHandle.Unresolved.WithExplicitType(Access.SETTER, annotation.getValue(FIELD_SETTER_HANDLE_VALUE).resolve(String.class), declaringType);
                    }
                }
            }

            /**
             * A binding for an offset mapping that represents a specific field.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class Resolved extends ForFieldHandle {

                /**
                 * The accessed field.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a resolved offset mapping for a field handle.
                 *
                 * @param access           The access type of the represented handle.
                 * @param fieldDescription The accessed field.
                 */
                public Resolved(Access access, FieldDescription fieldDescription) {
                    super(access);
                    this.fieldDescription = fieldDescription;
                }

                @Override
                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming declaring type for type member.")
                protected FieldDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    if (!fieldDescription.isStatic() && !fieldDescription.getDeclaringType().asErasure().isAssignableFrom(instrumentedType)) {
                        throw new IllegalStateException(fieldDescription + " is no member of " + instrumentedType);
                    } else if (!fieldDescription.isVisibleTo(instrumentedType)) {
                        throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                    }
                    return fieldDescription;
                }

                /**
                 * A factory that binds a field handle.
                 *
                 * @param <T> The annotation type this factory binds.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class Factory<T extends Annotation> implements OffsetMapping.Factory<T> {

                    /**
                     * The annotation type.
                     */
                    private final Class<T> annotationType;

                    /**
                     * The field to be bound.
                     */
                    private final FieldDescription fieldDescription;

                    /**
                     * The access type of the represented handle.
                     */
                    private final Access access;

                    /**
                     * Creates a new factory for binding a specific field handle.
                     *
                     * @param annotationType   The annotation type.
                     * @param fieldDescription The field to bind.
                     * @param access           The access type of the represented handle.
                     */
                    public Factory(Class<T> annotationType, FieldDescription fieldDescription, Access access) {
                        this.annotationType = annotationType;
                        this.fieldDescription = fieldDescription;
                        this.access = access;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<T> getAnnotationType() {
                        return annotationType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<T> annotation,
                                              AdviceType adviceType) {
                        if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                            throw new IllegalStateException("Cannot assign method handle to " + target);
                        }
                        return new Resolved(access, fieldDescription);
                    }
                }
            }
        }

        /**
         * An offset mapping for the {@link Advice.Origin} annotation.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForOrigin implements OffsetMapping {

            /**
             * The delimiter character.
             */
            private static final char DELIMITER = '#';

            /**
             * The escape character.
             */
            private static final char ESCAPE = '\\';

            /**
             * The renderers to apply.
             */
            private final List<Renderer> renderers;

            /**
             * Creates a new offset mapping for an origin value.
             *
             * @param renderers The renderers to apply.
             */
            public ForOrigin(List<Renderer> renderers) {
                this.renderers = renderers;
            }

            /**
             * Parses a pattern of an origin annotation.
             *
             * @param pattern The supplied pattern.
             * @return An appropriate offset mapping.
             */
            public static OffsetMapping parse(String pattern) {
                if (pattern.equals(Origin.DEFAULT)) {
                    return new ForOrigin(Collections.<Renderer>singletonList(Renderer.ForStringRepresentation.INSTANCE));
                } else {
                    List<Renderer> renderers = new ArrayList<Renderer>(pattern.length());
                    int from = 0;
                    for (int to = pattern.indexOf(DELIMITER); to != -1; to = pattern.indexOf(DELIMITER, from)) {
                        if (to != 0 && pattern.charAt(to - 1) == ESCAPE && (to == 1 || pattern.charAt(to - 2) != ESCAPE)) {
                            renderers.add(new Renderer.ForConstantValue(pattern.substring(from, Math.max(0, to - 1)) + DELIMITER));
                            from = to + 1;
                            continue;
                        } else if (pattern.length() == to + 1) {
                            throw new IllegalStateException("Missing sort descriptor for " + pattern + " at index " + to);
                        }
                        renderers.add(new Renderer.ForConstantValue(pattern.substring(from, to).replace("" + ESCAPE + ESCAPE, "" + ESCAPE)));
                        switch (pattern.charAt(to + 1)) {
                            case Renderer.ForMethodName.SYMBOL:
                                renderers.add(Renderer.ForMethodName.INSTANCE);
                                break;
                            case Renderer.ForTypeName.SYMBOL:
                                renderers.add(Renderer.ForTypeName.INSTANCE);
                                break;
                            case Renderer.ForDescriptor.SYMBOL:
                                renderers.add(Renderer.ForDescriptor.INSTANCE);
                                break;
                            case Renderer.ForReturnTypeName.SYMBOL:
                                renderers.add(Renderer.ForReturnTypeName.INSTANCE);
                                break;
                            case Renderer.ForJavaSignature.SYMBOL:
                                renderers.add(Renderer.ForJavaSignature.INSTANCE);
                                break;
                            case Renderer.ForPropertyName.SYMBOL:
                                renderers.add(Renderer.ForPropertyName.INSTANCE);
                                break;
                            default:
                                throw new IllegalStateException("Illegal sort descriptor " + pattern.charAt(to + 1) + " for " + pattern);
                        }
                        from = to + 2;
                    }
                    renderers.add(new Renderer.ForConstantValue(pattern.substring(from)));
                    return new ForOrigin(renderers);
                }
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Renderer renderer : renderers) {
                    stringBuilder.append(renderer.apply(instrumentedType, instrumentedMethod));
                }
                return Target.ForStackManipulation.of(stringBuilder.toString());
            }

            /**
             * A renderer for an origin pattern element.
             */
            public interface Renderer {

                /**
                 * Returns a string representation for this renderer.
                 *
                 * @param instrumentedType   The instrumented type.
                 * @param instrumentedMethod The instrumented method.
                 * @return The string representation.
                 */
                String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

                /**
                 * A renderer for a method's internal name.
                 */
                enum ForMethodName implements Renderer {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The method name symbol.
                     */
                    public static final char SYMBOL = 'm';

                    /**
                     * {@inheritDoc}
                     */
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return instrumentedMethod.getInternalName();
                    }
                }

                /**
                 * A renderer for a method declaring type's binary name.
                 */
                enum ForTypeName implements Renderer {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The type name symbol.
                     */
                    public static final char SYMBOL = 't';

                    /**
                     * {@inheritDoc}
                     */
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return instrumentedType.getName();
                    }
                }

                /**
                 * A renderer for a method descriptor.
                 */
                enum ForDescriptor implements Renderer {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The descriptor symbol.
                     */
                    public static final char SYMBOL = 'd';

                    /**
                     * {@inheritDoc}
                     */
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return instrumentedMethod.getDescriptor();
                    }
                }

                /**
                 * A renderer for a method's Java signature in binary form.
                 */
                enum ForJavaSignature implements Renderer {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The signature symbol.
                     */
                    public static final char SYMBOL = 's';

                    /**
                     * {@inheritDoc}
                     */
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        StringBuilder stringBuilder = new StringBuilder("(");
                        boolean comma = false;
                        for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                            if (comma) {
                                stringBuilder.append(',');
                            } else {
                                comma = true;
                            }
                            stringBuilder.append(typeDescription.getName());
                        }
                        return stringBuilder.append(')').toString();
                    }
                }

                /**
                 * A renderer for a method's return type in binary form.
                 */
                enum ForReturnTypeName implements Renderer {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The return type symbol.
                     */
                    public static final char SYMBOL = 'r';

                    /**
                     * {@inheritDoc}
                     */
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return instrumentedMethod.getReturnType().asErasure().getName();
                    }
                }

                /**
                 * A renderer for a method's {@link Object#toString()} representation.
                 */
                enum ForStringRepresentation implements Renderer {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return instrumentedMethod.toString();
                    }
                }

                /**
                 * A renderer for a constant value.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForConstantValue implements Renderer {

                    /**
                     * The constant value.
                     */
                    private final String value;

                    /**
                     * Creates a new renderer for a constant value.
                     *
                     * @param value The constant value.
                     */
                    public ForConstantValue(String value) {
                        this.value = value;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return value;
                    }
                }

                /**
                 * A renderer for a property name.
                 */
                enum ForPropertyName implements Renderer {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The signature symbol.
                     */
                    public static final char SYMBOL = 'p';

                    /**
                     * {@inheritDoc}
                     */
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(instrumentedMethod);
                    }
                }
            }

            /**
             * A factory for a method origin.
             */
            protected enum Factory implements OffsetMapping.Factory<Origin> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A description of the {@link Origin#value()} method.
                 */
                private static final MethodDescription.InDefinedShape ORIGIN_VALUE = TypeDescription.ForLoadedType.of(Origin.class)
                        .getDeclaredMethods()
                        .filter(named("value"))
                        .getOnly();

                /**
                 * {@inheritDoc}
                 */
                public Class<Origin> getAnnotationType() {
                    return Origin.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Origin> annotation,
                                          AdviceType adviceType) {
                    if (target.getType().asErasure().represents(Class.class)) {
                        return ForInstrumentedType.INSTANCE;
                    } else if (target.getType().asErasure().represents(Method.class)) {
                        return ForInstrumentedMethod.METHOD;
                    } else if (target.getType().asErasure().represents(Constructor.class)) {
                        return ForInstrumentedMethod.CONSTRUCTOR;
                    } else if (JavaType.EXECUTABLE.getTypeStub().equals(target.getType().asErasure())) {
                        return ForInstrumentedMethod.EXECUTABLE;
                    } else if (JavaType.METHOD_HANDLE.getTypeStub().equals(target.getType().asErasure())) {
                        return ForInstrumentedMethod.METHOD_HANDLE;
                    } else if (JavaType.METHOD_TYPE.getTypeStub().equals(target.getType().asErasure())) {
                        return ForInstrumentedMethod.METHOD_TYPE;
                    } else if (JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().equals(target.getType().asErasure())) {
                        return new OffsetMapping.ForStackManipulation(MethodInvocation.lookup(),
                                JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().asGenericType(),
                                target.getType(),
                                Assigner.Typing.STATIC);
                    } else if (target.getType().asErasure().isAssignableFrom(String.class)) {
                        return ForOrigin.parse(annotation.getValue(ORIGIN_VALUE).resolve(String.class));
                    } else {
                        throw new IllegalStateException("Non-supported type " + target.getType() + " for @Origin annotation");
                    }
                }
            }
        }

        /**
         * An offset mapping for assigning a method handle that invokes the instrumented method.
         */
        enum ForSelfCallHandle implements OffsetMapping {

            /**
             * A bound assignment that invokes the instrumented method as is.
             */
            BOUND {
                @Override
                protected StackManipulation decorate(MethodDescription methodDescription, StackManipulation stackManipulation) {
                    List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(1
                            + (methodDescription.isStatic() ? 0 : 2)
                            + methodDescription.getParameters().size() * 3);
                    stackManipulations.add(stackManipulation);
                    if (!methodDescription.isStatic()) {
                        stackManipulations.add(MethodVariableAccess.loadThis());
                        stackManipulations.add(MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLE.getTypeStub(), new MethodDescription.Token("bindTo",
                                Opcodes.ACC_PUBLIC,
                                JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                                new TypeList.Generic.Explicit(TypeDefinition.Sort.describe(Object.class))))));
                    }
                    if (!methodDescription.getParameters().isEmpty()) {
                        List<StackManipulation> values = new ArrayList<StackManipulation>(methodDescription.getParameters().size());
                        for (ParameterDescription parameterDescription : methodDescription.getParameters()) {
                            values.add(parameterDescription.getType().isPrimitive() ?
                                    new StackManipulation.Compound(MethodVariableAccess.load(parameterDescription), Assigner.DEFAULT.assign(parameterDescription.getType(),
                                            parameterDescription.getType().asErasure().asBoxed().asGenericType(),
                                            Assigner.Typing.STATIC)) : MethodVariableAccess.load(parameterDescription));
                        }
                        stackManipulations.add(IntegerConstant.forValue(0));
                        stackManipulations.add(ArrayFactory.forType(TypeDescription.ForLoadedType.of(Object.class).asGenericType()).withValues(values));
                        stackManipulations.add(
                                MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLES.getTypeStub(), new MethodDescription.Token("insertArguments",
                                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                        JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                                        new TypeList.Generic.Explicit(JavaType.METHOD_HANDLE.getTypeStub(), TypeDefinition.Sort.describe(int.class),
                                                TypeDefinition.Sort.describe(Object[].class))))));
                    }
                    return new StackManipulation.Compound(stackManipulations);
                }
            },

            /**
             * An unbound self call handle which requires manual assignment of parameters.
             */
            UNBOUND {
                @Override
                protected StackManipulation decorate(MethodDescription methodDescription, StackManipulation stackManipulation) {
                    return stackManipulation;
                }
            };

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                if (!instrumentedMethod.isMethod()) {
                    throw new IllegalStateException();
                }
                StackManipulation stackManipulation = (instrumentedMethod.isStatic()
                        ? JavaConstant.MethodHandle.of(instrumentedMethod.asDefined())
                        : JavaConstant.MethodHandle.ofSpecial(instrumentedMethod.asDefined(), instrumentedType)).toStackManipulation();
                return new Target.ForStackManipulation(decorate(instrumentedMethod, stackManipulation));
            }

            /**
             * Resolves a stack manipulation.
             *
             * @param methodDescription The method description being represented.
             * @param stackManipulation The stack manipulation for the current method handle.
             * @return The decorated stack manipulation.
             */
            protected abstract StackManipulation decorate(MethodDescription methodDescription, StackManipulation stackManipulation);

            /**
             * A factory for a self call method handle.
             */
            protected enum Factory implements OffsetMapping.Factory<SelfCallHandle> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * The {@code bound} property of the {@link SelfCallHandle} annotation.
                 */
                private static final MethodDescription.InDefinedShape SELF_CALL_HANDLE_BOUND = TypeDescription.ForLoadedType.of(SelfCallHandle.class)
                        .getDeclaredMethods()
                        .filter(named("bound"))
                        .getOnly();

                /**
                 * {@inheritDoc}
                 */
                public Class<SelfCallHandle> getAnnotationType() {
                    return SelfCallHandle.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<SelfCallHandle> annotation,
                                          AdviceType adviceType) {
                    if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                        throw new IllegalStateException("Cannot assign a MethodHandle to " + target);
                    }
                    return annotation.getValue(SELF_CALL_HANDLE_BOUND).resolve(Boolean.class)
                            ? ForSelfCallHandle.BOUND
                            : ForSelfCallHandle.UNBOUND;
                }
            }
        }

        /**
         * An offset mapping for a parameter where assignments are fully ignored and that always return the parameter type's default value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForUnusedValue implements OffsetMapping {

            /**
             * The unused type.
             */
            private final TypeDefinition target;

            /**
             * Creates a new offset mapping for an unused type.
             *
             * @param target The unused type.
             */
            public ForUnusedValue(TypeDefinition target) {
                this.target = target;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                return new Target.ForDefaultValue.ReadWrite(target);
            }

            /**
             * A factory for an offset mapping for an unused value.
             */
            protected enum Factory implements OffsetMapping.Factory<Unused> {

                /**
                 * A factory for representing an unused value.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Class<Unused> getAnnotationType() {
                    return Unused.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Unused> annotation,
                                          AdviceType adviceType) {
                    return new ForUnusedValue(target.getType());
                }
            }
        }

        /**
         * An offset mapping for a parameter where assignments are fully ignored and that is assigned a boxed version of the instrumented
         * method's return value or {@code null} if the return type is not primitive or {@code void}.
         */
        enum ForStubValue implements OffsetMapping, Factory<StubValue> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                return new Target.ForDefaultValue.ReadOnly(instrumentedMethod.getReturnType(), assigner.assign(instrumentedMethod.getReturnType(),
                        TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class),
                        Assigner.Typing.DYNAMIC));
            }

            /**
             * {@inheritDoc}
             */
            public Class<StubValue> getAnnotationType() {
                return StubValue.class;
            }

            /**
             * {@inheritDoc}
             */
            public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                      AnnotationDescription.Loadable<StubValue> annotation,
                                      AdviceType adviceType) {
                if (!target.getType().represents(Object.class)) {
                    throw new IllegalStateException("Cannot use StubValue on non-Object parameter type " + target);
                } else {
                    return this;
                }
            }
        }

        /**
         * An offset mapping that provides access to the value that is returned by the enter advice.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForEnterValue implements OffsetMapping {

            /**
             * The represented target type.
             */
            private final TypeDescription.Generic target;

            /**
             * The enter type.
             */
            private final TypeDescription.Generic enterType;

            /**
             * {@code true} if the annotated value is read-only.
             */
            private final boolean readOnly;

            /**
             * The typing to apply.
             */
            private final Assigner.Typing typing;

            /**
             * Creates a new offset mapping for the enter type.
             *
             * @param target     The represented target type.
             * @param enterType  The enter type.
             * @param annotation The represented annotation.
             */
            protected ForEnterValue(TypeDescription.Generic target, TypeDescription.Generic enterType, AnnotationDescription.Loadable<Enter> annotation) {
                this(target,
                        enterType,
                        annotation.getValue(Factory.ENTER_READ_ONLY).resolve(Boolean.class),
                        annotation.getValue(Factory.ENTER_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class));
            }

            /**
             * Creates a new offset mapping for the enter type.
             *
             * @param target    The represented target type.
             * @param enterType The enter type.
             * @param readOnly  {@code true} if the annotated value is read-only.
             * @param typing    The typing to apply.
             */
            public ForEnterValue(TypeDescription.Generic target, TypeDescription.Generic enterType, boolean readOnly, Assigner.Typing typing) {
                this.target = target;
                this.enterType = enterType;
                this.readOnly = readOnly;
                this.typing = typing;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                StackManipulation readAssignment = assigner.assign(enterType, target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + enterType + " to " + target);
                } else if (readOnly) {
                    return new Target.ForVariable.ReadOnly(target, argumentHandler.enter(), readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, enterType, typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to " + enterType);
                    }
                    return new Target.ForVariable.ReadWrite(target, argumentHandler.enter(), readAssignment, writeAssignment);
                }
            }

            /**
             * A factory for creating a {@link ForEnterValue} offset mapping.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Factory implements OffsetMapping.Factory<Enter> {

                /**
                 * A description of the {@link Argument#readOnly()} method.
                 */
                private static final MethodDescription.InDefinedShape ENTER_READ_ONLY;

                /**
                 * A description of the {@link Argument#typing()} method.
                 */
                private static final MethodDescription.InDefinedShape ENTER_TYPING;

                static {
                    MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(Enter.class).getDeclaredMethods();
                    ENTER_READ_ONLY = methods.filter(named("readOnly")).getOnly();
                    ENTER_TYPING = methods.filter(named("typing")).getOnly();
                }

                /**
                 * The supplied type of the enter advice.
                 */
                private final TypeDefinition enterType;

                /**
                 * Creates a new factory for creating a {@link ForEnterValue} offset mapping.
                 *
                 * @param enterType The supplied type of the enter method.
                 */
                protected Factory(TypeDefinition enterType) {
                    this.enterType = enterType;
                }

                /**
                 * Creates a new factory for creating a {@link ForEnterValue} offset mapping.
                 *
                 * @param typeDefinition The supplied type of the enter advice.
                 * @return An appropriate offset mapping factory.
                 */
                protected static OffsetMapping.Factory<Enter> of(TypeDefinition typeDefinition) {
                    return typeDefinition.represents(void.class)
                            ? new Illegal<Enter>(Enter.class)
                            : new Factory(typeDefinition);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<Enter> getAnnotationType() {
                    return Enter.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Enter> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.getValue(ENTER_READ_ONLY).resolve(Boolean.class)) {
                        throw new IllegalStateException("Cannot use writable " + target + " on read-only parameter");
                    } else {
                        return new ForEnterValue(target.getType(), enterType.asGenericType(), annotation);
                    }
                }
            }
        }

        /**
         * An offset mapping that provides access to the value that is returned by the exit advice.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForExitValue implements OffsetMapping {

            /**
             * The represented target type.
             */
            private final TypeDescription.Generic target;

            /**
             * The exit type.
             */
            private final TypeDescription.Generic exitType;

            /**
             * {@code true} if the annotated value is read-only.
             */
            private final boolean readOnly;

            /**
             * The typing to apply.
             */
            private final Assigner.Typing typing;

            /**
             * Creates a new offset mapping for the exit type.
             *
             * @param target     The represented target type.
             * @param exitType   The exit type.
             * @param annotation The represented annotation.
             */
            protected ForExitValue(TypeDescription.Generic target, TypeDescription.Generic exitType, AnnotationDescription.Loadable<Exit> annotation) {
                this(target,
                        exitType,
                        annotation.getValue(Factory.EXIT_READ_ONLY).resolve(Boolean.class),
                        annotation.getValue(Factory.EXIT_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class));
            }

            /**
             * Creates a new offset mapping for the enter type.
             *
             * @param target   The represented target type.
             * @param exitType The exit type.
             * @param readOnly {@code true} if the annotated value is read-only.
             * @param typing   The typing to apply.
             */
            public ForExitValue(TypeDescription.Generic target, TypeDescription.Generic exitType, boolean readOnly, Assigner.Typing typing) {
                this.target = target;
                this.exitType = exitType;
                this.readOnly = readOnly;
                this.typing = typing;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                StackManipulation readAssignment = assigner.assign(exitType, target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + exitType + " to " + target);
                } else if (readOnly) {
                    return new Target.ForVariable.ReadOnly(target, argumentHandler.exit(), readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, exitType, typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to " + exitType);
                    }
                    return new Target.ForVariable.ReadWrite(target, argumentHandler.exit(), readAssignment, writeAssignment);
                }
            }

            /**
             * A factory for creating a {@link ForExitValue} offset mapping.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Factory implements OffsetMapping.Factory<Exit> {

                /**
                 * A description of the {@link Exit#readOnly()} method.
                 */
                private static final MethodDescription.InDefinedShape EXIT_READ_ONLY;

                /**
                 * A description of the {@link Exit#typing()} method.
                 */
                private static final MethodDescription.InDefinedShape EXIT_TYPING;

                /*
                 * Resolves annotation properties.
                 */
                static {
                    MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(Exit.class).getDeclaredMethods();
                    EXIT_READ_ONLY = methods.filter(named("readOnly")).getOnly();
                    EXIT_TYPING = methods.filter(named("typing")).getOnly();
                }

                /**
                 * The supplied type of the exit advice.
                 */
                private final TypeDefinition exitType;

                /**
                 * Creates a new factory for creating a {@link ForExitValue} offset mapping.
                 *
                 * @param exitType The supplied type of the exit advice.
                 */
                protected Factory(TypeDefinition exitType) {
                    this.exitType = exitType;
                }

                /**
                 * Creates a new factory for creating a {@link ForExitValue} offset mapping.
                 *
                 * @param typeDefinition The supplied type of the enter method.
                 * @return An appropriate offset mapping factory.
                 */
                protected static OffsetMapping.Factory<Exit> of(TypeDefinition typeDefinition) {
                    return typeDefinition.represents(void.class)
                            ? new Illegal<Exit>(Exit.class)
                            : new Factory(typeDefinition);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<Exit> getAnnotationType() {
                    return Exit.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Exit> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.getValue(EXIT_READ_ONLY).resolve(Boolean.class)) {
                        throw new IllegalStateException("Cannot use writable " + target + " on read-only parameter");
                    } else {
                        return new ForExitValue(target.getType(), exitType.asGenericType(), annotation);
                    }
                }
            }
        }

        /**
         * An offset mapping that provides access to a named local variable that is declared by the advice methods via {@link Local}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForLocalValue implements OffsetMapping {

            /**
             * The variable's target type.
             */
            private final TypeDescription.Generic target;

            /**
             * The local variable's type.
             */
            private final TypeDescription.Generic localType;

            /**
             * The local variable's name.
             */
            private final String name;

            /**
             * Creates an offset mapping for a local variable that is declared by the advice methods via {@link Local}.
             *
             * @param target    The variable's target type.
             * @param localType The local variable's type.
             * @param name      The local variable's name.
             */
            public ForLocalValue(TypeDescription.Generic target, TypeDescription.Generic localType, String name) {
                this.target = target;
                this.localType = localType;
                this.name = name;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                StackManipulation readAssignment = assigner.assign(localType, target, Assigner.Typing.STATIC);
                StackManipulation writeAssignment = assigner.assign(target, localType, Assigner.Typing.STATIC);
                if (!readAssignment.isValid() || !writeAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + localType + " to " + target);
                } else {
                    return new Target.ForVariable.ReadWrite(target, argumentHandler.named(name), readAssignment, writeAssignment);
                }
            }

            /**
             * A factory for an offset mapping for a local variable that is declared by the advice methods via {@link Local}.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Factory implements OffsetMapping.Factory<Local> {

                /**
                 * A description of the {@link Local#value()} method.
                 */
                protected static final MethodDescription.InDefinedShape LOCAL_VALUE = TypeDescription.ForLoadedType.of(Local.class)
                        .getDeclaredMethods()
                        .filter(named("value"))
                        .getOnly();

                /**
                 * The mapping of type names to their type that are available.
                 */
                private final Map<String, TypeDefinition> namedTypes;

                /**
                 * Creates a factory for a {@link Local} variable mapping.
                 *
                 * @param namedTypes The mapping of type names to their type that are available.
                 */
                protected Factory(Map<String, TypeDefinition> namedTypes) {
                    this.namedTypes = namedTypes;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<Local> getAnnotationType() {
                    return Local.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Local> annotation,
                                          AdviceType adviceType) {
                    String name = annotation.getValue(LOCAL_VALUE).resolve(String.class);
                    TypeDefinition namedType = namedTypes.get(name);
                    if (namedType == null) {
                        throw new IllegalStateException("Named local variable is unknown: " + name);
                    }
                    return new ForLocalValue(target.getType(), namedType.asGenericType(), name);
                }
            }
        }

        /**
         * An offset mapping that provides access to the value that is returned by the instrumented method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForReturnValue implements OffsetMapping {

            /**
             * The type that the advice method expects for the return value.
             */
            private final TypeDescription.Generic target;

            /**
             * Determines if the parameter is to be treated as read-only.
             */
            private final boolean readOnly;

            /**
             * The typing to apply.
             */
            private final Assigner.Typing typing;

            /**
             * Creates a new offset mapping for a return value.
             *
             * @param target     The type that the advice method expects for the return value.
             * @param annotation The annotation being bound.
             */
            protected ForReturnValue(TypeDescription.Generic target, AnnotationDescription.Loadable<Return> annotation) {
                this(target,
                        annotation.getValue(Factory.RETURN_READ_ONLY).resolve(Boolean.class),
                        annotation.getValue(Factory.RETURN_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class));
            }

            /**
             * Creates a new offset mapping for a return value.
             *
             * @param target   The type that the advice method expects for the return value.
             * @param readOnly Determines if the parameter is to be treated as read-only.
             * @param typing   The typing to apply.
             */
            public ForReturnValue(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing) {
                this.target = target;
                this.readOnly = readOnly;
                this.typing = typing;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                StackManipulation readAssignment = assigner.assign(instrumentedMethod.getReturnType(), target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + instrumentedMethod.getReturnType() + " to " + target);
                } else if (readOnly) {
                    return instrumentedMethod.getReturnType().represents(void.class)
                            ? new Target.ForDefaultValue.ReadOnly(target)
                            : new Target.ForVariable.ReadOnly(instrumentedMethod.getReturnType(), argumentHandler.returned(), readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, instrumentedMethod.getReturnType(), typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to " + instrumentedMethod.getReturnType());
                    }
                    return instrumentedMethod.getReturnType().represents(void.class)
                            ? new Target.ForDefaultValue.ReadWrite(target)
                            : new Target.ForVariable.ReadWrite(instrumentedMethod.getReturnType(), argumentHandler.returned(), readAssignment, writeAssignment);
                }
            }

            /**
             * A factory for creating a {@link ForReturnValue} offset mapping.
             */
            protected enum Factory implements OffsetMapping.Factory<Return> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A description of the {@link Return#readOnly()} method.
                 */
                private static final MethodDescription.InDefinedShape RETURN_READ_ONLY;

                /**
                 * A description of the {@link Return#typing()} method.
                 */
                private static final MethodDescription.InDefinedShape RETURN_TYPING;

                /*
                 * Resolves the annotation properties.
                 */
                static {
                    MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(Return.class).getDeclaredMethods();
                    RETURN_READ_ONLY = methods.filter(named("readOnly")).getOnly();
                    RETURN_TYPING = methods.filter(named("typing")).getOnly();
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<Return> getAnnotationType() {
                    return Return.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Return> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.getValue(RETURN_READ_ONLY).resolve(Boolean.class)) {
                        throw new IllegalStateException("Cannot write return value for " + target + " in read-only context");
                    } else {
                        return new ForReturnValue(target.getType(), annotation);
                    }
                }
            }
        }

        /**
         * An offset mapping for accessing a {@link Throwable} of the instrumented method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForThrowable implements OffsetMapping {

            /**
             * The type of parameter that is being accessed.
             */
            private final TypeDescription.Generic target;

            /**
             * {@code true} if the parameter is read-only.
             */
            private final boolean readOnly;

            /**
             * The typing to apply.
             */
            private final Assigner.Typing typing;

            /**
             * Creates a new offset mapping for access of the exception that is thrown by the instrumented method..
             *
             * @param target     The type of parameter that is being accessed.
             * @param annotation The annotation to bind.
             */
            protected ForThrowable(TypeDescription.Generic target, AnnotationDescription.Loadable<Thrown> annotation) {
                this(target,
                        annotation.getValue(Factory.THROWN_READ_ONLY).resolve(Boolean.class),
                        annotation.getValue(Factory.THROWN_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class));
            }

            /**
             * Creates a new offset mapping for access of the exception that is thrown by the instrumented method..
             *
             * @param target   The type of parameter that is being accessed.
             * @param readOnly {@code true} if the parameter is read-only.
             * @param typing   The typing to apply.
             */
            public ForThrowable(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing) {
                this.target = target;
                this.readOnly = readOnly;
                this.typing = typing;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                StackManipulation readAssignment = assigner.assign(TypeDescription.ForLoadedType.of(Throwable.class).asGenericType(), target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign Throwable to " + target);
                } else if (readOnly) {
                    return new Target.ForVariable.ReadOnly(TypeDescription.ForLoadedType.of(Throwable.class), argumentHandler.thrown(), readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, TypeDescription.ForLoadedType.of(Throwable.class).asGenericType(), typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to Throwable");
                    }
                    return new Target.ForVariable.ReadWrite(TypeDescription.ForLoadedType.of(Throwable.class), argumentHandler.thrown(), readAssignment, writeAssignment);
                }
            }

            /**
             * A factory for accessing an exception that was thrown by the instrumented method.
             */
            protected enum Factory implements OffsetMapping.Factory<Thrown> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A description of the {@link Thrown#readOnly()} method.
                 */
                private static final MethodDescription.InDefinedShape THROWN_READ_ONLY;

                /**
                 * A description of the {@link Thrown#typing()} method.
                 */
                private static final MethodDescription.InDefinedShape THROWN_TYPING;

                /*
                 * Resolves annotation properties.
                 */
                static {
                    MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(Thrown.class).getDeclaredMethods();
                    THROWN_READ_ONLY = methods.filter(named("readOnly")).getOnly();
                    THROWN_TYPING = methods.filter(named("typing")).getOnly();
                }

                /**
                 * Resolves an appropriate offset mapping factory for the {@link Thrown} parameter annotation.
                 *
                 * @param adviceMethod The exit advice method, annotated with {@link OnMethodExit}.
                 * @return An appropriate offset mapping factory.
                 */
                @SuppressWarnings("unchecked") // In absence of @SafeVarargs
                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming annotation for exit advice.")
                protected static OffsetMapping.Factory<?> of(MethodDescription.InDefinedShape adviceMethod) {
                    return adviceMethod.getDeclaredAnnotations()
                            .ofType(OnMethodExit.class)
                            .getValue(ON_THROWABLE)
                            .resolve(TypeDescription.class)
                            .represents(NoExceptionHandler.class) ? new OffsetMapping.Factory.Illegal<Thrown>(Thrown.class) : Factory.INSTANCE;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<Thrown> getAnnotationType() {
                    return Thrown.class;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Thrown> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.getValue(THROWN_READ_ONLY).resolve(Boolean.class)) {
                        throw new IllegalStateException("Cannot use writable " + target + " on read-only parameter");
                    } else {
                        return new ForThrowable(target.getType(), annotation);
                    }
                }
            }
        }

        /**
         * An offset mapping for binding a stack manipulation.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForStackManipulation implements OffsetMapping {

            /**
             * The stack manipulation that loads the bound value.
             */
            private final StackManipulation stackManipulation;

            /**
             * The type of the loaded value.
             */
            private final TypeDescription.Generic typeDescription;

            /**
             * The target type of the annotated parameter.
             */
            private final TypeDescription.Generic targetType;

            /**
             * The typing to apply.
             */
            private final Assigner.Typing typing;

            /**
             * Creates an offset mapping that binds a stack manipulation.
             *
             * @param stackManipulation The stack manipulation that loads the bound value.
             * @param typeDescription   The type of the loaded value.
             * @param targetType        The target type of the annotated parameter.
             * @param typing            The typing to apply.
             */
            public ForStackManipulation(StackManipulation stackManipulation,
                                        TypeDescription.Generic typeDescription,
                                        TypeDescription.Generic targetType,
                                        Assigner.Typing typing) {
                this.stackManipulation = stackManipulation;
                this.typeDescription = typeDescription;
                this.targetType = targetType;
                this.typing = typing;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                StackManipulation assignment = assigner.assign(typeDescription, targetType, typing);
                if (!assignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + typeDescription + " to " + targetType);
                }
                return new Target.ForStackManipulation(new StackManipulation.Compound(stackManipulation, assignment));
            }

            /**
             * A factory that binds a stack manipulation.
             *
             * @param <T> The annotation type this factory binds.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class Factory<T extends Annotation> implements OffsetMapping.Factory<T> {

                /**
                 * The annotation type.
                 */
                private final Class<T> annotationType;

                /**
                 * The stack manipulation that loads the bound value.
                 */
                private final StackManipulation stackManipulation;

                /**
                 * The type of the loaded value.
                 */
                private final TypeDescription.Generic typeDescription;

                /**
                 * Creates a new factory for binding a type description.
                 *
                 * @param annotationType  The annotation type.
                 * @param typeDescription The type to bind.
                 */
                public Factory(Class<T> annotationType, TypeDescription typeDescription) {
                    this(annotationType, ClassConstant.of(typeDescription), TypeDescription.ForLoadedType.of(Class.class).asGenericType());
                }

                /**
                 * Creates a new factory for binding an enumeration.
                 *
                 * @param annotationType         The annotation type.
                 * @param enumerationDescription The enumeration to bind.
                 */
                public Factory(Class<T> annotationType, EnumerationDescription enumerationDescription) {
                    this(annotationType, FieldAccess.forEnumeration(enumerationDescription), enumerationDescription.getEnumerationType().asGenericType());
                }

                /**
                 * Creates a new factory for binding a Java constant.
                 *
                 * @param annotationType The annotation type.
                 * @param constant       The bound constant value.
                 */
                public Factory(Class<T> annotationType, ConstantValue constant) {
                    this(annotationType, constant.toStackManipulation(), constant.getTypeDescription().asGenericType());
                }

                /**
                 * Creates a new factory for binding a stack manipulation.
                 *
                 * @param annotationType    The annotation type.
                 * @param stackManipulation The stack manipulation that loads the bound value.
                 * @param typeDescription   The type of the loaded value.
                 */
                public Factory(Class<T> annotationType, StackManipulation stackManipulation, TypeDescription.Generic typeDescription) {
                    this.annotationType = annotationType;
                    this.stackManipulation = stackManipulation;
                    this.typeDescription = typeDescription;
                }

                /**
                 * Creates a binding for a fixed {@link String}, a primitive value or a method handle or type.
                 *
                 * @param annotationType The annotation type.
                 * @param value          The constant value to bind or {@code null} to bind the parameter type's default value.
                 * @param <S>            The annotation type.
                 * @return A factory for creating an offset mapping that binds the supplied value.
                 */
                public static <S extends Annotation> OffsetMapping.Factory<S> of(Class<S> annotationType, @MaybeNull Object value) {
                    return value == null
                            ? new OfDefaultValue<S>(annotationType)
                            : new Factory<S>(annotationType, ConstantValue.Simple.wrap(value));
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<T> annotation,
                                          AdviceType adviceType) {
                    return new ForStackManipulation(stackManipulation, typeDescription, target.getType(), Assigner.Typing.STATIC);
                }
            }

            /**
             * A factory for binding the annotated parameter's default value.
             *
             * @param <T> The annotation type this factory binds.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfDefaultValue<T extends Annotation> implements OffsetMapping.Factory<T> {

                /**
                 * The annotation type.
                 */
                private final Class<T> annotationType;

                /**
                 * Creates a factory for an offset mapping tat binds the parameter's default value.
                 *
                 * @param annotationType The annotation type.
                 */
                public OfDefaultValue(Class<T> annotationType) {
                    this.annotationType = annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    return new ForStackManipulation(DefaultValue.of(target.getType()), target.getType(), target.getType(), Assigner.Typing.STATIC);
                }
            }

            /**
             * A factory for binding an annotation's property.
             *
             * @param <T> The annotation type this factory binds.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfAnnotationProperty<T extends Annotation> implements OffsetMapping.Factory<T> {

                /**
                 * The annotation type.
                 */
                private final Class<T> annotationType;

                /**
                 * The annotation property.
                 */
                private final MethodDescription.InDefinedShape property;

                /**
                 * Creates a factory for binding an annotation property.
                 *
                 * @param annotationType The annotation type.
                 * @param property       The annotation property.
                 */
                protected OfAnnotationProperty(Class<T> annotationType, MethodDescription.InDefinedShape property) {
                    this.annotationType = annotationType;
                    this.property = property;
                }

                /**
                 * Creates a factory for an offset mapping that binds an annotation property.
                 *
                 * @param annotationType The annotation type to bind.
                 * @param property       The property to bind.
                 * @param <S>            The annotation type.
                 * @return A factory for binding a property of the annotation type.
                 */
                public static <S extends Annotation> OffsetMapping.Factory<S> of(Class<S> annotationType, String property) {
                    if (!annotationType.isAnnotation()) {
                        throw new IllegalArgumentException("Not an annotation type: " + annotationType);
                    }
                    try {
                        return new OfAnnotationProperty<S>(annotationType, new MethodDescription.ForLoadedMethod(annotationType.getMethod(property)));
                    } catch (NoSuchMethodException exception) {
                        throw new IllegalArgumentException("Cannot find a property " + property + " on " + annotationType, exception);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    ConstantValue value = ConstantValue.Simple.wrapOrNull(annotation.getValue(property).resolve());
                    if (value == null) {
                        throw new IllegalStateException("Property does not represent a constant value: " + property);
                    }
                    return new ForStackManipulation(value.toStackManipulation(),
                            value.getTypeDescription().asGenericType(),
                            target.getType(),
                            Assigner.Typing.STATIC);
                }
            }

            /**
             * Uses dynamic method invocation for binding an annotated parameter to a value.
             *
             * @param <T> The annotation type this factory binds.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfDynamicInvocation<T extends Annotation> implements OffsetMapping.Factory<T> {

                /**
                 * The annotation type.
                 */
                private final Class<T> annotationType;

                /**
                 * The bootstrap method or constructor.
                 */
                private final MethodDescription.InDefinedShape bootstrapMethod;

                /**
                 * The arguments to the bootstrap method.
                 */
                private final List<? extends JavaConstant> arguments;

                /**
                 * Creates a new factory for a dynamic invocation.
                 *
                 * @param annotationType  The annotation type.
                 * @param bootstrapMethod The bootstrap method or constructor.
                 * @param arguments       The arguments to the bootstrap method.
                 */
                public OfDynamicInvocation(Class<T> annotationType, MethodDescription.InDefinedShape bootstrapMethod, List<? extends JavaConstant> arguments) {
                    this.annotationType = annotationType;
                    this.bootstrapMethod = bootstrapMethod;
                    this.arguments = arguments;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    if (!target.getType().isInterface()) {
                        throw new IllegalArgumentException(target.getType() + " is not an interface");
                    } else if (!target.getType().getInterfaces().isEmpty()) {
                        throw new IllegalArgumentException(target.getType() + " must not extend other interfaces");
                    } else if (!target.getType().isPublic()) {
                        throw new IllegalArgumentException(target.getType() + " is mot public");
                    }
                    MethodList<?> methodCandidates = target.getType().getDeclaredMethods().filter(isAbstract());
                    if (methodCandidates.size() != 1) {
                        throw new IllegalArgumentException(target.getType() + " must declare exactly one abstract method");
                    }
                    return new ForStackManipulation(MethodInvocation.invoke(bootstrapMethod).dynamic(methodCandidates.getOnly().getInternalName(),
                            target.getType().asErasure(),
                            Collections.<TypeDescription>emptyList(),
                            arguments), target.getType(), target.getType(), Assigner.Typing.STATIC);
                }
            }
        }

        /**
         * An offset mapping that loads a serialized value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForSerializedValue implements OffsetMapping {

            /**
             * The type of the serialized value as it is used.
             */
            private final TypeDescription.Generic target;

            /**
             * The class type of the serialized value.
             */
            private final TypeDescription typeDescription;

            /**
             * The stack manipulation deserializing the represented value.
             */
            private final StackManipulation deserialization;

            /**
             * Creates a new offset mapping for a serialized value.
             *
             * @param target          The type of the serialized value as it is used.
             * @param typeDescription The class type of the serialized value.
             * @param deserialization The stack manipulation deserializing the represented value.
             */
            public ForSerializedValue(TypeDescription.Generic target, TypeDescription typeDescription, StackManipulation deserialization) {
                this.target = target;
                this.typeDescription = typeDescription;
                this.deserialization = deserialization;
            }

            /**
             * {@inheritDoc}
             */
            public Target resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  Sort sort) {
                StackManipulation assignment = assigner.assign(typeDescription.asGenericType(), target, Assigner.Typing.DYNAMIC);
                if (!assignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + typeDescription + " to " + target);
                }
                return new Target.ForStackManipulation(new StackManipulation.Compound(deserialization, assignment));
            }

            /**
             * A factory for loading a deserialized value.
             *
             * @param <T> The annotation type this factory binds.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class Factory<T extends Annotation> implements OffsetMapping.Factory<T> {

                /**
                 * The annotation type.
                 */
                private final Class<T> annotationType;

                /**
                 * The type description as which to treat the deserialized value.
                 */
                private final TypeDescription typeDescription;

                /**
                 * The stack manipulation that loads the represented value.
                 */
                private final StackManipulation deserialization;

                /**
                 * Creates a factory for loading a deserialized value.
                 *
                 * @param annotationType  The annotation type.
                 * @param typeDescription The type description as which to treat the deserialized value.
                 * @param deserialization The stack manipulation that loads the represented value.
                 */
                protected Factory(Class<T> annotationType, TypeDescription typeDescription, StackManipulation deserialization) {
                    this.annotationType = annotationType;
                    this.typeDescription = typeDescription;
                    this.deserialization = deserialization;
                }

                /**
                 * Creates a factory for an offset mapping that loads the provided value.
                 *
                 * @param annotationType The annotation type to be bound.
                 * @param target         The instance representing the value to be deserialized.
                 * @param targetType     The target type as which to use the target value.
                 * @param <S>            The annotation type the created factory binds.
                 * @param <U>            The type of the -represented constant.
                 * @return An appropriate offset mapping factory.
                 */
                public static <S extends Annotation, U extends Serializable> OffsetMapping.Factory<S> of(Class<S> annotationType, U target, Class<? super U> targetType) {
                    if (!targetType.isInstance(target)) {
                        throw new IllegalArgumentException(target + " is no instance of " + targetType);
                    }
                    return new Factory<S>(annotationType, TypeDescription.ForLoadedType.of(targetType), SerializedConstant.of(target));
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                /**
                 * {@inheritDoc}
                 */
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    return new ForSerializedValue(target.getType(), typeDescription, deserialization);
                }
            }
        }
    }

    /**
     * An argument handler is responsible for resolving offsets of the local variable array in the context of the applied instrumentation.
     */
    public interface ArgumentHandler {

        /**
         * The offset of the {@code this} reference.
         */
        int THIS_REFERENCE = 0;

        /**
         * Resolves an offset relative to an offset of the instrumented method.
         *
         * @param offset The offset to resolve.
         * @return The resolved offset.
         */
        int argument(int offset);

        /**
         * Resolves the offset of the exit value of the exit advice.
         *
         * @return The offset of the exit value.
         */
        int exit();

        /**
         * Resolves the offset of the enter value of the enter advice.
         *
         * @return The offset of the enter value.
         */
        int enter();

        /**
         * Returns the offset of the local variable with the given name.
         *
         * @param name The name of the local variable being accessed.
         * @return The named variable's offset.
         */
        int named(String name);

        /**
         * Resolves the offset of the returned value of the instrumented method.
         *
         * @return The offset of the returned value of the instrumented method.
         */
        int returned();

        /**
         * Resolves the offset of the thrown exception of the instrumented method.
         *
         * @return The offset of the thrown exception of the instrumented method.
         */
        int thrown();

        /**
         * An argument handler that is used for resolving the instrumented method.
         */
        interface ForInstrumentedMethod extends ArgumentHandler {

            /**
             * Prepares this argument handler for future offset access.
             *
             * @param methodVisitor The method visitor to which to write any potential byte code.
             * @return The minimum stack size that is required to apply this manipulation.
             */
            int prepare(MethodVisitor methodVisitor);

            /**
             * Binds an advice method as enter advice for this handler.
             *
             * @param typeToken The type token of the advice method.
             * @return The resolved argument handler for enter advice.
             */
            ForAdvice bindEnter(MethodDescription.TypeToken typeToken);

            /**
             * Binds an advice method as exit advice for this handler.
             *
             * @param typeToken     The type token of the advice method.
             * @param skipThrowable {@code true} if no throwable is stored.
             * @return The resolved argument handler for enter advice.
             */
            ForAdvice bindExit(MethodDescription.TypeToken typeToken, boolean skipThrowable);

            /**
             * Returns {@code true} if the original arguments are copied before invoking the instrumented method.
             *
             * @return {@code true} if the original arguments are copied before invoking the instrumented method.
             */
            boolean isCopyingArguments();

            /**
             * Returns a list of the named types in their declared order.
             *
             * @return A list of the named types in their declared order.
             */
            List<TypeDescription> getNamedTypes();

            /**
             * A default implementation of an argument handler for an instrumented method.
             */
            abstract class Default implements ForInstrumentedMethod {

                /**
                 * The instrumented method.
                 */
                protected final MethodDescription instrumentedMethod;

                /**
                 * The exit type or {@code void} if no exit type is defined.
                 */
                protected final TypeDefinition exitType;

                /**
                 * A mapping of all available local variables by their name to their type.
                 */
                protected final SortedMap<String, TypeDefinition> namedTypes;

                /**
                 * The enter type or {@code void} if no enter type is defined.
                 */
                protected final TypeDefinition enterType;

                /**
                 * Creates a new default argument handler for an instrumented method.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param exitType           The exit type or {@code void} if no exit type is defined.
                 * @param namedTypes         A mapping of all available local variables by their name to their type.
                 * @param enterType          The enter type or {@code void} if no enter type is defined.
                 */
                protected Default(MethodDescription instrumentedMethod,
                                  TypeDefinition exitType,
                                  SortedMap<String, TypeDefinition> namedTypes,
                                  TypeDefinition enterType) {
                    this.instrumentedMethod = instrumentedMethod;
                    this.namedTypes = namedTypes;
                    this.exitType = exitType;
                    this.enterType = enterType;
                }

                /**
                 * {@inheritDoc}
                 */
                public int exit() {
                    return instrumentedMethod.getStackSize();
                }

                /**
                 * {@inheritDoc}
                 */
                public int named(String name) {
                    return instrumentedMethod.getStackSize()
                            + exitType.getStackSize().getSize()
                            + StackSize.of(namedTypes.headMap(name).values());
                }

                /**
                 * {@inheritDoc}
                 */
                public int enter() {
                    return instrumentedMethod.getStackSize()
                            + exitType.getStackSize().getSize()
                            + StackSize.of(namedTypes.values());
                }

                /**
                 * {@inheritDoc}
                 */
                public int returned() {
                    return instrumentedMethod.getStackSize()
                            + exitType.getStackSize().getSize()
                            + StackSize.of(namedTypes.values())
                            + enterType.getStackSize().getSize();
                }

                /**
                 * {@inheritDoc}
                 */
                public int thrown() {
                    return instrumentedMethod.getStackSize()
                            + exitType.getStackSize().getSize()
                            + StackSize.of(namedTypes.values())
                            + enterType.getStackSize().getSize()
                            + instrumentedMethod.getReturnType().getStackSize().getSize();
                }

                /**
                 * {@inheritDoc}
                 */
                public ForAdvice bindEnter(MethodDescription.TypeToken typeToken) {
                    return new ForAdvice.Default.ForMethodEnter(instrumentedMethod, typeToken, exitType, namedTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public ForAdvice bindExit(MethodDescription.TypeToken typeToken, boolean skipThrowable) {
                    return new ForAdvice.Default.ForMethodExit(instrumentedMethod,
                            typeToken,
                            exitType,
                            namedTypes,
                            enterType,
                            skipThrowable ? StackSize.ZERO : StackSize.SINGLE);
                }

                /**
                 * {@inheritDoc}
                 */
                public List<TypeDescription> getNamedTypes() {
                    List<TypeDescription> namedTypes = new ArrayList<TypeDescription>(this.namedTypes.size());
                    for (TypeDefinition typeDefinition : this.namedTypes.values()) {
                        namedTypes.add(typeDefinition.asErasure());
                    }
                    return namedTypes;
                }

                /**
                 * A simple argument handler for an instrumented method.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class Simple extends Default {

                    /**
                     * Creates a new simple argument handler for an instrumented method.
                     *
                     * @param instrumentedMethod The instrumented method.
                     * @param exitType           The exit type or {@code void} if no exit type is defined.
                     * @param namedTypes         A mapping of all available local variables by their name to their type.
                     * @param enterType          The enter type or {@code void} if no enter type is defined.
                     */
                    protected Simple(MethodDescription instrumentedMethod,
                                     TypeDefinition exitType,
                                     SortedMap<String, TypeDefinition> namedTypes,
                                     TypeDefinition enterType) {
                        super(instrumentedMethod, exitType, namedTypes, enterType);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int argument(int offset) {
                        return offset < instrumentedMethod.getStackSize()
                                ? offset
                                : offset + exitType.getStackSize().getSize() + StackSize.of(namedTypes.values()) + enterType.getStackSize().getSize();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean isCopyingArguments() {
                        return false;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int prepare(MethodVisitor methodVisitor) {
                        return 0;
                    }
                }

                /**
                 * An argument handler for an instrumented method that copies all arguments before executing the instrumented method.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class Copying extends Default {

                    /**
                     * Creates a new copying argument handler for an instrumented method.
                     *
                     * @param instrumentedMethod The instrumented method.
                     * @param exitType           The exit type or {@code void} if no exit type is defined.
                     * @param namedTypes         A mapping of all available local variables by their name to their type.
                     * @param enterType          The enter type or {@code void} if no enter type is defined.
                     */
                    protected Copying(MethodDescription instrumentedMethod,
                                      TypeDefinition exitType,
                                      SortedMap<String, TypeDefinition> namedTypes,
                                      TypeDefinition enterType) {
                        super(instrumentedMethod, exitType, namedTypes, enterType);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int argument(int offset) {
                        return instrumentedMethod.getStackSize()
                                + exitType.getStackSize().getSize()
                                + StackSize.of(namedTypes.values())
                                + enterType.getStackSize().getSize()
                                + offset;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean isCopyingArguments() {
                        return true;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int prepare(MethodVisitor methodVisitor) {
                        StackSize stackSize;
                        if (!instrumentedMethod.isStatic()) {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize()
                                    + exitType.getStackSize().getSize()
                                    + StackSize.of(namedTypes.values())
                                    + enterType.getStackSize().getSize());
                            stackSize = StackSize.SINGLE;
                        } else {
                            stackSize = StackSize.ZERO;
                        }
                        for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                            Type type = Type.getType(parameterDescription.getType().asErasure().getDescriptor());
                            methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), parameterDescription.getOffset());
                            methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ISTORE), instrumentedMethod.getStackSize()
                                    + exitType.getStackSize().getSize()
                                    + StackSize.of(namedTypes.values())
                                    + enterType.getStackSize().getSize()
                                    + parameterDescription.getOffset());
                            stackSize = stackSize.maximum(parameterDescription.getType().getStackSize());
                        }
                        return stackSize.getSize();
                    }
                }
            }
        }

        /**
         * An argument handler that is used for resolving an advice method.
         */
        interface ForAdvice extends ArgumentHandler {

            /**
             * Resolves an offset of the advice method.
             *
             * @param offset The offset to resolve.
             * @return The resolved offset.
             */
            int mapped(int offset);

            /**
             * A default implementation for an argument handler for an advice method.
             */
            abstract class Default implements ForAdvice {

                /**
                 * The instrumented method.
                 */
                protected final MethodDescription instrumentedMethod;

                /**
                 * The type token of the advice method.
                 */
                protected final MethodDescription.TypeToken typeToken;

                /**
                 * The enter type or {@code void} if no enter type is defined.
                 */
                protected final TypeDefinition exitType;

                /**
                 * A mapping of all available local variables by their name to their type.
                 */
                protected final SortedMap<String, TypeDefinition> namedTypes;

                /**
                 * Creates a new argument handler for an enter advice.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param typeToken          The type token of the advice method.
                 * @param exitType           The exit type or {@code void} if no exit type is defined.
                 * @param namedTypes         A mapping of all available local variables by their name to their type.
                 */
                protected Default(MethodDescription instrumentedMethod,
                                  MethodDescription.TypeToken typeToken,
                                  TypeDefinition exitType,
                                  SortedMap<String, TypeDefinition> namedTypes) {
                    this.instrumentedMethod = instrumentedMethod;
                    this.typeToken = typeToken;
                    this.exitType = exitType;
                    this.namedTypes = namedTypes;
                }

                /**
                 * {@inheritDoc}
                 */
                public int argument(int offset) {
                    return offset;
                }

                /**
                 * {@inheritDoc}
                 */
                public int exit() {
                    return instrumentedMethod.getStackSize();
                }

                /**
                 * {@inheritDoc}
                 */
                public int named(String name) {
                    return instrumentedMethod.getStackSize()
                            + exitType.getStackSize().getSize()
                            + StackSize.of(namedTypes.headMap(name).values());
                }

                /**
                 * {@inheritDoc}
                 */
                public int enter() {
                    return instrumentedMethod.getStackSize()
                            + exitType.getStackSize().getSize()
                            + StackSize.of(namedTypes.values());
                }

                /**
                 * An argument handler for an enter advice method.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class ForMethodEnter extends Default {

                    /**
                     * Creates a new argument handler for an enter advice method.
                     *
                     * @param instrumentedMethod The instrumented method.
                     * @param typeToken          The type token of the advice method.
                     * @param exitType           The exit type or {@code void} if no exit type is defined.
                     * @param namedTypes         A mapping of all available local variables by their name to their type.
                     */
                    protected ForMethodEnter(MethodDescription instrumentedMethod,
                                             MethodDescription.TypeToken typeToken,
                                             TypeDefinition exitType,
                                             SortedMap<String, TypeDefinition> namedTypes) {
                        super(instrumentedMethod, typeToken, exitType, namedTypes);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int returned() {
                        throw new IllegalStateException("Cannot resolve the return value offset during enter advice");
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int thrown() {
                        throw new IllegalStateException("Cannot resolve the thrown value offset during enter advice");
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int mapped(int offset) {
                        return instrumentedMethod.getStackSize()
                                + exitType.getStackSize().getSize()
                                + StackSize.of(namedTypes.values())
                                - StackSize.of(typeToken.getParameterTypes()) + offset;
                    }
                }

                /**
                 * An argument handler for an exit advice method.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class ForMethodExit extends Default {

                    /**
                     * The enter type or {@code void} if no enter type is defined.
                     */
                    private final TypeDefinition enterType;

                    /**
                     * The stack size of a possibly stored throwable.
                     */
                    private final StackSize throwableSize;

                    /**
                     * Creates a new argument handler for an exit advice method.
                     *
                     * @param instrumentedMethod The instrumented method.
                     * @param typeToken          The type token of the advice method.
                     * @param exitType           The exit type or {@code void} if no exit type is defined.
                     * @param namedTypes         A mapping of all available local variables by their name to their type.
                     * @param enterType          The enter type or {@code void} if no enter type is defined.
                     * @param throwableSize      The stack size of a possibly stored throwable.
                     */
                    protected ForMethodExit(MethodDescription instrumentedMethod,
                                            MethodDescription.TypeToken typeToken,
                                            TypeDefinition exitType,
                                            SortedMap<String, TypeDefinition> namedTypes,
                                            TypeDefinition enterType,
                                            StackSize throwableSize) {
                        super(instrumentedMethod, typeToken, exitType, namedTypes);
                        this.enterType = enterType;
                        this.throwableSize = throwableSize;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int returned() {
                        return instrumentedMethod.getStackSize()
                                + exitType.getStackSize().getSize()
                                + StackSize.of(namedTypes.values())
                                + enterType.getStackSize().getSize();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int thrown() {
                        return instrumentedMethod.getStackSize()
                                + exitType.getStackSize().getSize()
                                + StackSize.of(namedTypes.values())
                                + enterType.getStackSize().getSize()
                                + instrumentedMethod.getReturnType().getStackSize().getSize();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int mapped(int offset) {
                        return instrumentedMethod.getStackSize()
                                + exitType.getStackSize().getSize()
                                + StackSize.of(namedTypes.values())
                                + enterType.getStackSize().getSize()
                                + instrumentedMethod.getReturnType().getStackSize().getSize()
                                + throwableSize.getSize()
                                - StackSize.of(typeToken.getParameterTypes())
                                + offset;
                    }
                }
            }
        }

        /**
         * A factory for creating an argument handler.
         */
        enum Factory {

            /**
             * A factory for creating a simple argument handler.
             */
            SIMPLE {
                @Override
                protected ForInstrumentedMethod resolve(MethodDescription instrumentedMethod,
                                                        TypeDefinition enterType,
                                                        TypeDefinition exitType,
                                                        SortedMap<String, TypeDefinition> namedTypes) {
                    return new ForInstrumentedMethod.Default.Simple(instrumentedMethod,
                            exitType,
                            namedTypes,
                            enterType);
                }
            },

            /**
             * A factory for creating an argument handler that copies all arguments before executing the instrumented method.
             */
            COPYING {
                @Override
                protected ForInstrumentedMethod resolve(MethodDescription instrumentedMethod,
                                                        TypeDefinition enterType,
                                                        TypeDefinition exitType,
                                                        SortedMap<String, TypeDefinition> namedTypes) {
                    return new ForInstrumentedMethod.Default.Copying(instrumentedMethod,
                            exitType,
                            namedTypes,
                            enterType);
                }
            };

            /**
             * Creates an argument handler.
             *
             * @param instrumentedMethod The instrumented method.
             * @param enterType          The enter type or {@code void} if no such type is defined.
             * @param exitType           The exit type or {@code void} if no exit type is defined.
             * @param namedTypes         A mapping of all available local variables by their name to their type.
             * @return An argument handler for the instrumented method.
             */
            protected abstract ForInstrumentedMethod resolve(MethodDescription instrumentedMethod,
                                                             TypeDefinition enterType,
                                                             TypeDefinition exitType,
                                                             SortedMap<String, TypeDefinition> namedTypes);
        }
    }

    /**
     * <p>
     * A post processor for advice methods that is invoked after advice is executed. A post processor
     * is invoked after the instrumented method and only after a regular completion of the method. When
     * invoked, the advice method's return value is stored in the local variable array. Upon completion,
     * the local variable array must still be intact and the stack must be empty. A frame is added
     * subsequently to the post processor's execution, making it feasible to add a jump instruction to the
     * end of the method after which no further byte code instructions must be issued. This also applies
     * to compound post processors. If a post processor emits a frame as its last instruction, it should
     * yield a <i>NOP</i> instruction to avoid that subsequent code starts with a frame.
     * </p>
     * <p>
     * <b>Important</b>: A post processor is triggered after the suppression handler. Exceptions triggered
     * by post processing code will therefore cause those exceptions to be propagated unless the post
     * processor configures explicit exception handling.
     * </p>
     */
    public interface PostProcessor {

        /**
         * Resolves this post processor for a given instrumented method.
         *
         * @param instrumentedType     The instrumented type.
         * @param instrumentedMethod   The instrumented method.
         * @param assigner             The assigner to use.
         * @param argumentHandler      The argument handler to use.
         * @param stackMapFrameHandler The argument handler for the instrumented method.
         * @param exceptionHandler     The exception handler that is resolved for the instrumented method.
         * @return The stack manipulation to apply.
         */
        StackManipulation resolve(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  Assigner assigner,
                                  ArgumentHandler argumentHandler,
                                  StackMapFrameHandler.ForPostProcessor stackMapFrameHandler,
                                  StackManipulation exceptionHandler);

        /**
         * A factory for creating a {@link PostProcessor}.
         */
        interface Factory {

            /**
             * Creates a post processor for a given advice method.
             *
             * @param annotations The annotations of the advice method.
             * @param returnType  The advice method's return type that is being post-processed.
             * @param exit        {@code true} if the advice is exit advice.
             * @return The created post processor.
             */
            PostProcessor make(List<? extends AnnotationDescription> annotations, TypeDescription returnType, boolean exit);

            /**
             * A compound factory for a post processor.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Compound implements Factory {

                /**
                 * The represented post processor factories.
                 */
                private final List<Factory> factories;

                /**
                 * Creates a compound post processor factory.
                 *
                 * @param factory The represented post processor factories.
                 */
                public Compound(Factory... factory) {
                    this(Arrays.asList(factory));
                }

                /**
                 * Creates a compound post processor factory.
                 *
                 * @param factories The represented post processor factories.
                 */
                public Compound(List<? extends Factory> factories) {
                    this.factories = new ArrayList<Factory>();
                    for (Factory factory : factories) {
                        if (factory instanceof Compound) {
                            this.factories.addAll(((Compound) factory).factories);
                        } else if (!(factory instanceof NoOp)) {
                            this.factories.add(factory);
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public PostProcessor make(List<? extends AnnotationDescription> annotations, TypeDescription returnType, boolean exit) {
                    List<PostProcessor> postProcessors = new ArrayList<PostProcessor>(factories.size());
                    for (Factory factory : factories) {
                        postProcessors.add(factory.make(annotations, returnType, exit));
                    }
                    return new PostProcessor.Compound(postProcessors);
                }
            }
        }

        /**
         * A non-operational advice post processor.
         */
        enum NoOp implements PostProcessor, Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription instrumentedMethod,
                                             Assigner assigner,
                                             ArgumentHandler argumentHandler,
                                             StackMapFrameHandler.ForPostProcessor stackMapFrameHandler,
                                             StackManipulation exceptionHandler) {
                return StackManipulation.Trivial.INSTANCE;
            }

            /**
             * {@inheritDoc}
             */
            public PostProcessor make(List<? extends AnnotationDescription> annotations, TypeDescription returnType, boolean exit) {
                return this;
            }
        }

        /**
         * A compound post processor.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Compound implements PostProcessor {

            /**
             * The represented post processors.
             */
            private final List<PostProcessor> postProcessors;

            /**
             * Creates a new compound post processor.
             *
             * @param postProcessors The represented post processors.
             */
            protected Compound(List<PostProcessor> postProcessors) {
                this.postProcessors = postProcessors;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription instrumentedMethod,
                                             Assigner assigner,
                                             ArgumentHandler argumentHandler,
                                             StackMapFrameHandler.ForPostProcessor stackMapFrameHandler,
                                             StackManipulation exceptionHandler) {
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(postProcessors.size());
                for (PostProcessor postProcessor : postProcessors) {
                    stackManipulations.add(postProcessor.resolve(instrumentedType,
                            instrumentedMethod,
                            assigner,
                            argumentHandler,
                            stackMapFrameHandler,
                            exceptionHandler));
                }
                return new StackManipulation.Compound(stackManipulations);
            }
        }
    }

    /**
     * Materializes an advice invocation within a delegation.
     */
    protected interface Delegator {

        /**
         * Materializes an invocation.
         *
         * @param instrumentedType   The instrumented type.
         * @param instrumentedMethod The instrumented method.
         * @return An appropriate stack manipulation which needs to consume all arguments for the advice
         * method and needs to provide a compatible return type.
         */
        StackManipulation apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

        /**
         * Returns the advice method's type token.
         *
         * @return The advice method's type token.
         */
        MethodDescription.TypeToken getTypeToken();

        /**
         * Asserts the visibility of the delegation target.
         *
         * @param instrumentedType The instrumented type.
         */
        void assertVisibility(TypeDescription instrumentedType);

        /**
         * A factory for creating a {@link Delegator}.
         */
        interface Factory {

            /**
             * Resolves a delegator.
             *
             * @param adviceMethod The advice method.
             * @param exit         {@code true} if the advice is applied as exit advice.
             * @return An appropriate delegator.
             */
            Delegator make(MethodDescription.InDefinedShape adviceMethod, boolean exit);
        }

        /**
         * Invokes an advice method using a regular method call.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForRegularInvocation implements Delegator {

            /**
             * The advice method.
             */
            private final MethodDescription.InDefinedShape adviceMethod;

            /**
             * Creates a delegator for a regular invocation.
             *
             * @param adviceMethod The advice method.
             */
            protected ForRegularInvocation(MethodDescription.InDefinedShape adviceMethod) {
                this.adviceMethod = adviceMethod;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                return MethodInvocation.invoke(adviceMethod);
            }

            /**
             * {@inheritDoc}
             */
            public MethodDescription.TypeToken getTypeToken() {
                return adviceMethod.asTypeToken();
            }

            /**
             * {@inheritDoc}
             */
            public void assertVisibility(TypeDescription instrumentedType) {
                if (!adviceMethod.isVisibleTo(instrumentedType)) {
                    throw new IllegalStateException(adviceMethod + " is not visible to " + instrumentedType);
                }
            }

            /**
             * A factory for a regular method invocation delegator.
             */
            protected enum Factory implements Delegator.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Delegator make(MethodDescription.InDefinedShape adviceMethod, boolean exit) {
                    return new ForRegularInvocation(adviceMethod);
                }
            }
        }

        /**
         * Invokes an advice method using a dynamic method call.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForDynamicInvocation implements Delegator {

            /**
             * The bootstrap method.
             */
            private final MethodDescription.InDefinedShape bootstrapMethod;

            /**
             * The advice method.
             */
            private final MethodDescription.SignatureToken signatureToken;

            /**
             * A resolver to provide the arguments to the bootstrap method.
             */
            private final BootstrapArgumentResolver resolver;

            /**
             * Creates a delegator for a dynamic method invocation.
             *
             * @param bootstrapMethod The bootstrap method.
             * @param signatureToken  The advice method's signature token.
             * @param resolver        A resolver to provide the arguments to the bootstrap method.
             */
            protected ForDynamicInvocation(MethodDescription.InDefinedShape bootstrapMethod,
                                           MethodDescription.SignatureToken signatureToken,
                                           BootstrapArgumentResolver resolver) {
                this.bootstrapMethod = bootstrapMethod;
                this.signatureToken = signatureToken;
                this.resolver = resolver;
            }

            /**
             * Creates a new dynamic invocation delegator.
             *
             * @param bootstrapMethod The bootstrap method or constructor.
             * @param resolverFactory A resolver factory to provide the arguments to the bootstrap method.
             * @param visitor         A visitor to apply to the parameter types prior to resolving the {@code MethodType}
             *                        that is passed to the bootstrap method. The supplied types might not be available
             *                        to the instrumented type what might make it necessary to camouflage them to avoid
             *                        class loading errors. The actual type should then rather be passed in a different
             *                        format by the supplied {@link BootstrapArgumentResolver}.
             * @return An appropriate delegator.
             */
            protected static Delegator.Factory of(MethodDescription.InDefinedShape bootstrapMethod,
                                                  BootstrapArgumentResolver.Factory resolverFactory,
                                                  TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
                if (!bootstrapMethod.isInvokeBootstrap()) {
                    throw new IllegalArgumentException("Not a suitable bootstrap target: " + bootstrapMethod);
                }
                return new Factory(bootstrapMethod, resolverFactory, visitor);
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                List<JavaConstant> constants = resolver.resolve(instrumentedType, instrumentedMethod);
                if (!bootstrapMethod.isInvokeBootstrap(TypeList.Explicit.of(constants))) {
                    throw new IllegalStateException("Cannot invoke " + bootstrapMethod + " with arguments: " + constants);
                }
                return MethodInvocation.invoke(bootstrapMethod).dynamic(signatureToken.getName(),
                        signatureToken.getReturnType(),
                        signatureToken.getParameterTypes(),
                        constants);
            }

            /**
             * {@inheritDoc}
             */
            public MethodDescription.TypeToken getTypeToken() {
                return signatureToken.asTypeToken();
            }

            /**
             * {@inheritDoc}
             */
            public void assertVisibility(TypeDescription instrumentedType) {
                if (!bootstrapMethod.isVisibleTo(instrumentedType)) {
                    throw new IllegalStateException(bootstrapMethod + " is not visible to " + instrumentedType);
                }
            }

            /**
             * A factory for creating a dynamic invocation dispatcher.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Factory implements Delegator.Factory {

                /**
                 * The bootstrap method.
                 */
                private final MethodDescription.InDefinedShape bootstrapMethod;

                /**
                 * A resolver factory to provide the arguments to the bootstrap method.
                 */
                private final BootstrapArgumentResolver.Factory resolverFactory;

                /**
                 * A visitor to apply to the parameter types prior to resolving the {@code MethodType}
                 * that is passed to the bootstrap method. The supplied types might not be available
                 * to the instrumented type what might make it necessary to camouflage them to avoid
                 * class loading errors. The actual type should then rather be passed in a different
                 * format by the supplied {@link BootstrapArgumentResolver}.
                 */
                private final TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

                /**
                 * Creates a factory for a dynamic invocation dispatcher.
                 *
                 * @param bootstrapMethod The bootstrap method.
                 * @param resolverFactory A resolver factory to provide the arguments to the bootstrap method.
                 * @param visitor         A visitor to apply to the parameter types prior to resolving the {@code MethodType}
                 *                        that is passed to the bootstrap method. The supplied types might not be available
                 *                        to the instrumented type what might make it necessary to camouflage them to avoid
                 *                        class loading errors. The actual type should then rather be passed in a different
                 *                        format by the supplied {@link BootstrapArgumentResolver}.
                 */
                protected Factory(MethodDescription.InDefinedShape bootstrapMethod,
                                  BootstrapArgumentResolver.Factory resolverFactory,
                                  TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
                    this.bootstrapMethod = bootstrapMethod;
                    this.resolverFactory = resolverFactory;
                    this.visitor = visitor;
                }

                /**
                 * {@inheritDoc}
                 */
                public Delegator make(MethodDescription.InDefinedShape adviceMethod, boolean exit) {
                    return new ForDynamicInvocation(bootstrapMethod,
                            new MethodDescription.SignatureToken(adviceMethod.getInternalName(),
                                    adviceMethod.getReturnType().accept(visitor).asErasure(),
                                    adviceMethod.getParameters().asTypeList().accept(visitor).asErasures()),
                            resolverFactory.resolve(adviceMethod, exit));
                }
            }
        }
    }

    /**
     * A resolver for the arguments that are provided to a bootstrap method if dynamic dispatch is used.
     */
    public interface BootstrapArgumentResolver {

        /**
         * Resolves the constants that are provided as arguments to the bootstrap methods.
         *
         * @param instrumentedType   The instrumented type.
         * @param instrumentedMethod The instrumented method.
         * @return A list of constants to supply as arguments to the bootstrap method.
         */
        List<JavaConstant> resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

        /**
         * A factory for creating a {@link BootstrapArgumentResolver}.
         */
        interface Factory {

            /**
             * Creates a bootstrap argument resolver for a given advice.
             *
             * @param adviceMethod The advice method.
             * @param exit         {@code true} if the method is bound as exit advice.
             * @return An appropriate bootstrap argument resolver.
             */
            BootstrapArgumentResolver resolve(MethodDescription.InDefinedShape adviceMethod, boolean exit);
        }

        /**
         * An argument resolver that supplies a default selection of arguments. The explicitly resolved constant values are:
         * <ul>
         * <li>A {@link String} of the target's binary class name.</li>
         * <li>A {@code int} with value {@code 0} for an enter advice and {code 1} for an exist advice.</li>
         * <li>A {@link Class} representing the class implementing the instrumented method.</li>
         * <li>A {@link String} with the name of the instrumented method.</li>
         * <li>A {@code java.lang.invoke.MethodHandle} representing the instrumented method unless the target is the type's static initializer.</li>
         * </ul>
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForDefaultValues implements BootstrapArgumentResolver {

            /**
             * The advice method.
             */
            private final MethodDescription.InDefinedShape adviceMethod;

            /**
             * {@code true} if the advice is applied as exit advice.
             */
            private final boolean exit;

            /**
             * Creates a bootstrap argument resolver with default values.
             *
             * @param adviceMethod The advice method.
             * @param exit         {@code true} if the advice is applied as exit advice.
             */
            protected ForDefaultValues(MethodDescription.InDefinedShape adviceMethod, boolean exit) {
                this.adviceMethod = adviceMethod;
                this.exit = exit;
            }

            /**
             * {@inheritDoc}
             */
            public List<JavaConstant> resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                if (instrumentedMethod.isTypeInitializer()) {
                    return Arrays.asList(JavaConstant.Simple.ofLoaded(adviceMethod.getDeclaringType().getName()),
                            JavaConstant.Simple.ofLoaded(exit ? 1 : 0),
                            JavaConstant.Simple.of(instrumentedType),
                            JavaConstant.Simple.ofLoaded(instrumentedMethod.getInternalName()));
                } else {
                    return Arrays.asList(JavaConstant.Simple.ofLoaded(adviceMethod.getDeclaringType().getName()),
                            JavaConstant.Simple.ofLoaded(exit ? 1 : 0),
                            JavaConstant.Simple.of(instrumentedType),
                            JavaConstant.Simple.ofLoaded(instrumentedMethod.getInternalName()),
                            JavaConstant.MethodHandle.of(instrumentedMethod.asDefined()));
                }
            }

            /**
             * A factory for creating a {@link ForDefaultValues}.
             */
            public enum Factory implements BootstrapArgumentResolver.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public BootstrapArgumentResolver resolve(MethodDescription.InDefinedShape adviceMethod, boolean exit) {
                    return new ForDefaultValues(adviceMethod, exit);
                }
            }
        }
    }

    /**
     * A handler for computing the instrumented method's size.
     */
    protected interface MethodSizeHandler {

        /**
         * Indicates that a size is not computed but handled directly by ASM.
         */
        int UNDEFINED_SIZE = Short.MAX_VALUE;

        /**
         * Records a minimum stack size required by the represented advice method.
         *
         * @param stackSize The minimum size required by the represented advice method.
         */
        void requireStackSize(int stackSize);

        /**
         * Requires a minimum length of the local variable array.
         *
         * @param localVariableLength The minimal required length of the local variable array.
         */
        void requireLocalVariableLength(int localVariableLength);

        /**
         * A method size handler for the instrumented method.
         */
        interface ForInstrumentedMethod extends MethodSizeHandler {

            /**
             * Binds a method size handler for the enter advice.
             *
             * @param typeToken The type token representing the enter advice.
             * @return A method size handler for the enter advice.
             */
            ForAdvice bindEnter(MethodDescription.TypeToken typeToken);

            /**
             * Binds the method size handler for the exit advice.
             *
             * @param typeToken The type token representing the exit advice.
             * @return A method size handler for the exit advice.
             */
            ForAdvice bindExit(MethodDescription.TypeToken typeToken);

            /**
             * Computes a compound stack size for the advice and the translated instrumented method.
             *
             * @param stackSize The required stack size of the instrumented method before translation.
             * @return The stack size required by the instrumented method and its advice methods.
             */
            int compoundStackSize(int stackSize);

            /**
             * Computes a compound local variable array length for the advice and the translated instrumented method.
             *
             * @param localVariableLength The required local variable array length of the instrumented method before translation.
             * @return The local variable length required by the instrumented method and its advice methods.
             */
            int compoundLocalVariableLength(int localVariableLength);
        }

        /**
         * A method size handler for an advice method.
         */
        interface ForAdvice extends MethodSizeHandler {

            /**
             * Requires additional padding for the operand stack that is required for this advice's execution.
             *
             * @param stackSizePadding The required padding.
             */
            void requireStackSizePadding(int stackSizePadding);

            /**
             * Requires additional padding for the local variable array that is required for this advice's execution.
             *
             * @param localVariableLengthPadding The required padding.
             */
            void requireLocalVariableLengthPadding(int localVariableLengthPadding);

            /**
             * Records the maximum values for stack size and local variable array which are required by the advice method
             * for its individual execution without translation.
             *
             * @param stackSize           The minimum required stack size.
             * @param localVariableLength The minimum required length of the local variable array.
             */
            void recordMaxima(int stackSize, int localVariableLength);
        }

        /**
         * A non-operational method size handler.
         */
        enum NoOp implements ForInstrumentedMethod, ForAdvice {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public ForAdvice bindEnter(MethodDescription.TypeToken typeToken) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public ForAdvice bindExit(MethodDescription.TypeToken typeToken) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public int compoundStackSize(int stackSize) {
                return UNDEFINED_SIZE;
            }

            /**
             * {@inheritDoc}
             */
            public int compoundLocalVariableLength(int localVariableLength) {
                return UNDEFINED_SIZE;
            }

            /**
             * {@inheritDoc}
             */
            public void requireStackSize(int stackSize) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void requireLocalVariableLength(int localVariableLength) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void requireStackSizePadding(int stackSizePadding) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void requireLocalVariableLengthPadding(int localVariableLengthPadding) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void recordMaxima(int stackSize, int localVariableLength) {
                /* do nothing */
            }
        }

        /**
         * A default implementation for a method size handler.
         */
        abstract class Default implements MethodSizeHandler.ForInstrumentedMethod {

            /**
             * The instrumented method.
             */
            protected final MethodDescription instrumentedMethod;

            /**
             * A list of virtual method arguments that are explicitly added before any code execution.
             */
            protected final List<? extends TypeDescription> initialTypes;

            /**
             * A list of virtual method arguments that are available before the instrumented method is executed.
             */
            protected final List<? extends TypeDescription> preMethodTypes;

            /**
             * A list of virtual method arguments that are available after the instrumented method has completed.
             */
            protected final List<? extends TypeDescription> postMethodTypes;

            /**
             * The maximum stack size required by a visited advice method.
             */
            protected int stackSize;

            /**
             * The maximum length of the local variable array required by a visited advice method.
             */
            protected int localVariableLength;

            /**
             * Creates a new default meta data handler that recomputes the space requirements of an instrumented method.
             *
             * @param instrumentedMethod The instrumented method.
             * @param initialTypes       A list of virtual method arguments that are explicitly added before any code execution.
             * @param preMethodTypes     A list of virtual method arguments that are available before the instrumented method is executed.
             * @param postMethodTypes    A list of virtual method arguments that are available after the instrumented method has completed.
             */
            protected Default(MethodDescription instrumentedMethod,
                              List<? extends TypeDescription> initialTypes,
                              List<? extends TypeDescription> preMethodTypes,
                              List<? extends TypeDescription> postMethodTypes) {
                this.instrumentedMethod = instrumentedMethod;
                this.initialTypes = initialTypes;
                this.preMethodTypes = preMethodTypes;
                this.postMethodTypes = postMethodTypes;
            }

            /**
             * Creates a method size handler applicable for the given instrumented method.
             *
             * @param instrumentedMethod The instrumented method.
             * @param initialTypes       A list of virtual method arguments that are explicitly added before any code execution.
             * @param preMethodTypes     A list of virtual method arguments that are available before the instrumented method is executed.
             * @param postMethodTypes    A list of virtual method arguments that are available after the instrumented method has completed.
             * @param copyArguments      {@code true} if the original arguments are copied before invoking the instrumented method.
             * @param writerFlags        The flags supplied to the ASM class writer.
             * @return An appropriate method size handler.
             */
            protected static MethodSizeHandler.ForInstrumentedMethod of(MethodDescription instrumentedMethod,
                                                                        List<? extends TypeDescription> initialTypes,
                                                                        List<? extends TypeDescription> preMethodTypes,
                                                                        List<? extends TypeDescription> postMethodTypes,
                                                                        boolean copyArguments,
                                                                        int writerFlags) {
                if ((writerFlags & (ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)) != 0) {
                    return NoOp.INSTANCE;
                } else if (copyArguments) {
                    return new WithCopiedArguments(instrumentedMethod, initialTypes, preMethodTypes, postMethodTypes);
                } else {
                    return new WithRetainedArguments(instrumentedMethod, initialTypes, preMethodTypes, postMethodTypes);
                }
            }

            /**
             * {@inheritDoc}
             */
            public MethodSizeHandler.ForAdvice bindEnter(MethodDescription.TypeToken typeToken) {
                return new ForAdvice(typeToken, instrumentedMethod.getStackSize() + StackSize.of(initialTypes));
            }

            /**
             * {@inheritDoc}
             */
            public void requireStackSize(int stackSize) {
                Default.this.stackSize = Math.max(this.stackSize, stackSize);
            }

            /**
             * {@inheritDoc}
             */
            public void requireLocalVariableLength(int localVariableLength) {
                this.localVariableLength = Math.max(this.localVariableLength, localVariableLength);
            }

            /**
             * {@inheritDoc}
             */
            public int compoundStackSize(int stackSize) {
                return Math.max(this.stackSize, stackSize);
            }

            /**
             * {@inheritDoc}
             */
            public int compoundLocalVariableLength(int localVariableLength) {
                return Math.max(this.localVariableLength, localVariableLength
                        + StackSize.of(postMethodTypes)
                        + StackSize.of(initialTypes)
                        + StackSize.of(preMethodTypes));
            }

            /**
             * A method size handler that expects that the original arguments are retained.
             */
            protected static class WithRetainedArguments extends Default {

                /**
                 * Creates a new default method size handler that expects that the original arguments are retained.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param initialTypes       A list of virtual method arguments that are explicitly added before any code execution.
                 * @param preMethodTypes     A list of virtual method arguments that are available before the instrumented method is executed.
                 * @param postMethodTypes    A list of virtual method arguments that are available after the instrumented method has completed.
                 */
                protected WithRetainedArguments(MethodDescription instrumentedMethod,
                                                List<? extends TypeDescription> initialTypes,
                                                List<? extends TypeDescription> preMethodTypes,
                                                List<? extends TypeDescription> postMethodTypes) {
                    super(instrumentedMethod, initialTypes, preMethodTypes, postMethodTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodSizeHandler.ForAdvice bindExit(MethodDescription.TypeToken typeToken) {
                    return new ForAdvice(typeToken, instrumentedMethod.getStackSize()
                            + StackSize.of(postMethodTypes)
                            + StackSize.of(initialTypes)
                            + StackSize.of(preMethodTypes));
                }

                /**
                 * {@inheritDoc}
                 */
                public int compoundLocalVariableLength(int localVariableLength) {
                    return Math.max(this.localVariableLength, localVariableLength
                            + StackSize.of(postMethodTypes)
                            + StackSize.of(initialTypes)
                            + StackSize.of(preMethodTypes));
                }
            }

            /**
             * A method size handler that expects that the original arguments were copied.
             */
            protected static class WithCopiedArguments extends Default {

                /**
                 * Creates a new default method size handler that expects the original arguments to be copied.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param initialTypes       A list of virtual method arguments that are explicitly added before any code execution.
                 * @param preMethodTypes     A list of virtual method arguments that are available before the instrumented method is executed.
                 * @param postMethodTypes    A list of virtual method arguments that are available after the instrumented method has completed.
                 */
                protected WithCopiedArguments(MethodDescription instrumentedMethod,
                                              List<? extends TypeDescription> initialTypes,
                                              List<? extends TypeDescription> preMethodTypes,
                                              List<? extends TypeDescription> postMethodTypes) {
                    super(instrumentedMethod, initialTypes, preMethodTypes, postMethodTypes);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodSizeHandler.ForAdvice bindExit(MethodDescription.TypeToken typeToken) {
                    return new ForAdvice(typeToken, 2 * instrumentedMethod.getStackSize()
                            + StackSize.of(initialTypes)
                            + StackSize.of(preMethodTypes)
                            + StackSize.of(postMethodTypes));
                }

                /**
                 * {@inheritDoc}
                 */
                public int compoundLocalVariableLength(int localVariableLength) {
                    return Math.max(this.localVariableLength, localVariableLength
                            + instrumentedMethod.getStackSize()
                            + StackSize.of(postMethodTypes)
                            + StackSize.of(initialTypes)
                            + StackSize.of(preMethodTypes));
                }
            }

            /**
             * A method size handler for an advice method.
             */
            protected class ForAdvice implements MethodSizeHandler.ForAdvice {

                /**
                 * The advice method.
                 */
                private final MethodDescription.TypeToken typeToken;

                /**
                 * The base of the local variable length that is implied by the method instrumentation prior to applying this advice method.
                 */
                private final int baseLocalVariableLength;

                /**
                 * The additional padding to apply to the operand stack.
                 */
                private int stackSizePadding;

                /**
                 * The additional padding to apply to the local variable array.
                 */
                private int localVariableLengthPadding;

                /**
                 * Creates a default method size handler for an advice method.
                 *
                 * @param typeToken               The type token representing the advice.
                 * @param baseLocalVariableLength The base of the local variable length that is implied by the method instrumentation
                 *                                prior to applying this advice method.
                 */
                protected ForAdvice(MethodDescription.TypeToken typeToken, int baseLocalVariableLength) {
                    this.typeToken = typeToken;
                    this.baseLocalVariableLength = baseLocalVariableLength;
                }

                /**
                 * {@inheritDoc}
                 */
                public void requireStackSize(int stackSize) {
                    Default.this.requireStackSize(stackSize);
                }

                /**
                 * {@inheritDoc}
                 */
                public void requireLocalVariableLength(int localVariableLength) {
                    Default.this.requireLocalVariableLength(localVariableLength);
                }

                /**
                 * {@inheritDoc}
                 */
                public void requireStackSizePadding(int stackSizePadding) {
                    this.stackSizePadding = Math.max(this.stackSizePadding, stackSizePadding);
                }

                /**
                 * {@inheritDoc}
                 */
                public void requireLocalVariableLengthPadding(int localVariableLengthPadding) {
                    this.localVariableLengthPadding = Math.max(this.localVariableLengthPadding, localVariableLengthPadding);
                }

                /**
                 * {@inheritDoc}
                 */
                public void recordMaxima(int stackSize, int localVariableLength) {
                    Default.this.requireStackSize(stackSize + stackSizePadding);
                    Default.this.requireLocalVariableLength(localVariableLength
                            - StackSize.of(typeToken.getParameterTypes())
                            + baseLocalVariableLength
                            + localVariableLengthPadding);
                }
            }
        }
    }

    /**
     * A handler for computing and translating stack map frames.
     */
    public interface StackMapFrameHandler {

        /**
         * Translates a frame.
         *
         * @param methodVisitor       The method visitor to write the frame to.
         * @param type                The frame's type.
         * @param localVariableLength The local variable length.
         * @param localVariable       An array containing the types of the current local variables.
         * @param stackSize           The size of the operand stack.
         * @param stack               An array containing the types of the current operand stack.
         */
        void translateFrame(MethodVisitor methodVisitor,
                            int type,
                            int localVariableLength,
                            @MaybeNull Object[] localVariable,
                            int stackSize,
                            @MaybeNull Object[] stack);

        /**
         * Injects a frame indicating the beginning of a return value handler for the currently handled method.
         *
         * @param methodVisitor The method visitor onto which to apply the stack map frame.
         */
        void injectReturnFrame(MethodVisitor methodVisitor);

        /**
         * Injects a frame indicating the beginning of an exception handler for the currently handled method.
         *
         * @param methodVisitor The method visitor onto which to apply the stack map frame.
         */
        void injectExceptionFrame(MethodVisitor methodVisitor);

        /**
         * Injects a frame indicating the completion of the currently handled method, i.e. all yielded types were added.
         *
         * @param methodVisitor The method visitor onto which to apply the stack map frame.
         */
        void injectCompletionFrame(MethodVisitor methodVisitor);

        /**
         * A stack map frame handler that can be used within a post processor. Emitting frames via this
         * handler is the only legal way for a post processor to produce frames.
         */
        interface ForPostProcessor {

            /**
             * Injects a frame that represents the current state.
             *
             * @param methodVisitor The method visitor onto which to apply the stack map frame.
             * @param stack         A list of types that are currently on the stack.
             */
            void injectIntermediateFrame(MethodVisitor methodVisitor, List<? extends TypeDescription> stack);
        }

        /**
         * A stack map frame handler for an instrumented method.
         */
        interface ForInstrumentedMethod extends StackMapFrameHandler {

            /**
             * Binds this metadata handler for the enter advice.
             *
             * @param typeToken The type token representing the advice method.
             * @return An appropriate metadata handler for the enter method.
             */
            ForAdvice bindEnter(MethodDescription.TypeToken typeToken);

            /**
             * Binds this metadata handler for the exit advice.
             *
             * @param typeToken The type token representing the advice method.
             * @return An appropriate metadata handler for the enter method.
             */
            ForAdvice bindExit(MethodDescription.TypeToken typeToken);

            /**
             * Returns a hint to supply to a {@link ClassReader} when parsing an advice method.
             *
             * @return The reader hint to supply to an ASM class reader.
             */
            int getReaderHint();

            /**
             * Injects a frame after initialization if any initialization is performed.
             *
             * @param methodVisitor The method visitor to write any frames to.
             */
            void injectInitializationFrame(MethodVisitor methodVisitor);

            /**
             * Injects a frame before executing the instrumented method.
             *
             * @param methodVisitor The method visitor to write any frames to.
             */
            void injectStartFrame(MethodVisitor methodVisitor);

            /**
             * Injects a frame indicating the completion of the currently handled method, i.e. all yielded types were added.
             *
             * @param methodVisitor The method visitor onto which to apply the stack map frame.
             */
            void injectPostCompletionFrame(MethodVisitor methodVisitor);
        }

        /**
         * A stack map frame handler for an advice method.
         */
        interface ForAdvice extends StackMapFrameHandler, ForPostProcessor {
            /* marker interface */
        }

        /**
         * A non-operational stack map frame handler.
         */
        enum NoOp implements ForInstrumentedMethod, ForAdvice {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public StackMapFrameHandler.ForAdvice bindEnter(MethodDescription.TypeToken typeToken) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public StackMapFrameHandler.ForAdvice bindExit(MethodDescription.TypeToken typeToken) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public int getReaderHint() {
                return ClassReader.SKIP_FRAMES;
            }

            /**
             * {@inheritDoc}
             */
            public void translateFrame(MethodVisitor methodVisitor,
                                       int type,
                                       int localVariableLength,
                                       @MaybeNull Object[] localVariable,
                                       int stackSize,
                                       @MaybeNull Object[] stack) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void injectReturnFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void injectExceptionFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void injectCompletionFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void injectInitializationFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void injectStartFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void injectPostCompletionFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void injectIntermediateFrame(MethodVisitor methodVisitor, List<? extends TypeDescription> stack) {
                /* do nothing */
            }
        }

        /**
         * A default implementation of a stack map frame handler for an instrumented method.
         */
        abstract class Default implements ForInstrumentedMethod {

            /**
             * An empty array indicating an empty frame.
             */
            protected static final Object[] EMPTY = new Object[0];

            /**
             * The instrumented type.
             */
            protected final TypeDescription instrumentedType;

            /**
             * The instrumented method.
             */
            protected final MethodDescription instrumentedMethod;

            /**
             * A list of virtual method arguments that are explicitly added before any code execution.
             */
            protected final List<? extends TypeDescription> initialTypes;

            /**
             * A list of virtual arguments that are available after the enter advice method is executed.
             */
            protected final List<? extends TypeDescription> latentTypes;

            /**
             * A list of virtual method arguments that are available before the instrumented method is executed.
             */
            protected final List<? extends TypeDescription> preMethodTypes;

            /**
             * A list of virtual method arguments that are available after the instrumented method has completed.
             */
            protected final List<? extends TypeDescription> postMethodTypes;

            /**
             * {@code true} if the meta data handler is expected to expand its frames.
             */
            protected final boolean expandFrames;

            /**
             * The current frame's size divergence from the original local variable array.
             */
            protected int currentFrameDivergence;

            /**
             * Creates a new default stack map frame handler.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param initialTypes       A list of virtual method arguments that are explicitly added before any code execution.
             * @param latentTypes        A list of virtual arguments that are available after the enter advice method is executed.
             * @param preMethodTypes     A list of virtual method arguments that are available before the instrumented method is executed.
             * @param postMethodTypes    A list of virtual method arguments that are available after the instrumented method has completed.
             * @param expandFrames       {@code true} if the meta data handler is expected to expand its frames.
             */
            protected Default(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              List<? extends TypeDescription> initialTypes,
                              List<? extends TypeDescription> latentTypes,
                              List<? extends TypeDescription> preMethodTypes,
                              List<? extends TypeDescription> postMethodTypes,
                              boolean expandFrames) {
                this.instrumentedType = instrumentedType;
                this.instrumentedMethod = instrumentedMethod;
                this.initialTypes = initialTypes;
                this.latentTypes = latentTypes;
                this.preMethodTypes = preMethodTypes;
                this.postMethodTypes = postMethodTypes;
                this.expandFrames = expandFrames;
            }

            /**
             * Creates an appropriate stack map frame handler for an instrumented method.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param initialTypes       A list of virtual method arguments that are explicitly added before any code execution.
             * @param latentTypes        A list of virtual arguments that are available after the enter advice method is executed.
             * @param preMethodTypes     A list of virtual method arguments that are available before the instrumented method is executed.
             * @param postMethodTypes    A list of virtual method arguments that are available after the instrumented method has completed.
             * @param exitAdvice         {@code true} if the current advice implies exit advice.
             * @param copyArguments      {@code true} if the original arguments are copied before invoking the instrumented method.
             * @param classFileVersion   The instrumented type's class file version.
             * @param writerFlags        The flags supplied to the ASM writer.
             * @param readerFlags        The reader flags supplied to the ASM reader.
             * @return An appropriate stack map frame handler for an instrumented method.
             */
            protected static ForInstrumentedMethod of(TypeDescription instrumentedType,
                                                      MethodDescription instrumentedMethod,
                                                      List<? extends TypeDescription> initialTypes,
                                                      List<? extends TypeDescription> latentTypes,
                                                      List<? extends TypeDescription> preMethodTypes,
                                                      List<? extends TypeDescription> postMethodTypes,
                                                      boolean exitAdvice,
                                                      boolean copyArguments,
                                                      ClassFileVersion classFileVersion,
                                                      int writerFlags,
                                                      int readerFlags) {
                if ((writerFlags & ClassWriter.COMPUTE_FRAMES) != 0 || classFileVersion.isLessThan(ClassFileVersion.JAVA_V6)) {
                    return NoOp.INSTANCE;
                } else if (!exitAdvice && initialTypes.isEmpty()) {
                    return new Trivial(instrumentedType,
                            instrumentedMethod,
                            latentTypes,
                            (readerFlags & ClassReader.EXPAND_FRAMES) != 0);
                } else if (copyArguments) {
                    return new WithPreservedArguments.WithArgumentCopy(instrumentedType,
                            instrumentedMethod,
                            initialTypes,
                            latentTypes,
                            preMethodTypes,
                            postMethodTypes,
                            (readerFlags & ClassReader.EXPAND_FRAMES) != 0);
                } else {
                    return new WithPreservedArguments.WithoutArgumentCopy(instrumentedType,
                            instrumentedMethod,
                            initialTypes,
                            latentTypes,
                            preMethodTypes,
                            postMethodTypes,
                            (readerFlags & ClassReader.EXPAND_FRAMES) != 0,
                            !instrumentedMethod.isConstructor());
                }
            }

            /**
             * {@inheritDoc}
             */
            public StackMapFrameHandler.ForAdvice bindEnter(MethodDescription.TypeToken typeToken) {
                return new ForAdvice(typeToken, initialTypes, latentTypes, preMethodTypes, TranslationMode.ENTER, instrumentedMethod.isConstructor()
                        ? Initialization.UNITIALIZED
                        : Initialization.INITIALIZED);
            }

            /**
             * {@inheritDoc}
             */
            public int getReaderHint() {
                return expandFrames
                        ? ClassReader.EXPAND_FRAMES
                        : AsmVisitorWrapper.NO_FLAGS;
            }

            /**
             * Translates a frame.
             *
             * @param methodVisitor       The method visitor to write the frame to.
             * @param translationMode     The translation mode to apply.
             * @param isStatic            {@code true} if the targeted type token represents a static method.
             * @param typeToken           A type token for the method description for which the frame is written.
             * @param additionalTypes     The additional types to consider part of the instrumented method's parameters.
             * @param type                The frame's type.
             * @param localVariableLength The local variable length.
             * @param localVariable       An array containing the types of the current local variables.
             * @param stackSize           The size of the operand stack.
             * @param stack               An array containing the types of the current operand stack.
             */
            protected void translateFrame(MethodVisitor methodVisitor,
                                          TranslationMode translationMode,
                                          boolean isStatic,
                                          MethodDescription.TypeToken typeToken,
                                          List<? extends TypeDescription> additionalTypes,
                                          int type,
                                          int localVariableLength,
                                          @MaybeNull Object[] localVariable,
                                          int stackSize,
                                          @MaybeNull Object[] stack) {
                switch (type) {
                    case Opcodes.F_SAME:
                    case Opcodes.F_SAME1:
                        break;
                    case Opcodes.F_APPEND:
                        currentFrameDivergence += localVariableLength;
                        break;
                    case Opcodes.F_CHOP:
                        currentFrameDivergence -= localVariableLength;
                        if (currentFrameDivergence < 0) {
                            throw new IllegalStateException(typeToken + " dropped " + Math.abs(currentFrameDivergence) + " implicit frames");
                        }
                        break;
                    case Opcodes.F_FULL:
                    case Opcodes.F_NEW:
                        if (typeToken.getParameterTypes().size() + (isStatic ? 0 : 1) > localVariableLength) {
                            throw new IllegalStateException("Inconsistent frame length for " + typeToken + ": " + localVariableLength);
                        }
                        int offset;
                        if (isStatic) {
                            offset = 0;
                        } else {
                            if (!translationMode.isPossibleThisFrameValue(instrumentedType, instrumentedMethod, localVariable[0])) {
                                throw new IllegalStateException(typeToken + " is inconsistent for 'this' reference: " + localVariable[0]);
                            }
                            offset = 1;
                        }
                        for (int index = 0; index < typeToken.getParameterTypes().size(); index++) {
                            if (!Initialization.INITIALIZED.toFrame(typeToken.getParameterTypes().get(index)).equals(localVariable[index + offset])) {
                                throw new IllegalStateException(typeToken + " is inconsistent at " + index + ": " + localVariable[index + offset]);
                            }
                        }
                        Object[] translated = new Object[localVariableLength
                                - (isStatic ? 0 : 1)
                                - typeToken.getParameterTypes().size()
                                + (instrumentedMethod.isStatic() ? 0 : 1)
                                + instrumentedMethod.getParameters().size()
                                + additionalTypes.size()];
                        int index = translationMode.copy(instrumentedType, instrumentedMethod, localVariable, translated);
                        for (TypeDescription typeDescription : additionalTypes) {
                            translated[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                        }
                        if (translated.length != index) {
                            System.arraycopy(localVariable,
                                    typeToken.getParameterTypes().size() + (isStatic ? 0 : 1),
                                    translated,
                                    index,
                                    translated.length - index);
                        }
                        localVariableLength = translated.length;
                        localVariable = translated;
                        currentFrameDivergence = translated.length - index;
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected frame type: " + type);
                }
                methodVisitor.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
            }

            /**
             * Injects a full stack map frame after the instrumented method has completed.
             *
             * @param methodVisitor  The method visitor onto which to write the stack map frame.
             * @param initialization The initialization to apply when resolving a reference to the instance on which a non-static method is invoked.
             * @param typesInArray   The types that were added to the local variable array additionally to the values of the instrumented method.
             * @param typesOnStack   The types currently on the operand stack.
             */
            protected void injectFullFrame(MethodVisitor methodVisitor,
                                           Initialization initialization,
                                           List<? extends TypeDescription> typesInArray,
                                           List<? extends TypeDescription> typesOnStack) {
                Object[] localVariable = new Object[instrumentedMethod.getParameters().size()
                        + (instrumentedMethod.isStatic() ? 0 : 1)
                        + typesInArray.size()];
                int index = 0;
                if (!instrumentedMethod.isStatic()) {
                    localVariable[index++] = initialization.toFrame(instrumentedType);
                }
                for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                    localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                }
                for (TypeDescription typeDescription : typesInArray) {
                    localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                }
                index = 0;
                Object[] stackType = new Object[typesOnStack.size()];
                for (TypeDescription typeDescription : typesOnStack) {
                    stackType[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                }
                methodVisitor.visitFrame(expandFrames ? Opcodes.F_NEW : Opcodes.F_FULL, localVariable.length, localVariable, stackType.length, stackType);
                currentFrameDivergence = 0;
            }

            /**
             * A translation mode that determines how the fixed frames of the instrumented method are written.
             */
            protected enum TranslationMode {

                /**
                 * A translation mode that simply copies the original frames which are available when translating frames of the instrumented method.
                 */
                COPY {
                    @Override
                    protected int copy(TypeDescription instrumentedType,
                                       MethodDescription instrumentedMethod,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int length = instrumentedMethod.getParameters().size() + (instrumentedMethod.isStatic() ? 0 : 1);
                        System.arraycopy(localVariable, 0, translated, 0, length);
                        return length;
                    }

                    @Override
                    protected boolean isPossibleThisFrameValue(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Object frame) {
                        return instrumentedMethod.isConstructor() && Opcodes.UNINITIALIZED_THIS.equals(frame) || Initialization.INITIALIZED.toFrame(instrumentedType).equals(frame);
                    }
                },

                /**
                 * A translation mode for the enter advice that considers that the {@code this} reference might not be initialized for a constructor.
                 */
                ENTER {
                    @Override
                    protected int copy(TypeDescription instrumentedType,
                                       MethodDescription instrumentedMethod,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int index = 0;
                        if (!instrumentedMethod.isStatic()) {
                            translated[index++] = instrumentedMethod.isConstructor()
                                    ? Opcodes.UNINITIALIZED_THIS
                                    : Initialization.INITIALIZED.toFrame(instrumentedType);
                        }
                        for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                            translated[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                        }
                        return index;
                    }

                    @Override
                    protected boolean isPossibleThisFrameValue(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Object frame) {
                        return instrumentedMethod.isConstructor()
                                ? Opcodes.UNINITIALIZED_THIS.equals(frame)
                                : Initialization.INITIALIZED.toFrame(instrumentedType).equals(frame);
                    }
                },

                /**
                 * A translation mode for an exit advice where the {@code this} reference is always initialized.
                 */
                EXIT {
                    @Override
                    protected int copy(TypeDescription instrumentedType,
                                       MethodDescription instrumentedMethod,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int index = 0;
                        if (!instrumentedMethod.isStatic()) {
                            translated[index++] = Initialization.INITIALIZED.toFrame(instrumentedType);
                        }
                        for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                            translated[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                        }
                        return index;
                    }

                    @Override
                    protected boolean isPossibleThisFrameValue(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Object frame) {
                        return Initialization.INITIALIZED.toFrame(instrumentedType).equals(frame);
                    }
                };

                /**
                 * Copies the fixed parameters of the instrumented method onto the operand stack.
                 *
                 * @param instrumentedType   The instrumented type.
                 * @param instrumentedMethod The instrumented method.
                 * @param localVariable      The original local variable array.
                 * @param translated         The array containing the translated frames.
                 * @return The amount of frames added to the translated frame array.
                 */
                protected abstract int copy(TypeDescription instrumentedType,
                                            MethodDescription instrumentedMethod,
                                            Object[] localVariable,
                                            Object[] translated);

                /**
                 * Checks if a variable value in a stack map frame is a legal value for describing a {@code this} reference.
                 *
                 * @param instrumentedType   The instrumented type.
                 * @param instrumentedMethod The instrumented method.
                 * @param frame              The frame value representing the {@code this} reference.
                 * @return {@code true} if the value is a legal representation of the {@code this} reference.
                 */
                protected abstract boolean isPossibleThisFrameValue(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Object frame);
            }

            /**
             * Represents the initialization state of a stack value that can either be initialized or uninitialized.
             */
            protected enum Initialization {

                /**
                 * Represents an uninitialized frame value within a constructor before invoking the super constructor.
                 */
                UNITIALIZED {
                    /**
                     * {@inheritDoc}
                     */
                    protected Object toFrame(TypeDescription typeDescription) {
                        if (typeDescription.isPrimitive()) {
                            throw new IllegalArgumentException("Cannot assume primitive uninitialized value: " + typeDescription);
                        }
                        return Opcodes.UNINITIALIZED_THIS;
                    }
                },

                /**
                 * Represents an initialized frame value.
                 */
                INITIALIZED {
                    /**
                     * {@inheritDoc}
                     */
                    protected Object toFrame(TypeDescription typeDescription) {
                        if (typeDescription.represents(boolean.class)
                                || typeDescription.represents(byte.class)
                                || typeDescription.represents(short.class)
                                || typeDescription.represents(char.class)
                                || typeDescription.represents(int.class)) {
                            return Opcodes.INTEGER;
                        } else if (typeDescription.represents(long.class)) {
                            return Opcodes.LONG;
                        } else if (typeDescription.represents(float.class)) {
                            return Opcodes.FLOAT;
                        } else if (typeDescription.represents(double.class)) {
                            return Opcodes.DOUBLE;
                        } else {
                            return typeDescription.getInternalName();
                        }
                    }
                };

                /**
                 * Initializes a frame value to its frame type.
                 *
                 * @param typeDescription The type being resolved.
                 * @return The frame value.
                 */
                protected abstract Object toFrame(TypeDescription typeDescription);
            }

            /**
             * A trivial stack map frame handler that applies a trivial translation for the instrumented method's stack map frames.
             */
            protected static class Trivial extends Default {

                /**
                 * Creates a new stack map frame handler that applies a trivial translation for the instrumented method's stack map frames.
                 *
                 * @param instrumentedType   The instrumented type.
                 * @param instrumentedMethod The instrumented method.
                 * @param latentTypes        A list of virtual arguments that are available after the enter advice method is executed.
                 * @param expandFrames       {@code true} if the meta data handler is expected to expand its frames.
                 */
                protected Trivial(TypeDescription instrumentedType, MethodDescription instrumentedMethod, List<? extends TypeDescription> latentTypes, boolean expandFrames) {
                    super(instrumentedType,
                            instrumentedMethod,
                            Collections.<TypeDescription>emptyList(),
                            latentTypes,
                            Collections.<TypeDescription>emptyList(),
                            Collections.<TypeDescription>emptyList(),
                            expandFrames);
                }

                /**
                 * {@inheritDoc}
                 */
                public void translateFrame(MethodVisitor methodVisitor,
                                           int type,
                                           int localVariableLength,
                                           @MaybeNull Object[] localVariable,
                                           int stackSize,
                                           @MaybeNull Object[] stack) {
                    methodVisitor.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
                }

                /**
                 * {@inheritDoc}
                 */
                public StackMapFrameHandler.ForAdvice bindExit(MethodDescription.TypeToken typeToken) {
                    throw new IllegalStateException("Did not expect exit advice " + typeToken + " for " + instrumentedMethod);
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectReturnFrame(MethodVisitor methodVisitor) {
                    throw new IllegalStateException("Did not expect return frame for " + instrumentedMethod);
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectExceptionFrame(MethodVisitor methodVisitor) {
                    throw new IllegalStateException("Did not expect exception frame for " + instrumentedMethod);
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectCompletionFrame(MethodVisitor methodVisitor) {
                    throw new IllegalStateException("Did not expect completion frame for " + instrumentedMethod);
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectPostCompletionFrame(MethodVisitor methodVisitor) {
                    throw new IllegalStateException("Did not expect post completion frame for " + instrumentedMethod);
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectInitializationFrame(MethodVisitor methodVisitor) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectStartFrame(MethodVisitor methodVisitor) {
                    /* do nothing */
                }
            }

            /**
             * A stack map frame handler that requires the original arguments of the instrumented method to be preserved in their original form.
             */
            protected abstract static class WithPreservedArguments extends Default {

                /**
                 * {@code true} if a completion frame for the method bust be a full frame to reflect an initialization change.
                 */
                protected boolean allowCompactCompletionFrame;

                /**
                 * Creates a new stack map frame handler that requires the stack map frames of the original arguments to be preserved.
                 *
                 * @param instrumentedType            The instrumented type.
                 * @param instrumentedMethod          The instrumented method.
                 * @param initialTypes                A list of virtual method arguments that are explicitly added before any code execution.
                 * @param latentTypes                 A list of virtual arguments that are available after the enter advice method is executed.
                 * @param preMethodTypes              A list of virtual method arguments that are available before the instrumented method is executed.
                 * @param postMethodTypes             A list of virtual method arguments that are available after the instrumented method has completed.
                 * @param expandFrames                {@code true} if the meta data handler is expected to expand its frames.
                 * @param allowCompactCompletionFrame {@code true} if a completion frame for the method bust be a full frame to reflect an initialization change.
                 */
                protected WithPreservedArguments(TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 List<? extends TypeDescription> initialTypes,
                                                 List<? extends TypeDescription> latentTypes,
                                                 List<? extends TypeDescription> preMethodTypes,
                                                 List<? extends TypeDescription> postMethodTypes,
                                                 boolean expandFrames,
                                                 boolean allowCompactCompletionFrame) {
                    super(instrumentedType, instrumentedMethod, initialTypes, latentTypes, preMethodTypes, postMethodTypes, expandFrames);
                    this.allowCompactCompletionFrame = allowCompactCompletionFrame;
                }

                @Override
                @SuppressFBWarnings(value = "RC_REF_COMPARISON_BAD_PRACTICE", justification = "ASM models frames by reference identity.")
                protected void translateFrame(MethodVisitor methodVisitor,
                                              TranslationMode translationMode,
                                              boolean isStatic,
                                              MethodDescription.TypeToken typeToken,
                                              List<? extends TypeDescription> additionalTypes,
                                              int type,
                                              int localVariableLength,
                                              @MaybeNull Object[] localVariable,
                                              int stackSize,
                                              @MaybeNull Object[] stack) {
                    if (type == Opcodes.F_FULL && localVariableLength > 0 && localVariable[0] != Opcodes.UNINITIALIZED_THIS) {
                        allowCompactCompletionFrame = true;
                    }
                    super.translateFrame(methodVisitor, translationMode, isStatic, typeToken, additionalTypes, type, localVariableLength, localVariable, stackSize, stack);
                }

                /**
                 * {@inheritDoc}
                 */
                public StackMapFrameHandler.ForAdvice bindExit(MethodDescription.TypeToken typeToken) {
                    return new ForAdvice(typeToken,
                            CompoundList.of(initialTypes, preMethodTypes, postMethodTypes),
                            Collections.<TypeDescription>emptyList(),
                            Collections.<TypeDescription>emptyList(),
                            TranslationMode.EXIT,
                            Initialization.INITIALIZED);
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectReturnFrame(MethodVisitor methodVisitor) {
                    if (!expandFrames && currentFrameDivergence == 0) {
                        if (instrumentedMethod.getReturnType().represents(void.class)) {
                            methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                        } else {
                            methodVisitor.visitFrame(Opcodes.F_SAME1,
                                    EMPTY.length,
                                    EMPTY,
                                    1,
                                    new Object[]{Initialization.INITIALIZED.toFrame(instrumentedMethod.getReturnType().asErasure())});
                        }
                    } else {
                        injectFullFrame(methodVisitor, Initialization.INITIALIZED, CompoundList.of(initialTypes, preMethodTypes),
                                instrumentedMethod.getReturnType().represents(void.class)
                                        ? Collections.<TypeDescription>emptyList()
                                        : Collections.singletonList(instrumentedMethod.getReturnType().asErasure()));
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectExceptionFrame(MethodVisitor methodVisitor) {
                    if (!expandFrames && currentFrameDivergence == 0) {
                        methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{Type.getInternalName(Throwable.class)});
                    } else {
                        injectFullFrame(methodVisitor, Initialization.INITIALIZED, CompoundList.of(initialTypes, preMethodTypes),
                                Collections.singletonList(TypeDescription.ForLoadedType.of(Throwable.class)));
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectCompletionFrame(MethodVisitor methodVisitor) {
                    if (allowCompactCompletionFrame && !expandFrames && currentFrameDivergence == 0 && postMethodTypes.size() < 4) {
                        if (postMethodTypes.isEmpty()) {
                            methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                        } else {
                            Object[] local = new Object[postMethodTypes.size()];
                            int index = 0;
                            for (TypeDescription typeDescription : postMethodTypes) {
                                local[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                            }
                            methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, EMPTY.length, EMPTY);
                        }
                    } else {
                        injectFullFrame(methodVisitor, Initialization.INITIALIZED, CompoundList.of(initialTypes, preMethodTypes, postMethodTypes),
                                Collections.<TypeDescription>emptyList());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectPostCompletionFrame(MethodVisitor methodVisitor) {
                    if (!expandFrames && currentFrameDivergence == 0) {
                        methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                    } else {
                        injectFullFrame(methodVisitor, Initialization.INITIALIZED, CompoundList.of(initialTypes, preMethodTypes, postMethodTypes),
                                Collections.<TypeDescription>emptyList());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectInitializationFrame(MethodVisitor methodVisitor) {
                    if (!initialTypes.isEmpty()) {
                        if (!expandFrames && initialTypes.size() < 4) {
                            Object[] localVariable = new Object[initialTypes.size()];
                            int index = 0;
                            for (TypeDescription typeDescription : initialTypes) {
                                localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                            }
                            methodVisitor.visitFrame(Opcodes.F_APPEND, localVariable.length, localVariable, EMPTY.length, EMPTY);
                        } else {
                            Object[] localVariable = new Object[(instrumentedMethod.isStatic() ? 0 : 1)
                                    + instrumentedMethod.getParameters().size()
                                    + initialTypes.size()];
                            int index = 0;
                            if (instrumentedMethod.isConstructor()) {
                                localVariable[index++] = Opcodes.UNINITIALIZED_THIS;
                            } else if (!instrumentedMethod.isStatic()) {
                                localVariable[index++] = Initialization.INITIALIZED.toFrame(instrumentedType);
                            }
                            for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                                localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                            }
                            for (TypeDescription typeDescription : initialTypes) {
                                localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                            }
                            methodVisitor.visitFrame(expandFrames ? Opcodes.F_NEW : Opcodes.F_FULL, localVariable.length, localVariable, EMPTY.length, EMPTY);
                        }
                    }
                }

                /**
                 * A stack map frame handler that expects that the original argument frames remain preserved throughout the original invocation.
                 */
                protected static class WithoutArgumentCopy extends WithPreservedArguments {

                    /**
                     * Creates a new stack map frame handler that expects the original frames to be preserved.
                     *
                     * @param instrumentedType            The instrumented type.
                     * @param instrumentedMethod          The instrumented method.
                     * @param initialTypes                A list of virtual method arguments that are explicitly added before any code execution.
                     * @param latentTypes                 A list of virtual arguments that are available after the enter advice method is executed.
                     * @param preMethodTypes              A list of virtual method arguments that are available before the instrumented method is executed.
                     * @param postMethodTypes             A list of virtual method arguments that are available after the instrumented method has completed.
                     * @param expandFrames                {@code true} if the meta data handler is expected to expand its frames.
                     * @param allowCompactCompletionFrame {@code true} if a completion frame for the method bust be a full frame to reflect an initialization change.
                     */
                    protected WithoutArgumentCopy(TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  List<? extends TypeDescription> initialTypes,
                                                  List<? extends TypeDescription> latentTypes,
                                                  List<? extends TypeDescription> preMethodTypes,
                                                  List<? extends TypeDescription> postMethodTypes,
                                                  boolean expandFrames,
                                                  boolean allowCompactCompletionFrame) {
                        super(instrumentedType, instrumentedMethod, initialTypes, latentTypes, preMethodTypes, postMethodTypes, expandFrames, allowCompactCompletionFrame);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void injectStartFrame(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void translateFrame(MethodVisitor methodVisitor,
                                               int type,
                                               int localVariableLength,
                                               @MaybeNull Object[] localVariable,
                                               int stackSize,
                                               @MaybeNull Object[] stack) {
                        translateFrame(methodVisitor,
                                TranslationMode.COPY,
                                instrumentedMethod.isStatic(),
                                instrumentedMethod.asTypeToken(),
                                CompoundList.of(initialTypes, preMethodTypes),
                                type,
                                localVariableLength,
                                localVariable,
                                stackSize,
                                stack);
                    }
                }

                /**
                 * A stack map frame handler that expects that an argument copy of the original method arguments was made.
                 */
                protected static class WithArgumentCopy extends WithPreservedArguments {

                    /**
                     * Creates a new stack map frame handler that expects an argument copy.
                     *
                     * @param instrumentedType   The instrumented type.
                     * @param instrumentedMethod The instrumented method.
                     * @param initialTypes       A list of virtual method arguments that are explicitly added before any code execution.
                     * @param latentTypes        The types that are given post execution of a possible enter advice.
                     * @param preMethodTypes     A list of virtual method arguments that are available before the instrumented method is executed.
                     * @param postMethodTypes    A list of virtual method arguments that are available after the instrumented method has completed.
                     * @param expandFrames       {@code true} if the meta data handler is expected to expand its frames.
                     */
                    protected WithArgumentCopy(TypeDescription instrumentedType,
                                               MethodDescription instrumentedMethod,
                                               List<? extends TypeDescription> initialTypes,
                                               List<? extends TypeDescription> latentTypes,
                                               List<? extends TypeDescription> preMethodTypes,
                                               List<? extends TypeDescription> postMethodTypes,
                                               boolean expandFrames) {
                        super(instrumentedType, instrumentedMethod, initialTypes, latentTypes, preMethodTypes, postMethodTypes, expandFrames, true);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void injectStartFrame(MethodVisitor methodVisitor) {
                        if (!instrumentedMethod.isStatic() || !instrumentedMethod.getParameters().isEmpty()) {
                            if (!expandFrames && (instrumentedMethod.isStatic() ? 0 : 1) + instrumentedMethod.getParameters().size() < 4) {
                                Object[] localVariable = new Object[(instrumentedMethod.isStatic() ? 0 : 1) + instrumentedMethod.getParameters().size()];
                                int index = 0;
                                if (instrumentedMethod.isConstructor()) {
                                    localVariable[index++] = Opcodes.UNINITIALIZED_THIS;
                                } else if (!instrumentedMethod.isStatic()) {
                                    localVariable[index++] = Initialization.INITIALIZED.toFrame(instrumentedType);
                                }
                                for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                                    localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                                }
                                methodVisitor.visitFrame(Opcodes.F_APPEND, localVariable.length, localVariable, EMPTY.length, EMPTY);
                            } else {
                                Object[] localVariable = new Object[(instrumentedMethod.isStatic() ? 0 : 2)
                                        + instrumentedMethod.getParameters().size() * 2
                                        + initialTypes.size()
                                        + preMethodTypes.size()];
                                int index = 0;
                                if (instrumentedMethod.isConstructor()) {
                                    localVariable[index++] = Opcodes.UNINITIALIZED_THIS;
                                } else if (!instrumentedMethod.isStatic()) {
                                    localVariable[index++] = Initialization.INITIALIZED.toFrame(instrumentedType);
                                }
                                for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                                    localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                                }
                                for (TypeDescription typeDescription : initialTypes) {
                                    localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                                }
                                for (TypeDescription typeDescription : preMethodTypes) {
                                    localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                                }
                                if (instrumentedMethod.isConstructor()) {
                                    localVariable[index++] = Opcodes.UNINITIALIZED_THIS;
                                } else if (!instrumentedMethod.isStatic()) {
                                    localVariable[index++] = Initialization.INITIALIZED.toFrame(instrumentedType);
                                }
                                for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                                    localVariable[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                                }
                                methodVisitor.visitFrame(expandFrames ? Opcodes.F_NEW : Opcodes.F_FULL, localVariable.length, localVariable, EMPTY.length, EMPTY);
                            }
                        }
                        currentFrameDivergence = (instrumentedMethod.isStatic() ? 0 : 1) + instrumentedMethod.getParameters().size();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressFBWarnings(value = "RC_REF_COMPARISON_BAD_PRACTICE", justification = "ASM models frames by reference identity.")
                    public void translateFrame(MethodVisitor methodVisitor,
                                               int type,
                                               int localVariableLength,
                                               @MaybeNull Object[] localVariable,
                                               int stackSize,
                                               @MaybeNull Object[] stack) {
                        switch (type) {
                            case Opcodes.F_SAME:
                            case Opcodes.F_SAME1:
                                break;
                            case Opcodes.F_APPEND:
                                currentFrameDivergence += localVariableLength;
                                break;
                            case Opcodes.F_CHOP:
                                currentFrameDivergence -= localVariableLength;
                                break;
                            case Opcodes.F_FULL:
                            case Opcodes.F_NEW:
                                Object[] translated = new Object[localVariableLength
                                        + (instrumentedMethod.isStatic() ? 0 : 1)
                                        + instrumentedMethod.getParameters().size()
                                        + initialTypes.size()
                                        + preMethodTypes.size()];
                                int index = 0;
                                if (instrumentedMethod.isConstructor()) {
                                    Initialization initialization = Initialization.INITIALIZED;
                                    for (int variableIndex = 0; variableIndex < localVariableLength; variableIndex++) {
                                        if (localVariable[variableIndex] == Opcodes.UNINITIALIZED_THIS) {
                                            initialization = Initialization.UNITIALIZED;
                                            break;
                                        }
                                    }
                                    translated[index++] = initialization.toFrame(instrumentedType);
                                } else if (!instrumentedMethod.isStatic()) {
                                    translated[index++] = Initialization.INITIALIZED.toFrame(instrumentedType);
                                }
                                for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                                    translated[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                                }
                                for (TypeDescription typeDescription : initialTypes) {
                                    translated[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                                }
                                for (TypeDescription typeDescription : preMethodTypes) {
                                    translated[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                                }
                                if (localVariableLength > 0) {
                                    System.arraycopy(localVariable, 0, translated, index, localVariableLength);
                                }
                                localVariableLength = translated.length;
                                localVariable = translated;
                                currentFrameDivergence = localVariableLength;
                                break;
                            default:
                                throw new IllegalArgumentException("Unexpected frame type: " + type);
                        }
                        if (instrumentedMethod.isConstructor() && currentFrameDivergence < instrumentedMethod.getStackSize()) {
                            throw new IllegalStateException(instrumentedMethod + " dropped implicit 'this' frame");
                        }
                        methodVisitor.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
                    }
                }
            }

            /**
             * A stack map frame handler for an advice method.
             */
            protected class ForAdvice implements StackMapFrameHandler.ForAdvice {

                /**
                 * A token for the method description for which frames are translated.
                 */
                protected final MethodDescription.TypeToken typeToken;

                /**
                 * The types provided before execution of the advice code.
                 */
                protected final List<? extends TypeDescription> startTypes;

                /**
                 * The types that are given post execution of the advice.
                 */
                private final List<? extends TypeDescription> intermediateTypes;

                /**
                 * The types provided after execution of the advice code.
                 */
                protected final List<? extends TypeDescription> endTypes;

                /**
                 * The translation mode to apply for this advice method. Should be either {@link TranslationMode#ENTER} or {@link TranslationMode#EXIT}.
                 */
                protected final TranslationMode translationMode;

                /**
                 * The initialization to apply when resolving a reference to the instance on which a non-static method is invoked.
                 */
                private final Initialization initialization;

                /**
                 * {@code true} if an intermediate frame was yielded.
                 */
                private boolean intermediate;

                /**
                 * Creates a new metadata handler for an advice method.
                 *
                 * @param typeToken         A token for the method description for which frames are translated.
                 * @param startTypes        The types provided before execution of the advice code.
                 * @param intermediateTypes The types that are given post execution of the advice.
                 * @param endTypes          The types provided after execution of the advice code.
                 * @param translationMode   The translation mode to apply for this advice method. Should be
                 *                          either {@link TranslationMode#ENTER} or {@link TranslationMode#EXIT}.
                 * @param initialization    The initialization to apply when resolving a reference to the instance on which a non-static method is invoked.
                 */
                protected ForAdvice(MethodDescription.TypeToken typeToken,
                                    List<? extends TypeDescription> startTypes,
                                    List<? extends TypeDescription> intermediateTypes,
                                    List<? extends TypeDescription> endTypes,
                                    TranslationMode translationMode,
                                    Initialization initialization) {
                    this.typeToken = typeToken;
                    this.startTypes = startTypes;
                    this.intermediateTypes = intermediateTypes;
                    this.endTypes = endTypes;
                    this.translationMode = translationMode;
                    this.initialization = initialization;
                    intermediate = false;
                }

                /**
                 * {@inheritDoc}
                 */
                public void translateFrame(MethodVisitor methodVisitor,
                                           int type,
                                           int localVariableLength,
                                           @MaybeNull Object[] localVariable,
                                           int stackSize,
                                           @MaybeNull Object[] stack) {
                    Default.this.translateFrame(methodVisitor,
                            translationMode,
                            true,
                            typeToken,
                            startTypes,
                            type,
                            localVariableLength,
                            localVariable,
                            stackSize,
                            stack);
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectReturnFrame(MethodVisitor methodVisitor) {
                    if (!expandFrames && currentFrameDivergence == 0) {
                        if (typeToken.getReturnType().represents(void.class)) {
                            methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                        } else {
                            methodVisitor.visitFrame(Opcodes.F_SAME1,
                                    EMPTY.length,
                                    EMPTY,
                                    1,
                                    new Object[]{Initialization.INITIALIZED.toFrame(typeToken.getReturnType())});
                        }
                    } else {
                        injectFullFrame(methodVisitor, initialization, startTypes, typeToken.getReturnType().represents(void.class)
                                ? Collections.<TypeDescription>emptyList()
                                : Collections.singletonList(typeToken.getReturnType()));
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectExceptionFrame(MethodVisitor methodVisitor) {
                    if (!expandFrames && currentFrameDivergence == 0) {
                        methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{Type.getInternalName(Throwable.class)});
                    } else {
                        injectFullFrame(methodVisitor, initialization, startTypes, Collections.singletonList(TypeDescription.ForLoadedType.of(Throwable.class)));
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectCompletionFrame(MethodVisitor methodVisitor) {
                    if (expandFrames) {
                        injectFullFrame(methodVisitor, initialization, CompoundList.of(startTypes, endTypes), Collections.<TypeDescription>emptyList());
                    } else if (currentFrameDivergence == 0 && (intermediate || endTypes.size() < 4)) {
                        if (intermediate || endTypes.isEmpty()) {
                            methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                        } else {
                            Object[] local = new Object[endTypes.size()];
                            int index = 0;
                            for (TypeDescription typeDescription : endTypes) {
                                local[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                            }
                            methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, EMPTY.length, EMPTY);
                        }
                    } else if (currentFrameDivergence < 3 && endTypes.isEmpty()) {
                        methodVisitor.visitFrame(Opcodes.F_CHOP, currentFrameDivergence, EMPTY, EMPTY.length, EMPTY);
                        currentFrameDivergence = 0;
                    } else {
                        injectFullFrame(methodVisitor, initialization, CompoundList.of(startTypes, endTypes), Collections.<TypeDescription>emptyList());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void injectIntermediateFrame(MethodVisitor methodVisitor, List<? extends TypeDescription> stack) {
                    if (expandFrames) {
                        injectFullFrame(methodVisitor, initialization, CompoundList.of(startTypes, intermediateTypes), stack);
                    } else if (intermediate && stack.size() < 2) {
                        if (stack.isEmpty()) {
                            methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                        } else {
                            methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{Initialization.INITIALIZED.toFrame(stack.get(0))});
                        }
                    } else if (currentFrameDivergence == 0
                            && intermediateTypes.size() < 4
                            && (stack.isEmpty() || stack.size() < 2 && intermediateTypes.isEmpty())) {
                        if (intermediateTypes.isEmpty()) {
                            if (stack.isEmpty()) {
                                methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                            } else {
                                methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{Initialization.INITIALIZED.toFrame(stack.get(0))});
                            }
                        } else {
                            Object[] local = new Object[intermediateTypes.size()];
                            int index = 0;
                            for (TypeDescription typeDescription : intermediateTypes) {
                                local[index++] = Initialization.INITIALIZED.toFrame(typeDescription);
                            }
                            methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, EMPTY.length, EMPTY);
                        }
                    } else if (currentFrameDivergence < 3 && intermediateTypes.isEmpty() && stack.isEmpty()) {
                        methodVisitor.visitFrame(Opcodes.F_CHOP, currentFrameDivergence, EMPTY, EMPTY.length, EMPTY);
                    } else {
                        injectFullFrame(methodVisitor, initialization, CompoundList.of(startTypes, intermediateTypes), stack);
                    }
                    currentFrameDivergence = intermediateTypes.size() - endTypes.size();
                    intermediate = true;
                }
            }
        }
    }

    /**
     * An exception handler is responsible for providing byte code for handling an exception thrown from a suppressing advice method.
     */
    public interface ExceptionHandler {

        /**
         * Resolves a stack manipulation to apply.
         *
         * @param instrumentedMethod The instrumented method.
         * @param instrumentedType   The instrumented type.
         * @return The stack manipulation to use.
         */
        StackManipulation resolve(MethodDescription instrumentedMethod, TypeDescription instrumentedType);

        /**
         * Default implementations for commonly used exception handlers.
         */
        enum Default implements ExceptionHandler {

            /**
             * An exception handler the suppresses the exception.
             */
            SUPPRESSING {
                /** {@inheritDoc} */
                public StackManipulation resolve(MethodDescription instrumentedMethod, TypeDescription instrumentedType) {
                    return Removal.SINGLE;
                }
            },

            /**
             * An exception handler that invokes {@link Throwable#printStackTrace()}.
             */
            PRINTING {
                /** {@inheritDoc} */
                public StackManipulation resolve(MethodDescription instrumentedMethod, TypeDescription instrumentedType) {
                    try {
                        return MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(Throwable.class.getMethod("printStackTrace")));
                    } catch (NoSuchMethodException exception) {
                        throw new IllegalStateException("Cannot locate Throwable::printStackTrace");
                    }
                }
            },

            /**
             * An exception handler that rethrows any suppressed {@link Throwable}. Normally, it is preferable to avoid suppression
             * altogether rather then to rethrow them. It can however be desired to make this behavior configurable where using this
             * handler allows to effectively disable the suppression.
             */
            RETHROWING {
                /** {@inheritDoc} */
                public StackManipulation resolve(MethodDescription instrumentedMethod, TypeDescription instrumentedType) {
                    return Throw.INSTANCE;
                }
            }
        }

        /**
         * A simple exception handler that returns a fixed stack manipulation.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Simple implements ExceptionHandler {

            /**
             * The stack manipulation to execute.
             */
            private final StackManipulation stackManipulation;

            /**
             * Creates a new simple exception handler.
             *
             * @param stackManipulation The stack manipulation to execute.
             */
            public Simple(StackManipulation stackManipulation) {
                this.stackManipulation = stackManipulation;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation resolve(MethodDescription instrumentedMethod, TypeDescription instrumentedType) {
                return stackManipulation;
            }
        }
    }

    /**
     * A dispatcher for implementing advice.
     */
    protected interface Dispatcher {

        /**
         * Indicates that a method does not represent advice and does not need to be visited.
         */
        @AlwaysNull
        MethodVisitor IGNORE_METHOD = null;

        /**
         * Expresses that an annotation should not be visited.
         */
        @AlwaysNull
        AnnotationVisitor IGNORE_ANNOTATION = null;

        /**
         * Returns {@code true} if this dispatcher is alive.
         *
         * @return {@code true} if this dispatcher is alive.
         */
        boolean isAlive();

        /**
         * The type that is produced as a result of executing this advice method.
         *
         * @return A description of the type that is produced by this advice method.
         */
        TypeDefinition getAdviceType();

        /**
         * A dispatcher that is not yet resolved.
         */
        interface Unresolved extends Dispatcher {

            /**
             * Indicates that this dispatcher requires access to the class file declaring the advice method.
             *
             * @return {@code true} if this dispatcher requires access to the advice method's class file.
             */
            boolean isBinary();

            /**
             * Returns the named types declared by this enter advice.
             *
             * @return The named types declared by this enter advice.
             */
            Map<String, TypeDefinition> getNamedTypes();

            /**
             * Resolves this dispatcher as a dispatcher for entering a method.
             *
             * @param userFactories        A list of custom factories for binding parameters of an advice method.
             * @param classReader          A class reader to query for a class file which might be {@code null} if this dispatcher is not binary.
             * @param methodExit           The unresolved dispatcher for the method exit advice.
             * @param postProcessorFactory The post processor factory to use.
             * @return This dispatcher as a dispatcher for entering a method.
             */
            Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                  @MaybeNull AsmClassReader classReader,
                                                  Unresolved methodExit,
                                                  PostProcessor.Factory postProcessorFactory);

            /**
             * Resolves this dispatcher as a dispatcher for exiting a method.
             *
             * @param userFactories        A list of custom factories for binding parameters of an advice method.
             * @param classReader          A class reader to query for a class file which might be {@code null} if this dispatcher is not binary.
             * @param methodEnter          The unresolved dispatcher for the method enter advice.
             * @param postProcessorFactory The post processor factory to use.
             * @return This dispatcher as a dispatcher for exiting a method.
             */
            Resolved.ForMethodExit asMethodExit(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                @MaybeNull AsmClassReader classReader,
                                                Unresolved methodEnter,
                                                PostProcessor.Factory postProcessorFactory);
        }

        /**
         * A suppression handler for optionally suppressing exceptions.
         */
        interface SuppressionHandler {

            /**
             * Binds the suppression handler for instrumenting a specific method.
             *
             * @param exceptionHandler The stack manipulation to apply within a suppression handler.
             * @return A bound version of the suppression handler.
             */
            Bound bind(StackManipulation exceptionHandler);

            /**
             * A bound version of a suppression handler that must not be reused.
             */
            interface Bound {

                /**
                 * Invoked to prepare the suppression handler, i.e. to write an exception handler entry if appropriate.
                 *
                 * @param methodVisitor The method visitor to apply the preparation to.
                 */
                void onPrepare(MethodVisitor methodVisitor);

                /**
                 * Invoked at the start of a method.
                 *
                 * @param methodVisitor The method visitor of the instrumented method.
                 */
                void onStart(MethodVisitor methodVisitor);

                /**
                 * Invoked at the end of a method.
                 *
                 * @param methodVisitor         The method visitor of the instrumented method.
                 * @param implementationContext The implementation context to use.
                 * @param methodSizeHandler     The advice method's method size handler.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param returnType            The return type of the advice method.
                 */
                void onEnd(MethodVisitor methodVisitor,
                           Implementation.Context implementationContext,
                           MethodSizeHandler.ForAdvice methodSizeHandler,
                           StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                           TypeDefinition returnType);

                /**
                 * Invoked at the end of a method if the exception handler should be wrapped in a skipping block.
                 *
                 * @param methodVisitor         The method visitor of the instrumented method.
                 * @param implementationContext The implementation context to use.
                 * @param methodSizeHandler     The advice method's method size handler.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param returnType            The return type of the advice method.
                 */
                void onEndWithSkip(MethodVisitor methodVisitor,
                                   Implementation.Context implementationContext,
                                   MethodSizeHandler.ForAdvice methodSizeHandler,
                                   StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                   TypeDefinition returnType);
            }

            /**
             * A non-operational suppression handler that does not suppress any method.
             */
            enum NoOp implements SuppressionHandler, Bound {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Bound bind(StackManipulation exceptionHandler) {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public void onPrepare(MethodVisitor methodVisitor) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onStart(MethodVisitor methodVisitor) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onEnd(MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  MethodSizeHandler.ForAdvice methodSizeHandler,
                                  StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                  TypeDefinition returnType) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onEndWithSkip(MethodVisitor methodVisitor,
                                          Implementation.Context implementationContext,
                                          MethodSizeHandler.ForAdvice methodSizeHandler,
                                          StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                          TypeDefinition returnType) {
                    /* do nothing */
                }
            }

            /**
             * A suppression handler that suppresses a given throwable type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Suppressing implements SuppressionHandler {

                /**
                 * The suppressed throwable type.
                 */
                private final TypeDescription suppressedType;

                /**
                 * Creates a new suppressing suppression handler.
                 *
                 * @param suppressedType The suppressed throwable type.
                 */
                protected Suppressing(TypeDescription suppressedType) {
                    this.suppressedType = suppressedType;
                }

                /**
                 * Resolves an appropriate suppression handler.
                 *
                 * @param suppressedType The suppressed type or {@link NoExceptionHandler} if no type should be suppressed.
                 * @return An appropriate suppression handler.
                 */
                protected static SuppressionHandler of(TypeDescription suppressedType) {
                    return suppressedType.represents(NoExceptionHandler.class)
                            ? NoOp.INSTANCE
                            : new Suppressing(suppressedType);
                }

                /**
                 * {@inheritDoc}
                 */
                public SuppressionHandler.Bound bind(StackManipulation exceptionHandler) {
                    return new Bound(suppressedType, exceptionHandler);
                }

                /**
                 * An active, bound suppression handler.
                 */
                protected static class Bound implements SuppressionHandler.Bound {

                    /**
                     * The suppressed throwable type.
                     */
                    private final TypeDescription suppressedType;

                    /**
                     * The stack manipulation to apply within a suppression handler.
                     */
                    private final StackManipulation exceptionHandler;

                    /**
                     * A label indicating the start of the method.
                     */
                    private final Label startOfMethod;

                    /**
                     * A label indicating the end of the method.
                     */
                    private final Label endOfMethod;

                    /**
                     * Creates a new active, bound suppression handler.
                     *
                     * @param suppressedType   The suppressed throwable type.
                     * @param exceptionHandler The stack manipulation to apply within a suppression handler.
                     */
                    protected Bound(TypeDescription suppressedType, StackManipulation exceptionHandler) {
                        this.suppressedType = suppressedType;
                        this.exceptionHandler = exceptionHandler;
                        startOfMethod = new Label();
                        endOfMethod = new Label();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onPrepare(MethodVisitor methodVisitor) {
                        methodVisitor.visitTryCatchBlock(startOfMethod, endOfMethod, endOfMethod, suppressedType.getInternalName());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onStart(MethodVisitor methodVisitor) {
                        methodVisitor.visitLabel(startOfMethod);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onEnd(MethodVisitor methodVisitor,
                                      Implementation.Context implementationContext,
                                      MethodSizeHandler.ForAdvice methodSizeHandler,
                                      StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                      TypeDefinition returnType) {
                        methodVisitor.visitLabel(endOfMethod);
                        stackMapFrameHandler.injectExceptionFrame(methodVisitor);
                        methodSizeHandler.requireStackSize(1 + exceptionHandler.apply(methodVisitor, implementationContext).getMaximalSize());
                        if (returnType.represents(boolean.class)
                                || returnType.represents(byte.class)
                                || returnType.represents(short.class)
                                || returnType.represents(char.class)
                                || returnType.represents(int.class)) {
                            methodVisitor.visitInsn(Opcodes.ICONST_0);
                        } else if (returnType.represents(long.class)) {
                            methodVisitor.visitInsn(Opcodes.LCONST_0);
                        } else if (returnType.represents(float.class)) {
                            methodVisitor.visitInsn(Opcodes.FCONST_0);
                        } else if (returnType.represents(double.class)) {
                            methodVisitor.visitInsn(Opcodes.DCONST_0);
                        } else if (!returnType.represents(void.class)) {
                            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onEndWithSkip(MethodVisitor methodVisitor,
                                              Implementation.Context implementationContext,
                                              MethodSizeHandler.ForAdvice methodSizeHandler,
                                              StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                              TypeDefinition returnType) {
                        Label skipExceptionHandler = new Label();
                        methodVisitor.visitJumpInsn(Opcodes.GOTO, skipExceptionHandler);
                        onEnd(methodVisitor, implementationContext, methodSizeHandler, stackMapFrameHandler, returnType);
                        methodVisitor.visitLabel(skipExceptionHandler);
                        stackMapFrameHandler.injectReturnFrame(methodVisitor);
                    }
                }
            }
        }

        /**
         * A relocation handler is responsible for chaining the usual control flow of an instrumented method.
         */
        interface RelocationHandler {

            /**
             * Binds this relocation handler to a relocation dispatcher.
             *
             * @param instrumentedMethod The instrumented method.
             * @param relocation         The relocation to apply.
             * @return A bound relocation handler.
             */
            Bound bind(MethodDescription instrumentedMethod, Relocation relocation);

            /**
             * A relocator is responsible for triggering a relocation if a relocation handler triggers a relocating condition.
             */
            interface Relocation {

                /**
                 * Applies this relocator.
                 *
                 * @param methodVisitor The method visitor to use.
                 */
                void apply(MethodVisitor methodVisitor);

                /**
                 * A relocation that unconditionally jumps to a given label.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForLabel implements Relocation {

                    /**
                     * The label to jump to.
                     */
                    private final Label label;

                    /**
                     * Creates a new relocation for an unconditional jump to a given label.
                     *
                     * @param label The label to jump to.
                     */
                    public ForLabel(Label label) {
                        this.label = label;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void apply(MethodVisitor methodVisitor) {
                        methodVisitor.visitJumpInsn(Opcodes.GOTO, label);
                    }
                }
            }

            /**
             * A bound {@link RelocationHandler}.
             */
            interface Bound {

                /**
                 * Indicates that this relocation handler does not require a minimal stack size.
                 */
                int NO_REQUIRED_SIZE = 0;

                /**
                 * Applies this relocation handler.
                 *
                 * @param methodVisitor         The method visitor to use.
                 * @param implementationContext The implementation context to use.
                 * @param offset                The offset of the relevant value.
                 * @return The minimal required stack size to apply this relocation handler.
                 */
                int apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, int offset);
            }

            /**
             * A disabled relocation handler that does never trigger a relocation.
             */
            enum Disabled implements RelocationHandler, Bound {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Bound bind(MethodDescription instrumentedMethod, Relocation relocation) {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public int apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, int offset) {
                    return NO_REQUIRED_SIZE;
                }
            }

            /**
             * A relocation handler factory that triggers a relocation for a default or non-default value.
             */
            enum ForValue {

                /**
                 * A relocation handler for an {@code int} type or any compatible type.
                 */
                BOOLEAN(Opcodes.ILOAD, Opcodes.BALOAD, Opcodes.IFNE, Opcodes.IFEQ, 0) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }
                },

                /**
                 * A relocation handler for an {@code int} type or any compatible type.
                 */
                BYTE(Opcodes.ILOAD, Opcodes.BALOAD, Opcodes.IFNE, Opcodes.IFEQ, 0) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }
                },

                /**
                 * A relocation handler for an {@code short} type or any compatible type.
                 */
                SHORT(Opcodes.ILOAD, Opcodes.SALOAD, Opcodes.IFNE, Opcodes.IFEQ, 0) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }
                },

                /**
                 * A relocation handler for an {@code char} type or any compatible type.
                 */
                CHARACTER(Opcodes.ILOAD, Opcodes.CALOAD, Opcodes.IFNE, Opcodes.IFEQ, 0) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }
                },

                /**
                 * A relocation handler for an {@code int} type or any compatible type.
                 */
                INTEGER(Opcodes.ILOAD, Opcodes.IALOAD, Opcodes.IFNE, Opcodes.IFEQ, 0) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }
                },

                /**
                 * A relocation handler for a {@code long} type.
                 */
                LONG(Opcodes.LLOAD, Opcodes.LALOAD, Opcodes.IFNE, Opcodes.IFEQ, 0) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        methodVisitor.visitInsn(Opcodes.L2I);
                    }
                },

                /**
                 * A relocation handler for a {@code float} type.
                 */
                FLOAT(Opcodes.FLOAD, Opcodes.FALOAD, Opcodes.IFNE, Opcodes.IFEQ, 2) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        methodVisitor.visitInsn(Opcodes.FCONST_0);
                        methodVisitor.visitInsn(Opcodes.FCMPL);
                    }
                },

                /**
                 * A relocation handler for a {@code double} type.
                 */
                DOUBLE(Opcodes.DLOAD, Opcodes.DALOAD, Opcodes.IFNE, Opcodes.IFEQ, 4) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        methodVisitor.visitInsn(Opcodes.DCONST_0);
                        methodVisitor.visitInsn(Opcodes.DCMPL);
                    }
                },

                /**
                 * A relocation handler for a reference type.
                 */
                REFERENCE(Opcodes.ALOAD, Opcodes.AALOAD, Opcodes.IFNONNULL, Opcodes.IFNULL, 0) {
                    @Override
                    protected void convertValue(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }
                };

                /**
                 * An opcode for loading a value of the represented type from the local variable array.
                 */
                private final int load;

                /**
                 * An opcode for loading a value of the represented type from an array.
                 */
                private final int arrayLoad;

                /**
                 * The opcode to check for a non-default value.
                 */
                private final int defaultJump;

                /**
                 * The opcode to check for a default value.
                 */
                private final int nonDefaultJump;

                /**
                 * The minimal required stack size to apply this relocation handler.
                 */
                private final int requiredSize;

                /**
                 * Creates a new relocation handler for a type's default or non-default value.
                 *
                 * @param load           An opcode for loading a value of the represented type from the local variable array.
                 * @param arrayLoad      An opcode for loading a value of the represented type from an array.
                 * @param defaultJump    The opcode to check for a non-default value.
                 * @param nonDefaultJump The opcode to check for a default value.
                 * @param requiredSize   The minimal required stack size to apply this relocation handler.
                 */
                ForValue(int load, int arrayLoad, int defaultJump, int nonDefaultJump, int requiredSize) {
                    this.load = load;
                    this.arrayLoad = arrayLoad;
                    this.defaultJump = defaultJump;
                    this.nonDefaultJump = nonDefaultJump;
                    this.requiredSize = requiredSize;
                }

                /**
                 * Resolves a relocation handler for a given type.
                 *
                 * @param typeDefinition The type to be resolved for a relocation attempt.
                 * @param index          The index in the array returned by the advice method that contains the value to be checked.
                 * @param inverted       {@code true} if the relocation should be applied for any non-default value of a type.
                 * @return An appropriate relocation handler.
                 */
                protected static RelocationHandler of(TypeDefinition typeDefinition, int index, boolean inverted) {
                    ForValue skipDispatcher;
                    if (typeDefinition.represents(boolean.class)) {
                        skipDispatcher = BOOLEAN;
                    } else if (typeDefinition.represents(byte.class)) {
                        skipDispatcher = BYTE;
                    } else if (typeDefinition.represents(short.class)) {
                        skipDispatcher = SHORT;
                    } else if (typeDefinition.represents(char.class)) {
                        skipDispatcher = CHARACTER;
                    } else if (typeDefinition.represents(int.class)) {
                        skipDispatcher = INTEGER;
                    } else if (typeDefinition.represents(long.class)) {
                        skipDispatcher = LONG;
                    } else if (typeDefinition.represents(float.class)) {
                        skipDispatcher = FLOAT;
                    } else if (typeDefinition.represents(double.class)) {
                        skipDispatcher = DOUBLE;
                    } else if (typeDefinition.represents(void.class)) {
                        throw new IllegalStateException("Cannot skip on default value for void return type");
                    } else {
                        skipDispatcher = REFERENCE;
                    }
                    return inverted
                            ? skipDispatcher.new OfNonDefault(index)
                            : skipDispatcher.new OfDefault(index);
                }

                /**
                 * Applies a value conversion prior to a applying a conditional jump.
                 *
                 * @param methodVisitor The method visitor to use.
                 */
                protected abstract void convertValue(MethodVisitor methodVisitor);

                /**
                 * A relocation handler that checks for a value being a default value.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class OfDefault implements RelocationHandler {

                    /**
                     * The index of the array returned by the advice method that contains the value to check for its value.
                     */
                    private final int index;

                    /**
                     * A relocation handler that checks if a value is a default value.
                     *
                     * @param index The index of the array returned by the advice method that contains the value to check for its value.
                     */
                    public OfDefault(int index) {
                        this.index = index;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Bound bind(MethodDescription instrumentedMethod, Relocation relocation) {
                        return new ForValue.Bound(instrumentedMethod, relocation, index, false);
                    }
                }

                /**
                 * A relocation handler that checks for a value being a non-default value.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class OfNonDefault implements RelocationHandler {

                    /**
                     * The index of the array returned by the advice method that contains the value to check for its value.
                     */
                    private final int index;

                    /**
                     * A relocation handler that checks if a value is a non-default value.
                     *
                     * @param index The index of the array returned by the advice method that contains the value to check for its value.
                     */
                    protected OfNonDefault(int index) {
                        this.index = index;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Bound bind(MethodDescription instrumentedMethod, Relocation relocation) {
                        return new ForValue.Bound(instrumentedMethod, relocation, index, true);
                    }
                }

                /**
                 * A bound relocation handler for {@link ForValue}.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class Bound implements RelocationHandler.Bound {

                    /**
                     * The instrumented method.
                     */
                    private final MethodDescription instrumentedMethod;

                    /**
                     * The relocation to apply.
                     */
                    private final Relocation relocation;

                    /**
                     * The array index of the relocated value.
                     */
                    private final int index;

                    /**
                     * {@code true} if the relocation should be applied for any non-default value of a type.
                     */
                    private final boolean inverted;

                    /**
                     * Creates a new bound relocation handler.
                     *
                     * @param instrumentedMethod The instrumented method.
                     * @param relocation         The relocation to apply.
                     * @param index              The array index of the relocated value.
                     * @param inverted           {@code true} if the relocation should be applied for any non-default value of a type.
                     */
                    protected Bound(MethodDescription instrumentedMethod, Relocation relocation, int index, boolean inverted) {
                        this.instrumentedMethod = instrumentedMethod;
                        this.relocation = relocation;
                        this.index = index;
                        this.inverted = inverted;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, int offset) {
                        if (instrumentedMethod.isConstructor()) {
                            throw new IllegalStateException("Cannot skip code execution from constructor: " + instrumentedMethod);
                        }
                        Label noSkip = new Label();
                        int size;
                        if (index < 0) {
                            size = requiredSize;
                            methodVisitor.visitVarInsn(load, offset);
                        } else {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, offset);
                            methodVisitor.visitJumpInsn(Opcodes.IFNULL, noSkip);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, offset);
                            size = Math.max(requiredSize, IntegerConstant.forValue(index)
                                    .apply(methodVisitor, implementationContext)
                                    .getMaximalSize() + 1);
                            methodVisitor.visitInsn(arrayLoad);
                        }
                        convertValue(methodVisitor);
                        methodVisitor.visitJumpInsn(inverted
                                ? nonDefaultJump
                                : defaultJump, noSkip);
                        relocation.apply(methodVisitor);
                        methodVisitor.visitLabel(noSkip);
                        return size;
                    }
                }
            }

            /**
             * A relocation handler that is triggered if the checked value is an instance of a given type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForType implements RelocationHandler {

                /**
                 * The type that triggers a relocation.
                 */
                private final TypeDescription typeDescription;

                /**
                 * The index of the array returned by the advice method that contains the value to check for its type.
                 */
                private final int index;

                /**
                 * Creates a new relocation handler that triggers a relocation if a value is an instance of a given type.
                 *
                 * @param typeDescription The type that triggers a relocation.
                 * @param index           The index of the array returned by the advice method that contains the value to check for its type.
                 */
                protected ForType(TypeDescription typeDescription, int index) {
                    this.typeDescription = typeDescription;
                    this.index = index;
                }

                /**
                 * Resolves a relocation handler that is triggered if the checked instance is of a given type.
                 *
                 * @param typeDescription The type that triggers a relocation.
                 * @param index           The array index of the value that is returned.
                 * @param returnedType    The type that is returned by the advice method.
                 * @return An appropriate relocation handler.
                 */
                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
                protected static RelocationHandler of(TypeDescription typeDescription, int index, TypeDefinition returnedType) {
                    TypeDefinition targetType;
                    if (index < 0) {
                        targetType = returnedType;
                    } else if (returnedType.isArray()) {
                        targetType = returnedType.getComponentType();
                    } else {
                        throw new IllegalStateException(returnedType + " is not an array type but an index for a relocation is defined");
                    }
                    if (typeDescription.represents(void.class)) {
                        return Disabled.INSTANCE;
                    } else if (typeDescription.represents(OnDefaultValue.class)) {
                        return ForValue.of(targetType, index, false);
                    } else if (typeDescription.represents(OnNonDefaultValue.class)) {
                        return ForValue.of(targetType, index, true);
                    } else if (typeDescription.isPrimitive() || targetType.isPrimitive()) {
                        throw new IllegalStateException("Cannot relocate execution by instance type for primitive type");
                    } else {
                        return new ForType(typeDescription, index);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public RelocationHandler.Bound bind(MethodDescription instrumentedMethod, Relocation relocation) {
                    return new Bound(instrumentedMethod, relocation);
                }

                /**
                 * A bound relocation handler for {@link ForType}.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class Bound implements RelocationHandler.Bound {

                    /**
                     * The instrumented method.
                     */
                    private final MethodDescription instrumentedMethod;

                    /**
                     * The relocation to use.
                     */
                    private final Relocation relocation;

                    /**
                     * Creates a new bound relocation handler.
                     *
                     * @param instrumentedMethod The instrumented method.
                     * @param relocation         The relocation to apply.
                     */
                    protected Bound(MethodDescription instrumentedMethod, Relocation relocation) {
                        this.instrumentedMethod = instrumentedMethod;
                        this.relocation = relocation;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, int offset) {
                        if (instrumentedMethod.isConstructor()) {
                            throw new IllegalStateException("Cannot skip code execution from constructor: " + instrumentedMethod);
                        }
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, offset);
                        Label noSkip = new Label();
                        int size;
                        if (index < 0) {
                            size = NO_REQUIRED_SIZE;
                        } else {
                            methodVisitor.visitJumpInsn(Opcodes.IFNULL, noSkip);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, offset);
                            size = IntegerConstant.forValue(index)
                                    .apply(methodVisitor, implementationContext)
                                    .getMaximalSize() + 1;
                            methodVisitor.visitInsn(Opcodes.AALOAD);
                        }
                        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, typeDescription.getInternalName());
                        methodVisitor.visitJumpInsn(Opcodes.IFEQ, noSkip);
                        relocation.apply(methodVisitor);
                        methodVisitor.visitLabel(noSkip);
                        return size;
                    }
                }
            }
        }

        /**
         * Represents a resolved dispatcher.
         */
        interface Resolved extends Dispatcher {

            /**
             * Returns the named types defined by this advice.
             *
             * @return The named types defined by this advice.
             */
            Map<String, TypeDefinition> getNamedTypes();

            /**
             * Binds this dispatcher for resolution to a specific method.
             *
             * @param instrumentedType      The instrumented type.
             * @param instrumentedMethod    The instrumented method.
             * @param methodVisitor         The method visitor for writing the instrumented method.
             * @param implementationContext The implementation context to use.
             * @param assigner              The assigner to use.
             * @param argumentHandler       A handler for accessing values on the local variable array.
             * @param methodSizeHandler     A handler for computing the method size requirements.
             * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
             * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
             * @param relocation            A relocation to use with a relocation handler.
             * @return A dispatcher that is bound to the instrumented method.
             */
            Bound bind(TypeDescription instrumentedType,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor,
                       Implementation.Context implementationContext,
                       Assigner assigner,
                       ArgumentHandler.ForInstrumentedMethod argumentHandler,
                       MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                       StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                       StackManipulation exceptionHandler,
                       RelocationHandler.Relocation relocation);

            /**
             * Represents a resolved dispatcher for entering a method.
             */
            interface ForMethodEnter extends Resolved {

                /**
                 * Returns {@code true} if the first discovered line number information should be prepended to the advice code.
                 *
                 * @return {@code true} if the first discovered line number information should be prepended to the advice code.
                 */
                boolean isPrependLineNumber();

                /**
                 * Returns the actual advice type, even if it is not required post advice processing.
                 *
                 * @return The actual advice type, even if it is not required post advice processing.
                 */
                TypeDefinition getActualAdviceType();
            }

            /**
             * Represents a resolved dispatcher for exiting a method.
             */
            interface ForMethodExit extends Resolved {

                /**
                 * Returns the type of throwable for which this exit advice is supposed to be invoked.
                 *
                 * @return The {@link Throwable} type for which to invoke this exit advice or a description of {@link NoExceptionHandler}
                 * if this exit advice does not expect to be invoked upon any throwable.
                 */
                TypeDescription getThrowable();

                /**
                 * Returns a factory for creating an {@link ArgumentHandler}.
                 *
                 * @return A factory for creating an {@link ArgumentHandler}.
                 */
                ArgumentHandler.Factory getArgumentHandlerFactory();
            }

            /**
             * An abstract base implementation of a {@link Resolved} dispatcher.
             */
            @HashCodeAndEqualsPlugin.Enhance
            abstract class AbstractBase implements Resolved {

                /**
                 * The post processor to apply.
                 */
                protected final PostProcessor postProcessor;

                /**
                 * A mapping from offset to a mapping for this offset with retained iteration order of the method's parameters.
                 */
                protected final Map<Integer, OffsetMapping> offsetMappings;

                /**
                 * The suppression handler to use.
                 */
                protected final SuppressionHandler suppressionHandler;

                /**
                 * The relocation handler to use.
                 */
                protected final RelocationHandler relocationHandler;

                /**
                 * Creates a new resolved version of a dispatcher.
                 *
                 * @param adviceMethod     The represented advice method.
                 * @param postProcessor    The post processor to use.
                 * @param factories        A list of factories to resolve for the parameters of the advice method.
                 * @param throwableType    The type to handle by a suppression handler or {@link NoExceptionHandler} to not handle any exceptions.
                 * @param relocatableType  The type to trigger a relocation of the method's control flow or {@code void} if no relocation should be executed.
                 * @param relocatableIndex The index within an array that is returned by the advice method, indicating the value to consider for relocation.
                 * @param adviceType       The applied advice type.
                 */
                protected AbstractBase(MethodDescription.InDefinedShape adviceMethod,
                                       PostProcessor postProcessor,
                                       List<? extends OffsetMapping.Factory<?>> factories,
                                       TypeDescription throwableType,
                                       TypeDescription relocatableType,
                                       int relocatableIndex,
                                       OffsetMapping.Factory.AdviceType adviceType) {
                    this.postProcessor = postProcessor;
                    Map<TypeDescription, OffsetMapping.Factory<?>> offsetMappings = new HashMap<TypeDescription, OffsetMapping.Factory<?>>();
                    for (OffsetMapping.Factory<?> factory : factories) {
                        offsetMappings.put(TypeDescription.ForLoadedType.of(factory.getAnnotationType()), factory);
                    }
                    this.offsetMappings = new LinkedHashMap<Integer, OffsetMapping>();
                    for (ParameterDescription.InDefinedShape parameterDescription : adviceMethod.getParameters()) {
                        OffsetMapping offsetMapping = null;
                        for (AnnotationDescription annotationDescription : parameterDescription.getDeclaredAnnotations()) {
                            OffsetMapping.Factory<?> factory = offsetMappings.get(annotationDescription.getAnnotationType());
                            if (factory != null) {
                                @SuppressWarnings("unchecked")
                                OffsetMapping current = factory.make(parameterDescription,
                                        (AnnotationDescription.Loadable) annotationDescription.prepare(factory.getAnnotationType()),
                                        adviceType);
                                if (offsetMapping == null) {
                                    offsetMapping = current;
                                } else {
                                    throw new IllegalStateException(parameterDescription + " is bound to both " + current + " and " + offsetMapping);
                                }
                            }
                        }
                        this.offsetMappings.put(parameterDescription.getOffset(), offsetMapping == null
                                ? new OffsetMapping.ForArgument.Unresolved(parameterDescription)
                                : offsetMapping);
                    }
                    suppressionHandler = SuppressionHandler.Suppressing.of(throwableType);
                    relocationHandler = RelocationHandler.ForType.of(relocatableType, relocatableIndex, adviceMethod.getReturnType());
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAlive() {
                    return true;
                }
            }
        }

        /**
         * A bound resolution of an advice method.
         */
        interface Bound {

            /**
             * Prepares the advice method's exception handlers.
             */
            void prepare();

            /**
             * Initialized the advice's methods local variables.
             */
            void initialize();

            /**
             * Applies this dispatcher.
             */
            void apply();
        }

        /**
         * An implementation for inactive devise that does not write any byte code.
         */
        enum Inactive implements Dispatcher.Unresolved, Resolved.ForMethodEnter, Resolved.ForMethodExit, Bound {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public boolean isAlive() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isBinary() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription getAdviceType() {
                return TypeDescription.ForLoadedType.of(void.class);
            }

            /**
             * {@inheritDoc}
             */
            public boolean isPrependLineNumber() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public TypeDefinition getActualAdviceType() {
                return TypeDescription.ForLoadedType.of(void.class);
            }

            /**
             * {@inheritDoc}
             */
            public Map<String, TypeDefinition> getNamedTypes() {
                return Collections.emptyMap();
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription getThrowable() {
                return NoExceptionHandler.DESCRIPTION;
            }

            /**
             * {@inheritDoc}
             */
            public ArgumentHandler.Factory getArgumentHandlerFactory() {
                return ArgumentHandler.Factory.SIMPLE;
            }

            /**
             * {@inheritDoc}
             */
            public Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                         @MaybeNull AsmClassReader classReader,
                                                         Unresolved methodExit,
                                                         PostProcessor.Factory postProcessorFactory) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public Resolved.ForMethodExit asMethodExit(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                       @MaybeNull AsmClassReader classReader,
                                                       Unresolved methodEnter,
                                                       PostProcessor.Factory postProcessorFactory) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public void prepare() {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void initialize() {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public void apply() {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public Bound bind(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              MethodVisitor methodVisitor,
                              Implementation.Context implementationContext,
                              Assigner assigner,
                              ArgumentHandler.ForInstrumentedMethod argumentHandler,
                              MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                              StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                              StackManipulation exceptionHandler,
                              RelocationHandler.Relocation relocation) {
                return this;
            }
        }

        /**
         * A dispatcher for an advice method that is being inlined into the instrumented method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Inlining implements Unresolved {

            /**
             * The advice method.
             */
            protected final MethodDescription.InDefinedShape adviceMethod;

            /**
             * A mapping of all available local variables by their name to their type.
             */
            private final Map<String, TypeDefinition> namedTypes;

            /**
             * Creates a dispatcher for inlined advice method.
             *
             * @param adviceMethod The advice method.
             */
            protected Inlining(MethodDescription.InDefinedShape adviceMethod) {
                this.adviceMethod = adviceMethod;
                namedTypes = new HashMap<String, TypeDefinition>();
                for (ParameterDescription parameterDescription : adviceMethod.getParameters()) {
                    AnnotationDescription.Loadable<Local> annotationDescription = parameterDescription.getDeclaredAnnotations().ofType(Local.class);
                    if (annotationDescription != null) {
                        String name = annotationDescription.getValue(OffsetMapping.ForLocalValue.Factory.LOCAL_VALUE).resolve(String.class);
                        TypeDefinition previous = namedTypes.put(name, parameterDescription.getType());
                        if (previous != null && !previous.equals(parameterDescription.getType())) {
                            throw new IllegalStateException("Local variable for " + name + " is defined with inconsistent types");
                        }
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public boolean isAlive() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isBinary() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription getAdviceType() {
                return adviceMethod.getReturnType().asErasure();
            }

            /**
             * {@inheritDoc}
             */
            public Map<String, TypeDefinition> getNamedTypes() {
                return namedTypes;
            }

            /**
             * {@inheritDoc}
             */
            public Dispatcher.Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                    @MaybeNull AsmClassReader classReader,
                                                                    Unresolved methodExit,
                                                                    PostProcessor.Factory postProcessorFactory) {
                if (classReader == null) {
                    throw new IllegalStateException("Class reader not expected null");
                }
                return Resolved.ForMethodEnter.of(adviceMethod,
                        postProcessorFactory.make(adviceMethod.getDeclaredAnnotations(), adviceMethod.getReturnType().asErasure(), false),
                        namedTypes,
                        userFactories,
                        methodExit.getAdviceType(),
                        classReader,
                        methodExit.isAlive());
            }

            /**
             * {@inheritDoc}
             */
            public Dispatcher.Resolved.ForMethodExit asMethodExit(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                  @MaybeNull AsmClassReader classReader,
                                                                  Unresolved methodEnter,
                                                                  PostProcessor.Factory postProcessorFactory) {
                Map<String, TypeDefinition> namedTypes = new HashMap<String, TypeDefinition>(methodEnter.getNamedTypes()), uninitializedNamedTypes = new HashMap<String, TypeDefinition>();
                for (Map.Entry<String, TypeDefinition> entry : this.namedTypes.entrySet()) {
                    TypeDefinition typeDefinition = namedTypes.get(entry.getKey()), uninitializedTypeDefinition = uninitializedNamedTypes.get(entry.getKey());
                    if (typeDefinition == null && uninitializedTypeDefinition == null) {
                        namedTypes.put(entry.getKey(), entry.getValue());
                        uninitializedNamedTypes.put(entry.getKey(), entry.getValue());
                    } else if (!(typeDefinition == null ? uninitializedTypeDefinition : typeDefinition).equals(entry.getValue())) {
                        throw new IllegalStateException("Local variable for " + entry.getKey() + " is defined with inconsistent types");
                    }
                }
                return Resolved.ForMethodExit.of(adviceMethod, postProcessorFactory.make(adviceMethod.getDeclaredAnnotations(),
                        adviceMethod.getReturnType().asErasure(),
                        true), namedTypes, uninitializedNamedTypes, userFactories, classReader, methodEnter.getAdviceType());
            }

            @Override
            public String toString() {
                return "Delegate to " + adviceMethod;
            }

            /**
             * A resolved version of a dispatcher.
             */
            protected abstract static class Resolved extends Dispatcher.Resolved.AbstractBase {

                /**
                 * The represented advice method.
                 */
                protected final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * A class reader to query for the class file of the advice method.
                 */
                protected final AsmClassReader classReader;

                /**
                 * Creates a new resolved version of a dispatcher.
                 *
                 * @param adviceMethod     The represented advice method.
                 * @param postProcessor    The post processor to apply.
                 * @param factories        A list of factories to resolve for the parameters of the advice method.
                 * @param throwableType    The type to handle by a suppression handler or {@link NoExceptionHandler} to not handle any exceptions.
                 * @param relocatableType  The type to trigger a relocation of the method's control flow or {@code void} if no relocation should be executed.
                 * @param relocatableIndex The index within an array that is returned by the advice method, indicating the value to consider for relocation.
                 * @param classReader      A class reader to query for the class file of the advice method.
                 */
                protected Resolved(MethodDescription.InDefinedShape adviceMethod,
                                   PostProcessor postProcessor,
                                   List<? extends OffsetMapping.Factory<?>> factories,
                                   TypeDescription throwableType,
                                   TypeDescription relocatableType,
                                   int relocatableIndex,
                                   AsmClassReader classReader) {
                    super(adviceMethod, postProcessor, factories, throwableType, relocatableType, relocatableIndex, OffsetMapping.Factory.AdviceType.INLINING);
                    this.adviceMethod = adviceMethod;
                    this.classReader = classReader;
                }

                /**
                 * Resolves the initialization types of this advice method.
                 *
                 * @param argumentHandler The argument handler to use for resolving the initialization.
                 * @return A mapping of parameter offsets to the type to initialize.
                 */
                protected abstract Map<Integer, TypeDefinition> resolveInitializationTypes(ArgumentHandler argumentHandler);

                /**
                 * Applies a resolution for a given instrumented method.
                 *
                 * @param methodVisitor         A method visitor for writing byte code to the instrumented method.
                 * @param implementationContext The implementation context to use.
                 * @param assigner              The assigner to use.
                 * @param argumentHandler       A handler for accessing values on the local variable array.
                 * @param methodSizeHandler     A handler for computing the method size requirements.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param instrumentedType      A description of the instrumented type.
                 * @param instrumentedMethod    A description of the instrumented method.
                 * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                 * @param relocationHandler     A bound relocation handler that is responsible for considering a non-standard control flow.
                 * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                 * @return A method visitor for visiting the advice method's byte code.
                 */
                protected abstract MethodVisitor apply(MethodVisitor methodVisitor,
                                                       Implementation.Context implementationContext,
                                                       Assigner assigner,
                                                       ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                                       MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                       StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                       TypeDescription instrumentedType,
                                                       MethodDescription instrumentedMethod,
                                                       SuppressionHandler.Bound suppressionHandler,
                                                       RelocationHandler.Bound relocationHandler,
                                                       StackManipulation exceptionHandler);

                /**
                 * A bound advice method that copies the code by first extracting the exception table and later appending the
                 * code of the method without copying any meta data.
                 */
                protected class AdviceMethodInliner extends ClassVisitor implements Bound {

                    /**
                     * A description of the instrumented type.
                     */
                    protected final TypeDescription instrumentedType;

                    /**
                     * The instrumented method.
                     */
                    protected final MethodDescription instrumentedMethod;

                    /**
                     * The method visitor for writing the instrumented method.
                     */
                    protected final MethodVisitor methodVisitor;

                    /**
                     * The implementation context to use.
                     */
                    protected final Implementation.Context implementationContext;

                    /**
                     * The assigner to use.
                     */
                    protected final Assigner assigner;

                    /**
                     * A handler for accessing values on the local variable array.
                     */
                    protected final ArgumentHandler.ForInstrumentedMethod argumentHandler;

                    /**
                     * A handler for computing the method size requirements.
                     */
                    protected final MethodSizeHandler.ForInstrumentedMethod methodSizeHandler;

                    /**
                     * A handler for translating and injecting stack map frames.
                     */
                    protected final StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler;

                    /**
                     * A bound suppression handler that is used for suppressing exceptions of this advice method.
                     */
                    protected final SuppressionHandler.Bound suppressionHandler;

                    /**
                     * A bound relocation handler that is responsible for considering a non-standard control flow.
                     */
                    protected final RelocationHandler.Bound relocationHandler;

                    /**
                     * The exception handler that is resolved for the instrumented method.
                     */
                    protected final StackManipulation exceptionHandler;

                    /**
                     * A class reader for parsing the class file containing the represented advice method.
                     */
                    protected final AsmClassReader classReader;

                    /**
                     * The labels that were found during parsing the method's exception handler in the order of their discovery.
                     */
                    protected final List<Label> labels;

                    /**
                     * Creates a new advice method inliner.
                     *
                     * @param instrumentedType      A description of the instrumented type.
                     * @param instrumentedMethod    The instrumented method.
                     * @param methodVisitor         The method visitor for writing the instrumented method.
                     * @param implementationContext The implementation context to use.
                     * @param assigner              The assigner to use.
                     * @param argumentHandler       A handler for accessing values on the local variable array.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                     * @param relocationHandler     A bound relocation handler that is responsible for considering a non-standard control flow.
                     * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                     * @param classReader           A class reader for parsing the class file containing the represented advice method.
                     */
                    protected AdviceMethodInliner(TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  MethodVisitor methodVisitor,
                                                  Implementation.Context implementationContext,
                                                  Assigner assigner,
                                                  ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                                  MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                  StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                  SuppressionHandler.Bound suppressionHandler,
                                                  RelocationHandler.Bound relocationHandler,
                                                  StackManipulation exceptionHandler,
                                                  AsmClassReader classReader) {
                        super(OpenedClassReader.ASM_API);
                        this.instrumentedType = instrumentedType;
                        this.instrumentedMethod = instrumentedMethod;
                        this.methodVisitor = methodVisitor;
                        this.implementationContext = implementationContext;
                        this.assigner = assigner;
                        this.argumentHandler = argumentHandler;
                        this.methodSizeHandler = methodSizeHandler;
                        this.stackMapFrameHandler = stackMapFrameHandler;
                        this.suppressionHandler = suppressionHandler;
                        this.relocationHandler = relocationHandler;
                        this.exceptionHandler = exceptionHandler;
                        this.classReader = classReader;
                        labels = new ArrayList<Label>();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void prepare() {
                        classReader.accept(new ExceptionTableExtractor(), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                        suppressionHandler.onPrepare(methodVisitor);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void initialize() {
                        for (Map.Entry<Integer, TypeDefinition> typeDefinition : resolveInitializationTypes(argumentHandler).entrySet()) {
                            if (typeDefinition.getValue().represents(boolean.class)
                                    || typeDefinition.getValue().represents(byte.class)
                                    || typeDefinition.getValue().represents(short.class)
                                    || typeDefinition.getValue().represents(char.class)
                                    || typeDefinition.getValue().represents(int.class)) {
                                methodVisitor.visitInsn(Opcodes.ICONST_0);
                                methodVisitor.visitVarInsn(Opcodes.ISTORE, typeDefinition.getKey());
                            } else if (typeDefinition.getValue().represents(long.class)) {
                                methodVisitor.visitInsn(Opcodes.LCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.LSTORE, typeDefinition.getKey());
                            } else if (typeDefinition.getValue().represents(float.class)) {
                                methodVisitor.visitInsn(Opcodes.FCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.FSTORE, typeDefinition.getKey());
                            } else if (typeDefinition.getValue().represents(double.class)) {
                                methodVisitor.visitInsn(Opcodes.DCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.DSTORE, typeDefinition.getKey());
                            } else {
                                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                                methodVisitor.visitVarInsn(Opcodes.ASTORE, typeDefinition.getKey());
                            }
                            methodSizeHandler.requireStackSize(typeDefinition.getValue().getStackSize().getSize());
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void apply() {
                        classReader.accept(this, ClassReader.SKIP_DEBUG | stackMapFrameHandler.getReaderHint());
                    }

                    @Override
                    @MaybeNull
                    public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, @MaybeNull String signature, @MaybeNull String[] exception) {
                        return adviceMethod.getInternalName().equals(internalName) && adviceMethod.getDescriptor().equals(descriptor)
                                ? new ExceptionTableSubstitutor(Inlining.Resolved.this.apply(methodVisitor,
                                implementationContext,
                                assigner,
                                argumentHandler,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                instrumentedType,
                                instrumentedMethod,
                                suppressionHandler,
                                relocationHandler,
                                exceptionHandler)) : IGNORE_METHOD;
                    }

                    /**
                     * A class visitor that extracts the exception tables of the advice method.
                     */
                    protected class ExceptionTableExtractor extends ClassVisitor {

                        /**
                         * Creates a new exception table extractor.
                         */
                        protected ExceptionTableExtractor() {
                            super(OpenedClassReader.ASM_API);
                        }

                        @Override
                        @MaybeNull
                        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, @MaybeNull String signature, @MaybeNull String[] exception) {
                            return adviceMethod.getInternalName().equals(internalName) && adviceMethod.getDescriptor().equals(descriptor)
                                    ? new ExceptionTableCollector(methodVisitor)
                                    : IGNORE_METHOD;
                        }
                    }

                    /**
                     * A visitor that only writes try-catch-finally blocks to the supplied method visitor. All labels of these tables are collected
                     * for substitution when revisiting the reminder of the method.
                     */
                    protected class ExceptionTableCollector extends MethodVisitor {

                        /**
                         * The method visitor for which the try-catch-finally blocks should be written.
                         */
                        private final MethodVisitor methodVisitor;

                        /**
                         * Creates a new exception table collector.
                         *
                         * @param methodVisitor The method visitor for which the try-catch-finally blocks should be written.
                         */
                        protected ExceptionTableCollector(MethodVisitor methodVisitor) {
                            super(OpenedClassReader.ASM_API);
                            this.methodVisitor = methodVisitor;
                        }

                        @Override
                        public void visitTryCatchBlock(Label start, Label end, Label handler, @MaybeNull String type) {
                            methodVisitor.visitTryCatchBlock(start, end, handler, type);
                            labels.addAll(Arrays.asList(start, end, handler));
                        }

                        @Override
                        @MaybeNull
                        public AnnotationVisitor visitTryCatchAnnotation(int typeReference, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
                            return methodVisitor.visitTryCatchAnnotation(typeReference, typePath, descriptor, visible);
                        }
                    }

                    /**
                     * A label substitutor allows to visit an advice method a second time after the exception handlers were already written.
                     * Doing so, this visitor substitutes all labels that were already created during the first visit to keep the mapping
                     * consistent. It is not required to resolve labels for non-code instructions as meta information is not propagated to
                     * the target method visitor for advice code.
                     */
                    protected class ExceptionTableSubstitutor extends MethodVisitor {

                        /**
                         * A map containing resolved substitutions.
                         */
                        private final Map<Label, Label> substitutions;

                        /**
                         * The current index of the visited labels that are used for try-catch-finally blocks.
                         */
                        private int index;

                        /**
                         * Creates a label substitutor.
                         *
                         * @param methodVisitor The method visitor for which to substitute labels.
                         */
                        protected ExceptionTableSubstitutor(MethodVisitor methodVisitor) {
                            super(OpenedClassReader.ASM_API, methodVisitor);
                            substitutions = new IdentityHashMap<Label, Label>();
                        }

                        @Override
                        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                            substitutions.put(start, labels.get(index++));
                            substitutions.put(end, labels.get(index++));
                            Label actualHandler = labels.get(index++);
                            substitutions.put(handler, actualHandler);
                            ((CodeTranslationVisitor) mv).propagateHandler(actualHandler);
                        }

                        @Override
                        @MaybeNull
                        public AnnotationVisitor visitTryCatchAnnotation(int typeReference, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
                            return IGNORE_ANNOTATION;
                        }

                        @Override
                        public void visitLabel(Label label) {
                            super.visitLabel(resolve(label));
                        }

                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            super.visitJumpInsn(opcode, resolve(label));
                        }

                        @Override
                        public void visitTableSwitchInsn(int minimum, int maximum, Label defaultOption, Label... label) {
                            super.visitTableSwitchInsn(minimum, maximum, defaultOption, resolve(label));
                        }

                        @Override
                        public void visitLookupSwitchInsn(Label defaultOption, int[] keys, Label[] label) {
                            super.visitLookupSwitchInsn(resolve(defaultOption), keys, resolve(label));
                        }

                        /**
                         * Resolves an array of labels.
                         *
                         * @param label The labels to resolved.
                         * @return An array containing the resolved arrays.
                         */
                        private Label[] resolve(Label[] label) {
                            Label[] resolved = new Label[label.length];
                            int index = 0;
                            for (Label aLabel : label) {
                                resolved[index++] = resolve(aLabel);
                            }
                            return resolved;
                        }

                        /**
                         * Resolves a single label if mapped or returns the original label.
                         *
                         * @param label The label to resolve.
                         * @return The resolved label.
                         */
                        private Label resolve(Label label) {
                            Label substitution = substitutions.get(label);
                            return substitution == null
                                    ? label
                                    : substitution;
                        }
                    }
                }

                /**
                 * A resolved dispatcher for implementing method enter advice.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected abstract static class ForMethodEnter extends Inlining.Resolved implements Dispatcher.Resolved.ForMethodEnter {

                    /**
                     * A mapping of all available local variables by their name to their type.
                     */
                    private final Map<String, TypeDefinition> namedTypes;

                    /**
                     * {@code true} if the first discovered line number information should be prepended to the advice code.
                     */
                    private final boolean prependLineNumber;

                    /**
                     * Creates a new resolved dispatcher for implementing method enter advice.
                     *
                     * @param adviceMethod  The represented advice method.
                     * @param postProcessor The post processor to apply.
                     * @param namedTypes    A mapping of all available local variables by their name to their type.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param exitType      The exit type or {@code void} if no exit type is defined.
                     * @param classReader   A class reader to query for the class file of the advice method.
                     */
                    @SuppressWarnings("unchecked") // In absence of @SafeVarargs
                    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming annotation for exit advice.")
                    protected ForMethodEnter(MethodDescription.InDefinedShape adviceMethod,
                                             PostProcessor postProcessor,
                                             Map<String, TypeDefinition> namedTypes,
                                             List<? extends OffsetMapping.Factory<?>> userFactories,
                                             TypeDefinition exitType,
                                             AsmClassReader classReader) {
                        super(adviceMethod,
                                postProcessor,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForArgument.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForFieldHandle.Unresolved.ReaderFactory.INSTANCE,
                                        OffsetMapping.ForFieldHandle.Unresolved.WriterFactory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForSelfCallHandle.Factory.INSTANCE,
                                        OffsetMapping.ForUnusedValue.Factory.INSTANCE,
                                        OffsetMapping.ForStubValue.INSTANCE,
                                        OffsetMapping.ForThrowable.Factory.INSTANCE,
                                        OffsetMapping.ForExitValue.Factory.of(exitType),
                                        new OffsetMapping.ForLocalValue.Factory(namedTypes),
                                        new OffsetMapping.Factory.Illegal<Thrown>(Thrown.class),
                                        new OffsetMapping.Factory.Illegal<Enter>(Enter.class),
                                        new OffsetMapping.Factory.Illegal<Return>(Return.class)), userFactories),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SUPPRESS_ENTER).resolve(TypeDescription.class),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SKIP_ON).resolve(TypeDescription.class),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SKIP_ON_INDEX).resolve(Integer.class),
                                classReader);
                        this.namedTypes = namedTypes;
                        prependLineNumber = adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(PREPEND_LINE_NUMBER).resolve(Boolean.class);
                    }

                    /**
                     * Resolves enter advice that only exposes the enter type if this is necessary.
                     *
                     * @param adviceMethod  The advice method.
                     * @param postProcessor The post processor to apply.
                     * @param namedTypes    A mapping of all available local variables by their name to their type.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param exitType      The exit type or {@code void} if no exit type is defined.
                     * @param classReader   The class reader for parsing the advice method's class file.
                     * @param methodExit    {@code true} if exit advice is applied.
                     * @return An appropriate enter handler.
                     */
                    protected static Resolved.ForMethodEnter of(MethodDescription.InDefinedShape adviceMethod,
                                                                PostProcessor postProcessor,
                                                                Map<String, TypeDefinition> namedTypes,
                                                                List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                TypeDefinition exitType,
                                                                AsmClassReader classReader,
                                                                boolean methodExit) {
                        return methodExit
                                ? new WithRetainedEnterType(adviceMethod, postProcessor, namedTypes, userFactories, exitType, classReader)
                                : new WithDiscardedEnterType(adviceMethod, postProcessor, namedTypes, userFactories, exitType, classReader);
                    }

                    @Override
                    protected Map<Integer, TypeDefinition> resolveInitializationTypes(ArgumentHandler argumentHandler) {
                        SortedMap<Integer, TypeDefinition> resolved = new TreeMap<Integer, TypeDefinition>();
                        for (Map.Entry<String, TypeDefinition> entry : namedTypes.entrySet()) {
                            resolved.put(argumentHandler.named(entry.getKey()), entry.getValue());
                        }
                        return resolved;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Bound bind(TypeDescription instrumentedType,
                                      MethodDescription instrumentedMethod,
                                      MethodVisitor methodVisitor,
                                      Implementation.Context implementationContext,
                                      Assigner assigner,
                                      ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                      MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                      StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                      StackManipulation exceptionHandler,
                                      RelocationHandler.Relocation relocation) {
                        return new AdviceMethodInliner(instrumentedType,
                                instrumentedMethod,
                                methodVisitor,
                                implementationContext,
                                assigner,
                                argumentHandler,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                suppressionHandler.bind(exceptionHandler),
                                relocationHandler.bind(instrumentedMethod, relocation),
                                exceptionHandler,
                                classReader);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean isPrependLineNumber() {
                        return prependLineNumber;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDefinition getActualAdviceType() {
                        return adviceMethod.getReturnType();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Map<String, TypeDefinition> getNamedTypes() {
                        return namedTypes;
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor,
                                                  Context implementationContext,
                                                  Assigner assigner,
                                                  ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                                  MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                  StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                  TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  SuppressionHandler.Bound suppressionHandler,
                                                  RelocationHandler.Bound relocationHandler,
                                                  StackManipulation exceptionHandler) {
                        MethodDescription.TypeToken typeToken = adviceMethod.asTypeToken();
                        return doApply(methodVisitor,
                                implementationContext,
                                assigner,
                                argumentHandler.bindEnter(typeToken),
                                methodSizeHandler.bindEnter(typeToken),
                                stackMapFrameHandler.bindEnter(typeToken),
                                instrumentedType,
                                instrumentedMethod,
                                suppressionHandler,
                                relocationHandler,
                                exceptionHandler);
                    }

                    /**
                     * Applies a resolution for a given instrumented method.
                     *
                     * @param instrumentedType      A description of the instrumented type.
                     * @param instrumentedMethod    The instrumented method that is being bound.
                     * @param methodVisitor         The method visitor for writing to the instrumented method.
                     * @param implementationContext The implementation context to use.
                     * @param assigner              The assigner to use.
                     * @param argumentHandler       A handler for accessing values on the local variable array.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param suppressionHandler    The bound suppression handler to use.
                     * @param relocationHandler     The bound relocation handler to use.
                     * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                     * @return A method visitor for visiting the advice method's byte code.
                     */
                    protected MethodVisitor doApply(MethodVisitor methodVisitor,
                                                    Implementation.Context implementationContext,
                                                    Assigner assigner,
                                                    ArgumentHandler.ForAdvice argumentHandler,
                                                    MethodSizeHandler.ForAdvice methodSizeHandler,
                                                    StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                    TypeDescription instrumentedType,
                                                    MethodDescription instrumentedMethod,
                                                    SuppressionHandler.Bound suppressionHandler,
                                                    RelocationHandler.Bound relocationHandler,
                                                    StackManipulation exceptionHandler) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    argumentHandler,
                                    OffsetMapping.Sort.ENTER));
                        }
                        return new CodeTranslationVisitor(methodVisitor,
                                implementationContext,
                                argumentHandler,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                instrumentedType,
                                instrumentedMethod,
                                assigner,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler,
                                relocationHandler,
                                exceptionHandler,
                                postProcessor,
                                false);
                    }

                    /**
                     * Implementation of an advice that does expose an enter type.
                     */
                    protected static class WithRetainedEnterType extends Inlining.Resolved.ForMethodEnter {


                        /**
                         * Creates a new resolved dispatcher for implementing method enter advice that does expose the enter type.
                         *
                         * @param adviceMethod  The represented advice method.
                         * @param postProcessor The post processor to apply.
                         * @param namedTypes    A mapping of all available local variables by their name to their type.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param exitType      The exit type or {@code void} if no exit type is defined.
                         * @param classReader   A class reader to query for the class file of the advice method.
                         */
                        protected WithRetainedEnterType(MethodDescription.InDefinedShape adviceMethod,
                                                        PostProcessor postProcessor,
                                                        Map<String, TypeDefinition> namedTypes,
                                                        List<? extends OffsetMapping.Factory<?>> userFactories,
                                                        TypeDefinition exitType,
                                                        AsmClassReader classReader) {
                            super(adviceMethod, postProcessor, namedTypes, userFactories, exitType, classReader);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDefinition getAdviceType() {
                            return adviceMethod.getReturnType();
                        }
                    }

                    /**
                     * Implementation of an advice that does not expose an enter type.
                     */
                    protected static class WithDiscardedEnterType extends Inlining.Resolved.ForMethodEnter {

                        /**
                         * Creates a new resolved dispatcher for implementing method enter advice that does not expose the enter type.
                         *
                         * @param adviceMethod  The represented advice method.
                         * @param postProcessor The post processor to apply.
                         * @param namedTypes    A mapping of all available local variables by their name to their type.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param exitType      The exit type or {@code void} if no exit type is defined.
                         * @param classReader   A class reader to query for the class file of the advice method.
                         */
                        protected WithDiscardedEnterType(MethodDescription.InDefinedShape adviceMethod,
                                                         PostProcessor postProcessor,
                                                         Map<String, TypeDefinition> namedTypes,
                                                         List<? extends OffsetMapping.Factory<?>> userFactories,
                                                         TypeDefinition exitType,
                                                         AsmClassReader classReader) {
                            super(adviceMethod, postProcessor, namedTypes, userFactories, exitType, classReader);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDefinition getAdviceType() {
                            return TypeDescription.ForLoadedType.of(void.class);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        protected MethodVisitor doApply(MethodVisitor methodVisitor,
                                                        Context implementationContext,
                                                        Assigner assigner,
                                                        ArgumentHandler.ForAdvice argumentHandler,
                                                        MethodSizeHandler.ForAdvice methodSizeHandler,
                                                        StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                        TypeDescription instrumentedType,
                                                        MethodDescription instrumentedMethod,
                                                        SuppressionHandler.Bound suppressionHandler,
                                                        RelocationHandler.Bound relocationHandler,
                                                        StackManipulation exceptionHandler) {
                            methodSizeHandler.requireLocalVariableLengthPadding(adviceMethod.getReturnType().getStackSize().getSize());
                            return super.doApply(methodVisitor,
                                    implementationContext,
                                    assigner,
                                    argumentHandler,
                                    methodSizeHandler,
                                    stackMapFrameHandler,
                                    instrumentedType,
                                    instrumentedMethod,
                                    suppressionHandler,
                                    relocationHandler,
                                    exceptionHandler);
                        }
                    }
                }

                /**
                 * A resolved dispatcher for implementing method exit advice.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected abstract static class ForMethodExit extends Inlining.Resolved implements Dispatcher.Resolved.ForMethodExit {

                    /**
                     * A mapping of uninitialized local variables by their name.
                     */
                    private final Map<String, TypeDefinition> uninitializedNamedTypes;

                    /**
                     * {@code true} if the arguments of the instrumented method should be copied before executing the instrumented method.
                     */
                    private final boolean backupArguments;

                    /**
                     * Creates a new resolved dispatcher for implementing method exit advice.
                     *
                     * @param adviceMethod            The represented advice method.
                     * @param postProcessor           The post processor to apply.
                     * @param namedTypes              A mapping of all available local variables by their name to their type.
                     * @param uninitializedNamedTypes A mapping of all uninitialized local variables by their name to their type.
                     * @param userFactories           A list of user-defined factories for offset mappings.
                     * @param classReader             The class reader for parsing the advice method's class file.
                     * @param enterType               The type of the value supplied by the enter advice method or {@code void} if no such value exists.
                     */
                    @SuppressWarnings("unchecked")
                    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming annotation for exit advice.")
                    protected ForMethodExit(MethodDescription.InDefinedShape adviceMethod,
                                            PostProcessor postProcessor,
                                            Map<String, TypeDefinition> namedTypes,
                                            Map<String, TypeDefinition> uninitializedNamedTypes,
                                            List<? extends OffsetMapping.Factory<?>> userFactories,
                                            AsmClassReader classReader,
                                            TypeDefinition enterType) {
                        super(adviceMethod,
                                postProcessor,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForArgument.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForFieldHandle.Unresolved.ReaderFactory.INSTANCE,
                                        OffsetMapping.ForFieldHandle.Unresolved.WriterFactory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForSelfCallHandle.Factory.INSTANCE,
                                        OffsetMapping.ForUnusedValue.Factory.INSTANCE,
                                        OffsetMapping.ForStubValue.INSTANCE,
                                        OffsetMapping.ForEnterValue.Factory.of(enterType),
                                        OffsetMapping.ForExitValue.Factory.of(adviceMethod.getReturnType()),
                                        new OffsetMapping.ForLocalValue.Factory(namedTypes),
                                        OffsetMapping.ForReturnValue.Factory.INSTANCE,
                                        OffsetMapping.ForThrowable.Factory.of(adviceMethod)
                                ), userFactories),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(SUPPRESS_EXIT).resolve(TypeDescription.class),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(REPEAT_ON).resolve(TypeDescription.class),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(REPEAT_ON_INDEX).resolve(Integer.class),
                                classReader);
                        this.uninitializedNamedTypes = uninitializedNamedTypes;
                        backupArguments = adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(BACKUP_ARGUMENTS).resolve(Boolean.class);
                    }

                    /**
                     * Resolves exit advice that handles exceptions depending on the specification of the exit advice.
                     *
                     * @param adviceMethod            The advice method.
                     * @param postProcessor           The post processor to apply.
                     * @param namedTypes              A mapping of all available local variables by their name to their type.
                     * @param uninitializedNamedTypes A mapping of all uninitialized local variables by their name to their type.
                     * @param userFactories           A list of user-defined factories for offset mappings.
                     * @param classReader             The class reader for parsing the advice method's class file.
                     * @param enterType               The type of the value supplied by the enter advice method or {@code void} if no such value exists.
                     * @return An appropriate exit handler.
                     */
                    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming annotation for exit advice.")
                    protected static Resolved.ForMethodExit of(MethodDescription.InDefinedShape adviceMethod,
                                                               PostProcessor postProcessor,
                                                               Map<String, TypeDefinition> namedTypes,
                                                               Map<String, TypeDefinition> uninitializedNamedTypes,
                                                               List<? extends OffsetMapping.Factory<?>> userFactories,
                                                               AsmClassReader classReader,
                                                               TypeDefinition enterType) {
                        TypeDescription throwable = adviceMethod.getDeclaredAnnotations()
                                .ofType(OnMethodExit.class)
                                .getValue(ON_THROWABLE).resolve(TypeDescription.class);
                        return throwable.represents(NoExceptionHandler.class)
                                ? new WithoutExceptionHandler(adviceMethod, postProcessor, namedTypes, uninitializedNamedTypes, userFactories, classReader, enterType)
                                : new WithExceptionHandler(adviceMethod, postProcessor, namedTypes, uninitializedNamedTypes, userFactories, classReader, enterType, throwable);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Map<String, TypeDefinition> getNamedTypes() {
                        return uninitializedNamedTypes;
                    }

                    @Override
                    protected Map<Integer, TypeDefinition> resolveInitializationTypes(ArgumentHandler argumentHandler) {
                        SortedMap<Integer, TypeDefinition> resolved = new TreeMap<Integer, TypeDefinition>();
                        for (Map.Entry<String, TypeDefinition> entry : uninitializedNamedTypes.entrySet()) {
                            resolved.put(argumentHandler.named(entry.getKey()), entry.getValue());
                        }
                        if (!adviceMethod.getReturnType().represents(void.class)) {
                            resolved.put(argumentHandler.exit(), adviceMethod.getReturnType());
                        }
                        return resolved;
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor,
                                                  Implementation.Context implementationContext,
                                                  Assigner assigner,
                                                  ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                                  MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                  StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                  TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  SuppressionHandler.Bound suppressionHandler,
                                                  RelocationHandler.Bound relocationHandler,
                                                  StackManipulation exceptionHandler) {
                        MethodDescription.TypeToken typeToken = adviceMethod.asTypeToken();
                        return doApply(methodVisitor,
                                implementationContext,
                                assigner,
                                argumentHandler.bindExit(typeToken, getThrowable().represents(NoExceptionHandler.class)),
                                methodSizeHandler.bindExit(typeToken),
                                stackMapFrameHandler.bindExit(typeToken),
                                instrumentedType,
                                instrumentedMethod,
                                suppressionHandler,
                                relocationHandler,
                                exceptionHandler);
                    }

                    /**
                     * Applies a resolution for a given instrumented method.
                     *
                     * @param instrumentedType      A description of the instrumented type.
                     * @param instrumentedMethod    The instrumented method that is being bound.
                     * @param methodVisitor         The method visitor for writing to the instrumented method.
                     * @param implementationContext The implementation context to use.
                     * @param assigner              The assigner to use.
                     * @param argumentHandler       A handler for accessing values on the local variable array.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param suppressionHandler    The bound suppression handler to use.
                     * @param relocationHandler     The bound relocation handler to use.
                     * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                     * @return A method visitor for visiting the advice method's byte code.
                     */
                    private MethodVisitor doApply(MethodVisitor methodVisitor,
                                                  Implementation.Context implementationContext,
                                                  Assigner assigner,
                                                  ArgumentHandler.ForAdvice argumentHandler,
                                                  MethodSizeHandler.ForAdvice methodSizeHandler,
                                                  StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                  TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  SuppressionHandler.Bound suppressionHandler,
                                                  RelocationHandler.Bound relocationHandler,
                                                  StackManipulation exceptionHandler) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    argumentHandler,
                                    OffsetMapping.Sort.EXIT));
                        }
                        return new CodeTranslationVisitor(methodVisitor,
                                implementationContext,
                                argumentHandler,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                instrumentedType,
                                instrumentedMethod,
                                assigner,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler,
                                relocationHandler,
                                exceptionHandler,
                                postProcessor,
                                true);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ArgumentHandler.Factory getArgumentHandlerFactory() {
                        return backupArguments
                                ? ArgumentHandler.Factory.COPYING
                                : ArgumentHandler.Factory.SIMPLE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDefinition getAdviceType() {
                        return adviceMethod.getReturnType();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Bound bind(TypeDescription instrumentedType,
                                      MethodDescription instrumentedMethod,
                                      MethodVisitor methodVisitor,
                                      Implementation.Context implementationContext,
                                      Assigner assigner,
                                      ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                      MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                      StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                      StackManipulation exceptionHandler,
                                      RelocationHandler.Relocation relocation) {
                        return new AdviceMethodInliner(instrumentedType,
                                instrumentedMethod,
                                methodVisitor,
                                implementationContext,
                                assigner,
                                argumentHandler,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                suppressionHandler.bind(exceptionHandler),
                                relocationHandler.bind(instrumentedMethod, relocation),
                                exceptionHandler,
                                classReader);
                    }

                    /**
                     * Implementation of exit advice that handles exceptions.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class WithExceptionHandler extends Inlining.Resolved.ForMethodExit {

                        /**
                         * The type of the handled throwable type for which this advice is invoked.
                         */
                        private final TypeDescription throwable;

                        /**
                         * Creates a new resolved dispatcher for implementing method exit advice that handles exceptions.
                         *
                         * @param adviceMethod            The represented advice method.
                         * @param postProcessor           The post processor to apply.
                         * @param namedTypes              A mapping of all available local variables by their name to their type.
                         * @param uninitializedNamedTypes A mapping of all uninitialized local variables by their name to their type.
                         * @param userFactories           A list of user-defined factories for offset mappings.
                         * @param classReader             The class reader for parsing the advice method's class file.
                         * @param enterType               The type of the value supplied by the enter advice method or
                         *                                a description of {@code void} if no such value exists.
                         * @param throwable               The type of the handled throwable type for which this advice is invoked.
                         */
                        protected WithExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                       PostProcessor postProcessor,
                                                       Map<String, TypeDefinition> namedTypes,
                                                       Map<String, TypeDefinition> uninitializedNamedTypes,
                                                       List<? extends OffsetMapping.Factory<?>> userFactories,
                                                       AsmClassReader classReader,
                                                       TypeDefinition enterType,
                                                       TypeDescription throwable) {
                            super(adviceMethod, postProcessor, namedTypes, uninitializedNamedTypes, userFactories, classReader, enterType);
                            this.throwable = throwable;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDescription getThrowable() {
                            return throwable;
                        }
                    }

                    /**
                     * Implementation of exit advice that ignores exceptions.
                     */
                    protected static class WithoutExceptionHandler extends Inlining.Resolved.ForMethodExit {

                        /**
                         * Creates a new resolved dispatcher for implementing method exit advice that does not handle exceptions.
                         *
                         * @param adviceMethod            The represented advice method.
                         * @param postProcessor           The post processor to apply.
                         * @param namedTypes              A mapping of all available local variables by their name to their type.
                         * @param uninitializedNamedTypes A mapping of all uninitialized local variables by their name to their type.
                         * @param userFactories           A list of user-defined factories for offset mappings.
                         * @param classReader             A class reader to query for the class file of the advice method.
                         * @param enterType               The type of the value supplied by the enter advice method or
                         *                                a description of {@code void} if no such value exists.
                         */
                        protected WithoutExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                          PostProcessor postProcessor,
                                                          Map<String, TypeDefinition> namedTypes,
                                                          Map<String, TypeDefinition> uninitializedNamedTypes,
                                                          List<? extends OffsetMapping.Factory<?>> userFactories,
                                                          AsmClassReader classReader,
                                                          TypeDefinition enterType) {
                            super(adviceMethod, postProcessor, namedTypes, uninitializedNamedTypes, userFactories, classReader, enterType);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDescription getThrowable() {
                            return NoExceptionHandler.DESCRIPTION;
                        }
                    }
                }
            }

            /**
             * A visitor for translating an advice method's byte code for inlining into the instrumented method.
             */
            protected static class CodeTranslationVisitor extends MethodVisitor {

                /**
                 * The original method visitor to which all instructions are eventually written to.
                 */
                protected final MethodVisitor methodVisitor;

                /**
                 * The implementation context to use.
                 */
                protected final Context implementationContext;

                /**
                 * A handler for accessing values on the local variable array.
                 */
                protected final ArgumentHandler.ForAdvice argumentHandler;

                /**
                 * A handler for computing the method size requirements.
                 */
                protected final MethodSizeHandler.ForAdvice methodSizeHandler;

                /**
                 * A handler for translating and injecting stack map frames.
                 */
                protected final StackMapFrameHandler.ForAdvice stackMapFrameHandler;

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The instrumented method.
                 */
                private final MethodDescription instrumentedMethod;

                /**
                 * The assigner to use.
                 */
                private final Assigner assigner;

                /**
                 * The advice method.
                 */
                protected final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * A mapping of offsets to resolved target offsets in the instrumented method.
                 */
                private final Map<Integer, OffsetMapping.Target> offsetMappings;

                /**
                 * A bound suppression handler that is used for suppressing exceptions of this advice method.
                 */
                private final SuppressionHandler.Bound suppressionHandler;

                /**
                 * A bound relocation handler that is responsible for considering a non-standard control flow.
                 */
                private final RelocationHandler.Bound relocationHandler;

                /**
                 * The exception handler that is resolved for the instrumented method.
                 */
                private final StackManipulation exceptionHandler;

                /**
                 * The post processor to apply.
                 */
                private final PostProcessor postProcessor;

                /**
                 * {@code true} if this visitor is for exit advice.
                 */
                private final boolean exit;

                /**
                 * A label indicating the end of the advice byte code.
                 */
                protected final Label endOfMethod;

                /**
                 * Creates a new code translation visitor.
                 *
                 * @param methodVisitor         A method visitor for writing the instrumented method's byte code.
                 * @param implementationContext The implementation context to use.
                 * @param argumentHandler       A handler for accessing values on the local variable array.
                 * @param methodSizeHandler     A handler for computing the method size requirements.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param instrumentedType      The instrumented type.
                 * @param instrumentedMethod    The instrumented method.
                 * @param assigner              The assigner to use.
                 * @param adviceMethod          The advice method.
                 * @param offsetMappings        A mapping of offsets to resolved target offsets in the instrumented method.
                 * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                 * @param relocationHandler     A bound relocation handler that is responsible for considering a non-standard control flow.
                 * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                 * @param postProcessor         The post processor to apply.
                 * @param exit                  {@code true} if this visitor is for exit advice.
                 */
                protected CodeTranslationVisitor(MethodVisitor methodVisitor,
                                                 Context implementationContext,
                                                 ArgumentHandler.ForAdvice argumentHandler,
                                                 MethodSizeHandler.ForAdvice methodSizeHandler,
                                                 StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                 TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 MethodDescription.InDefinedShape adviceMethod,
                                                 Map<Integer, OffsetMapping.Target> offsetMappings,
                                                 SuppressionHandler.Bound suppressionHandler,
                                                 RelocationHandler.Bound relocationHandler,
                                                 StackManipulation exceptionHandler,
                                                 PostProcessor postProcessor,
                                                 boolean exit) {
                    super(OpenedClassReader.ASM_API, StackAwareMethodVisitor.of(methodVisitor, instrumentedMethod));
                    this.methodVisitor = methodVisitor;
                    this.implementationContext = implementationContext;
                    this.argumentHandler = argumentHandler;
                    this.methodSizeHandler = methodSizeHandler;
                    this.stackMapFrameHandler = stackMapFrameHandler;
                    this.instrumentedType = instrumentedType;
                    this.instrumentedMethod = instrumentedMethod;
                    this.assigner = assigner;
                    this.adviceMethod = adviceMethod;
                    this.offsetMappings = offsetMappings;
                    this.suppressionHandler = suppressionHandler;
                    this.relocationHandler = relocationHandler;
                    this.exceptionHandler = exceptionHandler;
                    this.postProcessor = postProcessor;
                    this.exit = exit;
                    endOfMethod = new Label();
                }

                /**
                 * Propagates a label for an exception handler that is typically suppressed by the overlaying
                 * {@link Resolved.AdviceMethodInliner.ExceptionTableSubstitutor}.
                 *
                 * @param label The label to register as a target for an exception handler.
                 */
                protected void propagateHandler(Label label) {
                    ((StackAwareMethodVisitor) mv).register(label, Collections.singletonList(StackSize.SINGLE));
                }

                @Override
                public void visitParameter(String name, int modifiers) {
                    /* do nothing */
                }

                @Override
                public void visitAnnotableParameterCount(int count, boolean visible) {
                    /* do nothing */
                }

                @Override
                @MaybeNull
                public AnnotationVisitor visitAnnotationDefault() {
                    return IGNORE_ANNOTATION;
                }

                @Override
                @MaybeNull
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                @MaybeNull
                public AnnotationVisitor visitTypeAnnotation(int typeReference, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                @MaybeNull
                public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public void visitAttribute(Attribute attribute) {
                    /* do nothing */
                }

                @Override
                public void visitCode() {
                    suppressionHandler.onStart(methodVisitor);
                }

                @Override
                public void visitFrame(int type, int localVariableLength, @MaybeNull Object[] localVariable, int stackSize, @MaybeNull Object[] stack) {
                    stackMapFrameHandler.translateFrame(methodVisitor, type, localVariableLength, localVariable, stackSize, stack);
                }

                @Override
                public void visitVarInsn(int opcode, int offset) {
                    OffsetMapping.Target target = offsetMappings.get(offset);
                    if (target != null) {
                        StackManipulation stackManipulation;
                        StackSize expectedGrowth;
                        switch (opcode) {
                            case Opcodes.ILOAD:
                            case Opcodes.FLOAD:
                            case Opcodes.ALOAD:
                                stackManipulation = target.resolveRead();
                                expectedGrowth = StackSize.SINGLE;
                                break;
                            case Opcodes.DLOAD:
                            case Opcodes.LLOAD:
                                stackManipulation = target.resolveRead();
                                expectedGrowth = StackSize.DOUBLE;
                                break;
                            case Opcodes.ISTORE:
                            case Opcodes.FSTORE:
                            case Opcodes.ASTORE:
                            case Opcodes.LSTORE:
                            case Opcodes.DSTORE:
                                stackManipulation = target.resolveWrite();
                                expectedGrowth = StackSize.ZERO;
                                break;
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                        methodSizeHandler.requireStackSizePadding(stackManipulation.apply(mv, implementationContext).getMaximalSize() - expectedGrowth.getSize());
                    } else {
                        mv.visitVarInsn(opcode, argumentHandler.mapped(offset));
                    }
                }

                @Override
                public void visitIincInsn(int offset, int value) {
                    OffsetMapping.Target target = offsetMappings.get(offset);
                    if (target != null) {
                        methodSizeHandler.requireStackSizePadding(target.resolveIncrement(value).apply(mv, implementationContext).getMaximalSize());
                    } else {
                        mv.visitIincInsn(argumentHandler.mapped(offset), value);
                    }
                }

                @Override
                public void visitInsn(int opcode) {
                    switch (opcode) {
                        case Opcodes.RETURN:
                            ((StackAwareMethodVisitor) mv).drainStack();
                            break;
                        case Opcodes.IRETURN:
                            methodSizeHandler.requireLocalVariableLength(((StackAwareMethodVisitor) mv).drainStack(Opcodes.ISTORE, Opcodes.ILOAD, StackSize.SINGLE));
                            break;
                        case Opcodes.ARETURN:
                            methodSizeHandler.requireLocalVariableLength(((StackAwareMethodVisitor) mv).drainStack(Opcodes.ASTORE, Opcodes.ALOAD, StackSize.SINGLE));
                            break;
                        case Opcodes.FRETURN:
                            methodSizeHandler.requireLocalVariableLength(((StackAwareMethodVisitor) mv).drainStack(Opcodes.FSTORE, Opcodes.FLOAD, StackSize.SINGLE));
                            break;
                        case Opcodes.LRETURN:
                            methodSizeHandler.requireLocalVariableLength(((StackAwareMethodVisitor) mv).drainStack(Opcodes.LSTORE, Opcodes.LLOAD, StackSize.DOUBLE));
                            break;
                        case Opcodes.DRETURN:
                            methodSizeHandler.requireLocalVariableLength(((StackAwareMethodVisitor) mv).drainStack(Opcodes.DSTORE, Opcodes.DLOAD, StackSize.DOUBLE));
                            break;
                        default:
                            mv.visitInsn(opcode);
                            return;
                    }
                    mv.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                }

                @Override
                public void visitEnd() {
                    suppressionHandler.onEnd(methodVisitor, implementationContext, methodSizeHandler, stackMapFrameHandler, adviceMethod.getReturnType());
                    methodVisitor.visitLabel(endOfMethod);
                    if (adviceMethod.getReturnType().represents(boolean.class)
                            || adviceMethod.getReturnType().represents(byte.class)
                            || adviceMethod.getReturnType().represents(short.class)
                            || adviceMethod.getReturnType().represents(char.class)
                            || adviceMethod.getReturnType().represents(int.class)) {
                        stackMapFrameHandler.injectReturnFrame(methodVisitor);
                        methodVisitor.visitVarInsn(Opcodes.ISTORE, exit ? argumentHandler.exit() : argumentHandler.enter());
                    } else if (adviceMethod.getReturnType().represents(long.class)) {
                        stackMapFrameHandler.injectReturnFrame(methodVisitor);
                        methodVisitor.visitVarInsn(Opcodes.LSTORE, exit ? argumentHandler.exit() : argumentHandler.enter());
                    } else if (adviceMethod.getReturnType().represents(float.class)) {
                        stackMapFrameHandler.injectReturnFrame(methodVisitor);
                        methodVisitor.visitVarInsn(Opcodes.FSTORE, exit ? argumentHandler.exit() : argumentHandler.enter());
                    } else if (adviceMethod.getReturnType().represents(double.class)) {
                        stackMapFrameHandler.injectReturnFrame(methodVisitor);
                        methodVisitor.visitVarInsn(Opcodes.DSTORE, exit ? argumentHandler.exit() : argumentHandler.enter());
                    } else if (!adviceMethod.getReturnType().represents(void.class)) {
                        stackMapFrameHandler.injectReturnFrame(methodVisitor);
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, exit ? argumentHandler.exit() : argumentHandler.enter());
                    }
                    methodSizeHandler.requireStackSize(postProcessor
                            .resolve(instrumentedType, instrumentedMethod, assigner, argumentHandler, stackMapFrameHandler, exceptionHandler)
                            .apply(methodVisitor, implementationContext).getMaximalSize());
                    methodSizeHandler.requireStackSize(relocationHandler.apply(methodVisitor,
                            implementationContext,
                            exit ? argumentHandler.exit() : argumentHandler.enter()));
                    stackMapFrameHandler.injectCompletionFrame(methodVisitor);
                }

                @Override
                public void visitMaxs(int stackSize, int localVariableLength) {
                    methodSizeHandler.recordMaxima(stackSize, localVariableLength);
                }
            }
        }

        /**
         * A dispatcher for an advice method that is being invoked from the instrumented method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Delegating implements Unresolved {

            /**
             * The advice method.
             */
            protected final MethodDescription.InDefinedShape adviceMethod;

            /**
             * The delegator factory to use.
             */
            protected final Delegator.Factory delegatorFactory;

            /**
             * Creates a new delegating advice dispatcher.
             *
             * @param adviceMethod     The advice method.
             * @param delegatorFactory The delegator factory to use.
             */
            protected Delegating(MethodDescription.InDefinedShape adviceMethod, Delegator.Factory delegatorFactory) {
                this.adviceMethod = adviceMethod;
                this.delegatorFactory = delegatorFactory;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isAlive() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isBinary() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public TypeDefinition getAdviceType() {
                return adviceMethod.getReturnType();
            }

            /**
             * {@inheritDoc}
             */
            public Map<String, TypeDefinition> getNamedTypes() {
                return Collections.emptyMap();
            }

            /**
             * {@inheritDoc}
             */
            public Dispatcher.Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                    @MaybeNull AsmClassReader classReader,
                                                                    Unresolved methodExit,
                                                                    PostProcessor.Factory postProcessorFactory) {
                Delegator delegator = delegatorFactory.make(adviceMethod, false);
                return Resolved.ForMethodEnter.of(adviceMethod,
                        postProcessorFactory.make(adviceMethod.getDeclaredAnnotations(), delegator.getTypeToken().getReturnType(), false),
                        delegator,
                        userFactories,
                        methodExit.getAdviceType(),
                        methodExit.isAlive());
            }

            /**
             * {@inheritDoc}
             */
            public Dispatcher.Resolved.ForMethodExit asMethodExit(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                  @MaybeNull AsmClassReader classReader,
                                                                  Unresolved methodEnter,
                                                                  PostProcessor.Factory postProcessorFactory) {
                Map<String, TypeDefinition> namedTypes = methodEnter.getNamedTypes();
                for (ParameterDescription parameterDescription : adviceMethod.getParameters()) {
                    AnnotationDescription.Loadable<Local> annotationDescription = parameterDescription.getDeclaredAnnotations().ofType(Local.class);
                    if (annotationDescription != null) {
                        String name = annotationDescription.getValue(OffsetMapping.ForLocalValue.Factory.LOCAL_VALUE).resolve(String.class);
                        TypeDefinition typeDefinition = namedTypes.get(name);
                        if (typeDefinition == null) {
                            throw new IllegalStateException(adviceMethod + " attempts use of undeclared local variable " + name);
                        } else if (!typeDefinition.equals(parameterDescription.getType())) {
                            throw new IllegalStateException(adviceMethod + " does not read variable " + name + " as " + typeDefinition);
                        }
                    }
                }
                Delegator delegator = delegatorFactory.make(adviceMethod, true);
                return Resolved.ForMethodExit.of(adviceMethod,
                        postProcessorFactory.make(adviceMethod.getDeclaredAnnotations(), delegator.getTypeToken().getReturnType(), true),
                        delegator,
                        namedTypes,
                        userFactories,
                        methodEnter.getAdviceType());
            }

            @Override
            public String toString() {
                return "Delegate to " + adviceMethod;
            }

            /**
             * A resolved version of a dispatcher.
             */
            protected abstract static class Resolved extends Dispatcher.Resolved.AbstractBase {

                /**
                 * The delegator to use.
                 */
                protected final Delegator delegator;

                /**
                 * Creates a new resolved version of a dispatcher.
                 *
                 * @param adviceMethod     The represented advice method.
                 * @param postProcessor    The post processor to apply.
                 * @param factories        A list of factories to resolve for the parameters of the advice method.
                 * @param throwableType    The type to handle by a suppression handler or {@link NoExceptionHandler} to not handle any exceptions.
                 * @param relocatableType  The type to trigger a relocation of the method's control flow or {@code void} if no relocation should be executed.
                 * @param relocatableIndex The index within an array that is returned by the advice method, indicating the value to consider for relocation.
                 * @param delegator        The delegator to use.
                 */
                protected Resolved(MethodDescription.InDefinedShape adviceMethod,
                                   PostProcessor postProcessor,
                                   List<? extends OffsetMapping.Factory<?>> factories,
                                   TypeDescription throwableType,
                                   TypeDescription relocatableType,
                                   int relocatableIndex,
                                   Delegator delegator) {
                    super(adviceMethod, postProcessor, factories, throwableType, relocatableType, relocatableIndex, OffsetMapping.Factory.AdviceType.DELEGATION);
                    this.delegator = delegator;
                }

                /**
                 * {@inheritDoc}
                 */
                public Map<String, TypeDefinition> getNamedTypes() {
                    return Collections.emptyMap();
                }

                /**
                 * {@inheritDoc}
                 */
                public Bound bind(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  Assigner assigner,
                                  ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                  MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                  StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                  StackManipulation exceptionHandler,
                                  RelocationHandler.Relocation relocation) {
                    delegator.assertVisibility(instrumentedType);
                    return resolve(instrumentedType,
                            instrumentedMethod,
                            methodVisitor,
                            implementationContext,
                            assigner,
                            argumentHandler,
                            methodSizeHandler,
                            stackMapFrameHandler,
                            exceptionHandler,
                            relocation);
                }

                /**
                 * Binds this dispatcher for resolution to a specific method.
                 *
                 * @param instrumentedType      A description of the instrumented type.
                 * @param instrumentedMethod    The instrumented method that is being bound.
                 * @param methodVisitor         The method visitor for writing to the instrumented method.
                 * @param implementationContext The implementation context to use.
                 * @param assigner              The assigner to use.
                 * @param argumentHandler       A handler for accessing values on the local variable array.
                 * @param methodSizeHandler     A handler for computing the method size requirements.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
                 * @param relocation            A relocation to use with a relocation handler.
                 * @return An appropriate bound advice dispatcher.
                 */
                protected abstract Bound resolve(TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 MethodVisitor methodVisitor,
                                                 Implementation.Context implementationContext,
                                                 Assigner assigner,
                                                 ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                                 MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                 StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                 StackManipulation exceptionHandler,
                                                 RelocationHandler.Relocation relocation);

                /**
                 * A bound advice method that copies the code by first extracting the exception table and later appending the
                 * code of the method without copying any meta data.
                 */
                protected abstract static class AdviceMethodWriter implements Bound {

                    /**
                     * The advice method.
                     */
                    protected final MethodDescription.TypeToken typeToken;

                    /**
                     * The instrumented type.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * The instrumented method.
                     */
                    private final MethodDescription instrumentedMethod;

                    /**
                     * The assigner to use.
                     */
                    private final Assigner assigner;

                    /**
                     * The offset mappings available to this advice.
                     */
                    private final List<OffsetMapping.Target> offsetMappings;

                    /**
                     * The method visitor for writing the instrumented method.
                     */
                    protected final MethodVisitor methodVisitor;

                    /**
                     * The implementation context to use.
                     */
                    protected final Implementation.Context implementationContext;

                    /**
                     * A handler for accessing values on the local variable array.
                     */
                    protected final ArgumentHandler.ForAdvice argumentHandler;

                    /**
                     * A handler for computing the method size requirements.
                     */
                    protected final MethodSizeHandler.ForAdvice methodSizeHandler;

                    /**
                     * A handler for translating and injecting stack map frames.
                     */
                    protected final StackMapFrameHandler.ForAdvice stackMapFrameHandler;

                    /**
                     * A bound suppression handler that is used for suppressing exceptions of this advice method.
                     */
                    private final SuppressionHandler.Bound suppressionHandler;

                    /**
                     * A bound relocation handler that is responsible for considering a non-standard control flow.
                     */
                    private final RelocationHandler.Bound relocationHandler;

                    /**
                     * The exception handler that is resolved for the instrumented method.
                     */
                    private final StackManipulation exceptionHandler;

                    /**
                     * The post processor to apply.
                     */
                    private final PostProcessor postProcessor;

                    /**
                     * The delegator to use.
                     */
                    private final Delegator delegator;

                    /**
                     * Creates a new advice method writer.
                     *
                     * @param typeToken             The advice method's type token.
                     * @param instrumentedType      The instrumented type.
                     * @param instrumentedMethod    The instrumented method.
                     * @param assigner              The assigner to use.
                     * @param postProcessor         The post processor to apply.
                     * @param offsetMappings        The offset mappings available to this advice.
                     * @param methodVisitor         The method visitor for writing the instrumented method.
                     * @param implementationContext The implementation context to use.
                     * @param argumentHandler       A handler for accessing values on the local variable array.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                     * @param relocationHandler     A bound relocation handler that is responsible for considering a non-standard control flow.
                     * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                     * @param delegator             The delegator to use.
                     */
                    protected AdviceMethodWriter(MethodDescription.TypeToken typeToken,
                                                 TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 PostProcessor postProcessor,
                                                 List<OffsetMapping.Target> offsetMappings,
                                                 MethodVisitor methodVisitor,
                                                 Context implementationContext,
                                                 ArgumentHandler.ForAdvice argumentHandler,
                                                 MethodSizeHandler.ForAdvice methodSizeHandler,
                                                 StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                 SuppressionHandler.Bound suppressionHandler,
                                                 RelocationHandler.Bound relocationHandler,
                                                 StackManipulation exceptionHandler,
                                                 Delegator delegator) {
                        this.typeToken = typeToken;
                        this.instrumentedType = instrumentedType;
                        this.instrumentedMethod = instrumentedMethod;
                        this.assigner = assigner;
                        this.postProcessor = postProcessor;
                        this.offsetMappings = offsetMappings;
                        this.methodVisitor = methodVisitor;
                        this.implementationContext = implementationContext;
                        this.argumentHandler = argumentHandler;
                        this.methodSizeHandler = methodSizeHandler;
                        this.stackMapFrameHandler = stackMapFrameHandler;
                        this.suppressionHandler = suppressionHandler;
                        this.relocationHandler = relocationHandler;
                        this.exceptionHandler = exceptionHandler;
                        this.delegator = delegator;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void prepare() {
                        suppressionHandler.onPrepare(methodVisitor);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void apply() {
                        suppressionHandler.onStart(methodVisitor);
                        int index = 0, currentStackSize = 0, maximumStackSize = 0;
                        for (OffsetMapping.Target offsetMapping : offsetMappings) {
                            currentStackSize += typeToken.getParameterTypes().get(index++).getStackSize().getSize();
                            maximumStackSize = Math.max(maximumStackSize, currentStackSize + offsetMapping.resolveRead()
                                    .apply(methodVisitor, implementationContext)
                                    .getMaximalSize());
                        }
                        maximumStackSize = Math.max(maximumStackSize, delegator.apply(instrumentedType, instrumentedMethod)
                                .apply(methodVisitor, implementationContext)
                                .getMaximalSize());
                        suppressionHandler.onEndWithSkip(methodVisitor,
                                implementationContext,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                typeToken.getReturnType());
                        if (typeToken.getReturnType().represents(boolean.class)
                                || typeToken.getReturnType().represents(byte.class)
                                || typeToken.getReturnType().represents(short.class)
                                || typeToken.getReturnType().represents(char.class)
                                || typeToken.getReturnType().represents(int.class)) {
                            methodVisitor.visitVarInsn(Opcodes.ISTORE, isExitAdvice() ? argumentHandler.exit() : argumentHandler.enter());
                        } else if (typeToken.getReturnType().represents(long.class)) {
                            methodVisitor.visitVarInsn(Opcodes.LSTORE, isExitAdvice() ? argumentHandler.exit() : argumentHandler.enter());
                        } else if (typeToken.getReturnType().represents(float.class)) {
                            methodVisitor.visitVarInsn(Opcodes.FSTORE, isExitAdvice() ? argumentHandler.exit() : argumentHandler.enter());
                        } else if (typeToken.getReturnType().represents(double.class)) {
                            methodVisitor.visitVarInsn(Opcodes.DSTORE, isExitAdvice() ? argumentHandler.exit() : argumentHandler.enter());
                        } else if (typeToken.getReturnType().represents(void.class)) {
                            methodVisitor.visitInsn(Opcodes.NOP);
                        } else {
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, isExitAdvice() ? argumentHandler.exit() : argumentHandler.enter());
                        }
                        methodSizeHandler.requireStackSize(postProcessor
                                .resolve(instrumentedType, instrumentedMethod, assigner, argumentHandler, stackMapFrameHandler, exceptionHandler)
                                .apply(methodVisitor, implementationContext).getMaximalSize());
                        methodSizeHandler.requireStackSize(relocationHandler.apply(methodVisitor,
                                implementationContext,
                                isExitAdvice() ? argumentHandler.exit() : argumentHandler.enter()));
                        stackMapFrameHandler.injectCompletionFrame(methodVisitor);
                        methodSizeHandler.requireStackSize(Math.max(maximumStackSize, typeToken.getReturnType().getStackSize().getSize()));
                        methodSizeHandler.requireLocalVariableLength(instrumentedMethod.getStackSize() + typeToken.getReturnType().getStackSize().getSize());
                    }

                    /**
                     * Returns {@code true} if this writer represents exit advice.
                     *
                     * @return {@code true} if this writer represents exit advice.
                     */
                    protected abstract boolean isExitAdvice();

                    /**
                     * An advice method writer for a method enter.
                     */
                    protected static class ForMethodEnter extends AdviceMethodWriter {

                        /**
                         * Creates a new advice method writer.
                         *
                         * @param typeToken             The advice method's type token.
                         * @param instrumentedType      The instrumented type.
                         * @param instrumentedMethod    The instrumented method.
                         * @param assigner              The assigner to use.
                         * @param postProcessor         The post processor to apply.
                         * @param offsetMappings        The offset mappings available to this advice.
                         * @param methodVisitor         The method visitor for writing the instrumented method.
                         * @param implementationContext The implementation context to use.
                         * @param argumentHandler       A handler for accessing values on the local variable array.
                         * @param methodSizeHandler     A handler for computing the method size requirements.
                         * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                         * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                         * @param relocationHandler     A bound relocation handler that is responsible for considering a non-standard control flow.
                         * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                         * @param delegator             The delegator to use.
                         */
                        protected ForMethodEnter(MethodDescription.TypeToken typeToken,
                                                 TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 PostProcessor postProcessor,
                                                 List<OffsetMapping.Target> offsetMappings,
                                                 MethodVisitor methodVisitor,
                                                 Implementation.Context implementationContext,
                                                 ArgumentHandler.ForAdvice argumentHandler,
                                                 MethodSizeHandler.ForAdvice methodSizeHandler,
                                                 StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                 SuppressionHandler.Bound suppressionHandler,
                                                 RelocationHandler.Bound relocationHandler,
                                                 StackManipulation exceptionHandler,
                                                 Delegator delegator) {
                            super(typeToken,
                                    instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    postProcessor,
                                    offsetMappings,
                                    methodVisitor,
                                    implementationContext,
                                    argumentHandler,
                                    methodSizeHandler,
                                    stackMapFrameHandler,
                                    suppressionHandler,
                                    relocationHandler,
                                    exceptionHandler,
                                    delegator);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public void initialize() {
                            /* do nothing */
                        }

                        @Override
                        protected boolean isExitAdvice() {
                            return false;
                        }
                    }

                    /**
                     * An advice method writer for a method exit.
                     */
                    protected static class ForMethodExit extends AdviceMethodWriter {

                        /**
                         * Creates a new advice method writer.
                         *
                         * @param typeToken             The advice method's type token.
                         * @param instrumentedType      The instrumented type.
                         * @param instrumentedMethod    The instrumented method.
                         * @param assigner              The assigner to use.
                         * @param postProcessor         The post processor to apply.
                         * @param offsetMappings        The offset mappings available to this advice.
                         * @param methodVisitor         The method visitor for writing the instrumented method.
                         * @param implementationContext The implementation context to use.
                         * @param argumentHandler       A handler for accessing values on the local variable array.
                         * @param methodSizeHandler     A handler for computing the method size requirements.
                         * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                         * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                         * @param relocationHandler     A bound relocation handler that is responsible for considering a non-standard control flow.
                         * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                         * @param delegator             The delegator to use.
                         */
                        protected ForMethodExit(MethodDescription.TypeToken typeToken,
                                                TypeDescription instrumentedType,
                                                MethodDescription instrumentedMethod,
                                                Assigner assigner,
                                                PostProcessor postProcessor,
                                                List<OffsetMapping.Target> offsetMappings,
                                                MethodVisitor methodVisitor,
                                                Implementation.Context implementationContext,
                                                ArgumentHandler.ForAdvice argumentHandler,
                                                MethodSizeHandler.ForAdvice methodSizeHandler,
                                                StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                SuppressionHandler.Bound suppressionHandler,
                                                RelocationHandler.Bound relocationHandler,
                                                StackManipulation exceptionHandler,
                                                Delegator delegator) {
                            super(typeToken,
                                    instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    postProcessor,
                                    offsetMappings,
                                    methodVisitor,
                                    implementationContext,
                                    argumentHandler,
                                    methodSizeHandler,
                                    stackMapFrameHandler,
                                    suppressionHandler,
                                    relocationHandler,
                                    exceptionHandler,
                                    delegator);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public void initialize() {
                            if (typeToken.getReturnType().represents(boolean.class)
                                    || typeToken.getReturnType().represents(byte.class)
                                    || typeToken.getReturnType().represents(short.class)
                                    || typeToken.getReturnType().represents(char.class)
                                    || typeToken.getReturnType().represents(int.class)) {
                                methodVisitor.visitInsn(Opcodes.ICONST_0);
                                methodVisitor.visitVarInsn(Opcodes.ISTORE, argumentHandler.exit());
                            } else if (typeToken.getReturnType().represents(long.class)) {
                                methodVisitor.visitInsn(Opcodes.LCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.LSTORE, argumentHandler.exit());
                            } else if (typeToken.getReturnType().represents(float.class)) {
                                methodVisitor.visitInsn(Opcodes.FCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.FSTORE, argumentHandler.exit());
                            } else if (typeToken.getReturnType().represents(double.class)) {
                                methodVisitor.visitInsn(Opcodes.DCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.DSTORE, argumentHandler.exit());
                            } else if (!typeToken.getReturnType().represents(void.class)) {
                                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                                methodVisitor.visitVarInsn(Opcodes.ASTORE, argumentHandler.exit());
                            }
                            methodSizeHandler.requireStackSize(typeToken.getReturnType().getStackSize().getSize());
                        }

                        @Override
                        protected boolean isExitAdvice() {
                            return true;
                        }
                    }
                }

                /**
                 * A resolved dispatcher for implementing method enter advice.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected abstract static class ForMethodEnter extends Delegating.Resolved implements Dispatcher.Resolved.ForMethodEnter {

                    /**
                     * {@code true} if the first discovered line number information should be prepended to the advice code.
                     */
                    private final boolean prependLineNumber;

                    /**
                     * Creates a new resolved dispatcher for implementing method enter advice.
                     *
                     * @param adviceMethod  The represented advice method.
                     * @param postProcessor The post processor to apply.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param exitType      The exit type or {@code void} if no exit type is defined.
                     * @param delegator     The delegator to use.
                     */
                    @SuppressWarnings("unchecked") // In absence of @SafeVarargs
                    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming annotation for exit advice.")
                    protected ForMethodEnter(MethodDescription.InDefinedShape adviceMethod,
                                             PostProcessor postProcessor,
                                             List<? extends OffsetMapping.Factory<?>> userFactories,
                                             TypeDefinition exitType,
                                             Delegator delegator) {
                        super(adviceMethod,
                                postProcessor,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForArgument.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForFieldHandle.Unresolved.ReaderFactory.INSTANCE,
                                        OffsetMapping.ForFieldHandle.Unresolved.WriterFactory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForSelfCallHandle.Factory.INSTANCE,
                                        OffsetMapping.ForUnusedValue.Factory.INSTANCE,
                                        OffsetMapping.ForStubValue.INSTANCE,
                                        OffsetMapping.ForExitValue.Factory.of(exitType),
                                        new OffsetMapping.Factory.Illegal<Thrown>(Thrown.class),
                                        new OffsetMapping.Factory.Illegal<Enter>(Enter.class),
                                        new OffsetMapping.Factory.Illegal<Local>(Local.class),
                                        new OffsetMapping.Factory.Illegal<Return>(Return.class)), userFactories),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SUPPRESS_ENTER).resolve(TypeDescription.class),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SKIP_ON).resolve(TypeDescription.class),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SKIP_ON_INDEX).resolve(Integer.class),
                                delegator);
                        prependLineNumber = adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(PREPEND_LINE_NUMBER).resolve(Boolean.class);
                    }

                    /**
                     * Resolves enter advice that only exposes the enter type if this is necessary.
                     *
                     * @param adviceMethod  The advice method.
                     * @param postProcessor The post processor to apply.
                     * @param delegator     The delegator to use.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param exitType      The exit type or {@code void} if no exit type is defined.
                     * @param methodExit    {@code true} if exit advice is applied.
                     * @return An appropriate enter handler.
                     */
                    protected static Resolved.ForMethodEnter of(MethodDescription.InDefinedShape adviceMethod,
                                                                PostProcessor postProcessor,
                                                                Delegator delegator,
                                                                List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                TypeDefinition exitType,
                                                                boolean methodExit) {
                        return methodExit
                                ? new WithRetainedEnterType(adviceMethod, postProcessor, userFactories, exitType, delegator)
                                : new WithDiscardedEnterType(adviceMethod, postProcessor, userFactories, exitType, delegator);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean isPrependLineNumber() {
                        return prependLineNumber;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDefinition getActualAdviceType() {
                        return delegator.getTypeToken().getReturnType();
                    }

                    @Override
                    protected Bound resolve(TypeDescription instrumentedType,
                                            MethodDescription instrumentedMethod,
                                            MethodVisitor methodVisitor,
                                            Implementation.Context implementationContext,
                                            Assigner assigner,
                                            ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                            MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                            StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                            StackManipulation exceptionHandler,
                                            RelocationHandler.Relocation relocation) {
                        return doResolve(instrumentedType,
                                instrumentedMethod,
                                methodVisitor,
                                implementationContext,
                                assigner,
                                argumentHandler.bindEnter(delegator.getTypeToken()),
                                methodSizeHandler.bindEnter(delegator.getTypeToken()),
                                stackMapFrameHandler.bindEnter(delegator.getTypeToken()),
                                suppressionHandler.bind(exceptionHandler),
                                relocationHandler.bind(instrumentedMethod, relocation),
                                exceptionHandler);
                    }

                    /**
                     * Binds this dispatcher for resolution to a specific method.
                     *
                     * @param instrumentedType      A description of the instrumented type.
                     * @param instrumentedMethod    The instrumented method that is being bound.
                     * @param methodVisitor         The method visitor for writing to the instrumented method.
                     * @param implementationContext The implementation context to use.
                     * @param assigner              The assigner to use.
                     * @param argumentHandler       A handler for accessing values on the local variable array.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param suppressionHandler    The bound suppression handler to use.
                     * @param relocationHandler     The bound relocation handler to use.
                     * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                     * @return An appropriate bound advice dispatcher.
                     */
                    protected Bound doResolve(TypeDescription instrumentedType,
                                              MethodDescription instrumentedMethod,
                                              MethodVisitor methodVisitor,
                                              Implementation.Context implementationContext,
                                              Assigner assigner,
                                              ArgumentHandler.ForAdvice argumentHandler,
                                              MethodSizeHandler.ForAdvice methodSizeHandler,
                                              StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                              SuppressionHandler.Bound suppressionHandler,
                                              RelocationHandler.Bound relocationHandler,
                                              StackManipulation exceptionHandler) {
                        List<OffsetMapping.Target> offsetMappings = new ArrayList<OffsetMapping.Target>(this.offsetMappings.size());
                        for (OffsetMapping offsetMapping : this.offsetMappings.values()) {
                            offsetMappings.add(offsetMapping.resolve(instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    argumentHandler,
                                    OffsetMapping.Sort.ENTER));
                        }
                        return new AdviceMethodWriter.ForMethodEnter(delegator.getTypeToken(),
                                instrumentedType,
                                instrumentedMethod,
                                assigner,
                                postProcessor,
                                offsetMappings,
                                methodVisitor,
                                implementationContext,
                                argumentHandler,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                suppressionHandler,
                                relocationHandler,
                                exceptionHandler,
                                delegator);
                    }

                    /**
                     * Implementation of an advice that does expose an enter type.
                     */
                    protected static class WithRetainedEnterType extends Delegating.Resolved.ForMethodEnter {

                        /**
                         * Creates a new resolved dispatcher for implementing method enter advice that does expose the enter type.
                         *
                         * @param adviceMethod  The represented advice method.
                         * @param postProcessor The post processor to apply.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param exitType      The exit type or {@code void} if no exit type is defined.
                         * @param delegator     The delegator to use.
                         */
                        protected WithRetainedEnterType(MethodDescription.InDefinedShape adviceMethod,
                                                        PostProcessor postProcessor,
                                                        List<? extends OffsetMapping.Factory<?>> userFactories,
                                                        TypeDefinition exitType,
                                                        Delegator delegator) {
                            super(adviceMethod, postProcessor, userFactories, exitType, delegator);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDefinition getAdviceType() {
                            return delegator.getTypeToken().getReturnType();
                        }
                    }

                    /**
                     * Implementation of an advice that does not expose an enter type.
                     */
                    protected static class WithDiscardedEnterType extends Delegating.Resolved.ForMethodEnter {

                        /**
                         * Creates a new resolved dispatcher for implementing method enter advice that does not expose the enter type.
                         *
                         * @param adviceMethod  The represented advice method.
                         * @param postProcessor The post processor to apply.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param exitType      The exit type or {@code void} if no exit type is defined.
                         * @param delegator     The delegator to use.
                         */
                        protected WithDiscardedEnterType(MethodDescription.InDefinedShape adviceMethod,
                                                         PostProcessor postProcessor,
                                                         List<? extends OffsetMapping.Factory<?>> userFactories,
                                                         TypeDefinition exitType,
                                                         Delegator delegator) {
                            super(adviceMethod, postProcessor, userFactories, exitType, delegator);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDefinition getAdviceType() {
                            return TypeDescription.ForLoadedType.of(void.class);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        protected Bound doResolve(TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  MethodVisitor methodVisitor,
                                                  Context implementationContext,
                                                  Assigner assigner,
                                                  ArgumentHandler.ForAdvice argumentHandler,
                                                  MethodSizeHandler.ForAdvice methodSizeHandler,
                                                  StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                  SuppressionHandler.Bound suppressionHandler,
                                                  RelocationHandler.Bound relocationHandler,
                                                  StackManipulation exceptionHandler) {
                            methodSizeHandler.requireLocalVariableLengthPadding(delegator.getTypeToken().getReturnType().getStackSize().getSize());
                            return super.doResolve(instrumentedType,
                                    instrumentedMethod,
                                    methodVisitor,
                                    implementationContext,
                                    assigner,
                                    argumentHandler,
                                    methodSizeHandler,
                                    stackMapFrameHandler,
                                    suppressionHandler,
                                    relocationHandler,
                                    exceptionHandler);
                        }
                    }
                }

                /**
                 * A resolved dispatcher for implementing method exit advice.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected abstract static class ForMethodExit extends Delegating.Resolved implements Dispatcher.Resolved.ForMethodExit {

                    /**
                     * {@code true} if the arguments of the instrumented method should be copied prior to execution.
                     */
                    private final boolean backupArguments;

                    /**
                     * Creates a new resolved dispatcher for implementing method exit advice.
                     *
                     * @param adviceMethod  The represented advice method.
                     * @param postProcessor The post processor to apply.
                     * @param namedTypes    A mapping of all available local variables by their name to their type.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param enterType     The type of the value supplied by the enter advice method or {@code void} if no such value exists.
                     * @param delegator     The delegator to use.
                     */
                    @SuppressWarnings("unchecked")
                    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming annotation for exit advice.")
                    protected ForMethodExit(MethodDescription.InDefinedShape adviceMethod,
                                            PostProcessor postProcessor,
                                            Map<String, TypeDefinition> namedTypes,
                                            List<? extends OffsetMapping.Factory<?>> userFactories,
                                            TypeDefinition enterType,
                                            Delegator delegator) {
                        super(adviceMethod,
                                postProcessor,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForArgument.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForFieldHandle.Unresolved.ReaderFactory.INSTANCE,
                                        OffsetMapping.ForFieldHandle.Unresolved.WriterFactory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForSelfCallHandle.Factory.INSTANCE,
                                        OffsetMapping.ForUnusedValue.Factory.INSTANCE,
                                        OffsetMapping.ForStubValue.INSTANCE,
                                        OffsetMapping.ForEnterValue.Factory.of(enterType),
                                        OffsetMapping.ForExitValue.Factory.of(adviceMethod.getReturnType()),
                                        new OffsetMapping.ForLocalValue.Factory(namedTypes),
                                        OffsetMapping.ForReturnValue.Factory.INSTANCE,
                                        OffsetMapping.ForThrowable.Factory.of(adviceMethod)
                                ), userFactories),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(SUPPRESS_EXIT).resolve(TypeDescription.class),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(REPEAT_ON).resolve(TypeDescription.class),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(REPEAT_ON_INDEX).resolve(Integer.class),
                                delegator);
                        backupArguments = adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(BACKUP_ARGUMENTS).resolve(Boolean.class);
                    }

                    /**
                     * Resolves exit advice that handles exceptions depending on the specification of the exit advice.
                     *
                     * @param adviceMethod  The advice method.
                     * @param postProcessor The post processor to apply.
                     * @param delegator     The delegator to use.
                     * @param namedTypes    A mapping of all available local variables by their name to their type.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param enterType     The type of the value supplied by the enter advice method or {@code void} if no such value exists.
                     * @return An appropriate exit handler.
                     */
                    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming annotation for exit advice.")
                    protected static Resolved.ForMethodExit of(MethodDescription.InDefinedShape adviceMethod,
                                                               PostProcessor postProcessor,
                                                               Delegator delegator,
                                                               Map<String, TypeDefinition> namedTypes,
                                                               List<? extends OffsetMapping.Factory<?>> userFactories,
                                                               TypeDefinition enterType) {
                        TypeDescription throwable = adviceMethod.getDeclaredAnnotations()
                                .ofType(OnMethodExit.class)
                                .getValue(ON_THROWABLE)
                                .resolve(TypeDescription.class);
                        return throwable.represents(NoExceptionHandler.class)
                                ? new WithoutExceptionHandler(adviceMethod, postProcessor, namedTypes, userFactories, enterType, delegator)
                                : new WithExceptionHandler(adviceMethod, postProcessor, namedTypes, userFactories, enterType, throwable, delegator);
                    }

                    @Override
                    protected Bound resolve(TypeDescription instrumentedType,
                                            MethodDescription instrumentedMethod,
                                            MethodVisitor methodVisitor,
                                            Implementation.Context implementationContext,
                                            Assigner assigner,
                                            ArgumentHandler.ForInstrumentedMethod argumentHandler,
                                            MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                            StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                            StackManipulation exceptionHandler,
                                            RelocationHandler.Relocation relocation) {
                        return doResolve(instrumentedType,
                                instrumentedMethod,
                                methodVisitor,
                                implementationContext,
                                assigner,
                                argumentHandler.bindExit(delegator.getTypeToken(), getThrowable().represents(NoExceptionHandler.class)),
                                methodSizeHandler.bindExit(delegator.getTypeToken()),
                                stackMapFrameHandler.bindExit(delegator.getTypeToken()),
                                suppressionHandler.bind(exceptionHandler),
                                relocationHandler.bind(instrumentedMethod, relocation),
                                exceptionHandler);
                    }

                    /**
                     * Binds this dispatcher for resolution to a specific method.
                     *
                     * @param instrumentedType      A description of the instrumented type.
                     * @param instrumentedMethod    The instrumented method that is being bound.
                     * @param methodVisitor         The method visitor for writing to the instrumented method.
                     * @param implementationContext The implementation context to use.
                     * @param assigner              The assigner to use.
                     * @param argumentHandler       A handler for accessing values on the local variable array.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param suppressionHandler    The bound suppression handler to use.
                     * @param relocationHandler     The bound relocation handler to use.
                     * @param exceptionHandler      The exception handler that is resolved for the instrumented method.
                     * @return An appropriate bound advice dispatcher.
                     */
                    private Bound doResolve(TypeDescription instrumentedType,
                                            MethodDescription instrumentedMethod,
                                            MethodVisitor methodVisitor,
                                            Implementation.Context implementationContext,
                                            Assigner assigner,
                                            ArgumentHandler.ForAdvice argumentHandler,
                                            MethodSizeHandler.ForAdvice methodSizeHandler,
                                            StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                            SuppressionHandler.Bound suppressionHandler,
                                            RelocationHandler.Bound relocationHandler,
                                            StackManipulation exceptionHandler) {
                        List<OffsetMapping.Target> offsetMappings = new ArrayList<OffsetMapping.Target>(this.offsetMappings.size());
                        for (OffsetMapping offsetMapping : this.offsetMappings.values()) {
                            offsetMappings.add(offsetMapping.resolve(instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    argumentHandler,
                                    OffsetMapping.Sort.EXIT));
                        }
                        return new AdviceMethodWriter.ForMethodExit(delegator.getTypeToken(),
                                instrumentedType,
                                instrumentedMethod,
                                assigner,
                                postProcessor,
                                offsetMappings,
                                methodVisitor,
                                implementationContext,
                                argumentHandler,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                suppressionHandler,
                                relocationHandler,
                                exceptionHandler,
                                delegator);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ArgumentHandler.Factory getArgumentHandlerFactory() {
                        return backupArguments
                                ? ArgumentHandler.Factory.COPYING
                                : ArgumentHandler.Factory.SIMPLE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDefinition getAdviceType() {
                        return delegator.getTypeToken().getReturnType();
                    }

                    /**
                     * Implementation of exit advice that handles exceptions.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class WithExceptionHandler extends Delegating.Resolved.ForMethodExit {

                        /**
                         * The type of the handled throwable type for which this advice is invoked.
                         */
                        private final TypeDescription throwable;

                        /**
                         * Creates a new resolved dispatcher for implementing method exit advice that handles exceptions.
                         *
                         * @param adviceMethod  The represented advice method.
                         * @param postProcessor The post processor factory to apply.
                         * @param namedTypes    A mapping of all available local variables by their name to their type.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param enterType     The type of the value supplied by the enter advice method or
                         *                      a description of {@code void} if no such value exists.
                         * @param throwable     The type of the handled throwable type for which this advice is invoked.
                         * @param delegator     The delegator to use.
                         */
                        protected WithExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                       PostProcessor postProcessor,
                                                       Map<String, TypeDefinition> namedTypes,
                                                       List<? extends OffsetMapping.Factory<?>> userFactories,
                                                       TypeDefinition enterType,
                                                       TypeDescription throwable,
                                                       Delegator delegator) {
                            super(adviceMethod, postProcessor, namedTypes, userFactories, enterType, delegator);
                            this.throwable = throwable;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDescription getThrowable() {
                            return throwable;
                        }
                    }

                    /**
                     * Implementation of exit advice that ignores exceptions.
                     */
                    protected static class WithoutExceptionHandler extends Delegating.Resolved.ForMethodExit {

                        /**
                         * Creates a new resolved dispatcher for implementing method exit advice that does not handle exceptions.
                         *
                         * @param adviceMethod  The represented advice method.
                         * @param postProcessor The post processor factory to apply.
                         * @param namedTypes    A mapping of all available local variables by their name to their type.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param enterType     The type of the value supplied by the enter advice method or
                         *                      a description of {@code void} if no such value exists.
                         * @param delegator     The delegator to use.
                         */
                        protected WithoutExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                          PostProcessor postProcessor,
                                                          Map<String, TypeDefinition> namedTypes,
                                                          List<? extends OffsetMapping.Factory<?>> userFactories,
                                                          TypeDefinition enterType,
                                                          Delegator delegator) {
                            super(adviceMethod, postProcessor, namedTypes, userFactories, enterType, delegator);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDescription getThrowable() {
                            return NoExceptionHandler.DESCRIPTION;
                        }
                    }
                }
            }
        }
    }

    /**
     * A method visitor that weaves the advice methods' byte codes.
     */
    protected abstract static class AdviceVisitor extends ExceptionTableSensitiveMethodVisitor implements Dispatcher.RelocationHandler.Relocation {

        /**
         * The expected index for the {@code this} variable.
         */
        private static final int THIS_VARIABLE_INDEX = 0;

        /**
         * The expected name for the {@code this} variable.
         */
        private static final String THIS_VARIABLE_NAME = "this";

        /**
         * A description of the instrumented method.
         */
        protected final MethodDescription instrumentedMethod;

        /**
         * A label that indicates the start of the preparation of a user method execution.
         */
        private final Label preparationStart;

        /**
         * The dispatcher to be used for method enter.
         */
        private final Dispatcher.Bound methodEnter;

        /**
         * The dispatcher to be used for method exit.
         */
        protected final Dispatcher.Bound methodExit;

        /**
         * The handler for accessing arguments of the method's local variable array.
         */
        protected final ArgumentHandler.ForInstrumentedMethod argumentHandler;

        /**
         * A handler for computing the method size requirements.
         */
        protected final MethodSizeHandler.ForInstrumentedMethod methodSizeHandler;

        /**
         * A handler for translating and injecting stack map frames.
         */
        protected final StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler;

        /**
         * Creates a new advice visitor.
         *
         * @param methodVisitor         The actual method visitor that is underlying this method visitor to which all instructions are written.
         * @param implementationContext The implementation context to use.
         * @param assigner              The assigner to use.
         * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
         * @param instrumentedType      A description of the instrumented type.
         * @param instrumentedMethod    The instrumented method.
         * @param methodEnter           The method enter advice.
         * @param methodExit            The method exit advice.
         * @param postMethodTypes       A list of virtual method arguments that are available after the instrumented method has completed.
         * @param writerFlags           The ASM writer flags that were set.
         * @param readerFlags           The ASM reader flags that were set.
         */
        @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", justification = "Self reference is not used before constructor completion.")
        protected AdviceVisitor(MethodVisitor methodVisitor,
                                Context implementationContext,
                                Assigner assigner,
                                StackManipulation exceptionHandler,
                                TypeDescription instrumentedType,
                                MethodDescription instrumentedMethod,
                                Dispatcher.Resolved.ForMethodEnter methodEnter,
                                Dispatcher.Resolved.ForMethodExit methodExit,
                                List<? extends TypeDescription> postMethodTypes,
                                int writerFlags,
                                int readerFlags) {
            super(OpenedClassReader.ASM_API, methodVisitor);
            this.instrumentedMethod = instrumentedMethod;
            preparationStart = new Label();
            SortedMap<String, TypeDefinition> namedTypes = new TreeMap<String, TypeDefinition>();
            namedTypes.putAll(methodEnter.getNamedTypes());
            namedTypes.putAll(methodExit.getNamedTypes());
            argumentHandler = methodExit.getArgumentHandlerFactory().resolve(instrumentedMethod,
                    methodEnter.getAdviceType(),
                    methodExit.getAdviceType(),
                    namedTypes);
            List<TypeDescription> initialTypes = CompoundList.of(methodExit.getAdviceType().represents(void.class)
                    ? Collections.<TypeDescription>emptyList()
                    : Collections.singletonList(methodExit.getAdviceType().asErasure()), argumentHandler.getNamedTypes());
            List<TypeDescription> latentTypes = methodEnter.getActualAdviceType().represents(void.class)
                    ? Collections.<TypeDescription>emptyList()
                    : Collections.singletonList(methodEnter.getActualAdviceType().asErasure());
            List<TypeDescription> preMethodTypes = methodEnter.getAdviceType().represents(void.class)
                    ? Collections.<TypeDescription>emptyList()
                    : Collections.singletonList(methodEnter.getAdviceType().asErasure());
            methodSizeHandler = MethodSizeHandler.Default.of(instrumentedMethod,
                    initialTypes,
                    preMethodTypes,
                    postMethodTypes,
                    argumentHandler.isCopyingArguments(),
                    writerFlags);
            stackMapFrameHandler = StackMapFrameHandler.Default.of(instrumentedType,
                    instrumentedMethod,
                    initialTypes,
                    latentTypes,
                    preMethodTypes,
                    postMethodTypes,
                    methodExit.isAlive(),
                    argumentHandler.isCopyingArguments(),
                    implementationContext.getClassFileVersion(),
                    writerFlags,
                    readerFlags);
            this.methodEnter = methodEnter.bind(instrumentedType,
                    instrumentedMethod,
                    methodVisitor,
                    implementationContext,
                    assigner,
                    argumentHandler,
                    methodSizeHandler,
                    stackMapFrameHandler,
                    exceptionHandler,
                    this);
            this.methodExit = methodExit.bind(instrumentedType,
                    instrumentedMethod,
                    methodVisitor,
                    implementationContext,
                    assigner,
                    argumentHandler,
                    methodSizeHandler,
                    stackMapFrameHandler,
                    exceptionHandler,
                    new ForLabel(preparationStart));
        }

        @Override
        protected void onAfterExceptionTable() {
            methodEnter.prepare();
            onUserPrepare();
            methodExit.prepare();
            methodEnter.initialize();
            methodExit.initialize();
            stackMapFrameHandler.injectInitializationFrame(mv);
            methodEnter.apply();
            mv.visitLabel(preparationStart);
            methodSizeHandler.requireStackSize(argumentHandler.prepare(mv));
            stackMapFrameHandler.injectStartFrame(mv);
            mv.visitInsn(Opcodes.NOP);
            onUserStart();
        }

        /**
         * Invoked when the user method's exception handler (if any) is supposed to be prepared.
         */
        protected abstract void onUserPrepare();

        /**
         * Writes the advice for entering the instrumented method.
         */
        protected abstract void onUserStart();

        @Override
        protected void onVisitVarInsn(int opcode, int offset) {
            mv.visitVarInsn(opcode, argumentHandler.argument(offset));
        }

        @Override
        protected void onVisitIincInsn(int offset, int increment) {
            mv.visitIincInsn(argumentHandler.argument(offset), increment);
        }

        @Override
        public void onVisitFrame(int type, int localVariableLength, @MaybeNull Object[] localVariable, int stackSize, @MaybeNull Object[] stack) {
            stackMapFrameHandler.translateFrame(mv, type, localVariableLength, localVariable, stackSize, stack);
        }

        @Override
        public void visitMaxs(int stackSize, int localVariableLength) {
            onUserEnd();
            mv.visitMaxs(methodSizeHandler.compoundStackSize(stackSize), methodSizeHandler.compoundLocalVariableLength(localVariableLength));
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int offset) {
            // The 'this' variable is exempt from remapping as it is assumed immutable and remapping it confuses debuggers to not display the variable.
            mv.visitLocalVariable(name, descriptor, signature, start, end, offset == THIS_VARIABLE_INDEX && THIS_VARIABLE_NAME.equals(name)
                    ? offset
                    : argumentHandler.argument(offset));
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeReference,
                                                              TypePath typePath,
                                                              Label[] start,
                                                              Label[] end,
                                                              int[] offset,
                                                              String descriptor,
                                                              boolean visible) {
            int[] translated = new int[offset.length];
            for (int index = 0; index < offset.length; index++) {
                translated[index] = argumentHandler.argument(offset[index]);
            }
            return mv.visitLocalVariableAnnotation(typeReference, typePath, start, end, translated, descriptor, visible);
        }

        /**
         * Writes the advice for completing the instrumented method.
         */
        protected abstract void onUserEnd();

        /**
         * An advice visitor that does not apply exit advice.
         */
        protected static class WithoutExitAdvice extends AdviceVisitor {

            /**
             * Creates an advice visitor that does not apply exit advice.
             *
             * @param methodVisitor         The method visitor for the instrumented method.
             * @param implementationContext The implementation context to use.
             * @param assigner              The assigner to use.
             * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
             * @param instrumentedType      A description of the instrumented type.
             * @param instrumentedMethod    A description of the instrumented method.
             * @param methodEnter           The dispatcher to be used for method enter.
             * @param writerFlags           The ASM writer flags that were set.
             * @param readerFlags           The ASM reader flags that were set.
             */
            protected WithoutExitAdvice(MethodVisitor methodVisitor,
                                        Implementation.Context implementationContext,
                                        Assigner assigner,
                                        StackManipulation exceptionHandler,
                                        TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Dispatcher.Resolved.ForMethodEnter methodEnter,
                                        int writerFlags,
                                        int readerFlags) {
                super(methodVisitor,
                        implementationContext,
                        assigner,
                        exceptionHandler,
                        instrumentedType,
                        instrumentedMethod,
                        methodEnter,
                        Dispatcher.Inactive.INSTANCE,
                        Collections.<TypeDescription>emptyList(),
                        writerFlags,
                        readerFlags);
            }

            /**
             * {@inheritDoc}
             */
            public void apply(MethodVisitor methodVisitor) {
                if (instrumentedMethod.getReturnType().represents(boolean.class)
                        || instrumentedMethod.getReturnType().represents(byte.class)
                        || instrumentedMethod.getReturnType().represents(short.class)
                        || instrumentedMethod.getReturnType().represents(char.class)
                        || instrumentedMethod.getReturnType().represents(int.class)) {
                    methodVisitor.visitInsn(Opcodes.ICONST_0);
                    methodVisitor.visitInsn(Opcodes.IRETURN);
                } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                    methodVisitor.visitInsn(Opcodes.LCONST_0);
                    methodVisitor.visitInsn(Opcodes.LRETURN);
                } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                    methodVisitor.visitInsn(Opcodes.FCONST_0);
                    methodVisitor.visitInsn(Opcodes.FRETURN);
                } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                    methodVisitor.visitInsn(Opcodes.DCONST_0);
                    methodVisitor.visitInsn(Opcodes.DRETURN);
                } else if (instrumentedMethod.getReturnType().represents(void.class)) {
                    methodVisitor.visitInsn(Opcodes.RETURN);
                } else {
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                    methodVisitor.visitInsn(Opcodes.ARETURN);
                }
            }

            @Override
            protected void onUserPrepare() {
                /* do nothing */
            }

            @Override
            protected void onUserStart() {
                /* do nothing */
            }

            @Override
            protected void onUserEnd() {
                /* do nothing */
            }
        }

        /**
         * An advice visitor that applies exit advice.
         */
        protected abstract static class WithExitAdvice extends AdviceVisitor {

            /**
             * Indicates the handler for the value returned by the advice method.
             */
            protected final Label returnHandler;

            /**
             * Creates an advice visitor that applies exit advice.
             *
             * @param methodVisitor         The method visitor for the instrumented method.
             * @param implementationContext The implementation context to use.
             * @param assigner              The assigner to use.
             * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
             * @param instrumentedType      A description of the instrumented type.
             * @param instrumentedMethod    A description of the instrumented method.
             * @param methodEnter           The dispatcher to be used for method enter.
             * @param methodExit            The dispatcher to be used for method exit.
             * @param postMethodTypes       A list of virtual method arguments that are available after the instrumented method has completed.
             * @param writerFlags           The ASM writer flags that were set.
             * @param readerFlags           The ASM reader flags that were set.
             */
            protected WithExitAdvice(MethodVisitor methodVisitor,
                                     Implementation.Context implementationContext,
                                     Assigner assigner,
                                     StackManipulation exceptionHandler,
                                     TypeDescription instrumentedType,
                                     MethodDescription instrumentedMethod,
                                     Dispatcher.Resolved.ForMethodEnter methodEnter,
                                     Dispatcher.Resolved.ForMethodExit methodExit,
                                     List<? extends TypeDescription> postMethodTypes,
                                     int writerFlags,
                                     int readerFlags) {
                super(StackAwareMethodVisitor.of(methodVisitor, instrumentedMethod),
                        implementationContext,
                        assigner,
                        exceptionHandler,
                        instrumentedType,
                        instrumentedMethod,
                        methodEnter,
                        methodExit,
                        postMethodTypes,
                        writerFlags,
                        readerFlags);
                returnHandler = new Label();
            }

            /**
             * {@inheritDoc}
             */
            public void apply(MethodVisitor methodVisitor) {
                if (instrumentedMethod.getReturnType().represents(boolean.class)
                        || instrumentedMethod.getReturnType().represents(byte.class)
                        || instrumentedMethod.getReturnType().represents(short.class)
                        || instrumentedMethod.getReturnType().represents(char.class)
                        || instrumentedMethod.getReturnType().represents(int.class)) {
                    methodVisitor.visitInsn(Opcodes.ICONST_0);
                } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                    methodVisitor.visitInsn(Opcodes.LCONST_0);
                } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                    methodVisitor.visitInsn(Opcodes.FCONST_0);
                } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                    methodVisitor.visitInsn(Opcodes.DCONST_0);
                } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                }
                methodVisitor.visitJumpInsn(Opcodes.GOTO, returnHandler);
            }

            @Override
            protected void onVisitInsn(int opcode) {
                switch (opcode) {
                    case Opcodes.RETURN:
                        ((StackAwareMethodVisitor) mv).drainStack();
                        break;
                    case Opcodes.IRETURN:
                        methodSizeHandler.requireLocalVariableLength(((StackAwareMethodVisitor) mv).drainStack(Opcodes.ISTORE, Opcodes.ILOAD, StackSize.SINGLE));
                        break;
                    case Opcodes.FRETURN:
                        methodSizeHandler.requireLocalVariableLength(((StackAwareMethodVisitor) mv).drainStack(Opcodes.FSTORE, Opcodes.FLOAD, StackSize.SINGLE));
                        break;
                    case Opcodes.DRETURN:
                        methodSizeHandler.requireLocalVariableLength(((StackAwareMethodVisitor) mv).drainStack(Opcodes.DSTORE, Opcodes.DLOAD, StackSize.DOUBLE));
                        break;
                    case Opcodes.LRETURN:
                        methodSizeHandler.requireLocalVariableLength((((StackAwareMethodVisitor) mv).drainStack(Opcodes.LSTORE, Opcodes.LLOAD, StackSize.DOUBLE)));
                        break;
                    case Opcodes.ARETURN:
                        methodSizeHandler.requireLocalVariableLength((((StackAwareMethodVisitor) mv).drainStack(Opcodes.ASTORE, Opcodes.ALOAD, StackSize.SINGLE)));
                        break;
                    default:
                        mv.visitInsn(opcode);
                        return;
                }
                mv.visitJumpInsn(Opcodes.GOTO, returnHandler);
            }

            @Override
            protected void onUserEnd() {
                mv.visitLabel(returnHandler);
                onUserReturn();
                stackMapFrameHandler.injectCompletionFrame(mv);
                methodExit.apply();
                onExitAdviceReturn();
                if (instrumentedMethod.getReturnType().represents(boolean.class)
                        || instrumentedMethod.getReturnType().represents(byte.class)
                        || instrumentedMethod.getReturnType().represents(short.class)
                        || instrumentedMethod.getReturnType().represents(char.class)
                        || instrumentedMethod.getReturnType().represents(int.class)) {
                    mv.visitVarInsn(Opcodes.ILOAD, argumentHandler.returned());
                    mv.visitInsn(Opcodes.IRETURN);
                } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                    mv.visitVarInsn(Opcodes.LLOAD, argumentHandler.returned());
                    mv.visitInsn(Opcodes.LRETURN);
                } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                    mv.visitVarInsn(Opcodes.FLOAD, argumentHandler.returned());
                    mv.visitInsn(Opcodes.FRETURN);
                } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                    mv.visitVarInsn(Opcodes.DLOAD, argumentHandler.returned());
                    mv.visitInsn(Opcodes.DRETURN);
                } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                    mv.visitVarInsn(Opcodes.ALOAD, argumentHandler.returned());
                    mv.visitInsn(Opcodes.ARETURN);
                } else {
                    mv.visitInsn(Opcodes.RETURN);
                }
                methodSizeHandler.requireStackSize(instrumentedMethod.getReturnType().getStackSize().getSize());
            }

            /**
             * Invoked after the user method has returned.
             */
            protected abstract void onUserReturn();

            /**
             * Invoked after the exit advice method has returned.
             */
            protected abstract void onExitAdviceReturn();

            /**
             * An advice visitor that does not capture exceptions.
             */
            protected static class WithoutExceptionHandling extends WithExitAdvice {

                /**
                 * Creates a new advice visitor that does not capture exceptions.
                 *
                 * @param methodVisitor         The method visitor for the instrumented method.
                 * @param implementationContext The implementation context to use.
                 * @param assigner              The assigner to use.
                 * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
                 * @param instrumentedType      A description of the instrumented type.
                 * @param instrumentedMethod    A description of the instrumented method.
                 * @param methodEnter           The dispatcher to be used for method enter.
                 * @param methodExit            The dispatcher to be used for method exit.
                 * @param writerFlags           The ASM writer flags that were set.
                 * @param readerFlags           The ASM reader flags that were set.
                 */
                protected WithoutExceptionHandling(MethodVisitor methodVisitor,
                                                   Implementation.Context implementationContext,
                                                   Assigner assigner,
                                                   StackManipulation exceptionHandler,
                                                   TypeDescription instrumentedType,
                                                   MethodDescription instrumentedMethod,
                                                   Dispatcher.Resolved.ForMethodEnter methodEnter,
                                                   Dispatcher.Resolved.ForMethodExit methodExit,
                                                   int writerFlags,
                                                   int readerFlags) {
                    super(methodVisitor,
                            implementationContext,
                            assigner,
                            exceptionHandler,
                            instrumentedType,
                            instrumentedMethod,
                            methodEnter,
                            methodExit,
                            instrumentedMethod.getReturnType().represents(void.class)
                                    ? Collections.<TypeDescription>emptyList()
                                    : Collections.singletonList(instrumentedMethod.getReturnType().asErasure()),
                            writerFlags,
                            readerFlags);
                }

                @Override
                protected void onUserPrepare() {
                    /* do nothing */
                }

                @Override
                protected void onUserStart() {
                    /* do nothing */
                }

                @Override
                protected void onUserReturn() {
                    if (instrumentedMethod.getReturnType().represents(boolean.class)
                            || instrumentedMethod.getReturnType().represents(byte.class)
                            || instrumentedMethod.getReturnType().represents(short.class)
                            || instrumentedMethod.getReturnType().represents(char.class)
                            || instrumentedMethod.getReturnType().represents(int.class)) {
                        stackMapFrameHandler.injectReturnFrame(mv);
                        mv.visitVarInsn(Opcodes.ISTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                        stackMapFrameHandler.injectReturnFrame(mv);
                        mv.visitVarInsn(Opcodes.LSTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                        stackMapFrameHandler.injectReturnFrame(mv);
                        mv.visitVarInsn(Opcodes.FSTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                        stackMapFrameHandler.injectReturnFrame(mv);
                        mv.visitVarInsn(Opcodes.DSTORE, argumentHandler.returned());
                    } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                        stackMapFrameHandler.injectReturnFrame(mv);
                        mv.visitVarInsn(Opcodes.ASTORE, argumentHandler.returned());
                    }
                }

                @Override
                protected void onExitAdviceReturn() {
                    /* do nothing */
                }
            }

            /**
             * An advice visitor that captures exceptions by weaving try-catch blocks around user code.
             */
            protected static class WithExceptionHandling extends WithExitAdvice {

                /**
                 * The type of the handled throwable type for which this advice is invoked.
                 */
                private final TypeDescription throwable;

                /**
                 * Indicates the exception handler.
                 */
                private final Label exceptionHandler;

                /**
                 * Indicates the start of the user method.
                 */
                protected final Label userStart;

                /**
                 * Creates a new advice visitor that captures exception by weaving try-catch blocks around user code.
                 *
                 * @param methodVisitor         The method visitor for the instrumented method.
                 * @param instrumentedType      A description of the instrumented type.
                 * @param implementationContext The implementation context to use.
                 * @param assigner              The assigner to use.
                 * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
                 * @param instrumentedMethod    A description of the instrumented method.
                 * @param methodEnter           The dispatcher to be used for method enter.
                 * @param methodExit            The dispatcher to be used for method exit.
                 * @param writerFlags           The ASM writer flags that were set.
                 * @param readerFlags           The ASM reader flags that were set.
                 * @param throwable             The type of the handled throwable type for which this advice is invoked.
                 */
                protected WithExceptionHandling(MethodVisitor methodVisitor,
                                                Implementation.Context implementationContext,
                                                Assigner assigner,
                                                StackManipulation exceptionHandler,
                                                TypeDescription instrumentedType,
                                                MethodDescription instrumentedMethod,
                                                Dispatcher.Resolved.ForMethodEnter methodEnter,
                                                Dispatcher.Resolved.ForMethodExit methodExit,
                                                int writerFlags,
                                                int readerFlags,
                                                TypeDescription throwable) {
                    super(methodVisitor,
                            implementationContext,
                            assigner,
                            exceptionHandler,
                            instrumentedType,
                            instrumentedMethod,
                            methodEnter,
                            methodExit,
                            instrumentedMethod.getReturnType().represents(void.class)
                                    ? Collections.singletonList(TypeDescription.ForLoadedType.of(Throwable.class))
                                    : Arrays.asList(instrumentedMethod.getReturnType().asErasure(), TypeDescription.ForLoadedType.of(Throwable.class)),
                            writerFlags,
                            readerFlags);
                    this.throwable = throwable;
                    this.exceptionHandler = new Label();
                    userStart = new Label();
                }

                @Override
                protected void onUserPrepare() {
                    mv.visitTryCatchBlock(userStart, returnHandler, exceptionHandler, throwable.getInternalName());
                }

                @Override
                protected void onUserStart() {
                    mv.visitLabel(userStart);
                }

                @Override
                protected void onUserReturn() {
                    stackMapFrameHandler.injectReturnFrame(mv);
                    if (instrumentedMethod.getReturnType().represents(boolean.class)
                            || instrumentedMethod.getReturnType().represents(byte.class)
                            || instrumentedMethod.getReturnType().represents(short.class)
                            || instrumentedMethod.getReturnType().represents(char.class)
                            || instrumentedMethod.getReturnType().represents(int.class)) {
                        mv.visitVarInsn(Opcodes.ISTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                        mv.visitVarInsn(Opcodes.LSTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                        mv.visitVarInsn(Opcodes.FSTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                        mv.visitVarInsn(Opcodes.DSTORE, argumentHandler.returned());
                    } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                        mv.visitVarInsn(Opcodes.ASTORE, argumentHandler.returned());
                    }
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitVarInsn(Opcodes.ASTORE, argumentHandler.thrown());
                    Label endOfHandler = new Label();
                    mv.visitJumpInsn(Opcodes.GOTO, endOfHandler);
                    mv.visitLabel(exceptionHandler);
                    stackMapFrameHandler.injectExceptionFrame(mv);
                    mv.visitVarInsn(Opcodes.ASTORE, argumentHandler.thrown());
                    if (instrumentedMethod.getReturnType().represents(boolean.class)
                            || instrumentedMethod.getReturnType().represents(byte.class)
                            || instrumentedMethod.getReturnType().represents(short.class)
                            || instrumentedMethod.getReturnType().represents(char.class)
                            || instrumentedMethod.getReturnType().represents(int.class)) {
                        mv.visitInsn(Opcodes.ICONST_0);
                        mv.visitVarInsn(Opcodes.ISTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                        mv.visitInsn(Opcodes.LCONST_0);
                        mv.visitVarInsn(Opcodes.LSTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                        mv.visitInsn(Opcodes.FCONST_0);
                        mv.visitVarInsn(Opcodes.FSTORE, argumentHandler.returned());
                    } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                        mv.visitInsn(Opcodes.DCONST_0);
                        mv.visitVarInsn(Opcodes.DSTORE, argumentHandler.returned());
                    } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                        mv.visitInsn(Opcodes.ACONST_NULL);
                        mv.visitVarInsn(Opcodes.ASTORE, argumentHandler.returned());
                    }
                    mv.visitLabel(endOfHandler);
                    methodSizeHandler.requireStackSize(StackSize.SINGLE.getSize());
                }

                @Override
                protected void onExitAdviceReturn() {
                    mv.visitVarInsn(Opcodes.ALOAD, argumentHandler.thrown());
                    Label endOfHandler = new Label();
                    mv.visitJumpInsn(Opcodes.IFNULL, endOfHandler);
                    mv.visitVarInsn(Opcodes.ALOAD, argumentHandler.thrown());
                    mv.visitInsn(Opcodes.ATHROW);
                    mv.visitLabel(endOfHandler);
                    stackMapFrameHandler.injectPostCompletionFrame(mv);
                }
            }
        }
    }

    /**
     * A byte code appender for implementing {@link Advice}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Appender implements ByteCodeAppender {

        /**
         * The advice to implement.
         */
        private final Advice advice;

        /**
         * The current implementation target.
         */
        private final Implementation.Target implementationTarget;

        /**
         * The delegate byte code appender.
         */
        private final ByteCodeAppender delegate;

        /**
         * Creates a new appender for an advice component.
         *
         * @param advice               The advice to implement.
         * @param implementationTarget The current implementation target.
         * @param delegate             The delegate byte code appender.
         */
        protected Appender(Advice advice, Target implementationTarget, ByteCodeAppender delegate) {
            this.advice = advice;
            this.implementationTarget = implementationTarget;
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            EmulatingMethodVisitor emulatingMethodVisitor = new EmulatingMethodVisitor(methodVisitor, delegate);
            methodVisitor = advice.doWrap(implementationTarget.getInstrumentedType(),
                    instrumentedMethod,
                    emulatingMethodVisitor,
                    implementationContext,
                    AsmVisitorWrapper.NO_FLAGS,
                    AsmVisitorWrapper.NO_FLAGS);
            return emulatingMethodVisitor.resolve(methodVisitor, implementationContext, instrumentedMethod);
        }

        /**
         * A method visitor that allows for the emulation of a full method visitor invocation circle without delegating initial
         * and ending visitations to the underlying visitor.
         */
        protected static class EmulatingMethodVisitor extends MethodVisitor {

            /**
             * The delegate byte code appender.
             */
            private final ByteCodeAppender delegate;

            /**
             * The currently recorded minimal required stack size.
             */
            private int stackSize;

            /**
             * The currently recorded minimal required local variable array length.
             */
            private int localVariableLength;

            /**
             * Creates a new emulating method visitor.
             *
             * @param methodVisitor The underlying method visitor.
             * @param delegate      The delegate byte code appender.
             */
            protected EmulatingMethodVisitor(MethodVisitor methodVisitor, ByteCodeAppender delegate) {
                super(OpenedClassReader.ASM_API, methodVisitor);
                this.delegate = delegate;
            }

            /**
             * Resolves this this advice emulating method visitor for its delegate.
             *
             * @param methodVisitor         The method visitor to apply.
             * @param implementationContext The implementation context to apply.
             * @param instrumentedMethod    The instrumented method.
             * @return The resulting size of the implemented method.
             */
            protected ByteCodeAppender.Size resolve(MethodVisitor methodVisitor,
                                                    Implementation.Context implementationContext,
                                                    MethodDescription instrumentedMethod) {
                methodVisitor.visitCode();
                Size size = delegate.apply(methodVisitor, implementationContext, instrumentedMethod);
                methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                methodVisitor.visitEnd();
                return new ByteCodeAppender.Size(stackSize, localVariableLength);
            }

            @Override
            public void visitCode() {
                /* do nothing */
            }

            @Override
            public void visitMaxs(int stackSize, int localVariableLength) {
                this.stackSize = stackSize;
                this.localVariableLength = localVariableLength;
            }

            @Override
            public void visitEnd() {
                /* do nothing */
            }
        }
    }

    /**
     * <p>
     * Indicates that this method should be inlined before the matched method is invoked. Any class must declare
     * at most one method with this annotation. The annotated method must be static. When instrumenting constructors,
     * the {@code this} values can only be accessed for writing fields but not for reading fields or invoking methods.
     * </p>
     * <p>
     * The annotated method can return a value that is made accessible to another method annotated by {@link OnMethodExit}.
     * </p>
     *
     * @see Advice
     * @see Argument
     * @see This
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.METHOD)
    public @interface OnMethodEnter {

        /**
         * <p>
         * Determines if the execution of the instrumented method should be skipped. This does not include any exit advice.
         * </p>
         * <p>
         * When specifying a non-primitive type, this method's return value that is subject to an {@code instanceof} check where
         * the instrumented method is only executed, if the returned instance is {@code not} an instance of the specified class.
         * Alternatively, it is possible to specify either {@link OnDefaultValue} or {@link OnNonDefaultValue} where the instrumented
         * method is only executed if the advice method returns a default or non-default value of the advice method's return type.
         * It is illegal to specify a primitive type as an argument whereas setting the value to {@code void} indicates that the
         * instrumented method should never be skipped.
         * </p>
         * <p>
         * <b>Important</b>: Constructors cannot be skipped.
         * </p>
         *
         * @return A value defining what return values of the advice method indicate that the instrumented method
         * should be skipped or {@code void} if the instrumented method should never be skipped.
         */
        Class<?> skipOn() default void.class;

        /**
         * Indicates that the value to be used for {@link OnMethodEnter#skipOn()} should be read from an array that is returned by
         * the advice method, if the assigned property is non-negative. If the returned array is shorter than the supplied index,
         * an {@link ArrayIndexOutOfBoundsException} will be thrown, even if suppression is used.
         *
         * @return The array index for the value to be considered for skipping a method, or a negative value for using the returned value.
         */
        int skipOnIndex() default -1;

        /**
         * If set to {@code true}, the instrumented method's line number information is adjusted such that stack traces generated within
         * this advice method appear as if they were generated within the first line of the instrumented method. If set to {@code false},
         * no line number information is made available for such stack traces.
         *
         * @return {@code true} if this advice code should appear as if it was added within the first line of the instrumented method.
         */
        boolean prependLineNumber() default true;

        /**
         * Determines if the annotated method should be inlined into the instrumented method or invoked from it. When a method
         * is inlined, its byte code is copied into the body of the target method. this makes it is possible to execute code
         * with the visibility privileges of the instrumented method while loosing the privileges of the declared method methods.
         * When a method is not inlined, it is invoked similarly to a common Java method call. Note that it is not possible to
         * set breakpoints within a method when it is inlined as no debugging information is copied from the advice method into
         * the instrumented method.
         *
         * @return {@code true} if the annotated method should be inlined into the instrumented method.
         */
        boolean inline() default true;

        /**
         * Indicates that this advice should suppress any {@link Throwable} type being thrown during the advice's execution. By default,
         * any such exception is silently suppressed. Custom behavior can be configured by using {@link Advice#withExceptionHandler(StackManipulation)}.
         *
         * @return The type of {@link Throwable} to suppress.
         * @see Advice#withExceptionPrinting()
         */
        Class<? extends Throwable> suppress() default NoExceptionHandler.class;
    }

    /**
     * <p>
     * Indicates that this method should be executed before exiting the instrumented method. Any class must declare
     * at most one method with this annotation. The annotated method must be static.
     * </p>
     * <p>
     * By default, the annotated method is not invoked if the instrumented method terminates exceptionally. This behavior
     * can be changed by setting the {@link OnMethodExit#onThrowable()} property to an exception type for which this advice
     * method should be invoked. By setting the value to {@link Throwable}, the advice method is always invoked.
     * </p>
     *
     * @see Advice
     * @see Argument
     * @see This
     * @see Enter
     * @see Return
     * @see Thrown
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.METHOD)
    public @interface OnMethodExit {

        /**
         * <p>
         * Determines if the execution of the instrumented method should be repeated. This does not include any enter advice.
         * </p>
         * <p>
         * When specifying a non-primitive type, this method's return value that is subject to an {@code instanceof} check where
         * the instrumented method is only executed, if the returned instance is {@code not} an instance of the specified class.
         * Alternatively, it is possible to specify either {@link OnDefaultValue} or {@link OnNonDefaultValue} where the instrumented
         * method is only repeated if the advice method returns a default or non-default value of the advice method's return type.
         * It is illegal to specify a primitive type as an argument whereas setting the value to {@code void} indicates that the
         * instrumented method should never be repeated.
         * </p>
         * <p>
         * <b>Important</b>: Constructors cannot be repeated.
         * </p>
         *
         * @return A value defining what return values of the advice method indicate that the instrumented method
         * should be repeated or {@code void} if the instrumented method should never be repeated.
         */
        Class<?> repeatOn() default void.class;

        /**
         * Indicates that the value to be used for {@link OnMethodExit#repeatOn()} should be read from an array that is returned by
         * the advice method, if the assigned property is non-negative. If the returned array is shorter than the supplied index,
         * an {@link ArrayIndexOutOfBoundsException} will be thrown, even if suppression is used.
         *
         * @return The array index for the value to be considered for repeating a method, or a negative value for using the returned value.
         */
        int repeatOnIndex() default -1;

        /**
         * Indicates a {@link Throwable} super type for which this exit advice is invoked if it was thrown from the instrumented method.
         * If an exception is thrown, it is available via the {@link Thrown} parameter annotation. If a method returns exceptionally,
         * any parameter annotated with {@link Return} is assigned the parameter type's default value.
         *
         * @return The type of {@link Throwable} for which this exit advice handler is invoked.
         */
        Class<? extends Throwable> onThrowable() default NoExceptionHandler.class;

        /**
         * <p>
         * If {@code true}, all arguments of the instrumented method are copied before execution. Doing so, parameter reassignments applied
         * by the instrumented are not effective during the execution of the annotated exit advice.
         * </p>
         * <p>
         * Disabling this option can cause problems with the translation of stack map frames (meta data that is embedded in a Java class) if these
         * frames become inconsistent with the original arguments of the instrumented method. In this case, the original arguments are no longer
         * available to the exit advice such that Byte Buddy must abort the instrumentation with an error. If the instrumented method does not issue
         * a stack map frame due to a lack of branching instructions, Byte Buddy might not be able to discover such an inconsistency what can cause
         * a {@link VerifyError} instead of a Byte Buddy-issued exception as those inconsistencies are not discovered.
         * </p>
         *
         * @return {@code true} if a backup of all method arguments should be made.
         */
        boolean backupArguments() default true;

        /**
         * Determines if the annotated method should be inlined into the instrumented method or invoked from it. When a method
         * is inlined, its byte code is copied into the body of the target method. this makes it is possible to execute code
         * with the visibility privileges of the instrumented method while loosing the privileges of the declared method methods.
         * When a method is not inlined, it is invoked similarly to a common Java method call. Note that it is not possible to
         * set breakpoints within a method when it is inlined as no debugging information is copied from the advice method into
         * the instrumented method.
         *
         * @return {@code true} if the annotated method should be inlined into the instrumented method.
         */
        boolean inline() default true;

        /**
         * Indicates that this advice should suppress any {@link Throwable} type being thrown during the advice's execution. By default,
         * any such exception is silently suppressed. Custom behavior can be configured by using {@link Advice#withExceptionHandler(StackManipulation)}.
         *
         * @return The type of {@link Throwable} to suppress.
         * @see Advice#withExceptionPrinting()
         */
        Class<? extends Throwable> suppress() default NoExceptionHandler.class;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the {@code this} reference of the instrumented method.
     * </p>
     * <p>
     * <b>Important</b>: Parameters with this option must not be used when from a constructor in combination with
     * {@link OnMethodEnter} where the {@code this} reference is not available.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.This} or
     * {@link net.bytebuddy.asm.MemberSubstitution.This}. This annotation should be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface This {

        /**
         * <p>
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
         * </p>
         * <p>
         * <b>Important</b>: This property must be set to {@code true} if the advice method is not inlined.
         * </p>
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;

        /**
         * The typing that should be applied when assigning the {@code this} value.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;

        /**
         * Determines if the parameter should be assigned {@code null} if the instrumented method is static or a constructor within an enter advice.
         *
         * @return {@code true} if the value assignment is optional.
         */
        boolean optional() default false;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the parameter with index {@link Argument#value()} of
     * the instrumented method.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.Argument} or
     * {@link net.bytebuddy.asm.MemberSubstitution.Argument}. This annotation should be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Argument {

        /**
         * Returns the index of the mapped parameter.
         *
         * @return The index of the mapped parameter.
         */
        int value();

        /**
         * <p>
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
         * </p>
         * <p>
         * <b>Important</b>: This property must be set to {@code true} if the advice method is not inlined.
         * </p>
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;

        /**
         * The typing that should be applied when assigning the argument.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;

        /**
         * Indicates if a parameter binding is optional. If a binding is optional and a parameter with the specified index does not exist,
         * the parameter's default value is bound.
         *
         * @return {@code true} if the binding is optional.
         */
        boolean optional() default false;
    }

    /**
     * <p>
     * Assigns an array containing all arguments of the instrumented method to the annotated parameter. The annotated parameter must
     * be an array type. If the annotation indicates writability, the assigned array must have at least as many values as the
     * instrumented method or an {@link ArrayIndexOutOfBoundsException} is thrown.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.AllArguments} or
     * {@link net.bytebuddy.asm.MemberSubstitution.AllArguments}. This annotation should be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface AllArguments {

        /**
         * <p>
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
         * </p>
         * <p>
         * <b>Important</b>: This property must be set to {@code true} if the advice method is not inlined.
         * </p>
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;

        /**
         * The typing that should be applied when assigning the arguments.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;

        /**
         * Determines if the produced array should include the instrumented method's target reference within the array, if
         * the instrumented method is non-static.
         *
         * @return {@code true} if the method's target instance should be considered an element of the produced array.
         */
        boolean includeSelf() default false;

        /**
         * Determines if a {@code null} value should be assigned if the instrumented method does not declare any parameters.
         * If this option is set and a value is written to annotated parameter, the operation remains without effect.
         *
         * @return {@code true} if a {@code null} value should be assigned if the instrumented method does not declare any parameters.
         */
        boolean nullIfEmpty() default false;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the return value of the instrumented method. If the instrumented
     * method terminates exceptionally, the type's default value is assigned to the parameter, i.e. {@code 0} for numeric types
     * and {@code null} for reference types. If the return type is {@code void}, the annotated value is {@code null} if and only if
     * {@link Return#typing()} is set to {@link Assigner.Typing#DYNAMIC}.
     * </p>
     * <p>
     * <b>Note</b>: This annotation must only be used on {@link OnMethodExit} advice methods.
     * </p>
     *
     * @see Advice
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Return {

        /**
         * <p>
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
         * </p>
         * <p>
         * <b>Important</b>: This property must be set to {@code true} if the advice method is not inlined.
         * </p>
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;

        /**
         * Determines the typing that is applied when assigning the return value.
         *
         * @return The typing to apply when assigning the annotated parameter.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the {@link Throwable} thrown by the instrumented method or to {@code null}
     * if the method returned regularly. Note that the Java runtime does not enforce checked exceptions. In order to capture any error, the parameter
     * type must therefore be of type {@link Throwable}. By assigning another value or {@code null} to this parameter, a thrown exception can be
     * suppressed.
     * </p>
     * <p>
     * <b>Note</b>: This annotation must only be used on {@link OnMethodExit} advice methods.
     * </p>
     *
     * @see Advice
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Thrown {

        /**
         * <p>
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
         * </p>
         * <p>
         * <b>Important</b>: This property must be set to {@code true} if the advice method is not inlined.
         * </p>
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;

        /**
         * Determines the typing that is applied when assigning the captured {@link Throwable} to the annotated parameter.
         *
         * @return The typing to apply when assigning the annotated parameter.
         */
        Assigner.Typing typing() default Assigner.Typing.DYNAMIC;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should load a {@code java.lang.invoke.MethodHandle} that represents an invocation of
     * the current method. If the current method is virtual, it is bound to the current instance such that the virtual hierarchy
     * is avoided. This annotation can only be used on methods, not constructors.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with
     * {@link net.bytebuddy.asm.MemberSubstitution.SelfCallHandle}. This annotation should
     * be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface SelfCallHandle {

        /**
         * Determines if the method is bound to the arguments and instance of the current invocation.
         *
         * @return {@code true} if the handle should be bound to the current arguments.
         */
        boolean bound() default true;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a field in the scope of the instrumented type.
     * </p>
     * <p>
     * Setting {@link FieldValue#value()} is optional. If the value is not set, the field value attempts to bind a setter's
     * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
     * to be an illegal candidate for binding.
     * </p>
     * <p>
     * <b>Important</b>: Parameters with this option must not be used when from a constructor in combination with
     * {@link OnMethodEnter} and a non-static field where the {@code this} reference is not available.
     * </p>
     * <p>
     * <b>Note</b>: As the mapping is virtual, Byte Buddy might be required to reserve more space on the operand stack than the
     * optimal value when accessing this parameter. This does not normally matter as the additional space requirement is minimal.
     * However, if the runtime performance of class creation is secondary, one can require ASM to recompute the optimal frames by
     * setting {@link ClassWriter#COMPUTE_MAXS}. This is however only relevant when writing to a non-static field.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.FieldValue} or
     * {@link net.bytebuddy.asm.MemberSubstitution.FieldValue}. This annotation should
     * be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface FieldValue {

        /**
         * Returns the name of the field.
         *
         * @return The name of the field.
         */
        String value() default OffsetMapping.ForField.Unresolved.BEAN_PROPERTY;

        /**
         * Returns the type that declares the field that should be mapped to the annotated parameter. If this property
         * is set to {@code void}, the field is looked up implicitly within the instrumented class's class hierarchy.
         * The value can also be set to {@link TargetType} in order to look up the type on the instrumented type.
         *
         * @return The type that declares the field, {@code void} if this type should be determined implicitly or
         * {@link TargetType} for the instrumented type.
         */
        Class<?> declaringType() default void.class;

        /**
         * <p>
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
         * </p>
         * <p>
         * <b>Important</b>: This property must be set to {@code true} if the advice method is not inlined.
         * </p>
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;

        /**
         * The typing that should be applied when assigning the field value.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a {@code java.lang.invoke.MethodHandle} representing a field getter.
     * </p>
     * <p>
     * Setting {@link FieldValue#value()} is optional. If the value is not set, the field value attempts to bind a setter's
     * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
     * to be an illegal candidate for binding.
     * </p>
     * <p>
     * <b>Important</b>: Parameters with this option must not be used when from a constructor in combination with
     * {@link OnMethodEnter} and a non-static field where the {@code this} reference is not available.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.FieldGetterHandle} or
     * {@link net.bytebuddy.asm.MemberSubstitution.FieldGetterHandle}. This annotation should
     * be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface FieldGetterHandle {

        /**
         * Returns the name of the field.
         *
         * @return The name of the field.
         */
        String value() default OffsetMapping.ForFieldHandle.Unresolved.BEAN_PROPERTY;

        /**
         * Returns the type that declares the field that should be mapped to the annotated parameter. If this property
         * is set to {@code void}, the field is looked up implicitly within the instrumented class's class hierarchy.
         * The value can also be set to {@link TargetType} in order to look up the type on the instrumented type.
         *
         * @return The type that declares the field, {@code void} if this type should be determined implicitly or
         * {@link TargetType} for the instrumented type.
         */
        Class<?> declaringType() default void.class;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a {@code java.lang.invoke.MethodHandle} representing a field setter.
     * </p>
     * <p>
     * Setting {@link FieldValue#value()} is optional. If the value is not set, the field value attempts to bind a setter's
     * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
     * to be an illegal candidate for binding.
     * </p>
     * <p>
     * <b>Important</b>: Parameters with this option must not be used when from a constructor in combination with
     * {@link OnMethodEnter} and a non-static field where the {@code this} reference is not available.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.FieldSetterHandle} or
     * {@link net.bytebuddy.asm.MemberSubstitution.FieldSetterHandle}. This annotation should
     * be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface FieldSetterHandle {

        /**
         * Returns the name of the field.
         *
         * @return The name of the field.
         */
        String value() default OffsetMapping.ForFieldHandle.Unresolved.BEAN_PROPERTY;

        /**
         * Returns the type that declares the field that should be mapped to the annotated parameter. If this property
         * is set to {@code void}, the field is looked up implicitly within the instrumented class's class hierarchy.
         * The value can also be set to {@link TargetType} in order to look up the type on the instrumented type.
         *
         * @return The type that declares the field, {@code void} if this type should be determined implicitly or
         * {@link TargetType} for the instrumented type.
         */
        Class<?> declaringType() default void.class;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a string representation of the instrumented method,
     * a constant representing the {@link Class} declaring the adviced method or a {@link Method}, {@link Constructor}
     * or {@code java.lang.reflect.Executable} representing this method. It can also load the instrumented method's
     * {@code java.lang.invoke.MethodType}, {@code java.lang.invoke.MethodHandle} or
     * {@code java.lang.invoke.MethodHandles$Lookup}.
     * </p>
     * <p>
     * <b>Note</b>: A constant representing a {@link Method} or {@link Constructor} is not cached but is recreated for
     * every read.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.Origin} or
     * {@link net.bytebuddy.asm.MemberSubstitution.Origin}. This annotation should
     * be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Origin {

        /**
         * Indicates that the origin string should be indicated by the {@link Object#toString()} representation of the instrumented method.
         */
        String DEFAULT = "";

        /**
         * Returns the pattern the annotated parameter should be assigned. By default, the {@link Origin#toString()} representation
         * of the method is assigned. Alternatively, a pattern can be assigned where:
         * <ul>
         * <li>{@code #t} inserts the method's declaring type.</li>
         * <li>{@code #m} inserts the name of the method ({@code <init>} for constructors and {@code <clinit>} for static initializers).</li>
         * <li>{@code #d} for the method's descriptor.</li>
         * <li>{@code #s} for the method's signature.</li>
         * <li>{@code #r} for the method's return type.</li>
         * <li>{@code #p} for the property's name.</li>
         * </ul>
         * Any other {@code #} character must be escaped by {@code \} which can be escaped by itself. This property is ignored if the annotated
         * parameter is of type {@link Class}.
         *
         * @return The pattern the annotated parameter should be assigned.
         */
        String value() default DEFAULT;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the value that is returned by the advice method that is annotated
     * by {@link OnMethodEnter}.
     * </p>
     * <p><b>Note</b></p>: This annotation must only be used within an {@link OnMethodExit} advice and is only meaningful in
     * combination with an {@link OnMethodEnter} advice.
     *
     * @see Advice
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Enter {

        /**
         * <p>
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
         * </p>
         * <p>
         * <b>Important</b>: This property must be set to {@code true} if the advice method is not inlined.
         * </p>
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;

        /**
         * The typing that should be applied when assigning the enter value.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;
    }

    /**
     * Indicates that the annotated parameter should be mapped to the value that is returned by the advice method that is annotated
     * by {@link OnMethodExit}. Before the exit advice returns for the first time, this parameter is initialized to its type's default value.
     *
     * @see Advice
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Exit {

        /**
         * <p>
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
         * </p>
         * <p>
         * <b>Important</b>: This property must be set to {@code true} if the advice method is not inlined.
         * </p>
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;

        /**
         * The typing that should be applied when assigning the exit value.
         *
         * @return The typing to apply upon assignment.
         */
        Assigner.Typing typing() default Assigner.Typing.STATIC;
    }

    /**
     * Declares the annotated parameter as a local variable that is created by Byte Buddy for the instrumented method. The local variable can
     * be both read and written by advice methods with {@link OnMethodEnter} and {@link OnMethodExit} annotation and are uniquely identified by
     * their name. However, if a local variable is referenced from an exit advice method, it must also be declared by an enter advice method.
     * It is possible to annotate multiple parameters of an advice method with local variables of the same name as long as all annotated parameters
     * share the same parameter type. All local variables are initialized with their default value which is {@code 0} value for primitive types and
     * {@code null} for reference types.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Local {

        /**
         * The name of the local variable that the annotated parameter references.
         *
         * @return The name of the local variable that the annotated parameter references.
         */
        String value();
    }

    /**
     * <p>
     * Indicates that the annotated parameter should always return a boxed version of the instrumented method's return value
     * (i.e. {@code 0} for numeric values, {@code false} for {@code boolean} types and {@code null} for reference types). The annotated
     * parameter must be of type {@link Object} and cannot be assigned a value.
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.StubValue} or
     * {@link MemberSubstitution.StubValue}. This annotation should
     * be used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface StubValue {
        /* empty */
    }

    /**
     * <p>
     * Indicates that the annotated parameter should always return a default value (i.e. {@code 0} for numeric values, {@code false}
     * for {@code boolean} types and {@code null} for reference types).
     * </p>
     * <p>
     * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.implementation.bind.annotation.Empty} or
     * {@link net.bytebuddy.asm.MemberSubstitution.Unused}. This annotation should be
     * used only in combination with {@link Advice}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Unused {
        /* empty */
    }

    /**
     * <p>
     * A post processor that uses the return value of an advice method to define values for fields, arguments, the instrumented
     * method's return value or the exception being thrown. This post processor allows for the assignment of values from advice
     * methods that use delegation. When inlining advice code, it is recommended to assign values directly to annotated parameters.
     * </p>
     * <p>
     * Assignments can either be performed by returning a single value from an advice method or by returning an array with multiple
     * values. If the return type of an advice method is declared as an array type, the latter is assumed what can be overridden by
     * annotating the advice method with {@link AsScalar}. With a scalar assignment, the return type's default value is considered
     * as an indication to skip any assignment what can be configured via the annotation's property. When returning {@code null}
     * when using an array-typed assignment, the assignment will always be skipped.
     * </p>
     * <p>
     * <b>Important</b>: This post processor is not registered by default but requires explicit registration via
     * {@link Advice.WithCustomMapping#with(Advice.PostProcessor.Factory)}.
     * </p>
     * <p>
     * <b>Important</b>: Assignment exceptions will not be handled by a suppression handler but will be propagated.
     * </p>
     */
    @HashCodeAndEqualsPlugin.Enhance
    public abstract static class AssignReturned implements PostProcessor {

        /**
         * Indicates that a value is not assigned from an array but as a scalar value.
         */
        public static final int NO_INDEX = -1;

        /**
         * The advice method's return type.
         */
        protected final TypeDescription typeDescription;

        /**
         * The exception handler factory to use.
         */
        protected final ExceptionHandler.Factory exceptionHandlerFactory;

        /**
         * {@code true} if this post processor is used within exit advice.
         */
        protected final boolean exit;

        /**
         * {@code true} if a default value indicates that no assignment should be conducted.
         */
        protected final boolean skipOnDefaultValue;

        /**
         * Creates a new post processor for assigning an advice method's return value.
         *
         * @param typeDescription         The advice method's return type.
         * @param exceptionHandlerFactory The exception handler factory to use.
         * @param exit                    {@code true} if this post processor is used within exit advice.
         * @param skipOnDefaultValue      {@code true} if a default value indicates that no assignment should be conducted.
         */
        protected AssignReturned(TypeDescription typeDescription,
                                 ExceptionHandler.Factory exceptionHandlerFactory,
                                 boolean exit,
                                 boolean skipOnDefaultValue) {
            this.typeDescription = typeDescription;
            this.exceptionHandlerFactory = exceptionHandlerFactory;
            this.exit = exit;
            this.skipOnDefaultValue = skipOnDefaultValue;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation resolve(TypeDescription instrumentedType,
                                         MethodDescription instrumentedMethod,
                                         Assigner assigner,
                                         ArgumentHandler argumentHandler,
                                         StackMapFrameHandler.ForPostProcessor stackMapFrameHandler,
                                         StackManipulation exceptionHandler) {
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(getHandlers().size());
            for (Handler handler : getHandlers()) {
                stackManipulations.add(handler.resolve(instrumentedType,
                        instrumentedMethod,
                        assigner,
                        argumentHandler,
                        getType(),
                        toLoadInstruction(handler, exit ? argumentHandler.exit() : argumentHandler.enter())));
            }
            StackManipulation stackManipulation = exceptionHandlerFactory.wrap(new StackManipulation.Compound(stackManipulations),
                    exceptionHandler,
                    stackMapFrameHandler);
            return skipOnDefaultValue
                    ? DefaultValueSkip.of(stackManipulation, stackMapFrameHandler, exit ? argumentHandler.exit() : argumentHandler.enter(), typeDescription)
                    : stackManipulation;
        }

        /**
         * Returns the assigned type that is handled by any handler.
         *
         * @return The handled type.
         */
        protected abstract TypeDescription getType();

        /**
         * Returns a collection of all handlers to apply.
         *
         * @return The handlers to apply.
         */
        protected abstract Collection<Handler> getHandlers();

        /**
         * Creates a load instruction for the given handler.
         *
         * @param handler The handler for which to apply a load instruction.
         * @param offset  The offset of the value that is returned by the advice method.
         * @return A stack manipulation to load the handled value.
         */
        protected abstract StackManipulation toLoadInstruction(Handler handler, int offset);

        /**
         * Indicates that the advice method's return value is to be treated as a scalar value also if it is
         * of an array type. This implies that the advice method's return value is assigned as such and not by
         * array element.
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @java.lang.annotation.Target(ElementType.METHOD)
        public @interface AsScalar {

            /**
             * If {@code false}, a default value will cause an assignment. This implies that a suppressed error will
             * cause an assignment of the default value.
             *
             * @return {@code false}, a default value will cause an assignment.
             */
            boolean skipOnDefaultValue() default true;
        }

        /**
         * <p>
         * Assigns the advice method's return value to an argument of the instrumented method of the given index.
         * </p>
         * <p>
         * <b>Important</b>: This annotation has no effect unless an {@link AssignReturned} post processor is explicitly registered.
         * </p>
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @java.lang.annotation.Target(ElementType.METHOD)
        public @interface ToArguments {

            /**
             * The assignments to process.
             *
             * @return The assignments to process.
             */
            ToArgument[] value();

            /**
             * Defines a particular assignment for a {@link ToArguments}.
             */
            @java.lang.annotation.Target({})
            @RepeatedAnnotationPlugin.Enhance(ToArguments.class)
            @interface ToArgument {

                /**
                 * The index of the parameter to assign.
                 *
                 * @return The index of the parameter to assign.
                 */
                int value();

                /**
                 * The index in the array that is returned which represents the assigned value. If negative,
                 * a scalar return value is expected.
                 *
                 * @return The index in the array that is returned which represents the assigned value.
                 */
                int index() default NO_INDEX;

                /**
                 * The typing to apply when assigning the returned value to the targeted value.
                 *
                 * @return The typing to apply when assigning the returned value to the targeted value.
                 */
                Assigner.Typing typing() default Assigner.Typing.STATIC;
            }

            /**
             * A handler for a {@link ToArgument} annotation.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Handler implements AssignReturned.Handler {

                /**
                 * The index of the assigned argument.
                 */
                private final int value;

                /**
                 * The index in the array that is returned which represents the assigned value or a negative value
                 * if assigning a scalar value.
                 */
                private final int index;

                /**
                 * The typing to apply when assigning the returned value to the targeted value.
                 */
                private final Assigner.Typing typing;

                /**
                 * Creates a new handler.
                 *
                 * @param value  The index of the assigned argument.
                 * @param index  The index in the array that is returned which represents the assigned value or a
                 *               negative value if assigning a scalar value.
                 * @param typing The typing to apply when assigning the returned value to the targeted value.
                 */
                protected Handler(int value, int index, Assigner.Typing typing) {
                    this.value = value;
                    this.index = index;
                    this.typing = typing;
                }

                /**
                 * {@inheritDoc}
                 */
                public int getIndex() {
                    return index;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolve(TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 ArgumentHandler argumentHandler,
                                                 TypeDescription type,
                                                 StackManipulation value) {
                    if (instrumentedMethod.getParameters().size() < this.value) {
                        throw new IllegalStateException(instrumentedMethod + " declares less then " + this.value + " parameters");
                    }
                    StackManipulation assignment = assigner.assign(type.asGenericType(),
                            instrumentedMethod.getParameters().get(this.value).getType(),
                            typing);
                    if (!assignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + type + " to " + instrumentedMethod.getParameters().get(this.value).getType());
                    }
                    return new StackManipulation.Compound(value,
                            assignment,
                            MethodVariableAccess.of(instrumentedMethod.getParameters().get(this.value).getType())
                                    .storeAt(argumentHandler.argument(instrumentedMethod.getParameters().get(this.value).getOffset())));
                }

                /**
                 * A factory to create a handler for a {@link ToArguments} annotation.
                 */
                public enum Factory implements AssignReturned.Handler.Factory<ToArguments> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * A description of the {@link ToArguments#value()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_ARGUMENTS_VALUE;

                    /**
                     * A description of the {@link ToArgument#value()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_ARGUMENT_VALUE;

                    /**
                     * A description of the {@link ToArgument#index()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_ARGUMENT_INDEX;

                    /**
                     * A description of the {@link ToArgument#typing()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_ARGUMENT_TYPING;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        TO_ARGUMENTS_VALUE = TypeDescription.ForLoadedType.of(ToArguments.class)
                                .getDeclaredMethods()
                                .filter(named("value"))
                                .getOnly();
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(ToArgument.class).getDeclaredMethods();
                        TO_ARGUMENT_VALUE = methods.filter(named("value")).getOnly();
                        TO_ARGUMENT_INDEX = methods.filter(named("index")).getOnly();
                        TO_ARGUMENT_TYPING = methods.filter(named("typing")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<ToArguments> getAnnotationType() {
                        return ToArguments.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public List<AssignReturned.Handler> make(TypeDescription returnType,
                                                             boolean exit,
                                                             AnnotationDescription.Loadable<? extends ToArguments> annotation) {
                        List<AssignReturned.Handler> handlers = new ArrayList<AssignReturned.Handler>();
                        for (AnnotationDescription argument : annotation.getValue(TO_ARGUMENTS_VALUE).resolve(AnnotationDescription[].class)) {
                            int value = argument.getValue(TO_ARGUMENT_VALUE).resolve(Integer.class);
                            if (value < 0) {
                                throw new IllegalStateException("An argument cannot have a negative index for " + returnType);
                            }
                            handlers.add(new Handler(value,
                                    argument.getValue(TO_ARGUMENT_INDEX).resolve(Integer.class),
                                    argument.getValue(TO_ARGUMENT_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class)));
                        }
                        return handlers;
                    }
                }
            }
        }

        /**
         * <p>
         * Assigns the advice method's return value as an array to a number of arguments which are returned as an array where
         * each element assigns a single value with the same index as the instrumented method's parameter.
         * </p>
         * <p>
         * <b>Note</b>: If this is the only intended assignment, the return value must either be a nested array or be annotated
         * with {@link AsScalar} to indicate that the advice method's returned value should be treated as a singular return value.
         * </p>
         * <p>
         * <b>Important</b>: This annotation has no effect unless an {@link AssignReturned} post processor is explicitly registered.
         * </p>
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @java.lang.annotation.Target(ElementType.METHOD)
        public @interface ToAllArguments {

            /**
             * The index in the array that is returned which represents the assigned value. If negative,
             * a scalar return value is expected.
             *
             * @return The index in the array that is returned which represents the assigned value.
             */
            int index() default NO_INDEX;

            /**
             * The typing to apply when assigning the returned value to the targeted value.
             *
             * @return The typing to apply when assigning the returned value to the targeted value.
             */
            Assigner.Typing typing() default Assigner.Typing.STATIC;

            /**
             * A handler for a {@link ToAllArguments} annotation.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Handler implements AssignReturned.Handler {

                /**
                 * The index in the array that is returned which represents the assigned value or a negative value
                 * if assigning a scalar value.
                 */
                private final int index;

                /**
                 * The typing to apply when assigning the returned value to the targeted value.
                 */
                private final Assigner.Typing typing;

                /**
                 * Creates a new handler.
                 *
                 * @param index  The index in the array that is returned which represents the assigned value or a
                 *               negative value if assigning a scalar value.
                 * @param typing The typing to apply when assigning the returned value to the targeted value.
                 */
                protected Handler(int index, Assigner.Typing typing) {
                    this.index = index;
                    this.typing = typing;
                }

                /**
                 * {@inheritDoc}
                 */
                public int getIndex() {
                    return index;
                }

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
                public StackManipulation resolve(TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 ArgumentHandler argumentHandler,
                                                 TypeDescription type,
                                                 StackManipulation value) {
                    List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(instrumentedMethod.getParameters().size());
                    if (!type.isArray()) {
                        StackManipulation assignment = assigner.assign(type.asGenericType(), TypeDefinition.Sort.describe(Object[].class), typing);
                        if (!assignment.isValid()) {
                            throw new IllegalStateException("Cannot assign " + type + " to " + Object[].class);
                        }
                        type = TypeDescription.ForLoadedType.of(Object[].class);
                        value = new StackManipulation.Compound(value, assignment);
                    }
                    for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                        StackManipulation assignment = assigner.assign(type.getComponentType().asGenericType(), parameterDescription.getType(), typing);
                        if (!assignment.isValid()) {
                            throw new IllegalStateException("Cannot assign " + type.getComponentType() + " to " + parameterDescription);
                        }
                        stackManipulations.add(new StackManipulation.Compound(assignment, MethodVariableAccess.of(parameterDescription.getType())
                                .storeAt(argumentHandler.argument(parameterDescription.getOffset()))));
                    }
                    return new StackManipulation.Compound(value, ArrayAccess.of(type.getComponentType()).forEach(stackManipulations), Removal.SINGLE);
                }

                /**
                 * A factory to create a handler for a {@link ToAllArguments} annotation.
                 */
                public enum Factory implements AssignReturned.Handler.Factory<ToAllArguments> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * A description of the {@link ToAllArguments#index()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_ALL_ARGUMENTS_INDEX;

                    /**
                     * A description of the {@link ToAllArguments#typing()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_ALL_ARGUMENTS_TYPING;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(ToAllArguments.class).getDeclaredMethods();
                        TO_ALL_ARGUMENTS_INDEX = methods.filter(named("index")).getOnly();
                        TO_ALL_ARGUMENTS_TYPING = methods.filter(named("typing")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<ToAllArguments> getAnnotationType() {
                        return ToAllArguments.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public List<AssignReturned.Handler> make(TypeDescription typeDescription,
                                                             boolean exit,
                                                             AnnotationDescription.Loadable<? extends ToAllArguments> annotation) {
                        return Collections.<AssignReturned.Handler>singletonList(new Handler(annotation.getValue(TO_ALL_ARGUMENTS_INDEX).resolve(Integer.class),
                                annotation.getValue(TO_ALL_ARGUMENTS_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class)));
                    }
                }
            }
        }

        /**
         * <p>
         * Assigns the advice method's return value to the {@code this} reference of the instrumented method.
         * </p>
         * <p>
         * <b>Important</b>: This annotation has no effect unless an {@link AssignReturned} post processor is explicitly registered.
         * </p>
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @java.lang.annotation.Target(ElementType.METHOD)
        public @interface ToThis {

            /**
             * The index in the array that is returned which represents the assigned value. If negative,
             * a scalar return value is expected.
             *
             * @return The index in the array that is returned which represents the assigned value.
             */
            int index() default NO_INDEX;

            /**
             * The typing to apply when assigning the returned value to the targeted value.
             *
             * @return The typing to apply when assigning the returned value to the targeted value.
             */
            Assigner.Typing typing() default Assigner.Typing.STATIC;

            /**
             * A handler for the {@link ToThis} annotation.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Handler implements AssignReturned.Handler {

                /**
                 * The index in the array that is returned which represents the assigned value or a negative value
                 * if assigning a scalar value.
                 */
                private final int index;

                /**
                 * The typing to apply when assigning the returned value to the targeted value.
                 */
                private final Assigner.Typing typing;

                /**
                 * {@code true} if this handler is applied on exit advice.
                 */
                private final boolean exit;

                /**
                 * A handler for assigning the {@code this} reference.
                 *
                 * @param index  The index in the array that is returned which represents the assigned
                 *               value or a negative value if assigning a scalar value.
                 * @param typing The typing to apply when assigning the returned value to the targeted value.
                 * @param exit   {@code true} if this handler is applied on exit advice.
                 */
                protected Handler(int index, Assigner.Typing typing, boolean exit) {
                    this.index = index;
                    this.typing = typing;
                    this.exit = exit;
                }

                /**
                 * {@inheritDoc}
                 */
                public int getIndex() {
                    return index;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolve(TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 ArgumentHandler argumentHandler,
                                                 TypeDescription type,
                                                 StackManipulation value) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot assign this reference for static method " + instrumentedMethod);
                    } else if (!exit && instrumentedMethod.isConstructor()) {
                        throw new IllegalStateException("Cannot assign this reference in constructor prior to initialization for " + instrumentedMethod);
                    }
                    StackManipulation assignment = assigner.assign(type.asGenericType(),
                            instrumentedType.asGenericType(),
                            typing);
                    if (!assignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + type + " to " + instrumentedType);
                    }
                    return new StackManipulation.Compound(value,
                            assignment,
                            MethodVariableAccess.REFERENCE.storeAt(argumentHandler.argument(0)));
                }

                /**
                 * A handler factory for the {@link ToThis} annotation which assigns an advice method's return value
                 * to the <i>this</i> reference of a non-static method.
                 */
                public enum Factory implements AssignReturned.Handler.Factory<ToThis> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * A description of the {@link ToThis#index()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_THIS_INDEX;

                    /**
                     * A description of the {@link ToThis#typing()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_THIS_TYPING;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(ToThis.class).getDeclaredMethods();
                        TO_THIS_INDEX = methods.filter(named("index")).getOnly();
                        TO_THIS_TYPING = methods.filter(named("typing")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<ToThis> getAnnotationType() {
                        return ToThis.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public List<AssignReturned.Handler> make(TypeDescription returnType,
                                                             boolean exit,
                                                             AnnotationDescription.Loadable<? extends ToThis> annotation) {
                        return Collections.<AssignReturned.Handler>singletonList(new Handler(annotation.getValue(TO_THIS_INDEX).resolve(Integer.class),
                                annotation.getValue(TO_THIS_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class),
                                exit));
                    }
                }
            }
        }

        /**
         * <p>
         * Assigns the advice method's return value to a given field.
         * </p>
         * <p>
         * <b>Important</b>: This annotation has no effect unless an {@link AssignReturned} post processor is explicitly registered.
         * </p>
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @java.lang.annotation.Target(ElementType.METHOD)
        public @interface ToFields {

            /**
             * The field assignments to apply.
             *
             * @return The field assignments to apply.
             */
            ToField[] value();

            /**
             * Determines what fields are assigned when using a {@link ToFields} annotation.
             */
            @java.lang.annotation.Target({})
            @RepeatedAnnotationPlugin.Enhance(ToFields.class)
            @interface ToField {

                /**
                 * The accessed field's name or an empty string if the field name should be inferred from
                 * the method's accessor name.
                 *
                 * @return The accessed field's name or an empty string if the field name should be inferred
                 * from the method's accessor name.
                 */
                String value() default OffsetMapping.ForField.Unresolved.BEAN_PROPERTY;

                /**
                 * The field's declaring type or {@code void} if the type should be found within the
                 * instrumented type's hierarchy.
                 *
                 * @return The field's declaring type or {@code void} if the type should be found within the
                 * instrumented type's hierarchy.
                 */
                Class<?> declaringType() default void.class;

                /**
                 * The index in the array that is returned which represents the assigned value. If negative,
                 * a scalar return value is expected.
                 *
                 * @return The index in the array that is returned which represents the assigned value.
                 */
                int index() default NO_INDEX;

                /**
                 * The typing to apply when assigning the returned value to the targeted value.
                 *
                 * @return The typing to apply when assigning the returned value to the targeted value.
                 */
                Assigner.Typing typing() default Assigner.Typing.STATIC;
            }

            /**
             * A handler for a {@link ToField} annotation.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Handler implements AssignReturned.Handler {

                /**
                 * The index in the array that is returned which represents the assigned value or a negative value
                 * if assigning a scalar value.
                 */
                private final int index;

                /**
                 * The accessed field's name or an empty string if the field name should be inferred from the
                 * method's accessor name.
                 */
                private final String name;

                /**
                 * The field's declaring type or {@code void} if the type should be found within the instrumented
                 * type's hierarchy.
                 */
                private final TypeDescription declaringType;

                /**
                 * The typing to apply when assigning the returned value to the targeted value.
                 */
                private final Assigner.Typing typing;

                /**
                 * Creates a new handler for a {@link ToReturned} annotation.
                 *
                 * @param index         The index in the array that is returned which represents the assigned value or
                 *                      a negative value if assigning a scalar value.
                 * @param name          The accessed field's name or an empty string if the field name should be
                 *                      inferred from the method's accessor name.
                 * @param declaringType The field's declaring type or {@code void} if the type should be found within
                 *                      the instrumented type's hierarchy.
                 * @param typing        The typing to apply when assigning the returned value to the targeted value.
                 */
                protected Handler(int index,
                                  String name,
                                  TypeDescription declaringType,
                                  Assigner.Typing typing) {
                    this.index = index;
                    this.name = name;
                    this.declaringType = declaringType;
                    this.typing = typing;
                }

                /**
                 * {@inheritDoc}
                 */
                public int getIndex() {
                    return index;
                }

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
                public StackManipulation resolve(TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 ArgumentHandler argumentHandler,
                                                 TypeDescription type,
                                                 StackManipulation value) {
                    FieldLocator locator = declaringType.represents(void.class)
                            ? new FieldLocator.ForClassHierarchy(instrumentedType)
                            : new FieldLocator.ForExactType(declaringType);
                    FieldLocator.Resolution resolution = name.equals(OffsetMapping.ForField.Unresolved.BEAN_PROPERTY)
                            ? FieldLocator.Resolution.Simple.ofBeanAccessor(locator, instrumentedMethod)
                            : locator.locate(name);
                    StackManipulation stackManipulation;
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Cannot resolve field " + name + " for " + instrumentedType);
                    } else if (!resolution.getField().isVisibleTo(instrumentedType)) {
                        throw new IllegalStateException(resolution.getField() + " is not visible to " + instrumentedType);
                    } else if (resolution.getField().isStatic()) {
                        stackManipulation = StackManipulation.Trivial.INSTANCE;
                    } else if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot access member field " + resolution.getField() + " from static " + instrumentedMethod);
                    } else if (!instrumentedType.isAssignableTo(resolution.getField().getDeclaringType().asErasure())) {
                        throw new IllegalStateException(instrumentedType + " does not define " + resolution.getField());
                    } else {
                        stackManipulation = MethodVariableAccess.loadThis();
                    }
                    StackManipulation assignment = assigner.assign(type.asGenericType(),
                            resolution.getField().getType(),
                            typing);
                    if (!assignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + type + " to " + resolution.getField());
                    }
                    return new StackManipulation.Compound(stackManipulation,
                            value,
                            assignment,
                            FieldAccess.forField(resolution.getField()).write());
                }

                /**
                 * A factory to create a handler for a {@link ToFields} annotation.
                 */
                public enum Factory implements AssignReturned.Handler.Factory<ToFields> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * A description of the {@link ToFields#value()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_FIELDS_VALUE;

                    /**
                     * A description of the {@link ToField#value()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_FIELD_VALUE;

                    /**
                     * A description of the {@link ToField#index()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_FIELD_INDEX;

                    /**
                     * A description of the {@link ToField#declaringType()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_FIELD_DECLARING_TYPE;

                    /**
                     * A description of the {@link ToField#typing()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_FIELD_TYPING;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        TO_FIELDS_VALUE = TypeDescription.ForLoadedType.of(ToFields.class)
                                .getDeclaredMethods()
                                .filter(named("value"))
                                .getOnly();
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(ToField.class).getDeclaredMethods();
                        TO_FIELD_VALUE = methods.filter(named("value")).getOnly();
                        TO_FIELD_INDEX = methods.filter(named("index")).getOnly();
                        TO_FIELD_DECLARING_TYPE = methods.filter(named("declaringType")).getOnly();
                        TO_FIELD_TYPING = methods.filter(named("typing")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<ToFields> getAnnotationType() {
                        return ToFields.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public List<AssignReturned.Handler> make(TypeDescription returnType,
                                                             boolean exit,
                                                             AnnotationDescription.Loadable<? extends ToFields> annotation) {
                        List<AssignReturned.Handler> handlers = new ArrayList<AssignReturned.Handler>();
                        for (AnnotationDescription field : annotation.getValue(TO_FIELDS_VALUE).resolve(AnnotationDescription[].class)) {
                            handlers.add(new Handler(field.getValue(TO_FIELD_INDEX).resolve(Integer.class),
                                    field.getValue(TO_FIELD_VALUE).resolve(String.class),
                                    field.getValue(TO_FIELD_DECLARING_TYPE).resolve(TypeDescription.class),
                                    field.getValue(TO_FIELD_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class)));
                        }
                        return handlers;
                    }
                }
            }
        }

        /**
         * <p>
         * Assigns the advice method's return value to the instrumented method's return value. This annotation can only be used
         * with exit advice marked with {@link OnMethodExit}.
         * </p>
         * <p>
         * <b>Important</b>: This annotation has no effect unless an {@link AssignReturned} post processor is explicitly registered.
         * </p>
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @java.lang.annotation.Target(ElementType.METHOD)
        public @interface ToReturned {

            /**
             * The index in the array that is returned which represents the assigned value. If negative,
             * a scalar return value is expected.
             *
             * @return The index in the array that is returned which represents the assigned value.
             */
            int index() default NO_INDEX;

            /**
             * The typing to apply when assigning the returned value to the targeted value.
             *
             * @return The typing to apply when assigning the returned value to the targeted value.
             */
            Assigner.Typing typing() default Assigner.Typing.STATIC;

            /**
             * A handler for a {@link ToReturned} annotation.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Handler implements AssignReturned.Handler {

                /**
                 * The index in the array that is returned which represents the assigned value or a negative value
                 * if assigning a scalar value.
                 */
                private final int index;

                /**
                 * The typing to apply when assigning the returned value to the targeted value.
                 */
                private final Assigner.Typing typing;

                /**
                 * Creates a new handler for a {@link ToReturned} annotation.
                 *
                 * @param index  The index in the array that is returned which represents the assigned value or
                 *               a negative value if assigning a scalar value.
                 * @param typing The typing to apply when assigning the returned value to the targeted value.
                 */
                protected Handler(int index, Assigner.Typing typing) {
                    this.index = index;
                    this.typing = typing;
                }

                /**
                 * {@inheritDoc}
                 */
                public int getIndex() {
                    return index;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolve(TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 ArgumentHandler argumentHandler,
                                                 TypeDescription type,
                                                 StackManipulation value) {
                    if (instrumentedMethod.getReturnType().represents(void.class)) {
                        return StackManipulation.Trivial.INSTANCE;
                    }
                    StackManipulation assignment = assigner.assign(type.asGenericType(), instrumentedMethod.getReturnType(), typing);
                    if (!assignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + type + " to " + instrumentedMethod.getReturnType());
                    }
                    return new StackManipulation.Compound(value,
                            assignment,
                            MethodVariableAccess.of(instrumentedMethod.getReturnType()).storeAt(argumentHandler.returned()));
                }

                /**
                 * A factory to create a handler for a {@link ToReturned} annotation.
                 */
                public enum Factory implements AssignReturned.Handler.Factory<ToReturned> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * A description of the {@link ToReturned#index()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_RETURNED_INDEX;

                    /**
                     * A description of the {@link ToReturned#typing()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_RETURNED_TYPING;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(ToReturned.class).getDeclaredMethods();
                        TO_RETURNED_INDEX = methods.filter(named("index")).getOnly();
                        TO_RETURNED_TYPING = methods.filter(named("typing")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<ToReturned> getAnnotationType() {
                        return ToReturned.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public List<AssignReturned.Handler> make(TypeDescription returnType,
                                                             boolean exit,
                                                             AnnotationDescription.Loadable<? extends ToReturned> annotation) {
                        if (!exit) {
                            throw new IllegalStateException("Cannot write returned value from enter advice for " + returnType);
                        }
                        return Collections.<AssignReturned.Handler>singletonList(new ToReturned.Handler(annotation.getValue(TO_RETURNED_INDEX).resolve(Integer.class),
                                annotation.getValue(TO_RETURNED_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class)));
                    }
                }
            }
        }

        /**
         * <p>
         * Assigns the advice method's return value as the instrumented method's thrown exception. This annotation can only be used
         * with exit advice marked with {@link OnMethodExit}. Note that a {@code null} value on a scalar assignment deactivates this
         * handler and does not, by default, remove a thrown exception. To avoid this, an array assignment must be used or
         * {@link AsScalar#skipOnDefaultValue()} must be set to {@code false}.
         * </p>
         * <p>
         * <b>Important</b>: This annotation has no effect unless an {@link AssignReturned} post processor is explicitly registered.
         * </p>
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @java.lang.annotation.Target(ElementType.METHOD)
        public @interface ToThrown {

            /**
             * The index in the array that is returned which represents the assigned value. If negative,
             * a scalar return value is expected.
             *
             * @return The index in the array that is returned which represents the assigned value.
             */
            int index() default NO_INDEX;

            /**
             * The typing to apply when assigning the returned value to the targeted value.
             *
             * @return The typing to apply when assigning the returned value to the targeted value.
             */
            Assigner.Typing typing() default Assigner.Typing.STATIC;

            /**
             * A handler for a {@link ToThrown} annotation.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Handler implements AssignReturned.Handler {

                /**
                 * The index in the array that is returned which represents the assigned value or a negative value
                 * if assigning a scalar value.
                 */
                private final int index;

                /**
                 * The typing to apply when assigning the returned value to the targeted value.
                 */
                private final Assigner.Typing typing;

                /**
                 * Creates a new handler to assign a {@link ToThrown} annotation.
                 *
                 * @param index  The index in the array that is returned which represents the assigned value or a
                 *               negative value if assigning a scalar value.
                 * @param typing The typing to apply when assigning the returned value to the targeted value.
                 */
                protected Handler(int index, Assigner.Typing typing) {
                    this.index = index;
                    this.typing = typing;
                }

                /**
                 * {@inheritDoc}
                 */
                public int getIndex() {
                    return index;
                }

                /**
                 * {@inheritDoc}
                 */
                public StackManipulation resolve(TypeDescription instrumentedType,
                                                 MethodDescription instrumentedMethod,
                                                 Assigner assigner,
                                                 ArgumentHandler argumentHandler,
                                                 TypeDescription type,
                                                 StackManipulation value) {
                    StackManipulation assignment = assigner.assign(type.asGenericType(), TypeDefinition.Sort.describe(Throwable.class), typing);
                    if (!assignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + type + " to " + Throwable.class.getName());
                    }
                    return new StackManipulation.Compound(value,
                            assignment,
                            MethodVariableAccess.REFERENCE.storeAt(argumentHandler.thrown()));
                }

                /**
                 * A factory to create a handler for a {@link ToThrown} annotation.
                 */
                public enum Factory implements AssignReturned.Handler.Factory<ToThrown> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * A description of the {@link ToThrown#index()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_THROWN_INDEX;

                    /**
                     * A description of the {@link ToThrown#typing()} method.
                     */
                    private static final MethodDescription.InDefinedShape TO_THROWN_TYPING;

                    /*
                     * Resolves annotation properties.
                     */
                    static {
                        MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(ToThrown.class).getDeclaredMethods();
                        TO_THROWN_INDEX = methods.filter(named("index")).getOnly();
                        TO_THROWN_TYPING = methods.filter(named("typing")).getOnly();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<ToThrown> getAnnotationType() {
                        return ToThrown.class;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming annotation for exit advice.")
                    public List<AssignReturned.Handler> make(TypeDescription returnType,
                                                             boolean exit,
                                                             AnnotationDescription.Loadable<? extends ToThrown> annotation) {
                        if (!exit) {
                            throw new IllegalStateException("Cannot assign thrown value from enter advice for " + returnType); // TODO
                        }
                        return Collections.<AssignReturned.Handler>singletonList(new ToThrown.Handler(annotation.getValue(TO_THROWN_INDEX).resolve(Integer.class),
                                annotation.getValue(TO_THROWN_TYPING).resolve(EnumerationDescription.class).load(Assigner.Typing.class)));
                    }
                }
            }
        }

        /**
         * A post processor implementation of {@link AssignReturned} that works on the value of an array.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ForArray extends AssignReturned {

            /**
             * A mapping of the handlers to apply mapped to the index in the array that is returned by the advice method.
             */
            private final Map<Handler, Integer> handlers;

            /**
             * Creates a post processor to assign a returned array value by index.
             *
             * @param typeDescription         The array type that is returned by the advice method.
             * @param exceptionHandlerFactory The exception handler factory to use.
             * @param exit                    {@code true} if the post processor is applied to exit advice.
             * @param handlers                The handlers to apply.
             */
            protected ForArray(TypeDescription typeDescription,
                               ExceptionHandler.Factory exceptionHandlerFactory,
                               boolean exit,
                               Collection<List<Handler>> handlers) {
                super(typeDescription, exceptionHandlerFactory, exit, true);
                this.handlers = new LinkedHashMap<Handler, Integer>();
                for (List<Handler> collection : handlers) {
                    for (Handler handler : collection) {
                        int index = handler.getIndex();
                        if (index <= NO_INDEX) {
                            throw new IllegalStateException("Handler on array requires positive index for " + handler);
                        }
                        this.handlers.put(handler, index);
                    }
                }
            }

            @Override
            @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
            protected TypeDescription getType() {
                return typeDescription.getComponentType();
            }

            @Override
            protected Collection<Handler> getHandlers() {
                return handlers.keySet();
            }

            @Override
            @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
            protected StackManipulation toLoadInstruction(Handler handler, int offset) {
                return new StackManipulation.Compound(MethodVariableAccess.REFERENCE.loadFrom(offset),
                        IntegerConstant.forValue(handlers.get(handler)),
                        ArrayAccess.of(typeDescription.getComponentType()).load());
            }
        }

        /**
         * A post processor implementation of {@link AssignReturned} that uses the returned value as such.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ForScalar extends AssignReturned {

            /**
             * The list of handlers to apply.
             */
            private final List<Handler> handlers;

            /**
             * Creates a post processor to assign a returned scalar value.
             *
             * @param typeDescription         The type of the advice method.
             * @param exceptionHandlerFactory The exception handler factory to use.
             * @param exit                    {@code true} if the post processor is applied to exit advice.
             * @param skipOnDefaultValue      {@code true} if a default value indicates that no assignment should be conducted.
             * @param handlers                The handlers to apply.
             */
            protected ForScalar(TypeDescription typeDescription,
                                ExceptionHandler.Factory exceptionHandlerFactory,
                                boolean exit,
                                boolean skipOnDefaultValue,
                                Collection<List<Handler>> handlers) {
                super(typeDescription, exceptionHandlerFactory, exit, skipOnDefaultValue);
                this.handlers = new ArrayList<Handler>();
                for (List<Handler> collection : handlers) {
                    for (Handler handler : collection) {
                        int index = handler.getIndex();
                        if (index > NO_INDEX) {
                            throw new IllegalStateException("Handler on array requires negative index for " + handler);
                        }
                        this.handlers.add(handler);
                    }
                }
            }

            @Override
            protected TypeDescription getType() {
                return typeDescription;
            }

            @Override
            protected Collection<Handler> getHandlers() {
                return handlers;
            }

            @Override
            protected StackManipulation toLoadInstruction(Handler handler, int offset) {
                return MethodVariableAccess.of(typeDescription).loadFrom(offset);
            }
        }

        /**
         * A stack manipulation that applies a null-check on the returned value which indicates if an assignment
         * should be skipped, if discovered.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class DefaultValueSkip implements StackManipulation {

            /**
             * The wrapped stack manipulation in case the value is not a default value.
             */
            private final StackManipulation stackManipulation;

            /**
             * The stack map frame handler to use.
             */
            private final StackMapFrameHandler.ForPostProcessor stackMapFrameHandler;

            /**
             * The offset of the value of the returned type.
             */
            private final int offset;

            /**
             * The dispatcher to use.
             */
            private final Dispatcher dispatcher;

            /**
             * Creates a null-check wrapper.
             *
             * @param stackManipulation    The wrapped stack manipulation in case the value is not a default value.
             * @param stackMapFrameHandler The stack map frame handler to use.
             * @param offset               The offset of the value of the returned type.
             * @param dispatcher           The dispatcher to use.
             */
            protected DefaultValueSkip(StackManipulation stackManipulation,
                                       StackMapFrameHandler.ForPostProcessor stackMapFrameHandler,
                                       int offset,
                                       Dispatcher dispatcher) {
                this.stackManipulation = stackManipulation;
                this.stackMapFrameHandler = stackMapFrameHandler;
                this.offset = offset;
                this.dispatcher = dispatcher;
            }

            /**
             * Resolves a skipping stack manipulation for the supplied type.
             *
             * @param stackManipulation    The stack manipulation to wrap.
             * @param stackMapFrameHandler The stack map frame handler to use.
             * @param offset               The offset of the value of the returned type.
             * @param typeDefinition       The type that is returned by the advice method.
             * @return A suitable stack manipulation.
             */
            protected static StackManipulation of(StackManipulation stackManipulation,
                                                  StackMapFrameHandler.ForPostProcessor stackMapFrameHandler,
                                                  int offset,
                                                  TypeDefinition typeDefinition) {
                Dispatcher dispatcher;
                if (typeDefinition.isPrimitive()) {
                    if (typeDefinition.represents(boolean.class)
                            || typeDefinition.represents(byte.class)
                            || typeDefinition.represents(short.class)
                            || typeDefinition.represents(char.class)
                            || typeDefinition.represents(int.class)) {
                        dispatcher = Dispatcher.INTEGER;
                    } else if (typeDefinition.represents(long.class)) {
                        dispatcher = Dispatcher.LONG;
                    } else if (typeDefinition.represents(float.class)) {
                        dispatcher = Dispatcher.FLOAT;
                    } else if (typeDefinition.represents(double.class)) {
                        dispatcher = Dispatcher.DOUBLE;
                    } else {
                        throw new IllegalArgumentException("Cannot apply skip for " + typeDefinition);
                    }
                } else {
                    dispatcher = Dispatcher.REFERENCE;
                }
                return new DefaultValueSkip(stackManipulation, stackMapFrameHandler, offset, dispatcher);
            }

            /**
             * {@inheritDoc}
             */
            public boolean isValid() {
                return stackManipulation.isValid();
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                Label label = new Label();
                Size size = dispatcher.apply(methodVisitor, offset, label).aggregate(stackManipulation.apply(methodVisitor, implementationContext));
                methodVisitor.visitInsn(Opcodes.NOP);
                methodVisitor.visitLabel(label);
                stackMapFrameHandler.injectIntermediateFrame(methodVisitor, Collections.<TypeDescription>emptyList());
                methodVisitor.visitInsn(Opcodes.NOP);
                return size;
            }

            /**
             * A dispatcher for skipping a default value.
             */
            protected enum Dispatcher {

                /**
                 * A dispatcher for integer types.
                 */
                INTEGER {
                    @Override
                    protected Size apply(MethodVisitor methodVisitor, int offset, Label label) {
                        methodVisitor.visitVarInsn(Opcodes.ILOAD, offset);
                        methodVisitor.visitJumpInsn(Opcodes.IFEQ, label);
                        return new Size(0, 1);
                    }
                },

                /**
                 * A dispatcher for {@code long}.
                 */
                LONG {
                    @Override
                    protected Size apply(MethodVisitor methodVisitor, int offset, Label label) {
                        methodVisitor.visitVarInsn(Opcodes.LLOAD, offset);
                        methodVisitor.visitInsn(Opcodes.LCONST_0);
                        methodVisitor.visitInsn(Opcodes.LCMP);
                        methodVisitor.visitJumpInsn(Opcodes.IFEQ, label);
                        return new Size(0, 4);
                    }
                },

                /**
                 * A dispatcher for {@code float}.
                 */
                FLOAT {
                    @Override
                    protected Size apply(MethodVisitor methodVisitor, int offset, Label label) {
                        methodVisitor.visitVarInsn(Opcodes.FLOAD, offset);
                        methodVisitor.visitInsn(Opcodes.FCONST_0);
                        methodVisitor.visitInsn(Opcodes.FCMPL);
                        methodVisitor.visitJumpInsn(Opcodes.IFEQ, label);
                        return new Size(0, 2);
                    }
                },

                /**
                 * A dispatcher for {@code double}.
                 */
                DOUBLE {
                    @Override
                    protected Size apply(MethodVisitor methodVisitor, int offset, Label label) {
                        methodVisitor.visitVarInsn(Opcodes.DLOAD, offset);
                        methodVisitor.visitInsn(Opcodes.DCONST_0);
                        methodVisitor.visitInsn(Opcodes.DCMPL);
                        methodVisitor.visitJumpInsn(Opcodes.IFEQ, label);
                        return new Size(0, 4);
                    }
                },

                /**
                 * A dispatcher for reference types.
                 */
                REFERENCE {
                    @Override
                    protected Size apply(MethodVisitor methodVisitor, int offset, Label label) {
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, offset);
                        methodVisitor.visitJumpInsn(Opcodes.IFNULL, label);
                        return new Size(0, 2);
                    }
                };

                /**
                 * Applies this jump.
                 *
                 * @param methodVisitor The method visitor to use.
                 * @param offset        The offset of the advice return value.
                 * @param label         The label to jump to if a default value is discovered.
                 * @return The size of this instruction.
                 */
                protected abstract Size apply(MethodVisitor methodVisitor, int offset, Label label);
            }
        }

        /**
         * An exception handler to handle exceptions during assignment.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ExceptionHandler implements StackManipulation {

            /**
             * The stack manipulation that represents the assignment.
             */
            private final StackManipulation stackManipulation;

            /**
             * The exception handler to apply.
             */
            private final StackManipulation exceptionHandler;

            /**
             * The exception type to handle.
             */
            private final TypeDescription exceptionType;

            /**
             * The stack map frame handler to use.
             */
            private final StackMapFrameHandler.ForPostProcessor stackMapFrameHandler;

            /**
             * Creates a new exception handler for an assignment.
             *
             * @param stackManipulation    The stack manipulation that represents the assignment.
             * @param exceptionHandler     The exception handler to apply.
             * @param exceptionType        The exception type to handle.
             * @param stackMapFrameHandler The stack map frame handler to use.
             */
            protected ExceptionHandler(StackManipulation stackManipulation,
                                       StackManipulation exceptionHandler,
                                       TypeDescription exceptionType,
                                       StackMapFrameHandler.ForPostProcessor stackMapFrameHandler) {
                this.stackManipulation = stackManipulation;
                this.exceptionHandler = exceptionHandler;
                this.exceptionType = exceptionType;
                this.stackMapFrameHandler = stackMapFrameHandler;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isValid() {
                return stackManipulation.isValid() && exceptionHandler.isValid();
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                Label start = new Label(), handler = new Label(), end = new Label();
                methodVisitor.visitTryCatchBlock(start, handler, handler, exceptionType.getInternalName());
                methodVisitor.visitLabel(start);
                Size size = stackManipulation.apply(methodVisitor, implementationContext);
                methodVisitor.visitJumpInsn(Opcodes.GOTO, end);
                methodVisitor.visitLabel(handler);
                stackMapFrameHandler.injectIntermediateFrame(methodVisitor, Collections.singletonList(exceptionType));
                size = exceptionHandler.apply(methodVisitor, implementationContext).aggregate(size);
                methodVisitor.visitLabel(end);
                stackMapFrameHandler.injectIntermediateFrame(methodVisitor, Collections.<TypeDescription>emptyList());
                return size;
            }

            /**
             * A factory for wrapping an assignment with an exception handler, if appropriate.
             */
            public interface Factory {

                /**
                 * Wraps the supplied stack manipulation.
                 *
                 * @param stackManipulation    The stack manipulation that represents the assignment.
                 * @param exceptionHandler     The exception handler to apply.
                 * @param stackMapFrameHandler The stack map frame handler to use.
                 * @return The resolved stack manipulation.
                 */
                StackManipulation wrap(StackManipulation stackManipulation,
                                       StackManipulation exceptionHandler,
                                       StackMapFrameHandler.ForPostProcessor stackMapFrameHandler);

                /**
                 * A non-operational factory that does not produce an exception handler.
                 */
                enum NoOp implements Factory {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation wrap(StackManipulation stackManipulation,
                                                  StackManipulation exceptionHandler,
                                                  StackMapFrameHandler.ForPostProcessor stackMapFrameHandler) {
                        return stackManipulation;
                    }
                }

                /**
                 * A factory that creates an exception handler for a given exception type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Enabled implements Factory {

                    /**
                     * The exception type being handled.
                     */
                    private final TypeDescription exceptionType;

                    /**
                     * Creates a factory for an exception handler of the supplied exception type.
                     *
                     * @param exceptionType The exception type being handled.
                     */
                    protected Enabled(TypeDescription exceptionType) {
                        this.exceptionType = exceptionType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation wrap(StackManipulation stackManipulation,
                                                  StackManipulation exceptionHandler,
                                                  StackMapFrameHandler.ForPostProcessor stackMapFrameHandler) {
                        return new ExceptionHandler(stackManipulation, exceptionHandler, exceptionType, stackMapFrameHandler);
                    }
                }
            }
        }

        /**
         * A handler for an {@link AssignReturned} post processor to assign a value that was returned by
         * advice to a value of the instrumented method.
         */
        public interface Handler {

            /**
             * Returns the array offset which this handler intends to assign or a negative value if this handler
             * intends to assign a scalar value.
             *
             * @return The array offset which this handler intends to assign or a negative value if this handler
             * intends to assign a scalar value.
             */
            int getIndex();

            /**
             * Resolves this handler.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param assigner           The assigner to use.
             * @param argumentHandler    The argument handler for the handled advice method.
             * @param type               The type that this handler receives for assignment.
             * @param value              An instruction to load the handled value onto the operand stack.
             * @return The stack manipulation resolved by this handler.
             */
            StackManipulation resolve(TypeDescription instrumentedType,
                                      MethodDescription instrumentedMethod,
                                      Assigner assigner,
                                      ArgumentHandler argumentHandler,
                                      TypeDescription type,
                                      StackManipulation value);

            /**
             * A factory for resolving a handler for a given advice method.
             *
             * @param <T> The annotation type that activates this handler factory.
             */
            interface Factory<T extends Annotation> {

                /**
                 * Returns the annotation type that activates this handler factory.
                 *
                 * @return The annotation type that activates this handler factory.
                 */
                Class<T> getAnnotationType();

                /**
                 * Resolves a list of handlers for this factory.
                 *
                 * @param returnType The advice method's return type for which to resolve handlers.
                 * @param exit       {@code true} if this factory is applied for exit advice.
                 * @param annotation The annotation that activated this handler factory.
                 * @return A list of handlers to apply.
                 */
                List<Handler> make(TypeDescription returnType,
                                   boolean exit,
                                   AnnotationDescription.Loadable<? extends T> annotation);

                /**
                 * A simple implementation of a {@link Handler.Factory} that resolves a given list of {@link Handler}s.
                 *
                 * @param <S> The annotation type that activates this handler factory.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Simple<S extends Annotation> implements Factory<S> {

                    /**
                     * The annotation type that activates this handler factory.
                     */
                    private final Class<S> type;

                    /**
                     * The handlers this factory should return.
                     */
                    private final List<Handler> handlers;

                    /**
                     * Creates a new simple handler.
                     *
                     * @param type     The annotation type that activates this handler factory.
                     * @param handlers The handlers this factory should return.
                     */
                    public Simple(Class<S> type, List<Handler> handlers) {
                        this.type = type;
                        this.handlers = handlers;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<S> getAnnotationType() {
                        return type;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public List<Handler> make(TypeDescription returnType,
                                              boolean exit,
                                              AnnotationDescription.Loadable<? extends S> annotation) {
                        return handlers;
                    }
                }
            }
        }

        /**
         * A factory to create a {@link AssignReturned} post processor. This processor is only added if the advice
         * method does not return {@code void}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public static class Factory implements PostProcessor.Factory {

            /**
             * A description of the {@link AsScalar#skipOnDefaultValue()} method.
             */
            private static final MethodDescription.InDefinedShape SKIP_ON_DEFAULT_VALUE = TypeDescription.ForLoadedType.of(AsScalar.class)
                    .getDeclaredMethods()
                    .filter(named("skipOnDefaultValue"))
                    .getOnly();

            /**
             * The handler factories to apply.
             */
            private final List<? extends Handler.Factory<?>> factories;

            /**
             * The exception handler factory to use.
             */
            private final ExceptionHandler.Factory exceptionHandlerFactory;

            /**
             * Creates a new factory which a preresolved list of handler factories.
             */
            @SuppressWarnings("unchecked")
            public Factory() {
                this(Arrays.asList(ToArguments.Handler.Factory.INSTANCE,
                        ToAllArguments.Handler.Factory.INSTANCE,
                        ToThis.Handler.Factory.INSTANCE,
                        ToFields.Handler.Factory.INSTANCE,
                        ToReturned.Handler.Factory.INSTANCE,
                        ToThrown.Handler.Factory.INSTANCE), ExceptionHandler.Factory.NoOp.INSTANCE);
            }

            /**
             * Creates a new factory.
             *
             * @param factories               The handler factories to apply.
             * @param exceptionHandlerFactory The exception handler factory to use.
             */
            protected Factory(List<? extends Handler.Factory<?>> factories, ExceptionHandler.Factory exceptionHandlerFactory) {
                this.factories = factories;
                this.exceptionHandlerFactory = exceptionHandlerFactory;
            }

            /**
             * Includes a list of handlers upon discovering an annotation of a given type.
             *
             * @param type    The annotation type that activates the handler factory.
             * @param handler The handlers to use upon discovery.
             * @return A new {@link AssignReturned.Factory} that includes the provided handlers.
             */
            public Factory with(Class<? extends Annotation> type, Handler... handler) {
                return with(type, Arrays.asList(handler));
            }

            /**
             * Includes a list of handlers upon discovering an annotation of a given type.
             *
             * @param type     The annotation type that activates the handler factory.
             * @param handlers The handlers to use upon discovery.
             * @return A new {@link AssignReturned.Factory} that includes the provided handlers.
             */
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Factory with(Class<? extends Annotation> type, List<Handler> handlers) {
                return with(new Handler.Factory.Simple(type, handlers));
            }

            /**
             * Includes the provided handler factory.
             *
             * @param factory The handler factory to include.
             * @return new {@link AssignReturned.Factory} that includes the provided handler factory.
             */
            public Factory with(Handler.Factory<?> factory) {
                return new Factory(CompoundList.of(factories, factory), exceptionHandlerFactory);
            }

            /**
             * Configures this post processor to handle exceptions during assignment with the advice's {@link Advice.ExceptionHandler}.
             *
             * @param exceptionType The {@link Throwable} type to handle.
             * @return A new assigning post processor that handles the specified exception types.
             */
            public PostProcessor.Factory withSuppressed(Class<? extends Throwable> exceptionType) {
                return withSuppressed(TypeDescription.ForLoadedType.of(exceptionType));
            }

            /**
             * Configures this post processor to handle exceptions during assignment with the advice's {@link Advice.ExceptionHandler}.
             *
             * @param exceptionType The {@link Throwable} type to handle.
             * @return A new assigning post processor that handles the specified exception types.
             */
            public PostProcessor.Factory withSuppressed(TypeDescription exceptionType) {
                if (!exceptionType.isAssignableTo(Throwable.class)) {
                    throw new IllegalArgumentException(exceptionType + " is not a throwable type");
                }
                return new Factory(factories, new ExceptionHandler.Factory.Enabled(exceptionType));
            }

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            public PostProcessor make(List<? extends AnnotationDescription> annotations,
                                      TypeDescription returnType,
                                      boolean exit) {
                if (returnType.represents(void.class)) {
                    return NoOp.INSTANCE;
                }
                Map<String, Handler.Factory<?>> factories = new HashMap<String, Handler.Factory<?>>();
                for (Handler.Factory<?> factory : this.factories) {
                    if (factories.put(factory.getAnnotationType().getName(), factory) != null) {
                        throw new IllegalStateException("Duplicate registration of handler for " + factory.getAnnotationType());
                    }
                }
                Map<Class<?>, List<Handler>> handlers = new LinkedHashMap<Class<?>, List<Handler>>();
                boolean scalar = false, skipOnDefaultValue = true;
                for (AnnotationDescription annotation : annotations) {
                    if (annotation.getAnnotationType().represents(AsScalar.class)) {
                        scalar = true;
                        skipOnDefaultValue = annotation.getValue(SKIP_ON_DEFAULT_VALUE).resolve(Boolean.class);
                        continue;
                    }
                    Handler.Factory<?> factory = factories.get(annotation.getAnnotationType().getName());
                    if (factory != null) {
                        if (handlers.put(factory.getAnnotationType(), factory.make(returnType,
                                exit,
                                (AnnotationDescription.Loadable) annotation.prepare(factory.getAnnotationType()))) != null) {
                            throw new IllegalStateException("Duplicate handler registration for " + annotation.getAnnotationType());
                        }
                    }
                }
                if (handlers.isEmpty()) {
                    return NoOp.INSTANCE;
                } else {
                    return !scalar && returnType.isArray()
                            ? new ForArray(returnType, exceptionHandlerFactory, exit, handlers.values())
                            : new ForScalar(returnType, exceptionHandlerFactory, exit, skipOnDefaultValue, handlers.values());
                }
            }
        }
    }

    /**
     * A builder step for creating an {@link Advice} that uses custom mappings of annotations to constant pool values.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class WithCustomMapping {

        /**
         * The post processor factory to apply.
         */
        private final PostProcessor.Factory postProcessorFactory;

        /**
         * The delegator factory to use.
         */
        private final Delegator.Factory delegatorFactory;

        /**
         * The class reader factory to use.
         */
        private final AsmClassReader.Factory classReaderFactory;

        /**
         * A map containing dynamically computed constant pool values that are mapped by their triggering annotation type.
         */
        private final Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings;

        /**
         * Creates a new custom mapping builder step without including any custom mappings.
         */
        protected WithCustomMapping() {
            this(PostProcessor.NoOp.INSTANCE,
                    Collections.<Class<? extends Annotation>, OffsetMapping.Factory<?>>emptyMap(),
                    Delegator.ForRegularInvocation.Factory.INSTANCE,
                    AsmClassReader.Factory.Default.IMPLICIT);
        }

        /**
         * Creates a new custom mapping builder step with the given custom mappings.
         *
         * @param postProcessorFactory The post processor factory to apply.
         * @param offsetMappings       A map containing dynamically computed constant pool values that are mapped by their triggering annotation type.
         * @param delegatorFactory     The delegator factory to use.
         * @param classReaderFactory   The class reader factory to use.
         */
        protected WithCustomMapping(PostProcessor.Factory postProcessorFactory,
                                    Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings,
                                    Delegator.Factory delegatorFactory,
                                    AsmClassReader.Factory classReaderFactory) {
            this.postProcessorFactory = postProcessorFactory;
            this.offsetMappings = offsetMappings;
            this.delegatorFactory = delegatorFactory;
            this.classReaderFactory = classReaderFactory;
        }

        /**
         * Binds the supplied annotation to a type constant of the supplied value. Constants can be strings, method handles, method types
         * and any primitive or the value {@code null}.
         *
         * @param type  The type of the annotation being bound.
         * @param value The value to bind to the annotation or {@code null} to bind the parameter type's default value.
         * @param <T>   The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, @MaybeNull Object value) {
            return bind(OffsetMapping.ForStackManipulation.Factory.of(type, value));
        }

        /**
         * Binds the supplied annotation to the value of the supplied field. The field must be visible by the
         * instrumented type and must be declared by a super type of the instrumented field.
         *
         * @param type  The type of the annotation being bound.
         * @param field The field to bind to this annotation.
         * @param <T>   The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Field field) {
            return bind(type, new FieldDescription.ForLoadedField(field));
        }

        /**
         * Binds the supplied annotation to the value of the supplied field. The field must be visible by the
         * instrumented type and must be declared by a super type of the instrumented field. The binding is defined
         * as read-only and applied static typing.
         *
         * @param type             The type of the annotation being bound.
         * @param fieldDescription The field to bind to this annotation.
         * @param <T>              The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, FieldDescription fieldDescription) {
            return bind(new OffsetMapping.ForField.Resolved.Factory<T>(type, fieldDescription));
        }

        /**
         * Binds the supplied annotation to the supplied parameter's argument.
         *
         * @param type   The type of the annotation being bound.
         * @param method The method that defines the parameter.
         * @param index  The index of the parameter.
         * @param <T>    The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Method method, int index) {
            if (index < 0) {
                throw new IllegalArgumentException("A parameter cannot be negative: " + index);
            } else if (method.getParameterTypes().length <= index) {
                throw new IllegalArgumentException(method + " does not declare a parameter with index " + index);
            }
            return bind(type, new MethodDescription.ForLoadedMethod(method).getParameters().get(index));
        }

        /**
         * Binds the supplied annotation to the supplied parameter's argument.
         *
         * @param type        The type of the annotation being bound.
         * @param constructor The constructor that defines the parameter.
         * @param index       The index of the parameter.
         * @param <T>         The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Constructor<?> constructor, int index) {
            if (index < 0) {
                throw new IllegalArgumentException("A parameter cannot be negative: " + index);
            } else if (constructor.getParameterTypes().length <= index) {
                throw new IllegalArgumentException(constructor + " does not declare a parameter with index " + index);
            }
            return bind(type, new MethodDescription.ForLoadedConstructor(constructor).getParameters().get(index));
        }

        /**
         * Binds the supplied annotation to the supplied parameter's argument. The binding is declared read-only and
         * applies static typing.
         *
         * @param type                 The type of the annotation being bound.
         * @param parameterDescription The parameter for which to bind an argument.
         * @param <T>                  The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, ParameterDescription parameterDescription) {
            return bind(new OffsetMapping.ForArgument.Resolved.Factory<T>(type, parameterDescription));
        }

        /**
         * Binds the supplied annotation to the supplied type constant.
         *
         * @param type  The type of the annotation being bound.
         * @param value The type constant to bind.
         * @param <T>   The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Class<?> value) {
            return bind(type, TypeDescription.ForLoadedType.of(value));
        }

        /**
         * Binds the supplied annotation to the supplied type constant.
         *
         * @param type  The type of the annotation being bound.
         * @param value The type constant to bind.
         * @param <T>   The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, TypeDescription value) {
            return bind(new OffsetMapping.ForStackManipulation.Factory<T>(type, value));
        }

        /**
         * Binds the supplied annotation to the supplied enumeration constant.
         *
         * @param type  The type of the annotation being bound.
         * @param value The enumeration constant to bind.
         * @param <T>   The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Enum<?> value) {
            return bind(type, new EnumerationDescription.ForLoadedEnumeration(value));
        }

        /**
         * Binds the supplied annotation to the supplied enumeration constant.
         *
         * @param type  The type of the annotation being bound.
         * @param value The enumeration constant to bind.
         * @param <T>   The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, EnumerationDescription value) {
            return bind(new OffsetMapping.ForStackManipulation.Factory<T>(type, value));
        }

        /**
         * Binds the supplied annotation to the supplied fixed value.
         *
         * @param type  The type of the annotation being bound.
         * @param value The value to bind to this annotation.
         * @param <T>   The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        @SuppressWarnings("unchecked")
        public <T extends Annotation> WithCustomMapping bindSerialized(Class<T> type, Serializable value) {
            return bindSerialized(type, value, (Class<Serializable>) value.getClass());
        }

        /**
         * Binds the supplied annotation to the supplied fixed value.
         *
         * @param type       The type of the annotation being bound.
         * @param value      The value to bind to this annotation.
         * @param targetType The type of {@code value} as which the instance should be treated.
         * @param <T>        The annotation type.
         * @param <S>        The type of the serialized instance.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation, S extends Serializable> WithCustomMapping bindSerialized(Class<T> type, S value, Class<? super S> targetType) {
            return bind(OffsetMapping.ForSerializedValue.Factory.of(type, value, targetType));
        }

        /**
         * Binds the supplied annotation to the annotation's property of the specified name.
         *
         * @param type     The type of the annotation being bound.
         * @param property The name of the annotation property to be bound.
         * @param <T>      The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindProperty(Class<T> type, String property) {
            return bind(OffsetMapping.ForStackManipulation.OfAnnotationProperty.of(type, property));
        }

        /**
         * Binds the supplied annotation to the given Java constant.
         *
         * @param type     The type of the annotation being bound.
         * @param constant The Java constant that is bound.
         * @param <T>      The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, JavaConstant constant) {
            return bind(type, (ConstantValue) constant);
        }

        /**
         * Binds the supplied annotation to the given Java constant.
         *
         * @param type     The type of the annotation being bound.
         * @param constant The constant value that is bound.
         * @param <T>      The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, ConstantValue constant) {
            return bind(new OffsetMapping.ForStackManipulation.Factory<T>(type, constant.toStackManipulation(), constant.getTypeDescription().asGenericType()));
        }

        /**
         * Binds the supplied annotation to the annotation's property of the specified name.
         *
         * @param type              The type of the annotation being bound.
         * @param stackManipulation The stack manipulation loading the bound value.
         * @param targetType        The type of the loaded value.
         * @param <T>               The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, StackManipulation stackManipulation, java.lang.reflect.Type targetType) {
            return bind(type, stackManipulation, TypeDefinition.Sort.describe(targetType));
        }

        /**
         * Binds the supplied annotation to the annotation's property of the specified name.
         *
         * @param type              The type of the annotation being bound.
         * @param stackManipulation The stack manipulation loading the bound value.
         * @param targetType        The type of the loaded value.
         * @param <T>               The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, StackManipulation stackManipulation, TypeDescription.Generic targetType) {
            return bind(new OffsetMapping.ForStackManipulation.Factory<T>(type, stackManipulation, targetType));
        }

        /**
         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
         *
         * @param type                The type of the annotation being bound.
         * @param constructor         The constructor being bound as the lambda expression's implementation.
         * @param functionalInterface The functional interface that represents the lambda expression.
         * @param <T>                 The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type,
                                                                   Constructor<?> constructor,
                                                                   Class<?> functionalInterface) {
            return bindLambda(type,
                    new MethodDescription.ForLoadedConstructor(constructor),
                    TypeDescription.ForLoadedType.of(functionalInterface));
        }

        /**
         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
         *
         * @param type                The type of the annotation being bound.
         * @param constructor         The constructor being bound as the lambda expression's implementation.
         * @param functionalInterface The functional interface that represents the lambda expression.
         * @param methodGraphCompiler The method graph compiler that resolves the functional method of the function interface.
         * @param <T>                 The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type,
                                                                   Constructor<?> constructor,
                                                                   Class<?> functionalInterface,
                                                                   MethodGraph.Compiler methodGraphCompiler) {
            return bindLambda(type,
                    new MethodDescription.ForLoadedConstructor(constructor),
                    TypeDescription.ForLoadedType.of(functionalInterface),
                    methodGraphCompiler);
        }

        /**
         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
         *
         * @param type                The type of the annotation being bound.
         * @param method              The method being bound as the lambda expression's implementation.
         * @param functionalInterface The functional interface that represents the lambda expression.
         * @param <T>                 The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type,
                                                                   Method method,
                                                                   Class<?> functionalInterface) {
            return bindLambda(type,
                    new MethodDescription.ForLoadedMethod(method),
                    TypeDescription.ForLoadedType.of(functionalInterface));
        }

        /**
         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
         *
         * @param type                The type of the annotation being bound.
         * @param method              The method being bound as the lambda expression's implementation.
         * @param functionalInterface The functional interface that represents the lambda expression.
         * @param methodGraphCompiler The method graph compiler that resolves the functional method of the function interface.
         * @param <T>                 The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type,
                                                                   Method method,
                                                                   Class<?> functionalInterface,
                                                                   MethodGraph.Compiler methodGraphCompiler) {
            return bindLambda(type,
                    new MethodDescription.ForLoadedMethod(method),
                    TypeDescription.ForLoadedType.of(functionalInterface),
                    methodGraphCompiler);
        }

        /**
         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
         *
         * @param type                The type of the annotation being bound.
         * @param methodDescription   The method or constructor being bound as the lambda expression's implementation.
         * @param functionalInterface The functional interface that represents the lambda expression.
         * @param <T>                 The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type,
                                                                   MethodDescription.InDefinedShape methodDescription,
                                                                   TypeDescription functionalInterface) {
            return bindLambda(type, methodDescription, functionalInterface, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Binds the supplied annotation as a lambda expression via the JVM's lambda metafactory.
         *
         * @param type                The type of the annotation being bound.
         * @param methodDescription   The method or constuctor being bound as the lambda expression's implementation.
         * @param functionalInterface The functional interface that represents the lambda expression.
         * @param methodGraphCompiler The method graph compiler that resolves the functional method of the function interface.
         * @param <T>                 The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindLambda(Class<T> type,
                                                                   MethodDescription.InDefinedShape methodDescription,
                                                                   TypeDescription functionalInterface,
                                                                   MethodGraph.Compiler methodGraphCompiler) {
            if (!functionalInterface.isInterface()) {
                throw new IllegalArgumentException(functionalInterface + " is not an interface type");
            }
            MethodList<?> methods = methodGraphCompiler.compile((TypeDefinition) functionalInterface)
                    .listNodes()
                    .asMethodList()
                    .filter(isAbstract());
            if (methods.size() != 1) {
                throw new IllegalArgumentException(functionalInterface + " does not define exactly one abstract method: " + methods);
            }
            return bindDynamic(type, new MethodDescription.Latent(new TypeDescription.Latent("java.lang.invoke.LambdaMetafactory",
                            Opcodes.ACC_PUBLIC,
                            TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)),
                            "metafactory",
                            Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                            Collections.<TypeVariableToken>emptyList(),
                            JavaType.CALL_SITE.getTypeStub().asGenericType(),
                            Arrays.asList(new ParameterDescription.Token(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().asGenericType()),
                                    new ParameterDescription.Token(TypeDescription.ForLoadedType.of(String.class).asGenericType()),
                                    new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType()),
                                    new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType()),
                                    new ParameterDescription.Token(JavaType.METHOD_HANDLE.getTypeStub().asGenericType()),
                                    new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType())),
                            Collections.<TypeDescription.Generic>emptyList(),
                            Collections.<AnnotationDescription>emptyList(),
                            AnnotationValue.UNDEFINED,
                            TypeDescription.Generic.UNDEFINED),
                    JavaConstant.MethodType.ofSignature(methods.asDefined().getOnly()),
                    JavaConstant.MethodHandle.of(methodDescription),
                    JavaConstant.MethodType.ofSignature(methods.asDefined().getOnly()));
        }

        /**
         * Binds the supplied annotation to a dynamically bootstrapped value.
         *
         * @param type            The type of the annotation being bound.
         * @param bootstrapMethod The bootstrap method returning the call site.
         * @param constant        The arguments supplied to the bootstrap method.
         * @param <T>             The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, Method bootstrapMethod, Object... constant) {
            return bindDynamic(type, bootstrapMethod, Arrays.asList(constant));
        }

        /**
         * Binds the supplied annotation to a dynamically bootstrapped value.
         *
         * @param type            The type of the annotation being bound.
         * @param bootstrapMethod The bootstrap method returning the call site.
         * @param constants       The arguments supplied to the bootstrap method.
         * @param <T>             The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, Method bootstrapMethod, List<?> constants) {
            return bindDynamic(type, new MethodDescription.ForLoadedMethod(bootstrapMethod), constants);
        }

        /**
         * Binds the supplied annotation to a dynamically bootstrapped value.
         *
         * @param type            The type of the annotation being bound.
         * @param bootstrapMethod The bootstrap constructor returning the call site.
         * @param constant        The arguments supplied to the bootstrap method.
         * @param <T>             The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, Constructor<?> bootstrapMethod, Object... constant) {
            return bindDynamic(type, bootstrapMethod, Arrays.asList(constant));
        }

        /**
         * Binds the supplied annotation to a dynamically bootstrapped value.
         *
         * @param type            The type of the annotation being bound.
         * @param bootstrapMethod The bootstrap constructor returning the call site.
         * @param constants       The arguments supplied to the bootstrap method.
         * @param <T>             The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, Constructor<?> bootstrapMethod, List<?> constants) {
            return bindDynamic(type, new MethodDescription.ForLoadedConstructor(bootstrapMethod), constants);
        }

        /**
         * Binds the supplied annotation to a dynamically bootstrapped value.
         *
         * @param type            The type of the annotation being bound.
         * @param bootstrapMethod The bootstrap method or constructor returning the call site.
         * @param constant        The arguments supplied to the bootstrap method.
         * @param <T>             The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, MethodDescription.InDefinedShape bootstrapMethod, Object... constant) {
            return bindDynamic(type, bootstrapMethod, Arrays.asList(constant));
        }

        /**
         * Binds the supplied annotation to a dynamically bootstrapped value.
         *
         * @param type            The type of the annotation being bound.
         * @param bootstrapMethod The bootstrap method or constructor returning the call site.
         * @param constants       The arguments supplied to the bootstrap method.
         * @param <T>             The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bindDynamic(Class<T> type, MethodDescription.InDefinedShape bootstrapMethod, List<?> constants) {
            List<JavaConstant> arguments = JavaConstant.Simple.wrap(constants);
            if (!bootstrapMethod.isInvokeBootstrap(TypeList.Explicit.of(arguments))) {
                throw new IllegalArgumentException("Not a valid bootstrap method " + bootstrapMethod + " for " + arguments);
            }
            return bind(new OffsetMapping.ForStackManipulation.OfDynamicInvocation<T>(type, bootstrapMethod, arguments));
        }

        /**
         * Binds the supplied annotation to the annotation's property of the specified name.
         *
         * @param type          The type of the annotation being bound.
         * @param offsetMapping The offset mapping being bound.
         * @param <T>           The annotation type.
         * @return A new builder for an advice that considers the supplied annotation during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, OffsetMapping offsetMapping) {
            return bind(new OffsetMapping.Factory.Simple<T>(type, offsetMapping));
        }

        /**
         * Binds an annotation to a dynamically computed value. Whenever the {@link Advice} component discovers the given annotation on
         * a parameter of an advice method, the dynamic value is asked to provide a value that is then assigned to the parameter in question.
         *
         * @param offsetMapping The dynamic value that is computed for binding the parameter to a value.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public WithCustomMapping bind(OffsetMapping.Factory<?> offsetMapping) {
            Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings = new LinkedHashMap<Class<? extends Annotation>, OffsetMapping.Factory<?>>(this.offsetMappings);
            if (!offsetMapping.getAnnotationType().isAnnotation()) {
                throw new IllegalArgumentException("Not an annotation type: " + offsetMapping.getAnnotationType());
            } else if (offsetMappings.put(offsetMapping.getAnnotationType(), offsetMapping) != null) {
                throw new IllegalArgumentException("Annotation type already mapped: " + offsetMapping.getAnnotationType());
            }
            return new WithCustomMapping(postProcessorFactory, offsetMappings, delegatorFactory, classReaderFactory);
        }

        /**
         * Defines the supplied constructor as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
         * method arguments are:
         * <ul>
         * <li>A {@code java.lang.invoke.MethodHandles.Lookup} representing the source method.</li>
         * <li>A {@link String} representing the constructor's internal name {@code <init>}.</li>
         * <li>A {@code java.lang.invoke.MethodType} representing the type that is requested for binding.</li>
         * <li>A {@link String} of the target's binary class name.</li>
         * <li>A {@code int} with value {@code 0} for an enter advice and {code 1} for an exist advice.</li>
         * <li>A {@link Class} representing the class implementing the instrumented method.</li>
         * <li>A {@link String} with the name of the instrumented method.</li>
         * <li>A {@code java.lang.invoke.MethodHandle} representing the instrumented method unless the target is the type's static initializer.</li>
         * </ul>
         *
         * @param constructor The bootstrap constructor.
         * @return A new builder for an advice that uses the supplied constructor for bootstrapping.
         */
        public WithCustomMapping bootstrap(Constructor<?> constructor) {
            return bootstrap(new MethodDescription.ForLoadedConstructor(constructor));
        }

        /**
         * Defines the supplied constructor as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
         * method arguments are provided explicitly by the supplied resolver.
         *
         * @param constructor     The bootstrap constructor.
         * @param resolverFactory A resolver factory to provide the arguments to the bootstrap method.
         * @return A new builder for an advice that uses the supplied constructor for bootstrapping.
         */
        public WithCustomMapping bootstrap(Constructor<?> constructor, BootstrapArgumentResolver.Factory resolverFactory) {
            return bootstrap(new MethodDescription.ForLoadedConstructor(constructor), resolverFactory);
        }

        /**
         * Defines the supplied method as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
         * method arguments are:
         * <ul>
         * <li>A {@code java.lang.invoke.MethodHandles.Lookup} representing the source method.</li>
         * <li>A {@link String} representing the method's name.</li>
         * <li>A {@code java.lang.invoke.MethodType} representing the type that is requested for binding.</li>
         * <li>A {@link String} of the target's binary class name.</li>
         * <li>A {@code int} with value {@code 0} for an enter advice and {code 1} for an exist advice.</li>
         * <li>A {@link Class} representing the class implementing the instrumented method.</li>
         * <li>A {@link String} with the name of the instrumented method.</li>
         * <li>A {@code java.lang.invoke.MethodHandle} representing the instrumented method unless the target is the type's static initializer.</li>
         * </ul>
         *
         * @param method The bootstrap method.
         * @return A new builder for an advice that uses the supplied method for bootstrapping.
         */
        public WithCustomMapping bootstrap(Method method) {
            return bootstrap(new MethodDescription.ForLoadedMethod(method));
        }

        /**
         * Defines the supplied method as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
         * method arguments are provided explicitly by the supplied resolver.
         *
         * @param method   The bootstrap method.
         * @param resolver A resolver to provide the arguments to the bootstrap method.
         * @return A new builder for an advice that uses the supplied method for bootstrapping.
         */
        public WithCustomMapping bootstrap(Method method, BootstrapArgumentResolver.Factory resolver) {
            return bootstrap(new MethodDescription.ForLoadedMethod(method), resolver);
        }

        /**
         * Defines the supplied method as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
         * method arguments are provided explicitly by the supplied resolver.
         *
         * @param method   The bootstrap method.
         * @param resolver A resolver to provide the arguments to the bootstrap method.
         * @param visitor  A visitor to apply to the parameter types prior to resolving the {@code MethodType}
         *                 that is passed to the bootstrap method. The supplied types might not be available
         *                 to the instrumented type what might make it necessary to camouflage them to avoid
         *                 class loading errors. The actual type should then rather be passed in a different
         *                 format by the supplied {@link BootstrapArgumentResolver}.
         * @return A new builder for an advice that uses the supplied method for bootstrapping.
         */
        public WithCustomMapping bootstrap(Method method,
                                           BootstrapArgumentResolver.Factory resolver,
                                           TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
            return bootstrap(new MethodDescription.ForLoadedMethod(method), resolver, visitor);
        }

        /**
         * Defines the supplied method or constructor as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
         * method arguments are:
         * <ul>
         * <li>A {@code java.lang.invoke.MethodHandles.Lookup} representing the source method.</li>
         * <li>A {@link String} representing the method's name or constructor's internal name {@code <init>}.</li>
         * <li>A {@code java.lang.invoke.MethodType} representing the type that is requested for binding.</li>
         * <li>A {@link String} of the target's binary class name.</li>
         * <li>A {@code int} with value {@code 0} for an enter advice and {code 1} for an exist advice.</li>
         * <li>A {@link Class} representing the class implementing the instrumented method.</li>
         * <li>A {@link String} with the name of the instrumented method.</li>
         * <li>A {@code java.lang.invoke.MethodHandle} representing the instrumented method unless the target is the type's static initializer.</li>
         * </ul>
         *
         * @param bootstrap The bootstrap method or constructor.
         * @return A new builder for an advice that uses the supplied method or constructor for bootstrapping.
         */
        public WithCustomMapping bootstrap(MethodDescription.InDefinedShape bootstrap) {
            return bootstrap(bootstrap, BootstrapArgumentResolver.ForDefaultValues.Factory.INSTANCE);
        }

        /**
         * Defines the supplied method or constructor as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
         * method arguments are provided explicitly by the supplied resolver.
         *
         * @param bootstrap       The bootstrap method or constructor.
         * @param resolverFactory A factory for a resolver to provide arguments to the bootstrap method.
         * @return A new builder for an advice that uses the supplied method or constructor for bootstrapping.
         */
        public WithCustomMapping bootstrap(MethodDescription.InDefinedShape bootstrap, BootstrapArgumentResolver.Factory resolverFactory) {
            return bootstrap(bootstrap, resolverFactory, TypeDescription.Generic.Visitor.NoOp.INSTANCE);
        }

        /**
         * Defines the supplied method or constructor as a dynamic invocation bootstrap target for delegating advice methods. The bootstrap
         * method arguments are provided explicitly by the supplied resolver.
         *
         * @param bootstrap       The bootstrap method or constructor.
         * @param resolverFactory A factory for a resolver to provide arguments to the bootstrap method.
         * @param visitor         A visitor to apply to the parameter types prior to resolving the {@code MethodType}
         *                        that is passed to the bootstrap method. The supplied types might not be available
         *                        to the instrumented type what might make it necessary to camouflage them to avoid
         *                        class loading errors. The actual type should then rather be passed in a different
         *                        format by the supplied {@link BootstrapArgumentResolver}.
         * @return A new builder for an advice that uses the supplied method or constructor for bootstrapping.
         */
        public WithCustomMapping bootstrap(MethodDescription.InDefinedShape bootstrap,
                                           BootstrapArgumentResolver.Factory resolverFactory,
                                           TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
            return new WithCustomMapping(postProcessorFactory,
                    offsetMappings,
                    Delegator.ForDynamicInvocation.of(bootstrap, resolverFactory, visitor),
                    classReaderFactory);
        }

        /**
         * Adds the supplied post processor factory for advice method post processing.
         *
         * @param postProcessorFactory The post processor factory to add.
         * @return A new builder for an advice that applies the supplied post processor factory.
         */
        public WithCustomMapping with(PostProcessor.Factory postProcessorFactory) {
            return new WithCustomMapping(new PostProcessor.Factory.Compound(this.postProcessorFactory, postProcessorFactory),
                    offsetMappings,
                    delegatorFactory,
                    classReaderFactory);
        }

        /**
         * Defines the factory for producing class readers.
         *
         * @param classReaderFactory The class reader factory to use.
         * @return A new builder for an advice that applies the supplied post processor factory.
         */
        public WithCustomMapping with(AsmClassReader.Factory classReaderFactory) {
            return new WithCustomMapping(postProcessorFactory, offsetMappings, delegatorFactory, classReaderFactory);
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods. The advices binary representation is
         * accessed by querying the class loader of the supplied class for a class file.
         *
         * @param advice The type declaring the advice.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(Class<?> advice) {
            return to(advice, ClassFileLocator.ForClassLoader.of(advice.getClassLoader()));
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods.
         *
         * @param advice           The type declaring the advice.
         * @param classFileLocator The class file locator for locating the advisory class's class file.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(Class<?> advice, ClassFileLocator classFileLocator) {
            return to(TypeDescription.ForLoadedType.of(advice), classFileLocator);
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods.
         *
         * @param advice           A description of the type declaring the advice.
         * @param classFileLocator The class file locator for locating the advisory class's class file.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(TypeDescription advice, ClassFileLocator classFileLocator) {
            return Advice.to(advice,
                    postProcessorFactory,
                    classFileLocator,
                    new ArrayList<OffsetMapping.Factory<?>>(offsetMappings.values()),
                    delegatorFactory,
                    classReaderFactory);
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods. The advices binary representation is
         * accessed by querying the class loader of the supplied class for a class file.
         *
         * @param enterAdvice The type declaring the enter advice.
         * @param exitAdvice  The type declaring the exit advice.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(Class<?> enterAdvice, Class<?> exitAdvice) {
            ClassLoader enterLoader = enterAdvice.getClassLoader(), exitLoader = exitAdvice.getClassLoader();
            return to(enterAdvice, exitAdvice, enterLoader == exitLoader
                    ? ClassFileLocator.ForClassLoader.of(enterLoader)
                    : new ClassFileLocator.Compound(ClassFileLocator.ForClassLoader.of(enterLoader), ClassFileLocator.ForClassLoader.of(exitLoader)));
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods.
         *
         * @param enterAdvice      The type declaring the enter advice.
         * @param exitAdvice       The type declaring the exit advice.
         * @param classFileLocator The class file locator for locating the advisory class's class file.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(Class<?> enterAdvice, Class<?> exitAdvice, ClassFileLocator classFileLocator) {
            return to(TypeDescription.ForLoadedType.of(enterAdvice), TypeDescription.ForLoadedType.of(exitAdvice), classFileLocator);
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods. Using this method, a non-operational
         * class file locator is specified for the advice target. This implies that only advice targets with the <i>inline</i> target set
         * to {@code false} are resolvable by the returned instance.
         *
         * @param enterAdvice The type declaring the enter advice.
         * @param exitAdvice  The type declaring the exit advice.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(TypeDescription enterAdvice, TypeDescription exitAdvice) {
            return to(enterAdvice, exitAdvice, ClassFileLocator.NoOp.INSTANCE);
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods.
         *
         * @param enterAdvice      The type declaring the enter advice.
         * @param exitAdvice       The type declaring the exit advice.
         * @param classFileLocator The class file locator for locating the advisory class's class file.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(TypeDescription enterAdvice, TypeDescription exitAdvice, ClassFileLocator classFileLocator) {
            return Advice.to(enterAdvice,
                    exitAdvice,
                    postProcessorFactory,
                    classFileLocator,
                    new ArrayList<OffsetMapping.Factory<?>>(offsetMappings.values()),
                    delegatorFactory,
                    classReaderFactory);
        }
    }

    /**
     * A marker class that indicates that an advice method does not suppress any {@link Throwable}.
     */
    private static class NoExceptionHandler extends Throwable {

        /**
         * The class's serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * A description of the {@link NoExceptionHandler} type.
         */
        private static final TypeDescription DESCRIPTION = TypeDescription.ForLoadedType.of(NoExceptionHandler.class);

        /**
         * A private constructor as this class is not supposed to be invoked.
         */
        private NoExceptionHandler() {
            throw new UnsupportedOperationException("This class only serves as a marker type and should not be instantiated");
        }
    }

    /**
     * A marker type to be used as an argument for {@link OnMethodEnter#skipOn()}. If this value is set, the instrumented method
     * is not invoked if the annotated advice method <b>returns a default value</b>. A default value is {@code false} for a
     * {@code boolean} type, {@code 0} for a {@code byte}, {@code short}, {@code char}, {@code int}, {@code long}, {@code float}
     * or {@code double} type and {@code null} for a reference type. It is illegal to use this value if the advice method
     * returns {@code void}.
     */
    public static final class OnDefaultValue {

        /**
         * A private constructor as this class is not supposed to be invoked.
         */
        private OnDefaultValue() {
            throw new UnsupportedOperationException("This class only serves as a marker type and should not be instantiated");
        }
    }

    /**
     * A marker type to be used as an argument for {@link OnMethodEnter#skipOn()}. If this value is set, the instrumented method
     * is not invoked if the annotated advice method <b>returns a non-default value</b>. A default value is {@code false} for a
     * {@code boolean} type, {@code 0} for a {@code byte}, {@code short}, {@code char}, {@code int}, {@code long}, {@code float}
     * or {@code double} type and {@code null} for a reference type. It is illegal to use this value if the advice method
     * returns {@code void}.
     */
    public static final class OnNonDefaultValue {

        /**
         * A private constructor as this class is not supposed to be invoked.
         */
        private OnNonDefaultValue() {
            throw new UnsupportedOperationException("This class only serves as a marker type and should not be instantiated");
        }
    }
}

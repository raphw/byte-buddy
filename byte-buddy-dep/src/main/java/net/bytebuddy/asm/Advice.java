package net.bytebuddy.asm;

import com.google.auto.value.AutoValue;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
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
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.JavaType;
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
 * from the constant pool onto the operand stack. These instructions can however easily be transformerd for classes compiled to Java 4 and older
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
@AutoValue
public class Advice implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper, Implementation {

    /**
     * Indicates that no class reader is available to an adice method.
     */
    private static final ClassReader UNDEFINED = null;

    /**
     * A reference to the {@link OnMethodEnter#inline()} method.
     */
    private static final MethodDescription.InDefinedShape INLINE_ENTER;

    /**
     * A reference to the {@link OnMethodEnter#suppress()} method.
     */
    private static final MethodDescription.InDefinedShape SUPPRESS_ENTER;

    /**
     * A reference to the {@link OnMethodEnter#prependLineNumber()} method.
     */
    private static final MethodDescription.InDefinedShape PREPEND_LINE_NUMBER;

    /**
     * A reference to the {@link OnMethodEnter#skipOn()} method.
     */
    private static final MethodDescription.InDefinedShape SKIP_ON;

    /**
     * A reference to the {@link OnMethodExit#inline()} method.
     */
    private static final MethodDescription.InDefinedShape INLINE_EXIT;

    /**
     * A reference to the {@link OnMethodExit#suppress()} method.
     */
    private static final MethodDescription.InDefinedShape SUPPRESS_EXIT;

    /**
     * A reference to the {@link OnMethodExit#onThrowable()} method.
     */
    private static final MethodDescription.InDefinedShape ON_THROWABLE;

    /*
     * Extracts the annotation values for the enter and exit advice annotations.
     */
    static {
        MethodList<MethodDescription.InDefinedShape> enter = new TypeDescription.ForLoadedType(OnMethodEnter.class).getDeclaredMethods();
        INLINE_ENTER = enter.filter(named("inline")).getOnly();
        SUPPRESS_ENTER = enter.filter(named("suppress")).getOnly();
        SKIP_ON = enter.filter(named("skipOn")).getOnly();
        PREPEND_LINE_NUMBER = enter.filter(named("prependLineNumber")).getOnly();
        MethodList<MethodDescription.InDefinedShape> exit = new TypeDescription.ForLoadedType(OnMethodExit.class).getDeclaredMethods();
        INLINE_EXIT = exit.filter(named("inline")).getOnly();
        SUPPRESS_EXIT = exit.filter(named("suppress")).getOnly();
        ON_THROWABLE = exit.filter(named("onThrowable")).getOnly();
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
     * The stack manipulation to apply within a suppression handler.
     */
    private final StackManipulation exceptionHandler;

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
        this(methodEnter, methodExit, Assigner.DEFAULT, Removal.of(TypeDescription.THROWABLE), SuperMethodCall.INSTANCE);
    }

    /**
     * Creates a new advice.
     *
     * @param methodEnter      The dispatcher for instrumenting the instrumented method upon entering.
     * @param methodExit       The dispatcher for instrumenting the instrumented method upon exiting.
     * @param assigner         The assigner to use.
     * @param exceptionHandler The stack manipulation to apply within a suppression handler.
     * @param delegate         The delegate implementation to apply if this advice is used as an instrumentation.
     */
    private Advice(Dispatcher.Resolved.ForMethodEnter methodEnter,
                   Dispatcher.Resolved.ForMethodExit methodExit,
                   Assigner assigner,
                   StackManipulation exceptionHandler,
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
        return to(new TypeDescription.ForLoadedType(advice), classFileLocator);
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
        return to(advice, classFileLocator, Collections.<OffsetMapping.Factory<?>>emptyList());
    }

    /**
     * Creates a new advice.
     *
     * @param advice           A description of the type declaring the advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @param userFactories    A list of custom factories for user generated offset mappings.
     * @return A method visitor wrapper representing the supplied advice.
     */
    protected static Advice to(TypeDescription advice, ClassFileLocator classFileLocator, List<? extends OffsetMapping.Factory<?>> userFactories) {
        Dispatcher.Unresolved methodEnter = Dispatcher.Inactive.INSTANCE, methodExit = Dispatcher.Inactive.INSTANCE;
        for (MethodDescription.InDefinedShape methodDescription : advice.getDeclaredMethods()) {
            methodEnter = locate(OnMethodEnter.class, INLINE_ENTER, methodEnter, methodDescription);
            methodExit = locate(OnMethodExit.class, INLINE_EXIT, methodExit, methodDescription);
        }
        if (!methodEnter.isAlive() && !methodExit.isAlive()) {
            throw new IllegalArgumentException("No advice defined by " + advice);
        }
        try {
            ClassReader classReader = methodEnter.isBinary() || methodExit.isBinary()
                    ? new ClassReader(classFileLocator.locate(advice.getName()).resolve())
                    : UNDEFINED;
            Dispatcher.Resolved.ForMethodEnter resolved = methodEnter.asMethodEnter(userFactories, classReader);
            return new Advice(resolved, methodExit.asMethodExitTo(userFactories, classReader, resolved));
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
        return to(new TypeDescription.ForLoadedType(enterAdvice), new TypeDescription.ForLoadedType(exitAdvice), classFileLocator);
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
        return to(enterAdvice, exitAdvice, classFileLocator, Collections.<OffsetMapping.Factory<?>>emptyList());
    }

    /**
     * Creates a new advice.
     *
     * @param enterAdvice      The type declaring the enter advice.
     * @param exitAdvice       The type declaring the exit advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @param userFactories    A list of custom factories for user generated offset mappings.
     * @return A method visitor wrapper representing the supplied advice.
     */
    protected static Advice to(TypeDescription enterAdvice,
                               TypeDescription exitAdvice,
                               ClassFileLocator classFileLocator,
                               List<? extends OffsetMapping.Factory<?>> userFactories) {
        Dispatcher.Unresolved methodEnter = Dispatcher.Inactive.INSTANCE, methodExit = Dispatcher.Inactive.INSTANCE;
        for (MethodDescription.InDefinedShape methodDescription : enterAdvice.getDeclaredMethods()) {
            methodEnter = locate(OnMethodEnter.class, INLINE_ENTER, methodEnter, methodDescription);
        }
        if (!methodEnter.isAlive()) {
            throw new IllegalArgumentException("No enter advice defined by " + enterAdvice);
        }
        for (MethodDescription.InDefinedShape methodDescription : exitAdvice.getDeclaredMethods()) {
            methodExit = locate(OnMethodExit.class, INLINE_EXIT, methodExit, methodDescription);
        }
        if (!methodExit.isAlive()) {
            throw new IllegalArgumentException("No enter advice defined by " + exitAdvice);
        }
        try {
            Dispatcher.Resolved.ForMethodEnter resolved = methodEnter.asMethodEnter(userFactories, methodEnter.isBinary()
                    ? new ClassReader(classFileLocator.locate(enterAdvice.getName()).resolve())
                    : UNDEFINED);
            return new Advice(resolved, methodExit.asMethodExitTo(userFactories, methodExit.isBinary()
                    ? new ClassReader(classFileLocator.locate(exitAdvice.getName()).resolve())
                    : UNDEFINED, resolved));
        } catch (IOException exception) {
            throw new IllegalStateException("Error reading class file of " + enterAdvice + " or " + exitAdvice, exception);
        }
    }

    /**
     * Locates a dispatcher for the method if available.
     *
     * @param type              The annotation type that indicates a given form of advice that is currently resolved.
     * @param property          An annotation property that indicates if the advice method should be inlined.
     * @param dispatcher        Any previous dispatcher that was discovered or {@code null} if no such dispatcher was yet found.
     * @param methodDescription The method description that is considered as an advice method.
     * @return A resolved dispatcher or {@code null} if no dispatcher was resolved.
     */
    private static Dispatcher.Unresolved locate(Class<? extends Annotation> type,
                                                MethodDescription.InDefinedShape property,
                                                Dispatcher.Unresolved dispatcher,
                                                MethodDescription.InDefinedShape methodDescription) {
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
                    : new Dispatcher.Delegating(methodDescription);
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
        return new AsmVisitorWrapper.ForDeclaredMethods().method(matcher, this);
    }

    @Override
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
        methodVisitor = methodEnter.isPrependLineNumber()
                ? new LineNumberPrependingMethodVisitor(methodVisitor)
                : methodVisitor;
        if (!methodExit.isAlive()) {
            return new AdviceVisitor.WithoutExitAdvice(methodVisitor,
                    implementationContext,
                    assigner,
                    exceptionHandler,
                    instrumentedType,
                    instrumentedMethod,
                    methodEnter,
                    writerFlags,
                    readerFlags);
        } else if (methodExit.getThrowable().represents(NoExceptionHandler.class)) {
            return new AdviceVisitor.WithExitAdvice.WithoutExceptionHandling(methodVisitor,
                    implementationContext,
                    assigner,
                    exceptionHandler,
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
                    exceptionHandler,
                    instrumentedType,
                    instrumentedMethod,
                    methodEnter,
                    methodExit,
                    writerFlags,
                    readerFlags,
                    methodExit.getThrowable());
        }
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return delegate.prepare(instrumentedType);
    }

    @Override
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
        try {
            return withExceptionHandler(MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(Throwable.class.getMethod("printStackTrace"))));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Cannot locate Throwable::printStackTrace");
        }
    }

    /**
     * Configures this advice to execute the given stack manipulation upon a suppressed exception. The stack manipulation is executed with a
     * {@link Throwable} instance on the operand stack. The stack must be empty upon completing the exception handler.
     *
     * @param exceptionHandler The exception handler to apply.
     * @return A version of this advice that applies the supplied exception handler.
     */
    public Advice withExceptionHandler(StackManipulation exceptionHandler) {
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
         * @param context            The context in which the offset mapping is applied.
         * @return A suitable target mapping.
         */
        Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context);

        /**
         * A context for applying an {@link OffsetMapping}.
         */
        interface Context {

            /**
             * Returns {@code true} if the advice is applied on a fully initialized instance, i.e. describes if the {@code this}
             * instance is available or still uninitialized during calling the advice.
             *
             * @return {@code true} if the advice is applied onto a fully initialized method.
             */
            boolean isInitialized();

            /**
             * Returns the padding before writing additional values that this context applies.
             *
             * @return The required padding for this context.
             */
            int getPadding();

            /**
             * A context for an offset mapping describing a method entry.
             */
            enum ForMethodEntry implements Context {

                /**
                 * Describes a context for a method entry that is not a constructor.
                 */
                INITIALIZED(true),

                /**
                 * Describes a context for a method entry that is a constructor.
                 */
                NON_INITIALIZED(false);

                /**
                 * Resolves an appropriate method entry context for the supplied instrumented method.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @return An appropriate context.
                 */
                protected static Context of(MethodDescription instrumentedMethod) {
                    return instrumentedMethod.isConstructor()
                            ? NON_INITIALIZED
                            : INITIALIZED;
                }

                /**
                 * {@code true} if the method is no constructor, i.e. is invoked for an initialized instance upon entry.
                 */
                private final boolean initialized;

                /**
                 * Creates a new context for a method entry.
                 *
                 * @param initialized {@code true} if the method is no constructor, i.e. is invoked for an initialized instance upon entry.
                 */
                ForMethodEntry(boolean initialized) {
                    this.initialized = initialized;
                }

                @Override
                public boolean isInitialized() {
                    return initialized;
                }

                @Override
                public int getPadding() {
                    return StackSize.ZERO.getSize();
                }
            }

            /**
             * A context for an offset mapping describing a method exit.
             */
            enum ForMethodExit implements Context {

                /**
                 * A method exit with a zero sized padding.
                 */
                ZERO(StackSize.ZERO),

                /**
                 * A method exit with a single slot padding.
                 */
                SINGLE(StackSize.SINGLE),

                /**
                 * A method exit with a double slot padding.
                 */
                DOUBLE(StackSize.DOUBLE);

                /**
                 * The padding implied by this method exit.
                 */
                private final StackSize stackSize;

                /**
                 * Creates a new context for a method exit.
                 *
                 * @param stackSize The padding implied by this method exit.
                 */
                ForMethodExit(StackSize stackSize) {
                    this.stackSize = stackSize;
                }

                /**
                 * Resolves an appropriate method exit context for the supplied entry method type.
                 *
                 * @param typeDescription The type that is returned by the enter method.
                 * @return An appropriate context for the supplied entry method type.
                 */
                protected static Context of(TypeDefinition typeDescription) {
                    switch (typeDescription.getStackSize()) {
                        case ZERO:
                            return ZERO;
                        case SINGLE:
                            return SINGLE;
                        case DOUBLE:
                            return DOUBLE;
                        default:
                            throw new IllegalStateException("Unknown stack size: " + typeDescription);
                    }
                }

                @Override
                public boolean isInitialized() {
                    return true;
                }

                @Override
                public int getPadding() {
                    return stackSize.getSize();
                }
            }
        }

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

                @Override
                public StackManipulation resolveWrite() {
                    throw new IllegalStateException("Cannot write to read-only value");
                }

                @Override
                public StackManipulation resolveIncrement(int value) {
                    throw new IllegalStateException("Cannot write to read-only value");
                }
            }

            /**
             * A target for an offset mapping that represents a non-operational value. All writes are discarded and a value's
             * default value is returned upon every read.
             */
            @AutoValue
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

                @Override
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

                    @Override
                    public StackManipulation resolveWrite() {
                        throw new IllegalStateException("Cannot write to read-only default value");
                    }

                    @Override
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

                    @Override
                    public StackManipulation resolveWrite() {
                        return Removal.of(typeDefinition);
                    }

                    @Override
                    public StackManipulation resolveIncrement(int value) {
                        return StackManipulation.Trivial.INSTANCE;
                    }
                }
            }

            /**
             * A target for an offset mapping that represents a local variable.
             */
            @AutoValue
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

                @Override
                public StackManipulation resolveRead() {
                    return new StackManipulation.Compound(MethodVariableAccess.of(typeDefinition).loadFrom(offset), readAssignment);
                }

                /**
                 * A target for a read-only mapping of a local variable.
                 */
                public static class ReadOnly extends ForVariable {

                    /**
                     * Creates a read-only mapping for a method parameter.
                     *
                     * @param parameterDescription The mapped parameter.
                     */
                    public ReadOnly(ParameterDescription parameterDescription) {
                        this(parameterDescription, StackManipulation.Trivial.INSTANCE);
                    }

                    /**
                     * Creates a read-only mapping for a method parameter.
                     *
                     * @param parameterDescription The mapped parameter.
                     * @param readAssignment       An assignment to execute upon reading a value.
                     */
                    public ReadOnly(ParameterDescription parameterDescription, StackManipulation readAssignment) {
                        this(parameterDescription.getType(), parameterDescription.getOffset(), readAssignment);
                    }

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

                    @Override
                    public StackManipulation resolveWrite() {
                        throw new IllegalStateException("Cannot write to read-only parameter " + typeDefinition + " at " + offset);
                    }

                    @Override
                    public StackManipulation resolveIncrement(int value) {
                        throw new IllegalStateException("Cannot write to read-only variable " + typeDefinition + " at " + offset);
                    }
                }

                /**
                 * A target for a writable mapping of a local variable.
                 */
                @AutoValue
                public static class ReadWrite extends ForVariable {

                    /**
                     * A stack manipulation to apply upon a write to the variable.
                     */
                    private final StackManipulation writeAssignment;

                    /**
                     * Creates a new target mapping for a writable method parameter.
                     *
                     * @param parameterDescription The mapped parameter.
                     */
                    public ReadWrite(ParameterDescription parameterDescription) {
                        this(parameterDescription, StackManipulation.Trivial.INSTANCE, StackManipulation.Trivial.INSTANCE);
                    }

                    /**
                     * Creates a new target mapping for a writable method parameter.
                     *
                     * @param parameterDescription The mapped parameter.
                     * @param readAssignment       An assignment to execute upon reading a value.
                     * @param writeAssignment      A stack manipulation to apply upon a write to the variable.
                     */
                    public ReadWrite(ParameterDescription parameterDescription, StackManipulation readAssignment, StackManipulation writeAssignment) {
                        this(parameterDescription.getType(), parameterDescription.getOffset(), readAssignment, writeAssignment);
                    }

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

                    @Override
                    public StackManipulation resolveWrite() {
                        return new StackManipulation.Compound(writeAssignment, MethodVariableAccess.of(typeDefinition).storeAt(offset));
                    }

                    @Override
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
            @AutoValue
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

                @Override
                public StackManipulation resolveRead() {
                    return ArrayFactory.forType(target).withValues(valueReads);
                }

                @Override
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

                    @Override
                    public StackManipulation resolveWrite() {
                        throw new IllegalStateException("Cannot write to read-only array value");
                    }
                }

                /**
                 * A target mapping for a writable target mapping for an array of local variables.
                 */
                @AutoValue
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

                    @Override
                    public StackManipulation resolveWrite() {
                        return ArrayAccess.of(target).forEach(valueWrites);
                    }
                }
            }

            /**
             * A target for an offset mapping that loads a field value.
             */
            @AutoValue
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

                @Override
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

                    @Override
                    public StackManipulation resolveWrite() {
                        throw new IllegalStateException("Cannot write to read-only field value");
                    }

                    @Override
                    public StackManipulation resolveIncrement(int value) {
                        throw new IllegalStateException("Cannot write to read-only field value");
                    }
                }

                /**
                 * A mapping for a writable field.
                 */
                @AutoValue
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

                    @Override
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

                    @Override
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
            @AutoValue
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
                    return new ForStackManipulation(MethodConstant.forMethod(methodDescription));
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
                public static Target of(Object value) {
                    if (value == null) {
                        return new ForStackManipulation(NullConstant.INSTANCE);
                    } else if (value instanceof Boolean) {
                        return new ForStackManipulation(IntegerConstant.forValue((Boolean) value));
                    } else if (value instanceof Byte) {
                        return new ForStackManipulation(IntegerConstant.forValue((Byte) value));
                    } else if (value instanceof Short) {
                        return new ForStackManipulation(IntegerConstant.forValue((Short) value));
                    } else if (value instanceof Character) {
                        return new ForStackManipulation(IntegerConstant.forValue((Character) value));
                    } else if (value instanceof Integer) {
                        return new ForStackManipulation(IntegerConstant.forValue((Integer) value));
                    } else if (value instanceof Long) {
                        return new ForStackManipulation(LongConstant.forValue((Long) value));
                    } else if (value instanceof Float) {
                        return new ForStackManipulation(FloatConstant.forValue((Float) value));
                    } else if (value instanceof Double) {
                        return new ForStackManipulation(DoubleConstant.forValue((Double) value));
                    } else if (value instanceof String) {
                        return new ForStackManipulation(new TextConstant((String) value));
                    } else {
                        throw new IllegalArgumentException("Not a constant value: " + value);
                    }
                }

                @Override
                public StackManipulation resolveRead() {
                    return stackManipulation;
                }

                @Override
                public StackManipulation resolveWrite() {
                    throw new IllegalStateException("Cannot write to constant value: " + stackManipulation);
                }

                @Override
                public StackManipulation resolveIncrement(int value) {
                    throw new IllegalStateException("Cannot write to constant value: " + stackManipulation);
                }
            }
        }

        /**
         * Represents a factory for creating a {@link OffsetMapping} for a given parameter for a given annotation.
         *
         * @param <T> the annotation type that triggers this factory.
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
             * @param <T>
             */
            @AutoValue
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

                @Override
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    return offsetMapping;
                }
            }

            /**
             * A factory for an annotation whose use is not permitted.
             *
             * @param <T> The annotation type this factory binds.
             */
            @AutoValue
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

                @Override
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    throw new IllegalStateException("Usage of " + annotationType + " is not allowed on " + target);
                }
            }
        }

        /**
         * An offset mapping for a given parameter of the instrumented method.
         */
        @AutoValue
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                ParameterDescription parameterDescription = resolve(instrumentedMethod);
                StackManipulation readAssignment = assigner.assign(parameterDescription.getType(), target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + parameterDescription + " to " + target);
                } else if (readOnly) {
                    return new Target.ForVariable.ReadOnly(parameterDescription, readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, parameterDescription.getType(), typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + parameterDescription + " to " + target);
                    }
                    return new Target.ForVariable.ReadWrite(parameterDescription, readAssignment, writeAssignment);
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
            @AutoValue
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
                 * @param target   The target type.
                 * @param argument The annotation that triggers this binding.
                 */
                protected Unresolved(TypeDescription.Generic target, Argument argument) {
                    this(target, argument.readOnly(), argument.typing(), argument.value(), argument.optional());
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

                @Override
                public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                    if (optional && instrumentedMethod.getParameters().size() <= index) {
                        return readOnly
                                ? new Target.ForDefaultValue.ReadOnly(target)
                                : new Target.ForDefaultValue.ReadWrite(target);
                    }
                    return super.resolve(instrumentedType, instrumentedMethod, assigner, context);
                }

                /**
                 * A factory for a mapping of a parameter of the instrumented method.
                 */
                protected enum Factory implements OffsetMapping.Factory<Argument> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public Class<Argument> getAnnotationType() {
                        return Argument.class;
                    }

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<Argument> annotation,
                                              AdviceType adviceType) {
                        if (adviceType.isDelegation() && !annotation.loadSilent().readOnly()) {
                            throw new IllegalStateException("Cannot define writable field access for " + target + " when using delegation");
                        } else {
                            return new ForArgument.Unresolved(target.getType(), annotation.loadSilent());
                        }
                    }
                }
            }

            /**
             * An offset mapping for a specific parameter of the instrumented method.
             */
            @AutoValue
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
                @AutoValue
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

                    @Override
                    public Class<T> getAnnotationType() {
                        return annotationType;
                    }

                    @Override
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
        @AutoValue
        class ForThisReference implements OffsetMapping {

            /**
             * The offset of the {@code this} reference.
             */
            private static final int THIS_REFERENCE = 0;

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
            protected ForThisReference(TypeDescription.Generic target, This annotation) {
                this(target, annotation.readOnly(), annotation.typing(), annotation.optional());
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                if (instrumentedMethod.isStatic() || !context.isInitialized()) {
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
                    return new Target.ForVariable.ReadOnly(instrumentedType.asGenericType(), THIS_REFERENCE, readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, instrumentedType.asGenericType(), typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to " + instrumentedType);
                    }
                    return new Target.ForVariable.ReadWrite(instrumentedType.asGenericType(), THIS_REFERENCE, readAssignment, writeAssignment);
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

                @Override
                public Class<This> getAnnotationType() {
                    return This.class;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<This> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.loadSilent().readOnly()) {
                        throw new IllegalStateException("Cannot write to this reference for " + target + " in read-only context");
                    } else {
                        return new ForThisReference(target.getType(), annotation.loadSilent());
                    }
                }
            }
        }

        /**
         * An offset mapping that maps an array containing all arguments of the instrumented method.
         */
        @AutoValue
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
             * Creates a new offset mapping for an array containing all arguments.
             *
             * @param target     The component target type.
             * @param annotation The mapped annotation.
             */
            protected ForAllArguments(TypeDescription.Generic target, AllArguments annotation) {
                this(target, annotation.readOnly(), annotation.typing());
            }

            /**
             * Creates a new offset mapping for an array containing all arguments.
             *
             * @param target   The component target type.
             * @param readOnly {@code true} if the array is read-only.
             * @param typing   The typing to apply.
             */
            public ForAllArguments(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing) {
                this.target = target;
                this.readOnly = readOnly;
                this.typing = typing;
            }

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                List<StackManipulation> valueReads = new ArrayList<StackManipulation>(instrumentedMethod.getParameters().size());
                for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                    StackManipulation readAssignment = assigner.assign(parameterDescription.getType(), target, typing);
                    if (!readAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + parameterDescription + " to " + target);
                    }
                    valueReads.add(new StackManipulation.Compound(MethodVariableAccess.load(parameterDescription), readAssignment));
                }
                if (readOnly) {
                    return new Target.ForArray.ReadOnly(target, valueReads);
                } else {
                    List<StackManipulation> valueWrites = new ArrayList<StackManipulation>(instrumentedMethod.getParameters().size());
                    for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                        StackManipulation writeAssignment = assigner.assign(target, parameterDescription.getType(), typing);
                        if (!writeAssignment.isValid()) {
                            throw new IllegalStateException("Cannot assign " + target + " to " + parameterDescription);
                        }
                        valueWrites.add(new StackManipulation.Compound(writeAssignment, MethodVariableAccess.store(parameterDescription)));
                    }
                    return new Target.ForArray.ReadWrite(target, valueReads, valueWrites);
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

                @Override
                public Class<AllArguments> getAnnotationType() {
                    return AllArguments.class;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<AllArguments> annotation,
                                          AdviceType adviceType) {
                    if (!target.getType().represents(Object.class) && !target.getType().isArray()) {
                        throw new IllegalStateException("Cannot use AllArguments annotation on a non-array type");
                    } else if (adviceType.isDelegation() && !annotation.loadSilent().readOnly()) {
                        throw new IllegalStateException("Cannot define writable field access for " + target);
                    } else {
                        return new ForAllArguments(target.getType().represents(Object.class)
                                ? TypeDescription.Generic.OBJECT
                                : target.getType().getComponentType(), annotation.loadSilent());
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
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
            },

            /**
             * A constant that must be a {@link Constructor} instance.
             */
            CONSTRUCTOR {
                @Override
                protected boolean isRepresentable(MethodDescription instrumentedMethod) {
                    return instrumentedMethod.isConstructor();
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
            };

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                if (!isRepresentable(instrumentedMethod)) {
                    throw new IllegalStateException("Cannot represent " + instrumentedMethod + " as given method constant");
                }
                return Target.ForStackManipulation.of(instrumentedMethod.asDefined());
            }

            /**
             * Checks if the supplied method is representable for the assigned offset mapping.
             *
             * @param instrumentedMethod The instrumented method to represent.
             * @return {@code true} if this method is representable.
             */
            protected abstract boolean isRepresentable(MethodDescription instrumentedMethod);
        }

        /**
         * An offset mapping for a field.
         */
        @AutoValue
        abstract class ForField implements OffsetMapping {

            /**
             * The {@link FieldValue#value()} method.
             */
            private static final MethodDescription.InDefinedShape VALUE;

            /**
             * The {@link FieldValue#declaringType()}} method.
             */
            private static final MethodDescription.InDefinedShape DECLARING_TYPE;

            /**
             * The {@link FieldValue#readOnly()}} method.
             */
            private static final MethodDescription.InDefinedShape READ_ONLY;

            /**
             * The {@link FieldValue#typing()}} method.
             */
            private static final MethodDescription.InDefinedShape TYPING;

            /*
             * Looks up all annotation properties to avoid loading of the declaring field type.
             */
            static {
                MethodList<MethodDescription.InDefinedShape> methods = new TypeDescription.ForLoadedType(FieldValue.class).getDeclaredMethods();
                VALUE = methods.filter(named("value")).getOnly();
                DECLARING_TYPE = methods.filter(named("declaringType")).getOnly();
                READ_ONLY = methods.filter(named("readOnly")).getOnly();
                TYPING = methods.filter(named("typing")).getOnly();
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
            public ForField(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing) {
                this.target = target;
                this.readOnly = readOnly;
                this.typing = typing;
            }

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                FieldDescription fieldDescription = resolve(instrumentedType);
                if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot read non-static field " + fieldDescription + " from static method " + instrumentedMethod);
                } else if (!context.isInitialized() && !fieldDescription.isStatic()) {
                    throw new IllegalStateException("Cannot access non-static field before calling constructor: " + instrumentedMethod);
                }
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

            /**
             * Resolves the field being bound.
             *
             * @param instrumentedType The instrumented type.
             * @return The field being bound.
             */
            protected abstract FieldDescription resolve(TypeDescription instrumentedType);

            /**
             * An offset mapping for a field that is resolved from the instrumented type by its name.
             */
            @AutoValue
            public abstract static class Unresolved extends ForField {

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
                public Unresolved(TypeDescription.Generic target, boolean readOnly, Assigner.Typing typing, String name) {
                    super(target, readOnly, typing);
                    this.name = name;
                }

                @Override
                protected FieldDescription resolve(TypeDescription instrumentedType) {
                    FieldLocator.Resolution resolution = fieldLocator(instrumentedType).locate(name);
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
                                annotation.getValue(READ_ONLY).resolve(Boolean.class),
                                annotation.getValue(TYPING).loadSilent(Assigner.Typing.class.getClassLoader()).resolve(Assigner.Typing.class),
                                annotation.getValue(VALUE).resolve(String.class));
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
                @AutoValue
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
                                annotation.getValue(READ_ONLY).resolve(Boolean.class),
                                annotation.getValue(TYPING).loadSilent(Assigner.Typing.class.getClassLoader()).resolve(Assigner.Typing.class),
                                annotation.getValue(VALUE).resolve(String.class),
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

                    @Override
                    public Class<FieldValue> getAnnotationType() {
                        return FieldValue.class;
                    }

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<FieldValue> annotation,
                                              AdviceType adviceType) {
                        if (adviceType.isDelegation() && !annotation.getValue(ForField.READ_ONLY).resolve(Boolean.class)) {
                            throw new IllegalStateException("Cannot write to field for " + target + " in read-only context");
                        } else {
                            TypeDescription declaringType = annotation.getValue(DECLARING_TYPE).resolve(TypeDescription.class);
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
            @AutoValue
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
                protected FieldDescription resolve(TypeDescription instrumentedType) {
                    if (!fieldDescription.isStatic() && !fieldDescription.getDeclaringType().asErasure().isAssignableFrom(instrumentedType)) {
                        throw new IllegalStateException(fieldDescription + " is no member of " + instrumentedType);
                    } else if (!fieldDescription.isAccessibleTo(instrumentedType)) {
                        throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                    }
                    return fieldDescription;
                }

                /**
                 * A factory that binds a field.
                 *
                 * @param <T> The annotation type this factory binds.
                 */
                @AutoValue
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

                    @Override
                    public Class<T> getAnnotationType() {
                        return annotationType;
                    }

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                              AnnotationDescription.Loadable<T> annotation,
                                              AdviceType adviceType) {
                        return new Resolved(target.getType(), readOnly, typing, fieldDescription);
                    }
                }
            }
        }

        /**
         * An offset mapping for the {@link Advice.Origin} annotation.
         */
        @AutoValue
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
                            default:
                                throw new IllegalStateException("Illegal sort descriptor " + pattern.charAt(to + 1) + " for " + pattern);
                        }
                        from = to + 2;
                    }
                    renderers.add(new Renderer.ForConstantValue(pattern.substring(from)));
                    return new ForOrigin(renderers);
                }
            }

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
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

                    @Override
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

                    @Override
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

                    @Override
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

                    @Override
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

                    @Override
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

                    @Override
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return instrumentedMethod.toString();
                    }
                }

                /**
                 * A renderer for a constant value.
                 */
                @AutoValue
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

                    @Override
                    public String apply(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                        return value;
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

                @Override
                public Class<Origin> getAnnotationType() {
                    return Origin.class;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Origin> annotation,
                                          AdviceType adviceType) {
                    if (target.getType().asErasure().represents(Class.class)) {
                        return OffsetMapping.ForInstrumentedType.INSTANCE;
                    } else if (target.getType().asErasure().represents(Method.class)) {
                        return OffsetMapping.ForInstrumentedMethod.METHOD;
                    } else if (target.getType().asErasure().represents(Constructor.class)) {
                        return OffsetMapping.ForInstrumentedMethod.CONSTRUCTOR;
                    } else if (JavaType.EXECUTABLE.getTypeStub().equals(target.getType().asErasure())) {
                        return OffsetMapping.ForInstrumentedMethod.EXECUTABLE;
                    } else if (target.getType().asErasure().isAssignableFrom(String.class)) {
                        return ForOrigin.parse(annotation.loadSilent().value());
                    } else {
                        throw new IllegalStateException("Non-supported type " + target.getType() + " for @Origin annotation");
                    }
                }
            }
        }

        /**
         * An offset mapping for a parameter where assignments are fully ignored and that always return the parameter type's default value.
         */
        @AutoValue
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
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

                @Override
                public Class<Unused> getAnnotationType() {
                    return Unused.class;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Unused> annotation,
                                          AdviceType adviceType) {
                    return new ForUnusedValue(target.getType());
                }
            }
        }

        /**
         * An offset mapping for a parameter where assignments are fully ignored and that is assigned a boxed version of the instrumented
         * method's return valueor {@code null} if the return type is not primitive or {@code void}.
         */
        enum ForStubValue implements OffsetMapping, Factory<StubValue> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                return new Target.ForDefaultValue.ReadOnly(instrumentedMethod.getReturnType(), assigner.assign(instrumentedMethod.getReturnType(),
                        TypeDescription.Generic.OBJECT,
                        Assigner.Typing.DYNAMIC));
            }

            @Override
            public Class<StubValue> getAnnotationType() {
                return StubValue.class;
            }

            @Override
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
        @AutoValue
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
             * @param target    The represented target type.
             * @param enterType The enter type.
             * @param enter     The represented annotation.
             */
            protected ForEnterValue(TypeDescription.Generic target, TypeDescription.Generic enterType, Enter enter) {
                this(target, enterType, enter.readOnly(), enter.typing());
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                StackManipulation readAssignment = assigner.assign(enterType, target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + enterType + " to " + target);
                } else if (readOnly) {
                    return new Target.ForVariable.ReadOnly(target, instrumentedMethod.getStackSize(), readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, enterType, typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to " + enterType);
                    }
                    return new Target.ForVariable.ReadWrite(target, instrumentedMethod.getStackSize(), readAssignment, writeAssignment);
                }
            }

            /**
             * A factory for creating a {@link ForEnterValue} offset mapping.
             */
            @AutoValue
            protected static class Factory implements OffsetMapping.Factory<Enter> {

                /**
                 * The supplied type of the enter method.
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

                @Override
                public Class<Enter> getAnnotationType() {
                    return Enter.class;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Enter> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.loadSilent().readOnly()) {
                        throw new IllegalStateException("Cannot use writable " + target + " on read-only parameter");
                    } else {
                        return new ForEnterValue(target.getType(), enterType.asGenericType(), annotation.loadSilent());
                    }
                }
            }
        }

        /**
         * An offset mapping that provides access to the value that is returned by the instrumented method.
         */
        @AutoValue
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
            protected ForReturnValue(TypeDescription.Generic target, Return annotation) {
                this(target, annotation.readOnly(), annotation.typing());
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                int offset = instrumentedMethod.getStackSize() + context.getPadding();
                StackManipulation readAssignment = assigner.assign(instrumentedMethod.getReturnType(), target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + instrumentedMethod.getReturnType() + " to " + target);
                } else if (readOnly) {
                    return instrumentedMethod.getReturnType().represents(void.class)
                            ? new Target.ForDefaultValue.ReadOnly(target)
                            : new Target.ForVariable.ReadOnly(instrumentedMethod.getReturnType(), offset, readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, instrumentedMethod.getReturnType(), typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to " + instrumentedMethod.getReturnType());
                    }
                    return instrumentedMethod.getReturnType().represents(void.class)
                            ? new Target.ForDefaultValue.ReadWrite(target)
                            : new Target.ForVariable.ReadWrite(instrumentedMethod.getReturnType(), offset, readAssignment, writeAssignment);
                }
            }

            /**
             * A factory for creating a {@link ForReturnValue} offset mapping.
             */
            protected enum Factory implements OffsetMapping.Factory<Return> {

                /**
                 * The singelton instance.
                 */
                INSTANCE;

                @Override
                public Class<Return> getAnnotationType() {
                    return Return.class;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Return> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.loadSilent().readOnly()) {
                        throw new IllegalStateException("Cannot write return value for " + target + " in read-only context");
                    } else {
                        return new ForReturnValue(target.getType(), annotation.loadSilent());
                    }
                }
            }
        }

        /**
         * An offset mapping for accessing a {@link Throwable} of the instrumented method.
         */
        @AutoValue
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
            protected ForThrowable(TypeDescription.Generic target, Thrown annotation) {
                this(target, annotation.readOnly(), annotation.typing());
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                int offset = instrumentedMethod.getStackSize() + context.getPadding() + instrumentedMethod.getReturnType().getStackSize().getSize();
                StackManipulation readAssignment = assigner.assign(TypeDescription.THROWABLE.asGenericType(), target, typing);
                if (!readAssignment.isValid()) {
                    throw new IllegalStateException("Cannot assign Throwable to " + target);
                } else if (readOnly) {
                    return new Target.ForVariable.ReadOnly(TypeDescription.THROWABLE, offset, readAssignment);
                } else {
                    StackManipulation writeAssignment = assigner.assign(target, TypeDescription.THROWABLE.asGenericType(), typing);
                    if (!writeAssignment.isValid()) {
                        throw new IllegalStateException("Cannot assign " + target + " to Throwable");
                    }
                    return new Target.ForVariable.ReadWrite(TypeDescription.THROWABLE, offset, readAssignment, writeAssignment);
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
                 * Resolves an appropriate offset mapping factory for the {@link Thrown} parameter annotation.
                 *
                 * @param adviceMethod The exit advice method, annotated with {@link OnMethodExit}.
                 * @return An appropriate offset mapping factory.
                 */
                @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
                protected static OffsetMapping.Factory<?> of(MethodDescription.InDefinedShape adviceMethod) {
                    return adviceMethod.getDeclaredAnnotations()
                            .ofType(OnMethodExit.class)
                            .getValue(ON_THROWABLE)
                            .resolve(TypeDescription.class)
                            .represents(NoExceptionHandler.class) ? new OffsetMapping.Factory.Illegal<Thrown>(Thrown.class) : Factory.INSTANCE;
                }

                @Override
                public Class<Thrown> getAnnotationType() {
                    return Thrown.class;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target,
                                          AnnotationDescription.Loadable<Thrown> annotation,
                                          AdviceType adviceType) {
                    if (adviceType.isDelegation() && !annotation.loadSilent().readOnly()) {
                        throw new IllegalStateException("Cannot use writable " + target + " on read-only parameter");
                    } else {
                        return new ForThrowable(target.getType(), annotation.loadSilent());
                    }
                }
            }
        }

        /**
         * An offset mapping for binding a stack manipulation.
         */
        @AutoValue
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
                StackManipulation assigment = assigner.assign(typeDescription, targetType, typing);
                if (!assigment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + typeDescription + " to " + targetType);
                }
                return new Target.ForStackManipulation(new StackManipulation.Compound(stackManipulation, assigment));
            }

            /**
             * A factory that binds a stack manipulation.
             *
             * @param <T> The annotation type this factory binds.
             */
            @AutoValue
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
                    this(annotationType, ClassConstant.of(typeDescription), TypeDescription.CLASS.asGenericType());
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
                 * Creates a binding for a fixed {@link String} or primitive value.
                 *
                 * @param annotationType The annotation type.
                 * @param value          The primitive (wrapper) value or {@link String} value to bind.
                 * @param <S>            The annotation type.
                 * @return A factory for creating an offset mapping that binds the supplied value.
                 */
                public static <S extends Annotation> OffsetMapping.Factory<S> of(Class<S> annotationType, Object value) {
                    StackManipulation stackManipulation;
                    TypeDescription typeDescription;
                    if (value == null) {
                        return new OfDefaultValue<S>(annotationType);
                    } else if (value instanceof Boolean) {
                        stackManipulation = IntegerConstant.forValue((Boolean) value);
                        typeDescription = new TypeDescription.ForLoadedType(boolean.class);
                    } else if (value instanceof Byte) {
                        stackManipulation = IntegerConstant.forValue((Byte) value);
                        typeDescription = new TypeDescription.ForLoadedType(byte.class);
                    } else if (value instanceof Short) {
                        stackManipulation = IntegerConstant.forValue((Short) value);
                        typeDescription = new TypeDescription.ForLoadedType(short.class);
                    } else if (value instanceof Character) {
                        stackManipulation = IntegerConstant.forValue((Character) value);
                        typeDescription = new TypeDescription.ForLoadedType(char.class);
                    } else if (value instanceof Integer) {
                        stackManipulation = IntegerConstant.forValue((Integer) value);
                        typeDescription = new TypeDescription.ForLoadedType(int.class);
                    } else if (value instanceof Long) {
                        stackManipulation = LongConstant.forValue((Long) value);
                        typeDescription = new TypeDescription.ForLoadedType(long.class);
                    } else if (value instanceof Float) {
                        stackManipulation = FloatConstant.forValue((Float) value);
                        typeDescription = new TypeDescription.ForLoadedType(float.class);
                    } else if (value instanceof Double) {
                        stackManipulation = DoubleConstant.forValue((Double) value);
                        typeDescription = new TypeDescription.ForLoadedType(double.class);
                    } else if (value instanceof String) {
                        stackManipulation = new TextConstant((String) value);
                        typeDescription = TypeDescription.STRING;
                    } else {
                        throw new IllegalStateException("Not a constant value: " + value);
                    }
                    return new Factory<S>(annotationType, stackManipulation, typeDescription.asGenericType());
                }

                @Override
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                @Override
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
            @AutoValue
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

                @Override
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    return new ForStackManipulation(DefaultValue.of(target.getType()), target.getType(), target.getType(), Assigner.Typing.STATIC);
                }
            }

            /**
             * A factory for binding an annotation's property.
             *
             * @param <T> The annotation type this factory binds.
             */
            @AutoValue
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
                 * @param annotationType The annotion type to bind.
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

                @Override
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    Object value = annotation.getValue(property).resolve();
                    OffsetMapping.Factory<T> factory;
                    if (value instanceof TypeDescription) {
                        factory = new Factory<T>(annotationType, (TypeDescription) value);
                    } else if (value instanceof EnumerationDescription) {
                        factory = new Factory<T>(annotationType, (EnumerationDescription) value);
                    } else if (value instanceof AnnotationDescription) {
                        throw new IllegalStateException("Cannot bind annotation as fixed value for " + property);
                    } else {
                        factory = Factory.of(annotationType, value);
                    }
                    return factory.make(target, annotation, adviceType);
                }
            }
        }

        /**
         * An offset mapping that loads a serialized value.
         */
        @AutoValue
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

            @Override
            public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
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
            @AutoValue
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
                 * @return An appropriate offset mapping factory.
                 */
                public static <S extends Annotation> OffsetMapping.Factory<S> of(Class<S> annotationType, Serializable target, Class<?> targetType) {
                    if (!targetType.isInstance(target)) {
                        throw new IllegalArgumentException(target + " is no instance of " + targetType);
                    }
                    return new Factory<S>(annotationType, new TypeDescription.ForLoadedType(targetType), SerializedConstant.of(target));
                }

                @Override
                public Class<T> getAnnotationType() {
                    return annotationType;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
                    return new ForSerializedValue(target.getType(), typeDescription, deserialization);
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
             * Binds a method size handler for the entry advice.
             *
             * @param adviceMethod The method representing the entry advice.
             * @return A method size handler for the entry advice.
             */
            ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod);

            /**
             * Binds the method size handler for the exit advice.
             *
             * @param adviceMethod  The method representing the exit advice.
             * @param skipThrowable {@code true} if the exit advice is not invoked on an exception.
             * @return A method size handler for the exit advice.
             */
            ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod, boolean skipThrowable);

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
             * Records a minimum stack size required by the represented advice method.
             *
             * @param stackSize The minimum size required by the represented advice method.
             */
            void requireStackSize(int stackSize);

            /**
             * Records the maximum values for stack size and local variable array which are required by the advice method
             * for its individual execution without translation.
             *
             * @param stackSize           The minimum required stack size.
             * @param localVariableLength The minimum required length of the local variable array.
             */
            void recordMaxima(int stackSize, int localVariableLength);

            /**
             * Records a minimum padding additionally to the computed stack size that is required for implementing this advice method.
             *
             * @param padding The minimum required padding.
             */
            void recordPadding(int padding);
        }

        /**
         * A non-operational method size handler.
         */
        enum NoOp implements ForInstrumentedMethod, ForAdvice {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod) {
                return this;
            }

            @Override
            public ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod, boolean skipThrowable) {
                return this;
            }

            @Override
            public int compoundStackSize(int stackSize) {
                return UNDEFINED_SIZE;
            }

            @Override
            public int compoundLocalVariableLength(int localVariableLength) {
                return UNDEFINED_SIZE;
            }

            @Override
            public void requireLocalVariableLength(int localVariableLength) {
                /* do nothing */
            }

            @Override
            public void requireStackSize(int stackSize) {
                /* do nothing */
            }

            @Override
            public void recordMaxima(int stackSize, int localVariableLength) {
                /* do nothing */
            }

            @Override
            public void recordPadding(int padding) {
                /* do nothing */
            }
        }

        /**
         * A default implementation for a method size handler.
         */
        class Default implements MethodSizeHandler.ForInstrumentedMethod {

            /**
             * The instrumented method.
             */
            private final MethodDescription instrumentedMethod;

            /**
             * The list of types that the instrumented method requires in addition to the method parameters.
             */
            private final TypeList requiredTypes;

            /**
             * A list of types that are yielded by the instrumented method and available to the exit advice.
             */
            private final TypeList yieldedTypes;

            /**
             * The maximum stack size required by a visited advice method.
             */
            private int stackSize;

            /**
             * The maximum length of the local variable array required by a visited advice method.
             */
            private int localVariableLength;

            /**
             * Creates a new default meta data handler that recomputes the space requirements of an instrumented method.
             *
             * @param instrumentedMethod The instrumented method.
             * @param requiredTypes      The types this meta data handler expects to be available additionally to the instrumented method's parameters.
             * @param yieldedTypes       The types that are expected to be added after the instrumented method returns.
             */
            protected Default(MethodDescription instrumentedMethod, TypeList requiredTypes, TypeList yieldedTypes) {
                this.instrumentedMethod = instrumentedMethod;
                this.requiredTypes = requiredTypes;
                this.yieldedTypes = yieldedTypes;
            }

            /**
             * Creates a method size handler applicable for the given instrumented method.
             *
             * @param instrumentedMethod The instrumented method.
             * @param requiredTypes      The list of types that the instrumented method requires in addition to the method parameters.
             * @param yieldedTypes       A list of types that are yielded by the instrumented method and available to the exit advice.
             * @param writerFlags        The flags supplied to the ASM class writer.
             * @return An appropriate method size handler.
             */
            protected static MethodSizeHandler.ForInstrumentedMethod of(MethodDescription instrumentedMethod,
                                                                        List<? extends TypeDescription> requiredTypes,
                                                                        List<? extends TypeDescription> yieldedTypes,
                                                                        int writerFlags) {
                return (writerFlags & (ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)) != 0
                        ? NoOp.INSTANCE
                        : new Default(instrumentedMethod, new TypeList.Explicit(requiredTypes), new TypeList.Explicit(yieldedTypes));
            }

            @Override
            public MethodSizeHandler.ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod) {
                stackSize = Math.max(stackSize, adviceMethod.getReturnType().getStackSize().getSize());
                return new ForAdvice(adviceMethod, new TypeList.Empty(), new TypeList.Explicit(requiredTypes));
            }

            @Override
            public MethodSizeHandler.ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod, boolean skipThrowable) {
                stackSize = Math.max(stackSize, adviceMethod.getReturnType().getStackSize().maximum(skipThrowable
                        ? StackSize.ZERO
                        : StackSize.SINGLE).getSize());
                return new ForAdvice(adviceMethod, new TypeList.Explicit(CompoundList.of(requiredTypes, yieldedTypes)), new TypeList.Empty());
            }

            @Override
            public int compoundStackSize(int stackSize) {
                return Math.max(this.stackSize, stackSize);
            }

            @Override
            public int compoundLocalVariableLength(int localVariableLength) {
                return Math.max(this.localVariableLength, localVariableLength
                        + requiredTypes.getStackSize()
                        + yieldedTypes.getStackSize());
            }

            @Override
            public void requireLocalVariableLength(int localVariableLength) {
                this.localVariableLength = Math.max(this.localVariableLength, localVariableLength);
            }

            /**
             * A method size handler for an advice method.
             */
            protected class ForAdvice implements MethodSizeHandler.ForAdvice {

                /**
                 * The advice method.
                 */
                private final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * A list of types required by this advice method.
                 */
                private final TypeList requiredTypes;

                /**
                 * A list of types yielded by this advice method.
                 */
                private final TypeList yieldedTypes;

                /**
                 * The padding that this advice method requires additionally to its computed size.
                 */
                private int padding;

                /**
                 * Creates a new method size handler for an advice method.
                 *
                 * @param adviceMethod  The advice method.
                 * @param requiredTypes A list of types required by this advice method.
                 * @param yieldedTypes  A list of types yielded by this advice method.
                 */
                protected ForAdvice(MethodDescription.InDefinedShape adviceMethod, TypeList requiredTypes, TypeList yieldedTypes) {
                    this.adviceMethod = adviceMethod;
                    this.requiredTypes = requiredTypes;
                    this.yieldedTypes = yieldedTypes;
                    stackSize = Math.max(stackSize, adviceMethod.getReturnType().getStackSize().getSize());
                }

                @Override
                public void requireLocalVariableLength(int localVariableLength) {
                    Default.this.requireLocalVariableLength(localVariableLength);
                }

                @Override
                public void requireStackSize(int stackSize) {
                    Default.this.stackSize = Math.max(Default.this.stackSize, stackSize);
                }

                @Override
                public void recordMaxima(int stackSize, int localVariableLength) {
                    Default.this.stackSize = Math.max(Default.this.stackSize, stackSize) + padding;
                    Default.this.localVariableLength = Math.max(Default.this.localVariableLength, localVariableLength
                            - adviceMethod.getStackSize()
                            + instrumentedMethod.getStackSize()
                            + requiredTypes.getStackSize()
                            + yieldedTypes.getStackSize());
                }

                @Override
                public void recordPadding(int padding) {
                    this.padding = Math.max(this.padding, padding);
                }
            }
        }
    }

    /**
     * A handler for computing and translating stack map frames.
     */
    protected interface StackMapFrameHandler {

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
        void translateFrame(MethodVisitor methodVisitor, int type, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack);

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
         * @param secondary     {@code true} if another completion frame for this method was written previously.
         */
        void injectCompletionFrame(MethodVisitor methodVisitor, boolean secondary);

        /**
         * A stack map frame handler for an instrumented method.
         */
        interface ForInstrumentedMethod extends StackMapFrameHandler {

            /**
             * Binds this meta data handler for the entry advice.
             *
             * @param adviceMethod The entry advice method.
             * @return An appropriate meta data handler for the enter method.
             */
            ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod);

            /**
             * Binds this meta data handler for the exit advice.
             *
             * @param adviceMethod The exit advice method.
             * @return An appropriate meta data handler for the enter method.
             */
            ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod);

            /**
             * Returns a hint to supply to a {@link ClassReader} when parsing an advice method.
             *
             * @return The reader hint to supply to an ASM class reader.
             */
            int getReaderHint();
        }

        /**
         * A stack map frame handler for an advice method.
         */
        interface ForAdvice extends StackMapFrameHandler {
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

            @Override
            public StackMapFrameHandler.ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod) {
                return this;
            }

            @Override
            public StackMapFrameHandler.ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod) {
                return this;
            }

            @Override
            public int getReaderHint() {
                return ClassReader.SKIP_FRAMES;
            }

            @Override
            public void translateFrame(MethodVisitor methodVisitor,
                                       int type,
                                       int localVariableLength,
                                       Object[] localVariable,
                                       int stackSize,
                                       Object[] stack) {
                /* do nothing */
            }


            @Override
            public void injectReturnFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            @Override
            public void injectExceptionFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            @Override
            public void injectCompletionFrame(MethodVisitor methodVisitor, boolean secondary) {
                /* do nothing */
            }
        }

        /**
         * A default implementation of a stack map frame handler for an instrumented method.
         */
        class Default implements ForInstrumentedMethod {

            /**
             * An empty array indicating an empty frame.
             */
            private static final Object[] EMPTY = new Object[0];

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The instrumented method.
             */
            protected final MethodDescription instrumentedMethod;

            /**
             * A list of intermediate types to be considered as part of the instrumented method's steady signature.
             */
            protected final TypeList requiredTypes;

            /**
             * The types that are expected to be added after the instrumented method returns.
             */
            protected final TypeList yieldedTypes;

            /**
             * {@code true} if the meta data handler is expected to expand its frames.
             */
            private final boolean expandFrames;

            /**
             * The current frame's size divergence from the original local variable array.
             */
            private int currentFrameDivergence;

            /**
             * Creates a new default meta data handler.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param requiredTypes      A list of intermediate types to be considered as part of the instrumented method's steady signature.
             * @param yieldedTypes       The types that are expected to be added after the instrumented method returns.
             * @param expandFrames       {@code true} if the meta data handler is expected to expand its frames.
             */
            protected Default(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              TypeList requiredTypes,
                              TypeList yieldedTypes,
                              boolean expandFrames) {
                this.instrumentedType = instrumentedType;
                this.instrumentedMethod = instrumentedMethod;
                this.requiredTypes = requiredTypes;
                this.yieldedTypes = yieldedTypes;
                this.expandFrames = expandFrames;
            }

            /**
             * Creates an appropriate stack map frame handler for an instrumented method.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param requiredTypes      A list of intermediate types to be considered as part of the instrumented method's steady signature.
             * @param yieldedTypes       The types that are expected to be added after the instrumented method returns.
             * @param classFileVersion   The instrumented type's class file version.
             * @param writerFlags        The flags supplied to the ASM writier.
             * @param readerFlags        The reader flags supplied to the ASM reader.
             * @return An approrpiate stack map frame handler for an instrumented method.
             */
            protected static ForInstrumentedMethod of(TypeDescription instrumentedType,
                                                      MethodDescription instrumentedMethod,
                                                      List<? extends TypeDescription> requiredTypes,
                                                      List<? extends TypeDescription> yieldedTypes,
                                                      ClassFileVersion classFileVersion,
                                                      int writerFlags,
                                                      int readerFlags) {
                return (writerFlags & ClassWriter.COMPUTE_FRAMES) != 0 || classFileVersion.isLessThan(ClassFileVersion.JAVA_V6)
                        ? NoOp.INSTANCE
                        : new Default(instrumentedType, instrumentedMethod, new TypeList.Explicit(requiredTypes), new TypeList.Explicit(yieldedTypes), (readerFlags & ClassReader.EXPAND_FRAMES) != 0);
            }

            /**
             * Translates a type into a representation of its form inside a stack map frame.
             *
             * @param typeDescription The type to translate.
             * @return A stack entry representation of the supplied type.
             */
            protected static Object toFrame(TypeDescription typeDescription) {
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

            @Override
            public StackMapFrameHandler.ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod) {
                return new ForAdvice(adviceMethod, new TypeList.Empty(), requiredTypes, TranslationMode.ENTRY);
            }

            @Override
            public StackMapFrameHandler.ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod) {
                return new ForAdvice(adviceMethod, new TypeList.Explicit(CompoundList.of(requiredTypes, yieldedTypes)), new TypeList.Empty(), TranslationMode.EXIT);
            }

            @Override
            public int getReaderHint() {
                return expandFrames
                        ? ClassReader.EXPAND_FRAMES
                        : AsmVisitorWrapper.NO_FLAGS;
            }

            @Override
            public void translateFrame(MethodVisitor methodVisitor,
                                       int type,
                                       int localVariableLength,
                                       Object[] localVariable,
                                       int stackSize,
                                       Object[] stack) {
                translateFrame(methodVisitor,
                        TranslationMode.COPY,
                        instrumentedMethod,
                        requiredTypes,
                        type,
                        localVariableLength,
                        localVariable,
                        stackSize,
                        stack);
            }

            /**
             * Translates a frame.
             *
             * @param methodVisitor       The method visitor to write the frame to.
             * @param translationMode     The translation mode to apply.
             * @param methodDescription   The method description for which the frame is written.
             * @param additionalTypes     The additional types to consider part of the instrumented method's parameters.
             * @param type                The frame's type.
             * @param localVariableLength The local variable length.
             * @param localVariable       An array containing the types of the current local variables.
             * @param stackSize           The size of the operand stack.
             * @param stack               An array containing the types of the current operand stack.
             */
            protected void translateFrame(MethodVisitor methodVisitor,
                                          TranslationMode translationMode,
                                          MethodDescription methodDescription,
                                          TypeList additionalTypes,
                                          int type,
                                          int localVariableLength,
                                          Object[] localVariable,
                                          int stackSize,
                                          Object[] stack) {
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
                        if (methodDescription.getParameters().size() + (methodDescription.isStatic() ? 0 : 1) > localVariableLength) {
                            throw new IllegalStateException("Inconsistent frame length for " + methodDescription + ": " + localVariableLength);
                        }
                        int offset;
                        if (methodDescription.isStatic()) {
                            offset = 0;
                        } else {
                            if (!translationMode.isPossibleThisFrameValue(instrumentedType, instrumentedMethod, localVariable[0])) {
                                throw new IllegalStateException(methodDescription + " is inconsistent for 'this' reference: " + localVariable[0]);
                            }
                            offset = 1;
                        }
                        for (int index = 0; index < methodDescription.getParameters().size(); index++) {
                            if (!toFrame(methodDescription.getParameters().get(index).getType().asErasure()).equals(localVariable[index + offset])) {
                                throw new IllegalStateException(methodDescription + " is inconsistent at " + index + ": " + localVariable[index + offset]);
                            }
                        }
                        Object[] translated = new Object[localVariableLength
                                - methodDescription.getParameters().size()
                                - (methodDescription.isStatic() ? 0 : 1)
                                + instrumentedMethod.getParameters().size()
                                + (instrumentedMethod.isStatic() ? 0 : 1)
                                + additionalTypes.size()];
                        int index = translationMode.copy(instrumentedType, instrumentedMethod, methodDescription, localVariable, translated);
                        for (TypeDescription typeDescription : additionalTypes) {
                            translated[index++] = toFrame(typeDescription);
                        }
                        System.arraycopy(localVariable,
                                methodDescription.getParameters().size() + (methodDescription.isStatic() ? 0 : 1),
                                translated,
                                index,
                                translated.length - index);
                        localVariableLength = translated.length;
                        localVariable = translated;
                        currentFrameDivergence = translated.length - index;
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected frame type: " + type);
                }
                methodVisitor.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
            }

            @Override
            public void injectReturnFrame(MethodVisitor methodVisitor) {
                if (!expandFrames && currentFrameDivergence == 0 && !instrumentedMethod.isConstructor()) {
                    if (instrumentedMethod.getReturnType().represents(void.class)) {
                        methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                    } else {
                        methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{toFrame(instrumentedMethod.getReturnType().asErasure())});
                    }
                } else {
                    injectFullFrame(methodVisitor, requiredTypes, instrumentedMethod.getReturnType().represents(void.class)
                            ? Collections.<TypeDescription>emptyList()
                            : Collections.singletonList(instrumentedMethod.getReturnType().asErasure()));
                }
            }

            @Override
            public void injectExceptionFrame(MethodVisitor methodVisitor) {
                if (!expandFrames && currentFrameDivergence == 0) {
                    methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{Type.getInternalName(Throwable.class)});
                } else {
                    injectFullFrame(methodVisitor, requiredTypes, Collections.singletonList(TypeDescription.THROWABLE));
                }
            }

            @Override
            public void injectCompletionFrame(MethodVisitor methodVisitor, boolean secondary) {
                if (!expandFrames && currentFrameDivergence == 0 && (secondary || !instrumentedMethod.isConstructor())) {
                    if (secondary) {
                        methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                    } else {
                        Object[] local = new Object[yieldedTypes.size()];
                        int index = 0;
                        for (TypeDescription typeDescription : yieldedTypes) {
                            local[index++] = toFrame(typeDescription);
                        }
                        methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, EMPTY.length, EMPTY);
                    }
                } else {
                    injectFullFrame(methodVisitor, CompoundList.of(requiredTypes, yieldedTypes), Collections.<TypeDescription>emptyList());
                }
            }

            /**
             * Injects a full stack map frame.
             *
             * @param methodVisitor The method visitor onto which to write the stack map frame.
             * @param typesInArray  The types that were added to the local variable array additionally to the values of the instrumented method.
             * @param typesOnStack  The types currently on the operand stack.
             */
            protected void injectFullFrame(MethodVisitor methodVisitor,
                                           List<? extends TypeDescription> typesInArray,
                                           List<? extends TypeDescription> typesOnStack) {
                Object[] localVariable = new Object[instrumentedMethod.getParameters().size()
                        + (instrumentedMethod.isStatic() ? 0 : 1)
                        + typesInArray.size()];
                int index = 0;
                if (!instrumentedMethod.isStatic()) {
                    localVariable[index++] = toFrame(instrumentedType);
                }
                for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                    localVariable[index++] = toFrame(typeDescription);
                }
                for (TypeDescription typeDescription : typesInArray) {
                    localVariable[index++] = toFrame(typeDescription);
                }
                index = 0;
                Object[] stackType = new Object[typesOnStack.size()];
                for (TypeDescription typeDescription : typesOnStack) {
                    stackType[index++] = toFrame(typeDescription);
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
                                       MethodDescription methodDescription,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int length = instrumentedMethod.getParameters().size() + (instrumentedMethod.isStatic() ? 0 : 1);
                        System.arraycopy(localVariable, 0, translated, 0, length);
                        return length;
                    }

                    @Override
                    protected boolean isPossibleThisFrameValue(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Object frame) {
                        return instrumentedMethod.isConstructor() && Opcodes.UNINITIALIZED_THIS.equals(frame) || toFrame(instrumentedType).equals(frame);
                    }
                },

                /**
                 * A translation mode for the entry advice that considers that the {@code this} reference might not be initialized for a constructor.
                 */
                ENTRY {
                    @Override
                    protected int copy(TypeDescription instrumentedType,
                                       MethodDescription instrumentedMethod,
                                       MethodDescription methodDescription,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int index = 0;
                        if (!instrumentedMethod.isStatic()) {
                            translated[index++] = instrumentedMethod.isConstructor()
                                    ? Opcodes.UNINITIALIZED_THIS
                                    : toFrame(instrumentedType);
                        }
                        for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                            translated[index++] = toFrame(typeDescription);
                        }
                        return index;
                    }

                    @Override
                    protected boolean isPossibleThisFrameValue(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Object frame) {
                        return instrumentedMethod.isConstructor()
                                ? Opcodes.UNINITIALIZED_THIS.equals(frame)
                                : toFrame(instrumentedType).equals(frame);
                    }
                },

                /**
                 * A translation mode for an exit advice where the {@code this} reference is always initialized.
                 */
                EXIT {
                    @Override
                    protected int copy(TypeDescription instrumentedType,
                                       MethodDescription instrumentedMethod,
                                       MethodDescription methodDescription,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int index = 0;
                        if (!instrumentedMethod.isStatic()) {
                            translated[index++] = toFrame(instrumentedType);
                        }
                        for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                            translated[index++] = toFrame(typeDescription);
                        }
                        return index;
                    }

                    @Override
                    protected boolean isPossibleThisFrameValue(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Object frame) {
                        return toFrame(instrumentedType).equals(frame);
                    }
                };

                /**
                 * Copies the fixed parameters of the instrumented method onto the operand stack.
                 *
                 * @param instrumentedType   The instrumented type.
                 * @param instrumentedMethod The instrumented method.
                 * @param methodDescription  The method for which a frame is created.
                 * @param localVariable      The original local variable array.
                 * @param translated         The array containing the translated frames.
                 * @return The amount of frames added to the translated frame array.
                 */
                protected abstract int copy(TypeDescription instrumentedType,
                                            MethodDescription instrumentedMethod,
                                            MethodDescription methodDescription,
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
             * A stack map frame handler for an advice method.
             */
            protected class ForAdvice implements StackMapFrameHandler.ForAdvice {

                /**
                 * The method description for which frames are translated.
                 */
                protected final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * A list of intermediate types to be considered as part of the instrumented method's steady signature.
                 */
                protected final TypeList requiredTypes;

                /**
                 * The types that this method yields as a result.
                 */
                private final TypeList yieldedTypes;

                /**
                 * The translation mode to apply for this advice method. Should be either {@link TranslationMode#ENTRY} or {@link TranslationMode#EXIT}.
                 */
                protected final TranslationMode translationMode;

                /**
                 * Creates a new meta data handler for an advice method.
                 *
                 * @param adviceMethod    The method description for which frames are translated.
                 * @param requiredTypes   A list of expected types to be considered as part of the instrumented method's steady signature.
                 * @param yieldedTypes    The types that this method yields as a result.
                 * @param translationMode The translation mode to apply for this advice method. Should be
                 *                        either {@link TranslationMode#ENTRY} or {@link TranslationMode#EXIT}.
                 */
                protected ForAdvice(MethodDescription.InDefinedShape adviceMethod,
                                    TypeList requiredTypes,
                                    TypeList yieldedTypes,
                                    TranslationMode translationMode) {
                    this.adviceMethod = adviceMethod;
                    this.requiredTypes = requiredTypes;
                    this.yieldedTypes = yieldedTypes;
                    this.translationMode = translationMode;
                }

                @Override
                public void translateFrame(MethodVisitor methodVisitor,
                                           int type,
                                           int localVariableLength,
                                           Object[] localVariable,
                                           int stackSize,
                                           Object[] stack) {
                    Default.this.translateFrame(methodVisitor,
                            translationMode,
                            adviceMethod,
                            requiredTypes,
                            type,
                            localVariableLength,
                            localVariable,
                            stackSize,
                            stack);
                }

                @Override
                public void injectReturnFrame(MethodVisitor methodVisitor) {
                    if (!expandFrames && currentFrameDivergence == 0) {
                        if (yieldedTypes.isEmpty() || adviceMethod.getReturnType().represents(void.class)) {
                            methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                        } else {
                            methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{toFrame(adviceMethod.getReturnType().asErasure())});
                        }
                    } else {
                        injectFullFrame(methodVisitor, requiredTypes, yieldedTypes.isEmpty() || adviceMethod.getReturnType().represents(void.class)
                                ? Collections.<TypeDescription>emptyList()
                                : Collections.singletonList(adviceMethod.getReturnType().asErasure()));
                    }
                }

                @Override
                public void injectExceptionFrame(MethodVisitor methodVisitor) {
                    if (!expandFrames && currentFrameDivergence == 0) {
                        methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{Type.getInternalName(Throwable.class)});
                    } else {
                        injectFullFrame(methodVisitor, requiredTypes, Collections.singletonList(TypeDescription.THROWABLE));
                    }
                }

                @Override
                public void injectCompletionFrame(MethodVisitor methodVisitor, boolean secondary) {
                    if ((!expandFrames && currentFrameDivergence == 0 && yieldedTypes.size() < 4)) {
                        if (secondary || yieldedTypes.isEmpty()) {
                            methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                        } else {
                            Object[] local = new Object[yieldedTypes.size()];
                            int index = 0;
                            for (TypeDescription typeDescription : yieldedTypes) {
                                local[index++] = toFrame(typeDescription);
                            }
                            methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, EMPTY.length, EMPTY);
                        }
                    } else {
                        injectFullFrame(methodVisitor, CompoundList.of(requiredTypes, yieldedTypes), Collections.<TypeDescription>emptyList());
                    }
                }
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
        MethodVisitor IGNORE_METHOD = null;

        /**
         * Expresses that an annotation should not be visited.
         */
        AnnotationVisitor IGNORE_ANNOTATION = null;

        /**
         * Returns {@code true} if this dispatcher is alive.
         *
         * @return {@code true} if this dispatcher is alive.
         */
        boolean isAlive();

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
             * Resolves this dispatcher as a dispatcher for entering a method.
             *
             * @param userFactories A list of custom factories for binding parameters of an advice method.
             * @param classReader   A class reader to query for a class file which might be {@code null} if this dispatcher is not binary.
             * @return This dispatcher as a dispatcher for entering a method.
             */
            Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                  ClassReader classReader);

            /**
             * Resolves this dispatcher as a dispatcher for exiting a method.
             *
             * @param userFactories A list of custom factories for binding parameters of an advice method.
             * @param classReader   A class reader to query for a class file which might be {@code null} if this dispatcher is not binary.
             * @param dispatcher    The dispatcher for entering a method.
             * @return This dispatcher as a dispatcher for exiting a method.
             */
            Resolved.ForMethodExit asMethodExitTo(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                  ClassReader classReader,
                                                  Resolved.ForMethodEnter dispatcher);
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
             * A producer for a default return value if this is applicable.
             */
            interface ReturnValueProducer {

                /**
                 * Instructs this return value producer to assure the production of a default value for the return type of the currently handled method.
                 *
                 * @param methodVisitor The method visitor to write the default value to.
                 */
                void onDefaultValue(MethodVisitor methodVisitor);
            }

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
                 * @param returnValueProducer   A producer for defining a default return value of the advised method.
                 */
                void onEnd(MethodVisitor methodVisitor,
                           Implementation.Context implementationContext,
                           MethodSizeHandler.ForAdvice methodSizeHandler,
                           StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                           ReturnValueProducer returnValueProducer);

                /**
                 * Invoked at the end of a method. Additionally indicates that the handler block should be surrounding by a skipping instruction. This method
                 * is always followed by a stack map frame (if it is required for the class level and class writer setting).
                 *
                 * @param methodVisitor         The method visitor of the instrumented method.
                 * @param implementationContext The implementation context to use.
                 * @param methodSizeHandler     The advice method's method size handler.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param returnValueProducer   A producer for defining a default return value of the advised method.
                 */
                void onEndSkipped(MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  MethodSizeHandler.ForAdvice methodSizeHandler,
                                  StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                  ReturnValueProducer returnValueProducer);
            }

            /**
             * A non-operational suppression handler that does not suppress any method.
             */
            enum NoOp implements SuppressionHandler, Bound {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Bound bind(StackManipulation exceptionHandler) {
                    return this;
                }

                @Override
                public void onPrepare(MethodVisitor methodVisitor) {
                    /* do nothing */
                }

                @Override
                public void onStart(MethodVisitor methodVisitor) {
                    /* do nothing */
                }

                @Override
                public void onEnd(MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  MethodSizeHandler.ForAdvice methodSizeHandler,
                                  StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                  ReturnValueProducer returnValueProducer) {
                    /* do nothing */
                }

                @Override
                public void onEndSkipped(MethodVisitor methodVisitor,
                                         Implementation.Context implementationContext,
                                         MethodSizeHandler.ForAdvice methodSizeHandler,
                                         StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                         ReturnValueProducer returnValueProducer) {
                    /* do nothing */
                }
            }

            /**
             * A suppression handler that suppresses a given throwable type.
             */
            @AutoValue
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

                @Override
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

                    @Override
                    public void onPrepare(MethodVisitor methodVisitor) {
                        methodVisitor.visitTryCatchBlock(startOfMethod, endOfMethod, endOfMethod, suppressedType.getInternalName());
                    }

                    @Override
                    public void onStart(MethodVisitor methodVisitor) {
                        methodVisitor.visitLabel(startOfMethod);
                    }

                    @Override
                    public void onEnd(MethodVisitor methodVisitor,
                                      Implementation.Context implementationContext,
                                      MethodSizeHandler.ForAdvice methodSizeHandler,
                                      StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                      ReturnValueProducer returnValueProducer) {
                        methodVisitor.visitLabel(endOfMethod);
                        stackMapFrameHandler.injectExceptionFrame(methodVisitor);
                        methodSizeHandler.requireStackSize(1 + exceptionHandler.apply(methodVisitor, implementationContext).getMaximalSize());
                        returnValueProducer.onDefaultValue(methodVisitor);
                    }

                    @Override
                    public void onEndSkipped(MethodVisitor methodVisitor,
                                             Implementation.Context implementationContext,
                                             MethodSizeHandler.ForAdvice methodSizeHandler,
                                             StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                             ReturnValueProducer returnValueProducer) {
                        Label endOfHandler = new Label();
                        methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfHandler);
                        onEnd(methodVisitor, implementationContext, methodSizeHandler, stackMapFrameHandler, returnValueProducer);
                        methodVisitor.visitLabel(endOfHandler);
                    }
                }
            }
        }

        /**
         * Represents a resolved dispatcher.
         */
        interface Resolved extends Dispatcher {

            /**
             * Binds this dispatcher for resolution to a specific method.
             *
             * @param instrumentedType      The instrumented type.
             * @param instrumentedMethod    The instrumented method.
             * @param methodVisitor         The method visitor for writing the instrumented method.
             * @param implementationContext The implementation context to use.
             * @param assigner              The assigner to use.
             * @param methodSizeHandler     A handler for computing the method size requirements.
             * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
             * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
             * @return A dispatcher that is bound to the instrumented method.
             */
            Bound bind(TypeDescription instrumentedType,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor,
                       Implementation.Context implementationContext,
                       Assigner assigner,
                       MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                       StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                       StackManipulation exceptionHandler);

            /**
             * Represents a resolved dispatcher for entering a method.
             */
            interface ForMethodEnter extends Resolved {

                /**
                 * Returns the type that this dispatcher supplies as a result of its advice or a description of {@code void} if
                 * no type is supplied as a result of the enter advice.
                 *
                 * @return The type that this dispatcher supplies as a result of its advice or a description of {@code void}.
                 */
                TypeDefinition getEnterType();

                /**
                 * Returns {@code true} if the first discovered line number information should be prepended to the advice code.
                 *
                 * @return {@code true} if the first discovered line number information should be prepended to the advice code.
                 */
                boolean isPrependLineNumber();

                @Override
                Bound.ForMethodEnter bind(TypeDescription instrumentedType,
                                          MethodDescription instrumentedMethod,
                                          MethodVisitor methodVisitor,
                                          Implementation.Context implementationContext,
                                          Assigner assigner,
                                          MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                          StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                          StackManipulation exceptionHandler);

                /**
                 * A skip dispatcher is responsible for skipping the instrumented method depending on the
                 * return value of the enter advice method.
                 */
                interface SkipDispatcher {

                    /**
                     * Applies this skip dispatcher.
                     *
                     * @param methodVisitor        The method visitor to write to.
                     * @param methodSizeHandler    The method size handler of the advice method to use.
                     * @param stackMapFrameHandler The stack map frame handler of the advice method to use.
                     * @param instrumentedMethod   The instrumented method.
                     * @param skipHandler          The skip handler to use.
                     */
                    void apply(MethodVisitor methodVisitor,
                               MethodSizeHandler.ForAdvice methodSizeHandler,
                               StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                               MethodDescription instrumentedMethod,
                               Bound.SkipHandler skipHandler);

                    /**
                     * A disabled skip dispatcher where the instrumented method is always executed.
                     */
                    enum Disabled implements SkipDispatcher {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        @Override
                        public void apply(MethodVisitor methodVisitor,
                                          MethodSizeHandler.ForAdvice methodSizeHandler,
                                          StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                          MethodDescription instrumentedMethod,
                                          Bound.SkipHandler skipHandler) {
                            /* do nothing */
                        }
                    }

                    /**
                     * A skip dispatcher where the instrumented method is skipped for any default value of the advice method's return type.
                     * If the return type is {@code boolean}, the relationship is inversed, where the instrumented is skipped for a {@code true}
                     * return value.
                     */
                    enum ForValue implements SkipDispatcher {

                        /**
                         * A skip dispatcher for a {@code boolean}, {@code  byte}, {@code short}, {@code char} or {@code int} value.
                         */
                        FOR_INTEGER(Opcodes.ILOAD, Opcodes.IFNE, Opcodes.IFEQ) {
                            @Override
                            protected void convertValue(MethodVisitor methodVisitor, MethodSizeHandler.ForAdvice methodSizeHandler) {
                                /* do nothing */
                            }
                        },

                        /**
                         * A skip dispatcher for a {@code long} value.
                         */
                        FOR_LONG(Opcodes.LLOAD, Opcodes.IFNE, Opcodes.IFEQ) {
                            @Override
                            protected void convertValue(MethodVisitor methodVisitor, MethodSizeHandler.ForAdvice methodSizeHandler) {
                                methodVisitor.visitInsn(Opcodes.L2I);
                            }
                        },

                        /**
                         * A skip dispatcher for a {@code float} value.
                         */
                        FOR_FLOAT(Opcodes.FLOAD, Opcodes.IFNE, Opcodes.IFEQ) {
                            @Override
                            protected void convertValue(MethodVisitor methodVisitor, MethodSizeHandler.ForAdvice methodSizeHandler) {
                                methodVisitor.visitInsn(Opcodes.FCONST_0);
                                methodVisitor.visitInsn(Opcodes.FCMPL);
                                methodSizeHandler.requireStackSize(2);
                            }
                        },

                        /**
                         * A skip dispatcher for a {@code double} value.
                         */
                        FOR_DOUBLE(Opcodes.DLOAD, Opcodes.IFNE, Opcodes.IFEQ) {
                            @Override
                            protected void convertValue(MethodVisitor methodVisitor, MethodSizeHandler.ForAdvice methodSizeHandler) {
                                methodVisitor.visitInsn(Opcodes.DCONST_0);
                                methodVisitor.visitInsn(Opcodes.DCMPL);
                                methodSizeHandler.requireStackSize(4);
                            }
                        },

                        /**
                         * A skip dispatcher for a reference value.
                         */
                        FOR_REFERENCE(Opcodes.ALOAD, Opcodes.IFNONNULL, Opcodes.IFNULL) {
                            @Override
                            protected void convertValue(MethodVisitor methodVisitor, MethodSizeHandler.ForAdvice methodSizeHandler) {
                                /* do nothing */
                            }
                        };

                        /**
                         * The load opcode for this skip dispatcher.
                         */
                        private final int load;

                        /**
                         * The jump instruction that triggers skipping upon observing a value's default value.
                         */
                        private final int defaultJump;

                        /**
                         * The jump instruction that triggers skipping upon observing a value's non-default value.
                         */
                        private final int nonDefaultJump;

                        /**
                         * Creates a new skip dispatcher.
                         *
                         * @param load           The load opcode for this skip dispatcher.
                         * @param defaultJump    The jump instruction that triggers skipping upon observing a value's default value.
                         * @param nonDefaultJump The jump instruction that triggers skipping upon observing a value's non-default value.
                         */
                        ForValue(int load, int defaultJump, int nonDefaultJump) {
                            this.load = load;
                            this.defaultJump = defaultJump;
                            this.nonDefaultJump = nonDefaultJump;
                        }

                        /**
                         * Creates an appropriate skip dispatcher.
                         *
                         * @param typeDefinition The type for which to skip a value.
                         * @param inverted       {@code true} if the skip condition should be inverted to trigger upon non-default values.
                         * @return An appropriate skip dispatcher.
                         */
                        protected static SkipDispatcher of(TypeDefinition typeDefinition, boolean inverted) {
                            ForValue skipDispatcher;
                            if (typeDefinition.represents(long.class)) {
                                skipDispatcher = FOR_LONG;
                            } else if (typeDefinition.represents(float.class)) {
                                skipDispatcher = FOR_FLOAT;
                            } else if (typeDefinition.represents(double.class)) {
                                skipDispatcher = FOR_DOUBLE;
                            } else if (typeDefinition.represents(void.class)) {
                                throw new IllegalStateException("Cannot skip on default value for void return type");
                            } else if (typeDefinition.isPrimitive()) { // anyOf(byte, short, char, int)
                                skipDispatcher = FOR_INTEGER;
                            } else {
                                skipDispatcher = FOR_REFERENCE;
                            }
                            return inverted
                                    ? skipDispatcher.inverted()
                                    : skipDispatcher;
                        }

                        @Override
                        public void apply(MethodVisitor methodVisitor,
                                          MethodSizeHandler.ForAdvice methodSizeHandler,
                                          StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                          MethodDescription instrumentedMethod,
                                          Bound.SkipHandler skipHandler) {
                            doApply(methodVisitor, methodSizeHandler, stackMapFrameHandler, instrumentedMethod, skipHandler, false);
                        }

                        /**
                         * Applies this skip dispatcher.
                         *
                         * @param methodVisitor        The method visitor to write to.
                         * @param methodSizeHandler    The method size handler of the advice method to use.
                         * @param stackMapFrameHandler The stack map frame handler of the advice method to use.
                         * @param instrumentedMethod   The instrumented method.
                         * @param skipHandler          The skip handler to use.
                         * @param inverted             {@code true} if the skip condition should be inverted.
                         */
                        protected void doApply(MethodVisitor methodVisitor,
                                               MethodSizeHandler.ForAdvice methodSizeHandler,
                                               StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                               MethodDescription instrumentedMethod,
                                               Bound.SkipHandler skipHandler,
                                               boolean inverted) {
                            if (instrumentedMethod.isConstructor()) {
                                throw new IllegalStateException("Cannot skip code execution from constructor: " + instrumentedMethod);
                            }
                            methodVisitor.visitVarInsn(load, instrumentedMethod.getStackSize());
                            convertValue(methodVisitor, methodSizeHandler);
                            Label noSkip = new Label();
                            methodVisitor.visitJumpInsn(inverted
                                    ? nonDefaultJump
                                    : defaultJump, noSkip);
                            skipHandler.apply(methodVisitor);
                            methodVisitor.visitLabel(noSkip);
                            stackMapFrameHandler.injectCompletionFrame(methodVisitor, true);
                        }

                        /**
                         * Converts the return value to an {@code int} value.
                         *
                         * @param methodVisitor     The method visitor to use.
                         * @param methodSizeHandler The method size handler of the advice method to use.
                         */
                        protected abstract void convertValue(MethodVisitor methodVisitor, MethodSizeHandler.ForAdvice methodSizeHandler);

                        /**
                         * Returns an inverted version of this skip dispatcher.
                         *
                         * @return An inverted version of this skip dispatcher.
                         */
                        private SkipDispatcher inverted() {
                            return new Inverted();
                        }

                        /**
                         * An inverted version of a value-based skipped dispatcher that triggers upon observing a non-default value.
                         */
                        protected class Inverted implements SkipDispatcher {

                            @Override
                            public void apply(MethodVisitor methodVisitor,
                                              MethodSizeHandler.ForAdvice methodSizeHandler,
                                              StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                              MethodDescription instrumentedMethod,
                                              Bound.SkipHandler skipHandler) {
                                doApply(methodVisitor, methodSizeHandler, stackMapFrameHandler, instrumentedMethod, skipHandler, true);
                            }

                            /**
                             * Returns the outer instance.
                             *
                             * @return The outer instance.
                             */
                            private SkipDispatcher getOuter() {
                                return ForValue.this;
                            }

                            @Override // HE: Remove when Lombok support for getOuter is added.
                            public int hashCode() {
                                return ForValue.this.hashCode();
                            }

                            @Override // HE: Remove when Lombok support for getOuter is added.
                            public boolean equals(Object other) {
                                if (other == this) return true;
                                if (other == null || other.getClass() != getClass()) return false;
                                Inverted inverted = (Inverted) other;
                                return inverted.getOuter().equals(ForValue.this);
                            }
                        }
                    }

                    /**
                     * A skip dispatcher that skips a value if it is of a given instance.
                     */
                    @AutoValue
                    class ForType implements SkipDispatcher {

                        /**
                         * The type for which to skip instances.
                         */
                        private final TypeDescription typeDescription;

                        /**
                         * Creates a new skip dispatcher for a given type.
                         *
                         * @param typeDescription The type for which to skip instances.
                         */
                        protected ForType(TypeDescription typeDescription) {
                            this.typeDescription = typeDescription;
                        }

                        /**
                         * Creates a skip dispatcher for an advice method.
                         *
                         * @param adviceMethod The advice method for which to resolve a skip dispatcher.
                         * @return An appropriate skip dispatcher.
                         */
                        public static SkipDispatcher of(MethodDescription adviceMethod) {
                            return of(adviceMethod.getDeclaredAnnotations()
                                    .ofType(OnMethodEnter.class)
                                    .getValue(SKIP_ON)
                                    .resolve(TypeDescription.class), adviceMethod);
                        }

                        /**
                         * Creates a skip dispatcher for a given annotation type and advice method.
                         *
                         * @param typeDescription The type that was specified as an annotation value.
                         * @param adviceMethod    The advice method.
                         * @return An appropriate skip dispatcher.
                         */
                        protected static SkipDispatcher of(TypeDescription typeDescription, MethodDescription adviceMethod) {
                            if (typeDescription.represents(void.class)) {
                                return Disabled.INSTANCE;
                            } else if (typeDescription.represents(OnDefaultValue.class)) {
                                return ForValue.of(adviceMethod.getReturnType(), false);
                            } else if (typeDescription.represents(OnNonDefaultValue.class)) {
                                return ForValue.of(adviceMethod.getReturnType(), true);
                            } else if (typeDescription.isPrimitive() || adviceMethod.getReturnType().isPrimitive()) {
                                throw new IllegalStateException("Cannot skip method by instance type for primitive return value on " + adviceMethod);
                            } else {
                                return new ForType(typeDescription);
                            }
                        }

                        @Override
                        public void apply(MethodVisitor methodVisitor,
                                          MethodSizeHandler.ForAdvice methodSizeHandler,
                                          StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                          MethodDescription instrumentedMethod,
                                          Bound.SkipHandler skipHandler) {
                            if (instrumentedMethod.isConstructor()) {
                                throw new IllegalStateException("Cannot skip code execution from constructor: " + instrumentedMethod);
                            }
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, instrumentedMethod.getStackSize());
                            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, typeDescription.getInternalName());
                            Label noSkip = new Label();
                            methodVisitor.visitJumpInsn(Opcodes.IFEQ, noSkip);
                            skipHandler.apply(methodVisitor);
                            methodVisitor.visitLabel(noSkip);
                            stackMapFrameHandler.injectCompletionFrame(methodVisitor, true);
                        }
                    }
                }
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

                @Override
                Bound.ForMethodExit bind(TypeDescription instrumentedType,
                                         MethodDescription instrumentedMethod,
                                         MethodVisitor methodVisitor,
                                         Implementation.Context implementationContext,
                                         Assigner assigner,
                                         MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                         StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                         StackManipulation exceptionHandler);
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
             * A skip handler is responsible for writing code that skips the invocation of the original code
             * within the instrumented method.
             */
            interface SkipHandler {

                /**
                 * Applies this skip handler.
                 *
                 * @param methodVisitor The method visitor to write the code to.
                 */
                void apply(MethodVisitor methodVisitor);
            }

            /**
             * A bound dispatcher for a method enter.
             */
            interface ForMethodEnter extends Bound {

                /**
                 * Applies this dispatcher.
                 *
                 * @param skipHandler The skip handler to use.
                 */
                void apply(SkipHandler skipHandler);
            }

            /**
             * A bound dispatcher for a method exit.
             */
            interface ForMethodExit extends Bound {

                /**
                 * Applies this dispatcher.
                 */
                void apply();
            }
        }

        /**
         * An implementation for inactive devise that does not write any byte code.
         */
        enum Inactive implements Dispatcher.Unresolved, Resolved.ForMethodEnter, Resolved.ForMethodExit, Bound.ForMethodEnter, Bound.ForMethodExit {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isAlive() {
                return false;
            }

            @Override
            public boolean isBinary() {
                return false;
            }

            @Override
            public TypeDescription getThrowable() {
                return NoExceptionHandler.DESCRIPTION;
            }

            @Override
            public TypeDefinition getEnterType() {
                return TypeDescription.VOID;
            }

            @Override
            public boolean isPrependLineNumber() {
                return false;
            }

            @Override
            public Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                         ClassReader classReader) {
                return this;
            }

            @Override
            public Resolved.ForMethodExit asMethodExitTo(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                         ClassReader classReader,
                                                         Resolved.ForMethodEnter dispatcher) {
                return this;
            }

            @Override
            public void prepare() {
                /* do nothing */
            }

            @Override
            public void apply() {
                /* do nothing */
            }

            @Override
            public void apply(SkipHandler skipHandler) {
                /* do nothing */
            }

            @Override
            public Inactive bind(TypeDescription instrumentedType,
                                 MethodDescription instrumentedMethod,
                                 MethodVisitor methodVisitor,
                                 Implementation.Context implementationContext,
                                 Assigner assigner,
                                 MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                 StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                 StackManipulation exceptionHandler) {
                return this;
            }
        }

        /**
         * A dispatcher for an advice method that is being inlined into the instrumented method.
         */
        @AutoValue
        class Inlining implements Unresolved {

            /**
             * The advice method.
             */
            protected final MethodDescription.InDefinedShape adviceMethod;

            /**
             * Creates a dispatcher for inlined advice method.
             *
             * @param adviceMethod The advice method.
             */
            protected Inlining(MethodDescription.InDefinedShape adviceMethod) {
                this.adviceMethod = adviceMethod;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public boolean isBinary() {
                return true;
            }

            @Override
            public Dispatcher.Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                    ClassReader classReader) {
                return new Resolved.ForMethodEnter(adviceMethod, userFactories, classReader);
            }

            @Override
            public Dispatcher.Resolved.ForMethodExit asMethodExitTo(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                    ClassReader classReader,
                                                                    Dispatcher.Resolved.ForMethodEnter dispatcher) {
                return Resolved.ForMethodExit.of(adviceMethod, userFactories, classReader, dispatcher.getEnterType());
            }

            /**
             * A resolved version of a dispatcher.
             */
            protected abstract static class Resolved implements Dispatcher.Resolved {

                /**
                 * Indicates a read-only mapping for an offset.
                 */
                private static final boolean READ_ONLY = true;

                /**
                 * The represented advice method.
                 */
                protected final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * A class reader to query for the class file of the advice method.
                 */
                protected final ClassReader classReader;

                /**
                 * An unresolved mapping of offsets of the advice method based on the annotations discovered on each method parameter.
                 */
                protected final Map<Integer, OffsetMapping> offsetMappings;

                /**
                 * The suppression handler to use.
                 */
                protected final SuppressionHandler suppressionHandler;

                /**
                 * Creates a new resolved version of a dispatcher.
                 *
                 * @param adviceMethod  The represented advice method.
                 * @param factories     A list of factories to resolve for the parameters of the advice method.
                 * @param classReader   A class reader to query for the class file of the advice method.
                 * @param throwableType The type to handle by a suppression handler or {@link NoExceptionHandler} to not handle any exceptions.
                 */
                protected Resolved(MethodDescription.InDefinedShape adviceMethod,
                                   List<OffsetMapping.Factory<?>> factories,
                                   ClassReader classReader,
                                   TypeDescription throwableType) {
                    this.adviceMethod = adviceMethod;
                    Map<TypeDescription, OffsetMapping.Factory<?>> offsetMappings = new HashMap<TypeDescription, OffsetMapping.Factory<?>>();
                    for (OffsetMapping.Factory<?> factory : factories) {
                        offsetMappings.put(new TypeDescription.ForLoadedType(factory.getAnnotationType()), factory);
                    }
                    this.offsetMappings = new HashMap<Integer, OffsetMapping>();
                    for (ParameterDescription.InDefinedShape parameterDescription : adviceMethod.getParameters()) {
                        OffsetMapping offsetMapping = null;
                        for (AnnotationDescription annotationDescription : parameterDescription.getDeclaredAnnotations()) {
                            OffsetMapping.Factory<?> factory = offsetMappings.get(annotationDescription.getAnnotationType());
                            if (factory != null) {
                                @SuppressWarnings("unchecked")
                                OffsetMapping current = factory.make(parameterDescription,
                                        (AnnotationDescription.Loadable) annotationDescription.prepare(factory.getAnnotationType()),
                                        OffsetMapping.Factory.AdviceType.INLINING);
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
                    this.classReader = classReader;
                    suppressionHandler = SuppressionHandler.Suppressing.of(throwableType);
                }

                @Override
                public boolean isAlive() {
                    return true;
                }

                /**
                 * Applies a resolution for a given instrumented method.
                 *
                 * @param methodVisitor         A method visitor for writing byte code to the instrumented method.
                 * @param implementationContext The implementation context to use.
                 * @param assigner              The assigner to use.
                 * @param methodSizeHandler     A handler for computing the method size requirements.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param instrumentedType      A description of the instrumented type.
                 * @param instrumentedMethod    A description of the instrumented method.
                 * @param suppressionHandler    The bound suppression handler that is used for suppressing exceptions of this advice method.
                 * @return A method visitor for visiting the advice method's byte code.
                 */
                protected abstract MethodVisitor apply(MethodVisitor methodVisitor,
                                                       Implementation.Context implementationContext,
                                                       Assigner assigner,
                                                       MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                       StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                       TypeDescription instrumentedType,
                                                       MethodDescription instrumentedMethod,
                                                       SuppressionHandler.Bound suppressionHandler);

                @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    Inlining.Resolved resolved = (Inlining.Resolved) object;
                    return adviceMethod.equals(resolved.adviceMethod)
                            && offsetMappings.equals(resolved.offsetMappings)
                            && suppressionHandler.equals(resolved.suppressionHandler);
                }

                @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                public int hashCode() {
                    int result = adviceMethod.hashCode();
                    result = 31 * result + offsetMappings.hashCode();
                    result = 31 * result + suppressionHandler.hashCode();
                    return result;
                }

                /**
                 * A bound advice method that copies the code by first extracting the exception table and later appending the
                 * code of the method without copying any meta data.
                 */
                protected abstract class AdviceMethodInliner extends ClassVisitor implements Bound {

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
                     * A class reader for parsing the class file containing the represented advice method.
                     */
                    protected final ClassReader classReader;

                    /**
                     * The labels that were found during parsing the method's exception handler in the order of their discovery.
                     */
                    protected List<Label> labels;

                    /**
                     * Creates a new advice method inliner.
                     *
                     * @param instrumentedType      A description of the instrumented type.
                     * @param instrumentedMethod    The instrumented method.
                     * @param methodVisitor         The method visitor for writing the instrumented method.
                     * @param implementationContext The implementation context to use.
                     * @param assigner              The assigner to use.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                     * @param classReader           A class reader for parsing the class file containing the represented advice method.
                     */
                    protected AdviceMethodInliner(TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  MethodVisitor methodVisitor,
                                                  Implementation.Context implementationContext,
                                                  Assigner assigner,
                                                  MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                  StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                  SuppressionHandler.Bound suppressionHandler,
                                                  ClassReader classReader) {
                        super(Opcodes.ASM6);
                        this.instrumentedType = instrumentedType;
                        this.instrumentedMethod = instrumentedMethod;
                        this.methodVisitor = methodVisitor;
                        this.implementationContext = implementationContext;
                        this.assigner = assigner;
                        this.methodSizeHandler = methodSizeHandler;
                        this.stackMapFrameHandler = stackMapFrameHandler;
                        this.suppressionHandler = suppressionHandler;
                        this.classReader = classReader;
                        labels = new ArrayList<Label>();
                    }

                    @Override
                    public void prepare() {
                        classReader.accept(new ExceptionTableExtractor(), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                        suppressionHandler.onPrepare(methodVisitor);
                    }

                    /**
                     * Inlines the advice method.
                     */
                    protected void doApply() {
                        classReader.accept(this, ClassReader.SKIP_DEBUG | stackMapFrameHandler.getReaderHint());
                    }

                    @Override
                    public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
                        return adviceMethod.getInternalName().equals(internalName) && adviceMethod.getDescriptor().equals(descriptor)
                                ? new ExceptionTableSubstitutor(Inlining.Resolved.this.apply(methodVisitor,
                                implementationContext,
                                assigner,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                instrumentedType,
                                instrumentedMethod,
                                suppressionHandler)) : IGNORE_METHOD;
                    }

                    /**
                     * A class visitor that extracts the exception tables of the advice method.
                     */
                    protected class ExceptionTableExtractor extends ClassVisitor {

                        /**
                         * Creates a new exception table extractor.
                         */
                        protected ExceptionTableExtractor() {
                            super(Opcodes.ASM6);
                        }

                        @Override
                        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
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
                            super(Opcodes.ASM6);
                            this.methodVisitor = methodVisitor;
                        }

                        @Override
                        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                            methodVisitor.visitTryCatchBlock(start, end, handler, type);
                            labels.addAll(Arrays.asList(start, end, handler));
                        }

                        @Override
                        public AnnotationVisitor visitTryCatchAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
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
                         * Creates a label substitor.
                         *
                         * @param methodVisitor The method visitor for which to substitute labels.
                         */
                        protected ExceptionTableSubstitutor(MethodVisitor methodVisitor) {
                            super(Opcodes.ASM6, methodVisitor);
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
                        public AnnotationVisitor visitTryCatchAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
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
                protected static class ForMethodEnter extends Inlining.Resolved implements Dispatcher.Resolved.ForMethodEnter {

                    /**
                     * The skip dispatcher to use.
                     */
                    private final SkipDispatcher skipDispatcher;

                    /**
                     * {@code true} if the first discovered line number information should be prepended to the advice code.
                     */
                    private final boolean prependLineNumber;

                    /**
                     * Creates a new resolved dispatcher for implementing method enter advice.
                     *
                     * @param adviceMethod  The represented advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param classReader   A class reader to query for the class file of the advice method.
                     */
                    @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
                    protected ForMethodEnter(MethodDescription.InDefinedShape adviceMethod,
                                             List<? extends OffsetMapping.Factory<?>> userFactories,
                                             ClassReader classReader) {
                        super(adviceMethod,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForArgument.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForUnusedValue.Factory.INSTANCE,
                                        OffsetMapping.ForStubValue.INSTANCE,
                                        OffsetMapping.ForThrowable.Factory.INSTANCE,
                                        new OffsetMapping.Factory.Illegal<Thrown>(Thrown.class),
                                        new OffsetMapping.Factory.Illegal<Enter>(Enter.class),
                                        new OffsetMapping.Factory.Illegal<Return>(Return.class)), userFactories),
                                classReader,
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SUPPRESS_ENTER).resolve(TypeDescription.class));
                        skipDispatcher = SkipDispatcher.ForType.of(adviceMethod);
                        prependLineNumber = adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(PREPEND_LINE_NUMBER).resolve(Boolean.class);
                    }

                    @Override
                    public Bound.ForMethodEnter bind(TypeDescription instrumentedType,
                                                     MethodDescription instrumentedMethod,
                                                     MethodVisitor methodVisitor,
                                                     Implementation.Context implementationContext,
                                                     Assigner assigner,
                                                     MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                     StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                     StackManipulation exceptionHandler) {
                        return new AdviceMethodInliner(instrumentedType,
                                instrumentedMethod,
                                methodVisitor,
                                implementationContext,
                                assigner,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                suppressionHandler.bind(exceptionHandler),
                                classReader,
                                skipDispatcher);
                    }

                    @Override
                    public TypeDefinition getEnterType() {
                        return adviceMethod.getReturnType();
                    }

                    @Override
                    public boolean isPrependLineNumber() {
                        return prependLineNumber;
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor,
                                                  Context implementationContext,
                                                  Assigner assigner,
                                                  MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                  StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                  TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  SuppressionHandler.Bound suppressionHandler) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    OffsetMapping.Context.ForMethodEntry.of(instrumentedMethod)));
                        }
                        return new CodeTranslationVisitor.ForMethodEnter(methodVisitor,
                                implementationContext,
                                methodSizeHandler.bindEntry(adviceMethod),
                                stackMapFrameHandler.bindEntry(adviceMethod),
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler);
                    }

                    @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        if (!super.equals(object)) return false;
                        Inlining.Resolved.ForMethodEnter that = (Inlining.Resolved.ForMethodEnter) object;
                        return prependLineNumber == that.prependLineNumber && skipDispatcher.equals(that.skipDispatcher);
                    }

                    @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                    public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + skipDispatcher.hashCode();
                        result = 31 * result + (prependLineNumber ? 1 : 0);
                        return result;
                    }

                    /**
                     * An advice method inliner for a method enter.
                     */
                    protected class AdviceMethodInliner extends Inlining.Resolved.AdviceMethodInliner implements Bound.ForMethodEnter {

                        /**
                         * The skip dispatcher to use.
                         */
                        private final SkipDispatcher skipDispatcher;

                        /**
                         * Creates a new advice method inliner for a method enter.
                         *
                         * @param instrumentedType      A description of the instrumented type.
                         * @param instrumentedMethod    A description of the instrumented method.
                         * @param methodVisitor         The method visitor for writing the instrumented method.
                         * @param implementationContext The implementation context to use.
                         * @param assigner              The assigner to use.
                         * @param methodSizeHandler     A handler for computing the method size requirements.
                         * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                         * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                         * @param classReader           A class reader for parsing the class file containing the represented advice method.
                         * @param skipDispatcher        The skip dispatcher to use.
                         */
                        protected AdviceMethodInliner(TypeDescription instrumentedType,
                                                      MethodDescription instrumentedMethod,
                                                      MethodVisitor methodVisitor,
                                                      Implementation.Context implementationContext,
                                                      Assigner assigner,
                                                      MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                      StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                      SuppressionHandler.Bound suppressionHandler,
                                                      ClassReader classReader,
                                                      SkipDispatcher skipDispatcher) {
                            super(instrumentedType,
                                    instrumentedMethod,
                                    methodVisitor,
                                    implementationContext,
                                    assigner,
                                    methodSizeHandler,
                                    stackMapFrameHandler,
                                    suppressionHandler,
                                    classReader);
                            this.skipDispatcher = skipDispatcher;
                        }

                        @Override
                        public void apply(SkipHandler skipHandler) {
                            doApply();
                            skipDispatcher.apply(methodVisitor,
                                    methodSizeHandler.bindEntry(adviceMethod),
                                    stackMapFrameHandler.bindEntry(adviceMethod),
                                    instrumentedMethod,
                                    skipHandler);
                        }
                    }
                }

                /**
                 * A resolved dispatcher for implementing method exit advice.
                 */
                protected abstract static class ForMethodExit extends Inlining.Resolved implements Dispatcher.Resolved.ForMethodExit {

                    /**
                     * The additional stack size to consider when accessing the local variable array.
                     */
                    private final TypeDefinition enterType;

                    /**
                     * Creates a new resolved dispatcher for implementing method exit advice.
                     *
                     * @param adviceMethod  The represented advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param classReader   The class reader for parsing the advice method's class file.
                     * @param enterType     The type of the value supplied by the enter advice method or
                     *                      a description of {@code void} if no such value exists.
                     */
                    @SuppressWarnings("unchecked")
                    protected ForMethodExit(MethodDescription.InDefinedShape adviceMethod,
                                            List<? extends OffsetMapping.Factory<?>> userFactories,
                                            ClassReader classReader,
                                            TypeDefinition enterType) {
                        super(adviceMethod,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForArgument.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForUnusedValue.Factory.INSTANCE,
                                        OffsetMapping.ForStubValue.INSTANCE,
                                        new OffsetMapping.ForEnterValue.Factory(enterType),
                                        OffsetMapping.ForReturnValue.Factory.INSTANCE,
                                        OffsetMapping.ForThrowable.Factory.of(adviceMethod)
                                ), userFactories),
                                classReader,
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(SUPPRESS_EXIT).resolve(TypeDescription.class));
                        this.enterType = enterType;
                    }

                    /**
                     * Resolves exit advice that handles exceptions depending on the specification of the exit advice.
                     *
                     * @param adviceMethod  The advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param classReader   The class reader for parsing the advice method's class file.
                     * @param enterType     The type of the value supplied by the enter advice method or
                     *                      a description of {@code void} if no such value exists.
                     * @return An appropriate exit handler.
                     */
                    protected static Resolved.ForMethodExit of(MethodDescription.InDefinedShape adviceMethod,
                                                               List<? extends OffsetMapping.Factory<?>> userFactories,
                                                               ClassReader classReader,
                                                               TypeDefinition enterType) {
                        TypeDescription throwable = adviceMethod.getDeclaredAnnotations()
                                .ofType(OnMethodExit.class)
                                .getValue(ON_THROWABLE).resolve(TypeDescription.class);
                        return throwable.represents(NoExceptionHandler.class)
                                ? new WithoutExceptionHandler(adviceMethod, userFactories, classReader, enterType)
                                : new WithExceptionHandler(adviceMethod, userFactories, classReader, enterType, throwable);
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor,
                                                  Implementation.Context implementationContext,
                                                  Assigner assigner,
                                                  MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                  StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                  TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  SuppressionHandler.Bound suppressionHandler) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    OffsetMapping.Context.ForMethodExit.of(enterType)));
                        }
                        return new CodeTranslationVisitor.ForMethodExit(methodVisitor,
                                implementationContext,
                                methodSizeHandler.bindExit(adviceMethod, getThrowable().represents(NoExceptionHandler.class)),
                                stackMapFrameHandler.bindExit(adviceMethod),
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler,
                                enterType.getStackSize().getSize() + getPadding().getSize());
                    }


                    @Override
                    public Bound.ForMethodExit bind(TypeDescription instrumentedType,
                                                    MethodDescription instrumentedMethod,
                                                    MethodVisitor methodVisitor,
                                                    Implementation.Context implementationContext,
                                                    Assigner assigner,
                                                    MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                    StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                    StackManipulation exceptionHandler) {
                        return new AdviceMethodInliner(instrumentedType,
                                instrumentedMethod,
                                methodVisitor,
                                implementationContext,
                                assigner,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                suppressionHandler.bind(exceptionHandler),
                                classReader);
                    }

                    /**
                     * Returns the additional padding this exit advice implies.
                     *
                     * @return The additional padding this exit advice implies.
                     */
                    protected abstract StackSize getPadding();

                    /**
                     * An advice method inliner for a method exit.
                     */
                    protected class AdviceMethodInliner extends Inlining.Resolved.AdviceMethodInliner implements Bound.ForMethodExit {

                        /**
                         * Creates a new advice method inliner for a method exit.
                         *
                         * @param instrumentedType      A description of the instrumented type.
                         * @param instrumentedMethod    A description of the instrumented method.
                         * @param methodVisitor         The method visitor for writing the instrumented method.
                         * @param implementationContext The implementation context to use.
                         * @param assigner              The assigner to use.
                         * @param methodSizeHandler     A handler for computing the method size requirements.
                         * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                         * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                         * @param classReader           A class reader for parsing the class file containing the represented advice method.
                         */
                        public AdviceMethodInliner(TypeDescription instrumentedType,
                                                   MethodDescription instrumentedMethod,
                                                   MethodVisitor methodVisitor,
                                                   Implementation.Context implementationContext,
                                                   Assigner assigner,
                                                   MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                   StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                   SuppressionHandler.Bound suppressionHandler,
                                                   ClassReader classReader) {
                            super(instrumentedType,
                                    instrumentedMethod,
                                    methodVisitor,
                                    implementationContext,
                                    assigner,
                                    methodSizeHandler,
                                    stackMapFrameHandler,
                                    suppressionHandler,
                                    classReader);
                        }

                        @Override
                        public void apply() {
                            doApply();
                        }
                    }

                    @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        if (!super.equals(object)) return false;
                        Inlining.Resolved.ForMethodExit that = (Inlining.Resolved.ForMethodExit) object;
                        return enterType.equals(that.enterType);
                    }

                    @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                    public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + enterType.hashCode();
                        return result;
                    }

                    /**
                     * Implementation of exit advice that handles exceptions.
                     */
                    @AutoValue
                    protected static class WithExceptionHandler extends Inlining.Resolved.ForMethodExit {

                        /**
                         * The type of the handled throwable type for which this advice is invoked.
                         */
                        private final TypeDescription throwable;

                        /**
                         * Creates a new resolved dispatcher for implementing method exit advice that handles exceptions.
                         *
                         * @param adviceMethod  The represented advice method.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param classReader   The class reader for parsing the advice method's class file.
                         * @param enterType     The type of the value supplied by the enter advice method or
                         *                      a description of {@code void} if no such value exists.
                         * @param throwable     The type of the handled throwable type for which this advice is invoked.
                         */
                        protected WithExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                       List<? extends OffsetMapping.Factory<?>> userFactories,
                                                       ClassReader classReader,
                                                       TypeDefinition enterType,
                                                       TypeDescription throwable) {
                            super(adviceMethod, userFactories, classReader, enterType);
                            this.throwable = throwable;
                        }

                        @Override
                        protected StackSize getPadding() {
                            return throwable.getStackSize();
                        }

                        @Override
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
                         * @param adviceMethod  The represented advice method.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param classReader   A class reader to query for the class file of the advice method.
                         * @param enterType     The type of the value supplied by the enter advice method or
                         *                      a description of {@code void} if no such value exists.
                         */
                        protected WithoutExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                          List<? extends OffsetMapping.Factory<?>> userFactories,
                                                          ClassReader classReader,
                                                          TypeDefinition enterType) {
                            super(adviceMethod, userFactories, classReader, enterType);
                        }

                        @Override
                        protected StackSize getPadding() {
                            return StackSize.ZERO;
                        }

                        @Override
                        public TypeDescription getThrowable() {
                            return NoExceptionHandler.DESCRIPTION;
                        }
                    }
                }
            }

            /**
             * A visitor for translating an advice method's byte code for inlining into the instrumented method.
             */
            protected abstract static class CodeTranslationVisitor extends MethodVisitor implements SuppressionHandler.ReturnValueProducer {

                /**
                 * The original method visitor to which all instructions are eventually written to.
                 */
                protected final MethodVisitor methodVisitor;

                /**
                 * The implementation context to use.
                 */
                protected final Context implementationContext;

                /**
                 * A handler for computing the method size requirements.
                 */
                protected final MethodSizeHandler.ForAdvice methodSizeHandler;

                /**
                 * A handler for translating and injecting stack map frames.
                 */
                protected final StackMapFrameHandler.ForAdvice stackMapFrameHandler;

                /**
                 * The instrumented method.
                 */
                protected final MethodDescription instrumentedMethod;

                /**
                 * The advice method.
                 */
                protected final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * A mapping of offsets to resolved target offsets in the instrumented method.
                 */
                private final Map<Integer, OffsetMapping.Target> offsetMappings;

                /**
                 * A handler for optionally suppressing exceptions.
                 */
                private final SuppressionHandler.Bound suppressionHandler;

                /**
                 * A label indicating the end of the advice byte code.
                 */
                protected final Label endOfMethod;

                /**
                 * Creates a new code translation visitor.
                 *
                 * @param methodVisitor         A method visitor for writing the instrumented method's byte code.
                 * @param implementationContext The implementation context to use.
                 * @param methodSizeHandler     A handler for computing the method size requirements.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param instrumentedMethod    The instrumented method.
                 * @param adviceMethod          The advice method.
                 * @param offsetMappings        A mapping of offsets to resolved target offsets in the instrumented method.
                 * @param suppressionHandler    The suppression handler to use.
                 */
                protected CodeTranslationVisitor(MethodVisitor methodVisitor,
                                                 Context implementationContext,
                                                 MethodSizeHandler.ForAdvice methodSizeHandler,
                                                 StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                 MethodDescription instrumentedMethod,
                                                 MethodDescription.InDefinedShape adviceMethod,
                                                 Map<Integer, OffsetMapping.Target> offsetMappings,
                                                 SuppressionHandler.Bound suppressionHandler) {
                    super(Opcodes.ASM6, new StackAwareMethodVisitor(methodVisitor, instrumentedMethod));
                    this.methodVisitor = methodVisitor;
                    this.implementationContext = implementationContext;
                    this.methodSizeHandler = methodSizeHandler;
                    this.stackMapFrameHandler = stackMapFrameHandler;
                    this.instrumentedMethod = instrumentedMethod;
                    this.adviceMethod = adviceMethod;
                    this.offsetMappings = offsetMappings;
                    this.suppressionHandler = suppressionHandler;
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
                public AnnotationVisitor visitAnnotationDefault() {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
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
                public void visitFrame(int type, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
                    stackMapFrameHandler.translateFrame(methodVisitor, type, localVariableLength, localVariable, stackSize, stack);
                }

                @Override
                public void visitEnd() {
                    suppressionHandler.onEnd(methodVisitor, implementationContext, methodSizeHandler, stackMapFrameHandler, this);
                    methodVisitor.visitLabel(endOfMethod);
                    onMethodReturn();
                    stackMapFrameHandler.injectCompletionFrame(methodVisitor, false);
                }

                @Override
                public void visitMaxs(int stackSize, int localVariableLength) {
                    methodSizeHandler.recordMaxima(stackSize, localVariableLength);
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
                        methodSizeHandler.recordPadding(stackManipulation.apply(mv, implementationContext).getMaximalSize() - expectedGrowth.getSize());
                    } else {
                        mv.visitVarInsn(opcode, adjust(offset + instrumentedMethod.getStackSize() - adviceMethod.getStackSize()));
                    }
                }

                @Override
                public void visitIincInsn(int offset, int value) {
                    OffsetMapping.Target target = offsetMappings.get(offset);
                    if (target != null) {
                        methodSizeHandler.recordPadding(target.resolveIncrement(value).apply(mv, implementationContext).getMaximalSize());
                    } else {
                        mv.visitIincInsn(adjust(offset + instrumentedMethod.getStackSize() - adviceMethod.getStackSize()), value);
                    }
                }

                /**
                 * Adjusts the offset of a variable instruction within the advice method such that no arguments to
                 * the instrumented method are overridden.
                 *
                 * @param offset The original offset.
                 * @return The adjusted offset.
                 */
                protected abstract int adjust(int offset);

                @Override
                public abstract void visitInsn(int opcode);

                /**
                 * Invoked after returning from the advice method.
                 */
                protected abstract void onMethodReturn();

                /**
                 * A code translation visitor that retains the return value of the represented advice method.
                 */
                protected static class ForMethodEnter extends CodeTranslationVisitor {

                    /**
                     * {@code true} if the method can return non-exceptionally.
                     */
                    private boolean doesReturn;

                    /**
                     * Creates a code translation visitor for translating exit advice.
                     *
                     * @param methodVisitor         A method visitor for writing the instrumented method's byte code.
                     * @param implementationContext The implementation context to use.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param instrumentedMethod    The instrumented method.
                     * @param adviceMethod          The advice method.
                     * @param offsetMappings        A mapping of offsets to resolved target offsets in the instrumented method.
                     * @param suppressionHandler    The suppression handler to use.
                     */
                    protected ForMethodEnter(MethodVisitor methodVisitor,
                                             Context implementationContext,
                                             MethodSizeHandler.ForAdvice methodSizeHandler,
                                             StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                             MethodDescription instrumentedMethod,
                                             MethodDescription.InDefinedShape adviceMethod,
                                             Map<Integer, OffsetMapping.Target> offsetMappings,
                                             SuppressionHandler.Bound suppressionHandler) {
                        super(methodVisitor,
                                implementationContext,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler);
                        doesReturn = false;
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
                        doesReturn = true;
                    }

                    @Override
                    protected int adjust(int offset) {
                        return offset;
                    }

                    @Override
                    public void onDefaultValue(MethodVisitor methodVisitor) {
                        if (adviceMethod.getReturnType().represents(boolean.class)
                                || adviceMethod.getReturnType().represents(byte.class)
                                || adviceMethod.getReturnType().represents(short.class)
                                || adviceMethod.getReturnType().represents(char.class)
                                || adviceMethod.getReturnType().represents(int.class)) {
                            methodVisitor.visitInsn(Opcodes.ICONST_0);
                        } else if (adviceMethod.getReturnType().represents(long.class)) {
                            methodVisitor.visitInsn(Opcodes.LCONST_0);
                        } else if (adviceMethod.getReturnType().represents(float.class)) {
                            methodVisitor.visitInsn(Opcodes.FCONST_0);
                        } else if (adviceMethod.getReturnType().represents(double.class)) {
                            methodVisitor.visitInsn(Opcodes.DCONST_0);
                        } else if (!adviceMethod.getReturnType().represents(void.class)) {
                            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        }
                        doesReturn = true;
                    }

                    @Override
                    protected void onMethodReturn() {
                        Type returnType = Type.getType(adviceMethod.getReturnType().asErasure().getDescriptor());
                        if (doesReturn && !returnType.equals(Type.VOID_TYPE)) {
                            stackMapFrameHandler.injectReturnFrame(methodVisitor);
                            methodVisitor.visitVarInsn(returnType.getOpcode(Opcodes.ISTORE), instrumentedMethod.getStackSize());
                        }
                    }
                }

                /**
                 * A code translation visitor that discards the return value of the represented advice method.
                 */
                protected static class ForMethodExit extends CodeTranslationVisitor {

                    /**
                     * The padding after the instrumented method's arguments in the local variable array.
                     */
                    private final int padding;

                    /**
                     * Creates a code translation visitor for translating exit advice.
                     *
                     * @param methodVisitor         A method visitor for writing the instrumented method's byte code.
                     * @param implementationContext The implementation context to use.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param instrumentedMethod    The instrumented method.
                     * @param adviceMethod          The advice method.
                     * @param offsetMappings        A mapping of offsets to resolved target offsets in the instrumented method.
                     * @param suppressionHandler    The suppression handler to use.
                     * @param padding               The padding after the instrumented method's arguments in the local variable array.
                     */
                    protected ForMethodExit(MethodVisitor methodVisitor,
                                            Implementation.Context implementationContext,
                                            MethodSizeHandler.ForAdvice methodSizeHandler,
                                            StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                            MethodDescription instrumentedMethod,
                                            MethodDescription.InDefinedShape adviceMethod,
                                            Map<Integer, OffsetMapping.Target> offsetMappings,
                                            SuppressionHandler.Bound suppressionHandler,
                                            int padding) {
                        super(methodVisitor,
                                implementationContext,
                                methodSizeHandler,
                                stackMapFrameHandler,
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler);
                        this.padding = padding;
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        switch (opcode) {
                            case Opcodes.RETURN:
                                break;
                            case Opcodes.IRETURN:
                            case Opcodes.ARETURN:
                            case Opcodes.FRETURN:
                                mv.visitInsn(Opcodes.POP);
                                break;
                            case Opcodes.LRETURN:
                            case Opcodes.DRETURN:
                                mv.visitInsn(Opcodes.POP2);
                                break;
                            default:
                                mv.visitInsn(opcode);
                                return;
                        }
                        ((StackAwareMethodVisitor) mv).drainStack();
                        mv.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                    }

                    @Override
                    protected int adjust(int offset) {
                        return instrumentedMethod.getReturnType().getStackSize().getSize() + padding + offset;
                    }

                    @Override
                    public void onDefaultValue(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    protected void onMethodReturn() {
                        /* do nothing */
                    }
                }
            }
        }

        /**
         * A dispatcher for an advice method that is being invoked from the instrumented method.
         */
        @AutoValue
        class Delegating implements Unresolved {

            /**
             * The advice method.
             */
            protected final MethodDescription.InDefinedShape adviceMethod;

            /**
             * Creates a new delegating advice dispatcher.
             *
             * @param adviceMethod The advice method.
             */
            protected Delegating(MethodDescription.InDefinedShape adviceMethod) {
                this.adviceMethod = adviceMethod;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public boolean isBinary() {
                return false;
            }

            @Override
            public Dispatcher.Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                    ClassReader classReader) {
                return new Resolved.ForMethodEnter(adviceMethod, userFactories);
            }

            @Override
            public Dispatcher.Resolved.ForMethodExit asMethodExitTo(List<? extends OffsetMapping.Factory<?>> userFactories,
                                                                    ClassReader classReader,
                                                                    Dispatcher.Resolved.ForMethodEnter dispatcher) {
                return Resolved.ForMethodExit.of(adviceMethod, userFactories, dispatcher.getEnterType());
            }

            /**
             * A resolved version of a dispatcher.
             *
             * @param <T> The type of advice dispatcher that is bound.
             */
            protected abstract static class Resolved<T extends Bound> implements Dispatcher.Resolved {

                /**
                 * The represented advice method.
                 */
                protected final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * An unresolved mapping of offsets of the advice method based on the annotations discovered on each method parameter.
                 */
                protected final List<OffsetMapping> offsetMappings;

                /**
                 * The suppression handler to use.
                 */
                protected final SuppressionHandler suppressionHandler;

                /**
                 * Creates a new resolved version of a dispatcher.
                 *
                 * @param adviceMethod  The represented advice method.
                 * @param factories     A list of factories to resolve for the parameters of the advice method.
                 * @param throwableType The type to handle by a suppression handler or {@link NoExceptionHandler} to not handle any exceptions.
                 */
                protected Resolved(MethodDescription.InDefinedShape adviceMethod,
                                   List<? extends OffsetMapping.Factory<?>> factories,
                                   TypeDescription throwableType) {
                    this.adviceMethod = adviceMethod;
                    Map<TypeDescription, OffsetMapping.Factory<?>> offsetMappings = new HashMap<TypeDescription, OffsetMapping.Factory<?>>();
                    for (OffsetMapping.Factory<?> factory : factories) {
                        offsetMappings.put(new TypeDescription.ForLoadedType(factory.getAnnotationType()), factory);
                    }
                    this.offsetMappings = new ArrayList<OffsetMapping>();
                    for (ParameterDescription.InDefinedShape parameterDescription : adviceMethod.getParameters()) {
                        OffsetMapping offsetMapping = null;
                        for (AnnotationDescription annotationDescription : parameterDescription.getDeclaredAnnotations()) {
                            OffsetMapping.Factory<?> factory = offsetMappings.get(annotationDescription.getAnnotationType());
                            if (factory != null) {
                                @SuppressWarnings("unchecked")
                                OffsetMapping current = factory.make(parameterDescription,
                                        (AnnotationDescription.Loadable) annotationDescription.prepare(factory.getAnnotationType()),
                                        OffsetMapping.Factory.AdviceType.DELEGATION);
                                if (offsetMapping == null) {
                                    offsetMapping = current;
                                } else {
                                    throw new IllegalStateException(parameterDescription + " is bound to both " + current + " and " + offsetMapping);
                                }
                            }
                        }
                        this.offsetMappings.add(offsetMapping == null
                                ? new OffsetMapping.ForArgument.Unresolved(parameterDescription)
                                : offsetMapping);
                    }
                    suppressionHandler = SuppressionHandler.Suppressing.of(throwableType);
                }

                @Override
                public boolean isAlive() {
                    return true;
                }

                @Override
                public T bind(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              MethodVisitor methodVisitor,
                              Implementation.Context implementationContext,
                              Assigner assigner,
                              MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                              StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                              StackManipulation exceptionHandler) {
                    if (!adviceMethod.isVisibleTo(instrumentedType)) {
                        throw new IllegalStateException(adviceMethod + " is not visible to " + instrumentedMethod.getDeclaringType());
                    }
                    return resolve(instrumentedType,
                            instrumentedMethod,
                            methodVisitor,
                            implementationContext,
                            assigner,
                            methodSizeHandler,
                            stackMapFrameHandler,
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
                 * @param methodSizeHandler     A handler for computing the method size requirements.
                 * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                 * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
                 * @return An appropriate bound advice dispatcher.
                 */
                protected abstract T resolve(TypeDescription instrumentedType,
                                             MethodDescription instrumentedMethod,
                                             MethodVisitor methodVisitor,
                                             Implementation.Context implementationContext,
                                             Assigner assigner,
                                             MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                             StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                             StackManipulation exceptionHandler);

                @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    Delegating.Resolved<?> resolved = (Delegating.Resolved<?>) object;
                    return adviceMethod.equals(resolved.adviceMethod)
                            && offsetMappings.equals(resolved.offsetMappings)
                            && suppressionHandler.equals(resolved.suppressionHandler);
                }

                @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                public int hashCode() {
                    int result = adviceMethod.hashCode();
                    result = 31 * result + offsetMappings.hashCode();
                    result = 31 * result + suppressionHandler.hashCode();
                    return result;
                }

                /**
                 * A bound advice method that copies the code by first extracting the exception table and later appending the
                 * code of the method without copying any meta data.
                 */
                protected abstract static class AdviceMethodWriter implements Bound, SuppressionHandler.ReturnValueProducer {

                    /**
                     * Indicates an empty local variable array which is not required for calling a method.
                     */
                    private static final int EMPTY = 0;

                    /**
                     * The advice method.
                     */
                    protected final MethodDescription.InDefinedShape adviceMethod;

                    /**
                     * The instrumented method.
                     */
                    protected final MethodDescription instrumentedMethod;

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
                     * A handler for computing the method size requirements.
                     */
                    protected final MethodSizeHandler.ForAdvice methodSizeHandler;

                    /**
                     * A handler for translating and injecting stack map frmes.
                     */
                    protected final StackMapFrameHandler.ForAdvice stackMapFrameHandler;

                    /**
                     * A bound suppression handler that is used for suppressing exceptions of this advice method.
                     */
                    private final SuppressionHandler.Bound suppressionHandler;

                    /**
                     * Creates a new advice method writer.
                     *
                     * @param adviceMethod          The advice method.
                     * @param instrumentedMethod    The instrumented method.
                     * @param offsetMappings        The offset mappings available to this advice.
                     * @param methodVisitor         The method visitor for writing the instrumented method.
                     * @param implementationContext The implementation context to use.
                     * @param methodSizeHandler     A handler for computing the method size requirements.
                     * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                     * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                     */
                    protected AdviceMethodWriter(MethodDescription.InDefinedShape adviceMethod,
                                                 MethodDescription instrumentedMethod,
                                                 List<OffsetMapping.Target> offsetMappings,
                                                 MethodVisitor methodVisitor,
                                                 Context implementationContext,
                                                 MethodSizeHandler.ForAdvice methodSizeHandler,
                                                 StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                 SuppressionHandler.Bound suppressionHandler) {
                        this.adviceMethod = adviceMethod;
                        this.instrumentedMethod = instrumentedMethod;
                        this.offsetMappings = offsetMappings;
                        this.methodVisitor = methodVisitor;
                        this.implementationContext = implementationContext;
                        this.methodSizeHandler = methodSizeHandler;
                        this.stackMapFrameHandler = stackMapFrameHandler;
                        this.suppressionHandler = suppressionHandler;
                    }

                    @Override
                    public void prepare() {
                        suppressionHandler.onPrepare(methodVisitor);
                    }

                    /**
                     * Writes the advice method invocation.
                     */
                    protected void doApply() {
                        suppressionHandler.onStart(methodVisitor);
                        int index = 0, currentStackSize = 0, maximumStackSize = 0;
                        for (OffsetMapping.Target offsetMapping : offsetMappings) {
                            currentStackSize += adviceMethod.getParameters().get(index++).getType().getStackSize().getSize();
                            maximumStackSize = Math.max(maximumStackSize, currentStackSize + offsetMapping.resolveRead()
                                    .apply(methodVisitor, implementationContext)
                                    .getMaximalSize());
                        }
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                                adviceMethod.getDeclaringType().getInternalName(),
                                adviceMethod.getInternalName(),
                                adviceMethod.getDescriptor(),
                                false);
                        onMethodReturn();
                        suppressionHandler.onEndSkipped(methodVisitor, implementationContext, methodSizeHandler, stackMapFrameHandler, this);
                        stackMapFrameHandler.injectCompletionFrame(methodVisitor, false);
                        methodSizeHandler.recordMaxima(Math.max(maximumStackSize, adviceMethod.getReturnType().getStackSize().getSize()), EMPTY);
                    }

                    /**
                     * Invoked directly after the advice method was called.
                     */
                    protected abstract void onMethodReturn();

                    /**
                     * An advice method writer for a method entry.
                     */
                    protected static class ForMethodEnter extends AdviceMethodWriter implements Bound.ForMethodEnter {

                        /**
                         * The skip dispatcher to use.
                         */
                        private final Resolved.ForMethodEnter.SkipDispatcher skipDispatcher;

                        /**
                         * Creates a new advice method writer.
                         *
                         * @param adviceMethod          The advice method.
                         * @param instrumentedMethod    The instrumented method.
                         * @param offsetMappings        The offset mappings available to this advice.
                         * @param methodVisitor         The method visitor for writing the instrumented method.
                         * @param implementationContext The implementation context to use.
                         * @param methodSizeHandler     A handler for computing the method size requirements.
                         * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                         * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                         * @param skipDispatcher        The skip dispatcher to use.
                         */
                        protected ForMethodEnter(MethodDescription.InDefinedShape adviceMethod,
                                                 MethodDescription instrumentedMethod,
                                                 List<OffsetMapping.Target> offsetMappings,
                                                 MethodVisitor methodVisitor,
                                                 Implementation.Context implementationContext,
                                                 MethodSizeHandler.ForAdvice methodSizeHandler,
                                                 StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                 SuppressionHandler.Bound suppressionHandler,
                                                 Resolved.ForMethodEnter.SkipDispatcher skipDispatcher) {
                            super(adviceMethod,
                                    instrumentedMethod,
                                    offsetMappings,
                                    methodVisitor,
                                    implementationContext,
                                    methodSizeHandler,
                                    stackMapFrameHandler,
                                    suppressionHandler);
                            this.skipDispatcher = skipDispatcher;
                        }

                        @Override
                        protected void onMethodReturn() {
                            if (adviceMethod.getReturnType().represents(boolean.class)
                                    || adviceMethod.getReturnType().represents(byte.class)
                                    || adviceMethod.getReturnType().represents(short.class)
                                    || adviceMethod.getReturnType().represents(char.class)
                                    || adviceMethod.getReturnType().represents(int.class)) {
                                methodVisitor.visitVarInsn(Opcodes.ISTORE, instrumentedMethod.getStackSize());
                            } else if (adviceMethod.getReturnType().represents(long.class)) {
                                methodVisitor.visitVarInsn(Opcodes.LSTORE, instrumentedMethod.getStackSize());
                            } else if (adviceMethod.getReturnType().represents(float.class)) {
                                methodVisitor.visitVarInsn(Opcodes.FSTORE, instrumentedMethod.getStackSize());
                            } else if (adviceMethod.getReturnType().represents(double.class)) {
                                methodVisitor.visitVarInsn(Opcodes.DSTORE, instrumentedMethod.getStackSize());
                            } else if (!adviceMethod.getReturnType().represents(void.class)) {
                                methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                            }
                        }

                        @Override
                        public void apply(SkipHandler skipHandler) {
                            doApply();
                            skipDispatcher.apply(methodVisitor, methodSizeHandler, stackMapFrameHandler, instrumentedMethod, skipHandler);
                        }

                        @Override
                        public void onDefaultValue(MethodVisitor methodVisitor) {
                            if (adviceMethod.getReturnType().represents(boolean.class)
                                    || adviceMethod.getReturnType().represents(byte.class)
                                    || adviceMethod.getReturnType().represents(short.class)
                                    || adviceMethod.getReturnType().represents(char.class)
                                    || adviceMethod.getReturnType().represents(int.class)) {
                                methodVisitor.visitInsn(Opcodes.ICONST_0);
                                methodVisitor.visitVarInsn(Opcodes.ISTORE, instrumentedMethod.getStackSize());
                            } else if (adviceMethod.getReturnType().represents(long.class)) {
                                methodVisitor.visitInsn(Opcodes.LCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.LSTORE, instrumentedMethod.getStackSize());
                            } else if (adviceMethod.getReturnType().represents(float.class)) {
                                methodVisitor.visitInsn(Opcodes.FCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.FSTORE, instrumentedMethod.getStackSize());
                            } else if (adviceMethod.getReturnType().represents(double.class)) {
                                methodVisitor.visitInsn(Opcodes.DCONST_0);
                                methodVisitor.visitVarInsn(Opcodes.DSTORE, instrumentedMethod.getStackSize());
                            } else if (!adviceMethod.getReturnType().represents(void.class)) {
                                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                                methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                            }
                        }
                    }

                    /**
                     * An advice method writer for a method exit.
                     */
                    protected static class ForMethodExit extends AdviceMethodWriter implements Bound.ForMethodExit {

                        /**
                         * Creates a new advice method writer.
                         *
                         * @param adviceMethod          The advice method.
                         * @param instrumentedMethod    The instrumented method.
                         * @param offsetMappings        The offset mappings available to this advice.
                         * @param methodVisitor         The method visitor for writing the instrumented method.
                         * @param implementationContext The implementation context to use.
                         * @param methodSizeHandler     A handler for computing the method size requirements.
                         * @param stackMapFrameHandler  A handler for translating and injecting stack map frames.
                         * @param suppressionHandler    A bound suppression handler that is used for suppressing exceptions of this advice method.
                         */
                        protected ForMethodExit(MethodDescription.InDefinedShape adviceMethod,
                                                MethodDescription instrumentedMethod,
                                                List<OffsetMapping.Target> offsetMappings,
                                                MethodVisitor methodVisitor,
                                                Implementation.Context implementationContext,
                                                MethodSizeHandler.ForAdvice methodSizeHandler,
                                                StackMapFrameHandler.ForAdvice stackMapFrameHandler,
                                                SuppressionHandler.Bound suppressionHandler) {
                            super(adviceMethod,
                                    instrumentedMethod,
                                    offsetMappings,
                                    methodVisitor,
                                    implementationContext,
                                    methodSizeHandler,
                                    stackMapFrameHandler,
                                    suppressionHandler);
                        }

                        @Override
                        public void apply() {
                            doApply();
                        }

                        @Override
                        protected void onMethodReturn() {
                            switch (adviceMethod.getReturnType().getStackSize()) {
                                case ZERO:
                                    return;
                                case SINGLE:
                                    methodVisitor.visitInsn(Opcodes.POP);
                                    return;
                                case DOUBLE:
                                    methodVisitor.visitInsn(Opcodes.POP2);
                                    return;
                                default:
                                    throw new IllegalStateException("Unexpected size: " + adviceMethod.getReturnType().getStackSize());
                            }
                        }

                        @Override
                        public void onDefaultValue(MethodVisitor methodVisitor) {
                            /* do nothing */
                        }
                    }
                }

                /**
                 * A resolved dispatcher for implementing method enter advice.
                 */
                protected static class ForMethodEnter extends Delegating.Resolved<Bound.ForMethodEnter> implements Dispatcher.Resolved.ForMethodEnter {

                    /**
                     * The skip dispatcher to use.
                     */
                    private final SkipDispatcher skipDispatcher;

                    /**
                     * {@code true} if the first discovered line number information should be prepended to the advice code.
                     */
                    private final boolean prependLineNumber;

                    /**
                     * Creates a new resolved dispatcher for implementing method enter advice.
                     *
                     * @param adviceMethod  The represented advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     */
                    @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
                    protected ForMethodEnter(MethodDescription.InDefinedShape adviceMethod, List<? extends OffsetMapping.Factory<?>> userFactories) {
                        super(adviceMethod,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForArgument.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForUnusedValue.Factory.INSTANCE,
                                        OffsetMapping.ForStubValue.INSTANCE,
                                        new OffsetMapping.Factory.Illegal<Thrown>(Thrown.class),
                                        new OffsetMapping.Factory.Illegal<Enter>(Enter.class),
                                        new OffsetMapping.Factory.Illegal<Return>(Return.class)), userFactories),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SUPPRESS_ENTER).resolve(TypeDescription.class));
                        skipDispatcher = SkipDispatcher.ForType.of(adviceMethod);
                        prependLineNumber = adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(PREPEND_LINE_NUMBER).resolve(Boolean.class);
                    }

                    @Override
                    public TypeDefinition getEnterType() {
                        return adviceMethod.getReturnType();
                    }

                    @Override
                    public boolean isPrependLineNumber() {
                        return prependLineNumber;
                    }

                    @Override
                    protected Bound.ForMethodEnter resolve(TypeDescription instrumentedType,
                                                           MethodDescription instrumentedMethod,
                                                           MethodVisitor methodVisitor,
                                                           Implementation.Context implementationContext,
                                                           Assigner assigner,
                                                           MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                           StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                           StackManipulation exceptionHandler) {
                        List<OffsetMapping.Target> offsetMappings = new ArrayList<OffsetMapping.Target>(this.offsetMappings.size());
                        for (OffsetMapping offsetMapping : this.offsetMappings) {
                            offsetMappings.add(offsetMapping.resolve(instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    OffsetMapping.Context.ForMethodEntry.of(instrumentedMethod)));
                        }
                        return new AdviceMethodWriter.ForMethodEnter(adviceMethod,
                                instrumentedMethod,
                                offsetMappings,
                                methodVisitor,
                                implementationContext,
                                methodSizeHandler.bindEntry(adviceMethod),
                                stackMapFrameHandler.bindEntry(adviceMethod),
                                suppressionHandler.bind(exceptionHandler),
                                skipDispatcher);
                    }

                    @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        if (!super.equals(object)) return false;
                        Delegating.Resolved.ForMethodEnter that = (Delegating.Resolved.ForMethodEnter) object;
                        return prependLineNumber == that.prependLineNumber && skipDispatcher.equals(that.skipDispatcher);
                    }

                    @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                    public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + skipDispatcher.hashCode();
                        result = 31 * result + (prependLineNumber ? 1 : 0);
                        return result;
                    }
                }

                /**
                 * A resolved dispatcher for implementing method exit advice.
                 */
                protected abstract static class ForMethodExit extends Delegating.Resolved<Bound.ForMethodExit> implements Dispatcher.Resolved.ForMethodExit {

                    /**
                     * The additional stack size to consider when accessing the local variable array.
                     */
                    private final TypeDefinition enterType;

                    /**
                     * Creates a new resolved dispatcher for implementing method exit advice.
                     *
                     * @param adviceMethod  The represented advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param enterType     The type of the value supplied by the enter advice method or
                     *                      a description of {@code void} if no such value exists.
                     */
                    @SuppressWarnings("unchecked")
                    protected ForMethodExit(MethodDescription.InDefinedShape adviceMethod,
                                            List<? extends OffsetMapping.Factory<?>> userFactories,
                                            TypeDefinition enterType) {
                        super(adviceMethod,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForArgument.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForAllArguments.Factory.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Unresolved.Factory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForUnusedValue.Factory.INSTANCE,
                                        OffsetMapping.ForStubValue.INSTANCE,
                                        new OffsetMapping.ForEnterValue.Factory(enterType),
                                        OffsetMapping.ForReturnValue.Factory.INSTANCE,
                                        OffsetMapping.ForThrowable.Factory.of(adviceMethod)
                                ), userFactories),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(SUPPRESS_EXIT).resolve(TypeDescription.class));
                        this.enterType = enterType;
                    }

                    /**
                     * Resolves exit advice that handles exceptions depending on the specification of the exit advice.
                     *
                     * @param adviceMethod  The advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param enterType     The type of the value supplied by the enter advice method or
                     *                      a description of {@code void} if no such value exists.
                     * @return An appropriate exit handler.
                     */
                    protected static Resolved.ForMethodExit of(MethodDescription.InDefinedShape adviceMethod,
                                                               List<? extends OffsetMapping.Factory<?>> userFactories,
                                                               TypeDefinition enterType) {
                        TypeDescription throwable = adviceMethod.getDeclaredAnnotations()
                                .ofType(OnMethodExit.class)
                                .getValue(ON_THROWABLE).resolve(TypeDescription.class);
                        return throwable.represents(NoExceptionHandler.class)
                                ? new WithoutExceptionHandler(adviceMethod, userFactories, enterType)
                                : new WithExceptionHandler(adviceMethod, userFactories, enterType, throwable);
                    }

                    @Override
                    protected Bound.ForMethodExit resolve(TypeDescription instrumentedType,
                                                          MethodDescription instrumentedMethod,
                                                          MethodVisitor methodVisitor,
                                                          Implementation.Context implementationContext,
                                                          Assigner assigner,
                                                          MethodSizeHandler.ForInstrumentedMethod methodSizeHandler,
                                                          StackMapFrameHandler.ForInstrumentedMethod stackMapFrameHandler,
                                                          StackManipulation exceptionHandler) {
                        List<OffsetMapping.Target> offsetMappings = new ArrayList<OffsetMapping.Target>(this.offsetMappings.size());
                        for (OffsetMapping offsetMapping : this.offsetMappings) {
                            offsetMappings.add(offsetMapping.resolve(instrumentedType,
                                    instrumentedMethod,
                                    assigner,
                                    OffsetMapping.Context.ForMethodExit.of(enterType)));
                        }
                        return new AdviceMethodWriter.ForMethodExit(adviceMethod,
                                instrumentedMethod,
                                offsetMappings,
                                methodVisitor,
                                implementationContext,
                                methodSizeHandler.bindExit(adviceMethod, getThrowable().represents(NoExceptionHandler.class)),
                                stackMapFrameHandler.bindExit(adviceMethod),
                                suppressionHandler.bind(exceptionHandler));
                    }

                    @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        if (!super.equals(object)) return false;
                        Delegating.Resolved.ForMethodExit that = (Delegating.Resolved.ForMethodExit) object;
                        return enterType.equals(that.enterType);
                    }

                    @Override // HE: Remove after Lombok resolves ambiguous type names correctly.
                    public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + enterType.hashCode();
                        return result;
                    }

                    /**
                     * Implementation of exit advice that handles exceptions.
                     */
                    @AutoValue
                    protected static class WithExceptionHandler extends Delegating.Resolved.ForMethodExit {

                        /**
                         * The type of the handled throwable type for which this advice is invoked.
                         */
                        private final TypeDescription throwable;

                        /**
                         * Creates a new resolved dispatcher for implementing method exit advice that handles exceptions.
                         *
                         * @param adviceMethod  The represented advice method.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param enterType     The type of the value supplied by the enter advice method or
                         *                      a description of {@code void} if no such value exists.
                         * @param throwable     The type of the handled throwable type for which this advice is invoked.
                         */
                        protected WithExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                       List<? extends OffsetMapping.Factory<?>> userFactories,
                                                       TypeDefinition enterType,
                                                       TypeDescription throwable) {
                            super(adviceMethod, userFactories, enterType);
                            this.throwable = throwable;
                        }

                        @Override
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
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param enterType     The type of the value supplied by the enter advice method or
                         *                      a description of {@code void} if no such value exists.
                         */
                        protected WithoutExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                          List<? extends OffsetMapping.Factory<?>> userFactories,
                                                          TypeDefinition enterType) {
                            super(adviceMethod, userFactories, enterType);
                        }

                        @Override
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
    protected abstract static class AdviceVisitor extends ExceptionTableSensitiveMethodVisitor implements Dispatcher.Bound.SkipHandler {

        /**
         * Indicates a zero offset.
         */
        private static final int NO_OFFSET = 0;

        /**
         * The actual method visitor that is underlying this method visitor to which all instructions are written.
         */
        protected final MethodVisitor methodVisitor;

        /**
         * A description of the instrumented method.
         */
        protected final MethodDescription instrumentedMethod;

        /**
         * The required padding before using local variables after the instrumented method's arguments.
         */
        private final int padding;

        /**
         * The dispatcher to be used for method entry.
         */
        private final Dispatcher.Bound.ForMethodEnter methodEnter;

        /**
         * The dispatcher to be used for method exit.
         */
        protected final Dispatcher.Bound.ForMethodExit methodExit;

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
         * @param delegate              A delegate to which all instructions of the original method are written to. Must delegate to {@code methodVisitor}.
         * @param implementationContext The implementation context to use.
         * @param assigner              The assigner to use.
         * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
         * @param instrumentedType      A description of the instrumented type.
         * @param instrumentedMethod    The instrumented method.
         * @param methodEnter           The method enter advice.
         * @param methodExit            The method exit advice.
         * @param yieldedTypes          The types that are expected to be added after the instrumented method returns.
         * @param writerFlags           The ASM writer flags that were set.
         * @param readerFlags           The ASM reader flags that were set.
         */
        protected AdviceVisitor(MethodVisitor methodVisitor,
                                MethodVisitor delegate,
                                Context implementationContext,
                                Assigner assigner,
                                StackManipulation exceptionHandler,
                                TypeDescription instrumentedType,
                                MethodDescription instrumentedMethod,
                                Dispatcher.Resolved.ForMethodEnter methodEnter,
                                Dispatcher.Resolved.ForMethodExit methodExit,
                                List<? extends TypeDescription> yieldedTypes,
                                int writerFlags,
                                int readerFlags) {
            super(Opcodes.ASM6, delegate);
            this.methodVisitor = methodVisitor;
            this.instrumentedMethod = instrumentedMethod;
            padding = methodEnter.getEnterType().getStackSize().getSize();
            List<TypeDescription> requiredTypes = methodEnter.getEnterType().represents(void.class)
                    ? Collections.<TypeDescription>emptyList()
                    : Collections.singletonList(methodEnter.getEnterType().asErasure());
            methodSizeHandler = MethodSizeHandler.Default.of(instrumentedMethod, requiredTypes, yieldedTypes, writerFlags);
            stackMapFrameHandler = StackMapFrameHandler.Default.of(instrumentedType,
                    instrumentedMethod,
                    requiredTypes,
                    yieldedTypes,
                    implementationContext.getClassFileVersion(),
                    writerFlags,
                    readerFlags);
            this.methodEnter = methodEnter.bind(instrumentedType,
                    instrumentedMethod,
                    methodVisitor,
                    implementationContext,
                    assigner,
                    methodSizeHandler,
                    stackMapFrameHandler,
                    exceptionHandler);
            this.methodExit = methodExit.bind(instrumentedType,
                    instrumentedMethod,
                    methodVisitor,
                    implementationContext,
                    assigner,
                    methodSizeHandler,
                    stackMapFrameHandler,
                    exceptionHandler);
        }

        @Override
        protected void onAfterExceptionTable() {
            methodEnter.prepare();
            onUserPrepare();
            methodExit.prepare();
            methodEnter.apply(this);
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
            mv.visitVarInsn(opcode, resolve(offset));
        }

        @Override
        protected void onVisitIincInsn(int offset, int increment) {
            mv.visitIincInsn(resolve(offset), increment);
        }

        /**
         * Access the first variable after the instrumented variables and return type are stored.
         *
         * @param opcode The opcode for accessing the variable.
         */
        protected void variable(int opcode) {
            variable(opcode, NO_OFFSET);
        }

        /**
         * Access the first variable after the instrumented variables and return type are stored.
         *
         * @param opcode The opcode for accessing the variable.
         * @param offset The additional offset of the variable.
         */
        protected void variable(int opcode, int offset) {
            methodVisitor.visitVarInsn(opcode, instrumentedMethod.getStackSize() + padding + offset);
        }

        @Override
        public void visitFrame(int type, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
            stackMapFrameHandler.translateFrame(methodVisitor, type, localVariableLength, localVariable, stackSize, stack);
        }

        @Override
        public void visitMaxs(int stackSize, int localVariableLength) {
            onUserEnd();
            methodVisitor.visitMaxs(methodSizeHandler.compoundStackSize(stackSize), methodSizeHandler.compoundLocalVariableLength(localVariableLength));
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            mv.visitLocalVariable(name, descriptor, signature, start, end, resolve(index));
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeReference,
                                                              TypePath typePath,
                                                              Label[] start,
                                                              Label[] end,
                                                              int[] index,
                                                              String descriptor,
                                                              boolean visible) {
            return mv.visitLocalVariableAnnotation(typeReference, typePath, start, end, resolve(index), descriptor, visible);
        }

        /**
         * Resolves the index of a local variable in the context of the instrumentation.
         *
         * @param index The indices to adjust.
         * @return An array with adjusted indices.
         */
        private int[] resolve(int[] index) {
            int[] resolved = new int[index.length];
            for (int anIndex = 0; anIndex < index.length; anIndex++) {
                resolved[anIndex] = resolve(index[anIndex]);
            }
            return resolved;
        }

        /**
         * Resolves the index of a local variable in the context of the instrumentation.
         *
         * @param index The index to adjust.
         * @return The adjusted index.
         */
        private int resolve(int index) {
            return index < instrumentedMethod.getStackSize()
                    ? index
                    : padding + index;
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
             * @param methodEnter           The dispatcher to be used for method entry.
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
                        methodVisitor,
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

            @Override
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
             * {@code true} if the advice method ever returns non-exceptionally.
             */
            protected boolean doesReturn;

            /**
             * Creates an advice visitor that applies exit advice.
             *
             * @param methodVisitor         The method visitor for the instrumented method.
             * @param implementationContext The implementation context to use.
             * @param assigner              The assigner to use.
             * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
             * @param instrumentedType      A description of the instrumented type.
             * @param instrumentedMethod    A description of the instrumented method.
             * @param methodEnter           The dispatcher to be used for method entry.
             * @param methodExit            The dispatcher to be used for method exit.
             * @param yieldedTypes          The types that are expected to be added after the instrumented method returns.
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
                                     List<? extends TypeDescription> yieldedTypes,
                                     int writerFlags,
                                     int readerFlags) {
                super(methodVisitor,
                        new StackAwareMethodVisitor(methodVisitor, instrumentedMethod),
                        implementationContext,
                        assigner,
                        exceptionHandler,
                        instrumentedType,
                        instrumentedMethod,
                        methodEnter,
                        methodExit,
                        yieldedTypes,
                        writerFlags,
                        readerFlags);
                returnHandler = new Label();
                doesReturn = false;
            }

            @Override
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
                doesReturn = true;
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
                doesReturn = true;
            }

            @Override
            protected void onUserEnd() {
                Type returnType = Type.getType(instrumentedMethod.getReturnType().asErasure().getDescriptor());
                methodVisitor.visitLabel(returnHandler);
                if (doesReturn) {
                    stackMapFrameHandler.injectReturnFrame(methodVisitor);
                    if (!returnType.equals(Type.VOID_TYPE)) {
                        variable(returnType.getOpcode(Opcodes.ISTORE));
                    }
                }
                onUserReturn();
                methodExit.apply();
                onExitAdviceReturn();
                if (returnType.equals(Type.VOID_TYPE)) {
                    methodVisitor.visitInsn(Opcodes.RETURN);
                } else {
                    variable(returnType.getOpcode(Opcodes.ILOAD));
                    methodVisitor.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
                }
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
                 * @param methodEnter           The dispatcher to be used for method entry.
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
                    /* empty */
                }

                @Override
                protected void onUserStart() {
                    /* empty */
                }

                @Override
                protected void onUserReturn() {
                    if (!doesReturn || !instrumentedMethod.getReturnType().represents(void.class)) {
                        stackMapFrameHandler.injectCompletionFrame(methodVisitor, false);
                    }
                }

                @Override
                protected void onExitAdviceReturn() {
                    /* empty */
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
                 * Indicates the start of the user method.
                 */
                private final Label userStart;

                /**
                 * Indicates the exception handler.
                 */
                private final Label exceptionHandler;

                /**
                 * Creates a new advice visitor that captures exception by weaving try-catch blocks around user code.
                 *
                 * @param methodVisitor         The method visitor for the instrumented method.
                 * @param instrumentedType      A description of the instrumented type.
                 * @param implementationContext The implementation context to use.
                 * @param assigner              The assigner to use.
                 * @param exceptionHandler      The stack manipulation to apply within a suppression handler.
                 * @param instrumentedMethod    A description of the instrumented method.
                 * @param methodEnter           The dispatcher to be used for method entry.
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
                                    ? Collections.singletonList(TypeDescription.THROWABLE)
                                    : Arrays.asList(instrumentedMethod.getReturnType().asErasure(), TypeDescription.THROWABLE),
                            writerFlags,
                            readerFlags);
                    this.throwable = throwable;
                    userStart = new Label();
                    this.exceptionHandler = new Label();
                }

                @Override
                protected void onUserPrepare() {
                    methodVisitor.visitTryCatchBlock(userStart, returnHandler, exceptionHandler, throwable.getInternalName());
                }

                @Override
                protected void onUserStart() {
                    methodVisitor.visitLabel(userStart);
                }

                @Override
                protected void onUserReturn() {
                    Label endOfHandler = new Label();
                    if (doesReturn) {
                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        variable(Opcodes.ASTORE, instrumentedMethod.getReturnType().getStackSize().getSize());
                        methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfHandler);
                    }
                    methodVisitor.visitLabel(exceptionHandler);
                    stackMapFrameHandler.injectExceptionFrame(methodVisitor);
                    variable(Opcodes.ASTORE, instrumentedMethod.getReturnType().getStackSize().getSize());
                    storeDefaultReturn();
                    if (doesReturn) {
                        methodVisitor.visitLabel(endOfHandler);
                    }
                    stackMapFrameHandler.injectCompletionFrame(methodVisitor, false);
                }

                @Override
                protected void onExitAdviceReturn() {
                    variable(Opcodes.ALOAD, instrumentedMethod.getReturnType().getStackSize().getSize());
                    Label endOfHandler = new Label();
                    methodVisitor.visitJumpInsn(Opcodes.IFNULL, endOfHandler);
                    variable(Opcodes.ALOAD, instrumentedMethod.getReturnType().getStackSize().getSize());
                    methodVisitor.visitInsn(Opcodes.ATHROW);
                    methodVisitor.visitLabel(endOfHandler);
                    stackMapFrameHandler.injectCompletionFrame(methodVisitor, true);
                }

                /**
                 * Stores a default return value in the designated slot of the local variable array.
                 */
                private void storeDefaultReturn() {
                    if (instrumentedMethod.getReturnType().represents(boolean.class)
                            || instrumentedMethod.getReturnType().represents(byte.class)
                            || instrumentedMethod.getReturnType().represents(short.class)
                            || instrumentedMethod.getReturnType().represents(char.class)
                            || instrumentedMethod.getReturnType().represents(int.class)) {
                        methodVisitor.visitInsn(Opcodes.ICONST_0);
                        variable(Opcodes.ISTORE);
                    } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                        methodVisitor.visitInsn(Opcodes.LCONST_0);
                        variable(Opcodes.LSTORE);
                    } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                        methodVisitor.visitInsn(Opcodes.FCONST_0);
                        variable(Opcodes.FSTORE);
                    } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                        methodVisitor.visitInsn(Opcodes.DCONST_0);
                        variable(Opcodes.DSTORE);
                    } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        variable(Opcodes.ASTORE);
                    }
                }
            }
        }
    }

    /**
     * A byte code appender for implementing {@link Advice}.
     */
    @AutoValue
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

        @Override
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
                super(Opcodes.ASM6, methodVisitor);
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
         * When specifying a non-primitive type, this method's return value that is subject to an {@code instanceof} check where
         * the instrumented method is only executed, if the returned instance is {@code not} an instance of the specified class.
         * Alternatively, it is possible to specify either {@link OnDefaultValue} or {@link OnNonDefaultValue} where the instrumented
         * method is only executed if the advice method returns a default or non-default value of the advice method's return type.
         * It is illegal to specify a primitive type as an argument whereas setting the value to {@code void} indicates that the
         * instrumented method should never be skipped.
         *
         * @return A value defining what return values of the advice method indicate that the instrumented method
         * should be skipped or {@code void} if the instrumented method should never be skipped.
         */
        Class<?> skipOn() default void.class;

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
         * Indicates a {@link Throwable} super type for which this exit advice is invoked if it was thrown from the instrumented method.
         * If an exception is thrown, it is available via the {@link Thrown} parameter annotation. If a method returns exceptionally,
         * any parameter annotated with {@link Return} is assigned the parameter type's default value.
         *
         * @return The type of {@link Throwable} for which this exit advice handler is invoked.
         */
        Class<? extends Throwable> onThrowable() default NoExceptionHandler.class;

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
         * Determines if the parameter should be assigned {@code null} if the instrumented method is static or a constructor within
         * an entry method.
         *
         * @return {@code true} if the value assignment is optional.
         */
        boolean optional() default false;

        /**
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
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
    }

    /**
     * Indicates that the annotated parameter should be mapped to the parameter with index {@link Argument#value()} of
     * the instrumented method.
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
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the parameter of the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented methods parameter.
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
     * Assigns an array containing all arguments of the instrumented method to the annotated parameter. The annotated parameter must
     * be an array type. If the annotation indicates writability, the assigned array must have at least as many values as the
     * instrumented method or an {@link ArrayIndexOutOfBoundsException} is thrown.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface AllArguments {

        /**
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the type declaring the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented method's declaring type.
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
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the return value of the instrumented method. If the instrumented
     * method terminates exceptionally, the type's default value is assigned to the parameter, i.e. {@code 0} for numeric types
     * and {@code null} for reference types. If the return type is {@code void}, the annotated value is {@code null} if and only if
     * {@link Return#typing()} is set to {@link Assigner.Typing#DYNAMIC}.
     * </p>
     * <p>
     * <b>Note</b>: This annotation must only be used on exit advice methods.
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
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the parameter of the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented methods parameter.
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
     * <b>Note</b>: This annotation must only be used on exit advice methods.
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
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, it is illegal to
         * write to the annotated parameter. If this property is set to {@code true}, the annotated parameter can either be set
         * to {@code null} to suppress an exception that was thrown by the adviced method or it can be set to any other exception
         * that will be thrown after the advice method returned.
         * </p>
         * <p>
         * If an exception is suppressed, the default value for the return type is returned from the method, i.e. {@code 0} for any
         * numeric type and {@code null} for a reference type. The default value can be replaced via the {@link Return} annotation.
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
     * Indicates that the annotated parameter should be mapped to a field in the scope of the instrumented method.
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
        String value();

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
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the mapped field type if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the  annotated parameter can be any super type of the field type.
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
     * Indicates that the annotated parameter should be mapped to a string representation of the instrumented method,
     * a constant representing the {@link Class} declaring the adviced method or a {@link Method}, {@link Constructor}
     * or {@code java.lang.reflect.Executable} representing this method.
     * </p>
     * <p>
     * <b>Note</b>: A constant representing a {@link Method} or {@link Constructor} is not cached but is recreated for
     * every read.
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
     * <p><b>Note</b></p>: This annotation must only be used within an exit advice and is only meaningful in combination with an entry advice.
     *
     * @see Advice
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Enter {

        /**
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the parameter of the instrumented method if the typing is not also set to {@link Assigner.Typing#DYNAMIC}.
         * If this property is set to {@code true}, the annotated parameter can be any super type of the instrumented methods parameter.
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
     * Indicates that the annotated parameter should always return a default a boxed version of the instrumented methods return value
     * (i.e. {@code 0} for numeric values, {@code false} for {@code boolean} types and {@code null} for reference types). The annotated
     * parameter must be of type {@link Object} and cannot be assigned a value.
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
     * Indicates that the annotated parameter should always return a default value (i.e. {@code 0} for numeric values, {@code false}
     * for {@code boolean} types and {@code null} for reference types). Any assignments to this variable are without any effect.
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
     * A builder step for creating an {@link Advice} that uses custom mappings of annotations to constant pool values.
     */
    @AutoValue
    public static class WithCustomMapping {

        /**
         * A map containing dynamically computed constant pool values that are mapped by their triggering annotation type.
         */
        private final Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings;

        /**
         * Creates a new custom mapping builder step without including any custom mappings.
         */
        protected WithCustomMapping() {
            this(Collections.<Class<? extends Annotation>, OffsetMapping.Factory<?>>emptyMap());
        }

        /**
         * Creates a new custom mapping builder step with the given custom mappings.
         *
         * @param offsetMappings A map containing dynamically computed constant pool values that are mapped by their triggering annotation type.
         */
        protected WithCustomMapping(Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings) {
            this.offsetMappings = offsetMappings;
        }

        /**
         * Binds the supplied annotation to a type constant of the supplied value. Constants can be strings, method handles, method types
         * and any primitive or the value {@code null}.
         *
         * @param type  The type of the annotation being bound.
         * @param value The value to bind to the annotation.
         * @param <T>   The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<T> type, Object value) {
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
            return bind(type, new TypeDescription.ForLoadedType(value));
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
            Map<Class<? extends Annotation>, OffsetMapping.Factory<?>> offsetMappings = new HashMap<Class<? extends Annotation>, OffsetMapping.Factory<?>>(this.offsetMappings);
            if (!offsetMapping.getAnnotationType().isAnnotation()) {
                throw new IllegalArgumentException("Not an annotation type: " + offsetMapping.getAnnotationType());
            } else if (offsetMappings.put(offsetMapping.getAnnotationType(), offsetMapping) != null) {
                throw new IllegalArgumentException("Annotation type already mapped: " + offsetMapping.getAnnotationType());
            }
            return new WithCustomMapping(offsetMappings);
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
            return to(new TypeDescription.ForLoadedType(advice), classFileLocator);
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods.
         *
         * @param advice           A description of the type declaring the advice.
         * @param classFileLocator The class file locator for locating the advisory class's class file.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(TypeDescription advice, ClassFileLocator classFileLocator) {
            return Advice.to(advice, classFileLocator, new ArrayList<OffsetMapping.Factory<?>>(offsetMappings.values()));
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
            return to(new TypeDescription.ForLoadedType(enterAdvice), new TypeDescription.ForLoadedType(exitAdvice), classFileLocator);
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
            return Advice.to(enterAdvice, exitAdvice, classFileLocator, new ArrayList<OffsetMapping.Factory<?>>(offsetMappings.values()));
        }
    }

    /**
     * A marker class that indicates that an advice method does not suppress any {@link Throwable}.
     */
    private static class NoExceptionHandler extends Throwable {

        /**
         * A description of the {@link NoExceptionHandler} type.
         */
        private static final TypeDescription DESCRIPTION = new TypeDescription.ForLoadedType(NoExceptionHandler.class);

        /**
         * A private constructor as this class is not supposed to be invoked.
         */
        private NoExceptionHandler() {
            throw new UnsupportedOperationException("This marker class is not supposed to be instantiated");
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
            throw new UnsupportedOperationException("This marker class is not supposed to be instantiated");
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
            throw new UnsupportedOperationException("This marker class is not supposed to be instantiated");
        }
    }
}

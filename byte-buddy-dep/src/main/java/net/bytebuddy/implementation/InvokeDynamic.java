/*
 * Copyright 2014 - 2020 Rafael Winterhalter
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
package net.bytebuddy.implementation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * An implementation that applies a
 * <a href="http://docs.oracle.com/javase/8/docs/api/java/lang/invoke/package-summary.html">dynamic method invocation</a>.
 */
@HashCodeAndEqualsPlugin.Enhance
public class InvokeDynamic implements Implementation.Composable {

    /**
     * The bootstrap method.
     */
    protected final MethodDescription.InDefinedShape bootstrap;

    /**
     * The arguments that are provided to the bootstrap method.
     */
    protected final List<?> arguments;

    /**
     * The target provided that identifies the method to be bootstrapped.
     */
    protected final InvocationProvider invocationProvider;

    /**
     * A handler that handles the method return.
     */
    protected final TerminationHandler terminationHandler;

    /**
     * The assigner to be used.
     */
    protected final Assigner assigner;

    /**
     * Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected final Assigner.Typing typing;

    /**
     * Creates a new invoke dynamic implementation.
     *
     * @param bootstrap          The bootstrap method.
     * @param arguments          The arguments that are provided to the bootstrap method.
     * @param invocationProvider The target provided that identifies the method to be bootstrapped.
     * @param terminationHandler A handler that handles the method return.
     * @param assigner           The assigner to be used.
     * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected InvokeDynamic(MethodDescription.InDefinedShape bootstrap,
                            List<?> arguments,
                            InvocationProvider invocationProvider,
                            TerminationHandler terminationHandler,
                            Assigner assigner,
                            Assigner.Typing typing) {
        this.bootstrap = bootstrap;
        this.arguments = arguments;
        this.invocationProvider = invocationProvider;
        this.terminationHandler = terminationHandler;
        this.assigner = assigner;
        this.typing = typing;
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method.
     *
     * @param method   The bootstrap method that is used to link the instrumented method.
     * @param constant The constant values passed to the bootstrap method. Values can be represented either
     *                 as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
     *                 {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Method method, Object... constant) {
        return bootstrap(new MethodDescription.ForLoadedMethod(method), constant);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method.
     *
     * @param method    The bootstrap method that is used to link the instrumented method.
     * @param constants The constant values passed to the bootstrap method. Values can be represented either
     *                  as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
     *                  {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Method method, List<?> constants) {
        return bootstrap(new MethodDescription.ForLoadedMethod(method), constants);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap constructor.
     *
     * @param constructor The bootstrap constructor that is used to link the instrumented method.
     * @param constant    The constant values passed to the bootstrap method. Values can be represented either
     *                    as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
     *                    {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Constructor<?> constructor, Object... constant) {
        return bootstrap(new MethodDescription.ForLoadedConstructor(constructor), constant);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap constructor.
     *
     * @param constructor The bootstrap constructor that is used to link the instrumented method.
     * @param constants   The constant values passed to the bootstrap method. Values can be represented either
     *                    as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
     *                    {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Constructor<?> constructor, List<?> constants) {
        return bootstrap(new MethodDescription.ForLoadedConstructor(constructor), constants);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method or constructor.
     *
     * @param bootstrap The bootstrap method or constructor that is used to link the instrumented method.
     * @param constant  The constant values passed to the bootstrap method. Values can be represented either
     *                  as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
     *                  {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(MethodDescription.InDefinedShape bootstrap, Object... constant) {
        return bootstrap(bootstrap, Arrays.asList(constant));
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method or constructor.
     *
     * @param bootstrap The bootstrap method or constructor that is used to link the instrumented method.
     * @param constants The constant values passed to the bootstrap method. Values can be represented either
     *                  as {@link TypeDescription}, as {@link JavaConstant}, as {@link String} or a primitive
     *                  {@code int}, {@code long}, {@code float} or {@code double} represented as wrapper type.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(MethodDescription.InDefinedShape bootstrap, List<?> constants) {
        List<Object> arguments = new ArrayList<Object>(constants.size());
        List<TypeDescription> types = new ArrayList<TypeDescription>(constants.size());
        for (Object constant : constants) {
            if (constant instanceof JavaConstant) {
                arguments.add(((JavaConstant) constant).asConstantPoolValue());
                types.add(((JavaConstant) constant).getType());
            } else if (constant instanceof TypeDescription) {
                arguments.add(Type.getType(((TypeDescription) constant).getDescriptor()));
                types.add(TypeDescription.CLASS);
            } else {
                arguments.add(constant);
                TypeDescription typeDescription = TypeDescription.ForLoadedType.of(constant.getClass()).asUnboxed();
                types.add(typeDescription);
                if (JavaType.METHOD_TYPE.isInstance(constant) || JavaType.METHOD_HANDLE.isInstance(constant)) {
                    throw new IllegalArgumentException("Must be represented as a JavaConstant instance: " + constant);
                } else if (constant instanceof Class<?>) {
                    throw new IllegalArgumentException("Must be represented as a TypeDescription instance: " + constant);
                } else if (!typeDescription.isCompileTimeConstant()) {
                    throw new IllegalArgumentException("Not a compile-time constant: " + constant);
                }
            }
        }
        if (!bootstrap.isInvokeBootstrap(types)) {
            throw new IllegalArgumentException("Not a valid bootstrap method " + bootstrap + " for " + arguments);
        }
        return new WithImplicitTarget(bootstrap,
                arguments,
                new InvocationProvider.Default(),
                TerminationHandler.RETURNING,
                Assigner.DEFAULT,
                Assigner.Typing.STATIC);
    }

    /**
     * <p>
     * Creates a lambda expression using the JVM's lambda meta factory. The method that is implementing the lambda expression is provided
     * the explicit arguments first and the functional interface's method second.
     * </p>
     * <p>
     * <b>Important</b>: Byte Buddy does not validate that the provided arguments are correct considering the required arguments of the bound
     * functional interface. Binding an incorrect number of arguments or arguments of incompatible types does not create illegal byte code
     * but yields a runtime error when the call site is first used. This is done to support future extensions or alternative implementations
     * of the Java virtual machine.
     * </p>
     *
     * @param method              The method that implements the lambda expression.
     * @param functionalInterface The functional interface that is an instance of the lambda expression.
     * @return A builder for creating a lambda expression.
     */
    public static WithImplicitArguments lambda(Method method, Class<?> functionalInterface) {
        return lambda(new MethodDescription.ForLoadedMethod(method), TypeDescription.ForLoadedType.of(functionalInterface));
    }

    /**
     * <p>
     * Creates a lambda expression using the JVM's lambda meta factory. The method that is implementing the lambda expression is provided
     * the explicit arguments first and the functional interface's method second.
     * </p>
     * <p>
     * <b>Important</b>: Byte Buddy does not validate that the provided arguments are correct considering the required arguments of the bound
     * functional interface. Binding an incorrect number of arguments or arguments of incompatible types does not create illegal byte code
     * but yields a runtime error when the call site is first used. This is done to support future extensions or alternative implementations
     * of the Java virtual machine.
     * </p>
     *
     * @param method              The method that implements the lambda expression.
     * @param functionalInterface The functional interface that is an instance of the lambda expression.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A builder for creating a lambda expression.
     */
    public static WithImplicitArguments lambda(Method method, Class<?> functionalInterface, MethodGraph.Compiler methodGraphCompiler) {
        return lambda(new MethodDescription.ForLoadedMethod(method), TypeDescription.ForLoadedType.of(functionalInterface), methodGraphCompiler);
    }

    /**
     * <p>
     * Creates a lambda expression using the JVM's lambda meta factory. The method that is implementing the lambda expression is provided
     * the explicit arguments first and the functional interface's method second.
     * </p>
     * <p>
     * <b>Important</b>: Byte Buddy does not validate that the provided arguments are correct considering the required arguments of the bound
     * functional interface. Binding an incorrect number of arguments or arguments of incompatible types does not create illegal byte code
     * but yields a runtime error when the call site is first used. This is done to support future extensions or alternative implementations
     * of the Java virtual machine.
     * </p>
     *
     * @param methodDescription   The method that implements the lambda expression.
     * @param functionalInterface The functional interface that is an instance of the lambda expression.
     * @return A builder for creating a lambda expression.
     */
    public static WithImplicitArguments lambda(MethodDescription.InDefinedShape methodDescription, TypeDescription functionalInterface) {
        return lambda(methodDescription, functionalInterface, MethodGraph.Compiler.Default.forJavaHierarchy());
    }

    /**
     * <p>
     * Creates a lambda expression using the JVM's lambda meta factory. The method that is implementing the lambda expression is provided
     * the explicit arguments first and the functional interface's method second.
     * </p>
     * <p>
     * <b>Important</b>: Byte Buddy does not validate that the provided arguments are correct considering the required arguments of the bound
     * functional interface. Binding an incorrect number of arguments or arguments of incompatible types does not create illegal byte code
     * but yields a runtime error when the call site is first used. This is done to support future extensions or alternative implementations
     * of the Java virtual machine.
     * </p>
     *
     * @param methodDescription   The method that implements the lambda expression.
     * @param functionalInterface The functional interface that is an instance of the lambda expression.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A builder for creating a lambda expression.
     */
    public static WithImplicitArguments lambda(MethodDescription.InDefinedShape methodDescription,
                                               TypeDescription functionalInterface,
                                               MethodGraph.Compiler methodGraphCompiler) {
        if (!functionalInterface.isInterface()) {
            throw new IllegalArgumentException(functionalInterface + " is not an interface type");
        }
        MethodList<?> methods = methodGraphCompiler.compile(functionalInterface)
                .listNodes()
                .asMethodList()
                .filter(isAbstract());
        if (methods.size() != 1) {
            throw new IllegalArgumentException(functionalInterface + " does not define exactly one abstract method: " + methods);
        }
        return bootstrap(new MethodDescription.Latent(new TypeDescription.Latent("java.lang.invoke.LambdaMetafactory",
                        Opcodes.ACC_PUBLIC,
                        TypeDescription.Generic.OBJECT),
                        "metafactory",
                        Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                        Collections.<TypeVariableToken>emptyList(),
                        JavaType.CALL_SITE.getTypeStub().asGenericType(),
                        Arrays.asList(new ParameterDescription.Token(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().asGenericType()),
                                new ParameterDescription.Token(TypeDescription.STRING.asGenericType()),
                                new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType()),
                                new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType()),
                                new ParameterDescription.Token(JavaType.METHOD_HANDLE.getTypeStub().asGenericType()),
                                new ParameterDescription.Token(JavaType.METHOD_TYPE.getTypeStub().asGenericType())),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED),
                JavaConstant.MethodType.of(methods.asDefined().getOnly()),
                JavaConstant.MethodHandle.of(methodDescription),
                JavaConstant.MethodType.of(methods.asDefined().getOnly())).invoke(methods.asDefined().getOnly().getInternalName());
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code boolean} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withBooleanValue(boolean... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (boolean aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForBooleanConstant(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code byte} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withByteValue(byte... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (byte aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForByteConstant(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code short} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withShortValue(short... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (short aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForShortConstant(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code char} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withCharacterValue(char... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (char aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForCharacterConstant(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code int} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withIntegerValue(int... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (int aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForIntegerConstant(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code long} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withLongValue(long... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (long aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForLongConstant(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code float} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withFloatValue(float... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (float aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForFloatConstant(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code double} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withDoubleValue(double... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (double aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForDoubleConstant(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * <p>
     * Requires the bootstrap method to bootstrap a method that takes the specified arguments as its next parameters.
     * Note that any primitive parameters are passed as their wrapper types. Furthermore, values that can be stored
     * in the instrumented class's constant pool might be of different object identity when passed to the
     * bootstrapped method or might not be visible to the the created class what later results in a runtime error.
     * </p>
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withValue(Object... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (Object aValue : value) {
            argumentProviders.add(InvocationProvider.ArgumentProvider.ConstantPoolWrapper.of(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * <p>
     * Requires the bootstrap method to bootstrap a method that takes the specified argument as its next parameter while
     * allowing to specify the value to be of a different type than the actual instance type.
     * </p>
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public WithImplicitType withReference(Object value) {
        return new WithImplicitType.OfInstance(bootstrap,
                arguments,
                invocationProvider,
                terminationHandler,
                assigner,
                typing,
                value);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified arguments as its next parameters.
     * Note that any primitive parameters are passed as their wrapper types. Any value that is passed to the
     * bootstrapped method is guaranteed to be of the same object identity.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withReference(Object... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (Object aValue : value) {
            argumentProviders.add(InvocationProvider.ArgumentProvider.ForInstance.of(aValue));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Hands the provided types to the dynamically bound method. The type is stored in the generated class's
     * constant pool and is loaded at invocation time. For this to be possible, the created class's
     * class loader must be able to see the provided type.
     *
     * @param typeDescription The classes to provide to the bound method as an argument.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified type.
     */
    public InvokeDynamic withType(TypeDescription... typeDescription) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(typeDescription.length);
        for (TypeDescription aTypeDescription : typeDescription) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForClassConstant(aTypeDescription));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Hands the provided enumerations to the dynamically bound method. The enumeration values are read from
     * the enumeration class on demand. For this to be possible, the created class's class loader must be
     * able to see the enumeration type.
     *
     * @param enumerationDescription The enumeration values to provide to the bound method as an argument.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified enumerations.
     */
    public InvokeDynamic withEnumeration(EnumerationDescription... enumerationDescription) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(enumerationDescription.length);
        for (EnumerationDescription anEnumerationDescription : enumerationDescription) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForEnumerationValue(anEnumerationDescription));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Hands the provided Java instance to the dynamically bound method. The instance is stored in the generated class's
     * constant pool and is loaded at invocation time. For this to be possible, the created class's class loader must
     * be able to create the provided Java instance.
     *
     * @param javaConstant The Java instance to provide to the bound method as an argument.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified Java instance.
     */
    public InvokeDynamic withInstance(JavaConstant... javaConstant) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(javaConstant.length);
        for (JavaConstant aJavaConstant : javaConstant) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForJavaConstant(aJavaConstant));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Passes {@code null} values of the given types to the bootstrapped method.
     *
     * @param type The type that the {@code null} values should represent.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withNullValue(Class<?>... type) {
        return withNullValue(new TypeList.ForLoadedTypes(type).toArray(new TypeDescription[0]));
    }

    /**
     * Passes {@code null} values of the given types to the bootstrapped method.
     *
     * @param typeDescription The type that the {@code null} values should represent.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withNullValue(TypeDescription... typeDescription) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(typeDescription.length);
        for (TypeDescription aTypeDescription : typeDescription) {
            if (aTypeDescription.isPrimitive()) {
                throw new IllegalArgumentException("Cannot assign null to primitive type: " + aTypeDescription);
            }
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForNullValue(aTypeDescription));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Passes parameters of the instrumented method to the bootstrapped method.
     *
     * @param index The indices of the parameters that should be passed to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withArgument(int... index) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(index.length);
        for (int anIndex : index) {
            if (anIndex < 0) {
                throw new IllegalArgumentException("Method parameter indices cannot be negative: " + anIndex);
            }
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForMethodParameter(anIndex));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Passes a parameter of the instrumented method to the bootstrapped method.
     *
     * @param index The index of the parameter that should be passed to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified argument
     * with its implicit type.
     */
    public WithImplicitType withArgument(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Method parameter indices cannot be negative: " + index);
        }
        return new WithImplicitType.OfArgument(bootstrap,
                arguments,
                invocationProvider,
                terminationHandler,
                assigner,
                typing,
                index);
    }

    /**
     * Passes references to {@code this} onto the operand stack where the instance is represented as
     * the given types.
     *
     * @param type The types as which the {@code this} reference of the intercepted method should be masked.
     * @return This implementation where {@code this} references are passed as the next arguments.
     */
    public InvokeDynamic withThis(Class<?>... type) {
        return withThis(new TypeList.ForLoadedTypes(type).toArray(new TypeDescription[0]));
    }

    /**
     * Passes references to {@code this} onto the operand stack where the instance is represented as
     * the given types.
     *
     * @param typeDescription The types as which the {@code this} reference of the intercepted method should be masked.
     * @return This implementation where {@code this} references are passed as the next arguments.
     */
    public InvokeDynamic withThis(TypeDescription... typeDescription) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(typeDescription.length);
        for (TypeDescription aTypeDescription : typeDescription) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForThisInstance(aTypeDescription));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Adds all method arguments to the the bootstrapped method.
     *
     * @return This invoke dynamic implementation with all parameters of the instrumented method added.
     */
    public InvokeDynamic withMethodArguments() {
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArgument(InvocationProvider.ArgumentProvider.ForInterceptedMethodParameters.INSTANCE),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Adds a potential {@code this} reference and all method arguments to the the bootstrapped method.
     *
     * @return This invoke dynamic implementation with a potential {@code this} reference and all
     * parameters of the instrumented method added.
     */
    public InvokeDynamic withImplicitAndMethodArguments() {
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArgument(InvocationProvider.ArgumentProvider.ForInterceptedMethodInstanceAndParameters.INSTANCE),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Passes the values of the specified fields to the bootstrap method. Any of the specified fields must already
     * exist for the instrumented type.
     *
     * @param name The names of the fields to be passed to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withField(String... name) {
        return withField(FieldLocator.ForClassHierarchy.Factory.INSTANCE, name);
    }

    /**
     * Passes the values of the specified fields to the bootstrap method. Any of the specified fields must already
     * exist for the instrumented type.
     *
     * @param fieldLocatorFactory The field locator factory to use.
     * @param name                The names of the fields to be passed to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withField(FieldLocator.Factory fieldLocatorFactory, String... name) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(name.length);
        for (String aName : name) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForField(aName, fieldLocatorFactory));
        }
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Passes the values of the specified fields to the bootstrap method. Any of the specified fields must already
     * exist for the instrumented type.
     *
     * @param name The names of the fields to be passed to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public WithImplicitType withField(String name) {
        return withField(name, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
    }

    /**
     * Passes the values of the specified fields to the bootstrap method. Any of the specified fields must already
     * exist for the instrumented type.
     *
     * @param fieldLocatorFactory The field locator factory to use.
     * @param name                The names of the fields to be passed to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public WithImplicitType withField(String name, FieldLocator.Factory fieldLocatorFactory) {
        return new WithImplicitType.OfField(bootstrap,
                arguments,
                invocationProvider,
                terminationHandler,
                assigner,
                typing,
                name,
                fieldLocatorFactory);
    }

    /**
     * Instructs this implementation to use the provided assigner and decides if the assigner should apply
     * dynamic typing.
     *
     * @param assigner The assigner to use.
     * @param typing   {@code true} if the assigner should attempt dynamic typing.
     * @return The invoke dynamic instruction where the given assigner and dynamic-typing directive are applied.
     */
    public Implementation.Composable withAssigner(Assigner assigner, Assigner.Typing typing) {
        return new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * {@inheritDoc}
     */
    public Implementation andThen(Implementation implementation) {
        return new Implementation.Compound(new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider,
                TerminationHandler.DROPPING,
                assigner,
                typing),
                implementation);
    }

    /**
     * {@inheritDoc}
     */
    public Composable andThen(Composable implementation) {
        return new Implementation.Compound.Composable(new InvokeDynamic(bootstrap,
                arguments,
                invocationProvider,
                TerminationHandler.DROPPING,
                assigner,
                typing),
                implementation);
    }

    /**
     * {@inheritDoc}
     */
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return invocationProvider.prepare(instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(implementationTarget.getInstrumentedType());
    }

    /**
     * An invocation provider is responsible for loading the arguments of the invoked method onto the operand
     * stack and for creating the actual <i>invoke dynamic</i> instruction.
     */
    protected interface InvocationProvider {

        /**
         * Creates a target for the invocation.
         *
         * @param methodDescription The method that is being intercepted.
         * @return The target for the invocation.
         */
        Target make(MethodDescription methodDescription);

        /**
         * Appends the given arguments to the invocation to be loaded onto the operand stack.
         *
         * @param argumentProviders The next arguments to be loaded onto the operand stack.
         * @return An invocation provider for this target that loads the given arguments onto the operand stack.
         */
        InvocationProvider appendArguments(List<ArgumentProvider> argumentProviders);

        /**
         * Appends the given argument to the invocation to be loaded onto the operand stack.
         *
         * @param argumentProvider The next argument to be loaded onto the operand stack.
         * @return An invocation provider for this target that loads the given arguments onto the operand stack.
         */
        InvocationProvider appendArgument(ArgumentProvider argumentProvider);

        /**
         * Returns a copy of this invocation provider that does not add any arguments.
         *
         * @return A copy of this invocation provider that does not add any arguments.
         */
        InvocationProvider withoutArguments();

        /**
         * Returns a copy of this invocation provider that applies the given name provider.
         *
         * @param nameProvider The name provider to be used.
         * @return A copy of this invocation provider that applies the given name provider.
         */
        InvocationProvider withNameProvider(NameProvider nameProvider);

        /**
         * Returns a copy of this invocation provider that applies the given return type provider.
         *
         * @param returnTypeProvider The return type provider to be used.
         * @return A copy of this invocation provider that applies the given return type provider.
         */
        InvocationProvider withReturnTypeProvider(ReturnTypeProvider returnTypeProvider);

        /**
         * Prepares the instrumented type.
         *
         * @param instrumentedType The instrumented type to prepare.
         * @return The prepared instrumented type.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);

        /**
         * A target for a dynamic method invocation.
         */
        interface Target {

            /**
             * Resolves the target.
             *
             * @param instrumentedType The instrumented type.
             * @param assigner         The assigner to be used.
             * @param typing           Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @return The resolved target.
             */
            Resolved resolve(TypeDescription instrumentedType, Assigner assigner, Assigner.Typing typing);

            /**
             * Represents a resolved {@link net.bytebuddy.implementation.InvokeDynamic.InvocationProvider.Target}.
             */
            interface Resolved {

                /**
                 * Returns the stack manipulation that loads the arguments onto the operand stack.
                 *
                 * @return The stack manipulation that loads the arguments onto the operand stack.
                 */
                StackManipulation getStackManipulation();

                /**
                 * Returns the requested return type.
                 *
                 * @return The requested return type.
                 */
                TypeDescription getReturnType();

                /**
                 * Returns the internal name of the requested method.
                 *
                 * @return The internal name of the requested method.
                 */
                String getInternalName();

                /**
                 * Returns the types of the values on the operand stack.
                 *
                 * @return The types of the values on the operand stack.
                 */
                List<TypeDescription> getParameterTypes();

                /**
                 * A simple implementation of
                 * {@link net.bytebuddy.implementation.InvokeDynamic.InvocationProvider.Target.Resolved}.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Simple implements Resolved {

                    /**
                     * The stack manipulation that loads the arguments onto the operand stack.
                     */
                    private final StackManipulation stackManipulation;

                    /**
                     * The internal name of the requested method.
                     */
                    private final String internalName;

                    /**
                     * The requested return type.
                     */
                    private final TypeDescription returnType;

                    /**
                     * The types of the values on the operand stack.
                     */
                    private final List<TypeDescription> parameterTypes;

                    /**
                     * Creates a new simple instance.
                     *
                     * @param stackManipulation The stack manipulation that loads the arguments onto the operand stack.
                     * @param internalName      The internal name of the requested method.
                     * @param returnType        The requested return type.
                     * @param parameterTypes    The types of the values on the operand stack.
                     */
                    public Simple(StackManipulation stackManipulation,
                                  String internalName,
                                  TypeDescription returnType,
                                  List<TypeDescription> parameterTypes) {
                        this.stackManipulation = stackManipulation;
                        this.internalName = internalName;
                        this.returnType = returnType;
                        this.parameterTypes = parameterTypes;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation getStackManipulation() {
                        return stackManipulation;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDescription getReturnType() {
                        return returnType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public String getInternalName() {
                        return internalName;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public List<TypeDescription> getParameterTypes() {
                        return parameterTypes;
                    }
                }
            }
        }

        /**
         * An argument provider is responsible for loading arguments to a bootstrapped method onto the operand
         * stack and providing the types of these arguments.
         */
        interface ArgumentProvider {

            /**
             * Resolves an argument provider.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param assigner           The assigner to be used.
             * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @return A resolved version of this argument provider.
             */
            Resolved resolve(TypeDescription instrumentedType,
                             MethodDescription instrumentedMethod,
                             Assigner assigner,
                             Assigner.Typing typing);

            /**
             * Prepares the instrumented type.
             *
             * @param instrumentedType The instrumented type.
             * @return The prepared instrumented type.
             */
            InstrumentedType prepare(InstrumentedType instrumentedType);

            /**
             * An argument provider that loads a reference to the intercepted instance and all arguments of
             * the intercepted method.
             */
            enum ForInterceptedMethodInstanceAndParameters implements ArgumentProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                            instrumentedMethod.isStatic()
                                    ? instrumentedMethod.getParameters().asTypeList().asErasures()
                                    : CompoundList.of(instrumentedMethod.getDeclaringType().asErasure(), instrumentedMethod.getParameters().asTypeList().asErasures()));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider that loads all arguments of the intercepted method.
             */
            enum ForInterceptedMethodParameters implements ArgumentProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(MethodVariableAccess.allArgumentsOf(instrumentedMethod),
                            instrumentedMethod.getParameters().asTypeList().asErasures());
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * Represents wrapper types and types that could be stored in a class's constant pool as such
             * constant pool values.
             */
            enum ConstantPoolWrapper {

                /**
                 * Stores a {@link java.lang.Boolean} as a {@code boolean} and wraps it on load.
                 */
                BOOLEAN(boolean.class, Boolean.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Boolean) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Byte} as a {@code byte} and wraps it on load.
                 */
                BYTE(byte.class, Byte.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Byte) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Short} as a {@code short} and wraps it on load.
                 */
                SHORT(short.class, Short.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Short) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Character} as a {@code char} and wraps it on load.
                 */
                CHARACTER(char.class, Character.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Character) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Integer} as a {@code int} and wraps it on load.
                 */
                INTEGER(int.class, Integer.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Integer) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Long} as a {@code long} and wraps it on load.
                 */
                LONG(long.class, Long.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(LongConstant.forValue((Long) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Float} as a {@code float} and wraps it on load.
                 */
                FLOAT(float.class, Float.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(FloatConstant.forValue((Float) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Double} as a {@code double} and wraps it on load.
                 */
                DOUBLE(double.class, Double.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(DoubleConstant.forValue((Double) value));
                    }
                };

                /**
                 * The primitive type that can be stored on the constant pool.
                 */
                private final TypeDescription primitiveType;

                /**
                 * The wrapper type that is to be represented.
                 */
                private final TypeDescription wrapperType;

                /**
                 * Creates a new wrapper delegate for a primitive type.
                 *
                 * @param primitiveType The primitive type that can be stored on the constant pool.
                 * @param wrapperType   The wrapper type that is to be represented.
                 */
                ConstantPoolWrapper(Class<?> primitiveType, Class<?> wrapperType) {
                    this.primitiveType = TypeDescription.ForLoadedType.of(primitiveType);
                    this.wrapperType = TypeDescription.ForLoadedType.of(wrapperType);
                }

                /**
                 * Represents the given value by a constant pool value or as a field if this is not possible.
                 *
                 * @param value The value to provide to the bootstrapped method.
                 * @return An argument provider for this value.
                 */
                public static ArgumentProvider of(Object value) {
                    if (value instanceof Boolean) {
                        return BOOLEAN.make(value);
                    } else if (value instanceof Byte) {
                        return BYTE.make(value);
                    } else if (value instanceof Short) {
                        return SHORT.make(value);
                    } else if (value instanceof Character) {
                        return CHARACTER.make(value);
                    } else if (value instanceof Integer) {
                        return INTEGER.make(value);
                    } else if (value instanceof Long) {
                        return LONG.make(value);
                    } else if (value instanceof Float) {
                        return FLOAT.make(value);
                    } else if (value instanceof Double) {
                        return DOUBLE.make(value);
                    } else if (value instanceof String) {
                        return new ForStringConstant((String) value);
                    } else if (value instanceof Class<?>) {
                        return new ForClassConstant(TypeDescription.ForLoadedType.of((Class<?>) value));
                    } else if (value instanceof TypeDescription) {
                        return new ForClassConstant((TypeDescription) value);
                    } else if (value instanceof Enum<?>) {
                        return new ForEnumerationValue(new EnumerationDescription.ForLoadedEnumeration((Enum<?>) value));
                    } else if (value instanceof EnumerationDescription) {
                        return new ForEnumerationValue((EnumerationDescription) value);
                    } else if (JavaType.METHOD_HANDLE.isInstance(value)) {
                        return new ForJavaConstant(JavaConstant.MethodHandle.ofLoaded(value));
                    } else if (JavaType.METHOD_TYPE.isInstance(value)) {
                        return new ForJavaConstant(JavaConstant.MethodType.ofLoaded(value));
                    } else if (value instanceof JavaConstant) {
                        return new ForJavaConstant((JavaConstant) value);
                    } else {
                        return ForInstance.of(value);
                    }
                }

                /**
                 * Creates an argument provider for a given primitive value.
                 *
                 * @param value The wrapper-type value to provide to the bootstrapped method.
                 * @return An argument provider for this value.
                 */
                protected abstract ArgumentProvider make(Object value);

                /**
                 * An argument provider that loads a primitive value from the constant pool and wraps it.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class WrappingArgumentProvider implements ArgumentProvider {

                    /**
                     * The stack manipulation that represents the loading of the primitive value.
                     */
                    private final StackManipulation stackManipulation;

                    /**
                     * Creates a new wrapping argument provider.
                     *
                     * @param stackManipulation The stack manipulation that represents the loading of the
                     *                          primitive value.
                     */
                    protected WrappingArgumentProvider(StackManipulation stackManipulation) {
                        this.stackManipulation = stackManipulation;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                        return new Resolved.Simple(new StackManipulation.Compound(stackManipulation,
                                assigner.assign(primitiveType.asGenericType(), wrapperType.asGenericType(), typing)), wrapperType);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public InstrumentedType prepare(InstrumentedType instrumentedType) {
                        return instrumentedType;
                    }
                }
            }

            /**
             * A resolved {@link net.bytebuddy.implementation.InvokeDynamic.InvocationProvider.ArgumentProvider}.
             */
            interface Resolved {

                /**
                 * Returns a stack manipulation that loads the arguments onto the operand stack.
                 *
                 * @return A stack manipulation that loads the arguments onto the operand stack.
                 */
                StackManipulation getLoadInstruction();

                /**
                 * Returns a list of all types of the arguments that were loaded onto the operand stack.
                 *
                 * @return A list of all types of the arguments that were loaded onto the operand stack.
                 */
                List<TypeDescription> getLoadedTypes();

                /**
                 * A simple implementation of a resolved argument provider.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Simple implements Resolved {

                    /**
                     * A stack manipulation that loads the arguments onto the operand stack.
                     */
                    private final StackManipulation stackManipulation;

                    /**
                     * A list of all types of the arguments that were loaded onto the operand stack.
                     */
                    private final List<TypeDescription> loadedTypes;

                    /**
                     * Creates a simple resolved argument provider.
                     *
                     * @param stackManipulation A stack manipulation that loads the argument onto the operand stack.
                     * @param loadedType        The type of the arguments that is loaded onto the operand stack.
                     */
                    public Simple(StackManipulation stackManipulation, TypeDescription loadedType) {
                        this(stackManipulation, Collections.singletonList(loadedType));
                    }

                    /**
                     * Creates a simple resolved argument provider.
                     *
                     * @param stackManipulation A stack manipulation that loads the arguments onto the operand stack.
                     * @param loadedTypes       A list of all types of the arguments that were loaded onto the
                     *                          operand stack.
                     */
                    public Simple(StackManipulation stackManipulation, List<TypeDescription> loadedTypes) {
                        this.stackManipulation = stackManipulation;
                        this.loadedTypes = loadedTypes;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public StackManipulation getLoadInstruction() {
                        return stackManipulation;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public List<TypeDescription> getLoadedTypes() {
                        return loadedTypes;
                    }
                }
            }

            /**
             * An argument provider that loads the intercepted instance.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForThisInstance implements ArgumentProvider {

                /**
                 * The type as which the intercepted instance should be loaded onto the operand stack.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new argument provider for the instance of the instrumented type.
                 *
                 * @param typeDescription The type as which the instrumented type should be loaded onto the operand stack.
                 */
                protected ForThisInstance(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot get this instance from static method: " + instrumentedMethod);
                    } else if (!instrumentedType.isAssignableTo(typeDescription)) {
                        throw new IllegalStateException(instrumentedType + " is not assignable to " + instrumentedType);
                    }
                    return new Resolved.Simple(MethodVariableAccess.loadThis(), typeDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a value that is stored in a randomly named static field.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForInstance implements ArgumentProvider {

                /**
                 * The prefix of any field generated by this argument provider.
                 */
                private static final String FIELD_PREFIX = "invokeDynamic";

                /**
                 * The value that is stored in the static field.
                 */
                private final Object value;

                /**
                 * The type of the static field.
                 */
                private final TypeDescription fieldType;

                /**
                 * The name of the field.
                 */
                @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
                private final String name;

                /**
                 * Creates a new argument provider that stores the given value in a static field.
                 *
                 * @param value     The value that is to be provided to the bootstrapped method.
                 * @param fieldType The type of the field which is also provided to the bootstrap method.
                 */
                protected ForInstance(Object value, TypeDescription fieldType) {
                    this.value = value;
                    this.fieldType = fieldType;
                    name = FIELD_PREFIX + "$" + RandomString.make();
                }

                /**
                 * Creates a new argument provider that stores the given value in a static field of the instance type.
                 *
                 * @param value The value that is to be provided to the bootstrapped method.
                 * @return A corresponding argument provider.
                 */
                protected static ArgumentProvider of(Object value) {
                    return new ForInstance(value, TypeDescription.ForLoadedType.of(value.getClass()));
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    FieldDescription fieldDescription = instrumentedType.getDeclaredFields().filter(named(name)).getOnly();
                    StackManipulation stackManipulation = assigner.assign(fieldDescription.getType(), fieldType.asGenericType(), typing);
                    if (!stackManipulation.isValid()) {
                        throw new IllegalStateException("Cannot assign " + fieldDescription + " to " + fieldType);
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(FieldAccess.forField(fieldDescription).read(),
                            stackManipulation), fieldDescription.getType().asErasure());
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType
                            .withField(new FieldDescription.Token(name,
                                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE | Opcodes.ACC_SYNTHETIC,
                                    fieldType.asGenericType()))
                            .withInitializer(new LoadedTypeInitializer.ForStaticField(name, value));
                }
            }

            /**
             * Provides an argument from an existing field.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForField implements ArgumentProvider {

                /**
                 * The name of the field.
                 */
                protected final String fieldName;

                /**
                 * The field locator factory to use.
                 */
                protected final FieldLocator.Factory fieldLocatorFactory;

                /**
                 * Creates a new argument provider that loads the value of an existing field.
                 *
                 * @param fieldName           The name of the field.
                 * @param fieldLocatorFactory The field locator factory to use.
                 */
                protected ForField(String fieldName, FieldLocator.Factory fieldLocatorFactory) {
                    this.fieldName = fieldName;
                    this.fieldLocatorFactory = fieldLocatorFactory;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    FieldLocator.Resolution resolution = fieldLocatorFactory.make(instrumentedType).locate(fieldName);
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Cannot find a field " + fieldName + " for " + instrumentedType);
                    } else if (!resolution.getField().isStatic() && instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot access non-static " + resolution.getField() + " from " + instrumentedMethod);
                    }
                    return doResolve(new StackManipulation.Compound(resolution.getField().isStatic()
                                    ? StackManipulation.Trivial.INSTANCE
                                    : MethodVariableAccess.loadThis(), FieldAccess.forField(resolution.getField()).read()),
                            resolution.getField().getType(),
                            assigner,
                            typing);
                }

                /**
                 * Resolves this argument provider.
                 *
                 * @param access   The stack manipulation for accessing the argument value.
                 * @param type     The type of the loaded value.
                 * @param assigner The assigner to use.
                 * @param typing   The typing required.
                 * @return A resolved version of this arguments provider.
                 */
                protected Resolved doResolve(StackManipulation access, TypeDescription.Generic type, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(access, type.asErasure());
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                /**
                 * An argument provider for a field value with an explicit type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class WithExplicitType extends ForField {

                    /**
                     * The explicit type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates an argument provider for a field value with an explicit type.
                     *
                     * @param fieldName           The name of the field.
                     * @param fieldLocatorFactory The field locator factory to use.
                     * @param typeDescription     The explicit type.
                     */
                    protected WithExplicitType(String fieldName, FieldLocator.Factory fieldLocatorFactory, TypeDescription typeDescription) {
                        super(fieldName, fieldLocatorFactory);
                        this.typeDescription = typeDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    protected Resolved doResolve(StackManipulation access, TypeDescription.Generic typeDescription, Assigner assigner, Assigner.Typing typing) {
                        StackManipulation stackManipulation = assigner.assign(typeDescription, this.typeDescription.asGenericType(), typing);
                        if (!stackManipulation.isValid()) {
                            throw new IllegalStateException("Cannot assign " + typeDescription + " to " + this.typeDescription);
                        }
                        return new Resolved.Simple(new StackManipulation.Compound(access, stackManipulation), this.typeDescription);
                    }
                }
            }

            /**
             * An argument provider that loads an argument of the intercepted method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForMethodParameter implements ArgumentProvider {

                /**
                 * The index of the parameter.
                 */
                protected final int index;

                /**
                 * Creates an argument provider for an argument of the intercepted method.
                 *
                 * @param index The index of the parameter.
                 */
                protected ForMethodParameter(int index) {
                    this.index = index;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    ParameterList<?> parameters = instrumentedMethod.getParameters();
                    if (index >= parameters.size()) {
                        throw new IllegalStateException("No parameter " + index + " for " + instrumentedMethod);
                    }
                    return doResolve(MethodVariableAccess.load(parameters.get(index)), parameters.get(index).getType(), assigner, typing);
                }

                /**
                 * Resolves this argument provider.
                 *
                 * @param access   The stack manipulation for accessing the argument value.
                 * @param type     The type of the loaded value.
                 * @param assigner The assigner to use.
                 * @param typing   The typing required.
                 * @return A resolved version of this arguments provider.
                 */
                protected Resolved doResolve(StackManipulation access, TypeDescription.Generic type, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(access, type.asErasure());
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                /**
                 * An argument provider for a method parameter with an explicit type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class WithExplicitType extends ForMethodParameter {

                    /**
                     * The explicit type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new argument provider for a method parameter with an explicit type.
                     *
                     * @param index           The index of the parameter.
                     * @param typeDescription The explicit type.
                     */
                    protected WithExplicitType(int index, TypeDescription typeDescription) {
                        super(index);
                        this.typeDescription = typeDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    protected Resolved doResolve(StackManipulation access, TypeDescription.Generic type, Assigner assigner, Assigner.Typing typing) {
                        StackManipulation stackManipulation = assigner.assign(type, typeDescription.asGenericType(), typing);
                        if (!stackManipulation.isValid()) {
                            throw new IllegalStateException("Cannot assign " + type + " to " + typeDescription);
                        }
                        return new Resolved.Simple(new StackManipulation.Compound(access, stackManipulation), typeDescription);
                    }
                }
            }

            /**
             * An argument provider for a {@code boolean} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForBooleanConstant implements ArgumentProvider {

                /**
                 * The represented {@code boolean} value.
                 */
                private final boolean value;

                /**
                 * Creates a new argument provider for a {@code boolean} value.
                 *
                 * @param value The represented {@code boolean} value.
                 */
                protected ForBooleanConstant(boolean value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), TypeDescription.ForLoadedType.of(boolean.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@code byte} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForByteConstant implements ArgumentProvider {

                /**
                 * The represented {@code byte} value.
                 */
                private final byte value;

                /**
                 * Creates a new argument provider for a {@code byte} value.
                 *
                 * @param value The represented {@code byte} value.
                 */
                protected ForByteConstant(byte value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), TypeDescription.ForLoadedType.of(byte.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@code short} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForShortConstant implements ArgumentProvider {

                /**
                 * The represented {@code short} value.
                 */
                private final short value;

                /**
                 * Creates a new argument provider for a {@code short} value.
                 *
                 * @param value The represented {@code short} value.
                 */
                protected ForShortConstant(short value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), TypeDescription.ForLoadedType.of(short.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@code char} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForCharacterConstant implements ArgumentProvider {

                /**
                 * The represented {@code char} value.
                 */
                private final char value;

                /**
                 * Creates a new argument provider for a {@code char} value.
                 *
                 * @param value The represented {@code char} value.
                 */
                protected ForCharacterConstant(char value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), TypeDescription.ForLoadedType.of(char.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@code int} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForIntegerConstant implements ArgumentProvider {

                /**
                 * The represented {@code int} value.
                 */
                private final int value;

                /**
                 * Creates a new argument provider for a {@code int} value.
                 *
                 * @param value The represented {@code int} value.
                 */
                protected ForIntegerConstant(int value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), TypeDescription.ForLoadedType.of(int.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@code long} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForLongConstant implements ArgumentProvider {

                /**
                 * The represented {@code long} value.
                 */
                private final long value;

                /**
                 * Creates a new argument provider for a {@code long} value.
                 *
                 * @param value The represented {@code long} value.
                 */
                protected ForLongConstant(long value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(LongConstant.forValue(value), TypeDescription.ForLoadedType.of(long.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@code float} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForFloatConstant implements ArgumentProvider {

                /**
                 * The represented {@code float} value.
                 */
                private final float value;

                /**
                 * Creates a new argument provider for a {@code float} value.
                 *
                 * @param value The represented {@code float} value.
                 */
                protected ForFloatConstant(float value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(FloatConstant.forValue(value), TypeDescription.ForLoadedType.of(float.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@code double} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForDoubleConstant implements ArgumentProvider {

                /**
                 * The represented {@code double} value.
                 */
                private final double value;

                /**
                 * Creates a new argument provider for a {@code double} value.
                 *
                 * @param value The represented {@code double} value.
                 */
                protected ForDoubleConstant(double value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(DoubleConstant.forValue(value), TypeDescription.ForLoadedType.of(double.class));
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@link java.lang.String} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForStringConstant implements ArgumentProvider {

                /**
                 * The represented {@link java.lang.String} value.
                 */
                private final String value;

                /**
                 * Creates a new argument provider for a {@link java.lang.String} value.
                 *
                 * @param value The represented {@link java.lang.String} value.
                 */
                protected ForStringConstant(String value) {
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(new TextConstant(value), TypeDescription.STRING);
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a {@link java.lang.Class} constant.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForClassConstant implements ArgumentProvider {

                /**
                 * The type that is represented by this constant.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new argument provider for the given type description.
                 *
                 * @param typeDescription The type to represent.
                 */
                protected ForClassConstant(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(ClassConstant.of(typeDescription), TypeDescription.CLASS);
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for an {@link java.lang.Enum} constant.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForEnumerationValue implements ArgumentProvider {

                /**
                 * A description of the enumeration to represent.
                 */
                private final EnumerationDescription enumerationDescription;

                /**
                 * Creates a new argument provider for an enumeration value.
                 *
                 * @param enumerationDescription A description of the enumeration to represent.
                 */
                protected ForEnumerationValue(EnumerationDescription enumerationDescription) {
                    this.enumerationDescription = enumerationDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(FieldAccess.forEnumeration(enumerationDescription), enumerationDescription.getEnumerationType());
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for the {@code null} value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForNullValue implements ArgumentProvider {

                /**
                 * The type to be represented by the {@code null} value.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new argument provider for the {@code null} value.
                 *
                 * @param typeDescription The type to be represented by the {@code null} value.
                 */
                protected ForNullValue(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(NullConstant.INSTANCE, typeDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            /**
             * An argument provider for a Java instance.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJavaConstant implements ArgumentProvider {

                /**
                 * The Java instance to provide to the bootstrapped method.
                 */
                private final JavaConstant javaConstant;

                /**
                 * Creates a new argument provider for the given Java instance.
                 *
                 * @param javaConstant The Java instance to provide to the bootstrapped method.
                 */
                protected ForJavaConstant(JavaConstant javaConstant) {
                    this.javaConstant = javaConstant;
                }

                /**
                 * {@inheritDoc}
                 */
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(new JavaConstantValue(javaConstant), javaConstant.getType());
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }
        }

        /**
         * Provides the name of the method that is to be bound by a dynamic method call.
         */
        interface NameProvider {

            /**
             * Resolves the name given the intercepted method.
             *
             * @param methodDescription The intercepted method.
             * @return The name of the method to be bound by the bootstrap method.
             */
            String resolve(MethodDescription methodDescription);

            /**
             * A name provider that provides the name of the intercepted method.
             */
            enum ForInterceptedMethod implements NameProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public String resolve(MethodDescription methodDescription) {
                    return methodDescription.getInternalName();
                }
            }

            /**
             * A name provider that provides an explicit name.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForExplicitName implements NameProvider {

                /**
                 * The name to be provided.
                 */
                private final String internalName;

                /**
                 * Creates a new name provider for an explicit name.
                 *
                 * @param internalName The name to be provided.
                 */
                protected ForExplicitName(String internalName) {
                    this.internalName = internalName;
                }

                /**
                 * {@inheritDoc}
                 */
                public String resolve(MethodDescription methodDescription) {
                    return internalName;
                }
            }
        }

        /**
         * Provides the return type that is requested from the bootstrap method.
         */
        interface ReturnTypeProvider {

            /**
             * Resolves the return type that is requested from the bootstrap method.
             *
             * @param methodDescription The intercepted method.
             * @return The return type that is requested from the bootstrap method.
             */
            TypeDescription resolve(MethodDescription methodDescription);

            /**
             * Requests the return type of the intercepted method.
             */
            enum ForInterceptedMethod implements ReturnTypeProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription resolve(MethodDescription methodDescription) {
                    return methodDescription.getReturnType().asErasure();
                }
            }

            /**
             * Requests an explicit return type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForExplicitType implements ReturnTypeProvider {

                /**
                 * The requested return type.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new return type provider for an explicit return type.
                 *
                 * @param typeDescription The requested return type.
                 */
                protected ForExplicitType(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription resolve(MethodDescription methodDescription) {
                    return typeDescription;
                }
            }
        }

        /**
         * An invocation provider that requests a synthetic dynamic invocation where all arguments are explicitly
         * provided by the user.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Default implements InvocationProvider {

            /**
             * The provider for the name of the intercepted method.
             */
            private final NameProvider nameProvider;

            /**
             * The provider for the required return type.
             */
            private final ReturnTypeProvider returnTypeProvider;

            /**
             * The providers for the method arguments in their order.
             */
            private final List<ArgumentProvider> argumentProviders;

            /**
             * Creates a new default invocation provider that provides information and arguments of the
             * intercepted method.
             */
            protected Default() {
                this(NameProvider.ForInterceptedMethod.INSTANCE,
                        ReturnTypeProvider.ForInterceptedMethod.INSTANCE,
                        Collections.<ArgumentProvider>singletonList(ArgumentProvider.ForInterceptedMethodInstanceAndParameters.INSTANCE));
            }

            /**
             * Creates a new default invocation provider.
             *
             * @param nameProvider       The provider for the name of the intercepted method.
             * @param returnTypeProvider The provider for the required return type.
             * @param argumentProviders  The providers for the method arguments in their order.
             */
            protected Default(NameProvider nameProvider,
                              ReturnTypeProvider returnTypeProvider,
                              List<ArgumentProvider> argumentProviders) {
                this.nameProvider = nameProvider;
                this.returnTypeProvider = returnTypeProvider;
                this.argumentProviders = argumentProviders;
            }

            /**
             * {@inheritDoc}
             */
            public Target make(MethodDescription methodDescription) {
                return new Target(nameProvider.resolve(methodDescription),
                        returnTypeProvider.resolve(methodDescription),
                        argumentProviders,
                        methodDescription);
            }

            /**
             * {@inheritDoc}
             */
            public InvocationProvider appendArguments(List<ArgumentProvider> argumentProviders) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        CompoundList.of(this.argumentProviders, argumentProviders));
            }

            /**
             * {@inheritDoc}
             */
            public InvocationProvider appendArgument(ArgumentProvider argumentProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        CompoundList.of(this.argumentProviders, argumentProvider));
            }

            /**
             * {@inheritDoc}
             */
            public InvocationProvider withoutArguments() {
                return new Default(nameProvider,
                        returnTypeProvider,
                        Collections.<ArgumentProvider>emptyList());
            }

            /**
             * {@inheritDoc}
             */
            public InvocationProvider withNameProvider(NameProvider nameProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        argumentProviders);
            }

            /**
             * {@inheritDoc}
             */
            public InvocationProvider withReturnTypeProvider(ReturnTypeProvider returnTypeProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        argumentProviders);
            }

            /**
             * {@inheritDoc}
             */
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                for (ArgumentProvider argumentProvider : argumentProviders) {
                    instrumentedType = argumentProvider.prepare(instrumentedType);
                }
                return instrumentedType;
            }

            /**
             * A target for a synthetically bound method call.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Target implements InvocationProvider.Target {

                /**
                 * The name to be passed to the bootstrap method.
                 */
                private final String internalName;

                /**
                 * The return type to be requested from the bootstrapping method.
                 */
                private final TypeDescription returnType;

                /**
                 * The arguments to be passed to the bootstrap method.
                 */
                private final List<ArgumentProvider> argumentProviders;

                /**
                 * The intercepted method.
                 */
                private final MethodDescription instrumentedMethod;

                /**
                 * Creates a new target.
                 *
                 * @param internalName       The name to be passed to the bootstrap method.
                 * @param returnType         The return type to be requested from the bootstrapping method.
                 * @param argumentProviders  The arguments to be passed to the bootstrap method.
                 * @param instrumentedMethod The intercepted method.
                 */
                protected Target(String internalName,
                                 TypeDescription returnType,
                                 List<ArgumentProvider> argumentProviders,
                                 MethodDescription instrumentedMethod) {
                    this.internalName = internalName;
                    this.returnType = returnType;
                    this.argumentProviders = argumentProviders;
                    this.instrumentedMethod = instrumentedMethod;
                }

                /**
                 * {@inheritDoc}
                 */
                public InvocationProvider.Target.Resolved resolve(TypeDescription instrumentedType, Assigner assigner, Assigner.Typing typing) {
                    StackManipulation[] stackManipulation = new StackManipulation[argumentProviders.size()];
                    List<TypeDescription> parameterTypes = new ArrayList<TypeDescription>();
                    int index = 0;
                    for (ArgumentProvider argumentProvider : argumentProviders) {
                        ArgumentProvider.Resolved resolved = argumentProvider.resolve(instrumentedType, instrumentedMethod, assigner, typing);
                        parameterTypes.addAll(resolved.getLoadedTypes());
                        stackManipulation[index++] = resolved.getLoadInstruction();
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(stackManipulation),
                            internalName,
                            returnType,
                            parameterTypes);
                }
            }
        }
    }

    /**
     * A termination handler is responsible to handle the return value of a method that is invoked via a
     * {@link net.bytebuddy.implementation.InvokeDynamic}.
     */
    protected enum TerminationHandler {

        /**
         * A termination handler that returns the bound method's return value.
         */
        RETURNING {
            @Override
            protected StackManipulation resolve(MethodDescription interceptedMethod, TypeDescription returnType, Assigner assigner, Assigner.Typing typing) {
                StackManipulation stackManipulation = assigner.assign(returnType.asGenericType(), interceptedMethod.getReturnType(), typing);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot return " + returnType + " from " + interceptedMethod);
                }
                return new StackManipulation.Compound(stackManipulation, MethodReturn.of(interceptedMethod.getReturnType()));
            }
        },

        /**
         * A termination handler that drops the bound method's return value.
         */
        DROPPING {
            @Override
            protected StackManipulation resolve(MethodDescription interceptedMethod, TypeDescription returnType, Assigner assigner, Assigner.Typing typing) {
                return Removal.of(interceptedMethod.isConstructor()
                        ? interceptedMethod.getDeclaringType()
                        : interceptedMethod.getReturnType());
            }
        };

        /**
         * Returns a stack manipulation that handles the method return.
         *
         * @param interceptedMethod The method being intercepted.
         * @param returnType        The return type of the instrumented method.
         * @param assigner          The assigner to use.
         * @param typing            Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return A stack manipulation that handles the method return.
         */
        protected abstract StackManipulation resolve(MethodDescription interceptedMethod,
                                                     TypeDescription returnType,
                                                     Assigner assigner,
                                                     Assigner.Typing typing);
    }

    /**
     * An abstract delegator that allows to specify a configuration for any specification of an argument.
     */
    protected abstract static class AbstractDelegator extends InvokeDynamic {

        /**
         * Creates a new abstract delegator for a dynamic method invocation.
         *
         * @param bootstrap          The bootstrap method or constructor.
         * @param arguments          The arguments that are provided to the bootstrap method or constructor.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected AbstractDelegator(MethodDescription.InDefinedShape bootstrap,
                                    List<?> arguments,
                                    InvocationProvider invocationProvider,
                                    TerminationHandler terminationHandler,
                                    Assigner assigner,
                                    Assigner.Typing typing) {
            super(bootstrap, arguments, invocationProvider, terminationHandler, assigner, typing);
        }

        /**
         * Resolves the current configuration into a fully initialized invoke dynamic instance.
         *
         * @return The fully resolved invoke dynamic instance.
         */
        protected abstract InvokeDynamic materialize();

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withBooleanValue(boolean... value) {
            return materialize().withBooleanValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withByteValue(byte... value) {
            return materialize().withByteValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withShortValue(short... value) {
            return materialize().withShortValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withCharacterValue(char... value) {
            return materialize().withCharacterValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withIntegerValue(int... value) {
            return materialize().withIntegerValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withLongValue(long... value) {
            return materialize().withLongValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withFloatValue(float... value) {
            return materialize().withFloatValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withDoubleValue(double... value) {
            return materialize().withDoubleValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withValue(Object... value) {
            return materialize().withValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public WithImplicitType withReference(Object value) {
            return materialize().withReference(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withReference(Object... value) {
            return materialize().withReference(value);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withType(TypeDescription... typeDescription) {
            return materialize().withType(typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withInstance(JavaConstant... javaConstant) {
            return materialize().withInstance(javaConstant);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withNullValue(Class<?>... type) {
            return materialize().withNullValue(type);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withNullValue(TypeDescription... typeDescription) {
            return materialize().withNullValue(typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withArgument(int... index) {
            return materialize().withArgument(index);
        }

        /**
         * {@inheritDoc}
         */
        public WithImplicitType withArgument(int index) {
            return materialize().withArgument(index);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withThis(Class<?>... type) {
            return materialize().withThis(type);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withThis(TypeDescription... typeDescription) {
            return materialize().withThis(typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withMethodArguments() {
            return materialize().withMethodArguments();
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withImplicitAndMethodArguments() {
            return materialize().withImplicitAndMethodArguments();
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withField(String... fieldName) {
            return materialize().withField(fieldName);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withEnumeration(EnumerationDescription... enumerationDescription) {
            return materialize().withEnumeration(enumerationDescription);
        }

        /**
         * {@inheritDoc}
         */
        public InvokeDynamic withField(FieldLocator.Factory fieldLocatorFactory, String... name) {
            return materialize().withField(fieldLocatorFactory, name);
        }

        /**
         * {@inheritDoc}
         */
        public WithImplicitType withField(String name) {
            return materialize().withField(name);
        }

        /**
         * {@inheritDoc}
         */
        public WithImplicitType withField(String name, FieldLocator.Factory fieldLocatorFactory) {
            return materialize().withField(name, fieldLocatorFactory);
        }

        /**
         * {@inheritDoc}
         */
        public Composable withAssigner(Assigner assigner, Assigner.Typing typing) {
            return materialize().withAssigner(assigner, typing);
        }

        /**
         * {@inheritDoc}
         */
        public Implementation andThen(Implementation implementation) {
            return materialize().andThen(implementation);
        }

        /**
         * {@inheritDoc}
         */
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return materialize().prepare(instrumentedType);
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return materialize().appender(implementationTarget);
        }
    }

    /**
     * Representation of an {@link net.bytebuddy.implementation.InvokeDynamic} implementation where the bootstrapped
     * method is passed a {@code this} reference, if available, and any arguments of the instrumented method.
     */
    public static class WithImplicitArguments extends AbstractDelegator {

        /**
         * Creates a new dynamic method invocation with implicit arguments.
         *
         * @param bootstrap          The bootstrap method or constructor.
         * @param arguments          The arguments that are provided to the bootstrap method or constructor.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected WithImplicitArguments(MethodDescription.InDefinedShape bootstrap,
                                        List<?> arguments,
                                        InvocationProvider invocationProvider,
                                        TerminationHandler terminationHandler,
                                        Assigner assigner,
                                        Assigner.Typing typing) {
            super(bootstrap,
                    arguments,
                    invocationProvider,
                    terminationHandler,
                    assigner,
                    typing);
        }

        /**
         * Returns an instance of this instrumentation where the bootstrapped method is not passed any arguments.
         *
         * @return This implementation where the bootstrapped method is not passed any arguments.
         */
        public InvokeDynamic withoutArguments() {
            return new InvokeDynamic(bootstrap,
                    arguments,
                    invocationProvider.withoutArguments(),
                    terminationHandler,
                    assigner,
                    typing);
        }

        @Override
        protected InvokeDynamic materialize() {
            return withoutArguments();
        }

        /**
         * {@inheritDoc}
         */
        public WithImplicitArguments withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new WithImplicitArguments(bootstrap,
                    arguments,
                    invocationProvider,
                    terminationHandler,
                    assigner,
                    typing);
        }
    }

    /**
     * Representation of an {@link net.bytebuddy.implementation.InvokeDynamic} implementation where the bootstrapped
     * method is passed a {@code this} reference, if available, and any arguments of the instrumented method and
     * where the invocation target is implicit.
     */
    public static class WithImplicitTarget extends WithImplicitArguments {

        /**
         * Creates a new dynamic method invocation with implicit arguments and an implicit invocation target.
         *
         * @param bootstrap          The bootstrap method or constructor.
         * @param arguments          The arguments that are provided to the bootstrap method.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected WithImplicitTarget(MethodDescription.InDefinedShape bootstrap,
                                     List<?> arguments,
                                     InvocationProvider invocationProvider,
                                     TerminationHandler terminationHandler,
                                     Assigner assigner,
                                     Assigner.Typing typing) {
            super(bootstrap,
                    arguments,
                    invocationProvider,
                    terminationHandler,
                    assigner,
                    typing);
        }

        /**
         * Requests the bootstrap method to bind a method with the given return type. The return type
         * is he assigned to the intercepted method's return type.
         *
         * @param returnType The return type to request from the bootstrapping method.
         * @return This implementation where the bootstrap method is requested to bind a method with the given
         * return type.
         */
        public InvokeDynamic.WithImplicitArguments invoke(Class<?> returnType) {
            return invoke(TypeDescription.ForLoadedType.of(returnType));
        }

        /**
         * Requests the bootstrap method to bind a method with the given return type. The return type
         * is he assigned to the intercepted method's return type.
         *
         * @param returnType The return type to request from the bootstrapping method.
         * @return This implementation where the bootstrap method is requested to bind a method with the given
         * return type.
         */
        public InvokeDynamic.WithImplicitArguments invoke(TypeDescription returnType) {
            return new WithImplicitArguments(bootstrap,
                    arguments,
                    invocationProvider.withReturnTypeProvider(new InvocationProvider.ReturnTypeProvider.ForExplicitType(returnType)),
                    terminationHandler,
                    assigner,
                    typing);
        }

        /**
         * Requests the bootstrap method is passed the given method name.
         *
         * @param methodName The method name to pass to the bootstrapping method.
         * @return This implementation where the bootstrap method is passed the given method name.
         */
        public InvokeDynamic.WithImplicitArguments invoke(String methodName) {
            return new WithImplicitArguments(bootstrap,
                    arguments,
                    invocationProvider.withNameProvider(new InvocationProvider.NameProvider.ForExplicitName(methodName)),
                    terminationHandler,
                    assigner,
                    typing);
        }

        /**
         * Requests the bootstrap method to bind a method with the given return type. The return type
         * is he assigned to the intercepted method's return type. Also, the bootstrap method is passed the
         * given method name,
         *
         * @param methodName The method name to pass to the bootstrapping method.
         * @param returnType The return type to request from the bootstrapping method.
         * @return This implementation where the bootstrap method is requested to bind a method with the given
         * return type while being passed the given method name.
         */
        public InvokeDynamic.WithImplicitArguments invoke(String methodName, Class<?> returnType) {
            return invoke(methodName, TypeDescription.ForLoadedType.of(returnType));
        }

        /**
         * Requests the bootstrap method to bind a method with the given return type. The return type
         * is he assigned to the intercepted method's return type. Also, the bootstrap method is passed the
         * given method name,
         *
         * @param methodName The method name to pass to the bootstrapping method.
         * @param returnType The return type to request from the bootstrapping method.
         * @return This implementation where the bootstrap method is requested to bind a method with the given
         * return type while being passed the given method name.
         */
        public InvokeDynamic.WithImplicitArguments invoke(String methodName, TypeDescription returnType) {
            return new WithImplicitArguments(bootstrap,
                    arguments,
                    invocationProvider
                            .withNameProvider(new InvocationProvider.NameProvider.ForExplicitName(methodName))
                            .withReturnTypeProvider(new InvocationProvider.ReturnTypeProvider.ForExplicitType(returnType)),
                    terminationHandler,
                    assigner,
                    typing);
        }
    }

    /**
     * An {@link InvokeDynamic} invocation where the last argument is assigned its implicit type.
     */
    public abstract static class WithImplicitType extends AbstractDelegator {

        /**
         * Creates a new abstract delegator for a dynamic method invocation where the last argument is assigned an implicit type.
         *
         * @param bootstrap          The bootstrap method or constructor.
         * @param arguments          The arguments that are provided to the bootstrap method or constructor.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected WithImplicitType(MethodDescription.InDefinedShape bootstrap,
                                   List<?> arguments,
                                   InvocationProvider invocationProvider,
                                   TerminationHandler terminationHandler,
                                   Assigner assigner,
                                   Assigner.Typing typing) {
            super(bootstrap, arguments, invocationProvider, terminationHandler, assigner, typing);
        }

        /**
         * Represents the last value as an instance of the given type.
         *
         * @param type The type to represent to the dynamic method invocation.
         * @return A new dynamic method invocation where the last argument is represented by the given type.
         */
        public InvokeDynamic as(Class<?> type) {
            return as(TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Represents the last value as an instance of the given type.
         *
         * @param typeDescription The type to represent to the dynamic method invocation.
         * @return A new dynamic method invocation where the last argument is represented by the given type.
         */
        public abstract InvokeDynamic as(TypeDescription typeDescription);

        /**
         * A step in the invoke dynamic domain specific language that allows to explicitly specify a field type for a reference value.
         */
        @SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "Super type implementation covers use case")
        protected static class OfInstance extends WithImplicitType {

            /**
             * The value that is supplied as the next argument to the bootstrapped method.
             */
            private final Object value;

            /**
             * An argument provider that represents the argument with an implicit type.
             */
            private final InvocationProvider.ArgumentProvider argumentProvider;

            /**
             * Creates a new invoke dynamic instance with an implicit field type for the provided value.
             *
             * @param bootstrap          The bootstrap method or constructor.
             * @param arguments          The arguments that are provided to the bootstrap method or constructor.
             * @param invocationProvider The target provided that identifies the method to be bootstrapped.
             * @param terminationHandler A handler that handles the method return.
             * @param assigner           The assigner to be used.
             * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @param value              The value that is supplied as the next argument to the bootstrapped method.
             */
            protected OfInstance(MethodDescription.InDefinedShape bootstrap,
                                 List<?> arguments,
                                 InvocationProvider invocationProvider,
                                 TerminationHandler terminationHandler,
                                 Assigner assigner,
                                 Assigner.Typing typing,
                                 Object value) {
                super(bootstrap, arguments, invocationProvider, terminationHandler, assigner, typing);
                this.value = value;
                this.argumentProvider = InvocationProvider.ArgumentProvider.ForInstance.of(value);
            }

            @Override
            public InvokeDynamic as(TypeDescription typeDescription) {
                if (!typeDescription.asBoxed().isInstance(value)) {
                    throw new IllegalArgumentException(value + " is not of type " + typeDescription);
                }
                return new InvokeDynamic(bootstrap,
                        arguments,
                        invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForInstance(value, typeDescription)),
                        terminationHandler,
                        assigner,
                        typing);
            }

            @Override
            protected InvokeDynamic materialize() {
                return new InvokeDynamic(bootstrap,
                        arguments,
                        invocationProvider.appendArgument(argumentProvider),
                        terminationHandler,
                        assigner,
                        typing);
            }
        }

        /**
         * An invoke dynamic implementation where the last argument is an implicitly typed method argument.
         */
        @SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "Super type implementation covers use case")
        protected static class OfArgument extends WithImplicitType {

            /**
             * The index of the method argument.
             */
            private final int index;

            /**
             * Creates a new invoke dynamic instance with an implicit field type for the provided value.
             *
             * @param bootstrap          The bootstrap method or constructor.
             * @param arguments          The arguments that are provided to the bootstrap method or constructor.
             * @param invocationProvider The target provided that identifies the method to be bootstrapped.
             * @param terminationHandler A handler that handles the method return.
             * @param assigner           The assigner to be used.
             * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @param index              The index of of the argument to supply to the bootstrapped method.
             */
            protected OfArgument(MethodDescription.InDefinedShape bootstrap,
                                 List<?> arguments,
                                 InvocationProvider invocationProvider,
                                 TerminationHandler terminationHandler,
                                 Assigner assigner,
                                 Assigner.Typing typing,
                                 int index) {
                super(bootstrap, arguments, invocationProvider, terminationHandler, assigner, typing);
                this.index = index;
            }

            @Override
            public InvokeDynamic as(TypeDescription typeDescription) {
                return new InvokeDynamic(bootstrap,
                        arguments,
                        invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForMethodParameter.WithExplicitType(index, typeDescription)),
                        terminationHandler,
                        assigner,
                        typing);
            }

            @Override
            protected InvokeDynamic materialize() {
                return new InvokeDynamic(bootstrap,
                        arguments,
                        invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForMethodParameter(index)),
                        terminationHandler,
                        assigner,
                        typing);
            }
        }

        /**
         * An invoke dynamic implementation where the last argument is an implicitly typed field value.
         */
        @SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "Super type implementation covers use case")
        protected static class OfField extends WithImplicitType {

            /**
             * The field name.
             */
            private final String fieldName;

            /**
             * The field locator factory to use.
             */
            private final FieldLocator.Factory fieldLocatorFactory;

            /**
             * Creates a new abstract delegator for a dynamic method invocation where the last argument is assigned an implicit type.
             *
             * @param bootstrap           The bootstrap method or constructor.
             * @param arguments           The arguments that are provided to the bootstrap method.
             * @param invocationProvider  The target provided that identifies the method to be bootstrapped.
             * @param terminationHandler  A handler that handles the method return.
             * @param assigner            The assigner to be used.
             * @param typing              Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @param fieldName           The field name.
             * @param fieldLocatorFactory The field locator factory to use.
             */
            protected OfField(MethodDescription.InDefinedShape bootstrap,
                              List<?> arguments,
                              InvocationProvider invocationProvider,
                              TerminationHandler terminationHandler,
                              Assigner assigner,
                              Assigner.Typing typing,
                              String fieldName,
                              FieldLocator.Factory fieldLocatorFactory) {
                super(bootstrap, arguments, invocationProvider, terminationHandler, assigner, typing);
                this.fieldName = fieldName;
                this.fieldLocatorFactory = fieldLocatorFactory;
            }

            @Override
            public InvokeDynamic as(TypeDescription typeDescription) {
                return new InvokeDynamic(bootstrap,
                        arguments,
                        invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForField.WithExplicitType(fieldName, fieldLocatorFactory, typeDescription)),
                        terminationHandler,
                        assigner,
                        typing);
            }

            @Override
            protected InvokeDynamic materialize() {
                return new InvokeDynamic(bootstrap,
                        arguments,
                        invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForField(fieldName, fieldLocatorFactory)),
                        terminationHandler,
                        assigner,
                        typing);
            }
        }
    }

    /**
     * The byte code appender to be used by the {@link net.bytebuddy.implementation.InvokeDynamic} implementation.
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
    protected class Appender implements ByteCodeAppender {

        /**
         * The instrumented type of the current implementation.
         */
        private final TypeDescription instrumentedType;

        /**
         * Creates a new byte code appender for an invoke dynamic implementation.
         *
         * @param instrumentedType The instrumented type of the current implementation.
         */
        public Appender(TypeDescription instrumentedType) {
            this.instrumentedType = instrumentedType;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            InvocationProvider.Target.Resolved target = invocationProvider.make(instrumentedMethod).resolve(instrumentedType, assigner, typing);
            StackManipulation.Size size = new StackManipulation.Compound(
                    target.getStackManipulation(),
                    MethodInvocation.invoke(bootstrap).dynamic(target.getInternalName(),
                            target.getReturnType(),
                            target.getParameterTypes(),
                            arguments),
                    terminationHandler.resolve(instrumentedMethod, target.getReturnType(), assigner, typing)
            ).apply(methodVisitor, implementationContext);
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }
}

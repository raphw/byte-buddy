package net.bytebuddy.implementation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
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

import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * An implementation that applies a
 * <a href="http://docs.oracle.com/javase/8/docs/api/java/lang/invoke/package-summary.html">dynamic method invocation</a>.
 */
public class InvokeDynamic implements Implementation.Composable {

    /**
     * The bootstrap method.
     */
    protected final MethodDescription.InDefinedShape bootstrapMethod;

    /**
     * The arguments that are provided to the bootstrap method.
     */
    protected final List<?> handleArguments;

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
     * @param bootstrapMethod    The bootstrap method.
     * @param handleArguments    The arguments that are provided to the bootstrap method.
     * @param invocationProvider The target provided that identifies the method to be bootstrapped.
     * @param terminationHandler A handler that handles the method return.
     * @param assigner           The assigner to be used.
     * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected InvokeDynamic(MethodDescription.InDefinedShape bootstrapMethod,
                            List<?> handleArguments,
                            InvocationProvider invocationProvider,
                            TerminationHandler terminationHandler,
                            Assigner assigner,
                            Assigner.Typing typing) {
        this.bootstrapMethod = bootstrapMethod;
        this.handleArguments = handleArguments;
        this.invocationProvider = invocationProvider;
        this.terminationHandler = terminationHandler;
        this.assigner = assigner;
        this.typing = typing;
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method.
     *
     * @param method      The bootstrap method that is used to link the instrumented method.
     * @param rawArgument The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                    constant pool, i.e. primitive types (represented as their wrapper types) with a size of
     *                    at least 32 bit, {@link java.lang.String} types, {@link java.lang.Class} types as well
     *                    as {@code MethodType} and {@code MethodHandle} instances. In order to avoid class loading,
     *                    it is also possible to supply unloaded types as {@link TypeDescription},
     *                    {@link JavaConstant.MethodHandle} or
     *                    {@link JavaConstant.MethodType} instances.
     *                    instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Method method, Object... rawArgument) {
        return bootstrap(new MethodDescription.ForLoadedMethod(method), rawArgument);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method.
     *
     * @param method       The bootstrap method that is used to link the instrumented method.
     * @param rawArguments The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                     constant pool, i.e. primitive types (represented as their wrapper types) with a size of
     *                     at least 32 bit, {@link java.lang.String} types, {@link java.lang.Class} types as well
     *                     as {@code MethodType} and {@code MethodHandle} instances. In order to avoid class loading,
     *                     it is also possible to supply unloaded types as {@link TypeDescription},
     *                     {@link JavaConstant.MethodHandle} or
     *                     {@link JavaConstant.MethodType} instances.
     *                     instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Method method, List<?> rawArguments) {
        return bootstrap(new MethodDescription.ForLoadedMethod(method), rawArguments);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap constructor.
     *
     * @param constructor The bootstrap constructor that is used to link the instrumented method.
     * @param rawArgument The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                    constant pool, i.e. primitive types (represented as their wrapper types) with a size of
     *                    at least 32 bit, {@link java.lang.String} types, {@link java.lang.Class} types as well
     *                    as {@code MethodType} and {@code MethodHandle} instances. In order to avoid class loading,
     *                    it is also possible to supply unloaded types as {@link TypeDescription},
     *                    {@link JavaConstant.MethodHandle} or
     *                    {@link JavaConstant.MethodType} instances.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Constructor<?> constructor, Object... rawArgument) {
        return bootstrap(new MethodDescription.ForLoadedConstructor(constructor), rawArgument);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap constructor.
     *
     * @param constructor  The bootstrap constructor that is used to link the instrumented method.
     * @param rawArguments The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                     constant pool, i.e. primitive types (represented as their wrapper types) with a size of
     *                     at least 32 bit, {@link java.lang.String} types, {@link java.lang.Class} types as well
     *                     as {@code MethodType} and {@code MethodHandle} instances. In order to avoid class loading,
     *                     it is also possible to supply unloaded types as {@link TypeDescription},
     *                     {@link JavaConstant.MethodHandle} or
     *                     {@link JavaConstant.MethodType} instances.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Constructor<?> constructor, List<?> rawArguments) {
        return bootstrap(new MethodDescription.ForLoadedConstructor(constructor), rawArguments);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method or constructor.
     *
     * @param bootstrapMethod The bootstrap method or constructor that is used to link the instrumented method.
     * @param rawArgument     The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                        constant pool, i.e. primitive types (represented as their wrapper types) with a size of
     *                        at least 32 bit, {@link java.lang.String} types, {@link java.lang.Class} types as well
     *                        as {@code MethodType} and {@code MethodHandle} instances. In order to avoid class loading,
     *                        it is also possible to supply unloaded types as {@link TypeDescription},
     *                        {@link JavaConstant.MethodHandle} or
     *                        {@link JavaConstant.MethodType} instances.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(MethodDescription.InDefinedShape bootstrapMethod, Object... rawArgument) {
        return bootstrap(bootstrapMethod, Arrays.asList(rawArgument));
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method or constructor.
     *
     * @param bootstrapMethod The bootstrap method or constructor that is used to link the instrumented method.
     * @param rawArguments    The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                        constant pool, i.e. primitive types (represented as their wrapper types) with a size of
     *                        at least 32 bit, {@link java.lang.String} types, {@link java.lang.Class} types as well
     *                        as {@code MethodType} and {@code MethodHandle} instances. In order to avoid class loading,
     *                        it is also possible to supply unloaded types as {@link TypeDescription},
     *                        {@link JavaConstant.MethodHandle} or
     *                        {@link JavaConstant.MethodType} instances.
     * @return An implementation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(MethodDescription.InDefinedShape bootstrapMethod, List<?> rawArguments) {
        List<Object> arguments = new ArrayList<Object>(rawArguments.size());
        for (Object argument : rawArguments) {
            if (argument instanceof Class) {
                argument = new TypeDescription.ForLoadedType((Class<?>) argument);
            } else if (JavaType.METHOD_HANDLE.getTypeStub().isInstance(argument)) {
                argument = JavaConstant.MethodHandle.ofLoaded(argument);
            } else if (JavaType.METHOD_TYPE.getTypeStub().isInstance(argument)) {
                argument = JavaConstant.MethodType.ofLoaded(argument);
            }
            arguments.add(argument);
        }
        if (!bootstrapMethod.isBootstrap(arguments)) {
            throw new IllegalArgumentException("Not a valid bootstrap method " + bootstrapMethod + " for " + arguments);
        }
        List<Object> serializedArguments = new ArrayList<Object>(arguments.size());
        for (Object anArgument : arguments) {
            if (anArgument instanceof TypeDescription) {
                anArgument = Type.getType(((TypeDescription) anArgument).getDescriptor());
            } else if (anArgument instanceof JavaConstant) {
                anArgument = ((JavaConstant) anArgument).asConstantPoolValue();
            }
            serializedArguments.add(anArgument);
        }
        return new WithImplicitTarget(bootstrapMethod,
                serializedArguments,
                new InvocationProvider.Default(),
                TerminationHandler.ForMethodReturn.INSTANCE,
                Assigner.DEFAULT,
                Assigner.Typing.STATIC);
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
    public WithImplicitFieldType withReference(Object value) {
        return new WithImplicitFieldType(bootstrapMethod,
                handleArguments,
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
            argumentProviders.add(InvocationProvider.ArgumentProvider.ForStaticField.of(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return withNullValue(new TypeList.ForLoadedTypes(type).toArray(new TypeDescription[type.length]));
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
    public WithImplicitArgumentType withArgument(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Method parameter indices cannot be negative: " + index);
        }
        return new WithImplicitArgumentType(bootstrapMethod,
                handleArguments,
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
        return withThis(new TypeList.ForLoadedTypes(type).toArray(new TypeDescription[type.length]));
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
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
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArgument(InvocationProvider.ArgumentProvider.ForInterceptedMethodInstanceAndParameters.INSTANCE),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Passes the value of the specified instance field to the bootstrapped method. The field value of the created
     * private field must be set manually on any instrumented instance.
     *
     * @param fieldName The name of the field.
     * @param fieldType The type of the field.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the value of the defined
     * field.
     */
    public InvokeDynamic withInstanceField(String fieldName, java.lang.reflect.Type fieldType) {
        return withInstanceField(fieldName, TypeDefinition.Sort.describe(fieldType));
    }

    /**
     * Passes the value of the specified instance field to the bootstrapped method. The field value of the created
     * private field must be set manually on any instrumented instance.
     *
     * @param fieldName The name of the field.
     * @param fieldType The type of the field.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the value of the defined
     * field.
     */
    public InvokeDynamic withInstanceField(String fieldName, TypeDefinition fieldType) {
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForInstanceField(fieldName, fieldType.asGenericType())),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Passes the values of the specified fields to the bootstrap method. Any of the specified fields must already
     * exist for the instrumented type.
     *
     * @param fieldName The names of the fields to be passed to the bootstrapped method.
     * @return This invoke dynamic implementation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withField(String... fieldName) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(fieldName.length);
        for (String aFieldName : fieldName) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForExistingField(aFieldName));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Instructs this implementation to use the provided assigner and decides if the assigner should apply
     * dynamic typing.
     *
     * @param assigner The assigner to use.
     * @param typing   {@code true} if the assigner should attempt dynamic typing.
     * @return The invoke dynamic instruction where the given assigner and dynamic-typing directive are applied.
     */
    public InvokeDynamic withAssigner(Assigner assigner, Assigner.Typing typing) {
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider,
                terminationHandler,
                assigner,
                typing);
    }

    @Override
    public Implementation andThen(Implementation implementation) {
        return new Implementation.Compound(new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider,
                TerminationHandler.ForChainedInvocation.INSTANCE,
                assigner,
                typing),
                implementation);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return invocationProvider.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(implementationTarget.getInstrumentedType());
    }

    /**
     * Returns the invocation provider to be used for equals and hash code calculations.
     *
     * @return The invocation provider that represents this instance.
     */
    protected InvocationProvider getInvocationProvider() {
        return invocationProvider;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InvokeDynamic)) return false;
        InvokeDynamic that = (InvokeDynamic) other;
        return typing == that.typing
                && assigner.equals(that.assigner)
                && bootstrapMethod.equals(that.bootstrapMethod)
                && handleArguments.equals(that.handleArguments)
                && invocationProvider.equals(that.getInvocationProvider())
                && terminationHandler.equals(that.terminationHandler);
    }

    @Override
    public int hashCode() {
        int result = bootstrapMethod.hashCode();
        result = 31 * result + handleArguments.hashCode();
        result = 31 * result + getInvocationProvider().hashCode();
        result = 31 * result + terminationHandler.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + typing.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "InvokeDynamic{" +
                "bootstrapMethod=" + bootstrapMethod +
                ", handleArguments=" + handleArguments +
                ", invocationProvider=" + invocationProvider +
                ", terminationHandler=" + terminationHandler +
                ", assigner=" + assigner +
                ", typing=" + typing +
                '}';
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

                    @Override
                    public StackManipulation getStackManipulation() {
                        return stackManipulation;
                    }

                    @Override
                    public TypeDescription getReturnType() {
                        return returnType;
                    }

                    @Override
                    public String getInternalName() {
                        return internalName;
                    }

                    @Override
                    public List<TypeDescription> getParameterTypes() {
                        return parameterTypes;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Simple simple = (Simple) other;
                        return internalName.equals(simple.internalName)
                                && parameterTypes.equals(simple.parameterTypes)
                                && returnType.equals(simple.returnType)
                                && stackManipulation.equals(simple.stackManipulation);
                    }

                    @Override
                    public int hashCode() {
                        int result = stackManipulation.hashCode();
                        result = 31 * result + internalName.hashCode();
                        result = 31 * result + returnType.hashCode();
                        result = 31 * result + parameterTypes.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "InvokeDynamic.InvocationProvider.Target.Resolved.Simple{" +
                                "stackManipulation=" + stackManipulation +
                                ", internalName='" + internalName + '\'' +
                                ", returnType=" + returnType +
                                ", parameterTypes=" + parameterTypes +
                                '}';
                    }
                }
            }

            /**
             * A target that requests to dynamically invoke a method to substitute for a given method.
             */
            class ForMethodDescription implements Target, Target.Resolved {

                /**
                 * The method that is being substituted.
                 */
                private final MethodDescription.InDefinedShape methodDescription;

                /**
                 * Creates a new target for substituting a given method.
                 *
                 * @param methodDescription The method that is being substituted.
                 */
                protected ForMethodDescription(MethodDescription.InDefinedShape methodDescription) {
                    this.methodDescription = methodDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, Assigner assigner, Assigner.Typing typing) {
                    return this;
                }

                @Override
                public String getInternalName() {
                    return methodDescription.getInternalName();
                }

                @Override
                public TypeDescription getReturnType() {
                    return methodDescription.getReturnType().asErasure();
                }

                @Override
                public StackManipulation getStackManipulation() {
                    return MethodVariableAccess.allArgumentsOf(methodDescription).prependThisReference();
                }

                @Override
                public List<TypeDescription> getParameterTypes() {
                    return methodDescription.isStatic()
                            ? methodDescription.getParameters().asTypeList().asErasures()
                            : CompoundList.of(methodDescription.getDeclaringType().asErasure(), methodDescription.getParameters().asTypeList().asErasures());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && methodDescription.equals(((ForMethodDescription) other).methodDescription);
                }

                @Override
                public int hashCode() {
                    return methodDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.Target.ForMethodDescription{" +
                            "methodDescription=" + methodDescription +
                            '}';
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

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                            instrumentedMethod.isStatic()
                                    ? instrumentedMethod.getParameters().asTypeList().asErasures()
                                    : CompoundList.of(instrumentedMethod.getDeclaringType().asErasure(), instrumentedMethod.getParameters().asTypeList().asErasures()));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForInterceptedMethodInstanceAndParameters." + name();
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

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(MethodVariableAccess.allArgumentsOf(instrumentedMethod),
                            instrumentedMethod.getParameters().asTypeList().asErasures());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForInterceptedMethodParameters." + name();
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
                    this.primitiveType = new TypeDescription.ForLoadedType(primitiveType);
                    this.wrapperType = new TypeDescription.ForLoadedType(wrapperType);
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
                        return new ForClassConstant(new TypeDescription.ForLoadedType((Class<?>) value));
                    } else if (value instanceof Enum<?>) {
                        return new ForEnumerationValue(new EnumerationDescription.ForLoadedEnumeration((Enum<?>) value));
                    } else if (JavaType.METHOD_HANDLE.getTypeStub().isInstance(value)) {
                        return new ForJavaConstant(JavaConstant.MethodHandle.ofLoaded(value));
                    } else if (JavaType.METHOD_TYPE.getTypeStub().isInstance(value)) {
                        return new ForJavaConstant(JavaConstant.MethodType.ofLoaded(value));
                    } else {
                        return ForStaticField.of(value);
                    }
                }

                /**
                 * Creates an argument provider for a given primitive value.
                 *
                 * @param value The wrapper-type value to provide to the bootstrapped method.
                 * @return An argument provider for this value.
                 */
                protected abstract ArgumentProvider make(Object value);

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ConstantPoolWrapper." + name();
                }

                /**
                 * An argument provider that loads a primitive value from the constant pool and wraps it.
                 */
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

                    @Override
                    public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                        return new Resolved.Simple(new StackManipulation.Compound(stackManipulation,
                                assigner.assign(primitiveType.asGenericType(), wrapperType.asGenericType(), typing)), wrapperType);
                    }

                    @Override
                    public InstrumentedType prepare(InstrumentedType instrumentedType) {
                        return instrumentedType;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && ConstantPoolWrapper.this.equals(((WrappingArgumentProvider) other).getOuter())
                                && stackManipulation.equals(((WrappingArgumentProvider) other).stackManipulation);
                    }

                    /**
                     * Returns the outer instance.
                     *
                     * @return The outer instance.
                     */
                    private ConstantPoolWrapper getOuter() {
                        return ConstantPoolWrapper.this;
                    }

                    @Override
                    public int hashCode() {
                        return stackManipulation.hashCode() + 31 * ConstantPoolWrapper.this.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "InvokeDynamic.InvocationProvider.ArgumentProvider.ConstantPoolWrapper.WrappingArgumentProvider{" +
                                "constantPoolWrapper=" + ConstantPoolWrapper.this +
                                ", stackManipulation=" + stackManipulation +
                                '}';
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

                    @Override
                    public StackManipulation getLoadInstruction() {
                        return stackManipulation;
                    }

                    @Override
                    public List<TypeDescription> getLoadedTypes() {
                        return loadedTypes;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Simple simple = (Simple) other;
                        return loadedTypes.equals(simple.loadedTypes)
                                && stackManipulation.equals(simple.stackManipulation);
                    }

                    @Override
                    public int hashCode() {
                        int result = stackManipulation.hashCode();
                        result = 31 * result + loadedTypes.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "InvokeDynamic.InvocationProvider.ArgumentProvider.Resolved.Simple{" +
                                "stackManipulation=" + stackManipulation +
                                ", loadedTypes=" + loadedTypes +
                                '}';
                    }
                }
            }

            /**
             * An argument provider that loads the intercepted instance.
             */
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
                public ForThisInstance(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot get this instance from static method: " + instrumentedMethod);
                    } else if (!instrumentedType.isAssignableTo(typeDescription)) {
                        throw new IllegalStateException(instrumentedType + " is not assignable to " + instrumentedType);
                    }
                    return new Resolved.Simple(MethodVariableAccess.REFERENCE.loadOffset(0), typeDescription);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForThisInstance) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForThisInstance{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }

            /**
             * An argument provider for a value that is stored in a randomly named static field.
             */
            class ForStaticField implements ArgumentProvider {

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
                private final TypeDescription.Generic fieldType;

                /**
                 * The name of the field.
                 */
                private final String name;

                /**
                 * Creates a new argument provider that stores the given value in a static field.
                 *
                 * @param value     The value that is to be provided to the bootstrapped method.
                 * @param fieldType The type of the field which is also provided to the bootstrap method.
                 */
                public ForStaticField(Object value, TypeDescription.Generic fieldType) {
                    this.value = value;
                    this.fieldType = fieldType;
                    name = String.format("%s$%s", FIELD_PREFIX, RandomString.make());
                }

                /**
                 * Creates a new argument provider that stores the given value in a static field of the instance type.
                 *
                 * @param value The value that is to be provided to the bootstrapped method.
                 * @return A corresponding argument provider.
                 */
                public static ArgumentProvider of(Object value) {
                    return new ForStaticField(value, new TypeDescription.Generic.OfNonGenericType.ForLoadedType(value.getClass()));
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    FieldDescription fieldDescription = instrumentedType.getDeclaredFields().filter(named(name)).getOnly();
                    StackManipulation stackManipulation = assigner.assign(fieldDescription.getType(), fieldType, typing);
                    if (!stackManipulation.isValid()) {
                        throw new IllegalStateException("Cannot assign " + fieldDescription + " to " + fieldType);
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(FieldAccess.forField(fieldDescription).getter(),
                            stackManipulation), fieldDescription.getType().asErasure());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType
                            .withField(new FieldDescription.Token(name, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, fieldType))
                            .withInitializer(new LoadedTypeInitializer.ForStaticField(name, value));
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldType.equals(((ForStaticField) other).fieldType)
                            && value.equals(((ForStaticField) other).value);
                }

                @Override
                public int hashCode() {
                    return 31 * value.hashCode() + fieldType.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForStaticField{" +
                            "value=" + value +
                            ", fieldType=" + fieldType +
                            ", name='" + name + '\'' +
                            '}';
                }
            }

            /**
             * An argument provider that loads a value from an instance field.
             */
            class ForInstanceField implements ArgumentProvider {

                /**
                 * The name of the field.
                 */
                private final String fieldName;

                /**
                 * The type of the field.
                 */
                private final TypeDescription.Generic fieldType;

                /**
                 * Creates a new argument provider that provides a value from an instance field.
                 *
                 * @param fieldName The name of the field.
                 * @param fieldType The type of the field.
                 */
                public ForInstanceField(String fieldName, TypeDescription.Generic fieldType) {
                    this.fieldName = fieldName;
                    this.fieldType = fieldType;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot access " + fieldName + " from " + instrumentedMethod);
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(MethodVariableAccess.REFERENCE.loadOffset(0),
                            FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(fieldName)).getOnly()).getter()), fieldType.asErasure());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType.withField(new FieldDescription.Token(fieldName, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, fieldType));
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForInstanceField that = (ForInstanceField) other;
                    return fieldName.equals(that.fieldName) && fieldType.equals(that.fieldType);
                }

                @Override
                public int hashCode() {
                    int result = fieldName.hashCode();
                    result = 31 * result + fieldType.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForInstanceField{" +
                            "fieldName='" + fieldName + '\'' +
                            ", fieldType=" + fieldType +
                            '}';
                }
            }

            /**
             * Provides an argument from an existing field.
             */
            class ForExistingField implements ArgumentProvider {

                /**
                 * The name of the field.
                 */
                private final String fieldName;

                /**
                 * Creates a new argument provider that loads the value of an existing field.
                 *
                 * @param fieldName The name of the field.
                 */
                public ForExistingField(String fieldName) {
                    this.fieldName = fieldName;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    FieldDescription fieldDescription = locate(instrumentedType);
                    if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot access non-static " + fieldDescription + " from " + instrumentedMethod);
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.Trivial.INSTANCE
                                    : MethodVariableAccess.REFERENCE.loadOffset(0),
                            FieldAccess.forField(fieldDescription).getter()
                    ), fieldDescription.getType().asErasure());
                }

                /**
                 * Locates the specified field on the instrumented type.
                 *
                 * @param instrumentedType The instrumented type.
                 * @return The located field.
                 */
                private FieldDescription locate(TypeDescription instrumentedType) {
                    for (TypeDefinition currentType : instrumentedType) {
                        FieldList<?> fieldList = currentType.getDeclaredFields().filter(named(fieldName).and(isVisibleTo(instrumentedType)));
                        if (fieldList.size() != 0) {
                            return fieldList.getOnly();
                        }
                    }
                    throw new IllegalStateException(instrumentedType + " does not define a visible field " + fieldName);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldName.equals(((ForExistingField) other).fieldName);
                }

                @Override
                public int hashCode() {
                    return fieldName.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForExistingField{" +
                            "fieldName='" + fieldName + '\'' +
                            '}';
                }
            }

            /**
             * An argument provider that loads an argument of the intercepted method.
             */
            class ForMethodParameter implements ArgumentProvider {

                /**
                 * The index of the parameter.
                 */
                private final int index;

                /**
                 * Creates an argument provider for an argument of the intercepted method.
                 *
                 * @param index The index of the parameter.
                 */
                public ForMethodParameter(int index) {
                    this.index = index;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    ParameterList<?> parameters = instrumentedMethod.getParameters();
                    if (index >= parameters.size()) {
                        throw new IllegalStateException("No parameter " + index + " for " + instrumentedMethod);
                    }
                    return new Resolved.Simple(MethodVariableAccess.of(parameters.get(index).getType().asErasure())
                            .loadOffset(instrumentedMethod.getParameters().get(index).getOffset()), parameters.get(index).getType().asErasure());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && index == ((ForMethodParameter) other).index;
                }

                @Override
                public int hashCode() {
                    return index;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForMethodParameter{" +
                            "index=" + index +
                            '}';
                }
            }

            /**
             * An argument provider that loads an argument of the intercepted method.
             */
            class ForExplicitTypedMethodParameter implements ArgumentProvider {

                /**
                 * The index of the parameter.
                 */
                private final int index;

                /**
                 * The type of this method parameter.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates an argument provider for an argument of the intercepted method.
                 *
                 * @param index           The index of the parameter.
                 * @param typeDescription The type of this method parameter.
                 */
                public ForExplicitTypedMethodParameter(int index, TypeDescription typeDescription) {
                    this.index = index;
                    this.typeDescription = typeDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    ParameterList<?> parameters = instrumentedMethod.getParameters();
                    if (index >= parameters.size()) {
                        throw new IllegalStateException("No parameter " + index + " for " + instrumentedMethod);
                    }
                    StackManipulation stackManipulation = assigner.assign(parameters.get(index).getType(), typeDescription.asGenericType(), typing);
                    if (!stackManipulation.isValid()) {
                        throw new IllegalArgumentException("Cannot assign " + parameters.get(index) + " to " + typeDescription);
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(MethodVariableAccess.of(parameters.get(index)
                            .getType().asErasure()).loadOffset(parameters.get(index).getOffset()), stackManipulation), typeDescription);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && index == ((ForExplicitTypedMethodParameter) other).index
                            && typeDescription.equals(((ForExplicitTypedMethodParameter) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return index + 31 * typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForExplicitTypedMethodParameter{" +
                            "index=" + index +
                            ", typeDescription=" + typeDescription +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code boolean} value.
             */
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
                public ForBooleanConstant(boolean value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(boolean.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForBooleanConstant) other).value;
                }

                @Override
                public int hashCode() {
                    return (value ? 1 : 0);
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForBooleanConstant{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code byte} value.
             */
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
                public ForByteConstant(byte value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(byte.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForByteConstant) other).value;
                }

                @Override
                public int hashCode() {
                    return (int) value;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForByteConstant{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code short} value.
             */
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
                public ForShortConstant(short value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(short.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForShortConstant) other).value;
                }

                @Override
                public int hashCode() {
                    return (int) value;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForShortConstant{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code char} value.
             */
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
                public ForCharacterConstant(char value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(char.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForCharacterConstant) other).value;
                }

                @Override
                public int hashCode() {
                    return (int) value;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForCharacterConstant{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code int} value.
             */
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
                public ForIntegerConstant(int value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(int.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForIntegerConstant) other).value;
                }

                @Override
                public int hashCode() {
                    return value;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForIntegerConstant{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code long} value.
             */
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
                public ForLongConstant(long value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(LongConstant.forValue(value), new TypeDescription.ForLoadedType(long.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForLongConstant) other).value;
                }

                @Override
                public int hashCode() {
                    return (int) (value ^ (value >>> 32));
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForLongConstant{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code float} value.
             */
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
                public ForFloatConstant(float value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(FloatConstant.forValue(value), new TypeDescription.ForLoadedType(float.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && Float.compare(((ForFloatConstant) other).value, value) == 0;
                }

                @Override
                public int hashCode() {
                    return (value != +0.0f ? Float.floatToIntBits(value) : 0);
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForFloatConstant{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code double} value.
             */
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
                public ForDoubleConstant(double value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(DoubleConstant.forValue(value), new TypeDescription.ForLoadedType(double.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && Double.compare(((ForDoubleConstant) other).value, value) == 0;
                }

                @Override
                public int hashCode() {
                    long temp = Double.doubleToLongBits(value);
                    return (int) (temp ^ (temp >>> 32));
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForDoubleConstant{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@link java.lang.String} value.
             */
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
                public ForStringConstant(String value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(new TextConstant(value), TypeDescription.STRING);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value.equals(((ForStringConstant) other).value);
                }

                @Override
                public int hashCode() {
                    return value.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForStringConstant{" +
                            "value='" + value + '\'' +
                            '}';
                }
            }

            /**
             * An argument provider for a {@link java.lang.Class} constant.
             */
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
                public ForClassConstant(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(ClassConstant.of(typeDescription), TypeDescription.CLASS);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForClassConstant) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForClassConstant{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }

            /**
             * An argument provider for an {@link java.lang.Enum} constant.
             */
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
                public ForEnumerationValue(EnumerationDescription enumerationDescription) {
                    this.enumerationDescription = enumerationDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(FieldAccess.forEnumeration(enumerationDescription), enumerationDescription.getEnumerationType());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && enumerationDescription.equals(((ForEnumerationValue) other).enumerationDescription);
                }

                @Override
                public int hashCode() {
                    return enumerationDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForEnumerationValue{" +
                            "enumerationDescription=" + enumerationDescription +
                            '}';
                }
            }

            /**
             * An argument provider for the {@code null} value.
             */
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
                public ForNullValue(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(NullConstant.INSTANCE, typeDescription);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForNullValue) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForNullValue{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }

            /**
             * An argument provider for a Java instance.
             */
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
                public ForJavaConstant(JavaConstant javaConstant) {
                    this.javaConstant = javaConstant;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                    return new Resolved.Simple(javaConstant.asStackManipulation(), javaConstant.getType());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && javaConstant.equals(((ForJavaConstant) other).javaConstant);
                }

                @Override
                public int hashCode() {
                    return javaConstant.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForJavaConstant{" +
                            "javaConstant=" + javaConstant +
                            '}';
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

                @Override
                public String resolve(MethodDescription methodDescription) {
                    return methodDescription.getInternalName();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.NameProvider.ForInterceptedMethod." + name();
                }
            }

            /**
             * A name provider that provides an explicit name.
             */
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
                public ForExplicitName(String internalName) {
                    this.internalName = internalName;
                }

                @Override
                public String resolve(MethodDescription methodDescription) {
                    return internalName;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && internalName.equals(((ForExplicitName) other).internalName);
                }

                @Override
                public int hashCode() {
                    return internalName.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.NameProvider.ForExplicitName{" +
                            "internalName='" + internalName + '\'' +
                            '}';
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

                @Override
                public TypeDescription resolve(MethodDescription methodDescription) {
                    return methodDescription.getReturnType().asErasure();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ReturnTypeProvider.ForInterceptedMethod." + name();
                }
            }

            /**
             * Requests an explicit return type.
             */
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
                public ForExplicitType(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public TypeDescription resolve(MethodDescription methodDescription) {
                    return typeDescription;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForExplicitType) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ReturnTypeProvider.ForExplicitType{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }
        }

        /**
         * An invocation provider that requests a synthetic dynamic invocation where all arguments are explicitly
         * provided by the user.
         */
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
            public Default() {
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
            public Default(NameProvider nameProvider,
                           ReturnTypeProvider returnTypeProvider,
                           List<ArgumentProvider> argumentProviders) {
                this.nameProvider = nameProvider;
                this.returnTypeProvider = returnTypeProvider;
                this.argumentProviders = argumentProviders;
            }

            @Override
            public Target make(MethodDescription methodDescription) {
                return new Target(nameProvider.resolve(methodDescription),
                        returnTypeProvider.resolve(methodDescription),
                        argumentProviders,
                        methodDescription);
            }

            @Override
            public InvocationProvider appendArguments(List<ArgumentProvider> argumentProviders) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        CompoundList.of(this.argumentProviders, argumentProviders));
            }

            @Override
            public InvocationProvider appendArgument(ArgumentProvider argumentProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        CompoundList.of(this.argumentProviders, argumentProvider));
            }

            @Override
            public InvocationProvider withoutArguments() {
                return new Default(nameProvider,
                        returnTypeProvider,
                        Collections.<ArgumentProvider>emptyList());
            }

            @Override
            public InvocationProvider withNameProvider(NameProvider nameProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        argumentProviders);
            }

            @Override
            public InvocationProvider withReturnTypeProvider(ReturnTypeProvider returnTypeProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        argumentProviders);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                for (ArgumentProvider argumentProvider : argumentProviders) {
                    instrumentedType = argumentProvider.prepare(instrumentedType);
                }
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Default that = (Default) other;
                return argumentProviders.equals(that.argumentProviders)
                        && nameProvider.equals(that.nameProvider)
                        && returnTypeProvider.equals(that.returnTypeProvider);
            }

            @Override
            public int hashCode() {
                int result = nameProvider.hashCode();
                result = 31 * result + returnTypeProvider.hashCode();
                result = 31 * result + argumentProviders.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "InvokeDynamic.InvocationProvider.Default{" +
                        "nameProvider=" + nameProvider +
                        ", returnTypeProvider=" + returnTypeProvider +
                        ", argumentProviders=" + argumentProviders +
                        '}';
            }

            /**
             * A target for a synthetically bound method call.
             */
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
                public Target(String internalName,
                              TypeDescription returnType,
                              List<ArgumentProvider> argumentProviders,
                              MethodDescription instrumentedMethod) {
                    this.internalName = internalName;
                    this.returnType = returnType;
                    this.argumentProviders = argumentProviders;
                    this.instrumentedMethod = instrumentedMethod;
                }

                @Override
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

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Target target = (Target) other;
                    return argumentProviders.equals(target.argumentProviders)
                            && instrumentedMethod.equals(target.instrumentedMethod)
                            && internalName.equals(target.internalName)
                            && returnType.equals(target.returnType);
                }

                @Override
                public int hashCode() {
                    int result = internalName.hashCode();
                    result = 31 * result + returnType.hashCode();
                    result = 31 * result + argumentProviders.hashCode();
                    result = 31 * result + instrumentedMethod.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.Default.Target{" +
                            "internalName='" + internalName + '\'' +
                            ", returnType=" + returnType +
                            ", argumentProviders=" + argumentProviders +
                            ", instrumentedMethod=" + instrumentedMethod +
                            '}';
                }
            }
        }
    }

    /**
     * A termination handler is responsible to handle the return value of a method that is invoked via a
     * {@link net.bytebuddy.implementation.InvokeDynamic}.
     */
    protected interface TerminationHandler {

        /**
         * Returns a stack manipulation that handles the method return.
         *
         * @param interceptedMethod The method being intercepted.
         * @param returnType        The return type of the instrumented method.
         * @param assigner          The assigner to use.
         * @param typing            Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return A stack manipulation that handles the method return.
         */
        StackManipulation resolve(MethodDescription interceptedMethod,
                                  TypeDescription returnType,
                                  Assigner assigner,
                                  Assigner.Typing typing);

        /**
         * Returns the return value of the dynamic invocation from the intercepted method.
         */
        enum ForMethodReturn implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription interceptedMethod, TypeDescription returnType, Assigner assigner, Assigner.Typing typing) {
                StackManipulation stackManipulation = assigner.assign(returnType.asGenericType(), interceptedMethod.getReturnType(), typing);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot return " + returnType + " from " + interceptedMethod);
                }
                return new StackManipulation.Compound(stackManipulation, MethodReturn.of(interceptedMethod.getReturnType().asErasure()));
            }

            @Override
            public String toString() {
                return "InvokeDynamic.TerminationHandler.ForMethodReturn." + name();
            }
        }

        /**
         * Drops the return value of the dynamic invocation from the operand stack without returning from the
         * intercepted method.
         */
        enum ForChainedInvocation implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription interceptedMethod, TypeDescription returnType, Assigner assigner, Assigner.Typing typing) {
                return Removal.pop(interceptedMethod.isConstructor()
                        ? interceptedMethod.getDeclaringType().asErasure()
                        : interceptedMethod.getReturnType().asErasure());
            }

            @Override
            public String toString() {
                return "InvokeDynamic.TerminationHandler.ForChainedInvocation." + name();
            }
        }
    }

    /**
     * An abstract delegator that allows to specify a configuration for any specification of an argument.
     */
    protected abstract static class AbstractDelegator extends InvokeDynamic {

        /**
         * Creates a new abstract delegator for a dynamic method invocation.
         *
         * @param bootstrapMethod    The bootstrap method.
         * @param handleArguments    The arguments that are provided to the bootstrap method.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        public AbstractDelegator(MethodDescription.InDefinedShape bootstrapMethod,
                                 List<?> handleArguments,
                                 InvocationProvider invocationProvider,
                                 TerminationHandler terminationHandler,
                                 Assigner assigner,
                                 Assigner.Typing typing) {
            super(bootstrapMethod, handleArguments, invocationProvider, terminationHandler, assigner, typing);
        }

        /**
         * Resolves the current configuration into a fully initialized invoke dynamic instance.
         *
         * @return The fully resolved invoke dynamic instance.
         */
        protected abstract InvokeDynamic materialize();

        @Override
        public InvokeDynamic withBooleanValue(boolean... value) {
            return materialize().withBooleanValue(value);
        }

        @Override
        public InvokeDynamic withByteValue(byte... value) {
            return materialize().withByteValue(value);
        }

        @Override
        public InvokeDynamic withShortValue(short... value) {
            return materialize().withShortValue(value);
        }

        @Override
        public InvokeDynamic withCharacterValue(char... value) {
            return materialize().withCharacterValue(value);
        }

        @Override
        public InvokeDynamic withIntegerValue(int... value) {
            return materialize().withIntegerValue(value);
        }

        @Override
        public InvokeDynamic withLongValue(long... value) {
            return materialize().withLongValue(value);
        }

        @Override
        public InvokeDynamic withFloatValue(float... value) {
            return materialize().withFloatValue(value);
        }

        @Override
        public InvokeDynamic withDoubleValue(double... value) {
            return materialize().withDoubleValue(value);
        }

        @Override
        public InvokeDynamic withValue(Object... value) {
            return materialize().withValue(value);
        }

        @Override
        public WithImplicitFieldType withReference(Object value) {
            return materialize().withReference(value);
        }

        @Override
        public InvokeDynamic withReference(Object... value) {
            return materialize().withReference(value);
        }

        @Override
        public InvokeDynamic withType(TypeDescription... typeDescription) {
            return materialize().withType(typeDescription);
        }

        @Override
        public InvokeDynamic withInstance(JavaConstant... javaConstant) {
            return materialize().withInstance(javaConstant);
        }

        @Override
        public InvokeDynamic withNullValue(Class<?>... type) {
            return materialize().withNullValue(type);
        }

        @Override
        public InvokeDynamic withNullValue(TypeDescription... typeDescription) {
            return materialize().withNullValue(typeDescription);
        }

        @Override
        public InvokeDynamic withArgument(int... index) {
            return materialize().withArgument(index);
        }

        @Override
        public WithImplicitArgumentType withArgument(int index) {
            return materialize().withArgument(index);
        }

        @Override
        public InvokeDynamic withThis(Class<?>... type) {
            return materialize().withThis(type);
        }

        @Override
        public InvokeDynamic withThis(TypeDescription... typeDescription) {
            return materialize().withThis(typeDescription);
        }

        @Override
        public InvokeDynamic withMethodArguments() {
            return materialize().withMethodArguments();
        }

        @Override
        public InvokeDynamic withImplicitAndMethodArguments() {
            return materialize().withImplicitAndMethodArguments();
        }

        @Override
        public InvokeDynamic withInstanceField(String fieldName, java.lang.reflect.Type fieldType) {
            return materialize().withInstanceField(fieldName, fieldType);
        }

        @Override
        public InvokeDynamic withInstanceField(String fieldName, TypeDefinition fieldType) {
            return materialize().withInstanceField(fieldName, fieldType);
        }

        @Override
        public InvokeDynamic withField(String... fieldName) {
            return materialize().withField(fieldName);
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
         * @param bootstrapMethod    The bootstrap method.
         * @param handleArguments    The arguments that are provided to the bootstrap method.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected WithImplicitArguments(MethodDescription.InDefinedShape bootstrapMethod,
                                        List<?> handleArguments,
                                        InvocationProvider invocationProvider,
                                        TerminationHandler terminationHandler,
                                        Assigner assigner,
                                        Assigner.Typing typing) {
            super(bootstrapMethod,
                    handleArguments,
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
            return new InvokeDynamic(bootstrapMethod,
                    handleArguments,
                    invocationProvider.withoutArguments(),
                    terminationHandler,
                    assigner,
                    typing);
        }

        @Override
        protected InvokeDynamic materialize() {
            return withoutArguments();
        }

        @Override
        public WithImplicitArguments withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
                    invocationProvider,
                    terminationHandler,
                    assigner,
                    typing);
        }

        @Override
        public String toString() {
            return "InvokeDynamic.WithImplicitArguments{" +
                    "bootstrapMethod=" + bootstrapMethod +
                    ", handleArguments=" + handleArguments +
                    ", invocationProvider=" + invocationProvider +
                    ", terminationHandler=" + terminationHandler +
                    ", assigner=" + assigner +
                    ", typing=" + typing +
                    '}';
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
         * @param bootstrapMethod    The bootstrap method.
         * @param handleArguments    The arguments that are provided to the bootstrap method.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        protected WithImplicitTarget(MethodDescription.InDefinedShape bootstrapMethod,
                                     List<?> handleArguments,
                                     InvocationProvider invocationProvider,
                                     TerminationHandler terminationHandler,
                                     Assigner assigner,
                                     Assigner.Typing typing) {
            super(bootstrapMethod,
                    handleArguments,
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
            return invoke(new TypeDescription.ForLoadedType(returnType));
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
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
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
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
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
            return invoke(methodName, new TypeDescription.ForLoadedType(returnType));
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
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
                    invocationProvider
                            .withNameProvider(new InvocationProvider.NameProvider.ForExplicitName(methodName))
                            .withReturnTypeProvider(new InvocationProvider.ReturnTypeProvider.ForExplicitType(returnType)),
                    terminationHandler,
                    assigner,
                    typing);
        }

        @Override
        public String toString() {
            return "InvokeDynamic.WithImplicitTarget{" +
                    "bootstrapMethod=" + bootstrapMethod +
                    ", handleArguments=" + handleArguments +
                    ", invocationProvider=" + invocationProvider +
                    ", terminationHandler=" + terminationHandler +
                    ", assigner=" + assigner +
                    ", typing=" + typing +
                    '}';
        }
    }

    /**
     * A step in the invoke dynamic domain specific language that allows to explicitly specify a field type for a reference value.
     */
    @SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "Super type implementation convers use case")
    public static class WithImplicitFieldType extends AbstractDelegator {

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
         * @param bootstrapMethod    The bootstrap method.
         * @param handleArguments    The arguments that are provided to the bootstrap method.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param value              The value that is supplied as the next argument to the bootstrapped method.
         */
        protected WithImplicitFieldType(MethodDescription.InDefinedShape bootstrapMethod,
                                        List<?> handleArguments,
                                        InvocationProvider invocationProvider,
                                        TerminationHandler terminationHandler,
                                        Assigner assigner,
                                        Assigner.Typing typing,
                                        Object value) {
            super(bootstrapMethod, handleArguments, invocationProvider, terminationHandler, assigner, typing);
            this.value = value;
            this.argumentProvider = InvocationProvider.ArgumentProvider.ForStaticField.of(value);
        }

        /**
         * Defines the given value to be treated as an instance of the provided type.
         *
         * @param type The type as which the provided instance should be handed to the bootstrap method.
         * @return An invoke dynamic instance with the same configuration as this instance but with the provided argument
         * appended to the arguments of the bootstrapped method.
         */
        public InvokeDynamic as(java.lang.reflect.Type type) {
            return as(TypeDefinition.Sort.describe(type));
        }

        /**
         * Defines the given value to be treated as an instance of the provided type.
         *
         * @param typeDefinition The type as which the provided instance should be handed to the bootstrap method.
         * @return An invoke dynamic instance with the same configuration as this instance but with the provided argument
         * appended to the arguments of the bootstrapped method.
         */
        public InvokeDynamic as(TypeDefinition typeDefinition) {
            return new InvokeDynamic(bootstrapMethod,
                    handleArguments,
                    invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForStaticField(value, typeDefinition.asGenericType())),
                    terminationHandler,
                    assigner,
                    typing);
        }

        @Override
        protected InvokeDynamic materialize() {
            return new InvokeDynamic(bootstrapMethod,
                    handleArguments,
                    invocationProvider.appendArgument(argumentProvider),
                    terminationHandler,
                    assigner,
                    typing);
        }

        @Override
        public InvokeDynamic withAssigner(Assigner assigner, Assigner.Typing typing) {
            return materialize().withAssigner(assigner, typing);
        }

        @Override
        public Implementation andThen(Implementation implementation) {
            return materialize().andThen(implementation);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return materialize().prepare(instrumentedType);
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return materialize().appender(implementationTarget);
        }

        @Override
        protected InvocationProvider getInvocationProvider() {
            return materialize().getInvocationProvider();
        }

        @Override
        public String toString() {
            return "InvokeDynamic.WithImplicitFieldType{" +
                    "bootstrapMethod=" + bootstrapMethod +
                    ", handleArguments=" + handleArguments +
                    ", invocationProvider=" + invocationProvider +
                    ", terminationHandler=" + terminationHandler +
                    ", assigner=" + assigner +
                    ", typing=" + typing +
                    ", value=" + value +
                    '}';
        }
    }

    /**
     * An invoke dynamic implementation where the last argument is an implicitly typed method argument.
     */
    @SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "Super type implementation convers use case")
    public static class WithImplicitArgumentType extends AbstractDelegator {

        /**
         * The index of the method argument.
         */
        private final int index;

        /**
         * Creates a new invoke dynamic instance with an implicit field type for the provided value.
         *
         * @param bootstrapMethod    The bootstrap method.
         * @param handleArguments    The arguments that are provided to the bootstrap method.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param index              The index of of the argument to supply to the bootstapped method.
         */
        protected WithImplicitArgumentType(MethodDescription.InDefinedShape bootstrapMethod,
                                           List<?> handleArguments,
                                           InvocationProvider invocationProvider,
                                           TerminationHandler terminationHandler,
                                           Assigner assigner,
                                           Assigner.Typing typing,
                                           int index) {
            super(bootstrapMethod, handleArguments, invocationProvider, terminationHandler, assigner, typing);
            this.index = index;
        }

        /**
         * Defines the given argument to be treated as an instance of the provided type.
         *
         * @param type The type as which the provided argument should be handed to the bootstrap method.
         * @return An invoke dynamic instance with the same configuration as this instance but with the provided argument
         * appended to the arguments of the bootstrapped method.
         */
        public InvokeDynamic as(Class<?> type) {
            return as(new TypeDescription.ForLoadedType(type));
        }

        /**
         * Defines the given argument to be treated as an instance of the provided type.
         *
         * @param typeDescription The type as which the provided argument should be handed to the bootstrap method.
         * @return An invoke dynamic instance with the same configuration as this instance but with the provided argument
         * appended to the arguments of the bootstrapped method.
         */
        public InvokeDynamic as(TypeDescription typeDescription) {
            return new InvokeDynamic(bootstrapMethod,
                    handleArguments,
                    invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForExplicitTypedMethodParameter(index, typeDescription)),
                    terminationHandler,
                    assigner,
                    typing);
        }

        @Override
        protected InvokeDynamic materialize() {
            return new InvokeDynamic(bootstrapMethod,
                    handleArguments,
                    invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForMethodParameter(index)),
                    terminationHandler,
                    assigner,
                    typing);
        }

        @Override
        public InvokeDynamic withAssigner(Assigner assigner, Assigner.Typing typing) {
            return materialize().withAssigner(assigner, typing);
        }

        @Override
        public Implementation andThen(Implementation implementation) {
            return materialize().andThen(implementation);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return materialize().prepare(instrumentedType);
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return materialize().appender(implementationTarget);
        }

        @Override
        protected InvocationProvider getInvocationProvider() {
            return materialize().getInvocationProvider();
        }

        @Override
        public String toString() {
            return "InvokeDynamic.WithImplicitArgumentType{" +
                    "bootstrapMethod=" + bootstrapMethod +
                    ", handleArguments=" + handleArguments +
                    ", invocationProvider=" + invocationProvider +
                    ", terminationHandler=" + terminationHandler +
                    ", assigner=" + assigner +
                    ", typing=" + typing +
                    ", index=" + index +
                    '}';
        }
    }

    /**
     * The byte code appender to be used by the {@link net.bytebuddy.implementation.InvokeDynamic} implementation.
     */
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

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            InvocationProvider.Target.Resolved target = invocationProvider.make(instrumentedMethod).resolve(instrumentedType, assigner, typing);
            StackManipulation.Size size = new StackManipulation.Compound(
                    target.getStackManipulation(),
                    MethodInvocation.invoke(bootstrapMethod)
                            .dynamic(target.getInternalName(),
                                    target.getReturnType(),
                                    target.getParameterTypes(),
                                    handleArguments),
                    terminationHandler.resolve(instrumentedMethod, target.getReturnType(), assigner, typing)
            ).apply(methodVisitor, implementationContext);
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Appender appender = (Appender) other;
            return instrumentedType.equals(appender.instrumentedType)
                    && InvokeDynamic.this.equals(appender.getOuter());
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private InvokeDynamic getOuter() {
            return InvokeDynamic.this;
        }

        @Override
        public int hashCode() {
            return instrumentedType.hashCode();
        }

        @Override
        public String toString() {
            return "InvokeDynamic.Appender{" +
                    "invokeDynamic=" + InvokeDynamic.this +
                    ", instrumentedType=" + instrumentedType +
                    '}';
        }
    }
}

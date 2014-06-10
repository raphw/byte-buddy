package net.bytebuddy.instrumentation.method.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * A collection of common {@link net.bytebuddy.instrumentation.method.matcher.MethodMatcher}
 * implementations.
 * <p>&nbsp;</p>
 * This class is not meant to be instantiated but is usually imported statically in order to allow for improving
 * the readability of code.
 */
public final class MethodMatchers {

    /**
     * Non-invokable constructor for this utility class.
     */
    private MethodMatchers() {
        throw new UnsupportedOperationException();
    }

    /**
     * Selects a method by its exact name. For example, {@code name("foo")} will match {@code foo} but not {@code bar}
     * or {@code FOO}.
     *
     * @param name The name to be matched.
     * @return A method matcher for the specified name.
     */
    public static JunctionMethodMatcher named(String name) {
        return new MethodNameMethodMatcher(name, MatchMode.EQUALS_FULLY);
    }

    /**
     * Selects a method by its case insensitive, exact name. For example, {@code namedIgnoreCase("foo")} will match
     * {@code foo} and {@code FOO} but not {@code bar}.
     *
     * @param name The name to be matched.
     * @return A method matcher for the specified name.
     */
    public static JunctionMethodMatcher namedIgnoreCase(String name) {
        return new MethodNameMethodMatcher(name, MatchMode.EQUALS_FULLY_IGNORE_CASE);
    }

    /**
     * Selects a method by its exact name prefix. For example, {@code nameStartsWith("foo")} will match
     * {@code foo} and {@code foobar} but not {@code bar} and {@code FOO}.
     *
     * @param prefix The name prefix to be matched.
     * @return A method matcher for the specified name.
     */
    public static JunctionMethodMatcher nameStartsWith(String prefix) {
        return new MethodNameMethodMatcher(prefix, MatchMode.STARTS_WITH);
    }

    /**
     * Selects a method by its case insensitive exact name prefix. For example, {@code nameStartsWithIgnoreCase("foo")}
     * will match {@code foo}, {@code foobar} and {@code FOO} but not {@code bar}.
     *
     * @param prefix The name prefix to be matched.
     * @return A method matcher for the specified name.
     */
    public static JunctionMethodMatcher nameStartsWithIgnoreCase(String prefix) {
        return new MethodNameMethodMatcher(prefix, MatchMode.STARTS_WITH_IGNORE_CASE);
    }

    /**
     * Selects a method by its exact name suffix. For example, {@code nameEndsWith("bar")} will match {@code bar} and
     * {@code foobar} but not {@code BAR} and {@code foo}.
     *
     * @param suffix The name suffix to be matched.
     * @return A method matcher for the specified name.
     */
    public static JunctionMethodMatcher nameEndsWith(String suffix) {
        return new MethodNameMethodMatcher(suffix, MatchMode.ENDS_WITH);
    }

    /**
     * Selects a method by its case insensitive exact name suffix. For example, {@code nameEndsWithIgnoreCase("bar")}
     * will match {@code bar}, {@code foobar} and {@code BAR} but not {@code foo}.
     *
     * @param suffix The name suffix to be matched.
     * @return A method matcher for the specified name.
     */
    public static JunctionMethodMatcher nameEndsWithIgnoreCase(String suffix) {
        return new MethodNameMethodMatcher(suffix, MatchMode.ENDS_WITH_IGNORE_CASE);
    }

    /**
     * Selects a method by its exact name infix. For example, {@code nameContains("a")} will
     * match {@code bar}, {@code foobar} and {@code BaR} but not {@code foo} and {@code BAR}.
     *
     * @param infix The name infix to be matched.
     * @return A method matcher for the specified name.
     */
    public static JunctionMethodMatcher nameContains(String infix) {
        return new MethodNameMethodMatcher(infix, MatchMode.CONTAINS);
    }

    /**
     * Selects a method by its case insensitive exact name infix. For example, {@code nameContainsIgnoreCase("a")}
     * will match {@code bar}, {@code foobar}, {@code BAR} and {@code BAR} but not {@code foo}.
     *
     * @param infix The name infix to be matched.
     * @return A method matcher for the specified name.
     */
    public static JunctionMethodMatcher nameContainsIgnoreCase(String infix) {
        return new MethodNameMethodMatcher(infix, MatchMode.CONTAINS_IGNORE_CASE);
    }

    /**
     * Selects a method by its name matching a regular expression. For example, {@code matches("f(o){2}.*")} will
     * match {@code foo}, {@code foobar} but not {@code Foo} or {@code bar}.
     *
     * @param regex The regular expression to be matched.
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher nameMatches(String regex) {
        return new MethodNameMethodMatcher(regex, MatchMode.MATCHES);
    }

    /**
     * Selects a method when it is {@code public}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isPublic() {
        return new ModifierMethodMatcher(Modifier.PUBLIC);
    }

    /**
     * Selects a method when it is {@code protected}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isProtected() {
        return new ModifierMethodMatcher(Modifier.PROTECTED);
    }

    /**
     * Selects a method when it is package private, i.e. is defined without a explicit modifier for visibility.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isPackagePrivate() {
        return not(isPublic().or(isProtected()).or(isPrivate()));
    }

    /**
     * Selects a method when it is {@code private}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isPrivate() {
        return new ModifierMethodMatcher(Modifier.PRIVATE);
    }

    /**
     * Selects a method when it is {@code final}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isFinal() {
        return new ModifierMethodMatcher(Modifier.FINAL);
    }

    /**
     * Selects a method when it is {@code static}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isStatic() {
        return new ModifierMethodMatcher(Modifier.STATIC);
    }

    /**
     * Selects a method when it is {@code synchronized}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isSynchronized() {
        return new ModifierMethodMatcher(Modifier.SYNCHRONIZED);
    }

    /**
     * Selects a method when it is {@code native}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isNative() {
        return new ModifierMethodMatcher(Modifier.NATIVE);
    }

    /**
     * Selects a method when it is {@code strictfp}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isStrict() {
        return new ModifierMethodMatcher(Modifier.STRICT);
    }

    /**
     * Selects a method when it is defined using a var args argument.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isVarArgs() {
        return new ModifierMethodMatcher(Opcodes.ACC_VARARGS);
    }

    /**
     * Selects a method when it is {@code synthetic}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isSynthetic() {
        return new ModifierMethodMatcher(Opcodes.ACC_SYNTHETIC);
    }

    /**
     * Selects a method when it is marked as a bridge method.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isBridge() {
        return new ModifierMethodMatcher(Opcodes.ACC_BRIDGE);
    }

    /**
     * Selects a method by its exact return type.
     *
     * @param type The return type of the method.
     * @return A new method matcher that selects methods returning {@code type}.
     */
    public static JunctionMethodMatcher returns(Class<?> type) {
        return new ReturnTypeMethodMatcher(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Selects a method by its exact return type.
     *
     * @param type A description of the return type of the method.
     * @return A new method matcher that selects methods returning {@code type}.
     */
    public static JunctionMethodMatcher returns(TypeDescription type) {
        return new ReturnTypeMethodMatcher(type);
    }

    /**
     * Selects a method by its return type where only methods are matched that return a subtype of the given
     * type, including the given type itself.
     *
     * @param type The return type that should be matched for the given method.
     * @return A new method matcher that selects methods returning subtypes of {@code type}.
     */
    public static JunctionMethodMatcher returnsSubtypeOf(Class<?> type) {
        return returnsSubtypeOf(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Selects a method by its return type where only methods are matched that return a subtype of the given
     * type, including the given type itself.
     *
     * @param type The description of the return type that should be matched for the given method.
     * @return A new method matcher that selects methods returning subtypes of {@code type}.
     */
    public static JunctionMethodMatcher returnsSubtypeOf(TypeDescription type) {
        return new ReturnSubtypeMethodMatcher(type);
    }

    /**
     * Selects a method by its return type where only methods are matched that return a super type of the given
     * type, including the given type itself.
     *
     * @param type The return type that should be matched for the given method.
     * @return A new method matcher that selects methods returning super types of {@code type}.
     */
    public static JunctionMethodMatcher returnsSupertypeOf(Class<?> type) {
        return returnsSupertypeOf(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Selects a method by its return type where only methods are matched that return a super type of the given
     * type, including the given type itself.
     *
     * @param type The description of the return type that should be matched for the given method.
     * @return A new method matcher that selects methods returning super types of {@code type}.
     */
    public static JunctionMethodMatcher returnsSupertypeOf(TypeDescription type) {
        return new ReturnSupertypeMethodMatcher(type);
    }

    /**
     * Selects a method by its exact parameter types in their exact order.
     *
     * @param types The parameter types of the method.
     * @return A new method matcher that selects methods with the exact parameters contained by {@code types}.
     */
    public static JunctionMethodMatcher takesArguments(Class<?>... types) {
        return takesArguments(new TypeList.ForLoadedType(types));
    }

    /**
     * Selects a method by its exact parameter types in their exact order.
     *
     * @param types The parameter types of the method.
     * @return A new method matcher that selects methods with the exact parameters contained by {@code types}.
     */
    public static JunctionMethodMatcher takesArguments(TypeDescription... types) {
        return takesArguments(Arrays.asList(types));
    }

    /**
     * Selects a method by its exact parameter types in their exact order.
     *
     * @param types A list of parameter types of the method. The list will not be copied.
     * @return A new method matcher that selects methods with the exact parameters contained by {@code types}.
     */
    public static JunctionMethodMatcher takesArguments(List<? extends TypeDescription> types) {
        return new ParameterTypeMethodMatcher(types);
    }

    /**
     * Selects a method by the number of its parameters.
     *
     * @param number The number of parameters of any matching method.
     * @return A method matcher that matches methods with {@code number} parameters.
     */
    public static JunctionMethodMatcher takesArguments(int number) {
        return new ParameterCountMethodMatcher(number);
    }

    /**
     * Selects a method by its parameter types in their exact order where only parameter types are matched
     * that are subtypes of the given types, including the given type.
     *
     * @param types The parameter types to match against.
     * @return A new method matcher that selects methods by its parameter types as given by {@code types}.
     */
    public static JunctionMethodMatcher takesArgumentsAsSubtypesOf(Class<?>... types) {
        return takesArgumentsAsSubtypesOf(new TypeList.ForLoadedType(types));
    }

    /**
     * Selects a method by its parameter types in their exact order where only parameter types are matched
     * that are subtypes of the given types, including the given type.
     *
     * @param types The parameter types to match against.
     * @return A new method matcher that selects methods by its parameter types as given by {@code types}.
     */
    public static JunctionMethodMatcher takesArgumentsAsSubtypesOf(TypeDescription... types) {
        return takesArgumentsAsSubtypesOf(Arrays.asList(types));
    }

    /**
     * Selects a method by its parameter types in their exact order where only parameter types are matched
     * that are subtypes of the given types, including the given type.
     *
     * @param types The parameter types to match against.
     * @return A new method matcher that selects methods by its parameter types as given by {@code types}.
     */
    public static JunctionMethodMatcher takesArgumentsAsSubtypesOf(List<? extends TypeDescription> types) {
        return new ParameterSubtypeMethodMatcher(types);
    }

    /**
     * Selects a method by its parameter types in their exact order where only parameter types are matched
     * that are super types of the given types, including the given type.
     *
     * @param types The parameter types to match against.
     * @return A new method matcher that selects methods by its parameter types as given by {@code types}.
     */
    public static JunctionMethodMatcher takesArgumentsAsSuperTypesOf(Class<?>... types) {
        return takesArgumentsAsSuperTypesOf(new TypeList.ForLoadedType(types));
    }

    /**
     * Selects a method by its parameter types in their exact order where only parameter types are matched
     * that are super types of the given types, including the given type.
     *
     * @param types The parameter types to match against.
     * @return A new method matcher that selects methods by its parameter types as given by {@code types}.
     */
    public static JunctionMethodMatcher takesArgumentsAsSuperTypesOf(TypeDescription... types) {
        return takesArgumentsAsSuperTypesOf(Arrays.asList(types));
    }

    /**
     * Selects a method by its parameter types in their exact order where only parameter types are matched
     * that are super types of the given types, including the given type.
     *
     * @param types The parameter types to match against.
     * @return A new method matcher that selects methods by its parameter types as given by {@code types}.
     */
    public static JunctionMethodMatcher takesArgumentsAsSuperTypesOf(List<? extends TypeDescription> types) {
        return new ParameterSuperTypeMethodMatcher(types);
    }

    /**
     * Selects a method depending on whether they can throw a specific exception. Exceptions that subclass
     * {@link java.lang.Error} or {@link java.lang.RuntimeException} can always be thrown.
     *
     * @param exceptionType The exception type to be thrown.
     * @return A new method matcher that selects methods on whether they can throw {@code exceptionType}.
     */
    public static JunctionMethodMatcher canThrow(Class<? extends Throwable> exceptionType) {
        return RuntimeException.class.isAssignableFrom(exceptionType) || Error.class.isAssignableFrom(exceptionType)
                ? new BooleanMethodMatcher(true)
                : new DeclaredExceptionMethodMatcher(new TypeDescription.ForLoadedType(exceptionType));
    }

    /**
     * Selects a method depending on whether they can throw a specific exception. Exceptions that subclass
     * {@link java.lang.Error} or {@link java.lang.RuntimeException} can always be thrown.
     *
     * @param exceptionType The exception type to be thrown.
     * @return A new method matcher that selects methods on whether they can throw {@code exceptionType}.
     */
    public static JunctionMethodMatcher canThrow(TypeDescription exceptionType) {
        if (exceptionType.isAssignableTo(Throwable.class)) {
            if (exceptionType.isAssignableTo(RuntimeType.class) || exceptionType.isAssignableTo(Error.class)) {
                return new BooleanMethodMatcher(true);
            }
            return new DeclaredExceptionMethodMatcher(exceptionType);
        } else {
            throw new IllegalArgumentException(exceptionType + " is not an exception type");
        }
    }

    /**
     * Selects an exact method. Note that the resulting matcher will not check overriden signatures.
     *
     * @param method The method to match.
     * @return A method matcher that matches the given method.
     */
    public static JunctionMethodMatcher is(Method method) {
        return new LoadedMethodEqualityMethodMatcher(method);
    }

    /**
     * Selects an exact constructor. Note that the resulting matcher will not check overriden signatures.
     *
     * @param constructor The constructor to match.
     * @return A method matcher that matches the given constructor.
     */
    public static JunctionMethodMatcher is(Constructor<?> constructor) {
        return new LoadedConstructorEqualityMethodMatcher(constructor);
    }

    /**
     * A method matcher that matches a given method description.
     *
     * @param methodDescription The method description to be matched.
     * @return A method matcher that matches the given method description.
     */
    public static JunctionMethodMatcher is(MethodDescription methodDescription) {
        return new MethodDescriptionMethodMatcher(methodDescription);
    }

    /**
     * Selects methods that are not constructors.
     *
     * @return A new method matcher that matches methods but not constructors.
     */
    public static JunctionMethodMatcher isMethod() {
        return new IsMethodMethodMatcher();
    }

    /**
     * Selects methods that are not constructors.
     *
     * @return A new method matcher that matches constructors but not methods.
     */
    public static JunctionMethodMatcher isConstructor() {
        return not(isMethod());
    }

    /**
     * Checks if a method is visible for a given type.
     *
     * @param typeDescription The type to which a method should be visible.
     * @return A new method matcher that matches any method that is visible to the given type.
     */
    public static JunctionMethodMatcher isVisibleTo(Class<?> typeDescription) {
        return isVisibleTo(new TypeDescription.ForLoadedType(typeDescription));
    }

    /**
     * Checks if a method is visible for a given type.
     *
     * @param typeDescription The type to which a method should be visible.
     * @return A new method matcher that matches any method that is visible to the given type.
     */
    public static JunctionMethodMatcher isVisibleTo(TypeDescription typeDescription) {
        return new VisibilityMethodMatcher(typeDescription);
    }

    /**
     * Selects method that are declared by a given type. Note that this matcher will not attempt to
     * match overridden signatures.
     *
     * @param type The type to match the method declaration against.
     * @return A method matcher that is matching the given type.
     */
    public static JunctionMethodMatcher isDeclaredBy(TypeDescription type) {
        return new DeclaringTypeMethodMatcher(type);
    }

    /**
     * Selects method that are declared by a given type. Note that this matcher will not attempt to
     * match overridden signatures.
     *
     * @param type The type to match the method declaration against.
     * @return A method matcher that is matching the given type.
     */
    public static JunctionMethodMatcher isDeclaredBy(Class<?> type) {
        return isDeclaredBy(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Selects methods that are declared by any subtype of the given type.
     *
     * @param type The type to be matched against all declaring types of a method.
     * @return A method matcher that is matching any methods that are declared by a subtype of the given type.
     */
    public static JunctionMethodMatcher isDeclaredBySubtypeOf(TypeDescription type) {
        return new DeclaringSubTypeMethodMatcher(type);
    }

    /**
     * Selects methods that are declared by any subtype of the given type.
     *
     * @param type The type to be matched against all declaring types of a method.
     * @return A method matcher that is matching any methods that are declared by a subtype of the given type.
     */
    public static JunctionMethodMatcher isDeclaredBySubtypeOf(Class<?> type) {
        return isDeclaredBySubtypeOf(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Selects methods that are declared by any super type of the given type.
     *
     * @param type The type to be matched against all declaring types of a method.
     * @return A method matcher that is matching any methods that are declared by a super type of the given type.
     */
    public static JunctionMethodMatcher isDeclaredBySuperTypeOf(TypeDescription type) {
        return new DeclaringSuperTypeMethodMatcher(type);
    }

    /**
     * Selects methods that are declared by any subtype of the given type.
     *
     * @param type The type to be matched against all declaring types of a method.
     * @return A method matcher that is matching any methods that are declared by a super type of the given type.
     */
    public static JunctionMethodMatcher isDeclaredBySuperTypeOf(Class<?> type) {
        return isDeclaredBySuperTypeOf(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Creates a method matcher that matches any method that is declared by any of the given types.
     *
     * @param typeDescription A list of types to match.
     * @return A method matcher that matches any of the given types.
     */
    public static JunctionMethodMatcher isDeclaredByAny(Class<?>... typeDescription) {
        TypeDescription[] typeDescriptions = new TypeDescription[typeDescription.length];
        int index = 0;
        for (Class<?> type : typeDescription) {
            typeDescriptions[index++] = new TypeDescription.ForLoadedType(type);
        }
        return isDeclaredByAny(typeDescriptions);
    }

    /**
     * Creates a method matcher that matches any method that is declared by any of the given types.
     *
     * @param typeDescription A list of types to match.
     * @return A method matcher that matches any of the given types.
     */
    public static JunctionMethodMatcher isDeclaredByAny(TypeDescription... typeDescription) {
        JunctionMethodMatcher methodMatcher = none();
        for (TypeDescription type : typeDescription) {
            methodMatcher = methodMatcher.or(isDeclaredBy(type));
        }
        return methodMatcher;
    }

    /**
     * Matches methods that represent a Java bean setter, i.e. have signature that resembles
     * {@code void set...(T type)}.
     *
     * @return A matcher that matches any Java bean setter.
     */
    public static JunctionMethodMatcher isSetter() {
        return takesArguments(1).and(nameStartsWith("set")).and(returns(void.class));
    }

    /**
     * Matches methods that represent a Java bean setter, i.e. have signature that resembles
     * {@code void set...(T type)} where {@code T} represents {@code type}.
     *
     * @param type The type of the setter.
     * @return A matcher that matches any Java bean setter.
     */
    public static JunctionMethodMatcher isSetter(Class<?> type) {
        return isSetter(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches methods that represent a Java bean setter, i.e. have signature that resembles
     * {@code void set...(T type)} where {@code T} represents {@code type}.
     *
     * @param type The type of the setter.
     * @return A matcher that matches any Java bean setter.
     */
    public static JunctionMethodMatcher isSetter(TypeDescription type) {
        return isSetter().and(takesArguments(type));
    }

    /**
     * Matches methods that represent a Java bean getter, i.e. have signature that resembles
     * {@code T is...()} or {@code T get...()}.
     *
     * @return A matcher that matches any Java bean getter.
     */
    public static JunctionMethodMatcher isGetter() {
        return takesArguments(0).and(not(returns(void.class))).and(nameStartsWith("get")
                .or(nameStartsWith("is").and(returns(boolean.class).or(returns(Boolean.class)))));
    }

    /**
     * Matches methods that represent a Java bean getter, i.e. have signature that resembles
     * {@code T is...()} or {@code T get...()} where {@code T} represents {@code type}.
     *
     * @param type The type of the getter.
     * @return A matcher that matches any Java bean getter.
     */
    public static JunctionMethodMatcher isGetter(Class<?> type) {
        return isGetter(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Matches methods that represent a Java bean getter, i.e. have signature that resembles
     * {@code T is...()} or {@code T get...()} where {@code T} represents {@code type}.
     *
     * @param type The type of the getter.
     * @return A matcher that matches any Java bean getter.
     */
    public static JunctionMethodMatcher isGetter(TypeDescription type) {
        return isGetter().and(returns(type));
    }

    /**
     * Selects methods with identical signature to the given method description where the return type
     * is considered to be part of the signature.
     *
     * @param method The method to match against.
     * @return A method matcher selecting methods with the same signature as {@code methodDescription}.
     */
    public static JunctionMethodMatcher hasSameByteCodeSignatureAs(Method method) {
        return hasSameByteCodeSignatureAs(new MethodDescription.ForLoadedMethod(method));
    }

    /**
     * Selects methods with identical signature to the given method description where the return type
     * is considered to be part of the signature.
     *
     * @param constructor The constructor to match against.
     * @return A method matcher selecting methods with the same signature as {@code methodDescription}.
     */
    public static JunctionMethodMatcher hasSameByteCodeSignatureAs(Constructor<?> constructor) {
        return hasSameByteCodeSignatureAs(new MethodDescription.ForLoadedConstructor(constructor));
    }

    /**
     * Selects methods with identical signature to the given method description where the return type
     * is considered to be part of the signature.
     *
     * @param methodDescription The method to match against.
     * @return A method matcher selecting methods with the same signature as {@code methodDescription}.
     */
    public static JunctionMethodMatcher hasSameByteCodeSignatureAs(MethodDescription methodDescription) {
        return new MethodByteCodeSignatureMethodMatcher(methodDescription);
    }

    /**
     * Checks if a method has a Java compiler equal signature to another method which includes the name of the method
     * and the exact types and order of its parameters. The return type is not considered for equality.
     *
     * @param method The method to be matched against.
     * @return A matcher that selects methods with a compatible Java compiler signature.
     */
    public static JunctionMethodMatcher hasSameJavaCompilerSignatureAs(Method method) {
        return hasSameJavaCompilerSignatureAs(new MethodDescription.ForLoadedMethod(method));
    }

    /**
     * Checks if a method has a Java compiler equal signature to another method which includes the name of the method
     * and the exact types and order of its parameters. The return type is not considered for equality.
     *
     * @param constructor The constructor to be matched against.
     * @return A matcher that selects methods with a compatible Java compiler signature.
     */
    public static JunctionMethodMatcher hasSameJavaCompilerSignatureAs(Constructor<?> constructor) {
        return hasSameJavaCompilerSignatureAs(new MethodDescription.ForLoadedConstructor(constructor));
    }

    /**
     * Checks if a method has a Java compiler equal signature to another method which includes the name of the method
     * and the exact types and order of its parameters. The return type is not considered for equality.
     *
     * @param methodDescription A description of the method signature to be matched against.
     * @return A matcher that selects methods with a compatible Java compiler signature.
     */
    public static JunctionMethodMatcher hasSameJavaCompilerSignatureAs(MethodDescription methodDescription) {
        return (methodDescription.isConstructor() ? isConstructor() : named(methodDescription.getName()))
                .and(takesArguments(methodDescription.getParameterTypes()));
    }

    /**
     * Determines if a method could be the target of the given method for a compile time bridge.
     *
     * @param method The bridge method to match against.
     * @return A method matcher that determines if a method could be a bridge target for the given method.
     */
    public static JunctionMethodMatcher isBridgeMethodCompatibleTo(Method method) {
        return isBridgeMethodCompatibleTo(new MethodDescription.ForLoadedMethod(method));
    }

    /**
     * Checks if a method is annotated by a given parameter.
     *
     * @param annotationType The annotation type of interest.
     * @return A method matcher that matches methods that carry the given annotation.
     */
    public static JunctionMethodMatcher isAnnotatedBy(Class<? extends Annotation> annotationType) {
        return new AnnotationMethodMatcher(annotationType);
    }

    /**
     * Determines if a method could be the target of the given method for a compile time bridge.
     *
     * @param methodDescription A description of the bridge method to match against.
     * @return A method matcher that determines if a method could be a bridge target for the given method.
     */
    public static JunctionMethodMatcher isBridgeMethodCompatibleTo(MethodDescription methodDescription) {
        return (methodDescription.isConstructor() ? isConstructor() : named(methodDescription.getName()))
                .and(returnsSubtypeOf(methodDescription.getReturnType()))
                .and(takesArgumentsAsSubtypesOf(methodDescription.getParameterTypes()));
    }

    /**
     * Matches methods that represent a so-called visibility bridge.
     *
     * @return A matcher for identifying visibility bridges.
     */
    public static JunctionMethodMatcher isVisibilityBridge() {
        return new VisibilityBridgeMethodMatcher();
    }

    /**
     * Selects methods that are overridable, i.e. not constructors, final, private or static or is defined
     * in a final type.
     *
     * @return A new method matcher that matches overridable methods.
     */
    public static JunctionMethodMatcher isOverridable() {
        return new OverridableMethodMatcher();
    }

    /**
     * Only matches the default finalizer method as declared in {@link Object#finalize()} but not methods that
     * override this method.
     *
     * @return A new method matcher that matches the default finalizer.
     */
    public static JunctionMethodMatcher isDefaultFinalizer() {
        return new DefaultFinalizeMethodMatcher();
    }

    /**
     * Matches a method for being a finalizer method which is declared by any class.
     *
     * @return A matcher that selects any finalizer method.
     */
    public static JunctionMethodMatcher isFinalizer() {
        return named("finalize").and(takesArguments(0)).and(returns(void.class));
    }

    /**
     * Matches the {@link Object#hashCode()} method, independently of the method being overridden.
     *
     * @return A matcher for matching the hash code method.
     */
    public static JunctionMethodMatcher isHashCode() {
        return named("hashCode").and(takesArguments(0)).and(returns(int.class));
    }

    /**
     * Matches the {@link Object#equals(Object)}} method, independently of the method being overridden.
     *
     * @return A matcher for matching the equals method.
     */
    public static JunctionMethodMatcher isEquals() {
        return named("equals").and(takesArguments(Object.class)).and(returns(boolean.class));
    }

    /**
     * Inverts another method matcher.
     *
     * @param methodMatcher The method matcher to be inverted.
     * @return A method matcher that returns {@code true} if the {@code methodMatcher} returns {@code false}.
     */
    public static JunctionMethodMatcher not(MethodMatcher methodMatcher) {
        return new NegatingMethodMatcher(methodMatcher);
    }

    /**
     * Returns a method matcher that matches any method.
     *
     * @return A method matcher that always returns {@code true}.
     */
    public static JunctionMethodMatcher any() {
        return new BooleanMethodMatcher(true);
    }

    /**
     * Returns a method matcher that matches no method.
     *
     * @return A method matcher that always returns {@code false}.
     */
    public static JunctionMethodMatcher none() {
        return new BooleanMethodMatcher(false);
    }

    /**
     * Each match mode represents a way of comparing two strings to another.
     */
    private static enum MatchMode {

        /**
         * Checks if two strings equal and respects casing differences.
         */
        EQUALS_FULLY("named") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.equals(comparisonTarget);
            }
        },

        /**
         * Checks if two strings equal without respecting casing differences.
         */
        EQUALS_FULLY_IGNORE_CASE("namedIgnoreCase") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.equalsIgnoreCase(comparisonTarget);
            }
        },

        /**
         * Checks if a string starts with the a second string with respecting casing differences.
         */
        STARTS_WITH("startsWith") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.startsWith(comparisonTarget);
            }
        },

        /**
         * Checks if a string starts with a second string without respecting casing differences.
         */
        STARTS_WITH_IGNORE_CASE("startsWithIgnoreCase") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.toLowerCase().startsWith(comparisonTarget.toLowerCase());
            }
        },

        /**
         * Checks if a string ends with a second string with respecting casing differences.
         */
        ENDS_WITH("endsWith") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.endsWith(comparisonTarget);
            }
        },

        /**
         * Checks if a string ends with a second string without respecting casing differences.
         */
        ENDS_WITH_IGNORE_CASE("endsWithIgnoreCase") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.toLowerCase().endsWith(comparisonTarget.toLowerCase());
            }
        },

        /**
         * Checks if a string contains another string with respecting casing differences.
         */
        CONTAINS("contains") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.contains(comparisonTarget);
            }
        },

        /**
         * Checks if a string contains another string without respecting casing differences.
         */
        CONTAINS_IGNORE_CASE("containsIgnoreCase") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.toLowerCase().contains(comparisonTarget.toLowerCase());
            }
        },

        /**
         * Checks if a string can be matched by a regular expression.
         */
        MATCHES("matches") {
            @Override
            protected boolean matches(String comparisonTarget, String comparisonSource) {
                return comparisonSource.matches(comparisonTarget);
            }
        };

        /**
         * A description of the string for providing meaningful {@link Object#toString()} implementations for
         * method matchers that rely on a match mode.
         */
        private final String description;

        /**
         * Creates a new match mode.
         *
         * @param description The description of this mode for providing meaningful {@link Object#toString()}
         *                    implementations.
         */
        private MatchMode(String description) {
            this.description = description;
        }

        /**
         * Returns the description of this match mode.
         *
         * @return The description of this match mode.
         */
        private String getDescription() {
            return description;
        }

        /**
         * Matches a string against another string.
         *
         * @param comparisonTarget The target of the comparison against which the source string is compared.
         * @param comparisonSource The source which is subject of the comparison to another string.
         * @return {@code true} if the source matches the target.
         */
        protected abstract boolean matches(String comparisonTarget, String comparisonSource);
    }

    /**
     * Matches a method by its name. Constructors are never matched.
     */
    private static class MethodNameMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The name to match the method's name against.
         */
        private final String name;

        /**
         * The mode of matching the given name.
         */
        private final MatchMode matchMode;

        /**
         * Creates a new method name matcher.
         *
         * @param name      The name to be matched.
         * @param matchMode The mode of matching the given name.
         */
        public MethodNameMethodMatcher(String name, MatchMode matchMode) {
            this.name = name;
            this.matchMode = matchMode;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return !methodDescription.isConstructor() && matchMode.matches(name, methodDescription.getName());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && matchMode == ((MethodNameMethodMatcher) other).matchMode
                    && name.equals(((MethodNameMethodMatcher) other).name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + matchMode.hashCode();
        }

        @Override
        public String toString() {
            return matchMode.getDescription() + '(' + name + ')';
        }
    }

    /**
     * Matches a method by a modifier. The method is matched if any bit of the modifier is set on a method.
     */
    private static class ModifierMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The modifiers to match.
         */
        private final int modifiers;

        /**
         * Creates a new modifier method matcher.
         *
         * @param modifiers The modifiers to match.
         */
        public ModifierMethodMatcher(int modifiers) {
            this.modifiers = modifiers;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return (methodDescription.getModifiers() & modifiers) != 0;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && modifiers == ((ModifierMethodMatcher) other).modifiers;
        }

        @Override
        public String toString() {
            return "modifiers(" + modifiers + ')';
        }

        @Override
        public int hashCode() {
            return modifiers;
        }
    }

    /**
     * Matches a method by its exact parameter types.
     */
    private static class ParameterTypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The parameter types to match by their exact types.
         */
        private final List<? extends TypeDescription> parameterTypes;

        /**
         * Creates a new parameter type matcher.
         *
         * @param parameterTypes The parameter types to match by their exact types.
         */
        private ParameterTypeMethodMatcher(List<? extends TypeDescription> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getParameterTypes().equals(parameterTypes);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && parameterTypes.equals(((ParameterTypeMethodMatcher) other).parameterTypes);
        }

        @Override
        public int hashCode() {
            return parameterTypes.hashCode();
        }

        @Override
        public String toString() {
            return "parameters(" + parameterTypes + ')';
        }
    }

    /**
     * Matches a method by its parameter's subtypes.
     */
    private static class ParameterSubtypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The parameter types to match by their subtypes.
         */
        private final List<? extends TypeDescription> parameterTypes;

        /**
         * Creates a new parameter subtype matcher.
         *
         * @param parameterTypes The parameter types to match by their subtypes.
         */
        private ParameterSubtypeMethodMatcher(List<? extends TypeDescription> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            TypeList parameterTypes = methodDescription.getParameterTypes();
            if (parameterTypes.size() != this.parameterTypes.size()) {
                return false;
            }
            int currentIndex = 0;
            for (TypeDescription parameterType : parameterTypes) {
                if (!parameterType.isAssignableTo(this.parameterTypes.get(currentIndex++))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && parameterTypes.equals(((ParameterSubtypeMethodMatcher) other).parameterTypes);
        }

        @Override
        public int hashCode() {
            return parameterTypes.hashCode();
        }

        @Override
        public String toString() {
            return "parametersAssignableTo(" + parameterTypes + ')';
        }
    }

    /**
     * Matches a method by its parameter's super types.
     */
    private static class ParameterSuperTypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The parameter types to match by their super types.
         */
        private final List<? extends TypeDescription> parameterTypes;

        /**
         * Creates a new parameter super type matcher.
         *
         * @param parameterTypes The parameter types to match by their super types.
         */
        private ParameterSuperTypeMethodMatcher(List<? extends TypeDescription> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            TypeList parameterTypes = methodDescription.getParameterTypes();
            if (parameterTypes.size() != this.parameterTypes.size()) {
                return false;
            }
            int currentIndex = 0;
            for (TypeDescription parameterType : parameterTypes) {
                if (!parameterType.isAssignableFrom(this.parameterTypes.get(currentIndex++))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && parameterTypes.equals(((ParameterSuperTypeMethodMatcher) other).parameterTypes);
        }

        @Override
        public int hashCode() {
            return parameterTypes.hashCode();
        }

        @Override
        public String toString() {
            return "parametersAssignableFrom(" + parameterTypes + ')';
        }
    }

    /**
     * Matches a method by its parameter count.
     */
    private static class ParameterCountMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The expected number of parameters to positively match a method.
         */
        private final int numberOfParameters;

        /**
         * Creates a new parameter count matcher.
         *
         * @param numberOfParameters The expected number of parameters to positively match a method.
         */
        private ParameterCountMethodMatcher(int numberOfParameters) {
            this.numberOfParameters = numberOfParameters;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getParameterTypes().size() == numberOfParameters;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && numberOfParameters == ((ParameterCountMethodMatcher) other).numberOfParameters;
        }

        @Override
        public int hashCode() {
            return numberOfParameters;
        }

        @Override
        public String toString() {
            return "parameterCount(" + numberOfParameters + ')';
        }
    }

    /**
     * Matches a method by its return type.
     */
    private static class ReturnTypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The return type to match exactly.
         */
        private final TypeDescription returnType;

        /**
         * Creates a new return type matcher.
         *
         * @param returnType The return type to match exactly.
         */
        public ReturnTypeMethodMatcher(TypeDescription returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getReturnType().equals(returnType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && returnType.equals(((ReturnTypeMethodMatcher) other).returnType);
        }

        @Override
        public int hashCode() {
            return returnType.hashCode();
        }

        @Override
        public String toString() {
            return "returns(" + returnType + ')';
        }
    }

    /**
     * Matches a method by its return type's subtype.
     */
    private static class ReturnSubtypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The return type to match by its subtype.
         */
        private final TypeDescription returnType;

        /**
         * Creates a new return subtype matcher.
         *
         * @param returnType The return type to match by its subtype.
         */
        public ReturnSubtypeMethodMatcher(TypeDescription returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getReturnType().isAssignableTo(returnType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && returnType.equals(((ReturnSubtypeMethodMatcher) other).returnType);
        }

        @Override
        public int hashCode() {
            return returnType.hashCode();
        }

        @Override
        public String toString() {
            return "returnsSubtypeOf(" + returnType + ')';
        }
    }

    /**
     * Matches a method by its return type's super type.
     */
    private static class ReturnSupertypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The return type to match by its subtype.
         */
        private final TypeDescription returnType;

        /**
         * Creates a new return subtype matcher.
         *
         * @param returnType The return type to match by its super type.
         */
        public ReturnSupertypeMethodMatcher(TypeDescription returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getReturnType().isAssignableFrom(returnType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && returnType.equals(((ReturnSupertypeMethodMatcher) other).returnType);
        }

        @Override
        public int hashCode() {
            return returnType.hashCode();
        }

        @Override
        public String toString() {
            return "returnsSupertypeOf(" + returnType + ')';
        }
    }

    /**
     * Matches a method by its declared and implicit exceptions.
     */
    private static class DeclaredExceptionMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The exception type to check for being throwable from a method.
         */
        private final TypeDescription exceptionType;

        /**
         * Creates a new exception method matcher.
         *
         * @param exceptionType The exception type to check for being throwable from a method.
         */
        public DeclaredExceptionMethodMatcher(TypeDescription exceptionType) {
            this.exceptionType = exceptionType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            for (TypeDescription exceptionType : methodDescription.getExceptionTypes()) {
                if (exceptionType.isAssignableFrom(this.exceptionType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && exceptionType.equals(((DeclaredExceptionMethodMatcher) other).exceptionType);
        }

        @Override
        public int hashCode() {
            return exceptionType.hashCode();
        }

        @Override
        public String toString() {
            return "canThrow(" + exceptionType + ')';
        }
    }

    /**
     * Matches a method by its exact method description.
     */
    private static class MethodDescriptionMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The method description to match exactly.
         */
        private final MethodDescription methodDescription;

        /**
         * Creates a new method description method matcher.
         *
         * @param methodDescription The method description to match exactly.
         */
        private MethodDescriptionMethodMatcher(MethodDescription methodDescription) {
            this.methodDescription = methodDescription;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.equals(this.methodDescription);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodDescription.equals(((MethodDescriptionMethodMatcher) other).methodDescription);
        }

        @Override
        public int hashCode() {
            return methodDescription.hashCode();
        }

        @Override
        public String toString() {
            return "is(" + methodDescription + ')';
        }
    }

    /**
     * Matches a method by it representing a loaded {@link java.lang.reflect.Method}.
     */
    private static class LoadedMethodEqualityMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The loaded method to check if it is represented by a matched method.
         */
        private final Method method;

        /**
         * Creates a new method equality method matcher.
         *
         * @param method The loaded method to check if it is represented by a matched method.
         */
        public LoadedMethodEqualityMethodMatcher(Method method) {
            this.method = method;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.represents(method);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && method.equals(((LoadedMethodEqualityMethodMatcher) other).method);
        }

        @Override
        public int hashCode() {
            return method.hashCode();
        }

        @Override
        public String toString() {
            return "is(" + method + ')';
        }
    }

    /**
     * Matches a method by it representing a loaded {@link java.lang.reflect.Constructor}.
     */
    private static class LoadedConstructorEqualityMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The loaded constructor to check if it is represented by a matched method.
         */
        private final Constructor<?> constructor;

        /**
         * Creates a new constructor equality method matcher.
         *
         * @param constructor The loaded constructor to check if it is represented by a matched method.
         */
        public LoadedConstructorEqualityMethodMatcher(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.represents(constructor);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && constructor.equals(((LoadedConstructorEqualityMethodMatcher) other).constructor);
        }

        @Override
        public int hashCode() {
            return constructor.hashCode();
        }

        @Override
        public String toString() {
            return "is(" + constructor + ')';
        }
    }

    /**
     * Matches a method by its exact byte code method signature.
     */
    private static class MethodByteCodeSignatureMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The method whose exact signature is to be matched.
         */
        private final MethodDescription methodDescription;

        /**
         * Creates a new method signature method matcher.
         *
         * @param methodDescription The method whose exact signature is to be matched.
         */
        private MethodByteCodeSignatureMethodMatcher(MethodDescription methodDescription) {
            this.methodDescription = methodDescription;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getInternalName().equals(this.methodDescription.getInternalName())
                    && methodDescription.getReturnType().equals(this.methodDescription.getReturnType())
                    && methodDescription.getParameterTypes().equals(this.methodDescription.getParameterTypes());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodDescription.equals(((MethodByteCodeSignatureMethodMatcher) other).methodDescription);
        }

        @Override
        public int hashCode() {
            return methodDescription.hashCode();
        }

        @Override
        public String toString() {
            return "signatureOf(" + methodDescription + ')';
        }
    }

    /**
     * Matches a method by representing an actual method instead of a constructor.
     */
    private static class IsMethodMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return !methodDescription.isConstructor();
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof IsMethodMethodMatcher;
        }

        @Override
        public int hashCode() {
            return 47;
        }

        @Override
        public String toString() {
            return "isMethod()";
        }
    }

    /**
     * Matches a method by being overridable.
     */
    private static class OverridableMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isOverridable();
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof OverridableMethodMatcher;
        }

        @Override
        public String toString() {
            return "isOverridable()";
        }

        @Override
        public int hashCode() {
            return 51;
        }
    }

    /**
     * Matches a method by being the default finalizer which is declared by {@link Object#finalize()}.
     */
    private static class DefaultFinalizeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The name of the finalize method.
         */
        private static final String FINALIZE_METHOD_NAME = "finalize";

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getDeclaringType().represents(Object.class)
                    && methodDescription.getName().equals(FINALIZE_METHOD_NAME)
                    && methodDescription.getParameterTypes().size() == 0;
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof DefaultFinalizeMethodMatcher;
        }

        @Override
        public String toString() {
            return "isDefaultFinalizer()";
        }

        @Override
        public int hashCode() {
            return 54;
        }
    }

    /**
     * Matches a method by being visible to another type.
     */
    private static class VisibilityMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The type for which the visibility is to be checked.
         */
        private final TypeDescription typeDescription;

        /**
         * Creates a new visibility method matcher.
         *
         * @param typeDescription The type for which the visibility is to be checked.
         */
        private VisibilityMethodMatcher(TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isVisibleTo(typeDescription);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && typeDescription.equals(((VisibilityMethodMatcher) other).typeDescription);
        }

        @Override
        public int hashCode() {
            return typeDescription.hashCode();
        }

        @Override
        public String toString() {
            return "visibleTo(" + typeDescription.getName() + ')';
        }
    }

    /**
     * Matches a method by its exact declaring type.
     */
    private static class DeclaringTypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The type to match exactly against a method's declaring type.
         */
        private final TypeDescription declaringType;

        /**
         * Creates a new declaring type method matcher.
         *
         * @param declaringType The type to match exactly against a method's declaring type.
         */
        private DeclaringTypeMethodMatcher(TypeDescription declaringType) {
            this.declaringType = declaringType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getDeclaringType().equals(declaringType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && declaringType.equals(((DeclaringTypeMethodMatcher) other).declaringType);
        }

        @Override
        public int hashCode() {
            return declaringType.hashCode();
        }

        @Override
        public String toString() {
            return "declaredBy(" + declaringType.getName() + ')';
        }
    }

    /**
     * Matches a method by its declaring type's subtype.
     */
    private static class DeclaringSubTypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The type to match to be a subtype of a method's declaring type.
         */
        private final TypeDescription declaringType;

        /**
         * Creates a new declaring subtype method matcher.
         *
         * @param declaringType The type to match to be a subtype of a method's declaring type.
         */
        private DeclaringSubTypeMethodMatcher(TypeDescription declaringType) {
            this.declaringType = declaringType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getDeclaringType().isAssignableTo(declaringType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && declaringType.equals(((DeclaringSubTypeMethodMatcher) other).declaringType);
        }

        @Override
        public int hashCode() {
            return declaringType.hashCode();
        }

        @Override
        public String toString() {
            return "declaredBySubtypeOf(" + declaringType.getName() + ')';
        }
    }

    /**
     * Matches a method by its declaring type's super type.
     */
    private static class DeclaringSuperTypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The type to match to be a super type of a method's declaring type.
         */
        private final TypeDescription declaringType;

        /**
         * Creates a new declaring super type method matcher.
         *
         * @param declaringType The type to match to be a super type of a method's declaring type.
         */
        private DeclaringSuperTypeMethodMatcher(TypeDescription declaringType) {
            this.declaringType = declaringType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getDeclaringType().isAssignableFrom(declaringType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && declaringType.equals(((DeclaringSuperTypeMethodMatcher) other).declaringType);
        }

        @Override
        public int hashCode() {
            return declaringType.hashCode();
        }

        @Override
        public String toString() {
            return "declaredBySuperTypeOf(" + declaringType.getName() + ')';
        }
    }

    /**
     * Matches a method by its annotations.
     */
    private static class AnnotationMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The annotation type to match to be present on a method.
         */
        private final Class<? extends Annotation> annotationType;

        /**
         * Creates a new annotation method matcher.
         *
         * @param annotationType The annotation type to match to be present on a method.
         */
        private AnnotationMethodMatcher(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isAnnotationPresent(annotationType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotationType.equals(((AnnotationMethodMatcher) other).annotationType);
        }

        @Override
        public int hashCode() {
            return annotationType.hashCode();
        }

        @Override
        public String toString() {
            return "isAnnotatedBy(" + annotationType.getName() + ')';
        }
    }

    /**
     * Matches a method by a boolean property.
     */
    private static class BooleanMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The result of any attempt to match a method.
         */
        private final boolean matches;

        /**
         * Creates a new boolean method matcher.
         *
         * @param matches The result of any attempt to match a method.
         */
        private BooleanMethodMatcher(boolean matches) {
            this.matches = matches;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return matches;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && matches == ((BooleanMethodMatcher) other).matches;
        }

        @Override
        public int hashCode() {
            return (matches ? 1 : 0);
        }

        @Override
        public String toString() {
            return Boolean.toString(matches);
        }
    }

    /**
     * Matches a method by negating another method matcher.
     */
    private static class NegatingMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        /**
         * The method matcher to negate.
         */
        private final MethodMatcher methodMatcher;

        /**
         * Creates a new negating method matcher.
         *
         * @param methodMatcher The method matcher to negate.
         */
        public NegatingMethodMatcher(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return !methodMatcher.matches(methodDescription);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodMatcher.equals(((NegatingMethodMatcher) other).methodMatcher);
        }

        @Override
        public int hashCode() {
            return -1 * methodMatcher.hashCode();
        }

        @Override
        public String toString() {
            return "not(" + methodMatcher + ')';
        }
    }

    /**
     * Matches a method by representing a visibility bridge, i.e. a Java bridge method that is only
     * introduced in order to increase the visibility of a super type's method.
     */
    private static class VisibilityBridgeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isBridge()
                    && methodDescription.getDeclaringType()
                    .getDeclaredMethods()
                    .filter(isMethod()
                            .and(not(is(methodDescription)))
                            .and(isBridgeMethodCompatibleTo(methodDescription)))
                    .size() == 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 56;
        }

        @Override
        public String toString() {
            return "isVisibilityBridge()";
        }
    }
}

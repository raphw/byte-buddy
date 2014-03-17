package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * A collection of common {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher}
 * implementations.
 * <p/>
 * This class is not meant to be instantiated but is usually imported statically in order to allow for improving
 * the readability of code.
 */
public final class MethodMatchers {

    private static enum MatchMode {

        EQUALS_FULLY,
        EQUALS_FULLY_IGNORE_CASE,
        STARTS_WITH,
        STARTS_WITH_IGNORE_CASE,
        ENDS_WITH,
        ENDS_WITH_IGNORE_CASE,
        CONTAINS,
        CONTAINS_IGNORE_CASE,
        MATCHES;

        private boolean matches(String left, String right) {
            switch (this) {
                case EQUALS_FULLY:
                    return right.equals(left);
                case EQUALS_FULLY_IGNORE_CASE:
                    return right.equalsIgnoreCase(left);
                case STARTS_WITH:
                    return right.startsWith(left);
                case STARTS_WITH_IGNORE_CASE:
                    return right.toLowerCase().startsWith(left.toLowerCase());
                case ENDS_WITH:
                    return right.endsWith(left);
                case ENDS_WITH_IGNORE_CASE:
                    return right.toLowerCase().endsWith(left.toLowerCase());
                case CONTAINS:
                    return right.contains(left);
                case CONTAINS_IGNORE_CASE:
                    return right.toLowerCase().contains(left.toLowerCase());
                case MATCHES:
                    return right.matches(left);
                default:
                    throw new AssertionError("Unknown match mode: " + this);
            }
        }
    }

    private static class MethodNameMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final String methodName;
        private final MatchMode matchMode;

        public MethodNameMethodMatcher(String methodName, MatchMode matchMode) {
            this.methodName = methodName;
            this.matchMode = matchMode;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return matchMode.matches(methodName, methodDescription.getName());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && matchMode == ((MethodNameMethodMatcher) other).matchMode
                    && methodName.equals(((MethodNameMethodMatcher) other).methodName);
        }

        @Override
        public int hashCode() {
            return 31 * methodName.hashCode() + matchMode.hashCode();
        }

        @Override
        public String toString() {
            return "MethodNameMethodMatcher{methodName='" + methodName + "\', matchMode=" + matchMode + '}';
        }
    }

    private static class ModifierMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final int modifiers;

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
            return "ModifierMethodMatcher{modifiers=" + modifiers + '}';
        }

        @Override
        public int hashCode() {
            return modifiers;
        }
    }

    private static class ParameterTypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeList parameterType;

        private ParameterTypeMatcher(Class<?>[] parameterType) {
            this.parameterType = new TypeList.ForLoadedType(parameterType);
        }

        private ParameterTypeMatcher(List<? extends TypeDescription> parameterTypes) {
            parameterType = new TypeList.Explicit(parameterTypes);
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getParameterTypes().equals(parameterType);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && parameterType.equals(((ParameterTypeMatcher) o).parameterType);
        }

        @Override
        public int hashCode() {
            return parameterType.hashCode();
        }

        @Override
        public String toString() {
            return "ParameterTypeMatcher{parameterType=" + parameterType + '}';
        }
    }

    private static class ParameterCountMatcher extends JunctionMethodMatcher.AbstractBase {

        private final int numberOfParameters;

        private ParameterCountMatcher(int numberOfParameters) {
            this.numberOfParameters = numberOfParameters;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getParameterTypes().size() == numberOfParameters;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && numberOfParameters == ((ParameterCountMatcher) other).numberOfParameters;
        }

        @Override
        public int hashCode() {
            return numberOfParameters;
        }

        @Override
        public String toString() {
            return "ParameterCountMatcher{numberOfParameters=" + numberOfParameters + '}';
        }
    }

    private static class ReturnTypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeDescription returnType;

        public ReturnTypeMatcher(Class<?> returnType) {
            this.returnType = new TypeDescription.ForLoadedType(returnType);
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getReturnType().equals(returnType);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && returnType.equals(((ReturnTypeMatcher) o).returnType);
        }

        @Override
        public int hashCode() {
            return returnType.hashCode();
        }

        @Override
        public String toString() {
            return "ReturnTypeMatcher{returnType=" + returnType + '}';
        }
    }

    private static class ExceptionMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeDescription exceptionType;

        public ExceptionMethodMatcher(Class<?> exceptionType) {
            this.exceptionType = new TypeDescription.ForLoadedType(exceptionType);
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
                    && exceptionType.equals(((ExceptionMethodMatcher) other).exceptionType);
        }

        @Override
        public int hashCode() {
            return exceptionType.hashCode();
        }

        @Override
        public String toString() {
            return "ExceptionMethodMatcher{exceptionType=" + exceptionType + '}';
        }
    }

    private static class MethodDescriptionMatcher extends JunctionMethodMatcher.AbstractBase {

        private final MethodDescription methodDescription;

        private MethodDescriptionMatcher(MethodDescription methodDescription) {
            this.methodDescription = methodDescription;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.equals(this.methodDescription);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodDescription.equals(((MethodDescriptionMatcher) other).methodDescription);
        }

        @Override
        public int hashCode() {
            return methodDescription.hashCode();
        }

        @Override
        public String toString() {
            return "MethodDescriptionMatcher{methodDescription=" + methodDescription + '}';
        }
    }

    private static class MethodEqualityMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final Method method;

        public MethodEqualityMethodMatcher(Method method) {
            this.method = method;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.represents(method);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && method.equals(((MethodEqualityMethodMatcher) other).method);
        }

        @Override
        public int hashCode() {
            return method.hashCode();
        }

        @Override
        public String toString() {
            return "MethodEqualityMethodMatcher{method=" + method + '}';
        }
    }

    private static class ConstructorEqualityMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final Constructor<?> constructor;

        public ConstructorEqualityMethodMatcher(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.represents(constructor);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && constructor.equals(((ConstructorEqualityMethodMatcher) other).constructor);
        }

        @Override
        public int hashCode() {
            return constructor.hashCode();
        }

        @Override
        public String toString() {
            return "ConstructorEqualityMethodMatcher{constructor=" + constructor + '}';
        }
    }

    private static class MethodSignatureMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final MethodDescription methodDescription;

        private MethodSignatureMethodMatcher(MethodDescription methodDescription) {
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
                    && methodDescription.equals(((MethodSignatureMethodMatcher) other).methodDescription);
        }

        @Override
        public int hashCode() {
            return methodDescription.hashCode();
        }

        @Override
        public String toString() {
            return "MethodSignatureMethodMatcher{methodDescription=" + methodDescription + '}';
        }
    }

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
        public String toString() {
            return "IsMethodMethodMatcher";
        }

        @Override
        public int hashCode() {
            return 47;
        }
    }

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
            return "OverridableMethodMatcher";
        }

        @Override
        public int hashCode() {
            return 51;
        }
    }

    private static class DefaultFinalizeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

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
            return "DefaultFinalizeMethodMatcher";
        }

        @Override
        public int hashCode() {
            return 54;
        }
    }

    private static class PackageNameMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final String packageName;

        private PackageNameMethodMatcher(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getDeclaringType().getPackageName().equals(packageName);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && packageName.equals(((PackageNameMethodMatcher) o).packageName);
        }

        @Override
        public int hashCode() {
            return packageName.hashCode();
        }

        @Override
        public String toString() {
            return "PackageNameMethodMatcher{packageName='" + packageName + '\'' + '}';
        }
    }

    private static class DeclaringTypeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeDescription declaringType;

        private DeclaringTypeMethodMatcher(Class<?> declaringType) {
            this.declaringType = new TypeDescription.ForLoadedType(declaringType);
        }

        private DeclaringTypeMethodMatcher(TypeDescription declaringType) {
            this.declaringType = declaringType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getDeclaringType().equals(declaringType);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && declaringType.equals(((DeclaringTypeMethodMatcher) o).declaringType);
        }

        @Override
        public int hashCode() {
            return declaringType.hashCode();
        }

        @Override
        public String toString() {
            return "DeclaringTypeMethodMatcher{declaringType=" + declaringType + '}';
        }
    }

    private static class BooleanMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final boolean matches;

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
            return "BooleanMethodMatcher{nameMatches=" + matches + '}';
        }
    }

    private static class NegatingMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final MethodMatcher methodMatcher;

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
            return methodMatcher.hashCode();
        }

        @Override
        public String toString() {
            return "NegatingMethodMatcher{methodMatcher=" + methodMatcher + '}';
        }
    }

    /**
     * Selects a method by its exact internalName. For example, {@code internalName("foo")} will match {@code foo} but not
     * {@code bar} or {@code FOO}.
     *
     * @param name The internalName to be matched.
     * @return A method matcher for the specified internalName.
     */
    public static JunctionMethodMatcher named(String name) {
        return new MethodNameMethodMatcher(name, MatchMode.EQUALS_FULLY);
    }

    /**
     * Selects a method by its case insensitive, exact internalName. For example, {@code namedIgnoreCase("foo")} will match
     * {@code foo} and {@code FOO} but not {@code bar}.
     *
     * @param name The internalName to be matched.
     * @return A method matcher for the specified internalName.
     */
    public static JunctionMethodMatcher namedIgnoreCase(String name) {
        return new MethodNameMethodMatcher(name, MatchMode.EQUALS_FULLY_IGNORE_CASE);
    }

    /**
     * Selects a method by its exact internalName prefix. For example, {@code nameStartsWith("foo")} will match
     * {@code foo} and {@code foobar} but not {@code bar} and {@code FOO}.
     *
     * @param prefix The internalName prefix to be matched.
     * @return A method matcher for the specified internalName.
     */
    public static JunctionMethodMatcher nameStartsWith(String prefix) {
        return new MethodNameMethodMatcher(prefix, MatchMode.STARTS_WITH);
    }

    /**
     * Selects a method by its case insensitive exact internalName prefix. For example, {@code nameStartsWithIgnoreCase("foo")}
     * will match {@code foo}, {@code foobar} and {@code FOO} but not {@code bar}.
     *
     * @param prefix The internalName prefix to be matched.
     * @return A method matcher for the specified internalName.
     */
    public static JunctionMethodMatcher nameStartsWithIgnoreCase(String prefix) {
        return new MethodNameMethodMatcher(prefix, MatchMode.STARTS_WITH_IGNORE_CASE);
    }

    /**
     * Selects a method by its exact internalName suffix. For example, {@code nameEndsWith("bar")} will match {@code bar} and
     * {@code foobar} but not {@code BAR} and {@code foo}.
     *
     * @param suffix The internalName suffix to be matched.
     * @return A method matcher for the specified internalName.
     */
    public static JunctionMethodMatcher nameEndsWith(String suffix) {
        return new MethodNameMethodMatcher(suffix, MatchMode.ENDS_WITH);
    }

    /**
     * Selects a method by its case insensitive exact internalName suffix. For example, {@code nameEndsWithIgnoreCase("bar")}
     * will match {@code bar}, {@code foobar} and {@code BAR} but not {@code foo}.
     *
     * @param suffix The internalName suffix to be matched.
     * @return A method matcher for the specified internalName.
     */
    public static JunctionMethodMatcher nameEndsWithIgnoreCase(String suffix) {
        return new MethodNameMethodMatcher(suffix, MatchMode.ENDS_WITH_IGNORE_CASE);
    }

    /**
     * Selects a method by its exact internalName infix. For example, {@code nameContains("a")} will
     * match {@code bar}, {@code foobar} and {@code BaR} but not {@code foo} and {@code BAR}.
     *
     * @param infix The internalName infix to be matched.
     * @return A method matcher for the specified internalName.
     */
    public static JunctionMethodMatcher nameContains(String infix) {
        return new MethodNameMethodMatcher(infix, MatchMode.CONTAINS);
    }

    /**
     * Selects a method by its case insensitive exact internalName infix. For example, {@code nameContainsIgnoreCase("a")}
     * will match {@code bar}, {@code foobar}, {@code BAR} and {@code BAR} but not {@code foo}.
     *
     * @param infix The internalName infix to be matched.
     * @return A method matcher for the specified internalName.
     */
    public static JunctionMethodMatcher nameContainsIgnoreCase(String infix) {
        return new MethodNameMethodMatcher(infix, MatchMode.CONTAINS_IGNORE_CASE);
    }

    /**
     * Selects a method by its internalName matching a regular expression. For example, {@code matches("f(o){2}.*")} will
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
     * Selects a method when it is marked as a bridge method
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isBridge() {
        return new ModifierMethodMatcher(Opcodes.ACC_BRIDGE);
    }

    /**
     * Selects a method by its return type.
     *
     * @param type The return type of the method.
     * @return A new method matcher that selects methods returning {@code type}.
     */
    public static JunctionMethodMatcher returns(Class<?> type) {
        return new ReturnTypeMatcher(type);
    }

    /**
     * Selects a method by its parameter types in their exact order.
     *
     * @param types The parameter types of the method.
     * @return A new method matcher that selects methods with the exact parameters contained by {@code types}.
     */
    public static JunctionMethodMatcher takesArguments(Class<?>... types) {
        return new ParameterTypeMatcher(types);
    }

    public static JunctionMethodMatcher takesArguments(TypeDescription... types) {
        return new ParameterTypeMatcher(Arrays.asList(types));
    }

    public static JunctionMethodMatcher takesArguments(List<? extends TypeDescription> types) {
        return new ParameterTypeMatcher(types);
    }

    public static JunctionMethodMatcher takesArguments(int number) {
        return new ParameterCountMatcher(number);
    }

    /**
     * Selects a method depending on whether they can throw a specific exception.
     *
     * @param exceptionType The exception type to be thrown.
     * @return A new method matcher that selects methods on whether they can throw {@code exceptionType}.
     */
    public static JunctionMethodMatcher canThrow(Class<? extends Throwable> exceptionType) {
        return new ExceptionMethodMatcher(exceptionType);
    }

    /**
     * Selects an exact method. Note that the resulting matcher will not check overriden signatures.
     *
     * @param method The method to match.
     * @return A method matcher that matches the given method.
     */
    public static JunctionMethodMatcher is(Method method) {
        return new MethodEqualityMethodMatcher(method);
    }

    /**
     * Selects an exact constructor. Note that the resulting matcher will not check overriden signatures.
     *
     * @param constructor The constructor to match.
     * @return A method matcher that matches the given constructor.
     */
    public static JunctionMethodMatcher is(Constructor<?> constructor) {
        return new ConstructorEqualityMethodMatcher(constructor);
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
     * Selects methods that are not constructors.
     *
     * @return A new method matcher that matches constructors but not methods.
     */
    public static JunctionMethodMatcher isVisibleFromPackage(String packageName) {
        return new PackageNameMethodMatcher(packageName);
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
        return new DeclaringTypeMethodMatcher(type);
    }

    /**
     * Selects methods with identical signature to the given method description where the return type
     * is considered to be part of the signature.
     *
     * @param methodDescription The method to match against.
     * @return A method matcher selecting methods with the same signature as {@code methodDescription}.
     */
    public static JunctionMethodMatcher hasSameSignatureAs(MethodDescription methodDescription) {
        return new MethodSignatureMethodMatcher(methodDescription);
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

    public static JunctionMethodMatcher isFinalizer() {
        return named("finalize").and(takesArguments(0)).and(returns(void.class));
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
     * A method matcher that matches a given method description.
     *
     * @param methodDescription The method description to be matched.
     * @return A method matcher that matches the given method description.
     */
    public static JunctionMethodMatcher describedBy(MethodDescription methodDescription) {
        return new MethodDescriptionMatcher(methodDescription);
    }

    public static JunctionMethodMatcher javaSignatureCompatibleTo(MethodDescription methodDescription) {
        return (methodDescription.isConstructor() ? isConstructor() : named(methodDescription.getName()))
                .and(takesArguments(methodDescription.getParameterTypes()));
    }

    private MethodMatchers() {
        throw new AssertionError();
    }
}

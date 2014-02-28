package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

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
    public static JunctionMethodMatcher matches(String regex) {
        return new MethodNameMethodMatcher(regex, MatchMode.MATCHES);
    }

    private static class ModifierMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final int modifierMask;

        public ModifierMethodMatcher(int modifierMask) {
            this.modifierMask = modifierMask;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return (methodDescription.getModifiers() & modifierMask) != 0;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && modifierMask == ((ModifierMethodMatcher) other).modifierMask;
        }

        @Override
        public String toString() {
            return "ModifierMethodMatcher{modifierMask=" + modifierMask + '}';
        }

        @Override
        public int hashCode() {
            return modifierMask;
        }
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

    private static class VarArgsMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isVarArgs();
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof VarArgsMethodMatcher;
        }

        @Override
        public String toString() {
            return "VarArgsMethodMatcher";
        }

        @Override
        public int hashCode() {
            return 31;
        }
    }

    /**
     * Selects a method when it is defined using a var args argument.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isVarArgs() {
        return new VarArgsMethodMatcher();
    }

    private static class SyntheticMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isSynthetic();
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof SyntheticMethodMatcher;
        }

        @Override
        public String toString() {
            return "SyntheticMethodMatcher";
        }

        @Override
        public int hashCode() {
            return 37;
        }
    }

    /**
     * Selects a method when it is {@code synthetic}.
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isSynthetic() {
        return new SyntheticMethodMatcher();
    }

    private static class BridgeMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isBridge();
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof BridgeMethodMatcher;
        }

        @Override
        public String toString() {
            return "BridgeMethodMatcher";
        }

        @Override
        public int hashCode() {
            return 41;
        }
    }

    /**
     * Selects a method when it is marked as a bridge method
     *
     * @return A method matcher for the specified regular expression.
     */
    public static JunctionMethodMatcher isBridge() {
        return new BridgeMethodMatcher();
    }

    private static class ReturnTypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final Class<?> returnType;

        public ReturnTypeMatcher(Class<?> returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getReturnType().represents(returnType);
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

    /**
     * Selects a method by its return type.
     *
     * @param type The return type of the method.
     * @return A new method matcher that selects methods returning {@code type}.
     */
    public static JunctionMethodMatcher returns(Class<?> type) {
        return new ReturnTypeMatcher(type);
    }

    private static class ParameterTypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final Class<?>[] parameterType;

        public ParameterTypeMatcher(Class<?>[] parameterType) {
            this.parameterType = parameterType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            List<TypeDescription> parameterTypes = methodDescription.getParameterTypes();
            if (parameterTypes.size() != parameterType.length) {
                return false;
            }
            int i = 0;
            for (TypeDescription typeDescription : parameterTypes) {
                if (!typeDescription.represents(parameterType[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(parameterType, ((ParameterTypeMatcher) other).parameterType);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(parameterType);
        }

        @Override
        public String toString() {
            return "ParameterTypeMatcher{parameterType=" + Arrays.toString(parameterType) + '}';
        }
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

    private static class ExceptionMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final Class<?> exceptionType;

        public ExceptionMethodMatcher(Class<?> exceptionType) {
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

    /**
     * Selects a method depending on whether they can throw a specific exception.
     *
     * @param exceptionType The exception type to be thrown.
     * @return A new method matcher that selects methods on whether they can throw {@code exceptionType}.
     */
    public static JunctionMethodMatcher canThrow(Class<? extends Exception> exceptionType) {
        return new ExceptionMethodMatcher(exceptionType);
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

    /**
     * Selects an exact method.
     *
     * @param method The method to match.
     * @return A method matcher that matches the given method.
     */
    public static JunctionMethodMatcher is(Method method) {
        return new MethodEqualityMethodMatcher(method);
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

    /**
     * Selects an exact constructor.
     *
     * @param constructor The constructor to match.
     * @return A method matcher that matches the given constructor.
     */
    public static JunctionMethodMatcher is(Constructor<?> constructor) {
        return new ConstructorEqualityMethodMatcher(constructor);
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

    private static class PackageNameMatcher extends JunctionMethodMatcher.AbstractBase {

        private final String packageName;

        private PackageNameMatcher(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getDeclaringType().getPackageName().equals(packageName);
        }
    }

    /**
     * Selects methods that are not constructors.
     *
     * @return A new method matcher that matches constructors but not methods.
     */
    public static JunctionMethodMatcher isDefinedInPackage(String packageName) {
        return new PackageNameMatcher(packageName);
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

    /**
     * Selects methods that are overridable, i.e. not constructors, final, private or static or is defined
     * in a final type.
     *
     * @return A new method matcher that matches overridable methods.
     */
    public static JunctionMethodMatcher isOverridable() {
        return new OverridableMethodMatcher();
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

    /**
     * Only matches the default finalizer method as declared in {@link Object#finalize()} but not methods that
     * override this method.
     *
     * @return A new method matcher that matches the default finalizer.
     */
    public static JunctionMethodMatcher isDefaultFinalize() {
        return new DefaultFinalizeMethodMatcher();
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
     * Inverts another method matcher.
     *
     * @param methodMatcher The method matcher to be inverted.
     * @return A method matcher that returns {@code true} if the {@code methodMatcher} returns {@code false}.
     */
    public static JunctionMethodMatcher not(MethodMatcher methodMatcher) {
        return new NegatingMethodMatcher(methodMatcher);
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
            return "BooleanMethodMatcher{matches=" + matches + '}';
        }
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

    /**
     * A method matcher that matches a given method description.
     *
     * @param methodDescription The method description to be matched.
     * @return A method matcher that matches the given method description.
     */
    public static JunctionMethodMatcher describedBy(MethodDescription methodDescription) {
        return new MethodDescriptionMatcher(methodDescription);
    }

    private MethodMatchers() {
        throw new AssertionError();
    }
}

package net.bytebuddy.instrumentation.method.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Opcodes;

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

    private static enum MatchMode {

        EQUALS_FULLY("named"),
        EQUALS_FULLY_IGNORE_CASE("namedIgnoreCase"),
        STARTS_WITH("startsWith"),
        STARTS_WITH_IGNORE_CASE("startsWithIgnoreCase"),
        ENDS_WITH("endsWith"),
        ENDS_WITH_IGNORE_CASE("endsWithIgnoreCase"),
        CONTAINS("contains"),
        CONTAINS_IGNORE_CASE("containsIgnoreCase"),
        MATCHES("matches");

        private final String description;

        private MatchMode(String description) {
            this.description = description;
        }

        private String getDescription() {
            return description;
        }

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

        private final String name;
        private final MatchMode matchMode;

        public MethodNameMethodMatcher(String name, MatchMode matchMode) {
            this.name = name;
            this.matchMode = matchMode;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return matchMode.matches(name, methodDescription.getName());
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
            return "modifiers(" + modifiers + ')';
        }

        @Override
        public int hashCode() {
            return modifiers;
        }
    }

    private static class ParameterTypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final List<? extends TypeDescription> parameterTypes;

        private ParameterTypeMatcher(List<? extends TypeDescription> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getParameterTypes().equals(parameterTypes);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && parameterTypes.equals(((ParameterTypeMatcher) other).parameterTypes);
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

    private static class ParameterSubtypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final List<? extends TypeDescription> parameterTypes;

        private ParameterSubtypeMatcher(List<? extends TypeDescription> parameterTypes) {
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
                    && parameterTypes.equals(((ParameterSubtypeMatcher) other).parameterTypes);
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

    private static class ParameterSuperTypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final List<? extends TypeDescription> parameterTypes;

        private ParameterSuperTypeMatcher(List<? extends TypeDescription> parameterTypes) {
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
                    && parameterTypes.equals(((ParameterSuperTypeMatcher) other).parameterTypes);
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
            return "parameterCount(" + numberOfParameters + ')';
        }
    }

    private static class ReturnTypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeDescription returnType;

        public ReturnTypeMatcher(TypeDescription returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getReturnType().equals(returnType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && returnType.equals(((ReturnTypeMatcher) other).returnType);
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

    private static class ReturnSubtypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeDescription returnType;

        public ReturnSubtypeMatcher(TypeDescription returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getReturnType().isAssignableTo(returnType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && returnType.equals(((ReturnSubtypeMatcher) other).returnType);
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

    private static class ReturnSupertypeMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeDescription returnType;

        public ReturnSupertypeMatcher(TypeDescription returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getReturnType().isAssignableFrom(returnType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && returnType.equals(((ReturnSupertypeMatcher) other).returnType);
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

    private static class ExceptionMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeDescription exceptionType;

        public ExceptionMethodMatcher(TypeDescription exceptionType) {
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
            return "canThrow(" + exceptionType + ')';
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
            return "is(" + methodDescription + ')';
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
            return "is(" + method + ')';
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
            return "is(" + constructor + ')';
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
            return "signatureOf(" + methodDescription + ')';
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
            return "isMethod()";
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
            return "isOverridable()";
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
            return "isDefaultFinalizer()";
        }

        @Override
        public int hashCode() {
            return 54;
        }
    }

    private static class VisibilityMethodMatcher extends JunctionMethodMatcher.AbstractBase {

        private final TypeDescription typeDescription;

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
            return "visibleTo(" + typeDescription + ')';
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
            return "declaredBy(" + declaringType + ')';
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
            return Boolean.toString(matches);
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
            return "not(" + methodMatcher + ')';
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
     * Selects a method by its exact return type.
     *
     * @param type The return type of the method.
     * @return A new method matcher that selects methods returning {@code type}.
     */
    public static JunctionMethodMatcher returns(Class<?> type) {
        return new ReturnTypeMatcher(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Selects a method by its exact return type.
     *
     * @param type A description of the return type of the method.
     * @return A new method matcher that selects methods returning {@code type}.
     */
    public static JunctionMethodMatcher returns(TypeDescription type) {
        return new ReturnTypeMatcher(type);
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
        return new ReturnSubtypeMatcher(type);
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
        return new ReturnSupertypeMatcher(type);
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
        return new ParameterTypeMatcher(types);
    }

    /**
     * Selects a method by the number of its parameters.
     *
     * @param number The number of parameters of any matching method.
     * @return A method matcher that matches methods with {@code number} parameters.
     */
    public static JunctionMethodMatcher takesArguments(int number) {
        return new ParameterCountMatcher(number);
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
        return new ParameterSubtypeMatcher(types);
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
        return new ParameterSuperTypeMatcher(types);
    }

    /**
     * Selects a method depending on whether they can throw a specific exception. Exceptions that subclass
     * {@link java.lang.Error} or {@link java.lang.RuntimeException} can always be thrown.
     *
     * @param exceptionType The exception type to be thrown.
     * @return A new method matcher that selects methods on whether they can throw {@code exceptionType}.
     */
    public static JunctionMethodMatcher canThrow(Class<? extends Throwable> exceptionType) {
        if (RuntimeException.class.isAssignableFrom(exceptionType) || Error.class.isAssignableFrom(exceptionType)) {
            return new BooleanMethodMatcher(true);
        }
        return new ExceptionMethodMatcher(new TypeDescription.ForLoadedType(exceptionType));
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
            return new ExceptionMethodMatcher(exceptionType);
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
     * A method matcher that matches a given method description.
     *
     * @param methodDescription The method description to be matched.
     * @return A method matcher that matches the given method description.
     */
    public static JunctionMethodMatcher is(MethodDescription methodDescription) {
        return new MethodDescriptionMatcher(methodDescription);
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
        return new DeclaringTypeMethodMatcher(type);
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
        return new MethodSignatureMethodMatcher(methodDescription);
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

    private MethodMatchers() {
        throw new AssertionError();
    }
}

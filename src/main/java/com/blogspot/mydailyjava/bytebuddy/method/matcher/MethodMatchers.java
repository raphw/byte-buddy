package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

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
                    return left.equals(right);
                case EQUALS_FULLY_IGNORE_CASE:
                    return left.equalsIgnoreCase(right);
                case STARTS_WITH:
                    return left.startsWith(right);
                case STARTS_WITH_IGNORE_CASE:
                    return left.toLowerCase().startsWith(right.toLowerCase());
                case ENDS_WITH:
                    return left.endsWith(right);
                case ENDS_WITH_IGNORE_CASE:
                    return left.toLowerCase().endsWith(right.toLowerCase());
                case CONTAINS:
                    return left.contains(right);
                case CONTAINS_IGNORE_CASE:
                    return left.toLowerCase().contains(right.toLowerCase());
                case MATCHES:
                    return left.matches(right);
                default:
                    throw new AssertionError();
            }
        }
    }

    private static class ClassNameMethodMatcher extends JunctionMethodMatcher {

        private final Class<?> type;

        public ClassNameMethodMatcher(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean matches(Method method) {
            try {
                return type.getDeclaredMethod(method.getName(), method.getParameterTypes()) != null;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    }

    public static JunctionMethodMatcher declaredIn(Class<?> type) {
        return new ClassNameMethodMatcher(type);
    }

    private static class MethodNameMethodMatcher extends JunctionMethodMatcher {

        private final String methodName;
        private final MatchMode matchMode;

        public MethodNameMethodMatcher(String methodName, MatchMode matchMode) {
            this.methodName = methodName;
            this.matchMode = matchMode;
        }

        @Override
        public boolean matches(Method method) {
            return matchMode.matches(methodName, method.getName());
        }
    }

    public static JunctionMethodMatcher named(String name) {
        return new MethodNameMethodMatcher(name, MatchMode.EQUALS_FULLY);
    }

    public static JunctionMethodMatcher namedIgnoreCase(String prefix) {
        return new MethodNameMethodMatcher(prefix, MatchMode.EQUALS_FULLY_IGNORE_CASE);
    }

    public static JunctionMethodMatcher nameStartsWith(String prefix) {
        return new MethodNameMethodMatcher(prefix, MatchMode.STARTS_WITH);
    }

    public static JunctionMethodMatcher nameStartsWithIgnoreCase(String name) {
        return new MethodNameMethodMatcher(name, MatchMode.STARTS_WITH_IGNORE_CASE);
    }

    public static JunctionMethodMatcher nameEndsWith(String suffix) {
        return new MethodNameMethodMatcher(suffix, MatchMode.ENDS_WITH);
    }

    public static JunctionMethodMatcher nameEndsWithIgnoreCase(String suffix) {
        return new MethodNameMethodMatcher(suffix, MatchMode.ENDS_WITH_IGNORE_CASE);
    }

    public static JunctionMethodMatcher nameContains(String contains) {
        return new MethodNameMethodMatcher(contains, MatchMode.CONTAINS);
    }

    public static JunctionMethodMatcher nameContainsIgnoreCase(String contains) {
        return new MethodNameMethodMatcher(contains, MatchMode.CONTAINS_IGNORE_CASE);
    }

    public static JunctionMethodMatcher matches(String regex) {
        return new MethodNameMethodMatcher(regex, MatchMode.MATCHES);
    }

    private static class ModifierMethodMatcher extends JunctionMethodMatcher {

        private final int modifierMask;

        public ModifierMethodMatcher(int modifierMask) {
            this.modifierMask = modifierMask;
        }

        @Override
        public boolean matches(Method method) {
            return (method.getModifiers() & modifierMask) != 0;
        }
    }

    public static JunctionMethodMatcher isPublic() {
        return new ModifierMethodMatcher(Modifier.PUBLIC);
    }

    public static JunctionMethodMatcher isProtected() {
        return new ModifierMethodMatcher(Modifier.PROTECTED);
    }

    public static JunctionMethodMatcher isPackagePrivate() {
        return not(isPublic().or(isProtected()).or(isPrivate()));
    }

    public static JunctionMethodMatcher isPrivate() {
        return new ModifierMethodMatcher(Modifier.PRIVATE);
    }

    public static JunctionMethodMatcher isFinal() {
        return new ModifierMethodMatcher(Modifier.FINAL);
    }

    public static JunctionMethodMatcher isStatic() {
        return new ModifierMethodMatcher(Modifier.STATIC);
    }

    private static class SyntheticMethodMatcher extends JunctionMethodMatcher {

        @Override
        public boolean matches(Method method) {
            return method.isSynthetic();
        }
    }

    public static JunctionMethodMatcher isSynthetic() {
        return new SyntheticMethodMatcher();
    }

    private static class ReturnTypeMatcher extends JunctionMethodMatcher {

        private final Class<?> returnType;

        public ReturnTypeMatcher(Class<?> returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(Method method) {
            return method.getReturnType() == returnType;
        }
    }

    public static JunctionMethodMatcher returns(Class<?> type) {
        return new ReturnTypeMatcher(type);
    }

    private static class ParameterTypeMatcher extends JunctionMethodMatcher {

        private final Class<?>[] parameterType;

        public ParameterTypeMatcher(Class<?>[] parameterType) {
            this.parameterType = parameterType;
        }

        @Override
        public boolean matches(Method method) {
            return Arrays.equals(parameterType, method.getParameterTypes());
        }
    }

    public static JunctionMethodMatcher takesArguments(Class<?>... types) {
        return new ParameterTypeMatcher(types);
    }

    private static class ExceptionMethodMatcher extends JunctionMethodMatcher {

        private final Class<?> exceptionType;

        public ExceptionMethodMatcher(Class<?> exceptionType) {
            this.exceptionType = exceptionType;
        }

        @Override
        public boolean matches(Method method) {
            for (Class<?> exceptionType : method.getExceptionTypes()) {
                if (exceptionType.isAssignableFrom(exceptionType)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static JunctionMethodMatcher canThrow(Class<? extends Exception> exceptionType) {
        return new ExceptionMethodMatcher(exceptionType);
    }

    private static class EqualityMethodMatcher extends JunctionMethodMatcher {

        private final Method method;

        public EqualityMethodMatcher(Method method) {
            this.method = method;
        }

        @Override
        public boolean matches(Method method) {
            return method.equals(this.method);
        }
    }

    public static JunctionMethodMatcher is(Method method) {
        return new EqualityMethodMatcher(method);
    }

    private static class NegatingMethodMatcher extends JunctionMethodMatcher {

        private final MethodMatcher methodMatcher;

        public NegatingMethodMatcher(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public boolean matches(Method method) {
            return !methodMatcher.matches(method);
        }
    }

    public static JunctionMethodMatcher not(MethodMatcher methodMatcher) {
        return new NegatingMethodMatcher(methodMatcher);
    }

    private static class BooleanMatcher extends JunctionMethodMatcher {

        private final boolean matches;

        private BooleanMatcher(boolean matches) {
            this.matches = matches;
        }

        @Override
        public boolean matches(Method method) {
            return matches;
        }
    }

    public static JunctionMethodMatcher any() {
        return new BooleanMatcher(true);
    }

    public static JunctionMethodMatcher none() {
        return new BooleanMatcher(false);
    }

    private MethodMatchers() {
        throw new AssertionError();
    }
}

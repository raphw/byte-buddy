package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

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

        private final String declaringClass;

        public ClassNameMethodMatcher(Class<?> declaringClass) {
            this.declaringClass = Type.getInternalName(declaringClass);
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            return classContext.getName().equals(declaringClass);
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
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            return matchMode.matches(methodName, methodContext.getName());
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

    private static class AccessMethodMatcher extends JunctionMethodMatcher {

        private final int access;

        public AccessMethodMatcher(int access) {
            this.access = access;
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            return (methodContext.getAccess() & access) != 0;
        }
    }

    public static JunctionMethodMatcher isPublic() {
        return new AccessMethodMatcher(Opcodes.ACC_PUBLIC);
    }

    public static JunctionMethodMatcher isProtected() {
        return new AccessMethodMatcher(Opcodes.ACC_PROTECTED);
    }

    public static JunctionMethodMatcher isPackagePrivate() {
        return not(isPublic().or(isProtected()).or(isPrivate()));
    }

    public static JunctionMethodMatcher isPrivate() {
        return new AccessMethodMatcher(Opcodes.ACC_PRIVATE);
    }

    private static class ReturnTypeMatcher extends JunctionMethodMatcher {

        private final Class<?> returnType;

        public ReturnTypeMatcher(Class<?> returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            return methodContext.getType().getReturnType().getClassName().equals(returnType.getName());
        }
    }

    public static JunctionMethodMatcher returns(Class<?> type) {
        return new ReturnTypeMatcher(type);
    }

    private static class ArgumentTypeMatcher extends JunctionMethodMatcher {

        private final Class<?>[] argumentType;

        public ArgumentTypeMatcher(Class<?>[] argumentType) {
            this.argumentType = argumentType;
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            Type[] argumentType = methodContext.getType().getArgumentTypes();
            if (this.argumentType.length != argumentType.length) {
                return false;
            }
            for (int i = 0; i < argumentType.length; i++) {
                if (!this.argumentType[i].getName().equals(argumentType[i].getClassName())) {
                    return false;
                }
            }
            return true;
        }
    }

    public static JunctionMethodMatcher takesArguments(Class<?>... types) {
        return new ArgumentTypeMatcher(types);
    }

    private static class ExceptionMethodMatcher extends JunctionMethodMatcher {

        private final Class<?> exceptionType;

        public ExceptionMethodMatcher(Class<?> exceptionType) {
            this.exceptionType = exceptionType;
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            for (String exceptionName : methodContext.getExceptions()) {
                if (exceptionName.equals(Type.getInternalName(exceptionType))) {
                    return true;
                }
            }
            return false;
        }
    }

    public static JunctionMethodMatcher canThrow(Class<? extends Exception> exceptionType) {
        return new ExceptionMethodMatcher(exceptionType);
    }

    private static class ReflectionMethodMatcher extends JunctionMethodMatcher {

        private final Method method;

        public ReflectionMethodMatcher(Method method) {
            this.method = method;
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            return Type.getMethodDescriptor(method).equals(methodContext.getType().getDescriptor());
        }
    }

    public static JunctionMethodMatcher is(Method method) {
        return new ReflectionMethodMatcher(method);
    }

    private static class NegatingMethodMatcher extends JunctionMethodMatcher {

        private final MethodMatcher methodMatcher;

        public NegatingMethodMatcher(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
            return !methodMatcher.matches(classContext, methodContext);
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
        public boolean matches(ClassContext classContext, MethodContext methodContext) {
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

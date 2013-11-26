package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

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
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return declaringClass.equals(classTypeName);
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
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return matchMode.matches(this.methodName, methodName);
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
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return (methodAccess & access) != 0;
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

        private final String returnTypeName;

        public ReturnTypeMatcher(Class<?> type) {
            returnTypeName = Type.getDescriptor(type);
        }

        @Override
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return methodDesc.endsWith(returnTypeName);
        }
    }

    public static JunctionMethodMatcher returns(Class<?> type) {
        return new ReturnTypeMatcher(type);
    }

    private static class ArgumentTypeMatcher extends JunctionMethodMatcher {

        private static final char ARGUMENT_TYPES_SUFFIX = ')';

        private final String argumentTypeNames;

        public ArgumentTypeMatcher(Class<?>[] type) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Class<?> t : type) {
                stringBuilder.append(Type.getDescriptor(t));
            }
            argumentTypeNames = stringBuilder.toString();
        }

        @Override
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return methodDesc.substring(1).startsWith(argumentTypeNames)
                    && methodDesc.charAt(argumentTypeNames.length() + 2) == ARGUMENT_TYPES_SUFFIX;
        }
    }

    public static JunctionMethodMatcher takesArguments(Class<?>... types) {
        return new ArgumentTypeMatcher(types);
    }

    private static class ExceptionMethodMatcher extends JunctionMethodMatcher {

        private final Set<String> exceptionNames;

        public ExceptionMethodMatcher(Class<?> exceptionType) {
            exceptionNames = new HashSet<String>();
            exceptionNames.add(Type.getInternalName(RuntimeException.class));
            do {
                exceptionNames.add(Type.getInternalName(exceptionType));
            } while ((exceptionType = exceptionType.getSuperclass()) != Object.class);
        }

        @Override
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            for (String e : methodException) {
                if (exceptionNames.contains(methodException)) {
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

        private final String classTypeName, methodName, methodDesc;

        public ReflectionMethodMatcher(Method method) {
            classTypeName = Type.getInternalName(method.getDeclaringClass());
            methodName = method.getName();
            methodDesc = Type.getMethodDescriptor(method);
        }

        @Override
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return classTypeName.equals(this.classTypeName)
                    && methodName.equals(this.methodName)
                    && methodDesc.equals(this.methodDesc);
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
        public boolean matches(String classTypeName,
                               String methodName,
                               int methodAccess,
                               String methodDesc,
                               String methodSignature,
                               String[] methodException) {
            return !methodMatcher.matches(classTypeName, methodName, methodAccess,
                    methodDesc, methodSignature, methodException);
        }
    }

    public static JunctionMethodMatcher not(MethodMatcher methodMatcher) {
        return new NegatingMethodMatcher(methodMatcher);
    }

    private MethodMatchers() {
        throw new AssertionError();
    }
}

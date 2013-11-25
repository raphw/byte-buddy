package com.blogspot.mydailyjava.bytebuddy.asm.method.score;

import com.blogspot.mydailyjava.bytebuddy.asm.method.MethodArgumentTypes;
import com.blogspot.mydailyjava.bytebuddy.asm.method.MethodReturnType;
import com.blogspot.mydailyjava.bytebuddy.asm.method.score.annotation.*;
import com.blogspot.mydailyjava.bytebuddy.asm.method.stack.*;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AnnotationMethodScorer implements MethodScorer {

    public static class Factory implements MethodScorer.Factory {

        @Override
        public MethodScorer make(String targetClassName, int access, String desc, String signature, String[] exceptions) {
            return new AnnotationMethodScorer(targetClassName, desc, exceptions);
        }
    }

    private final String sourceTypeName;
    private final List<String> sourceMethodArgumentTypeNames;
    private final MethodReturnType sourceMethodReturnType;
    private final String[] sourceMethodExceptions;

    public AnnotationMethodScorer(String sourceTypeName, String sourceMethodDesc, String[] sourceMethodExceptions) {
        this.sourceTypeName = sourceTypeName;
        sourceMethodArgumentTypeNames = MethodArgumentTypes.listNAmes(sourceMethodDesc);
        this.sourceMethodReturnType = new MethodReturnType(sourceMethodDesc);
        this.sourceMethodExceptions = sourceMethodExceptions;
    }

    @Override
    public ScoredMethodDelegation evaluate(Method targetMethod) {
        Annotation[][] targetMethodArgumentAnnotations = targetMethod.getParameterAnnotations();
        Class<?>[] targetMethodArgumentType = targetMethod.getParameterTypes();
        List<MethodCallStackValue> targetMethodCallStack = new ArrayList<MethodCallStackValue>(targetMethodArgumentType.length);
        for (String sourceMethodException : sourceMethodExceptions) {
            evaluateExceptionTypeCompatibility(sourceMethodException, targetMethod.getExceptionTypes());
        }
        evaluateReturnTypeCompatibility(targetMethod.getReturnType());
        int score = 0;
        boolean[] sourceMethodArgumentIsBound = new boolean[sourceMethodArgumentTypeNames.size()];
        for (int i = 0; i < targetMethodArgumentType.length; i++) {
            score += scoreParameter(targetMethodArgumentType[i], targetMethodArgumentAnnotations[i],
                    sourceMethodArgumentIsBound, targetMethodCallStack);
        }
        return new ScoredMethodDelegation(score, targetMethodCallStack);
    }

    private int scoreParameter(Class<?> targetMethodArgumentType,
                               Annotation[] targetMethodArgumentAnnotation,
                               boolean[] sourceMethodArgumentIsBound,
                               List<MethodCallStackValue> targetMethodCallStack) {
        if (tryLoadIndexedArgument(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return 1;
        } else if (tryLoadThis(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return 1;
        } else if (tryLoadAllArguments(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return 1;
        } else if (tryLoadClassId(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return 1;
        } else if (tryLoadObjectId(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return 1;
        } else if (tryLoadPlainArgument(targetMethodArgumentType, sourceMethodArgumentIsBound, targetMethodCallStack)) {
            return 1;
        }
        throw new NoScoreException();
    }

    private boolean tryLoadIndexedArgument(Class<?> targetMethodArgumentType,
                                           Annotation[] targetMethodArgumentAnnotation,
                                           List<MethodCallStackValue> targetMethodCallStack) {
        Argument argument = tryFind(Argument.class, targetMethodArgumentAnnotation);
        if (argument != null
                && matches(targetMethodArgumentType, sourceMethodArgumentTypeNames.get(argument.value()))) {
            targetMethodCallStack.add(new ArgumentReference(argument.value()));
            return true;
        }
        return false;
    }

    private boolean tryLoadAllArguments(Class<?> targetMethodArgumentType,
                                        Annotation[] targetMethodArgumentAnnotation,
                                        List<MethodCallStackValue> targetMethodCallStack) {
        if (tryFind(AllArguments.class, targetMethodArgumentAnnotation) != null
                && isAssignableArray(targetMethodArgumentType)) {
            targetMethodCallStack.add(new AllArgumentsReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadThis(Class<?> targetMethodArgumentType,
                                Annotation[] targetMethodArgumentAnnotation,
                                List<MethodCallStackValue> targetMethodCallStack) {
        if (tryFind(This.class, targetMethodArgumentAnnotation) != null
                && matches(targetMethodArgumentType, sourceTypeName)) {
            targetMethodCallStack.add(new ThisReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadClassId(Class<?> targetMethodArgumentType,
                                   Annotation[] targetMethodArgumentAnnotation,
                                   List<MethodCallStackValue> targetMethodCallStack) {
        if (tryFind(ClassId.class, targetMethodArgumentAnnotation) != null
                && isUuid(targetMethodArgumentType)) {
            targetMethodCallStack.add(new ClassIdReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadObjectId(Class<?> targetMethodArgumentType,
                                    Annotation[] targetMethodArgumentAnnotation,
                                    List<MethodCallStackValue> targetMethodCallStack) {
        if (tryFind(ObjectId.class, targetMethodArgumentAnnotation) != null
                && isUuid(targetMethodArgumentType)) {
            targetMethodCallStack.add(new ObjectIdReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadPlainArgument(Class<?> targetMethodArgumentType,
                                         boolean[] sourceMethodArgumentIsBound,
                                         List<MethodCallStackValue> targetMethodCallStack) {
        int nextUnboundIndex = findNextUnboundIndex(targetMethodArgumentType, sourceMethodArgumentIsBound);
        if (nextUnboundIndex >= 0
                && matches(targetMethodArgumentType, sourceMethodArgumentTypeNames.get(nextUnboundIndex))) {
            targetMethodCallStack.add(new ArgumentReference(nextUnboundIndex));
            return true;
        }
        return false;
    }

    private int findNextUnboundIndex(Class<?> targetMethodArgumentType, boolean[] sourceMethodArgumentIsBound) {
        for (int index = 0; index < sourceMethodArgumentIsBound.length; index++) {
            if (!sourceMethodArgumentIsBound[index]) {
                if (matches(targetMethodArgumentType, sourceMethodArgumentTypeNames.get(index)))
                    sourceMethodArgumentIsBound[index] = true;
                return index;
            }
        }
        return -1;
    }

    private void evaluateReturnTypeCompatibility(Class<?> targetMethodReturnType) {
        if (!matches(targetMethodReturnType, sourceMethodReturnType.getReturnTypeName())) {
            throw new NoScoreException();
        }
    }

    private void evaluateExceptionTypeCompatibility(String sourceExceptionType, Class<?>[] targetExceptionTypes) {
        for (Class<?> targetExceptionType : targetExceptionTypes) {
            evaluateExceptionTypeCompatibility(sourceExceptionType, targetExceptionType);
        }
    }

    private void evaluateExceptionTypeCompatibility(String sourceExceptionType, Class<?> targetExceptionType) {
        if (!matches(sourceExceptionType, targetExceptionType)) {
            throw new NoScoreException();
        }
    }

    private boolean matches(Class<?> superType, String subTypeName) {
        try {
            return superType.isAssignableFrom(Class.forName(Type.getType(subTypeName).getClassName()));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean matches(String superTypeName, Class<?> subType) {
        try {
            return Class.forName(Type.getType(superTypeName).getClassName()).isAssignableFrom(subType);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isAssignableArray(Class<?> targetMethodArgumentType) {
        if (targetMethodArgumentType.isArray()) {
            targetMethodArgumentType = targetMethodArgumentType.getComponentType();
            for (String sourceMethodArgumentTypeName : sourceMethodArgumentTypeNames) {
                if (!matches(targetMethodArgumentType, sourceMethodArgumentTypeName)) {
                    throw new NoScoreException();
                }
            }
        }
        throw new NoScoreException();
    }

    private boolean isUuid(Class<?> type) {
        return type == UUID.class;
    }

    @SuppressWarnings("unchecked")
    private static <T> T tryFind(Class<T> annotationType, Annotation[] annotation) {
        for (Annotation a : annotation) {
            if (a.getClass() == annotationType) {
                return (T) a;
            }
        }
        return null;
    }
}

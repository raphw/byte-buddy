package com.blogspot.mydailyjava.bytebuddy.method.score;


import com.blogspot.mydailyjava.bytebuddy.method.MethodArgumentTypes;
import com.blogspot.mydailyjava.bytebuddy.method.MethodReturnType;
import com.blogspot.mydailyjava.bytebuddy.method.score.annotation.*;
import com.blogspot.mydailyjava.bytebuddy.method.stack.*;
import com.blogspot.mydailyjava.bytebuddy.type.TypeCompatibilityEvaluator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationMethodScorer implements MethodScorer {

    private static final double DOUBLE_SCORE = 2d;
    private static final double SINGLE_SCORE = 1d;
    private static final double HALF_SCORE = 0.5d;
    private static final double NO_SCORE = 0d;

    public static class Factory implements MethodScorer.Factory {

        @Override
        public MethodScorer make(String classTypeName,
                                 String methodName,
                                 int methodAccess,
                                 String methodDesc,
                                 String methodSignature,
                                 String[] methodException,
                                 TypeCompatibilityEvaluator typeCompatibilityEvaluator) {
            return new AnnotationMethodScorer(classTypeName, methodName, methodDesc, methodException, typeCompatibilityEvaluator);
        }
    }

    private final String classTypeName;
    private final String sourceMethodName;
    private final List<String> sourceMethodArgumentTypeNames;
    private final MethodReturnType sourceMethodReturnType;
    private final String[] sourceMethodExceptions;
    private final TypeCompatibilityEvaluator typeCompatibilityEvaluator;

    public AnnotationMethodScorer(String classTypeName,
                                  String sourceMethodName,
                                  String sourceMethodDesc,
                                  String[] sourceMethodExceptions,
                                  TypeCompatibilityEvaluator typeCompatibilityEvaluator) {
        sourceMethodArgumentTypeNames = MethodArgumentTypes.listNAmes(sourceMethodDesc);
        sourceMethodReturnType = new MethodReturnType(sourceMethodDesc);
        this.classTypeName = classTypeName;
        this.sourceMethodName = sourceMethodName;
        this.sourceMethodExceptions = sourceMethodExceptions;
        this.typeCompatibilityEvaluator = typeCompatibilityEvaluator;
    }

    @Override
    public MatchedMethod evaluate(Method targetMethod) {
        assertReturnType(targetMethod.getReturnType());
        assertExceptions(targetMethod);
        Annotation[][] targetMethodArgumentAnnotations = targetMethod.getParameterAnnotations();
        Class<?>[] targetMethodArgumentType = targetMethod.getParameterTypes();
        List<MethodCallStackValue> targetMethodCallStack = new ArrayList<MethodCallStackValue>(targetMethodArgumentType.length);
        double score = 0;
        boolean[] sourceMethodArgumentIsBound = new boolean[sourceMethodArgumentTypeNames.size()];
        for (int i = 0; i < targetMethodArgumentType.length; i++) {
            score += assertAndScoreParameter(targetMethodArgumentType[i],
                    makeAnnotationMap(targetMethodArgumentAnnotations[i]),
                    sourceMethodArgumentIsBound,
                    targetMethodCallStack);
        }
        score += scoreMethodNameEquality(targetMethod);
        return new MatchedMethod(score, targetMethodCallStack);
    }

    private Map<Class<? extends Annotation>, Annotation> makeAnnotationMap(Annotation[] annotation) {
        if (annotation.length == 0) {
            return Collections.emptyMap();
        }
        Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
        for (Annotation a : annotation) {
            annotationMap.put(a.annotationType(), a);
        }
        return annotationMap;
    }

    private void assertReturnType(Class<?> targetMethodReturnType) {
        if (!typeCompatibilityEvaluator.isAssignable(targetMethodReturnType, sourceMethodReturnType.getReturnTypeName())) {
            throw new NoScoreException();
        }
    }

    private void assertExceptions(Method targetMethod) {
        if (!typeCompatibilityEvaluator.isThrowable(targetMethod.getExceptionTypes(), sourceMethodExceptions)) {
            throw new NoScoreException();
        }
    }

    private double assertAndScoreParameter(Class<?> targetMethodArgumentType,
                                           Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotation,
                                           boolean[] sourceMethodArgumentIsBound,
                                           List<MethodCallStackValue> targetMethodCallStack) {
        if (tryLoadIndexedArgument(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return getScore(targetMethodCallStack);
        } else if (tryLoadThis(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return getScore(targetMethodCallStack);
        } else if (tryLoadAllArguments(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return getScore(targetMethodCallStack);
        } else if (tryLoadClassId(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return getScore(targetMethodCallStack);
        } else if (tryLoadInstanceId(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)) {
            return getScore(targetMethodCallStack);
        } else if (tryLoadPlainArgument(targetMethodArgumentType, sourceMethodArgumentIsBound, targetMethodCallStack)) {
            return getScore(targetMethodCallStack);
        }
        throw new NoScoreException();
    }

    private double getScore(List<MethodCallStackValue> methodCallStackValues) {
        return getScore(methodCallStackValues.get(methodCallStackValues.size() - 1));
    }

    protected double getScore(MethodCallStackValue methodCallStackValue) {
        return DOUBLE_SCORE;
    }

    private boolean tryLoadIndexedArgument(Class<?> targetMethodArgumentType,
                                           Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotations,
                                           List<MethodCallStackValue> targetMethodCallStack) {
        Argument argument = (Argument) targetMethodArgumentAnnotations.get(Argument.class);
        if (argument != null && typeCompatibilityEvaluator.isAssignable(
                targetMethodArgumentType, sourceMethodArgumentTypeNames.get(argument.value()))) {
            targetMethodCallStack.add(new ArgumentReference(argument.value()));
            return true;
        }
        return false;
    }

    private boolean tryLoadAllArguments(Class<?> targetMethodArgumentType,
                                        Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotations,
                                        List<MethodCallStackValue> targetMethodCallStack) {
        if (targetMethodArgumentAnnotations.get(AllArguments.class) != null
                && typeCompatibilityEvaluator.isAssignable(targetMethodArgumentType, sourceMethodArgumentTypeNames)) {
            targetMethodCallStack.add(new AllArgumentsReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadThis(Class<?> targetMethodArgumentType,
                                Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotations,
                                List<MethodCallStackValue> targetMethodCallStack) {
        if (targetMethodArgumentAnnotations.get(This.class) != null
                && typeCompatibilityEvaluator.isAssignable(targetMethodArgumentType, classTypeName)) {
            targetMethodCallStack.add(new ThisReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadClassId(Class<?> targetMethodArgumentType,
                                   Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotations,
                                   List<MethodCallStackValue> targetMethodCallStack) {
        if (targetMethodArgumentAnnotations.get(ClassId.class) != null
                && isUuidType(targetMethodArgumentType)) {
            targetMethodCallStack.add(new ClassIdReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadInstanceId(Class<?> targetMethodArgumentType,
                                      Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotations,
                                      List<MethodCallStackValue> targetMethodCallStack) {
        if (targetMethodArgumentAnnotations.get(InstanceId.class) != null
                && isUuidType(targetMethodArgumentType)) {
            targetMethodCallStack.add(new InstanceIdReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadPlainArgument(Class<?> targetMethodArgumentType,
                                         boolean[] sourceMethodArgumentIsBound,
                                         List<MethodCallStackValue> targetMethodCallStack) {
        int nextUnboundIndex = findNextUnboundIndex(targetMethodArgumentType, sourceMethodArgumentIsBound);
        if (nextUnboundIndex >= 0 && typeCompatibilityEvaluator.isAssignable(
                targetMethodArgumentType, sourceMethodArgumentTypeNames.get(nextUnboundIndex))) {
            targetMethodCallStack.add(new ArgumentReference(nextUnboundIndex));
            return true;
        }
        return false;
    }

    private int findNextUnboundIndex(Class<?> targetMethodArgumentType, boolean[] sourceMethodArgumentIsBound) {
        for (int index = 0; index < sourceMethodArgumentIsBound.length; index++) {
            if (!sourceMethodArgumentIsBound[index]) {
                if (typeCompatibilityEvaluator.isAssignable(targetMethodArgumentType, sourceMethodArgumentTypeNames.get(index)))
                    sourceMethodArgumentIsBound[index] = true;
                return index;
            }
        }
        return -1;
    }

    protected double scoreMethodNameEquality(Method targetMethod) {
        return sourceMethodName.equals(targetMethod.getName()) ? SINGLE_SCORE : NO_SCORE;
    }

    private boolean isUuidType(Class<?> type) {
        return type == UUID.class;
    }
}

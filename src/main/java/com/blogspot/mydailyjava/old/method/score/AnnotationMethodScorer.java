package com.blogspot.mydailyjava.old.method.score;


import com.blogspot.mydailyjava.old.method.MethodArgumentTypes;
import com.blogspot.mydailyjava.old.method.MethodReturnType;
import com.blogspot.mydailyjava.old.method.score.annotation.*;
import com.blogspot.mydailyjava.old.method.stack.*;
import com.blogspot.mydailyjava.old.type.TypeCompatibilityEvaluator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationMethodScorer implements MethodScorer {

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
    public MatchedMethod match(Method targetMethod) throws NoMatchException {
        assertReturnType(targetMethod.getReturnType());
        assertExceptions(targetMethod);
        Annotation[][] targetMethodArgumentAnnotations = targetMethod.getParameterAnnotations();
        Class<?>[] targetMethodArgumentType = targetMethod.getParameterTypes();
        List<MethodCallStackValue> targetMethodCallStack = new ArrayList<MethodCallStackValue>(targetMethodArgumentType.length);
        boolean[] sourceMethodArgumentIsBound = new boolean[sourceMethodArgumentTypeNames.size()];
        for (int i = 0; i < targetMethodArgumentType.length; i++) {
            assertParameters(targetMethodArgumentType[i],
                    makeAnnotationMap(targetMethodArgumentAnnotations[i]),
                    sourceMethodArgumentIsBound,
                    targetMethodCallStack);
        }
//        scoreMethodNameEquality(targetMethod);
        return new MatchedMethod(targetMethodCallStack);
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

    private void assertReturnType(Class<?> targetMethodReturnType) throws NoMatchException {
        if (!typeCompatibilityEvaluator.isAssignable(targetMethodReturnType, sourceMethodReturnType.getReturnTypeName())) {
            throw new NoMatchException();
        }
    }

    private void assertExceptions(Method targetMethod) throws NoMatchException {
        if (!typeCompatibilityEvaluator.isThrowable(targetMethod.getExceptionTypes(), sourceMethodExceptions)) {
            throw new NoMatchException();
        }
    }

    private void assertParameters(Class<?> targetMethodArgumentType,
                                  Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotation,
                                  boolean[] sourceMethodArgumentIsBound,
                                  List<MethodCallStackValue> targetMethodCallStack) throws NoMatchException {
        if (!(tryLoadIndexedArgument(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)
                || tryLoadThis(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)
                || tryLoadAllArguments(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)
                || tryLoadClassId(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)
                || tryLoadInstanceId(targetMethodArgumentType, targetMethodArgumentAnnotation, targetMethodCallStack)
                || tryLoadPlainArgument(targetMethodArgumentType, sourceMethodArgumentIsBound, targetMethodCallStack))) {
            throw new NoMatchException();
        }
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
                                        List<MethodCallStackValue> targetMethodCallStack) throws NoMatchException {
        if (targetMethodArgumentAnnotations.get(AllArguments.class) != null && assertClassAssignable(targetMethodArgumentType)) {
            targetMethodCallStack.add(new AllArgumentsReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadThis(Class<?> targetMethodArgumentType,
                                Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotations,
                                List<MethodCallStackValue> targetMethodCallStack) throws NoMatchException {
        if (targetMethodArgumentAnnotations.get(This.class) != null && assertClassAssignable(targetMethodArgumentType)) {
            targetMethodCallStack.add(new ThisReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadClassId(Class<?> targetMethodArgumentType,
                                   Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotations,
                                   List<MethodCallStackValue> targetMethodCallStack) {
        if (targetMethodArgumentAnnotations.get(ClassId.class) != null && assertUuidType(targetMethodArgumentType)) {
            targetMethodCallStack.add(new ClassIdReference());
            return true;
        }
        return false;
    }

    private boolean tryLoadInstanceId(Class<?> targetMethodArgumentType,
                                      Map<Class<? extends Annotation>, Annotation> targetMethodArgumentAnnotations,
                                      List<MethodCallStackValue> targetMethodCallStack) {
        if (targetMethodArgumentAnnotations.get(InstanceId.class) != null && assertUuidType(targetMethodArgumentType)) {
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

    private boolean assertUuidType(Class<?> type) {
        if (!(type == UUID.class)) {
            throw new IllegalStateException();
        }
        return true;
    }

    private boolean assertClassAssignable(Class<?> targetMethodArgumentType) throws NoMatchException {
        if (!typeCompatibilityEvaluator.isAssignable(targetMethodArgumentType, classTypeName)) {
            throw new NoMatchException();
        }
        return true;
    }
}

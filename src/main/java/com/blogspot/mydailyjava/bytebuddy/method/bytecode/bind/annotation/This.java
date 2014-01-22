package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.util.Iterator;

public @interface This {

    static enum Binder implements AnnotationDrivenBinder.ArgumentBinder<This> {
        INSTANCE;

        @Override
        public Class<This> getHandledType() {
            return This.class;
        }

        @Override
        public IdentifiedBinding<?> bind(This annotation,
                                         int targetParameterIndex,
                                         MethodDescription source,
                                         MethodDescription target,
                                         TypeDescription typeDescription,
                                         Assigner assigner) {
            if(source.isStatic()) {
                return IdentifiedBinding.makeIllegal();
            }
            Class<?> targetType = target.getParameterTypes()[targetParameterIndex];
            boolean runtimeType = isRuntimeType(target, targetParameterIndex);
            Assignment assignment = assigner.assign(typeDescription.getSuperClass(), targetType, runtimeType);
            Iterator<Class<?>> interfaces = typeDescription.getInterfaces().iterator();
            while(!assignment.isAssignable() && interfaces.hasNext()) {
                assignment = assigner.assign(interfaces.next(), targetType, runtimeType);
            }
            return IdentifiedBinding.makeAnonymous(MethodArgument.OBJECT_REFERENCE.loadFromIndex(0));
        }

        private static boolean isRuntimeType(MethodDescription methodDescription, int parameterIndex) {
            for (Annotation annotation : methodDescription.getParameterAnnotations()[parameterIndex]) {
                if (annotation.annotationType() == RuntimeType.class) {
                    return true;
                }
            }
            return false;
        }
    }
}

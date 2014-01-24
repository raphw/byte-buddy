package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;

import java.lang.annotation.*;
import java.util.Iterator;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
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
            Class<?> targetType = target.getParameterTypes()[targetParameterIndex];
            if (targetType.isPrimitive()) {
                throw new IllegalStateException(String.format("The %d. argument of %s is a primitive type " +
                        "and can never be bound to an instance", targetParameterIndex, target));
            } else if(targetType.isArray()) {
                throw new IllegalStateException(String.format("The %d. argument of %s is an array type " +
                        "and can never be bound to an instance", targetParameterIndex, target));
            } else if (source.isStatic()) {
                return IdentifiedBinding.makeIllegal();
            }
            boolean runtimeType = RuntimeType.Verifier.check(target, targetParameterIndex);
            Assignment assignment = assigner.assign(typeDescription.getSuperClass(), targetType, runtimeType);
            Iterator<Class<?>> interfaces = typeDescription.getInterfaces().iterator();
            while (!assignment.isValid() && interfaces.hasNext()) {
                assignment = assigner.assign(interfaces.next(), targetType, runtimeType);
            }
            return IdentifiedBinding.makeAnonymous(new Assignment.Compound(MethodArgument.OBJECT_REFERENCE.loadFromIndex(0), assignment));
        }
    }
}

package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.TypeProxy;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface Super {

    static enum Instantiation {
        CONSTRUCTOR {
            @Override
            protected StackManipulation proxyFor(TypeDescription parameterType,
                                                 TypeDescription instrumentedType,
                                                 Super annotation) {
                List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(annotation.constructorArguments().length);
                for(Class<?> constructorParameter : annotation.constructorArguments()) {
                    typeDescriptions.add(constructorParameter == TargetType.class
                            ? TargetType.DESCRIPTION
                            : new TypeDescription.ForLoadedType(constructorParameter));
                }
                return new TypeProxy.ByConstructor(parameterType, instrumentedType, typeDescriptions, annotation.ignoreFinalizer());
            }
        },
        UNSAFE {
            @Override
            protected StackManipulation proxyFor(TypeDescription parameterType,
                                                 TypeDescription instrumentedType,
                                                 Super annotation) {
                return new TypeProxy.ByReflectionFactory(parameterType, instrumentedType, annotation.ignoreFinalizer());
            }
        };

        protected abstract StackManipulation proxyFor(TypeDescription parameterType,
                                                      TypeDescription instrumentedType,
                                                      Super annotation);
    }

    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Super> {
        INSTANCE;

        @Override
        public Class<Super> getHandledType() {
            return Super.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(Super annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               TypeDescription instrumentedType,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getParameterTypes().get(targetParameterIndex);
            if (source.isStatic() || !instrumentedType.isAssignableTo(parameterType)) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(annotation
                        .strategy()
                        .proxyFor(parameterType, instrumentedType, annotation));
            }
        }
    }

    Instantiation strategy() default Instantiation.CONSTRUCTOR;

    boolean ignoreFinalizer() default true;

    Class<?>[] constructorArguments() default {};
}

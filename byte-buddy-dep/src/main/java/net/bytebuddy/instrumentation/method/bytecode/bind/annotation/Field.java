package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isStatic;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.not;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

public @interface Field {

    static final String BEAN_PROPERTY = "";

    boolean serializableProxy() default false;

    String value() default BEAN_PROPERTY;

    static class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Field> {

        private final MethodDescription getterMethod;

        private final MethodDescription setterMethod;

        protected Binder(MethodDescription getterMethod, MethodDescription setterMethod) {
            this.getterMethod = getterMethod;
            this.setterMethod = setterMethod;
        }

        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Field> install(Class<?> getterType,
                                                                                        Class<?> setterType) {
            return install(new TypeDescription.ForLoadedType(nonNull(getterType)), new TypeDescription.ForLoadedType(nonNull(setterType)));
        }

        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Field> install(TypeDescription getterType,
                                                                                        TypeDescription setterType) {
            MethodDescription getterMethod = onlyMethod(nonNull(getterType));
            if (!getterMethod.getReturnType().represents(Object.class)) {
                throw new IllegalArgumentException(getterMethod + " must take a single Object-typed parameter");
            } else if (getterMethod.getParameterTypes().size() != 0) {
                throw new IllegalArgumentException(getterMethod + " must not declare parameters");
            }
            MethodDescription setterMethod = onlyMethod(nonNull(setterType));
            if (!setterMethod.getReturnType().represents(void.class)) {
                throw new IllegalArgumentException(setterMethod + " must return void");
            } else if (setterMethod.getParameterTypes().size() != 1 || !setterMethod.getParameterTypes().get(0).represents(Object.class)) {
                throw new IllegalArgumentException(setterMethod + " must declare a single Object-typed parameters");
            }
            return new Binder(getterMethod, setterMethod);
        }

        private static MethodDescription onlyMethod(TypeDescription typeDescription) {
            if (!typeDescription.isInterface()) {
                throw new IllegalArgumentException(typeDescription + " is not an interface");
            } else if (typeDescription.getInterfaces().size() > 0) {
                throw new IllegalArgumentException(typeDescription + " must not extend other interfaces");
            } else if (!typeDescription.isPublic()) {
                throw new IllegalArgumentException(typeDescription + " is mot public");
            }
            MethodList methodCandidates = typeDescription.getDeclaredMethods().filter(not(isStatic()));
            if (methodCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " must declare exactly one non-static method");
            }
            return methodCandidates.getOnly();
        }

        @Override
        public Class<Field> getHandledType() {
            return Field.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Field> annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            return null;
        }
    }
}

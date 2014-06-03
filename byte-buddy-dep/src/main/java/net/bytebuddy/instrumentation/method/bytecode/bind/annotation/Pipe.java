package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;

import java.lang.annotation.*;
import java.util.LinkedHashMap;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Pipe {

    static class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> {

        private final MethodDescription methodDescription;

        private Binder(MethodDescription methodDescription) {
            this.methodDescription = methodDescription;
        }

        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> compile(Class<?> type) {
            TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
            MethodList methodList = typeDescription.getDeclaredMethods().filter(not(isStatic()));
            if (!typeDescription.isInterface()
                    || !typeDescription.isPublic()
                    || typeDescription.getInterfaces().size() > 0
                    || methodList.size() != 1
                    || methodList.filter(takesArguments(1)).size() == 0
                    || methodList.getOnly().getParameterTypes().get(0).isPrimitive()) {
                throw new IllegalArgumentException(type + " does not represent a plain, public interface with one" +
                        "non-static method with a single non-primitive argument");
            }
            return new Binder(methodList.getOnly());
        }

        @Override
        public Class<Pipe> getHandledType() {
            return Pipe.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(Pipe annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            return null;
        }

        private static class ForwardingType implements AuxiliaryType {

            private static final String FIELD_NAME_PREFIX = "argument";

            private final MethodDescription targetMethod;

            private ForwardingType(MethodDescription targetMethod) {
                this.targetMethod = targetMethod;
            }

            private static LinkedHashMap<String, TypeDescription> extractFields(MethodDescription methodDescription) {
                TypeList parameterTypes = methodDescription.getParameterTypes();
                LinkedHashMap<String, TypeDescription> typeDescriptions = new LinkedHashMap<String, TypeDescription>(parameterTypes.size());
                int currentIndex = 0;
                for (TypeDescription parameterType : parameterTypes) {
                    typeDescriptions.put(fieldName(currentIndex++), parameterType);
                }
                return typeDescriptions;
            }

            private static String fieldName(int index) {
                return String.format("%s%d", FIELD_NAME_PREFIX, index);
            }

            @Override
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                extractFields(targetMethod);
                return null;
            }

            private class MethodCall implements Instrumentation {

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public ByteCodeAppender appender(Target instrumentationTarget) {
                    return null;
                }
            }
        }

    }
}

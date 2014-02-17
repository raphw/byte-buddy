package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public interface InstrumentedType extends TypeDescription {

    static abstract class AbstractInstrumentedType extends AbstractTypeDescription implements InstrumentedType {

        protected class FieldToken extends FieldDescription.AbstractFieldDescription {

            private final String name;
            private final TypeDescription fieldType;
            private final int modifiers;
            private final boolean synthetic;

            public FieldToken(String name, TypeDescription fieldType, int modifiers, boolean synthetic) {
                this.name = name;
                this.fieldType = fieldType;
                this.modifiers = modifiers;
                this.synthetic = synthetic;
            }

            private FieldToken(FieldDescription fieldDescription) {
                name = fieldDescription.getName();
                fieldType = withSubstitutedSelfReference(fieldDescription.getFieldType());
                modifiers = fieldDescription.getModifiers();
                synthetic = fieldDescription.isSynthetic();
            }

            @Override
            public TypeDescription getFieldType() {
                return fieldType;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return false;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescriptor() {
                return fieldType.getDescriptor();
            }

            @Override
            public TypeDescription getDeclaringType() {
                return AbstractInstrumentedType.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }

            @Override
            public boolean isSynthetic() {
                return synthetic;
            }
        }

        protected class MethodToken extends MethodDescription.AbstractMethodDescription {

            private final String internalName;
            private final TypeDescription returnType;
            private final List<TypeDescription> parameterTypes;
            private final int modifiers;
            private final boolean synthetic;

            public MethodToken(String internalName,
                               TypeDescription returnType,
                               List<? extends TypeDescription> parameterTypes,
                               int modifiers,
                               boolean synthetic) {
                this.internalName = internalName;
                this.returnType = returnType;
                this.parameterTypes = new ArrayList<TypeDescription>(parameterTypes);
                this.modifiers = modifiers;
                this.synthetic = synthetic;
            }

            private MethodToken(MethodDescription methodDescription) {
                this.internalName = methodDescription.getInternalName();
                this.returnType = withSubstitutedSelfReference(methodDescription.getReturnType());
                this.parameterTypes = new ArrayList<TypeDescription>(methodDescription.getParameterTypes().size());
                for (TypeDescription typeDescription : methodDescription.getParameterTypes()) {
                    parameterTypes.add(withSubstitutedSelfReference(typeDescription));
                }
                this.modifiers = methodDescription.getModifiers();
                this.synthetic = methodDescription.isSynthetic();
            }

            @Override
            public TypeDescription getReturnType() {
                return returnType;
            }

            @Override
            public TypeList getParameterTypes() {
                return new TypeList.Explicit(parameterTypes);
            }

            @Override
            public Annotation[][] getParameterAnnotations() {
                return new Annotation[0][0];
            }

            @Override
            public TypeList getExceptionTypes() {
                return new TypeList.Empty();
            }

            @Override
            public boolean isVarArgs() {
                return false;
            }

            @Override
            public boolean isConstructor() {
                return CONSTRUCTOR_INTERNAL_NAME.equals(internalName);
            }

            @Override
            public boolean isBridge() {
                return false;
            }

            @Override
            public boolean represents(Method method) {
                return false;
            }

            @Override
            public boolean represents(Constructor<?> constructor) {
                return false;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return false;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public String getName() {
                return isConstructor() ? getDeclaringType().getName() : getInternalName();
            }

            @Override
            public String getInternalName() {
                return internalName;
            }

            @Override
            public TypeDescription getDeclaringType() {
                return AbstractInstrumentedType.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }

            @Override
            public boolean isSynthetic() {
                return synthetic;
            }
        }

        private TypeDescription withSubstitutedSelfReference(TypeDescription typeDescription) {
            return typeDescription instanceof InstrumentedType ? this : typeDescription;
        }

        protected final List<FieldDescription> fieldDescriptions;
        protected final List<MethodDescription> methodDescriptions;

        protected AbstractInstrumentedType(List<? extends FieldDescription> fieldDescriptions,
                                           List<? extends MethodDescription> methodDescriptions) {
            this.fieldDescriptions = new ArrayList<FieldDescription>(fieldDescriptions.size());
            for (FieldDescription fieldDescription : fieldDescriptions) {
                this.fieldDescriptions.add(new FieldToken(fieldDescription));
            }
            this.methodDescriptions = new ArrayList<MethodDescription>(methodDescriptions.size());
            for (MethodDescription methodDescription : methodDescriptions) {
                this.methodDescriptions.add(new MethodToken(methodDescription));
            }
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public boolean isInstance(Object object) {
            return isAssignableFrom(object.getClass());
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            return isAssignableFrom(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            return isAssignableTo(new ForLoadedType(type));
        }

        @Override
        public boolean represents(Class<?> type) {
            return type.getName().equals(getName());
        }

        @Override
        public boolean isInterface() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public TypeDescription getComponentType() {
            return null;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public boolean isAnnotation() {
            return false;
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return null;
        }

        @Override
        public TypeDescription getEnclosingClass() {
            return null;
        }

        @Override
        public String getSimpleName() {
            return getName().substring(getPackageName().length(), getName().length());
        }

        @Override
        public String getCanonicalName() {
            return getName();
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public boolean isLocalClass() {
            return false;
        }

        @Override
        public boolean isMemberClass() {
            return false;
        }

        @Override
        public FieldList getDeclaredFields() {
            return new FieldList.Explicit(fieldDescriptions);
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.Explicit(methodDescriptions);
        }

        @Override
        public String getPackageName() {
            int packageIndex = getName().lastIndexOf('.');
            if (packageIndex == -1) {
                return "";
            } else {
                return getName().substring(0, packageIndex);
            }
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        @Override
        public String getDescriptor() {
            return "L" + getInternalName() + ";";
        }

        @Override
        public TypeDescription getDeclaringType() {
            return null;
        }
    }

    InstrumentedType withField(String name,
                               TypeDescription fieldType,
                               int modifiers,
                               boolean synthetic);

    InstrumentedType withMethod(String name,
                                TypeDescription returnType,
                                List<? extends TypeDescription> parameterTypes,
                                int modifiers,
                                boolean synthetic);
}

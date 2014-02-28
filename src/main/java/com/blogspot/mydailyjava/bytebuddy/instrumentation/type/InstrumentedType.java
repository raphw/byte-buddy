package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementations of this interface represent an instrumented type that is subject to change. Implementations
 * should however be immutable and return new instance when their mutator methods are called.
 */
public interface InstrumentedType extends TypeDescription {

    /**
     * A mask for modifiers that represent visibility.
     */
    static final int VISIBILITY_MODIFIER_MASK = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    /**
     * A mask for modifiers that are represented by types and members.
     */
    static final int GENERAL_MODIFIER_MASK = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_DEPRECATED;

    /**
     * A mask for modifiers that represents types.
     */
    static final int TYPE_MODIFIER_MASK = VISIBILITY_MODIFIER_MASK | GENERAL_MODIFIER_MASK
            | Modifier.ABSTRACT | Modifier.FINAL | Modifier.INTERFACE | Modifier.STRICT | Opcodes.ACC_ANNOTATION
            | Opcodes.ACC_ENUM | Opcodes.ACC_STRICT | Opcodes.ACC_SUPER;

    /**
     * A mask for modifiers that represents type members.
     */
    static final int MEMBER_MODIFIER_MASK = VISIBILITY_MODIFIER_MASK | TYPE_MODIFIER_MASK
            | Modifier.FINAL | Modifier.SYNCHRONIZED;

    /**
     * A mask for modifiers that represents fields.
     */
    static final int FIELD_MODIFIER_MASK = MEMBER_MODIFIER_MASK | Modifier.TRANSIENT | Modifier.VOLATILE;

    /**
     * A mask for modifiers that represents modifiers and constructors.
     */
    static final int METHOD_MODIFIER_MASK = MEMBER_MODIFIER_MASK | Modifier.ABSTRACT | Modifier.SYNCHRONIZED
            | Modifier.NATIVE | Modifier.STRICT | Opcodes.ACC_BRIDGE | Opcodes.ACC_VARARGS;

    /**
     * An abstract base implementation of an instrumented type.
     */
    static abstract class AbstractBase extends AbstractTypeDescription implements InstrumentedType {

        /**
         * An implementation of a new field for the enclosing instrumented type.
         */
        protected class FieldToken extends FieldDescription.AbstractFieldDescription {

            private final String name;
            private final TypeDescription fieldType;
            private final int modifiers;

            /**
             * Creates a new field for the enclosing instrumented type.
             *
             * @param name      The internalName of the field.
             * @param fieldType The type description of the field.
             * @param modifiers The modifiers of the field.
             */
            public FieldToken(String name, TypeDescription fieldType, int modifiers) {
                this.name = name;
                this.fieldType = fieldType;
                this.modifiers = modifiers;
            }

            private FieldToken(String typeInternalName, FieldDescription fieldDescription) {
                name = fieldDescription.getName();
                fieldType = withSubstitutedSelfReference(typeInternalName, fieldDescription.getFieldType());
                modifiers = fieldDescription.getModifiers();
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
                return AbstractBase.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }
        }

        /**
         * An implementation of a new method or constructor for the enclosing instrumented type.
         */
        protected class MethodToken extends MethodDescription.AbstractMethodDescription {

            private final String internalName;
            private final TypeDescription returnType;
            private final List<TypeDescription> parameterTypes;
            private final int modifiers;

            /**
             * Creates a new method or constructor for the enclosing instrumented type.
             *
             * @param internalName   The internal internalName of the method or constructor.
             * @param returnType     A description of the return type of this method.
             * @param parameterTypes A list of descriptions of the parameter types of this method.
             * @param modifiers      The modifiers of this method.
             */
            public MethodToken(String internalName,
                               TypeDescription returnType,
                               List<? extends TypeDescription> parameterTypes,
                               int modifiers) {
                this.internalName = internalName;
                this.returnType = returnType;
                this.parameterTypes = new ArrayList<TypeDescription>(parameterTypes);
                this.modifiers = modifiers;
            }

            private MethodToken(String typeInternalName, MethodDescription methodDescription) {
                internalName = methodDescription.getInternalName();
                returnType = withSubstitutedSelfReference(typeInternalName, methodDescription.getReturnType());
                parameterTypes = new ArrayList<TypeDescription>(methodDescription.getParameterTypes().size());
                for (TypeDescription typeDescription : methodDescription.getParameterTypes()) {
                    parameterTypes.add(withSubstitutedSelfReference(typeInternalName, typeDescription));
                }
                modifiers = methodDescription.getModifiers();
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
            public boolean isConstructor() {
                return CONSTRUCTOR_INTERNAL_NAME.equals(internalName);
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
                return AbstractBase.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }
        }

        private TypeDescription withSubstitutedSelfReference(String instrumentedTypeName, TypeDescription typeDescription) {
            return typeDescription.getInternalName().equals(instrumentedTypeName) ? this : typeDescription;
        }

        /**
         * The type initializer for this instrumented type.
         */
        protected final TypeInitializer typeInitializer;

        /**
         * A list of field descriptions registered for this instrumented type.
         */
        protected final List<FieldDescription> fieldDescriptions;

        /**
         * A list of method descriptions registered for this instrumented type.
         */
        protected final List<MethodDescription> methodDescriptions;

        /**
         * Creates a new instrumented type with a no-op type initializer and without registered fields or methods.
         */
        protected AbstractBase() {
            typeInitializer = TypeInitializer.NoOp.INSTANCE;
            fieldDescriptions = Collections.emptyList();
            methodDescriptions = Collections.emptyList();
        }

        /**
         * Creates a new instrumented type with the given type initializer and field and methods. All field and method
         * descriptions will be replaced by new instances where type descriptions with the internalName of this type as given by
         * {@code typeInternalName} are replaced by references to {@code this}.
         *
         * @param typeInitializer    A type initializer for this instrumented type.
         * @param typeInternalName   The internal internalName of this instrumented type.
         * @param fieldDescriptions  A list of field descriptions for this instrumented type.
         * @param methodDescriptions A list of method descriptions for this instrumented type.
         */
        protected AbstractBase(TypeInitializer typeInitializer,
                               String typeInternalName,
                               List<? extends FieldDescription> fieldDescriptions,
                               List<? extends MethodDescription> methodDescriptions) {
            this.typeInitializer = typeInitializer;
            this.fieldDescriptions = new ArrayList<FieldDescription>(fieldDescriptions.size());
            for (FieldDescription fieldDescription : fieldDescriptions) {
                this.fieldDescriptions.add(new FieldToken(typeInternalName, fieldDescription));
            }
            this.methodDescriptions = new ArrayList<MethodDescription>(methodDescriptions.size());
            for (MethodDescription methodDescription : methodDescriptions) {
                this.methodDescriptions.add(new MethodToken(typeInternalName, methodDescription));
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

        @Override
        public TypeInitializer getTypeInitializer() {
            return typeInitializer;
        }
    }

    /**
     * Creates a new instrumented type that includes a new field.
     *
     * @param name      The internalName of the new field.
     * @param fieldType A description of the type of the new field.
     * @param modifiers The modifier of the new field.
     * @return A new instrumented type that is equal to this instrumented type but with the additional field.
     */
    InstrumentedType withField(String name,
                               TypeDescription fieldType,
                               int modifiers);

    /**
     * Creates a new instrumented type that includes a new method or constructor.
     *
     * @param name           The internalName of the new field.
     * @param returnType     A description of the return type of the new field.
     * @param parameterTypes A list of descriptions of the parameter types.
     * @param modifiers      The modifier of the new field.
     * @return A new instrumented type that is equal to this instrumented type but with the additional field.
     */
    InstrumentedType withMethod(String name,
                                TypeDescription returnType,
                                List<? extends TypeDescription> parameterTypes,
                                int modifiers);

    /**
     * Creates a new instrumented type that includes the given
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer}.
     *
     * @param typeInitializer The type initializer to include.
     * @return A new instrumented type that is equal to this instrumented type but with the additional type initializer.
     */
    InstrumentedType withInitializer(TypeInitializer typeInitializer);

    /**
     * Returns the {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer}s that were registered
     * for this instrumented type.
     *
     * @return The registered type initializers for this instrumented type.
     */
    TypeInitializer getTypeInitializer();
}

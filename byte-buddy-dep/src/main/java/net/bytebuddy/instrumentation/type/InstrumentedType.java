package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementations of this interface represent an instrumented type that is subject to change. Implementations
 * should however be immutable and return new instance when their mutator methods are called.
 */
public interface InstrumentedType extends TypeDescription {

    /**
     * Creates a new instrumented type that includes a new field.
     *
     * @param internalName The internal name of the new field.
     * @param fieldType    A description of the type of the new field.
     * @param modifiers    The modifier of the new field.
     * @return A new instrumented type that is equal to this instrumented type but with the additional field.
     */
    InstrumentedType withField(String internalName,
                               TypeDescription fieldType,
                               int modifiers);

    /**
     * Creates a new instrumented type that includes a new method or constructor.
     *
     * @param internalName   The internal name of the new field.
     * @param returnType     A description of the return type of the new field.
     * @param parameterTypes A list of descriptions of the parameter types.
     * @param exceptionTypes A list of descriptions of the exception types that are declared by this method.
     * @param modifiers      The modifier of the new field.
     * @return A new instrumented type that is equal to this instrumented type but with the additional field.
     */
    InstrumentedType withMethod(String internalName,
                                TypeDescription returnType,
                                List<? extends TypeDescription> parameterTypes,
                                List<? extends TypeDescription> exceptionTypes,
                                int modifiers);

    /**
     * Creates a new instrumented type that includes the given
     * {@link net.bytebuddy.instrumentation.LoadedTypeInitializer}.
     *
     * @param loadedTypeInitializer The type initializer to include.
     * @return A new instrumented type that is equal to this instrumented type but with the additional type initializer.
     */
    InstrumentedType withInitializer(LoadedTypeInitializer loadedTypeInitializer);

    /**
     * Creates a new instrumented type that executes the given initializer in the instrumented type's
     * type initializer.
     *
     * @param stackManipulation The stack manipulation to execute.
     * @return A new instrumented type that is equal to this instrumented type but with the given stack manipulation
     * attached to its type initializer.
     */
    InstrumentedType withInitializer(StackManipulation stackManipulation);

    /**
     * Returns the {@link net.bytebuddy.instrumentation.LoadedTypeInitializer}s that were registered
     * for this instrumented type.
     *
     * @return The registered loaded type initializers for this instrumented type.
     */
    LoadedTypeInitializer getLoadedTypeInitializer();

    /**
     * Returns this instrumented type's type initializer.
     *
     * @return This instrumented type's type initializer.
     */
    TypeInitializer getTypeInitializer();

    /**
     * Creates a <i>compressed</i> version of this instrumented type which only needs to fulfil the
     * {@link net.bytebuddy.instrumentation.type.TypeDescription} interface. This allows
     * for a potential compression of the representation of {@code this} instrumented type.
     *
     * @return A (potentially) compressed version of {@code this} instrumented type.
     */
    TypeDescription detach();

    /**
     * A type initializer is responsible for defining a type's static initialization block.
     */
    static interface TypeInitializer {

        /**
         * A simple, defined type initializer that executes a given
         * {@link net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation}.
         */
        static class Simple implements TypeInitializer {

            /**
             * The stack manipulation to apply within the type initializer.
             */
            private final StackManipulation stackManipulation;

            /**
             * Creates a new simple type initializer.
             *
             * @param stackManipulation The stack manipulation to apply within the type initializer.
             */
            public Simple(StackManipulation stackManipulation) {
                this.stackManipulation = stackManipulation;
            }

            @Override
            public boolean isDefined() {
                return true;
            }

            @Override
            public TypeInitializer expandWith(StackManipulation stackManipulation) {
                return new Simple(new StackManipulation.Compound(this.stackManipulation, stackManipulation));
            }

            @Override
            public StackManipulation getStackManipulation() {
                return stackManipulation;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && stackManipulation.equals(((Simple) other).stackManipulation);
            }

            @Override
            public int hashCode() {
                return stackManipulation.hashCode();
            }

            @Override
            public String toString() {
                return "InstrumentedType.TypeInitializer.Simple{" +
                        "stackManipulation=" + stackManipulation +
                        '}';
            }
        }

        /**
         * Canonical implementation of a non-defined type initializer.
         */
        static enum None implements TypeInitializer {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isDefined() {
                return false;
            }

            @Override
            public TypeInitializer expandWith(StackManipulation stackManipulation) {
                return new Simple(stackManipulation);
            }

            @Override
            public StackManipulation getStackManipulation() {
                throw new IllegalStateException("Cannot execute a non-defined type initializer");
            }
        }

        /**
         * Indicates if this type initializer is defined.
         *
         * @return {@code true} if this type initializer is defined.
         */
        boolean isDefined();

        /**
         * Expands this type initializer with a stack manipulation.
         *
         * @param stackManipulation The stack manipulation to apply within the type initializer.
         * @return A defined type initializer.
         */
        TypeInitializer expandWith(StackManipulation stackManipulation);

        /**
         * Returns the stack manipulation of this type initializer. This method must only be called
         * if this type initializer is defined.
         *
         * @return The stack manipulation of this type initializer.
         */
        StackManipulation getStackManipulation();
    }

    /**
     * An abstract base implementation of an instrumented type.
     */
    abstract static class AbstractBase extends AbstractTypeDescription.OfSimpleType implements InstrumentedType {

        /**
         * The loaded type initializer for this instrumented type.
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

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
         * Creates a new instrumented type with a no-op loaded type initializer and without registered fields or
         * methods.
         */
        protected AbstractBase() {
            loadedTypeInitializer = LoadedTypeInitializer.NoOp.INSTANCE;
            typeInitializer = TypeInitializer.None.INSTANCE;
            fieldDescriptions = Collections.emptyList();
            methodDescriptions = Collections.emptyList();
        }

        /**
         * Creates a new instrumented type with the given loaded type initializer and field and methods. All field and
         * method descriptions will be replaced by new instances where type descriptions with the internalName of this
         * type as given by {@code typeInternalName} are replaced by references to {@code this}.
         *
         * @param loadedTypeInitializer A loaded type initializer for this instrumented type.
         * @param typeInitializer       A type initializer for this instrumented type.
         * @param typeName              The non-internal name of this instrumented type.
         * @param fieldDescriptions     A list of field descriptions for this instrumented type.
         * @param methodDescriptions    A list of method descriptions for this instrumented type.
         */
        protected AbstractBase(LoadedTypeInitializer loadedTypeInitializer,
                               TypeInitializer typeInitializer,
                               String typeName,
                               List<? extends FieldDescription> fieldDescriptions,
                               List<? extends MethodDescription> methodDescriptions) {
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.typeInitializer = typeInitializer;
            this.fieldDescriptions = new ArrayList<FieldDescription>(fieldDescriptions.size());
            for (FieldDescription fieldDescription : fieldDescriptions) {
                this.fieldDescriptions.add(new FieldToken(typeName, fieldDescription));
            }
            this.methodDescriptions = new ArrayList<MethodDescription>(methodDescriptions.size());
            for (MethodDescription methodDescription : methodDescriptions) {
                this.methodDescriptions.add(new MethodToken(typeName, methodDescription));
            }
        }

        /**
         * Substitutes an <i>outdated</i> reference to the instrumented type with a reference to <i>this</i>.
         *
         * @param typeName        The non-internal name of this instrumented type.
         * @param typeDescription The type description to be checked to represent this instrumented type.
         * @return This type, if the type description represents the name of the instrumented type or the given
         * instrumented type if this is not the case.
         */
        private TypeDescription withSubstitutedSelfReference(String typeName, TypeDescription typeDescription) {
            return typeDescription.getName().equals(typeName) ? this : typeDescription;
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return null;
        }

        @Override
        public TypeDescription getEnclosingType() {
            return null;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return null;
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public String getCanonicalName() {
            return getName();
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
        public LoadedTypeInitializer getLoadedTypeInitializer() {
            return loadedTypeInitializer;
        }

        @Override
        public TypeInitializer getTypeInitializer() {
            return typeInitializer;
        }

        /**
         * An implementation of a new field for the enclosing instrumented type.
         */
        protected class FieldToken extends FieldDescription.AbstractFieldDescription {

            /**
             * The name of the field token.
             */
            private final String name;

            /**
             * The type of the field.
             */
            private final TypeDescription fieldType;

            /**
             * The modifiers of the field.
             */
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

            /**
             * Creates a new field for the enclosing instrumented type.
             *
             * @param typeName         The non-internal name of the enclosing instrumented type.
             * @param fieldDescription The field description to copy.
             */
            private FieldToken(String typeName, FieldDescription fieldDescription) {
                name = fieldDescription.getName();
                fieldType = withSubstitutedSelfReference(typeName, fieldDescription.getFieldType());
                modifiers = fieldDescription.getModifiers();
            }

            @Override
            public TypeDescription getFieldType() {
                return fieldType;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Empty();
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

            /**
             * The internal name of the represented method.
             */
            private final String internalName;

            /**
             * The return type of the represented method.
             */
            private final TypeDescription returnType;

            /**
             * The parameter types of the represented method.
             */
            private final List<TypeDescription> parameterTypes;

            /**
             * The exception types of the represented method.
             */
            private final List<TypeDescription> exceptionTypes;

            /**
             * The modifiers of the represented method.
             */
            private final int modifiers;

            /**
             * Creates a new method or constructor for the enclosing instrumented type.
             *
             * @param internalName   The internal internalName of the method or constructor.
             * @param returnType     A description of the return type of this method.
             * @param parameterTypes A list of descriptions of the parameter types of this method.
             * @param exceptionTypes A list of descriptions of the exception types that are declared by this method.
             * @param modifiers      The modifiers of this method.
             */
            public MethodToken(String internalName,
                               TypeDescription returnType,
                               List<? extends TypeDescription> parameterTypes,
                               List<? extends TypeDescription> exceptionTypes,
                               int modifiers) {
                this.internalName = internalName;
                this.returnType = returnType;
                this.parameterTypes = new ArrayList<TypeDescription>(parameterTypes);
                this.exceptionTypes = new ArrayList<TypeDescription>(exceptionTypes);
                this.modifiers = modifiers;
            }

            /**
             * Creates a new method or constructor for the enclosing instrumented type.
             *
             * @param typeName          The non-internal name of the enclosing instrumented type.
             * @param methodDescription The method description to copy.
             */
            private MethodToken(String typeName, MethodDescription methodDescription) {
                internalName = methodDescription.getInternalName();
                returnType = withSubstitutedSelfReference(typeName, methodDescription.getReturnType());
                parameterTypes = new ArrayList<TypeDescription>(methodDescription.getParameterTypes().size());
                for (TypeDescription typeDescription : methodDescription.getParameterTypes()) {
                    parameterTypes.add(withSubstitutedSelfReference(typeName, typeDescription));
                }
                exceptionTypes = new ArrayList<TypeDescription>(methodDescription.getExceptionTypes().size());
                for (TypeDescription typeDescription : methodDescription.getExceptionTypes()) {
                    exceptionTypes.add(withSubstitutedSelfReference(typeName, typeDescription));
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
            public TypeList getExceptionTypes() {
                return new TypeList.Explicit(exceptionTypes);
            }

            @Override
            public boolean isConstructor() {
                return CONSTRUCTOR_INTERNAL_NAME.equals(internalName);
            }

            @Override
            public boolean isTypeInitializer() {
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
            public List<AnnotationList> getParameterAnnotations() {
                return AnnotationList.Empty.asList(parameterTypes.size());
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Empty();
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

            @Override
            public Object getDefaultValue() {
                return null;
            }
        }
    }
}

package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.description.type.generic.TypeVariableSource;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.MethodVisitor;

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
                               GenericTypeDescription fieldType,
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
                                GenericTypeDescription returnType,
                                List<? extends GenericTypeDescription> parameterTypes,
                                List<? extends GenericTypeDescription> exceptionTypes,
                                int modifiers);

    /**
     * Creates a new instrumented type that includes the given {@link net.bytebuddy.implementation.LoadedTypeInitializer}.
     *
     * @param loadedTypeInitializer The type initializer to include.
     * @return A new instrumented type that is equal to this instrumented type but with the additional type initializer.
     */
    InstrumentedType withInitializer(LoadedTypeInitializer loadedTypeInitializer);

    /**
     * Creates a new instrumented type that executes the given initializer in the instrumented type's
     * type initializer.
     *
     * @param byteCodeAppender The byte code to add to the type initializer.
     * @return A new instrumented type that is equal to this instrumented type but with the given stack manipulation
     * attached to its type initializer.
     */
    InstrumentedType withInitializer(ByteCodeAppender byteCodeAppender);

    /**
     * Returns the {@link net.bytebuddy.implementation.LoadedTypeInitializer}s that were registered
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
     * A type initializer is responsible for defining a type's static initialization block.
     */
    interface TypeInitializer extends ByteCodeAppender {

        /**
         * Indicates if this type initializer is defined.
         *
         * @return {@code true} if this type initializer is defined.
         */
        boolean isDefined();

        /**
         * Expands this type initializer with another byte code appender. For this to be possible, this type initializer must
         * be defined.
         *
         * @param byteCodeAppender The byte code appender to apply within the type initializer.
         * @return A defined type initializer.
         */
        TypeInitializer expandWith(ByteCodeAppender byteCodeAppender);

        /**
         * Returns this type initializer with an ending return statement. For this to be possible, this type initializer must
         * be defined.
         *
         * @return This type initializer with an ending return statement.
         */
        ByteCodeAppender withReturn();

        /**
         * Canonical implementation of a non-defined type initializer.
         */
        enum None implements TypeInitializer {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isDefined() {
                return false;
            }

            @Override
            public TypeInitializer expandWith(ByteCodeAppender byteCodeAppender) {
                return new TypeInitializer.Simple(byteCodeAppender);
            }

            @Override
            public ByteCodeAppender withReturn() {
                throw new IllegalStateException("Cannot append return to non-defined type initializer");
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                throw new IllegalStateException("Cannot apply a non-defined type initializer");
            }

            @Override
            public String toString() {
                return "InstrumentedType.TypeInitializer.None." + name();
            }
        }

        /**
         * A simple, defined type initializer that executes a given {@link net.bytebuddy.implementation.bytecode.ByteCodeAppender}.
         */
        class Simple implements TypeInitializer {

            /**
             * The stack manipulation to apply within the type initializer.
             */
            private final ByteCodeAppender byteCodeAppender;

            /**
             * Creates a new simple type initializer.
             *
             * @param byteCodeAppender The byte code appender manipulation to apply within the type initializer.
             */
            public Simple(ByteCodeAppender byteCodeAppender) {
                this.byteCodeAppender = byteCodeAppender;
            }

            @Override
            public boolean isDefined() {
                return true;
            }

            @Override
            public TypeInitializer expandWith(ByteCodeAppender byteCodeAppender) {
                return new TypeInitializer.Simple(new ByteCodeAppender.Compound(this.byteCodeAppender, byteCodeAppender));
            }

            @Override
            public ByteCodeAppender withReturn() {
                return new ByteCodeAppender.Compound(byteCodeAppender, new ByteCodeAppender.Simple(MethodReturn.VOID));
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                return byteCodeAppender.apply(methodVisitor, implementationContext, instrumentedMethod);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && byteCodeAppender.equals(((TypeInitializer.Simple) other).byteCodeAppender);
            }

            @Override
            public int hashCode() {
                return byteCodeAppender.hashCode();
            }

            @Override
            public String toString() {
                return "InstrumentedType.TypeInitializer.Simple{" +
                        "byteCodeAppender=" + byteCodeAppender +
                        '}';
            }
        }
    }

    /**
     * An abstract base implementation of an instrumented type.
     */
    abstract class AbstractBase extends AbstractTypeDescription.OfSimpleType implements InstrumentedType {

        /**
         * The loaded type initializer for this instrumented type.
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * The type initializer for this instrumented type.
         */
        protected final TypeInitializer typeInitializer;

        protected final List<GenericTypeDescription> typeVariables;

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
            typeVariables = Collections.emptyList();
            fieldDescriptions = Collections.emptyList();
            methodDescriptions = Collections.emptyList();
        }

        protected AbstractBase(LoadedTypeInitializer loadedTypeInitializer,
                               TypeInitializer typeInitializer,
                               ElementMatcher<? super TypeDescription> matcher,
                               List<? extends GenericTypeDescription> typeVariables,
                               List<? extends FieldDescription> fieldDescriptions,
                               List<? extends MethodDescription> methodDescriptions) {
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.typeInitializer = typeInitializer;
            this.typeVariables = new ArrayList<GenericTypeDescription>(typeVariables.size());
            for (GenericTypeDescription typeVariable : typeVariables) {
                this.typeVariables.add(new TypeVariableToken(matcher, typeVariable));
            }
            this.fieldDescriptions = new ArrayList<FieldDescription>(fieldDescriptions.size());
            for (FieldDescription fieldDescription : fieldDescriptions) {
                this.fieldDescriptions.add(new FieldToken(matcher, fieldDescription));
            }
            this.methodDescriptions = new ArrayList<MethodDescription>(methodDescriptions.size());
            for (MethodDescription methodDescription : methodDescriptions) {
                this.methodDescriptions.add(new MethodToken(matcher, methodDescription));
            }
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
        public GenericTypeList getTypeVariables() {
            return new GenericTypeList.Explicit(typeVariables);
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

        @Override
        public PackageDescription getPackage() {
            String packageName = getPackageName();
            return packageName == null
                    ? null
                    : new PackageDescription.Simple(packageName);
        }

        protected class TypeVariableToken extends GenericTypeDescription.ForTypeVariable {

            private final String symbol;

            private final List<GenericTypeDescription> bounds;

            private TypeVariableToken(ElementMatcher<? super TypeDescription> matcher, GenericTypeDescription typeVariable) {
                symbol = typeVariable.getSymbol();
                bounds = null; // TODO!
//                bounds = TargetType.resolve(typeVariable.getUpperBounds(), AbstractBase.this, matcher);
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return new GenericTypeList.Explicit(bounds);
            }

            @Override
            public TypeVariableSource getVariableSource() {
                return AbstractBase.this;
            }

            @Override
            public String getSymbol() {
                return symbol;
            }
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
            private final GenericTypeDescription fieldType;

            /**
             * The modifiers of the field.
             */
            private final int modifiers;

            /**
             * The declared annotations of this field.
             */
            private final List<AnnotationDescription> declaredAnnotations;

            private FieldToken(ElementMatcher<? super TypeDescription> matcher, FieldDescription fieldDescription) {
                name = fieldDescription.getName();
                fieldType = TargetType.resolve(fieldDescription.getFieldTypeGen(), AbstractBase.this, matcher);
                modifiers = fieldDescription.getModifiers();
                declaredAnnotations = fieldDescription.getDeclaredAnnotations();
            }

            @Override
            public GenericTypeDescription getFieldTypeGen() {
                return fieldType;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Explicit(declaredAnnotations);
            }

            @Override
            public String getName() {
                return name;
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

            private final List<GenericTypeDescription> typeVariables;

            /**
             * The return type of the represented method.
             */
            private final GenericTypeDescription returnType;

            /**
             * The exception types of the represented method.
             */
            private final List<GenericTypeDescription> exceptionTypes;

            /**
             * The modifiers of the represented method.
             */
            private final int modifiers;

            /**
             * The declared annotations of this method.
             */
            private final List<AnnotationDescription> declaredAnnotations;

            /**
             * A list of descriptions of the method's parameters.
             */
            private final List<ParameterDescription> parameters;

            /**
             * The default value of this method or {@code null} if no such value exists.
             */
            private final Object defaultValue;

            private MethodToken(ElementMatcher<? super TypeDescription> matcher, MethodDescription methodDescription) {
                internalName = methodDescription.getInternalName();
                typeVariables = new ArrayList<GenericTypeDescription>(methodDescription.getTypeVariables().size());
                for (GenericTypeDescription typeVariable : methodDescription.getTypeVariables()) {
                    typeVariables.add(new TypeVariableToken(matcher, typeVariable));
                }
                returnType = TargetType.resolve(methodDescription.getReturnTypeGen(), AbstractBase.this, matcher);
                exceptionTypes = TargetType.resolve(methodDescription.getExceptionTypesGen(), AbstractBase.this, matcher);
                modifiers = methodDescription.getModifiers();
                declaredAnnotations = methodDescription.getDeclaredAnnotations();
                parameters = new ArrayList<ParameterDescription>(methodDescription.getParameters().size());
                for (ParameterDescription parameterDescription : methodDescription.getParameters()) {
                    parameters.add(new ParameterToken(matcher, parameterDescription));
                }
                defaultValue = methodDescription.getDefaultValue();
            }

            @Override
            public GenericTypeDescription getReturnTypeGen() {
                return returnType;
            }

            @Override
            public GenericTypeList getExceptionTypesGen() {
                return new GenericTypeList.Explicit(exceptionTypes);
            }

            @Override
            public ParameterList getParameters() {
                return new ParameterList.Explicit(parameters);
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Explicit(declaredAnnotations);
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
            public GenericTypeList getTypeVariables() {
                return new GenericTypeList.Explicit(typeVariables);
            }

            @Override
            public Object getDefaultValue() {
                return defaultValue;
            }

            protected class TypeVariableToken extends GenericTypeDescription.ForTypeVariable {

                private final String symbol;

                private final List<GenericTypeDescription> bounds;

                private TypeVariableToken(ElementMatcher<? super TypeDescription> matcher, GenericTypeDescription typeVariable) {
                    symbol = typeVariable.getSymbol();
                    bounds = TargetType.resolve(typeVariable.getUpperBounds(), AbstractBase.this, matcher);
                }

                @Override
                public GenericTypeList getUpperBounds() {
                    return new GenericTypeList.Explicit(bounds);
                }

                @Override
                public TypeVariableSource getVariableSource() {
                    return MethodToken.this;
                }

                @Override
                public String getSymbol() {
                    return symbol;
                }
            }

            /**
             * An implementation of a method parameter for a method of an instrumented type.
             */
            protected class ParameterToken extends ParameterDescription.AbstractParameterDescription {

                /**
                 * The type of the parameter.
                 */
                private final GenericTypeDescription parameterType;

                /**
                 * The index of the parameter.
                 */
                private final int index;

                /**
                 * The name of the parameter or {@code null} if no explicit name is known for this parameter.
                 */
                private final String name;

                /**
                 * The modifiers of this parameter or {@code null} if no such modifiers are known.
                 */
                private final Integer modifiers;

                /**
                 * The annotations of this parameter.
                 */
                private final List<AnnotationDescription> parameterAnnotations;

                protected ParameterToken(ElementMatcher<? super TypeDescription> matcher, ParameterDescription parameterDescription) {
                    parameterType = TargetType.resolve(parameterDescription.getTypeGen(), AbstractBase.this, matcher);
                    index = parameterDescription.getIndex();
                    name = parameterDescription.isNamed()
                            ? parameterDescription.getName()
                            : null;
                    modifiers = parameterDescription.hasModifiers()
                            ? getModifiers()
                            : null;
                    parameterAnnotations = Collections.emptyList();
                }

                @Override
                public GenericTypeDescription getTypeGen() {
                    return parameterType;
                }

                @Override
                public MethodDescription getDeclaringMethod() {
                    return MethodToken.this;
                }

                @Override
                public int getIndex() {
                    return index;
                }

                @Override
                public boolean isNamed() {
                    return name != null;
                }

                @Override
                public boolean hasModifiers() {
                    return modifiers != null;
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Explicit(parameterAnnotations);
                }

                @Override
                public int getModifiers() {
                    return hasModifiers()
                            ? modifiers
                            : super.getModifiers();
                }

                @Override
                public String getName() {
                    return isNamed()
                            ? name
                            : super.getName();
                }
            }
        }
    }
}

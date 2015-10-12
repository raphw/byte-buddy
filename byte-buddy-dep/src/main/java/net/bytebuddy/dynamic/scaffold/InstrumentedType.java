package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.joinUnique;

/**
 * Implementations of this interface represent an instrumented type that is subject to change. Implementations
 * should however be immutable and return new instance when their mutator methods are called.
 */
public interface InstrumentedType extends TypeDescription {

    /**
     * Creates a new instrumented type that includes a new field.
     *
     * @param fieldToken A token that represents the field's shape. This token must represent types in their detached state.
     * @return A new instrumented type that is equal to this instrumented type but with the additional field.
     */
    InstrumentedType withField(FieldDescription.Token fieldToken);

    /**
     * Creates a new instrumented type that includes a new method or constructor.
     *
     * @param methodToken A token that represents the method's shape. This token must represent types in their detached state.
     * @return A new instrumented type that is equal to this instrumented type but with the additional method.
     */
    InstrumentedType withMethod(MethodDescription.Token methodToken);

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
     * A default implementation of an instrumented type.
     */
    class Default extends AbstractBase.OfSimpleType implements InstrumentedType {

        /**
         * The binary name of the instrumented type.
         */
        private final String name;

        /**
         * The modifiers of the instrumented type.
         */
        private final int modifiers;

        /**
         * A list of type variables of the instrumented type.
         */
        private final List<? extends GenericTypeDescription> typeVariables;

        /**
         * The generic super type of the instrumented type.
         */
        private final GenericTypeDescription superType;

        /**
         * A list of interfaces of the instrumented type.
         */
        private final List<? extends GenericTypeDescription> interfaceTypes;

        /**
         * A list of field tokens describing the fields of the instrumented type.
         */
        private final List<? extends FieldDescription.Token> fieldTokens;

        /**
         * A list of method tokens describing the methods of the instrumented type.
         */
        private final List<? extends MethodDescription.Token> methodTokens;

        /**
         * A list of annotations of the annotated type.
         */
        private final List<? extends AnnotationDescription> annotationDescriptions;

        /**
         * The type initializer of the instrumented type.
         */
        private final TypeInitializer typeInitializer;

        /**
         * The loaded type initializer of the instrumented type.
         */
        private final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * The declaring type of the instrumented type or {@code null} if no such type exists.
         */
        private final TypeDescription declaringType;

        /**
         * The enclosing method of the instrumented type or {@code null} if no such type exists.
         */
        private final MethodDescription enclosingMethod;

        /**
         * The enclosing type of the instrumented type or {@code null} if no such type exists.
         */
        private final TypeDescription enclosingType;

        /**
         * {@code true} if this type is a member class.
         */
        private final boolean memberClass;

        /**
         * {@code true} if this type is a anonymous class.
         */
        private final boolean anonymousClass;

        /**
         * {@code true} if this type is a local class.
         */
        private final boolean localClass;

        /**
         * Creates a new instrumented type with default values for any properties that only exist for types that are declared within another type.
         *
         * @param name                   The binary name of the instrumented type.
         * @param modifiers              The modifiers of the instrumented type.
         * @param typeVariables          A list of type variables of the instrumented type.
         * @param superType              The generic super type of the instrumented type.
         * @param interfaceTypes         A list of interfaces of the instrumented type.
         * @param fieldTokens            A list of field tokens describing the fields of the instrumented type.
         * @param methodTokens           A list of method tokens describing the methods of the instrumented type.
         * @param annotationDescriptions A list of annotations of the annotated type.
         * @param typeInitializer        The type initializer of the instrumented type.
         * @param loadedTypeInitializer  The loaded type initializer of the instrumented type.
         */
        public Default(String name,
                       int modifiers,
                       List<? extends GenericTypeDescription> typeVariables,
                       GenericTypeDescription superType,
                       List<? extends GenericTypeDescription> interfaceTypes,
                       List<? extends FieldDescription.Token> fieldTokens,
                       List<? extends MethodDescription.Token> methodTokens,
                       List<? extends AnnotationDescription> annotationDescriptions,
                       TypeInitializer typeInitializer,
                       LoadedTypeInitializer loadedTypeInitializer) {
            this(name,
                    modifiers,
                    typeVariables,
                    superType,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    null,
                    null,
                    null,
                    false,
                    false,
                    false);
        }

        /**
         * Creates a new instrumented type.
         *
         * @param name                   The binary name of the instrumented type.
         * @param modifiers              The modifiers of the instrumented type.
         * @param typeVariables          A list of type variables of the instrumented type.
         * @param superType              The generic super type of the instrumented type.
         * @param interfaceTypes         A list of interfaces of the instrumented type.
         * @param fieldTokens            A list of field tokens describing the fields of the instrumented type.
         * @param methodTokens           A list of method tokens describing the methods of the instrumented type.
         * @param annotationDescriptions A list of annotations of the annotated type.
         * @param typeInitializer        The type initializer of the instrumented type.
         * @param loadedTypeInitializer  The loaded type initializer of the instrumented type.
         * @param declaringType          The declaring type of the instrumented type or {@code null} if no such type exists.
         * @param enclosingMethod        The enclosing method of the instrumented type or {@code null} if no such type exists.
         * @param enclosingType          The enclosing type of the instrumented type or {@code null} if no such type exists.
         * @param memberClass            {@code true} if this type is a member class.
         * @param anonymousClass         {@code true} if this type is a anonymous class.
         * @param localClass             {@code true} if this type is a local class.
         */
        public Default(String name,
                       int modifiers,
                       List<? extends GenericTypeDescription> typeVariables,
                       GenericTypeDescription superType,
                       List<? extends GenericTypeDescription> interfaceTypes,
                       List<? extends FieldDescription.Token> fieldTokens,
                       List<? extends MethodDescription.Token> methodTokens,
                       List<? extends AnnotationDescription> annotationDescriptions,
                       TypeInitializer typeInitializer,
                       LoadedTypeInitializer loadedTypeInitializer,
                       TypeDescription declaringType,
                       MethodDescription enclosingMethod,
                       TypeDescription enclosingType,
                       boolean memberClass,
                       boolean anonymousClass,
                       boolean localClass) {
            this.name = name;
            this.modifiers = modifiers;
            this.typeVariables = typeVariables;
            this.superType = superType;
            this.interfaceTypes = interfaceTypes;
            this.fieldTokens = fieldTokens;
            this.methodTokens = methodTokens;
            this.annotationDescriptions = annotationDescriptions;
            this.typeInitializer = typeInitializer;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.declaringType = declaringType;
            this.enclosingMethod = enclosingMethod;
            this.enclosingType = enclosingType;
            this.memberClass = memberClass;
            this.anonymousClass = anonymousClass;
            this.localClass = localClass;
        }

        @Override
        public InstrumentedType withField(FieldDescription.Token fieldToken) {
            return new Default(this.name,
                    this.modifiers,
                    typeVariables,
                    superType,
                    interfaceTypes,
                    joinUnique(fieldTokens, fieldToken),
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer);
        }

        @Override
        public InstrumentedType withMethod(MethodDescription.Token methodToken) {
            return new Default(name,
                    this.modifiers,
                    typeVariables,
                    superType,
                    interfaceTypes,
                    fieldTokens,
                    joinUnique(methodTokens, methodToken),
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer);
        }

        @Override
        public InstrumentedType withInitializer(LoadedTypeInitializer loadedTypeInitializer) {
            return new Default(name,
                    modifiers,
                    typeVariables,
                    superType,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    new LoadedTypeInitializer.Compound(this.loadedTypeInitializer, loadedTypeInitializer));
        }

        @Override
        public InstrumentedType withInitializer(ByteCodeAppender byteCodeAppender) {
            return new Default(name,
                    modifiers,
                    typeVariables,
                    superType,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer.expandWith(byteCodeAppender),
                    loadedTypeInitializer);
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
        public MethodDescription getEnclosingMethod() {
            return enclosingMethod;
        }

        @Override
        public TypeDescription getEnclosingType() {
            return enclosingType;
        }

        @Override
        public boolean isAnonymousClass() {
            return anonymousClass;
        }

        @Override
        public boolean isLocalClass() {
            return localClass;
        }

        @Override
        public boolean isMemberClass() {
            return memberClass;
        }

        @Override
        public PackageDescription getPackage() {
            int packageIndex = name.lastIndexOf('.');
            return packageIndex == -1
                    ? PackageDescription.UNDEFINED
                    : new PackageDescription.Simple(name.substring(0, packageIndex));
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(annotationDescriptions);
        }

        @Override
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        @Override
        protected GenericTypeDescription getDeclaredSuperType() {
            return superType == null
                    ? TypeDescription.UNDEFINED
                    : superType.accept(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(this));
        }

        @Override
        protected GenericTypeList getDeclaredInterfaces() {
            return GenericTypeList.ForDetachedTypes.attach(this, interfaceTypes);
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.ForTokens(this, fieldTokens);
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.ForTokens(this, methodTokens);
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return GenericTypeList.ForDetachedTypes.OfTypeVariable.attach(this, typeVariables);
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}

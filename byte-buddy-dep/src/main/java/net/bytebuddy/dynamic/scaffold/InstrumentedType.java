package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;

import java.lang.annotation.ElementType;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Implementations of this interface represent an instrumented type that is subject to change. Implementations
 * should however be immutable and return new instance when its builder methods are invoked.
 */
public interface InstrumentedType extends TypeDescription {

    /**
     * Creates a new instrumented type that includes a new field.
     *
     * @param token A token that represents the field's shape. This token must represent types in their detached state.
     * @return A new instrumented type that is equal to this instrumented type but with the additional field.
     */
    InstrumentedType withField(FieldDescription.Token token);

    /**
     * Creates a new instrumented type that includes a new method or constructor.
     *
     * @param token A token that represents the method's shape. This token must represent types in their detached state.
     * @return A new instrumented type that is equal to this instrumented type but with the additional method.
     */
    InstrumentedType withMethod(MethodDescription.Token token);

    /**
     * Creates a new instrumented type with changed modifiers.
     *
     * @param modifiers The instrumented type's modifiers.
     * @return A new instrumented type that is equal to this instrumented type but with the given modifiers.
     */
    InstrumentedType withModifiers(int modifiers);

    /**
     * Creates a new instrumented type with the given interfaces implemented.
     *
     * @param interfaceTypes The interface types to implement.
     * @return A new instrumented type that is equal to this instrumented type but with the given interfaces implemented.
     */
    InstrumentedType withInterfaces(TypeList.Generic interfaceTypes);

    /**
     * Creates a new instrumented type with the given type variable defined.
     *
     * @param typeVariable The type variable to declare.
     * @return A new instrumented type that is equal to this instrumented type but with the given type variable declared.
     */
    InstrumentedType withTypeVariable(TypeVariableToken typeVariable);

    /**
     * Creates a new instrumented type with the given annotations.
     *
     * @param annotationDescriptions The annotations to add to the instrumented type.
     * @return A new instrumented type that is equal to this instrumented type but annotated with the given annotations
     */
    InstrumentedType withAnnotations(List<? extends AnnotationDescription> annotationDescriptions);

    /**
     * Creates a new instrumented type with the supplied nest host. An instrumented type can be its own nest host.
     * Setting a nest host removes all nest members from the instrumented type.
     *
     * @param nestHost The nest host of the created instrumented type.
     * @return A new instrumented type with the supplied type as its nest host.
     */
    InstrumentedType withNestHost(TypeDescription nestHost);

    /**
     * Creates a new instrumented types with the supplied nest members added to this instrumented type. The instrumented
     * type is defined as a nest host if this method is invoked. Any previous nest members are prepended to the supplied types.
     *
     * @param nestMembers The nest members to add to the created instrumented type.
     * @return A new instrumented type that applies the supplied nest members.
     */
    InstrumentedType withNestMembers(TypeList nestMembers);

    /**
     * Creates a new instrumented type with the supplied enclosing type.
     *
     * @param enclosingType The type to define as the created instrumented type's enclosing type.
     * @return A new instrumented type with the supplied type as its enclosing type.
     */
    InstrumentedType withEnclosingType(TypeDescription enclosingType);

    /**
     * Creates a new instrumented type with the supplied enclosing method.
     *
     * @param enclosingMethod The method to define as the created instrumented type's enclosing method.
     * @return A new instrumented type with the supplied method as its enclosing method.
     */
    InstrumentedType withEnclosingMethod(MethodDescription.InDefinedShape enclosingMethod);

    /**
     * Creates a new instrumented type that is declared by the supplied type..
     *
     * @param declaringType The type that declares the instrumented type.
     * @return A new instrumented type that is declared by the instrumented type.
     */
    InstrumentedType withDeclaringType(TypeDescription declaringType);

    /**
     * Creates a new instrumented type that indicates that it declared the supplied types.
     *
     * @param declaredTypes The types to add to the created instrumented type as declared types.
     * @return A new instrumented type that indicates that it has declared the supplied types.
     */
    InstrumentedType withDeclaredTypes(TypeList declaredTypes);

    /**
     * Creates a new instrumented type that indicates that is defined as a local class. Setting this property
     * resets the anonymous class property.
     *
     * @param localClass {@code true} if the instrumented type is supposed to be treated as a local class.
     * @return A new instrumented type that is treated as a local class.
     */
    InstrumentedType withLocalClass(boolean localClass);

    /**
     * Creates a new instrumented type that indicates that is defined as an anonymous class. Setting this property
     * resets the local class property.
     *
     * @param anonymousClass {@code true} if the instrumented type is supposed to be treated as an anonymous class.
     * @return A new instrumented type that is treated as an anonymous class.
     */
    InstrumentedType withAnonymousClass(boolean anonymousClass);

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
     * Validates the instrumented type to define a legal Java type.
     *
     * @return This instrumented type as a non-modifiable type description.
     */
    TypeDescription validated();

    /**
     * Implementations represent an {@link InstrumentedType} with a flexible name.
     */
    interface WithFlexibleName extends InstrumentedType {

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withField(FieldDescription.Token token);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withMethod(MethodDescription.Token token);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withModifiers(int modifiers);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withInterfaces(TypeList.Generic interfaceTypes);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withNestHost(TypeDescription nestHost);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withNestMembers(TypeList nestMembers);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withEnclosingType(TypeDescription enclosingType);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withEnclosingMethod(MethodDescription.InDefinedShape enclosingMethod);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withDeclaringType(TypeDescription declaringType);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withDeclaredTypes(TypeList declaredTypes);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withLocalClass(boolean localClass);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withAnonymousClass(boolean anonymousClass);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withTypeVariable(TypeVariableToken typeVariable);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withAnnotations(List<? extends AnnotationDescription> annotationDescriptions);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withInitializer(LoadedTypeInitializer loadedTypeInitializer);

        /**
         * {@inheritDoc}
         */
        WithFlexibleName withInitializer(ByteCodeAppender byteCodeAppender);

        /**
         * Creates a new instrumented type with a changed name.
         *
         * @param name The name of the instrumented type.
         * @return A new instrumented type that has the given name.
         */
        WithFlexibleName withName(String name);

        /**
         * Applies a transformation onto all existing type variables of this instrumented type. A transformation is potentially unsafe
         * and it is the responsibility of the supplier to return a valid type variable token from the transformer.
         *
         * @param matcher     The matcher to decide what type variables to transform.
         * @param transformer The transformer to apply on all matched type variables.
         * @return A new instrumented type with all matched type variables transformed.
         */
        WithFlexibleName withTypeVariables(ElementMatcher<? super Generic> matcher, Transformer<TypeVariableToken> transformer);
    }

    /**
     * Implementations are able to prepare an {@link InstrumentedType}.
     */
    interface Prepareable {

        /**
         * Prepares a given instrumented type.
         *
         * @param instrumentedType The instrumented type in its current form.
         * @return The prepared instrumented type.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);
    }

    /**
     * A factory for creating an {@link InstrumentedType}.
     */
    interface Factory {

        /**
         * Creates an instrumented type that represents the provided type.
         *
         * @param typeDescription The type to represent.
         * @return An appropriate instrumented type.
         */
        InstrumentedType.WithFlexibleName represent(TypeDescription typeDescription);

        /**
         * Creates a new instrumented type as a subclass.
         *
         * @param name       The type's name.
         * @param modifiers  The type's modifiers.
         * @param superClass The type's super class.
         * @return A new instrumented type representing a subclass of the given parameters.
         */
        InstrumentedType.WithFlexibleName subclass(String name, int modifiers, TypeDescription.Generic superClass);

        /**
         * Default implementations of instrumented type factories.
         */
        enum Default implements Factory {

            /**
             * A factory for an instrumented type that allows to modify represented types.
             */
            MODIFIABLE {
                /** {@inheritDoc} */
                public InstrumentedType.WithFlexibleName represent(TypeDescription typeDescription) {
                    return new InstrumentedType.Default(typeDescription.getName(),
                            typeDescription.getModifiers(),
                            typeDescription.getSuperClass(),
                            typeDescription.getTypeVariables().asTokenList(is(typeDescription)),
                            typeDescription.getInterfaces().accept(Generic.Visitor.Substitutor.ForDetachment.of(typeDescription)),
                            typeDescription.getDeclaredFields().asTokenList(is(typeDescription)),
                            typeDescription.getDeclaredMethods().asTokenList(is(typeDescription)),
                            typeDescription.getDeclaredAnnotations(),
                            TypeInitializer.None.INSTANCE,
                            LoadedTypeInitializer.NoOp.INSTANCE,
                            typeDescription.getDeclaringType(),
                            typeDescription.getEnclosingMethod(),
                            typeDescription.getEnclosingType(),
                            typeDescription.getDeclaredTypes(),
                            typeDescription.isAnonymousType(),
                            typeDescription.isLocalType(),
                            typeDescription.isNestHost()
                                    ? TargetType.DESCRIPTION
                                    : typeDescription.getNestHost(),
                            typeDescription.isNestHost()
                                    ? typeDescription.getNestMembers().filter(not(is(typeDescription)))
                                    : Collections.<TypeDescription>emptyList());
                }
            },

            /**
             * A factory for an instrumented type that does not allow to modify represented types.
             */
            FROZEN {
                /** {@inheritDoc} */
                public InstrumentedType.WithFlexibleName represent(TypeDescription typeDescription) {
                    return new Frozen(typeDescription, LoadedTypeInitializer.NoOp.INSTANCE);
                }
            };

            /**
             * {@inheritDoc}
             */
            public InstrumentedType.WithFlexibleName subclass(String name, int modifiers, TypeDescription.Generic superClass) {
                return new InstrumentedType.Default(name,
                        modifiers,
                        superClass,
                        Collections.<TypeVariableToken>emptyList(),
                        Collections.<Generic>emptyList(),
                        Collections.<FieldDescription.Token>emptyList(),
                        Collections.<MethodDescription.Token>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        TypeInitializer.None.INSTANCE,
                        LoadedTypeInitializer.NoOp.INSTANCE,
                        TypeDescription.UNDEFINED,
                        MethodDescription.UNDEFINED,
                        TypeDescription.UNDEFINED,
                        Collections.<TypeDescription>emptyList(),
                        false,
                        false,
                        TargetType.DESCRIPTION,
                        Collections.<TypeDescription>emptyList());
            }
        }
    }

    /**
     * A default implementation of an instrumented type.
     */
    class Default extends AbstractBase.OfSimpleType implements InstrumentedType.WithFlexibleName {

        /**
         * A set containing all keywords of the Java programming language.
         */
        private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
                "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized", "boolean",
                "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import",
                "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short",
                "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile",
                "const", "float", "native", "super", "while"
        ));

        /**
         * The binary name of the instrumented type.
         */
        private final String name;

        /**
         * The modifiers of the instrumented type.
         */
        private final int modifiers;

        /**
         * The generic super type of the instrumented type.
         */
        private final Generic superClass;

        /**
         * The instrumented type's type variables in their tokenized form.
         */
        private final List<? extends TypeVariableToken> typeVariables;

        /**
         * A list of interfaces of the instrumented type.
         */
        private final List<? extends Generic> interfaceTypes;

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
        private final MethodDescription.InDefinedShape enclosingMethod;

        /**
         * The enclosing type of the instrumented type or {@code null} if no such type exists.
         */
        private final TypeDescription enclosingType;

        /**
         * A list of types that are declared by this type.
         */
        private final List<? extends TypeDescription> declaredTypes;

        /**
         * {@code true} if this type is a anonymous class.
         */
        private final boolean anonymousClass;

        /**
         * {@code true} if this type is a local class.
         */
        private final boolean localClass;

        /**
         * The nest host of this instrumented type or a description of {@link TargetType} if this type is its own nest host.
         */
        private final TypeDescription nestHost;

        /**
         * A list of all members of this types nest group excluding this type.
         */
        private final List<? extends TypeDescription> nestMembers;

        /**
         * Creates a new instrumented type.
         *
         * @param name                   The binary name of the instrumented type.
         * @param modifiers              The modifiers of the instrumented type.
         * @param typeVariables          The instrumented type's type variables in their tokenized form.
         * @param superClass             The generic super type of the instrumented type.
         * @param interfaceTypes         A list of interfaces of the instrumented type.
         * @param fieldTokens            A list of field tokens describing the fields of the instrumented type.
         * @param methodTokens           A list of method tokens describing the methods of the instrumented type.
         * @param annotationDescriptions A list of annotations of the annotated type.
         * @param typeInitializer        The type initializer of the instrumented type.
         * @param loadedTypeInitializer  The loaded type initializer of the instrumented type.
         * @param declaringType          The declaring type of the instrumented type or {@code null} if no such type exists.
         * @param enclosingMethod        The enclosing method of the instrumented type or {@code null} if no such type exists.
         * @param enclosingType          The enclosing type of the instrumented type or {@code null} if no such type exists.
         * @param declaredTypes          A list of types that are declared by this type.
         * @param anonymousClass         {@code true} if this type is a anonymous class.
         * @param localClass             {@code true} if this type is a local class.
         * @param nestHost               The nest host of this instrumented type or a description of {@link TargetType} if this type is its own nest host.
         * @param nestMembers            A list of all members of this types nest group excluding this type.
         */
        protected Default(String name,
                          int modifiers,
                          Generic superClass,
                          List<? extends TypeVariableToken> typeVariables,
                          List<? extends Generic> interfaceTypes,
                          List<? extends FieldDescription.Token> fieldTokens,
                          List<? extends MethodDescription.Token> methodTokens,
                          List<? extends AnnotationDescription> annotationDescriptions,
                          TypeInitializer typeInitializer,
                          LoadedTypeInitializer loadedTypeInitializer,
                          TypeDescription declaringType,
                          MethodDescription.InDefinedShape enclosingMethod,
                          TypeDescription enclosingType,
                          List<? extends TypeDescription> declaredTypes,
                          boolean anonymousClass,
                          boolean localClass,
                          TypeDescription nestHost,
                          List<? extends TypeDescription> nestMembers) {
            this.name = name;
            this.modifiers = modifiers;
            this.typeVariables = typeVariables;
            this.superClass = superClass;
            this.interfaceTypes = interfaceTypes;
            this.fieldTokens = fieldTokens;
            this.methodTokens = methodTokens;
            this.annotationDescriptions = annotationDescriptions;
            this.typeInitializer = typeInitializer;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.declaringType = declaringType;
            this.enclosingMethod = enclosingMethod;
            this.enclosingType = enclosingType;
            this.declaredTypes = declaredTypes;
            this.anonymousClass = anonymousClass;
            this.localClass = localClass;
            this.nestHost = nestHost;
            this.nestMembers = nestMembers;
        }

        /**
         * Creates a new instrumented type.
         *
         * @param name                The type's name.
         * @param superClass          The type's super class.
         * @param modifierContributor The type's modifiers.
         * @return An appropriate instrumented type.
         */
        public static InstrumentedType of(String name, TypeDescription.Generic superClass, ModifierContributor.ForType... modifierContributor) {
            return of(name, superClass, ModifierContributor.Resolver.of(modifierContributor).resolve());
        }

        /**
         * Creates a new instrumented type.
         *
         * @param name       The type's name.
         * @param superClass The type's super class.
         * @param modifiers  The type's modifiers.
         * @return An appropriate instrumented type.
         */
        public static InstrumentedType of(String name, TypeDescription.Generic superClass, int modifiers) {
            return Factory.Default.MODIFIABLE.subclass(name, modifiers, superClass);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withModifiers(int modifiers) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withField(FieldDescription.Token token) {
            return new Default(this.name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    CompoundList.of(fieldTokens, token.accept(Generic.Visitor.Substitutor.ForDetachment.of(this))),
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withMethod(MethodDescription.Token token) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    CompoundList.of(methodTokens, token.accept(Generic.Visitor.Substitutor.ForDetachment.of(this))),
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withInterfaces(TypeList.Generic interfaceTypes) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    CompoundList.of(this.interfaceTypes, interfaceTypes.accept(Generic.Visitor.Substitutor.ForDetachment.of(this))),
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withAnnotations(List<? extends AnnotationDescription> annotationDescriptions) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    CompoundList.of(this.annotationDescriptions, annotationDescriptions),
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withNestHost(TypeDescription nestHost) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost.equals(this)
                            ? TargetType.DESCRIPTION
                            : nestHost,
                    Collections.<TypeDescription>emptyList());
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withNestMembers(TypeList nestMembers) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    TargetType.DESCRIPTION,
                    CompoundList.of(this.nestMembers, nestMembers));
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withEnclosingType(TypeDescription enclosingType) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    MethodDescription.UNDEFINED,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withEnclosingMethod(MethodDescription.InDefinedShape enclosingMethod) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingMethod.getDeclaringType(),
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withDeclaringType(TypeDescription declaringType) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withDeclaredTypes(TypeList declaredTypes) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    CompoundList.of(this.declaredTypes, declaredTypes),
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withTypeVariable(TypeVariableToken typeVariable) {
            return new Default(name,
                    modifiers,
                    superClass,
                    CompoundList.of(typeVariables, typeVariable.accept(Generic.Visitor.Substitutor.ForDetachment.of(this))),
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withName(String name) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withTypeVariables(ElementMatcher<? super Generic> matcher, Transformer<TypeVariableToken> transformer) {
            List<TypeVariableToken> typeVariables = new ArrayList<TypeVariableToken>(this.typeVariables.size());
            int index = 0;
            for (TypeVariableToken typeVariableToken : this.typeVariables) {
                typeVariables.add(matcher.matches(getTypeVariables().get(index++))
                        ? transformer.transform(this, typeVariableToken)
                        : typeVariableToken);
            }
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withLocalClass(boolean localClass) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    false,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withAnonymousClass(boolean anonymousClass) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    false,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withInitializer(LoadedTypeInitializer loadedTypeInitializer) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer,
                    new LoadedTypeInitializer.Compound(this.loadedTypeInitializer, loadedTypeInitializer),
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withInitializer(ByteCodeAppender byteCodeAppender) {
            return new Default(name,
                    modifiers,
                    superClass,
                    typeVariables,
                    interfaceTypes,
                    fieldTokens,
                    methodTokens,
                    annotationDescriptions,
                    typeInitializer.expandWith(byteCodeAppender),
                    loadedTypeInitializer,
                    declaringType,
                    enclosingMethod,
                    enclosingType,
                    declaredTypes,
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        /**
         * {@inheritDoc}
         */
        public LoadedTypeInitializer getLoadedTypeInitializer() {
            return loadedTypeInitializer;
        }

        /**
         * {@inheritDoc}
         */
        public TypeInitializer getTypeInitializer() {
            return typeInitializer;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getEnclosingMethod() {
            return enclosingMethod;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnclosingType() {
            return enclosingType;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getDeclaredTypes() {
            return new TypeList.Explicit(declaredTypes);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAnonymousType() {
            return anonymousClass;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isLocalType() {
            return localClass;
        }

        /**
         * {@inheritDoc}
         */
        public PackageDescription getPackage() {
            int packageIndex = name.lastIndexOf('.');
            return new PackageDescription.Simple(packageIndex == -1
                    ? EMPTY_NAME
                    : name.substring(0, packageIndex));
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(annotationDescriptions);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        /**
         * {@inheritDoc}
         */
        public Generic getSuperClass() {
            return superClass == null
                    ? Generic.UNDEFINED
                    : new Generic.LazyProjection.WithResolvedErasure(superClass, Generic.Visitor.Substitutor.ForAttachment.of(this));
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getInterfaces() {
            return new TypeList.Generic.ForDetachedTypes.WithResolvedErasure(interfaceTypes, TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
        }

        /**
         * {@inheritDoc}
         */
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.ForTokens(this, fieldTokens);
        }

        /**
         * {@inheritDoc}
         */
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.ForTokens(this, methodTokens);
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getTypeVariables() {
            return TypeList.Generic.ForDetachedTypes.attachVariables(this, typeVariables);
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return modifiers;
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getNestHost() {
            return nestHost.represents(TargetType.class)
                    ? this
                    : nestHost;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getNestMembers() {
            return nestHost.represents(TargetType.class)
                    ? new TypeList.Explicit(CompoundList.of(this, nestMembers))
                    : nestHost.getNestMembers();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription validated() {
            if (!isValidIdentifier(getName().split("\\."))) {
                throw new IllegalStateException("Illegal type name: " + getName() + " for " + this);
            } else if ((getModifiers() & ~ModifierContributor.ForType.MASK) != EMPTY_MASK) {
                throw new IllegalStateException("Illegal modifiers " + getModifiers() + " for " + this);
            } else if (isPackageType() && getModifiers() != PackageDescription.PACKAGE_MODIFIERS) {
                throw new IllegalStateException("Illegal modifiers " + getModifiers() + " for package " + this);
            }
            TypeDescription.Generic superClass = getSuperClass();
            if (superClass != null) {
                if (!superClass.accept(Generic.Visitor.Validator.SUPER_CLASS)) {
                    throw new IllegalStateException("Illegal super class " + superClass + " for " + this);
                } else if (!superClass.accept(Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE)) {
                    throw new IllegalStateException("Illegal type annotations on super class " + superClass + " for " + this);
                } else if (!superClass.asErasure().isVisibleTo(this)) {
                    throw new IllegalStateException("Invisible super type " + superClass + " for " + this);
                }
            }
            Set<TypeDescription> interfaceErasures = new HashSet<TypeDescription>();
            for (TypeDescription.Generic interfaceType : getInterfaces()) {
                if (!interfaceType.accept(Generic.Visitor.Validator.INTERFACE)) {
                    throw new IllegalStateException("Illegal interface " + interfaceType + " for " + this);
                } else if (!interfaceType.accept(Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE)) {
                    throw new IllegalStateException("Illegal type annotations on interface " + interfaceType + " for " + this);
                } else if (!interfaceErasures.add(interfaceType.asErasure())) {
                    throw new IllegalStateException("Already implemented interface " + interfaceType + " for " + this);
                } else if (!interfaceType.asErasure().isVisibleTo(this)) {
                    throw new IllegalStateException("Invisible interface type " + interfaceType + " for " + this);
                }
            }
            TypeList.Generic typeVariables = getTypeVariables();
            if (!typeVariables.isEmpty() && isAssignableTo(Throwable.class)) {
                throw new IllegalStateException("Cannot define throwable " + this + " to be generic");
            }
            Set<String> typeVariableNames = new HashSet<String>();
            for (TypeDescription.Generic typeVariable : typeVariables) {
                String variableSymbol = typeVariable.getSymbol();
                if (!typeVariableNames.add(variableSymbol)) {
                    throw new IllegalStateException("Duplicate type variable symbol '" + typeVariable + "' for " + this);
                } else if (!isValidIdentifier(variableSymbol)) {
                    throw new IllegalStateException("Illegal type variable name '" + typeVariable + "' for " + this);
                } else if (!Generic.Visitor.Validator.ForTypeAnnotations.ofFormalTypeVariable(typeVariable)) {
                    throw new IllegalStateException("Illegal type annotation on '" + typeVariable + "' for " + this);
                }
                boolean interfaceBound = false;
                Set<TypeDescription.Generic> bounds = new HashSet<Generic>();
                for (TypeDescription.Generic bound : typeVariable.getUpperBounds()) {
                    if (!bound.accept(Generic.Visitor.Validator.TYPE_VARIABLE)) {
                        throw new IllegalStateException("Illegal type variable bound " + bound + " of " + typeVariable + " for " + this);
                    } else if (!bound.accept(Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE)) {
                        throw new IllegalStateException("Illegal type annotations on type variable " + bound + " for " + this);
                    } else if (!bounds.add(bound)) {
                        throw new IllegalStateException("Duplicate bound " + bound + " of " + typeVariable + " for " + this);
                    } else if (interfaceBound && (bound.getSort().isTypeVariable() || !bound.isInterface())) {
                        throw new IllegalStateException("Illegal interface bound " + bound + " of " + typeVariable + " for " + this);
                    }
                    interfaceBound = true;
                }
                if (!interfaceBound) {
                    throw new IllegalStateException("Type variable " + typeVariable + " for " + this + " does not define at least one bound");
                }
            }
            TypeDescription enclosingType = getEnclosingType();
            if (enclosingType != null && (enclosingType.isArray() || enclosingType.isPrimitive())) {
                throw new IllegalStateException("Cannot define array type or primitive type " + enclosingType + " + as enclosing type for " + this);
            }
            MethodDescription.InDefinedShape enclosingMethod = getEnclosingMethod();
            if (enclosingMethod != null && enclosingMethod.isTypeInitializer()) {
                throw new IllegalStateException("Cannot enclose type declaration in class initializer " + enclosingMethod);
            }
            TypeDescription declaringType = getDeclaringType();
            if (declaringType != null) {
                if (declaringType.isPrimitive() || declaringType.isArray()) {
                    throw new IllegalStateException("Cannot define array type or primitive type " + declaringType + " as declaring type for " + this);
                }
            } else if (enclosingType == null && enclosingMethod == null && (isLocalType() || isAnonymousType())) {
                throw new IllegalStateException("Cannot define an anonymous or local class without a declaring type for " + this);
            }
            Set<TypeDescription> declaredTypes = new HashSet<TypeDescription>();
            for (TypeDescription declaredType : getDeclaredTypes()) {
                if (declaredType.isArray() || declaredType.isPrimitive()) {
                    throw new IllegalStateException("Cannot define array type or primitive type " + declaredType + " + as declared type for " + this);
                } else if (!declaredTypes.add(declaredType)) {
                    throw new IllegalStateException("Duplicate definition of declared type " + declaredType);
                }
            }
            TypeDescription nestHost = getNestHost();
            if (nestHost.equals(this)) {
                Set<TypeDescription> nestMembers = new HashSet<TypeDescription>();
                for (TypeDescription nestMember : getNestMembers()) {
                    if (nestMember.isArray() || nestMember.isPrimitive()) {
                        throw new IllegalStateException("Cannot define array type or primitive type " + nestMember + " + as nest member of " + this);
                    } else if (!nestMember.isSamePackage(this)) {
                        throw new IllegalStateException("Cannot define nest member " + nestMember + " + within different package then " + this);
                    } else if (!nestMembers.add(nestMember)) {
                        throw new IllegalStateException("Duplicate definition of nest member " + nestMember);
                    }
                }
            } else if (nestHost.isArray() || nestHost.isPrimitive()) {
                throw new IllegalStateException("Cannot define array type or primitive type " + nestHost + " + as nest host for " + this);
            } else if (!nestHost.isSamePackage(this)) {
                throw new IllegalStateException("Cannot define nest host " + nestHost + " + within different package then " + this);
            }
            Set<TypeDescription> typeAnnotationTypes = new HashSet<TypeDescription>();
            for (AnnotationDescription annotationDescription : getDeclaredAnnotations()) {
                if (!annotationDescription.getElementTypes().contains(ElementType.TYPE)
                        && !(isAnnotation() && annotationDescription.getElementTypes().contains(ElementType.ANNOTATION_TYPE))
                        && !(isPackageType() && annotationDescription.getElementTypes().contains(ElementType.PACKAGE))) {
                    throw new IllegalStateException("Cannot add " + annotationDescription + " on " + this);
                } else if (!typeAnnotationTypes.add(annotationDescription.getAnnotationType())) {
                    throw new IllegalStateException("Duplicate annotation " + annotationDescription + " for " + this);
                }
            }
            Set<FieldDescription.SignatureToken> fieldSignatureTokens = new HashSet<FieldDescription.SignatureToken>();
            for (FieldDescription.InDefinedShape fieldDescription : getDeclaredFields()) {
                String fieldName = fieldDescription.getName();
                if (!fieldSignatureTokens.add(fieldDescription.asSignatureToken())) {
                    throw new IllegalStateException("Duplicate field definition for " + fieldDescription);
                } else if (!isValidIdentifier(fieldName)) {
                    throw new IllegalStateException("Illegal field name for " + fieldDescription);
                } else if ((fieldDescription.getModifiers() & ~ModifierContributor.ForField.MASK) != EMPTY_MASK) {
                    throw new IllegalStateException("Illegal field modifiers " + fieldDescription.getModifiers() + " for " + fieldDescription);
                }
                Generic fieldType = fieldDescription.getType();
                if (!fieldType.accept(Generic.Visitor.Validator.FIELD)) {
                    throw new IllegalStateException("Illegal field type " + fieldType + " for " + fieldDescription);
                } else if (!fieldType.accept(Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE)) {
                    throw new IllegalStateException("Illegal type annotations on " + fieldType + " for " + this);
                } else if (!fieldDescription.isSynthetic() && !fieldType.asErasure().isVisibleTo(this)) {
                    throw new IllegalStateException("Invisible field type " + fieldDescription.getType() + " for " + fieldDescription);
                }
                Set<TypeDescription> fieldAnnotationTypes = new HashSet<TypeDescription>();
                for (AnnotationDescription annotationDescription : fieldDescription.getDeclaredAnnotations()) {
                    if (!annotationDescription.getElementTypes().contains(ElementType.FIELD)) {
                        throw new IllegalStateException("Cannot add " + annotationDescription + " on " + fieldDescription);
                    } else if (!fieldAnnotationTypes.add(annotationDescription.getAnnotationType())) {
                        throw new IllegalStateException("Duplicate annotation " + annotationDescription + " for " + fieldDescription);
                    }
                }
            }
            Set<MethodDescription.SignatureToken> methodSignatureTokens = new HashSet<MethodDescription.SignatureToken>();
            for (MethodDescription.InDefinedShape methodDescription : getDeclaredMethods()) {
                if (!methodSignatureTokens.add(methodDescription.asSignatureToken())) {
                    throw new IllegalStateException("Duplicate method signature for " + methodDescription);
                } else if ((methodDescription.getModifiers() & ~ModifierContributor.ForMethod.MASK) != 0) {
                    throw new IllegalStateException("Illegal modifiers " + methodDescription.getModifiers() + " for " + methodDescription);
                }
                Set<String> methodTypeVariableNames = new HashSet<String>();
                for (TypeDescription.Generic typeVariable : methodDescription.getTypeVariables()) {
                    String variableSymbol = typeVariable.getSymbol();
                    if (!methodTypeVariableNames.add(variableSymbol)) {
                        throw new IllegalStateException("Duplicate type variable symbol '" + typeVariable + "' for " + methodDescription);
                    } else if (!isValidIdentifier(variableSymbol)) {
                        throw new IllegalStateException("Illegal type variable name '" + typeVariable + "' for " + methodDescription);
                    } else if (!Generic.Visitor.Validator.ForTypeAnnotations.ofFormalTypeVariable(typeVariable)) {
                        throw new IllegalStateException("Illegal type annotation on '" + typeVariable + "' for " + methodDescription);
                    }
                    boolean interfaceBound = false;
                    Set<TypeDescription.Generic> bounds = new HashSet<Generic>();
                    for (TypeDescription.Generic bound : typeVariable.getUpperBounds()) {
                        if (!bound.accept(Generic.Visitor.Validator.TYPE_VARIABLE)) {
                            throw new IllegalStateException("Illegal type variable bound " + bound + " of " + typeVariable + " for " + methodDescription);
                        } else if (!bound.accept(Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE)) {
                            throw new IllegalStateException("Illegal type annotations on bound " + bound + " of " + typeVariable + " for " + this);
                        } else if (!bounds.add(bound)) {
                            throw new IllegalStateException("Duplicate bound " + bound + " of " + typeVariable + " for " + methodDescription);
                        } else if (interfaceBound && (bound.getSort().isTypeVariable() || !bound.isInterface())) {
                            throw new IllegalStateException("Illegal interface bound " + bound + " of " + typeVariable + " for " + methodDescription);
                        }
                        interfaceBound = true;
                    }
                    if (!interfaceBound) {
                        throw new IllegalStateException("Type variable " + typeVariable + " for " + methodDescription + " does not define at least one bound");
                    }
                }
                Generic returnType = methodDescription.getReturnType();
                if (methodDescription.isTypeInitializer()) {
                    throw new IllegalStateException("Illegal explicit declaration of a type initializer by " + this);
                } else if (methodDescription.isConstructor()) {
                    if (!returnType.represents(void.class)) {
                        throw new IllegalStateException("A constructor must return void " + methodDescription);
                    } else if (!returnType.getDeclaredAnnotations().isEmpty()) {
                        throw new IllegalStateException("The void non-type must not be annotated for " + methodDescription);
                    }
                } else if (!isValidIdentifier(methodDescription.getInternalName())) {
                    throw new IllegalStateException("Illegal method name " + returnType + " for " + methodDescription);
                } else if (!returnType.accept(Generic.Visitor.Validator.METHOD_RETURN)) {
                    throw new IllegalStateException("Illegal return type " + returnType + " for " + methodDescription);
                } else if (!returnType.accept(Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE)) {
                    throw new IllegalStateException("Illegal type annotations on return type " + returnType + " for " + methodDescription);
                } else if (!methodDescription.isSynthetic() && !methodDescription.getReturnType().asErasure().isVisibleTo(this)) {
                    throw new IllegalStateException("Invisible return type " + methodDescription.getReturnType() + " for " + methodDescription);
                }
                Set<String> parameterNames = new HashSet<String>();
                for (ParameterDescription.InDefinedShape parameterDescription : methodDescription.getParameters()) {
                    Generic parameterType = parameterDescription.getType();
                    if (!parameterType.accept(Generic.Visitor.Validator.METHOD_PARAMETER)) {
                        throw new IllegalStateException("Illegal parameter type of " + parameterDescription + " for " + methodDescription);
                    } else if (!parameterType.accept(Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE)) {
                        throw new IllegalStateException("Illegal type annotations on parameter " + parameterDescription + " for " + methodDescription);
                    } else if (!methodDescription.isSynthetic() && !parameterType.asErasure().isVisibleTo(this)) {
                        throw new IllegalStateException("Invisible parameter type of " + parameterDescription + " for " + methodDescription);
                    }
                    if (parameterDescription.isNamed()) {
                        String parameterName = parameterDescription.getName();
                        if (!parameterNames.add(parameterName)) {
                            throw new IllegalStateException("Duplicate parameter name of " + parameterDescription + " for " + methodDescription);
                        } else if (!isValidIdentifier(parameterName)) {
                            throw new IllegalStateException("Illegal parameter name of " + parameterDescription + " for " + methodDescription);
                        }
                    }
                    if (parameterDescription.hasModifiers() && (parameterDescription.getModifiers() & ~ModifierContributor.ForParameter.MASK) != EMPTY_MASK) {
                        throw new IllegalStateException("Illegal modifiers of " + parameterDescription + " for " + methodDescription);
                    }
                    Set<TypeDescription> parameterAnnotationTypes = new HashSet<TypeDescription>();
                    for (AnnotationDescription annotationDescription : parameterDescription.getDeclaredAnnotations()) {
                        if (!annotationDescription.getElementTypes().contains(ElementType.PARAMETER)) {
                            throw new IllegalStateException("Cannot add " + annotationDescription + " on " + parameterDescription);
                        } else if (!parameterAnnotationTypes.add(annotationDescription.getAnnotationType())) {
                            throw new IllegalStateException("Duplicate annotation " + annotationDescription + " of " + parameterDescription + " for " + methodDescription);
                        }
                    }
                }
                for (TypeDescription.Generic exceptionType : methodDescription.getExceptionTypes()) {
                    if (!exceptionType.accept(Generic.Visitor.Validator.EXCEPTION)) {
                        throw new IllegalStateException("Illegal exception type " + exceptionType + " for " + methodDescription);
                    } else if (!exceptionType.accept(Generic.Visitor.Validator.ForTypeAnnotations.INSTANCE)) {
                        throw new IllegalStateException("Illegal type annotations on " + exceptionType + " for " + methodDescription);
                    } else if (!methodDescription.isSynthetic() && !exceptionType.asErasure().isVisibleTo(this)) {
                        throw new IllegalStateException("Invisible exception type " + exceptionType + " for " + methodDescription);
                    }
                }
                Set<TypeDescription> methodAnnotationTypes = new HashSet<TypeDescription>();
                for (AnnotationDescription annotationDescription : methodDescription.getDeclaredAnnotations()) {
                    if (!annotationDescription.getElementTypes().contains(methodDescription.isMethod() ? ElementType.METHOD : ElementType.CONSTRUCTOR)) {
                        throw new IllegalStateException("Cannot add " + annotationDescription + " on " + methodDescription);
                    } else if (!methodAnnotationTypes.add(annotationDescription.getAnnotationType())) {
                        throw new IllegalStateException("Duplicate annotation " + annotationDescription + " for " + methodDescription);
                    }
                }
                AnnotationValue<?, ?> defaultValue = methodDescription.getDefaultValue();
                if (defaultValue != null && !methodDescription.isDefaultValue(defaultValue)) {
                    throw new IllegalStateException("Illegal default value " + defaultValue + "for " + methodDescription);
                }
                Generic receiverType = methodDescription.getReceiverType();
                if (receiverType != null && !receiverType.accept(Generic.Visitor.Validator.RECEIVER)) {
                    throw new IllegalStateException("Illegal receiver type " + receiverType + " for " + methodDescription);
                } else if (methodDescription.isStatic()) {
                    if (receiverType != null) {
                        throw new IllegalStateException("Static method " + methodDescription + " defines a non-null receiver " + receiverType);
                    }
                } else if (methodDescription.isConstructor()) {
                    if (receiverType == null || !receiverType.asErasure().equals(enclosingType == null ? this : enclosingType)) {
                        throw new IllegalStateException("Constructor " + methodDescription + " defines an illegal receiver " + receiverType);
                    }
                } else if (/* methodDescription.isMethod() */ receiverType == null || !equals(receiverType.asErasure())) {
                    throw new IllegalStateException("Method " + methodDescription + " defines an illegal receiver " + receiverType);
                }
            }
            return this;
        }

        /**
         * Checks if an array of identifiers is a valid compound Java identifier.
         *
         * @param identifier an array of potentially invalid Java identifiers.
         * @return {@code true} if all identifiers are valid and the array is not empty.
         */
        private static boolean isValidIdentifier(String[] identifier) {
            if (identifier.length == 0) {
                return false;
            }
            for (String part : identifier) {
                if (!isValidIdentifier(part)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks if a Java identifier is valid.
         *
         * @param identifier The identifier to check for validity.
         * @return {@code true} if the given identifier is valid.
         */
        private static boolean isValidIdentifier(String identifier) {
            if (KEYWORDS.contains(identifier) || identifier.length() == 0 || !Character.isJavaIdentifierStart(identifier.charAt(0))) {
                return false;
            } else if (identifier.equals(PackageDescription.PACKAGE_CLASS_NAME)) {
                return true;
            }
            for (int index = 1; index < identifier.length(); index++) {
                if (!Character.isJavaIdentifierPart(identifier.charAt(index))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A frozen representation of an instrumented type of which the structure must not be modified.
     */
    class Frozen extends AbstractBase.OfSimpleType implements InstrumentedType.WithFlexibleName {

        /**
         * The represented type description.
         */
        private final TypeDescription typeDescription;

        /**
         * The type's loaded type initializer.
         */
        private final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * Creates a new frozen representation of an instrumented type.
         *
         * @param typeDescription       The represented type description.
         * @param loadedTypeInitializer The type's loaded type initializer.
         */
        protected Frozen(TypeDescription typeDescription, LoadedTypeInitializer loadedTypeInitializer) {
            this.typeDescription = typeDescription;
            this.loadedTypeInitializer = loadedTypeInitializer;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return typeDescription.getDeclaredAnnotations();
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return typeDescription.getModifiers();
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getTypeVariables() {
            return typeDescription.getTypeVariables();
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return typeDescription.getName();
        }

        /**
         * {@inheritDoc}
         */
        public Generic getSuperClass() {
            return typeDescription.getSuperClass();
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getInterfaces() {
            return typeDescription.getInterfaces();
        }

        /**
         * {@inheritDoc}
         */
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return typeDescription.getDeclaredFields();
        }

        /**
         * {@inheritDoc}
         */
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return typeDescription.getDeclaredMethods();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAnonymousType() {
            return typeDescription.isAnonymousType();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isLocalType() {
            return typeDescription.isLocalType();
        }

        /**
         * {@inheritDoc}
         */
        public PackageDescription getPackage() {
            return typeDescription.getPackage();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnclosingType() {
            return typeDescription.getEnclosingType();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            return typeDescription.getDeclaringType();
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getDeclaredTypes() {
            return typeDescription.getDeclaredTypes();
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getEnclosingMethod() {
            return typeDescription.getEnclosingMethod();
        }

        /**
         * {@inheritDoc}
         */
        public String getGenericSignature() {
            // Embrace use of native generic signature by direct delegation.
            return typeDescription.getGenericSignature();
        }

        /**
         * {@inheritDoc}
         */
        public int getActualModifiers(boolean superFlag) {
            // Embrace use of native actual modifiers by direct delegation.
            return typeDescription.getActualModifiers(superFlag);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getNestHost() {
            return typeDescription.getNestHost();
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getNestMembers() {
            return typeDescription.getNestMembers();
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withField(FieldDescription.Token token) {
            throw new IllegalStateException("Cannot define field for frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withMethod(MethodDescription.Token token) {
            throw new IllegalStateException("Cannot define method for frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withModifiers(int modifiers) {
            throw new IllegalStateException("Cannot change modifiers for frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withInterfaces(TypeList.Generic interfaceTypes) {
            throw new IllegalStateException("Cannot add interfaces for frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withTypeVariable(TypeVariableToken typeVariable) {
            throw new IllegalStateException("Cannot define type variable for frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withAnnotations(List<? extends AnnotationDescription> annotationDescriptions) {
            throw new IllegalStateException("Cannot add annotation to frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withNestHost(TypeDescription nestHost) {
            throw new IllegalStateException("Cannot set nest host of frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withNestMembers(TypeList nestMembers) {
            throw new IllegalStateException("Cannot add nest members to frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withEnclosingType(TypeDescription enclosingType) {
            throw new IllegalStateException("Cannot set enclosing type of frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withEnclosingMethod(MethodDescription.InDefinedShape enclosingMethod) {
            throw new IllegalStateException("Cannot set enclosing method of frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withDeclaringType(TypeDescription declaringType) {
            throw new IllegalStateException("Cannot add declaring type to frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withDeclaredTypes(TypeList declaredTypes) {
            throw new IllegalStateException("Cannot add declared types to frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withLocalClass(boolean localClass) {
            throw new IllegalStateException("Cannot define local class state to frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withAnonymousClass(boolean anonymousClass) {
            throw new IllegalStateException("Cannot define anonymous class state to frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withInitializer(LoadedTypeInitializer loadedTypeInitializer) {
            return new Frozen(typeDescription, new LoadedTypeInitializer.Compound(this.loadedTypeInitializer, loadedTypeInitializer));
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withInitializer(ByteCodeAppender byteCodeAppender) {
            throw new IllegalStateException("Cannot add initializer to frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withName(String name) {
            throw new IllegalStateException("Cannot change name of frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public WithFlexibleName withTypeVariables(ElementMatcher<? super Generic> matcher, Transformer<TypeVariableToken> transformer) {
            throw new IllegalStateException("Cannot add type variables of frozen type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public LoadedTypeInitializer getLoadedTypeInitializer() {
            return loadedTypeInitializer;
        }

        /**
         * {@inheritDoc}
         */
        public TypeInitializer getTypeInitializer() {
            return TypeInitializer.None.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription validated() {
            return typeDescription;
        }
    }
}

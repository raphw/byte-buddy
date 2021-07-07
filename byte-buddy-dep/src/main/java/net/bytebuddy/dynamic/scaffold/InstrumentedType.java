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
 * should however be immutable and return new instance when its builder methods are invoked. 此接口的实现表示一个可更改插桩类型。然而，实现应该是不可变的，并在调用其生成器方法时返回新实例
 */
public interface InstrumentedType extends TypeDescription {
    // 字节码生成器类型，代表了最后生成 Subject 对象，包含了字节码的结构等
    /**
     * Creates a new instrumented type that includes a new field. 创建一个包含新字段的新插桩类型
     *
     * @param token A token that represents the field's shape. This token must represent types in their detached state. 表示字段形状的标记，该标记必须表示处于分离状态的类型
     * @return A new instrumented type that is equal to this instrumented type but with the additional field. 等于该插桩类型的新插桩类型，但带有附加字段，也就是基于当前的插桩类型，添加附加字段
     */
    InstrumentedType withField(FieldDescription.Token token); // 往插桩类型中添加字段

    /**
     * Creates a new instrumented type that includes a new method or constructor.
     *
     * @param token A token that represents the method's shape. This token must represent types in their detached state.
     * @return A new instrumented type that is equal to this instrumented type but with the additional method.
     */
    InstrumentedType withMethod(MethodDescription.Token token); // 往插桩类型中添加方法

    /**
     * Creates a new instrumented type with changed modifiers. 创建修改描述符的新插桩类型
     *
     * @param modifiers The instrumented type's modifiers.
     * @return A new instrumented type that is equal to this instrumented type but with the given modifiers.
     */
    InstrumentedType withModifiers(int modifiers); // 修改插桩类型的描述符

    /**
     * Creates a new instrumented type with the given interfaces implemented. 使用实现的给定接口创建新的插桩类型
     *
     * @param interfaceTypes The interface types to implement.
     * @return A new instrumented type that is equal to this instrumented type but with the given interfaces implemented.
     */
    InstrumentedType withInterfaces(TypeList.Generic interfaceTypes);

    /**
     * Creates a new instrumented type with the given type variable defined. 使用定义的给定类型变量创建新的插桩类型
     *
     * @param typeVariable The type variable to declare.
     * @return A new instrumented type that is equal to this instrumented type but with the given type variable declared.
     */
    InstrumentedType withTypeVariable(TypeVariableToken typeVariable);

    /**
     * Creates a new instrumented type with the given annotations. 使用给定的注释创建新的插桩类型
     *
     * @param annotationDescriptions The annotations to add to the instrumented type.
     * @return A new instrumented type that is equal to this instrumented type but annotated with the given annotations
     */
    InstrumentedType withAnnotations(List<? extends AnnotationDescription> annotationDescriptions);

    /**
     * Creates a new instrumented type with the supplied nest host. An instrumented type can be its own nest host.
     * Setting a nest host removes all nest members from the instrumented type. 使用提供的嵌套主机创建新的插桩类型。插桩类型可以是它自己的嵌套主机。设置嵌套主机将从插桩类型中删除所有嵌套成员。
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
     * Creates a new instrumented type that indicates that is defined as a local class.
     *
     * @param localClass {@code true} if the instrumented type is supposed to be treated as a local class.
     * @return A new instrumented type that is treated as a local class.
     */
    InstrumentedType withLocalClass(boolean localClass);

    /**
     * Creates a new instrumented type that indicates that is defined as an anonymous class.
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
     * for this instrumented type. 返回为此插桩类型注册的 {@link net.bytebuddy.implementation.LoadedTypeInitializer}
     *
     * @return The registered loaded type initializers for this instrumented type. 为此插桩类型注册的已加载类型初始值设定项
     */
    LoadedTypeInitializer getLoadedTypeInitializer();

    /**
     * Returns this instrumented type's type initializer.
     *
     * @return This instrumented type's type initializer.
     */
    TypeInitializer getTypeInitializer();

    /**
     * Validates the instrumented type to define a legal Java type. 验证插桩类型以定义合法的Java类型
     *
     * @return This instrumented type as a non-modifiable type description. 此插桩类型为不可编辑的类型描述
     */
    TypeDescription validated();

    /**
     * Implementations represent an {@link InstrumentedType} with a flexible name. 实现表示一个具有灵活名称的{@link InstrumentedType}
     */
    interface WithFlexibleName extends InstrumentedType {

        @Override
        WithFlexibleName withField(FieldDescription.Token token);

        @Override
        WithFlexibleName withMethod(MethodDescription.Token token);

        @Override
        WithFlexibleName withModifiers(int modifiers);

        @Override
        WithFlexibleName withInterfaces(TypeList.Generic interfaceTypes);

        @Override
        WithFlexibleName withNestHost(TypeDescription nestHost);

        @Override
        WithFlexibleName withNestMembers(TypeList nestMembers);

        @Override
        WithFlexibleName withEnclosingType(TypeDescription enclosingType);

        @Override
        WithFlexibleName withEnclosingMethod(MethodDescription.InDefinedShape enclosingMethod);

        @Override
        WithFlexibleName withDeclaringType(TypeDescription declaringType);

        @Override
        WithFlexibleName withDeclaredTypes(TypeList declaredTypes);

        @Override
        WithFlexibleName withLocalClass(boolean localClass);

        @Override
        WithFlexibleName withAnonymousClass(boolean anonymousClass);

        @Override
        WithFlexibleName withTypeVariable(TypeVariableToken typeVariable);

        @Override
        WithFlexibleName withAnnotations(List<? extends AnnotationDescription> annotationDescriptions);

        @Override
        WithFlexibleName withInitializer(LoadedTypeInitializer loadedTypeInitializer);

        @Override
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
     * Implementations are able to prepare an {@link InstrumentedType}. 实现能够准备一个 {@link InstrumentedType}
     */
    interface Prepareable {

        /**
         * Prepares a given instrumented type. 准备 给定的插桩类，也就是可以往给定插桩类添加一些额外的信息
         *
         * @param instrumentedType The instrumented type in its current form. 当前形式的插桩类
         * @return The prepared instrumented type. 准备好的插桩类
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);
    }

    /**
     * A factory for creating an {@link InstrumentedType}. 用于创建 {@link InstrumentedType} 的工厂
     */
    interface Factory {

        /**
         * Creates an instrumented type that represents the provided type. 创建表示所提供类型的插桩类型
         *
         * @param typeDescription The type to represent. 要表示的类型
         * @return An appropriate instrumented type. 一个合适的插桩类型
         */
        InstrumentedType.WithFlexibleName represent(TypeDescription typeDescription);

        /**
         * Creates a new instrumented type as a subclass. 创建一个新的插桩类型作为子类
         *
         * @param name       The type's name.       类名
         * @param modifiers  The type's modifiers.  描述符
         * @param superClass The type's super class. 超类
         * @return A new instrumented type representing a subclass of the given parameters. 一种新的插桩类型，表示给定参数的子类
         */
        InstrumentedType.WithFlexibleName subclass(String name, int modifiers, TypeDescription.Generic superClass);

        /**
         * Default implementations of instrumented type factories. 插桩类工厂的默认实现
         */
        enum Default implements Factory {

            /**
             * A factory for an instrumented type that allows to modify represented types. 插桩类的工厂，允许修改表示的类型
             */
            MODIFIABLE {
                @Override
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
                            typeDescription.isAnonymousClass(),
                            typeDescription.isLocalClass(),
                            typeDescription.getNestHost().equals(typeDescription)
                                    ? TargetType.DESCRIPTION
                                    : typeDescription.getNestHost(),
                            typeDescription.getNestMembers().filter(not(is(typeDescription))));
                }
            },

            /**
             * A factory for an instrumented type that does not allow to modify represented types. 插桩类的工厂，不允许修改表示的类型
             */
            FROZEN {
                @Override
                public InstrumentedType.WithFlexibleName represent(TypeDescription typeDescription) {
                    return new Frozen(typeDescription, LoadedTypeInitializer.NoOp.INSTANCE);
                }
            };

            @Override
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
     * A default implementation of an instrumented type. 插桩类的默认实现
     */
    class Default extends AbstractBase.OfSimpleType implements InstrumentedType.WithFlexibleName {

        /**
         * A set containing all keywords of the Java programming language. 包含Java编程语言的所有关键字的集合
         */
        private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
                "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized", "boolean",
                "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import",
                "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short",
                "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile",
                "const", "float", "native", "super", "while"
        ));

        /**
         * The binary name of the instrumented type. 插桩类型的二进制名称
         */
        private final String name;

        /**
         * The modifiers of the instrumented type. 插桩类型的修饰符
         */
        private final int modifiers;

        /**
         * The generic super type of the instrumented type. 插桩类型的泛型超类型
         */
        private final Generic superClass;

        /**
         * The instrumented type's type variables in their tokenized form. 插桩类型的类型变量的标记形式
         */
        private final List<? extends TypeVariableToken> typeVariables;

        /**
         * A list of interfaces of the instrumented type. 插桩类型的接口列表
         */
        private final List<? extends Generic> interfaceTypes;

        /**
         * A list of field tokens describing the fields of the instrumented type. 描述插桩类型字段的字段标记列表
         */
        private final List<? extends FieldDescription.Token> fieldTokens;

        /**
         * A list of method tokens describing the methods of the instrumented type. 描述插桩类型的方法的方法标记的列表
         */
        private final List<? extends MethodDescription.Token> methodTokens;

        /**
         * A list of annotations of the annotated type. 注解类型的注解列表
         */
        private final List<? extends AnnotationDescription> annotationDescriptions;

        /**
         * The type initializer of the instrumented type. 插桩类型的类型初始值设定项
         */
        private final TypeInitializer typeInitializer;

        /**
         * The loaded type initializer of the instrumented type. 插桩类型的已加载类型初始值设定项
         */
        private final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * The declaring type of the instrumented type or {@code null} if no such type exists. 插桩类型的声明类型，如果不存在此类类型，则为{@code null}
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
         * A list of types that are declared by this type. 此类型声明的类型列表
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
         * The nest host of this instrumented type or a description of {@link TargetType} if this type is its own nest host. 此插入指令类型的嵌套主机或{@link TargetType}的描述（如果此类型是其自己的嵌套主机
         */
        private final TypeDescription nestHost;

        /**
         * A list of all members of this types nest group excluding this type. 此类型嵌套组中除此类型之外的所有成员的列表
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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
                    anonymousClass,
                    localClass,
                    nestHost,
                    nestMembers);
        }

        @Override
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
                    localClass,
                    nestHost,
                    nestMembers);
        }

        @Override
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

        @Override
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

        @Override
        public LoadedTypeInitializer getLoadedTypeInitializer() {
            return loadedTypeInitializer;
        }

        @Override
        public TypeInitializer getTypeInitializer() {
            return typeInitializer;
        }

        @Override
        public MethodDescription.InDefinedShape getEnclosingMethod() {
            return enclosingMethod;
        }

        @Override
        public TypeDescription getEnclosingType() {
            return enclosingType;
        }

        @Override
        public TypeList getDeclaredTypes() {
            return new TypeList.Explicit(declaredTypes);
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
        public PackageDescription getPackage() {
            int packageIndex = name.lastIndexOf('.');
            return new PackageDescription.Simple(packageIndex == -1
                    ? EMPTY_NAME
                    : name.substring(0, packageIndex));
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
        public Generic getSuperClass() {
            return superClass == null
                    ? Generic.UNDEFINED
                    : new Generic.LazyProjection.WithResolvedErasure(superClass, Generic.Visitor.Substitutor.ForAttachment.of(this));
        }

        @Override
        public TypeList.Generic getInterfaces() {
            return new TypeList.Generic.ForDetachedTypes.WithResolvedErasure(interfaceTypes, TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
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
        public TypeList.Generic getTypeVariables() {
            return TypeList.Generic.ForDetachedTypes.attachVariables(this, typeVariables);
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public TypeDescription getNestHost() {
            return nestHost.represents(TargetType.class)
                    ? this
                    : nestHost;
        }

        @Override
        public TypeList getNestMembers() {
            return new TypeList.Explicit(CompoundList.of(this, nestMembers));
        }

        @Override
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
            } else if (enclosingType == null && enclosingMethod == null && (isLocalClass() || isAnonymousClass())) {
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
         * Checks if a Java identifier is valid. 检查 Java 标识符是否有效
         *
         * @param identifier The identifier to check for validity.
         * @return {@code true} if the given identifier is valid.
         */
        private static boolean isValidIdentifier(String identifier) {
            if (KEYWORDS.contains(identifier) || identifier.isEmpty() || !Character.isJavaIdentifierStart(identifier.charAt(0))) {
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
     * A frozen representation of an instrumented type of which the structure must not be modified. 不能修改其结构的已插桩类的冻结表示
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
         * Creates a new frozen representation of an instrumented type. 创建插桩类型的新冻结表示
         *
         * @param typeDescription       The represented type description.
         * @param loadedTypeInitializer The type's loaded type initializer.
         */
        protected Frozen(TypeDescription typeDescription, LoadedTypeInitializer loadedTypeInitializer) {
            this.typeDescription = typeDescription;
            this.loadedTypeInitializer = loadedTypeInitializer;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return typeDescription.getDeclaredAnnotations();
        }

        @Override
        public int getModifiers() {
            return typeDescription.getModifiers();
        }

        @Override
        public TypeList.Generic getTypeVariables() {
            return typeDescription.getTypeVariables();
        }

        @Override
        public String getName() {
            return typeDescription.getName();
        }

        @Override
        public Generic getSuperClass() {
            return typeDescription.getSuperClass();
        }

        @Override
        public TypeList.Generic getInterfaces() {
            return typeDescription.getInterfaces();
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return typeDescription.getDeclaredFields();
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return typeDescription.getDeclaredMethods();
        }

        @Override
        public boolean isAnonymousClass() {
            return typeDescription.isAnonymousClass();
        }

        @Override
        public boolean isLocalClass() {
            return typeDescription.isLocalClass();
        }

        @Override
        public boolean isMemberClass() {
            return typeDescription.isMemberClass();
        }

        @Override
        public PackageDescription getPackage() {
            return typeDescription.getPackage();
        }

        @Override
        public TypeDescription getEnclosingType() {
            return typeDescription.getEnclosingType();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return typeDescription.getDeclaringType();
        }

        @Override
        public TypeList getDeclaredTypes() {
            return typeDescription.getDeclaredTypes();
        }

        @Override
        public MethodDescription.InDefinedShape getEnclosingMethod() {
            return typeDescription.getEnclosingMethod();
        }

        @Override
        public String getGenericSignature() {
            // Embrace use of native generic signature by direct delegation.
            return typeDescription.getGenericSignature();
        }

        @Override
        public int getActualModifiers(boolean superFlag) {
            // Embrace use of native actual modifiers by direct delegation.
            return typeDescription.getActualModifiers(superFlag);
        }

        @Override
        public TypeDescription getNestHost() {
            return typeDescription.getNestHost();
        }

        @Override
        public TypeList getNestMembers() {
            return typeDescription.getNestMembers();
        }

        @Override
        public WithFlexibleName withField(FieldDescription.Token token) {
            throw new IllegalStateException("Cannot define field for frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withMethod(MethodDescription.Token token) {
            throw new IllegalStateException("Cannot define method for frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withModifiers(int modifiers) {
            throw new IllegalStateException("Cannot change modifiers for frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withInterfaces(TypeList.Generic interfaceTypes) {
            throw new IllegalStateException("Cannot add interfaces for frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withTypeVariable(TypeVariableToken typeVariable) {
            throw new IllegalStateException("Cannot define type variable for frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withAnnotations(List<? extends AnnotationDescription> annotationDescriptions) {
            throw new IllegalStateException("Cannot add annotation to frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withNestHost(TypeDescription nestHost) {
            throw new IllegalStateException("Cannot set nest host of frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withNestMembers(TypeList nestMembers) {
            throw new IllegalStateException("Cannot add nest members to frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withEnclosingType(TypeDescription enclosingType) {
            throw new IllegalStateException("Cannot set enclosing type of frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withEnclosingMethod(MethodDescription.InDefinedShape enclosingMethod) {
            throw new IllegalStateException("Cannot set enclosing method of frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withDeclaringType(TypeDescription declaringType) {
            throw new IllegalStateException("Cannot add declaring type to frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withDeclaredTypes(TypeList declaredTypes) {
            throw new IllegalStateException("Cannot add declared types to frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withLocalClass(boolean localClass) {
            throw new IllegalStateException("Cannot define local class state to frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withAnonymousClass(boolean anonymousClass) {
            throw new IllegalStateException("Cannot define anonymous class state to frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withInitializer(LoadedTypeInitializer loadedTypeInitializer) {
            return new Frozen(typeDescription, new LoadedTypeInitializer.Compound(this.loadedTypeInitializer, loadedTypeInitializer));
        }

        @Override
        public WithFlexibleName withInitializer(ByteCodeAppender byteCodeAppender) {
            throw new IllegalStateException("Cannot add initializer to frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withName(String name) {
            throw new IllegalStateException("Cannot change name of frozen type: " + typeDescription);
        }

        @Override
        public WithFlexibleName withTypeVariables(ElementMatcher<? super Generic> matcher, Transformer<TypeVariableToken> transformer) {
            throw new IllegalStateException("Cannot add type variables of frozen type: " + typeDescription);
        }

        @Override
        public LoadedTypeInitializer getLoadedTypeInitializer() {
            return loadedTypeInitializer;
        }

        @Override
        public TypeInitializer getTypeInitializer() {
            return TypeInitializer.None.INSTANCE;
        }

        @Override
        public TypeDescription validated() {
            return typeDescription;
        }
    }
}
// 字节码本身一个 table 的结构
// 这个 InstrumentedType 就是代表这个 table 的结构
// 也包含了设置这个 table 的方法，比如withField（XXX），那么最后这个 InstrumentedType 就会被用来生成这样一个 class 文件
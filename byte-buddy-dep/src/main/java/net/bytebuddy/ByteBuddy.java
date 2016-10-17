package net.bytebuddy;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.*;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.dynamic.scaffold.inline.RebaseDynamicTypeBuilder;
import net.bytebuddy.dynamic.scaffold.inline.RedefinitionDynamicTypeBuilder;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Instances of this class serve as a focus point for configuration of the library's behavior and as an entry point
 * to any form of code generation using the library. For this purpose, Byte Buddy offers a fluent API which allows
 * for the step-wise generation of a new Java type. A type is generated either by:
 * <ul>
 * <li><b>Subclassing</b> some type: A subclass - as the name suggests - extends another, existing Java type. Virtual
 * members of the generated type's super types can be overridden. Subclasses can also be interface extensions of one
 * or several interfaces.</li>
 * <li><b>Redefining</b> a type: By redefining a type, it is not only possible to override virtual methods of the
 * redefined type but also to redefine existing methods. This way, it is also possible to change the behavior of
 * non-virtual methods and constructors of the redefined type.</li>
 * <li><b>Rebasing</b> a type: Rebasing a type works similar to creating a subclass, i.e. any method being overridden
 * is still capable of invoking any original code of the rebased type. Any rebased method is however inlined into the
 * rebased type and any original code is preserved automatically. This way, the type's identity does not change.</li>
 * </ul>
 * Byte Buddy's API does not change when a type is rebased, redefined or subclassed. All types are created via the
 * {@link net.bytebuddy.dynamic.DynamicType.Builder} interface. Byte Buddy's API is expressed by fully immutable
 * components and is therefore thread-safe. As a consequence, method calls must be chained for all of Byte Buddy's
 * component, e.g. a method call like the following has no effect:
 * <pre>
 * ByteBuddy byteBuddy = new ByteBuddy();
 * byteBuddy.foo()</pre>
 * Instead, the following method chain is corrent use of the API:
 * <pre>
 * ByteBuddy byteBuddy = new ByteBuddy().foo();</pre>
 * <p>
 * For the creation of Java agents, Byte Buddy offers a convenience API implemented by the
 * {@link net.bytebuddy.agent.builder.AgentBuilder}. The API wraps a {@link ByteBuddy} instance and offers agent-specific
 * configuration opportunities by integrating against the {@link java.lang.instrument.Instrumentation} API.
 * </p>
 *
 * @see net.bytebuddy.agent.builder.AgentBuilder
 */
public class ByteBuddy {

    /**
     * The default prefix for the default {@link net.bytebuddy.NamingStrategy}.
     */
    private static final String BYTE_BUDDY_DEFAULT_PREFIX = "ByteBuddy";

    /**
     * The default suffix when defining a {@link AuxiliaryType.NamingStrategy}.
     */
    private static final String BYTE_BUDDY_DEFAULT_SUFFIX = "auxiliary";

    /**
     * The class file version to use for types that are not based on an existing class file.
     */
    protected final ClassFileVersion classFileVersion;

    /**
     * The naming strategy to use.
     */
    protected final NamingStrategy namingStrategy;

    /**
     * The naming strategy to use for naming auxiliary types.
     */
    protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    /**
     * The annotation value filter factory to use.
     */
    protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

    /**
     * The annotation retention strategy to use.
     */
    protected final AnnotationRetention annotationRetention;

    /**
     * The implementation context factory to use.
     */
    protected final Implementation.Context.Factory implementationContextFactory;

    /**
     * The method graph compiler to use.
     */
    protected final MethodGraph.Compiler methodGraphCompiler;

    /**
     * A matcher for identifying methods that should be excluded from instrumentation.
     */
    protected final LatentMatcher<? super MethodDescription> ignoredMethods;

    /**
     * Determines if a type should be explicitly validated.
     */
    protected final TypeValidation typeValidation;

    /**
     * <p>
     * Creates a new Byte Buddy instance with a default configuration that is suitable for most use cases.
     * </p>
     * <p>
     * When creating this configuration, Byte Buddy attempts to discover the current JVM's version. If this
     * is not possible, class files are created Java 6-compatible.
     * </p>
     *
     * @see ClassFileVersion#ofThisVm(ClassFileVersion)
     */
    public ByteBuddy() {
        this(ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V6));
    }

    /**
     * Creates a new Byte Buddy instance with a default configuration that is suitable for most use cases.
     *
     * @param classFileVersion The class file version to use for types that are not based on an existing class file.
     */
    public ByteBuddy(ClassFileVersion classFileVersion) {
        this(classFileVersion,
                new NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_PREFIX),
                new AuxiliaryType.NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_SUFFIX),
                AnnotationValueFilter.Default.APPEND_DEFAULTS,
                AnnotationRetention.ENABLED,
                Implementation.Context.Default.Factory.INSTANCE,
                MethodGraph.Compiler.DEFAULT,
                TypeValidation.ENABLED,
                new LatentMatcher.Resolved<MethodDescription>(isSynthetic().or(isDefaultFinalizer())));
    }

    /**
     * Creates a new Byte Buddy instance.
     *
     * @param classFileVersion             The class file version to use for types that are not based on an existing class file.
     * @param namingStrategy               The naming strategy to use.
     * @param auxiliaryTypeNamingStrategy  The naming strategy to use for naming auxiliary types.
     * @param annotationValueFilterFactory The annotation value filter factory to use.
     * @param annotationRetention          The annotation retention strategy to use.
     * @param implementationContextFactory The implementation context factory to use.
     * @param methodGraphCompiler          The method graph compiler to use.
     * @param typeValidation               Determines if a type should be explicitly validated.
     * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
     */
    protected ByteBuddy(ClassFileVersion classFileVersion,
                        NamingStrategy namingStrategy,
                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                        AnnotationValueFilter.Factory annotationValueFilterFactory,
                        AnnotationRetention annotationRetention,
                        Implementation.Context.Factory implementationContextFactory,
                        MethodGraph.Compiler methodGraphCompiler,
                        TypeValidation typeValidation,
                        LatentMatcher<? super MethodDescription> ignoredMethods) {
        this.classFileVersion = classFileVersion;
        this.namingStrategy = namingStrategy;
        this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
        this.annotationValueFilterFactory = annotationValueFilterFactory;
        this.annotationRetention = annotationRetention;
        this.implementationContextFactory = implementationContextFactory;
        this.ignoredMethods = ignoredMethods;
        this.typeValidation = typeValidation;
        this.methodGraphCompiler = methodGraphCompiler;
    }

    /**
     * <p>
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     * </p>
     * <p>
     * When extending a class, Byte Buddy imitates all visible constructors of the subclassed type. Any constructor is implemented
     * to only invoke its super type constructor of equal signature. Another behavior can be specified by supplying an explicit
     * {@link ConstructorStrategy} by {@link ByteBuddy#subclass(Class, ConstructorStrategy)}.
     * </p>
     *
     * @param superClass The super class or interface type to extend.
     * @param <T>        A loaded type that the generated class is guaranteed to inherit.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public <T> DynamicType.Builder<T> subclass(Class<T> superClass) {
        return subclass(new TypeDescription.ForLoadedType(superClass));
    }

    /**
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     *
     * @param superClass          The super class or interface type to extend.
     * @param constructorStrategy A constructor strategy that determines the
     * @param <T>                 A loaded type that the generated class is guaranteed to inherit.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public <T> DynamicType.Builder<T> subclass(Class<T> superClass, ConstructorStrategy constructorStrategy) {
        return subclass(new TypeDescription.ForLoadedType(superClass), constructorStrategy);
    }

    /**
     * <p>
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     * </p>
     * <p>
     * When extending a class, Byte Buddy imitates all visible constructors of the subclassed type. Any constructor is implemented
     * to only invoke its super type constructor of equal signature. Another behavior can be specified by supplying an explicit
     * {@link ConstructorStrategy} by {@link ByteBuddy#subclass(Class, ConstructorStrategy)}.
     * </p>
     *
     * @param superType The super class or interface type to extend. The type must be a raw type or parameterized type. All type
     *                  variables that are referenced by the generic type must be declared by the generated subclass before creating
     *                  the type.
     * @param <T>       A loaded type that the generated class is guaranteed to inherit.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public <T> DynamicType.Builder<T> subclass(Type superType) {
        return subclass(TypeDefinition.Sort.describe(superType));
    }

    /**
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     *
     * @param superType           The super class or interface type to extend. The type must be a raw type or parameterized
     *                            type. All type variables that are referenced by the generic type must be declared by the
     *                            generated subclass before creating the type.
     * @param constructorStrategy A constructor strategy that determines the
     * @param <T>                 A loaded type that the generated class is guaranteed to inherit.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public <T> DynamicType.Builder<T> subclass(Type superType, ConstructorStrategy constructorStrategy) {
        return subclass(TypeDefinition.Sort.describe(superType), constructorStrategy);
    }

    /**
     * <p>
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     * </p>
     * <p>
     * When extending a class, Byte Buddy imitates all visible constructors of the subclassed type and sets them to be {@code public}.
     * Any constructor is implemented to only invoke its super type constructor of equal signature. Another behavior can be specified by
     * supplying an explicit {@link ConstructorStrategy} by {@link ByteBuddy#subclass(TypeDefinition, ConstructorStrategy)}.
     * </p>
     *
     * @param superType The super class or interface type to extend. The type must be a raw type or parameterized type. All type
     *                  variables that are referenced by the generic type must be declared by the generated subclass before creating
     *                  the type.
     * @param <T>       A loaded type that the generated class is guaranteed to inherit.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public <T> DynamicType.Builder<T> subclass(TypeDefinition superType) {
        return subclass(superType, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING);
    }

    /**
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     *
     * @param superType           The super class or interface type to extend. The type must be a raw type or parameterized
     *                            type. All type variables that are referenced by the generic type must be declared by the
     *                            generated subclass before creating the type.
     * @param constructorStrategy A constructor strategy that determines the
     * @param <T>                 A loaded type that the generated class is guaranteed to inherit.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public <T> DynamicType.Builder<T> subclass(TypeDefinition superType, ConstructorStrategy constructorStrategy) {
        TypeDescription.Generic actualSuperType;
        TypeList.Generic interfaceTypes;
        if (superType.isPrimitive() || superType.isArray() || superType.isFinal()) {
            throw new IllegalArgumentException("Cannot subclass primitive, array or final types: " + superType);
        } else if (superType.isInterface()) {
            interfaceTypes = new TypeList.Generic.Explicit(superType.asGenericType());
            actualSuperType = TypeDescription.Generic.OBJECT;
        } else {
            interfaceTypes = new TypeList.Generic.Empty();
            actualSuperType = superType.asGenericType();
        }
        return new SubclassDynamicTypeBuilder<T>(InstrumentedType.Default.subclass(namingStrategy.subclass(superType.asGenericType()),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.PLAIN).resolve(superType.getModifiers()),
                actualSuperType).withInterfaces(interfaceTypes),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                constructorStrategy);
    }

    /**
     * Creates a new, plain interface type.
     *
     * @return A type builder that creates a new interface type.
     */
    public DynamicType.Builder<?> makeInterface() {
        return makeInterface(Collections.<TypeDescription>emptyList());
    }

    /**
     * Creates a new interface type that extends the provided interface.
     *
     * @param interfaceType An interface type that the generated interface implements.
     * @param <T>           A loaded type that the generated interface is guaranteed to inherit.
     * @return A type builder that creates a new interface type.
     */
    @SuppressWarnings("unchecked")
    public <T> DynamicType.Builder<T> makeInterface(Class<T> interfaceType) {
        return (DynamicType.Builder<T>) makeInterface(Collections.<Type>singletonList(interfaceType));
    }

    /**
     * Creates a new interface type that extends the provided interface.
     *
     * @param interfaceType The interface types to implement. The types must be raw or parameterized types. All type
     *                      variables that are referenced by a parameterized type must be declared by the generated
     *                      subclass before creating the type.
     * @return A type builder that creates a new interface type.
     */
    public DynamicType.Builder<?> makeInterface(Type... interfaceType) {
        return makeInterface(new TypeList.Generic.ForLoadedTypes(interfaceType));
    }

    /**
     * Creates a new interface type that extends the provided interface.
     *
     * @param interfaceTypes The interface types to implement. The types must be raw or parameterized types. All
     *                       type variables that are referenced by a parameterized type must be declared by the
     *                       generated subclass before creating the type.
     * @return A type builder that creates a new interface type.
     */
    public DynamicType.Builder<?> makeInterface(List<? extends Type> interfaceTypes) {
        return makeInterface(new TypeList.Generic.ForLoadedTypes(interfaceTypes));
    }

    /**
     * Creates a new interface type that extends the provided interface.
     *
     * @param interfaceType The interface types to implement. The types must be raw or parameterized types. All
     *                      type variables that are referenced by a parameterized type must be declared by the
     *                      generated subclass before creating the type.
     * @return A type builder that creates a new interface type.
     */
    public DynamicType.Builder<?> makeInterface(TypeDefinition... interfaceType) {
        return makeInterface(Arrays.asList(interfaceType));
    }

    /**
     * Creates a new interface type that extends the provided interface.
     *
     * @param interfaceTypes The interface types to implement. The types must be raw or parameterized types. All
     *                       type variables that are referenced by a parameterized type must be declared by the
     *                       generated subclass before creating the type.
     * @return A type builder that creates a new interface type.
     */
    public DynamicType.Builder<?> makeInterface(Collection<? extends TypeDefinition> interfaceTypes) {
        return subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS).implement(interfaceTypes).modifiers(TypeManifestation.INTERFACE, Visibility.PUBLIC);
    }

    /**
     * Creates a new package definition. Package definitions are defined by classes named {@code package-info}
     * without any methods or fields but permit annotations. Any field or method definition will cause an
     * {@link IllegalStateException} to be thrown when the type is created.
     *
     * @param name The fully qualified name of the package.
     * @return A type builder that creates a {@code package-info} class file.
     */
    public DynamicType.Builder<?> makePackage(String name) {
        return new SubclassDynamicTypeBuilder<Object>(InstrumentedType.Default.subclass(name + "." + PackageDescription.PACKAGE_CLASS_NAME,
                PackageDescription.PACKAGE_MODIFIERS,
                TypeDescription.Generic.OBJECT),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    /**
     * Creates a new {@link Annotation} type. Annotation properties are implemented as non-static, public methods with the
     * property type being defined as the return type.
     *
     * @return A type builder that creates a new {@link Annotation} type.
     */
    public DynamicType.Builder<? extends Annotation> makeAnnotation() {
        return new SubclassDynamicTypeBuilder<Annotation>(InstrumentedType.Default.subclass(namingStrategy.subclass(TypeDescription.Generic.ANNOTATION),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.ANNOTATION).resolve(),
                TypeDescription.Generic.OBJECT).withInterfaces(new TypeList.Generic.Explicit(TypeDescription.Generic.ANNOTATION)),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    /**
     * Creates a new {@link Enum} type.
     *
     * @param value The names of the type's enumeration constants
     * @return A type builder for creating an enumeration type.
     */
    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(String... value) {
        return makeEnumeration(Arrays.asList(value));
    }

    /**
     * Creates a new {@link Enum} type.
     *
     * @param values The names of the type's enumeration constants
     * @return A type builder for creating an enumeration type.
     */
    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(Collection<? extends String> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Require at least one enumeration constant");
        }
        TypeDescription.Generic enumType = TypeDescription.Generic.Builder.parameterizedType(Enum.class, TargetType.class).build();
        return new SubclassDynamicTypeBuilder<Enum<?>>(InstrumentedType.Default.subclass(namingStrategy.subclass(enumType),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL, EnumerationState.ENUMERATION).resolve(),
                enumType),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Visibility.PRIVATE).withParameters(String.class, int.class)
                .intercept(SuperMethodCall.INSTANCE)
                .defineMethod(EnumerationImplementation.ENUM_VALUE_OF_METHOD_NAME,
                        TargetType.class,
                        Visibility.PUBLIC, Ownership.STATIC).withParameters(String.class)
                .intercept(MethodCall.invoke(enumType.getDeclaredMethods()
                        .filter(named(EnumerationImplementation.ENUM_VALUE_OF_METHOD_NAME).and(takesArguments(Class.class, String.class))).getOnly())
                        .withOwnType().withArgument(0)
                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .defineMethod(EnumerationImplementation.ENUM_VALUES_METHOD_NAME,
                        TargetType[].class,
                        Visibility.PUBLIC, Ownership.STATIC)
                .intercept(new EnumerationImplementation(new ArrayList<String>(values)));
    }

    /**
     * <p>
     * Redefines the given type where any intercepted method that is declared by the redefined type is fully replaced
     * by the new implementation.
     * </p>
     * <p>
     * The class file of the redefined type is located by querying the redefined type's class loader by name. For specifying an
     * alternative {@link ClassFileLocator}, use {@link ByteBuddy#redefine(Class, ClassFileLocator)}.
     * </p>
     * <p>
     * <b>Note</b>: When a user redefines a class with the purpose of reloading this class using a {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy},
     * it is important that no fields or methods are added to the redefined class. Note that some {@link Implementation}s implicitly add fields or methods.
     * Finally, Byte Buddy might be forced to add a method if a redefined class already defines a class initializer. This can be disabled by setting
     * {@link ByteBuddy#with(Implementation.Context.Factory)} to use a {@link net.bytebuddy.implementation.Implementation.Context.Disabled.Factory}
     * where the class initializer is retained <i>as is</i>.
     * </p>
     *
     * @param type The type that is being redefined.
     * @param <T>  The loaded type of the redefined type.
     * @return A type builder for redefining the provided type.
     */
    public <T> DynamicType.Builder<T> redefine(Class<T> type) {
        return redefine(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    /**
     * <p>
     * Redefines the given type where any intercepted method that is declared by the redefined type is fully replaced
     * by the new implementation.
     * </p>
     * <p>
     * <b>Note</b>: When a user redefines a class with the purpose of reloading this class using a {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy},
     * it is important that no fields or methods are added to the redefined class. Note that some {@link Implementation}s implicitly add fields or methods.
     * Finally, Byte Buddy might be forced to add a method if a redefined class already defines a class initializer. This can be disabled by setting
     * {@link ByteBuddy#with(Implementation.Context.Factory)} to use a {@link net.bytebuddy.implementation.Implementation.Context.Disabled.Factory}
     * where the class initializer is retained <i>as is</i>.
     * </p>
     *
     * @param type             The type that is being redefined.
     * @param classFileLocator The class file locator that is queried for the redefined type's class file.
     * @param <T>              The loaded type of the redefined type.
     * @return A type builder for redefining the provided type.
     */
    public <T> DynamicType.Builder<T> redefine(Class<T> type, ClassFileLocator classFileLocator) {
        return redefine(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    /**
     * <p>
     * Redefines the given type where any intercepted method that is declared by the redefined type is fully replaced
     * by the new implementation.
     * </p>
     * <p>
     * <b>Note</b>: When a user redefines a class with the purpose of reloading this class using a {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy},
     * it is important that no fields or methods are added to the redefined class. Note that some {@link Implementation}s implicitly add fields or methods.
     * Finally, Byte Buddy might be forced to add a method if a redefined class already defines a class initializer. This can be disabled by setting
     * {@link ByteBuddy#with(Implementation.Context.Factory)} to use a {@link net.bytebuddy.implementation.Implementation.Context.Disabled.Factory}
     * where the class initializer is retained <i>as is</i>.
     * </p>
     *
     * @param type             The type that is being redefined.
     * @param classFileLocator The class file locator that is queried for the redefined type's class file.
     * @param <T>              The loaded type of the redefined type.
     * @return A type builder for redefining the provided type.
     */
    public <T> DynamicType.Builder<T> redefine(TypeDescription type, ClassFileLocator classFileLocator) {
        return new RedefinitionDynamicTypeBuilder<T>(InstrumentedType.Default.of(type),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                type,
                classFileLocator);
    }

    /**
     * <p>
     * Rebases the given type where any intercepted method that is declared by the redefined type is preserved within the
     * rebased type's class such that the class's original can be invoked from the new method implementations. Rebasing a
     * type can be seen similarly to creating a subclass where the subclass is later merged with the original class file.
     * </p>
     * <p>
     * The class file of the rebased type is located by querying the rebased type's class loader by name. For specifying an
     * alternative {@link ClassFileLocator}, use {@link ByteBuddy#redefine(Class, ClassFileLocator)}.
     * </p>
     *
     * @param type                 The type that is being rebased.
     * @param <T>                  The loaded type of the rebased type.
     * @return A type builder for rebasing the provided type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> type) {
        return rebase(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    /**
     * <p>
     * Rebases the given type where any intercepted method that is declared by the redefined type is preserved within the
     * rebased type's class such that the class's original can be invoked from the new method implementations. Rebasing a
     * type can be seen similarly to creating a subclass where the subclass is later merged with the original class file.
     * </p>
     * <p>
     * When a method is rebased, the original method is copied into a new method with a different name. These names are
     * generated automatically by Byte Buddy unless a {@link MethodNameTransformer} is specified explicitly.
     * Use {@link ByteBuddy#rebase(Class, ClassFileLocator, MethodNameTransformer)} for doing so.
     * </p>
     *
     * @param type             The type that is being rebased.
     * @param classFileLocator The class file locator that is queried for the rebased type's class file.
     * @param <T>              The loaded type of the rebased type.
     * @return A type builder for rebasing the provided type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> type, ClassFileLocator classFileLocator) {
        return rebase(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    /**
     * Rebases the given type where any intercepted method that is declared by the redefined type is preserved within the
     * rebased type's class such that the class's original can be invoked from the new method implementations. Rebasing a
     * type can be seen similarly to creating a subclass where the subclass is later merged with the original class file.
     *
     * @param type                  The type that is being rebased.
     * @param classFileLocator      The class file locator that is queried for the rebased type's class file.
     * @param methodNameTransformer The method name transformer for renaming a method that is rebased.
     * @param <T>                   The loaded type of the rebased type.
     * @return A type builder for rebasing the provided type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> type, ClassFileLocator classFileLocator, MethodNameTransformer methodNameTransformer) {
        return rebase(new TypeDescription.ForLoadedType(type), classFileLocator, methodNameTransformer);
    }

    /**
     * <p>
     * Rebases the given type where any intercepted method that is declared by the redefined type is preserved within the
     * rebased type's class such that the class's original can be invoked from the new method implementations. Rebasing a
     * type can be seen similarly to creating a subclass where the subclass is later merged with the original class file.
     * </p>
     * <p>
     * When a method is rebased, the original method is copied into a new method with a different name. These names are
     * generated automatically by Byte Buddy unless a {@link MethodNameTransformer} is specified explicitly.
     * Use {@link ByteBuddy#rebase(TypeDescription, ClassFileLocator, MethodNameTransformer)} for doing so.
     * </p>
     *
     * @param type             The type that is being rebased.
     * @param classFileLocator The class file locator that is queried for the rebased type's class file.
     * @param <T>              The loaded type of the rebased type.
     * @return A type builder for rebasing the provided type.
     */
    public <T> DynamicType.Builder<T> rebase(TypeDescription type, ClassFileLocator classFileLocator) {
        return rebase(type, classFileLocator, MethodNameTransformer.Suffixing.withRandomSuffix());
    }

    /**
     * Rebases the given type where any intercepted method that is declared by the redefined type is preserved within the
     * rebased type's class such that the class's original can be invoked from the new method implementations. Rebasing a
     * type can be seen similarly to creating a subclass where the subclass is later merged with the original class file.
     *
     * @param type                  The type that is being rebased.
     * @param classFileLocator      The class file locator that is queried for the rebased type's class file.
     * @param methodNameTransformer The method name transformer for renaming a method that is rebased.
     * @param <T>                   The loaded type of the rebased type.
     * @return A type builder for rebasing the provided type.
     */
    public <T> DynamicType.Builder<T> rebase(TypeDescription type, ClassFileLocator classFileLocator, MethodNameTransformer methodNameTransformer) {
        return new RebaseDynamicTypeBuilder<T>(InstrumentedType.Default.of(type),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                type,
                classFileLocator,
                methodNameTransformer);
    }

    /**
     * Rebases a package. This offers an opportunity to add annotations to the package definition. Packages are defined
     * by classes named {@code package-info} without any methods or fields but permit annotations. Any field or method
     * definition will cause an {@link IllegalStateException} to be thrown when the type is created.
     *
     * @param aPackage         The package that is being rebased.
     * @param classFileLocator The class file locator to use for locating the package's class file.
     * @return A type builder for rebasing the given package.
     */
    public DynamicType.Builder<?> rebase(Package aPackage, ClassFileLocator classFileLocator) {
        return rebase(new PackageDescription.ForLoadedPackage(aPackage), classFileLocator);
    }

    /**
     * Rebases a package. This offers an opportunity to add annotations to the package definition. Packages are defined
     * by classes named {@code package-info} without any methods or fields but permit annotations. Any field or method
     * definition will cause an {@link IllegalStateException} to be thrown when the type is created.
     *
     * @param aPackage         The package that is being rebased.
     * @param classFileLocator The class file locator to use for locating the package's class file.
     * @return A type builder for rebasing the given package.
     */
    public DynamicType.Builder<?> rebase(PackageDescription aPackage, ClassFileLocator classFileLocator) {
        return rebase(new TypeDescription.ForPackageDescription(aPackage), classFileLocator);
    }

    /**
     * Creates a new configuration where all class files that are not based on an existing class file are created
     * using the supplied class file version. When creating a Byte Buddy instance by {@link ByteBuddy#ByteBuddy()}, the class
     * file version is detected automatically. If the class file version is known before creating a Byte Buddy instance, the
     * {@link ByteBuddy#ByteBuddy(ClassFileVersion)} constructor should be used.
     *
     * @param classFileVersion The class file version to use for types that are not based on an existing class file.
     * @return A new Byte Buddy instance that uses the supplied class file version.
     */
    public ByteBuddy with(ClassFileVersion classFileVersion) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    /**
     * Creates a new configuration where new types are named by applying the given naming strategy. By default, Byte Buddy
     * simply retains the name of rebased and redefined types but adds a random suffix to the name of created subclasses or
     * -interfaces. If a type is defined within the {@code java.*} namespace, Byte Buddy also adds a suffix to the generated
     * class because this namespace is only available for the bootstrap class loader.
     *
     * @param namingStrategy The naming strategy to apply when creating a new dynamic type.
     * @return A new Byte Buddy instance that uses the supplied naming strategy.
     */
    public ByteBuddy with(NamingStrategy namingStrategy) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    /**
     * Creates a new configuration where auxiliary types are named by applying the given naming strategy. Auxiliary types
     * are helper types that might be required for implementing certain {@link Implementation}s. By default, Byte Buddy
     * adds a random suffix to the instrumented type's name when naming its auxiliary types.
     *
     * @param auxiliaryTypeNamingStrategy The naming strategy to apply when creating a new auxiliary type.
     * @return A new Byte Buddy instance that uses the supplied naming strategy for auxiliary types.
     */
    public ByteBuddy with(AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    /**
     * Creates a new configuration where annotation values are written according to the given filter factory. Using
     * a filter factory, it is for example possible not to include certain values into a class file such that the
     * runtime returns an annotation type's default value. By default, Byte Buddy includes all values into a class file,
     * also such values for which a default value exists.
     *
     * @param annotationValueFilterFactory The annotation value filter factory to use.
     * @return A new Byte Buddy instance that uses the supplied annotation value filter factory.
     */
    public ByteBuddy with(AnnotationValueFilter.Factory annotationValueFilterFactory) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    /**
     * <p>
     * Creates a new configuration where annotations that are found in an existing class file are or are not preserved
     * in the format they are discovered, i.e. rewritten in the format they were already present in the class file.
     * By default, Byte Buddy retains annotations when a class is rebased or redefined.
     * </p>
     * <p>
     * <b>Warning</b>: Retaining annotations can cause problems when annotations of a field or method are added based
     * on the annotations of a matched method. Doing so, Byte Buddy might write the annotations of the field or method
     * explicitly to a class file while simultaneously retaining the existing annotation what results in duplicates.
     * When matching fields or methods while adding annotations, disabling annotation retention might be required.
     * </p>
     *
     * @param annotationRetention The annotation retention strategy to use.
     * @return A new Byte Buddy instance that uses the supplied annotation retention strategy.
     */
    public ByteBuddy with(AnnotationRetention annotationRetention) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    /**
     * Creates a new configuration where the {@link net.bytebuddy.implementation.Implementation.Context} of any created
     * type is a product of the given implementation context factory. An implementation context might imply unwanted
     * side-effects, for example, the creation of an additional synthetic methods in order to support specific features
     * for realizing an {@link Implementation}. By default, Byte Buddy supplies a factory that enables all features. When
     * redefining a loaded class, it is however required by the JVM that no additional members are added such that a
     * {@link net.bytebuddy.implementation.Implementation.Context.Disabled} factory might be more appropriate.
     *
     * @param implementationContextFactory The implementation context factory to use for defining an instrumented type.
     * @return A new Byte Buddy instance that uses the supplied implementation context factory.
     */
    public ByteBuddy with(Implementation.Context.Factory implementationContextFactory) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    /**
     * Creates a new configuration where the {@link MethodGraph.Compiler} is used for creating a {@link MethodGraph}
     * of the instrumented type. A method graph is a representation of a type's virtual methods, including all information
     * on bridge methods that are inserted by the Java compiler. Creating a method graph is a rather expensive operation
     * and more efficient strategies might exist for certain types or ava types that are created by alternative JVM
     * languages. By default, a general purpose method graph compiler is used that uses the information that is exposed
     * by the generic type information that is embedded in any class file.
     *
     * @param methodGraphCompiler The method graph compiler to use for analyzing the instrumented type.
     * @return A new Byte Buddy instance that uses the supplied method graph compiler.
     */
    public ByteBuddy with(MethodGraph.Compiler methodGraphCompiler) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    /**
     * Creates a new configuration that applies the supplied type validation. By default, explicitly type validation is applied
     * by Byte Buddy but it might be disabled for performance reason or for voluntarily creating illegal types. The Java virtual
     * machine applies its own type validation where some {@link Error} is thrown if a type is invalid, while Byte Buddy throws
     * some {@link RuntimeException}.
     *
     * @param typeValidation The type validation to apply during type creation.
     * @return A new Byte Buddy instance that applies the supplied type validation.
     */
    public ByteBuddy with(TypeValidation typeValidation) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    /**
     * Creates a new configuration where any {@link MethodDescription} that matches the provided method matcher is excluded
     * from instrumentation. Any previous matcher for ignored methods is replaced. By default, Byte Buddy ignores any
     * synthetic method (bridge methods are handled automatically) and the {@link Object#finalize()} method.
     *
     * @param ignoredMethods A matcher for identifying methods to be excluded from instrumentation.
     * @return A new Byte Buddy instance that excludes any method from instrumentation if it is matched by the supplied matcher.
     */
    public ByteBuddy ignore(ElementMatcher<? super MethodDescription> ignoredMethods) {
        return ignore(new LatentMatcher.Resolved<MethodDescription>(ignoredMethods));
    }

    /**
     * <p>
     * Creates a new configuration where any {@link MethodDescription} that matches the provided method matcher is excluded
     * from instrumentation. Any previous matcher for ignored methods is replaced. By default, Byte Buddy ignores any
     * synthetic method (bridge methods are handled automatically) and the {@link Object#finalize()} method. Using a latent
     * matcher gives opportunity to resolve an {@link ElementMatcher} based on the instrumented type before applying the matcher.
     * </p>
     *
     * @param ignoredMethods A matcher for identifying methods to be excluded from instrumentation.
     * @return A new Byte Buddy instance that excludes any method from instrumentation if it is matched by the supplied matcher.
     */
    public ByteBuddy ignore(LatentMatcher<? super MethodDescription> ignoredMethods) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ByteBuddy byteBuddy = (ByteBuddy) other;
        return classFileVersion.equals(byteBuddy.classFileVersion)
                && annotationValueFilterFactory.equals(byteBuddy.annotationValueFilterFactory)
                && annotationRetention == byteBuddy.annotationRetention
                && namingStrategy.equals(byteBuddy.namingStrategy)
                && auxiliaryTypeNamingStrategy.equals(byteBuddy.auxiliaryTypeNamingStrategy)
                && implementationContextFactory.equals(byteBuddy.implementationContextFactory)
                && methodGraphCompiler.equals(byteBuddy.methodGraphCompiler)
                && typeValidation.equals(byteBuddy.typeValidation)
                && ignoredMethods.equals(byteBuddy.ignoredMethods);
    }

    @Override
    public int hashCode() {
        int result = classFileVersion.hashCode();
        result = 31 * result + annotationValueFilterFactory.hashCode();
        result = 31 * result + annotationRetention.hashCode();
        result = 31 * result + namingStrategy.hashCode();
        result = 31 * result + auxiliaryTypeNamingStrategy.hashCode();
        result = 31 * result + implementationContextFactory.hashCode();
        result = 31 * result + methodGraphCompiler.hashCode();
        result = 31 * result + typeValidation.hashCode();
        result = 31 * result + ignoredMethods.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ByteBuddy{" +
                "classFileVersion=" + classFileVersion +
                ", annotationValueFilterFactory=" + annotationValueFilterFactory +
                ", annotationRetention=" + annotationRetention +
                ", namingStrategy=" + namingStrategy +
                ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                ", implementationContextFactory=" + implementationContextFactory +
                ", methodGraphCompiler=" + methodGraphCompiler +
                ", typeValidation=" + typeValidation +
                ", ignoredMethods=" + ignoredMethods +
                '}';
    }

    /**
     * An implementation fo the {@code values} method of an enumeration type.
     */
    protected static class EnumerationImplementation implements Implementation {

        /**
         * The name of the {@link java.lang.Object#clone()} method.
         */
        protected static final String CLONE_METHOD_NAME = "clone";

        /**
         * The name of the {@code valueOf} method that is defined for any enumeration.
         */
        protected static final String ENUM_VALUE_OF_METHOD_NAME = "valueOf";

        /**
         * The name of the {@code values} method that is defined for any enumeration.
         */
        protected static final String ENUM_VALUES_METHOD_NAME = "values";

        /**
         * The field modifiers to use for any field that is added to an enumeration.
         */
        private static final int ENUM_FIELD_MODIFIERS = Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC;

        /**
         * The name of the field containing an array of all enumeration values.
         */
        private static final String ENUM_VALUES = "$VALUES";

        /**
         * The names of the enumerations to define for the enumeration.
         */
        private final List<String> values;

        /**
         * Creates a new implementation of an enumeration type.
         *
         * @param values The values of the enumeration.
         */
        protected EnumerationImplementation(List<String> values) {
            this.values = values;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            for (String value : values) {
                instrumentedType = instrumentedType.withField(new FieldDescription.Token(value,
                        ENUM_FIELD_MODIFIERS | Opcodes.ACC_ENUM,
                        TargetType.DESCRIPTION.asGenericType()));
            }
            return instrumentedType
                    .withField(new FieldDescription.Token(ENUM_VALUES,
                            ENUM_FIELD_MODIFIERS | Opcodes.ACC_SYNTHETIC,
                            TypeDescription.ArrayProjection.of(TargetType.DESCRIPTION).asGenericType()))
                    .withInitializer(new InitializationAppender(values));
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new ValuesMethodAppender(implementationTarget.getInstrumentedType());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && values.equals(((EnumerationImplementation) other).values);
        }

        @Override
        public int hashCode() {
            return values.hashCode();
        }

        @Override
        public String toString() {
            return "ByteBuddy.EnumerationImplementation{" +
                    "values=" + values +
                    '}';
        }

        /**
         * A byte code appender for the {@code values} method of any enumeration type.
         */
        protected static class ValuesMethodAppender implements ByteCodeAppender {

            /**
             * The instrumented enumeration type.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new appender for the {@code values} method.
             *
             * @param instrumentedType The instrumented enumeration type.
             */
            protected ValuesMethodAppender(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                FieldDescription valuesField = instrumentedType.getDeclaredFields().filter(named(ENUM_VALUES)).getOnly();
                MethodDescription cloneMethod = TypeDescription.Generic.OBJECT.getDeclaredMethods().filter(named(CLONE_METHOD_NAME)).getOnly();
                return new Size(new StackManipulation.Compound(
                        FieldAccess.forField(valuesField).getter(),
                        MethodInvocation.invoke(cloneMethod).virtual(valuesField.getType().asErasure()),
                        TypeCasting.to(valuesField.getType().asErasure()),
                        MethodReturn.REFERENCE
                ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((ValuesMethodAppender) other).instrumentedType);
            }

            @Override
            public int hashCode() {
                return instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "ByteBuddy.EnumerationImplementation.ValuesMethodAppender{" +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }
        }

        /**
         * A byte code appender for the type initializer of any enumeration type.
         */
        protected static class InitializationAppender implements ByteCodeAppender {

            /**
             * The values of the enumeration that is being created.
             */
            private final List<String> values;

            /**
             * Creates an appender for an enumerations type initializer.
             *
             * @param values The values of the enumeration that is being created.
             */
            protected InitializationAppender(List<String> values) {
                this.values = values;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                TypeDescription instrumentedType = instrumentedMethod.getDeclaringType().asErasure();
                MethodDescription enumConstructor = instrumentedType.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(String.class, int.class)))
                        .getOnly();
                int ordinal = 0;
                StackManipulation stackManipulation = StackManipulation.Trivial.INSTANCE;
                List<FieldDescription> enumerationFields = new ArrayList<FieldDescription>(values.size());
                for (String value : values) {
                    FieldDescription fieldDescription = instrumentedType.getDeclaredFields().filter(named(value)).getOnly();
                    stackManipulation = new StackManipulation.Compound(stackManipulation,
                            TypeCreation.of(instrumentedType),
                            Duplication.SINGLE,
                            new TextConstant(value),
                            IntegerConstant.forValue(ordinal++),
                            MethodInvocation.invoke(enumConstructor),
                            FieldAccess.forField(fieldDescription).putter());
                    enumerationFields.add(fieldDescription);
                }
                List<StackManipulation> fieldGetters = new ArrayList<StackManipulation>(values.size());
                for (FieldDescription fieldDescription : enumerationFields) {
                    fieldGetters.add(FieldAccess.forField(fieldDescription).getter());
                }
                stackManipulation = new StackManipulation.Compound(
                        stackManipulation,
                        ArrayFactory.forType(instrumentedType.asGenericType()).withValues(fieldGetters),
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(ENUM_VALUES)).getOnly()).putter()
                );
                return new Size(stackManipulation.apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && values.equals(((InitializationAppender) other).values);
            }

            @Override
            public int hashCode() {
                return values.hashCode();
            }

            @Override
            public String toString() {
                return "ByteBuddy.EnumerationImplementation.InitializationAppender{" +
                        "values=" + values +
                        '}';
            }
        }
    }
}

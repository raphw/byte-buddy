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
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.dynamic.scaffold.inline.RebaseDynamicTypeBuilder;
import net.bytebuddy.dynamic.scaffold.inline.RedefinitionDynamicTypeBuilder;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ByteBuddy {

    /**
     * The default prefix for the default {@link net.bytebuddy.NamingStrategy}.
     */
    public static final String BYTE_BUDDY_DEFAULT_PREFIX = "ByteBuddy";

    /**
     * The default suffix when defining a naming strategy for auxiliary types.
     */
    public static final String BYTE_BUDDY_DEFAULT_SUFFIX = "auxiliary";

    /**
     * The class file version of the current configuration.
     */
    protected final ClassFileVersion classFileVersion;

    protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

    /**
     * The naming strategy of the current configuration.
     */
    protected final NamingStrategy namingStrategy;

    /**
     * The naming strategy for auxiliary types of the current configuation.
     */
    protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    /**
     * The currently defined implementation context factory.
     */
    protected final Implementation.Context.Factory implementationContextFactory;

    /**
     * A matcher for identifying methods that should never be intercepted.
     */
    protected final ElementMatcher<? super MethodDescription> ignoredMethods;

    /**
     * The method graph compiler to use.
     */
    protected final MethodGraph.Compiler methodGraphCompiler;

    /**
     * Defines a new {@code ByteBuddy} default configuration for the current Java virtual machine's
     * class file version.
     */
    public ByteBuddy() {
        this(ClassFileVersion.forCurrentJavaVersion());
    }

    /**
     * Defines a new {@code ByteBuddy} default configuration for the given class file version.
     *
     * @param classFileVersion The class file version to apply.
     */
    public ByteBuddy(ClassFileVersion classFileVersion) {
        this(classFileVersion,
                new NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_PREFIX),
                new AuxiliaryType.NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_SUFFIX),
                AnnotationValueFilter.Default.APPEND_DEFAULTS,
                Implementation.Context.Default.Factory.INSTANCE,
                isSynthetic().or(isDefaultFinalizer()),
                MethodGraph.Compiler.DEFAULT);
    }

    protected ByteBuddy(ClassFileVersion classFileVersion,
                        NamingStrategy namingStrategy,
                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                        AnnotationValueFilter.Factory annotationValueFilterFactory,
                        Implementation.Context.Factory implementationContextFactory,
                        ElementMatcher<? super MethodDescription> ignoredMethods,
                        MethodGraph.Compiler methodGraphCompiler) {
        this.ignoredMethods = ignoredMethods;
        this.classFileVersion = classFileVersion;
        this.namingStrategy = namingStrategy;
        this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
        this.annotationValueFilterFactory = annotationValueFilterFactory;
        this.implementationContextFactory = implementationContextFactory;
        this.methodGraphCompiler = methodGraphCompiler;
    }

    /**
     * Creates a dynamic type builder that creates a subclass of a given loaded type where the subclass
     * is created by the {@link net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default#IMITATE_SUPER_TYPE}
     * strategy.
     *
     * @param superType The type or interface to be extended or implemented by the dynamic type.
     * @param <T>       The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that extends or implements the given loaded type.
     */
    public <T> DynamicType.Builder<T> subclass(Class<T> superType) {
        return subclass(new TypeDescription.ForLoadedType(superType));
    }

    /**
     * Creates a dynamic type builder that creates a subclass of a given loaded type.
     *
     * @param superType           The type or interface to be extended or implemented by the dynamic type.
     * @param constructorStrategy The constructor strategy to apply.
     * @param <T>                 The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that extends or implements the given loaded type.
     */
    public <T> DynamicType.Builder<T> subclass(Class<T> superType, ConstructorStrategy constructorStrategy) {
        return subclass(new TypeDescription.ForLoadedType(superType), constructorStrategy);
    }

    /**
     * Creates a dynamic type builder that creates a subclass of a given loaded type where the subclass
     * is created by the {@link net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default#IMITATE_SUPER_TYPE}
     * strategy.
     *
     * @param superType The type or interface to be extended or implemented by the dynamic type.
     * @param <T>       The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that extends or implements the given loaded type.
     */
    public <T> DynamicType.Builder<T> subclass(Type superType) {
        return subclass(TypeDefinition.Sort.describe(superType));
    }

    /**
     * Creates a dynamic type builder that creates a subclass of a given loaded type.
     *
     * @param superType           The type or interface to be extended or implemented by the dynamic type.
     * @param constructorStrategy The constructor strategy to apply.
     * @param <T>                 The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that extends or implements the given loaded type.
     */
    public <T> DynamicType.Builder<T> subclass(Type superType, ConstructorStrategy constructorStrategy) {
        return subclass(TypeDefinition.Sort.describe(superType), constructorStrategy);
    }

    /**
     * Creates a dynamic type builder that creates a subclass of a given type description where the subclass
     * is created by the {@link net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default#IMITATE_SUPER_TYPE}
     * strategy.
     *
     * @param superType The type or interface to be extended or implemented by the dynamic type.
     * @param <T>       The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that extends or implements the given type description.
     */
    public <T> DynamicType.Builder<T> subclass(TypeDefinition superType) {
        return subclass(superType, ConstructorStrategy.Default.IMITATE_SUPER_TYPE);
    }

    /**
     * Creates a dynamic type builder that creates a subclass of a given type description.
     *
     * @param superType           The type or interface to be extended or implemented by the dynamic type.
     * @param constructorStrategy The constructor strategy to apply.
     * @param <T>                 The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that extends or implements the given type description.
     */
    public <T> DynamicType.Builder<T> subclass(TypeDefinition superType, ConstructorStrategy constructorStrategy) {
        TypeDescription.Generic actualSuperType;
        List<TypeDescription.Generic> interfaceTypes;
        if (superType.isPrimitive() || superType.isArray() || superType.asErasure().isFinal()) {
            throw new IllegalArgumentException("Cannot subclass primitive, array or final types: " + superType);
        } else if (superType.asErasure().isInterface()) {
            interfaceTypes = Collections.singletonList(superType.asGenericType());
            actualSuperType = TypeDescription.Generic.OBJECT;
        } else {
            interfaceTypes = Collections.emptyList();
            actualSuperType = superType.asGenericType();
        }
        return new SubclassDynamicTypeBuilder<T>(InstrumentedType.Default.subclass(namingStrategy.subclass(actualSuperType),
                interfaceTypes.isEmpty()
                        ? ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.INTERFACE).resolve(actualSuperType.asErasure().getModifiers())
                        : ModifierContributor.Resolver.of(Visibility.PUBLIC).resolve(actualSuperType.asErasure().getModifiers()),
                actualSuperType).withInterfaces(interfaceTypes),
                ignoredMethods,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                constructorStrategy);
    }

    /**
     * Creates a dynamic type builder for an interface that does not extend any interfaces.
     *
     * @return A builder for creating a new interface.
     */
    public DynamicType.Builder<?> makeInterface() {
        return makeInterface(Collections.<TypeDescription>emptyList());
    }

    /**
     * Creates a dynamic type builder for an interface that extends the given interface.
     *
     * @param type The interface to extend.
     * @param <T>  The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interface.
     */
    @SuppressWarnings("unchecked")
    public <T> DynamicType.Builder<T> makeInterface(Class<T> type) {
        return (DynamicType.Builder<T>) makeInterface(Collections.<TypeDescription>singletonList(new TypeDescription.ForLoadedType(type)));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param type The interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(Type... type) {
        return makeInterface(new TypeList.Generic.ForLoadedTypes(type));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param type The interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(Class<?>... type) {
        return makeInterface(new TypeList.ForLoadedTypes(type));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param types The interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(List<? extends Type> types) {
        return makeInterface(new TypeList.Generic.ForLoadedTypes(types));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param typeDefinition Descriptions of the interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(TypeDefinition... typeDefinition) {
        return makeInterface(Arrays.asList(typeDefinition));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param typeDefinitions The interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(Collection<? extends TypeDefinition> typeDefinitions) {
        return subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS).implement(typeDefinitions);
    }

    /**
     * Creates a new Java package. A Java package is represented as a Java type with name <i>package-info</i>. The explicit creation of a
     * package can be useful for adding annotations to this package.
     *
     * @param name The name of the package.
     * @return A dynamic type that represents the created package.
     */
    public DynamicType.Builder<?> makePackage(String name) {
        return new SubclassDynamicTypeBuilder<Object>(InstrumentedType.Default.subclass(name + "." + PackageDescription.PACKAGE_CLASS_NAME,
                PackageDescription.PACKAGE_MODIFIERS,
                TypeDescription.Generic.OBJECT),
                ignoredMethods,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    /**
     * Rebases the given package. A Java package is represented as a type named <i>package-info</i>. The explicit creation of a
     * package can be useful for adding annotations to this package.
     *
     * @param aPackage         The package to rebase.
     * @param classFileLocator A class file locator for locating the given packages class file.
     * @return A dynamic type that represents the created package.
     */
    public DynamicType.Builder<?> rebase(Package aPackage, ClassFileLocator classFileLocator) {
        return rebase(new PackageDescription.ForLoadedPackage(aPackage), classFileLocator);
    }

    /**
     * Rebases the given package. A Java package is represented as a type named <i>package-info</i>. The explicit creation of a
     * package can be useful for adding annotations to this package.
     *
     * @param packageDescription The description of the package to rebase.
     * @param classFileLocator   A class file locator for locating the given packages class file.
     * @return A dynamic type that represents the created package.
     */
    public DynamicType.Builder<?> rebase(PackageDescription packageDescription, ClassFileLocator classFileLocator) {
        return rebase(new TypeDescription.ForPackageDescription(packageDescription), classFileLocator);
    }

    /**
     * Creates a dynamic type builder for a new annotation type.
     *
     * @return A builder for a new annotation type.
     */
    public DynamicType.Builder<? extends Annotation> makeAnnotation() {
        return new SubclassDynamicTypeBuilder<Annotation>(InstrumentedType.Default.subclass(namingStrategy.subclass(TypeDescription.Generic.ANNOTATION),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.ANNOTATION).resolve(),
                TypeDescription.Generic.OBJECT).withInterfaces(Collections.singletonList(TypeDescription.Generic.ANNOTATION)),
                ignoredMethods,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    /**
     * Creates a new enumeration type.
     *
     * @param value The enumeration values to define.
     * @return A builder for a new enumeration type with the given values.
     */
    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(String... value) {
        return makeEnumeration(Arrays.asList(value));
    }

    /**
     * Creates a new enumeration type.
     *
     * @param values The enumeration values to define.
     * @return A builder for a new enumeration type with the given values.
     */
    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(Collection<? extends String> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Require at least one enumeration constant");
        }
        TypeDescription.Generic enumType = TypeDescription.Generic.Builder.parameterizedType(Enum.class, TargetType.class).asType();
        return new SubclassDynamicTypeBuilder<Enum<?>>(InstrumentedType.Default.subclass(namingStrategy.subclass(enumType),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL, EnumerationState.ENUMERATION).resolve(),
                enumType),
                ignoredMethods,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Visibility.PRIVATE).withParameters(String.class, int.class)
                .intercept(MethodCall.invoke(enumType.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(String.class, int.class))).getOnly())
                        .withArgument(0, 1))
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
     * Creates a dynamic type builder for redefining of the given type. The given class must be found on the
     * class path or by the class's {@link java.lang.ClassLoader}. Otherwise, the class file to the redefined class
     * must be located explicitly by providing a locator by
     * {@link net.bytebuddy.ByteBuddy#redefine(Class, net.bytebuddy.dynamic.ClassFileLocator)}.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code type} and the
     * corresponding class file get out of sync, i.e. a type is redefined several times without providing an updated
     * version of the class file.
     * </p>
     *
     * @param type The type to redefine.
     * @param <T>  The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that redefines the given type description.
     */
    public <T> DynamicType.Builder<T> redefine(Class<T> type) {
        return redefine(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    /**
     * <p>
     * Creates a dynamic type builder for redefining of the given type.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code levelType} and the
     * corresponding class file get out of sync, i.e. a type is redefined several times without providing an updated
     * version of the class file.
     * </p>
     *
     * @param type             The type to redefine.
     * @param classFileLocator A locator for finding a class file that represents a type.
     * @param <T>              The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that redefines the given type description.
     */
    public <T> DynamicType.Builder<T> redefine(Class<T> type, ClassFileLocator classFileLocator) {
        return redefine(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    /**
     * <p>
     * Creates a dynamic type builder for redefining of the given type.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code typeDescription} and the
     * corresponding class file get out of sync, i.e. a type is redefined several times without providing an updated
     * version of the class file.
     * </p>
     *
     * @param typeDescription  The type to redefine.
     * @param classFileLocator A locator for finding a class file that represents a type.
     * @param <T>              The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that redefines the given type description.
     */
    public <T> DynamicType.Builder<T> redefine(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return new RedefinitionDynamicTypeBuilder<T>(InstrumentedType.Default.represent(typeDescription),
                ignoredMethods,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                typeDescription,
                classFileLocator);
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics. The given class must be found on the class path or
     * by the provided class's {@link java.lang.ClassLoader}. Otherwise, the class file to the redefined class
     * must be located explicitly by providing a locator by
     * {@link net.bytebuddy.ByteBuddy#rebase(Class, net.bytebuddy.dynamic.ClassFileLocator)}.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code type} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param type The type which is to be rebased.
     * @param <T>  The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> type) {
        return rebase(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code type} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param type             The type which is to be rebased.
     * @param classFileLocator A locator for finding a class file that represents a type.
     * @param <T>              The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> type, ClassFileLocator classFileLocator) {
        return rebase(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code type} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param type                  The type which is to be rebased.
     * @param classFileLocator      A locator for finding a class file that represents a type.
     * @param methodNameTransformer The method name transformer that is used for rebasing methods.
     * @param <T>                   The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> type,
                                             ClassFileLocator classFileLocator,
                                             MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        return rebase(new TypeDescription.ForLoadedType(type), classFileLocator, methodNameTransformer);
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code typeDescription} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param typeDescription  The type which is to be rebased.
     * @param classFileLocator A locator for finding a class file that represents a type.
     * @param <T>              The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return rebase(typeDescription, classFileLocator, MethodRebaseResolver.MethodNameTransformer.Suffixing.withRandomSuffix());
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code typeDescription} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param typeDescription       The type which is to be rebased.
     * @param classFileLocator      A locator for finding a class file that represents a type.
     * @param methodNameTransformer The method name transformer that is used for rebasing methods.
     * @param <T>                   The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(TypeDescription typeDescription,
                                             ClassFileLocator classFileLocator,
                                             MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        return new RebaseDynamicTypeBuilder<T>(InstrumentedType.Default.represent(typeDescription),
                ignoredMethods,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                typeDescription,
                classFileLocator,
                methodNameTransformer);
    }

    public ByteBuddy with(ClassFileVersion classFileVersion) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                implementationContextFactory,
                ignoredMethods,
                methodGraphCompiler);
    }

    public ByteBuddy with(NamingStrategy namingStrategy) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                implementationContextFactory,
                ignoredMethods,
                methodGraphCompiler);
    }

    public ByteBuddy with(AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                implementationContextFactory,
                ignoredMethods,
                methodGraphCompiler);
    }

    public ByteBuddy with(Implementation.Context.Factory implementationContextFactory) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                implementationContextFactory,
                ignoredMethods,
                methodGraphCompiler);
    }

    public ByteBuddy with(MethodGraph.Compiler methodGraphCompiler) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                implementationContextFactory,
                ignoredMethods,
                methodGraphCompiler);
    }

    public ByteBuddy ignore(ElementMatcher<? super MethodDescription> ignoredMethods) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                implementationContextFactory,
                ignoredMethods,
                methodGraphCompiler);
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
                        TargetType.GENERIC_DESCRIPTION));
            }
            return instrumentedType
                    .withField(new FieldDescription.Token(ENUM_VALUES,
                            ENUM_FIELD_MODIFIERS | Opcodes.ACC_SYNTHETIC,
                            TypeDescription.Generic.OfGenericArray.Latent.of(TargetType.GENERIC_DESCRIPTION, 1)))
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
                            TypeCreation.forType(instrumentedType),
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
                        ArrayFactory.forType(instrumentedType).withValues(fieldGetters),
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

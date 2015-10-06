package net.bytebuddy;

import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.*;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.MethodTransformer;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.dynamic.scaffold.inline.RebaseDynamicTypeBuilder;
import net.bytebuddy.dynamic.scaffold.inline.RedefinitionDynamicTypeBuilder;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.attribute.AnnotationAppender;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
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
import net.bytebuddy.matcher.LatentMethodMatcher;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

/**
 * {@code ByteBuddy} instances are configurable factories for creating new Java types at a JVM's runtime.
 * Such types are represented by {@link net.bytebuddy.dynamic.DynamicType}s which can be saved to disk or loaded into
 * the Java virtual machine. Each instance of {@code ByteBuddy} is immutable where any of the factory methods returns
 * a new instance that represents the altered configuration.
 * <p>&nbsp;</p>
 * Note that any configuration defines not to implement any synthetic methods or the default finalizer method
 * {@link Object#finalize()}. This behavior can be altered by
 * {@link net.bytebuddy.ByteBuddy#withIgnoredMethods(net.bytebuddy.matcher.ElementMatcher)}.
 */
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

    /**
     * The naming strategy of the current configuration.
     */
    protected final NamingStrategy.Unbound namingStrategy;

    /**
     * The naming strategy for auxiliary types of the current configuation.
     */
    protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    /**
     * A list of interface types to be implemented by any class that is implemented by the current configuration.
     */
    protected final List<TypeDescription> interfaceTypes;

    /**
     * A matcher for identifying methods that should never be intercepted.
     */
    protected final ElementMatcher<? super MethodDescription> ignoredMethods;

    /**
     * The class visitor wrapper chain for the current configuration.
     */
    protected final ClassVisitorWrapper.Chain classVisitorWrapperChain;

    /**
     * The method registry for the current configuration.
     */
    protected final MethodRegistry methodRegistry;

    /**
     * The modifiers to apply to any type that is generated by this configuration.
     */
    protected final Definable<Integer> modifiers;

    /**
     * The method graph compiler to use.
     */
    protected final MethodGraph.Compiler methodGraphCompiler;

    /**
     * The type attribute appender factory to apply to any type that is generated by this configuration.
     */
    protected final TypeAttributeAppender typeAttributeAppender;

    /**
     * The default field attribute appender factory which is applied to any field that is defined
     * for implementations that are applied by this configuration.
     */
    protected final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;

    /**
     * The default method attribute appender factory which is applied to any method that is defined
     * or intercepted for implementations that are applied by this configuration.
     */
    protected final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;

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
        this(nonNull(classFileVersion),
                new NamingStrategy.Unbound.Default(BYTE_BUDDY_DEFAULT_PREFIX),
                new AuxiliaryType.NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_SUFFIX),
                new TypeList.Empty(),
                isSynthetic().or(isDefaultFinalizer()),
                new ClassVisitorWrapper.Chain(),
                new MethodRegistry.Default(),
                new Definable.Undefined<Integer>(),
                TypeAttributeAppender.NoOp.INSTANCE,
                MethodGraph.Compiler.DEFAULT,
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE);
    }

    /**
     * Defines a new {@code ByteBuddy} configuration.
     *
     * @param classFileVersion                      The currently defined class file version.
     * @param namingStrategy                        The currently defined naming strategy.
     * @param auxiliaryTypeNamingStrategy           The currently defined naming strategy for auxiliary types.
     * @param interfaceTypes                        The currently defined collection of interfaces to be implemented
     *                                              by any dynamically created type.
     * @param ignoredMethods                        The methods to always be ignored.
     *                                              process.
     * @param classVisitorWrapperChain              The class visitor wrapper chain to be applied to any implementation
     *                                              process.
     * @param methodRegistry                        The currently valid method registry.
     * @param modifiers                             The modifiers to define for any implementation process.
     * @param typeAttributeAppender                 The type attribute appender to apply to any implementation process.
     * @param methodGraphCompiler                   The method graph compiler to use.
     * @param defaultFieldAttributeAppenderFactory  The field attribute appender to apply as a default for any field
     *                                              definition.
     * @param defaultMethodAttributeAppenderFactory The method attribute appender to apply as a default for any
     *                                              method definition or implementation.
     */
    protected ByteBuddy(ClassFileVersion classFileVersion,
                        NamingStrategy.Unbound namingStrategy,
                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                        List<TypeDescription> interfaceTypes,
                        ElementMatcher<? super MethodDescription> ignoredMethods,
                        ClassVisitorWrapper.Chain classVisitorWrapperChain,
                        MethodRegistry methodRegistry,
                        Definable<Integer> modifiers,
                        TypeAttributeAppender typeAttributeAppender,
                        MethodGraph.Compiler methodGraphCompiler,
                        FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                        MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory) {
        this.classFileVersion = classFileVersion;
        this.namingStrategy = namingStrategy;
        this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
        this.interfaceTypes = interfaceTypes;
        this.ignoredMethods = ignoredMethods;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.methodRegistry = methodRegistry;
        this.modifiers = modifiers;
        this.typeAttributeAppender = typeAttributeAppender;
        this.methodGraphCompiler = methodGraphCompiler;
        this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
        this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
    }

    /**
     * Returns the class file version that is defined for the current configuration.
     *
     * @return The class file version that is defined for this configuration.
     */
    public ClassFileVersion getClassFileVersion() {
        return classFileVersion;
    }

    /**
     * Returns the naming strategy for the current configuration.
     *
     * @return The naming strategy for the current configuration.
     */
    public NamingStrategy.Unbound getNamingStrategy() {
        return namingStrategy;
    }

    /**
     * Returns the naming strategy for the current configuration.
     *
     * @return The naming strategy for the current configuration.
     */
    public List<TypeDescription> getInterfaceTypes() {
        return Collections.unmodifiableList(interfaceTypes);
    }

    /**
     * Returns the matcher for the ignored methods for the current configuration.
     *
     * @return The matcher for the ignored methods for the current configuration.
     */
    public ElementMatcher<? super MethodDescription> getIgnoredMethods() {
        return ignoredMethods;
    }

    /**
     * Returns the class visitor wrapper chain for the current configuration.
     *
     * @return The class visitor wrapper chain for the current configuration.
     */
    public ClassVisitorWrapper.Chain getClassVisitorWrapperChain() {
        return classVisitorWrapperChain;
    }

    /**
     * Returns the method registry for the current configuration.
     *
     * @return The method registry for the current configuration.
     */
    public MethodRegistry getMethodRegistry() {
        return methodRegistry;
    }

    /**
     * Returns the modifiers to apply to any type that is generated by this configuration.
     *
     * @return The modifiers to apply to any type that is generated by this configuration.
     */
    public Definable<Integer> getModifiers() {
        return modifiers;
    }

    /**
     * Returns the method graph compiler that is used.
     *
     * @return The method graph compiler that is used.
     */
    public MethodGraph.Compiler getMethodGraphCompiler() {
        return methodGraphCompiler;
    }

    /**
     * Returns the type attribute appender factory to apply to any type that is generated by this configuration.
     *
     * @return The type attribute appender factory to apply to any type that is generated by this configuration.
     */
    public TypeAttributeAppender getTypeAttributeAppender() {
        return typeAttributeAppender;
    }

    /**
     * Returns the default field attribute appender factory which is applied to any field that is defined
     * for implementations that are applied by this configuration.
     *
     * @return The default field attribute appender factory which is applied to any field that is defined
     * for implementations that are applied by this configuration.
     */
    public FieldAttributeAppender.Factory getDefaultFieldAttributeAppenderFactory() {
        return defaultFieldAttributeAppenderFactory;
    }

    /**
     * Returns the default method attribute appender factory which is applied to any method that is defined
     * or intercepted for implementations that are applied by this configuration.
     *
     * @return The default method attribute appender factory which is applied to any method that is defined
     * or intercepted for implementations that are applied by this configuration.
     */
    public MethodAttributeAppender.Factory getDefaultMethodAttributeAppenderFactory() {
        return defaultMethodAttributeAppenderFactory;
    }

    /**
     * Returns the used naming strategy for auxiliary types.
     *
     * @return The used naming strategy for auxiliary types.
     */
    public AuxiliaryType.NamingStrategy getAuxiliaryTypeNamingStrategy() {
        return auxiliaryTypeNamingStrategy;
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
        return subclass(new TypeDescription.ForLoadedType(nonNull(superType)));
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
        return subclass(new TypeDescription.ForLoadedType(nonNull(superType)), constructorStrategy);
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
    public <T> DynamicType.Builder<T> subclass(TypeDescription superType) {
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
    public <T> DynamicType.Builder<T> subclass(TypeDescription superType, ConstructorStrategy constructorStrategy) {
        TypeDescription actualSuperType = isExtendable(superType);
        List<TypeDescription> interfaceTypes = this.interfaceTypes;
        if (nonNull(superType).isInterface()) {
            actualSuperType = TypeDescription.OBJECT;
            interfaceTypes = joinUniqueRaw(interfaceTypes, Collections.singleton(superType));
        }
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                nonNull(namingStrategy.subclass(superType)),
                auxiliaryTypeNamingStrategy,
                actualSuperType,
                interfaceTypes,
                modifiers.resolve(superType.getModifiers() & ~TypeManifestation.ANNOTATION.getMask()),
                typeAttributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                nonNull(constructorStrategy));
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
        return (DynamicType.Builder<T>) makeInterface(Collections.<TypeDescription>singletonList(new TypeDescription.ForLoadedType(nonNull(type))));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param type The interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(Class<?>... type) {
        return makeInterface(new TypeList.ForLoadedType(nonNull(type)));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param types The interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(Iterable<? extends Class<?>> types) {
        return makeInterface(new TypeList.ForLoadedType(toList(types)));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param typeDescription Descriptions of the interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(TypeDescription... typeDescription) {
        return makeInterface(Arrays.asList(typeDescription));
    }

    /**
     * Creates a dynamic type builder for an interface that extends a number of given interfaces.
     *
     * @param typeDescriptions The interface types to extend.
     * @return A dynamic type builder for this configuration that defines an interface that extends the specified
     * interfaces.
     */
    public DynamicType.Builder<?> makeInterface(Collection<? extends TypeDescription> typeDescriptions) {
        return new SubclassDynamicTypeBuilder<Object>(classFileVersion,
                namingStrategy.create(),
                auxiliaryTypeNamingStrategy,
                TypeDescription.OBJECT,
                join(interfaceTypes, toList(nonNull(typeDescriptions))),
                modifiers.resolve(Opcodes.ACC_PUBLIC) | TypeManifestation.INTERFACE.getMask(),
                typeAttributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    /**
     * Creates a new Java package. A Java package is represented as a Java type with name <i>package-info</i>. The explicit creation of a
     * package can be useful for adding annotations to this package.
     *
     * @param name The name of the package.
     * @return A dynamic type that represents the created package.
     */
    public DynamicType.Builder<?> makePackage(String name) {
        return new SubclassDynamicTypeBuilder<Class<?>>(classFileVersion,
                new NamingStrategy.Fixed(isValidIdentifier(name) + "." + PackageDescription.PACKAGE_CLASS_NAME),
                auxiliaryTypeNamingStrategy,
                TypeDescription.OBJECT,
                new TypeList.Empty(),
                PackageDescription.PACKAGE_MODIFIERS,
                typeAttributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    /**
     * Rebases the given the package. A Java package is represented as a type named <i>package-info</i>. The explicit creation of a
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
     * Rebases the given the package. A Java package is represented as a type named <i>package-info</i>. The explicit creation of a
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
    @SuppressWarnings("unchecked")
    public DynamicType.Builder<? extends Annotation> makeAnnotation() {
        return (DynamicType.Builder<? extends Annotation>) (Object) new SubclassDynamicTypeBuilder<Object>(classFileVersion,
                namingStrategy.create(),
                auxiliaryTypeNamingStrategy,
                TypeDescription.OBJECT,
                Collections.<TypeDescription>singletonList(new TypeDescription.ForLoadedType(Annotation.class)),
                modifiers.resolve(Opcodes.ACC_PUBLIC) | TypeManifestation.ANNOTATION.getMask(),
                typeAttributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
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
    @SuppressWarnings("unchecked")
    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(Collection<? extends String> values) {
        if (unique(nonNull(values)).size() == 0) {
            throw new IllegalArgumentException("Require at least one enumeration constant");
        }
        return new SubclassDynamicTypeBuilder<Enum<?>>(classFileVersion,
                nonNull(namingStrategy.subclass(TypeDescription.ENUM)),
                auxiliaryTypeNamingStrategy,
                TypeDescription.ENUM,
                interfaceTypes,
                Visibility.PUBLIC.getMask() | TypeManifestation.FINAL.getMask() | EnumerationState.ENUMERATION.getMask(),
                typeAttributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Arrays.<Class<?>>asList(String.class, int.class), Visibility.PRIVATE)
                .intercept(MethodCall.invoke(TypeDescription.ENUM.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(String.class, int.class))).getOnly())
                        .withArgument(0, 1))
                .defineMethod(EnumerationImplementation.ENUM_VALUE_OF_METHOD_NAME,
                        TargetType.class,
                        Collections.<Class<?>>singletonList(String.class),
                        Visibility.PUBLIC, Ownership.STATIC)
                .intercept(MethodCall.invoke(TypeDescription.ENUM.getDeclaredMethods()
                        .filter(named(EnumerationImplementation.ENUM_VALUE_OF_METHOD_NAME).and(takesArguments(Class.class, String.class))).getOnly())
                        .withOwnType().withArgument(0)
                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .defineMethod(EnumerationImplementation.ENUM_VALUES_METHOD_NAME,
                        TargetType[].class,
                        Collections.<Class<?>>emptyList(),
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
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code levelType} and the
     * corresponding class file get out of sync, i.e. a type is redefined several times without providing an updated
     * version of the class file.
     * </p>
     *
     * @param levelType The type to redefine.
     * @param <T>       The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that redefines the given type description.
     */
    public <T> DynamicType.Builder<T> redefine(Class<T> levelType) {
        return redefine(levelType, ClassFileLocator.ForClassLoader.of(levelType.getClassLoader()));
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
     * @param levelType        The type to redefine.
     * @param classFileLocator A locator for finding a class file that represents a type.
     * @param <T>              The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that redefines the given type description.
     */
    public <T> DynamicType.Builder<T> redefine(Class<T> levelType, ClassFileLocator classFileLocator) {
        return redefine(new TypeDescription.ForLoadedType(nonNull(levelType)), classFileLocator);
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
     * @param levelType        The type to redefine.
     * @param classFileLocator A locator for finding a class file that represents a type.
     * @param <T>              The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that redefines the given type description.
     */
    public <T> DynamicType.Builder<T> redefine(TypeDescription levelType, ClassFileLocator classFileLocator) {
        return new RedefinitionDynamicTypeBuilder<T>(classFileVersion,
                nonNull(namingStrategy.redefine(levelType)),
                auxiliaryTypeNamingStrategy,
                nonNull(levelType),
                interfaceTypes,
                modifiers.resolve(levelType.getModifiers()),
                typeAttributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                nonNull(classFileLocator));
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
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code levelType} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param levelType The type which is to be rebased.
     * @param <T>       The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> levelType) {
        return rebase(levelType, ClassFileLocator.ForClassLoader.of(levelType.getClassLoader()));
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code levelType} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param levelType        The type which is to be rebased.
     * @param classFileLocator A locator for finding a class file that represents a type.
     * @param <T>              The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> levelType, ClassFileLocator classFileLocator) {
        return rebase(new TypeDescription.ForLoadedType(nonNull(levelType)), classFileLocator);
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code levelType} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param levelType             The type which is to be rebased.
     * @param classFileLocator      A locator for finding a class file that represents a type.
     * @param methodNameTransformer The method name transformer that is used for rebasing methods.
     * @param <T>                   The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(Class<T> levelType,
                                             ClassFileLocator classFileLocator,
                                             MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        return rebase(new TypeDescription.ForLoadedType(nonNull(levelType)), classFileLocator, methodNameTransformer);
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code levelType} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param levelType        The type which is to be rebased.
     * @param classFileLocator A locator for finding a class file that represents a type.
     * @param <T>              The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(TypeDescription levelType, ClassFileLocator classFileLocator) {
        return rebase(levelType, classFileLocator, MethodRebaseResolver.MethodNameTransformer.Suffixing.withRandomSuffix());
    }

    /**
     * <p>
     * Creates a dynamic type by weaving any changes into an already defined <i>level type</i>. The rebased type is
     * created by adding methods to the <i>level type</i> where the original method implementations are copied to
     * renamed, private methods within the created dynamic type and therefore remain invokable as super method calls.
     * The result is a rebased type with subclass semantics.
     * </p>
     * <p>
     * <b>Note</b>: It is possible to experience unexpected errors in case that the provided {@code levelType} and the
     * corresponding class file get out of sync, i.e. a type is rebased several times without updating the class file.
     * </p>
     *
     * @param levelType             The type which is to be rebased.
     * @param classFileLocator      A locator for finding a class file that represents a type.
     * @param methodNameTransformer The method name transformer that is used for rebasing methods.
     * @param <T>                   The most specific known type that the created dynamic type represents.
     * @return A dynamic type builder for this configuration that creates a rebased version of the given type.
     */
    public <T> DynamicType.Builder<T> rebase(TypeDescription levelType,
                                             ClassFileLocator classFileLocator,
                                             MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        return new RebaseDynamicTypeBuilder<T>(classFileVersion,
                nonNull(namingStrategy.rebase(isDefineable(levelType))),
                auxiliaryTypeNamingStrategy,
                levelType,
                interfaceTypes,
                modifiers.resolve(levelType.getModifiers()),
                TypeAttributeAppender.NoOp.INSTANCE,
                ignoredMethods,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                nonNull(classFileLocator),
                nonNull(methodNameTransformer));
    }

    /**
     * Defines a new class file version for this configuration.
     *
     * @param classFileVersion The class file version to define for this configuration.
     * @return A new configuration that represents this configuration with the given class file version.
     */
    public ByteBuddy withClassFileVersion(ClassFileVersion classFileVersion) {
        return new ByteBuddy(nonNull(classFileVersion),
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new naming strategy for this configuration.
     *
     * @param namingStrategy The unbound naming strategy to apply to the current configuration.
     * @return A new configuration that represents this configuration with the given unbound naming strategy.
     */
    public ByteBuddy withNamingStrategy(NamingStrategy.Unbound namingStrategy) {
        return new ByteBuddy(classFileVersion,
                nonNull(namingStrategy),
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new naming strategy for this configuration.
     *
     * @param namingStrategy The naming strategy to apply to the current configuration.
     * @return A new configuration that represents this configuration with the given naming strategy.
     */
    public ByteBuddy withNamingStrategy(NamingStrategy namingStrategy) {
        return withNamingStrategy(new NamingStrategy.Unbound.Unified(nonNull(namingStrategy)));
    }

    /**
     * Defines a naming strategy for auxiliary types.
     *
     * @param auxiliaryTypeNamingStrategy The naming strategy to use.
     * @return This configuration with the defined naming strategy for auxiliary types.
     */
    public ByteBuddy withNamingStrategy(AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                nonNull(auxiliaryTypeNamingStrategy),
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new modifier contributors for this configuration that replaces the currently defined modifier
     * contributes which might currently be implicit.
     *
     * @param modifierContributor The modifier contributors to define explicitly for this configuration.
     * @return A new configuration that represents this configuration with the given modifier contributors.
     */
    public ByteBuddy withModifiers(ModifierContributor.ForType... modifierContributor) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                new Definable.Defined<Integer>(resolveModifierContributors(TYPE_MODIFIER_MASK, nonNull(modifierContributor))),
                typeAttributeAppender,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new type attribute appender for this configuration that replaces the currently defined type
     * attribute appender.
     *
     * @param typeAttributeAppender The type attribute appender to define for this configuration.
     * @return A new configuration that represents this configuration with the given type attribute appender.
     */
    public ByteBuddy withAttribute(TypeAttributeAppender typeAttributeAppender) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                nonNull(typeAttributeAppender),
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new type annotation for this configuration that replaces the currently defined type
     * attribute appender.
     *
     * @param annotation The type annotations to define for this configuration.
     * @return A new configuration that represents this configuration with the given annotations as its new
     * type attribute appender.
     */
    public ByteBuddy withTypeAnnotation(Annotation... annotation) {
        return withTypeAnnotation(new AnnotationList.ForLoadedAnnotation(nonNull(annotation)));
    }

    /**
     * Defines a new type annotation for this configuration that replaces the currently defined type
     * attribute appender.
     *
     * @param annotations The type annotations to define for this configuration.
     * @return A new configuration that represents this configuration with the given annotations as its new
     * type attribute appender.
     */
    public ByteBuddy withTypeAnnotation(Iterable<? extends Annotation> annotations) {
        return withTypeAnnotation(new AnnotationList.ForLoadedAnnotation(toList(annotations)));
    }

    /**
     * Defines a new type annotation for this configuration that replaces the currently defined type
     * attribute appender.
     *
     * @param annotation The type annotations to define for this configuration.
     * @return A new configuration that represents this configuration with the given annotations as its new
     * type attribute appender.
     */
    public ByteBuddy withTypeAnnotation(AnnotationDescription... annotation) {
        return withTypeAnnotation(Arrays.asList(annotation));
    }

    /**
     * Defines a new type annotation for this configuration that replaces the currently defined type
     * attribute appender.
     *
     * @param annotations The type annotations to define for this configuration.
     * @return A new configuration that represents this configuration with the given annotations as its new
     * type attribute appender.
     */
    public ByteBuddy withTypeAnnotation(Collection<? extends AnnotationDescription> annotations) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                new TypeAttributeAppender.ForAnnotation(new ArrayList<AnnotationDescription>(nonNull(annotations)), AnnotationAppender.ValueFilter.AppendDefaults.INSTANCE),
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines all dynamic types that are created by this configuration to implement the given interfaces.
     *
     * @param type The interface types to implement.
     * @return The same configuration where any dynamic type that is created by the resulting configuration will
     * implement the given interfaces.
     */
    public OptionalMethodInterception withImplementing(Class<?>... type) {
        return withImplementing(new TypeList.ForLoadedType(nonNull(type)));
    }

    /**
     * Defines all dynamic types that are created by this configuration to implement the given interfaces.
     *
     * @param types The interface types to implement.
     * @return The same configuration where any dynamic type that is created by the resulting configuration will
     * implement the given interfaces.
     */
    public OptionalMethodInterception withImplementing(Iterable<? extends Class<?>> types) {
        return withImplementing(new TypeList.ForLoadedType(toList(types)));
    }

    /**
     * Defines all dynamic types that are created by this configuration to implement the given interfaces.
     *
     * @param type The interface types to implement.
     * @return The same configuration where any dynamic type that is created by the resulting configuration will
     * implement the given interfaces.
     */
    public OptionalMethodInterception withImplementing(TypeDescription... type) {
        return withImplementing(Arrays.asList(type));
    }

    /**
     * Defines all dynamic types that are created by this configuration to implement the given interfaces.
     *
     * @param types The interface types to implement.
     * @return The same configuration where any dynamic type that is created by the resulting configuration will
     * implement the given interfaces.
     */
    public OptionalMethodInterception withImplementing(Collection<? extends TypeDescription> types) {
        return new OptionalMethodInterception(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                joinUniqueRaw(interfaceTypes, toList(isImplementable(types))),
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                new LatentMethodMatcher.Resolved(isDeclaredBy(anyOf(new GenericTypeList.Explicit(toList(types)).asErasures()))));
    }

    /**
     * Defines a new method matcher for methods that are ignored by any dynamic type that is created by this
     * configuration which will replace the current configuration. By default, this method matcher is defined
     * to ignore instrumenting synthetic methods and the default finalizer method. The only exception from per-default
     * overridable synthetic methods are bridge methods which is only implemented by the Java compiler to increase a
     * method's visibility.
     *
     * @param ignoredMethods The methods to always be ignored for any instrumentation.
     * @return A new configuration that represents this configuration with the given method matcher defining methods
     * that are to be ignored for any instrumentation.
     */
    public ByteBuddy withIgnoredMethods(ElementMatcher<? super MethodDescription> ignoredMethods) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                nonNull(ignoredMethods),
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new class visitor to be appended to the current collection of {@link org.objectweb.asm.ClassVisitor}s
     * that are to be applied onto any creation process of a dynamic type.
     *
     * @param classVisitorWrapper The class visitor wrapper to ba appended to the current chain of class visitor wrappers.
     * @return The same configuration with the given class visitor wrapper to be applied onto any creation process of a dynamic type.
     */
    public ByteBuddy withClassVisitor(ClassVisitorWrapper classVisitorWrapper) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain.append(nonNull(classVisitorWrapper)),
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new method graph compiler to be used for extracting a type's invokable methods.
     *
     * @param methodGraphCompiler The method graph compiler to use.
     * @return The same configuration with the given method graph compiler to be applied onto any creation process of a dynamic type.
     */
    public ByteBuddy withMethodGraphCompiler(MethodGraph.Compiler methodGraphCompiler) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                nonNull(methodGraphCompiler),
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new default field attribute appender factory that is applied onto any field.
     *
     * @param attributeAppenderFactory The attribute appender factory that is applied as a default on any
     *                                 field that is created by a dynamic type that is created with this
     *                                 configuration.
     * @return The same configuration with the given field attribute appender factory to be applied as a default to
     * the creation process of any field of a dynamic type.
     */
    public ByteBuddy withDefaultFieldAttributeAppender(FieldAttributeAppender.Factory attributeAppenderFactory) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                methodGraphCompiler,
                nonNull(attributeAppenderFactory),
                defaultMethodAttributeAppenderFactory);
    }

    /**
     * Defines a new default method attribute appender factory that is applied onto any method.
     *
     * @param attributeAppenderFactory The attribute appender factory that is applied as a default on any
     *                                 method that is created or intercepted by a dynamic type that is created
     *                                 with this configuration.
     * @return The same configuration with the given method attribute appender factory to be applied as a default to
     * the creation or interception process of any method of a dynamic type.
     */
    public ByteBuddy withDefaultMethodAttributeAppender(MethodAttributeAppender.Factory attributeAppenderFactory) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                interfaceTypes,
                ignoredMethods,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                nonNull(attributeAppenderFactory));
    }

    /**
     * Intercepts a given selection of byte code level methods, i.e. a method, a constructor or the type initializer.
     *
     * @param methodMatcher The method matcher representing all byte code methods to intercept.
     * @return A matched method interception for the given selection.
     */
    public MatchedMethodInterception invokable(ElementMatcher<? super MethodDescription> methodMatcher) {
        return invokable(new LatentMethodMatcher.Resolved(nonNull(methodMatcher)));
    }

    /**
     * Intercepts a given selection of byte code level methods, i.e. a method, a constructor or the type initializer.
     *
     * @param methodMatcher The latent method matcher representing all byte code methods to intercept.
     * @return A matched method interception for the given selection.
     */
    public MatchedMethodInterception invokable(LatentMethodMatcher methodMatcher) {
        return new MatchedMethodInterception(nonNull(methodMatcher));
    }

    /**
     * Intercepts a given method selection.
     *
     * @param methodMatcher The method matcher representing all methods to intercept.
     * @return A matched method interception for the given selection.
     */
    public MatchedMethodInterception method(ElementMatcher<? super MethodDescription> methodMatcher) {
        return invokable(isMethod().and(nonNull(methodMatcher)));
    }

    /**
     * Intercepts a given constructor selection.
     *
     * @param methodMatcher The method matcher representing all constructors to intercept.
     * @return A matched method interception for the given selection.
     */
    public MatchedMethodInterception constructor(ElementMatcher<? super MethodDescription> methodMatcher) {
        return invokable(isConstructor().and(nonNull(methodMatcher)));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        ByteBuddy byteBuddy = (ByteBuddy) other;
        return classFileVersion.equals(byteBuddy.classFileVersion)
                && classVisitorWrapperChain.equals(byteBuddy.classVisitorWrapperChain)
                && defaultFieldAttributeAppenderFactory.equals(byteBuddy.defaultFieldAttributeAppenderFactory)
                && defaultMethodAttributeAppenderFactory.equals(byteBuddy.defaultMethodAttributeAppenderFactory)
                && ignoredMethods.equals(byteBuddy.ignoredMethods)
                && interfaceTypes.equals(byteBuddy.interfaceTypes)
                && methodGraphCompiler.equals(byteBuddy.methodGraphCompiler)
                && methodRegistry.equals(byteBuddy.methodRegistry)
                && modifiers.equals(byteBuddy.modifiers)
                && namingStrategy.equals(byteBuddy.namingStrategy)
                && auxiliaryTypeNamingStrategy.equals(byteBuddy.auxiliaryTypeNamingStrategy)
                && typeAttributeAppender.equals(byteBuddy.typeAttributeAppender);
    }

    @Override
    public int hashCode() {
        int result = classFileVersion.hashCode();
        result = 31 * result + namingStrategy.hashCode();
        result = 31 * result + auxiliaryTypeNamingStrategy.hashCode();
        result = 31 * result + interfaceTypes.hashCode();
        result = 31 * result + ignoredMethods.hashCode();
        result = 31 * result + classVisitorWrapperChain.hashCode();
        result = 31 * result + methodRegistry.hashCode();
        result = 31 * result + modifiers.hashCode();
        result = 31 * result + methodGraphCompiler.hashCode();
        result = 31 * result + typeAttributeAppender.hashCode();
        result = 31 * result + defaultFieldAttributeAppenderFactory.hashCode();
        result = 31 * result + defaultMethodAttributeAppenderFactory.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ByteBuddy{" +
                "classFileVersion=" + classFileVersion +
                ", namingStrategy=" + namingStrategy +
                ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                ", interfaceTypes=" + interfaceTypes +
                ", ignoredMethods=" + ignoredMethods +
                ", classVisitorWrapperChain=" + classVisitorWrapperChain +
                ", methodRegistry=" + methodRegistry +
                ", modifiers=" + modifiers +
                ", methodGraphCompiler=" + methodGraphCompiler +
                ", typeAttributeAppender=" + typeAttributeAppender +
                ", defaultFieldAttributeAppenderFactory=" + defaultFieldAttributeAppenderFactory +
                ", defaultMethodAttributeAppenderFactory=" + defaultMethodAttributeAppenderFactory +
                '}';
    }

    /**
     * Any definable instance is either {@link net.bytebuddy.ByteBuddy.Definable.Defined} when a value is provided
     * or {@link net.bytebuddy.ByteBuddy.Definable.Undefined} if a value is not provided. A defined definable will
     * return its defined value on request while an undefined definable will return the provided default.
     *
     * @param <T> The type of the definable object.
     */
    protected interface Definable<T> {

        /**
         * Returns the value of this instance or the provided default value for an undefined definable.
         *
         * @param defaultValue The default value that is returned for an {@link net.bytebuddy.ByteBuddy.Definable.Undefined}
         *                     definable.
         * @return The value that is represented by this instance.
         */
        T resolve(T defaultValue);

        /**
         * Checks if this value is explicitly defined.
         *
         * @return {@code true} if this value is defined.
         */
        boolean isDefined();

        /**
         * A representation of an undefined {@link net.bytebuddy.ByteBuddy.Definable}.
         *
         * @param <T> The type of the definable object.
         */
        class Undefined<T> implements Definable<T> {

            @Override
            public T resolve(T defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean isDefined() {
                return false;
            }

            @Override
            public boolean equals(Object other) {
                return other != null && other.getClass() == getClass();
            }

            @Override
            public int hashCode() {
                return 31;
            }

            @Override
            public String toString() {
                return "ByteBuddy.Definable.Undefined{}";
            }
        }

        /**
         * A representation of a defined {@link net.bytebuddy.ByteBuddy.Definable} for a given value.
         *
         * @param <T> The type of the definable object.
         */
        class Defined<T> implements Definable<T> {

            /**
             * The value that is represented by this defined definable.
             */
            private final T value;

            /**
             * Creates a new defined instance for the given value.
             *
             * @param value The defined value.
             */
            public Defined(T value) {
                this.value = value;
            }

            @Override
            public T resolve(T defaultValue) {
                return value;
            }

            @Override
            public boolean isDefined() {
                return true;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value.equals(((Defined) other).value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }

            @Override
            public String toString() {
                return "ByteBuddy.Definable.Defined{value=" + value + '}';
            }
        }
    }

    /**
     * Implementations of this interface are capable of defining a method interception for a given set of methods.
     */
    public interface MethodInterceptable {

        /**
         * Intercepts the currently selected methods with the provided implementation. If this intercepted method is
         * not yet declared by the current type, it might be added to the currently built type as a result of this
         * interception. If the method is already declared by the current type, its byte code code might be copied
         * into the body of a synthetic method in order to preserve the original code's invokeability.
         *
         * @param implementation The implementation to apply to the currently selected method.
         * @return A configuration which will intercept the currently selected methods by the given implementation.
         */
        MethodAnnotationTarget intercept(Implementation implementation);

        /**
         * Implements the currently selected methods as {@code abstract} methods.
         *
         * @return A configuration which will implement the currently selected methods as {@code abstract} methods.
         */
        MethodAnnotationTarget withoutCode();

        /**
         * Defines a default annotation value to set for any matched method.
         *
         * @param value The value that the annotation property should set as a default.
         * @param type  The type of the annotation property.
         * @return A configuration which defines the given default value for all matched methods.
         */
        MethodAnnotationTarget withDefaultValue(Object value, Class<?> type);

        /**
         * Defines a default annotation value to set for any matched method. The value is to be represented in a wrapper format,
         * {@code enum} values should be handed as {@link net.bytebuddy.description.enumeration.EnumerationDescription}
         * instances, annotations as {@link AnnotationDescription} instances and
         * {@link Class} values as {@link TypeDescription} instances. Other values are handed in their actual form or as their wrapper types.
         *
         * @param value A non-loaded value that the annotation property should set as a default.
         * @return A configuration which defines the given default value for all matched methods.
         */
        MethodAnnotationTarget withDefaultValue(Object value);
    }

    /**
     * A {@link net.bytebuddy.ByteBuddy} configuration with a selected set of methods for which annotations can
     * be defined.
     */
    public static class MethodAnnotationTarget extends Proxy {

        /**
         * The method matcher representing the current method selection.
         */
        protected final LatentMethodMatcher methodMatcher;

        /**
         * The handler for the entry that is to be registered.
         */
        protected final MethodRegistry.Handler handler;

        /**
         * The method attribute appender factory that was defined for the current method selection.
         */
        protected final MethodAttributeAppender.Factory attributeAppenderFactory;

        /**
         * The method transformer to apply.
         */
        protected final MethodTransformer methodTransformer;

        /**
         * Creates a new method annotation target.
         *
         * @param classFileVersion                      The currently defined class file version.
         * @param namingStrategy                        The currently defined naming strategy.
         * @param auxiliaryTypeNamingStrategy           The currently defined naming strategy for auxiliary types.
         * @param interfaceTypes                        The currently defined collection of interfaces to be implemented
         *                                              by any dynamically created type.
         * @param ignoredMethods                        The methods to always be ignored.
         *                                              process.
         * @param classVisitorWrapperChain              The class visitor wrapper chain to be applied to any implementation
         *                                              process.
         * @param methodRegistry                        The currently valid method registry.
         * @param modifiers                             The modifiers to define for any implementation process.
         * @param typeAttributeAppender                 The type attribute appender to apply to any implementation process.
         * @param methodGraphCompiler                   The method graph compiler to use.
         * @param defaultFieldAttributeAppenderFactory  The field attribute appender to apply as a default for any field
         *                                              definition.
         * @param defaultMethodAttributeAppenderFactory The method attribute appender to apply as a default for any
         *                                              method definition or implementation.
         * @param methodMatcher                         The method matcher representing the current method selection.
         * @param handler                               The handler for the entry that is to be registered.
         * @param attributeAppenderFactory              The method attribute appender factory that was defined for the current method selection.
         * @param methodTransformer                     The method transformer to apply.
         */
        protected MethodAnnotationTarget(ClassFileVersion classFileVersion,
                                         NamingStrategy.Unbound namingStrategy,
                                         AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                         List<TypeDescription> interfaceTypes,
                                         ElementMatcher<? super MethodDescription> ignoredMethods,
                                         ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                         MethodRegistry methodRegistry,
                                         Definable<Integer> modifiers,
                                         TypeAttributeAppender typeAttributeAppender,
                                         MethodGraph.Compiler methodGraphCompiler,
                                         FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                         MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                         LatentMethodMatcher methodMatcher,
                                         MethodRegistry.Handler handler,
                                         MethodAttributeAppender.Factory attributeAppenderFactory,
                                         MethodTransformer methodTransformer) {
            super(classFileVersion,
                    namingStrategy,
                    auxiliaryTypeNamingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    methodGraphCompiler,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory);
            this.methodMatcher = methodMatcher;
            this.handler = handler;
            this.attributeAppenderFactory = attributeAppenderFactory;
            this.methodTransformer = methodTransformer;
        }

        /**
         * Defines a given attribute appender factory to be applied for the currently selected methods.
         *
         * @param attributeAppenderFactory The method attribute appender factory to apply to the currently
         *                                 selected methods.
         * @return A method annotation target that represents the current configuration with the additional
         * attribute appender factory applied to the current method selection.
         */
        public MethodAnnotationTarget attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new MethodAnnotationTarget(classFileVersion,
                    namingStrategy,
                    auxiliaryTypeNamingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    methodGraphCompiler,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    methodMatcher,
                    handler,
                    new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory, nonNull(attributeAppenderFactory)),
                    methodTransformer);
        }

        /**
         * Defines an method annotation for the currently selected methods.
         *
         * @param annotation The annotations to defined for the currently selected methods.
         * @return A method annotation target that represents the current configuration with the additional
         * annotations added to the currently selected methods.
         */
        public MethodAnnotationTarget annotateMethod(Annotation... annotation) {
            return annotateMethod(new AnnotationList.ForLoadedAnnotation(nonNull(annotation)));
        }

        /**
         * Defines an method annotation for the currently selected methods.
         *
         * @param annotation The annotations to defined for the currently selected methods.
         * @return A method annotation target that represents the current configuration with the additional
         * annotations added to the currently selected methods.
         */
        public MethodAnnotationTarget annotateMethod(AnnotationDescription... annotation) {
            return annotateMethod(new AnnotationList.Explicit(Arrays.asList(nonNull(annotation))));
        }

        /**
         * Defines an method annotation for the currently selected methods.
         *
         * @param annotations The annotations to defined for the currently selected methods.
         * @return A method annotation target that represents the current configuration with the additional
         * annotations added to the currently selected methods.
         */
        public MethodAnnotationTarget annotateMethod(Collection<? extends AnnotationDescription> annotations) {
            return attribute(new MethodAttributeAppender.ForAnnotation(new ArrayList<AnnotationDescription>(nonNull(annotations)),
                    AnnotationAppender.ValueFilter.AppendDefaults.INSTANCE));
        }

        /**
         * Defines an method annotation for a parameter of the currently selected methods.
         *
         * @param parameterIndex The index of the parameter for which the annotations should be applied
         *                       with the first parameter index by {@code 0}.
         * @param annotation     The annotations to defined for the currently selected methods' parameters
         *                       ath the given index.
         * @return A method annotation target that represents the current configuration with the additional
         * annotations added to the currently selected methods' parameters at the given index.
         */
        public MethodAnnotationTarget annotateParameter(int parameterIndex, Annotation... annotation) {
            return annotateParameter(parameterIndex, new AnnotationList.ForLoadedAnnotation(nonNull(annotation)));
        }

        /**
         * Defines an method annotation for a parameter of the currently selected methods.
         *
         * @param parameterIndex The index of the parameter for which the annotations should be applied
         *                       with the first parameter index by {@code 0}.
         * @param annotation     The annotations to defined for the currently selected methods' parameters
         *                       ath the given index.
         * @return A method annotation target that represents the current configuration with the additional
         * annotations added to the currently selected methods' parameters at the given index.
         */
        public MethodAnnotationTarget annotateParameter(int parameterIndex, AnnotationDescription... annotation) {
            return annotateParameter(parameterIndex, new AnnotationList.Explicit(Arrays.asList(nonNull(annotation))));
        }

        /**
         * Defines an method annotation for a parameter of the currently selected methods.
         *
         * @param parameterIndex The index of the parameter for which the annotations should be applied
         *                       with the first parameter index by {@code 0}.
         * @param annotations    The annotations to defined for the currently selected methods' parameters
         *                       ath the given index.
         * @return A method annotation target that represents the current configuration with the additional
         * annotations added to the currently selected methods' parameters at the given index.
         */
        public MethodAnnotationTarget annotateParameter(int parameterIndex, Collection<? extends AnnotationDescription> annotations) {
            return attribute(new MethodAttributeAppender.ForAnnotation(parameterIndex, new ArrayList<AnnotationDescription>(nonNull(annotations)),
                    AnnotationAppender.ValueFilter.AppendDefaults.INSTANCE));
        }

        @Override
        protected ByteBuddy materialize() {
            return new ByteBuddy(classFileVersion,
                    namingStrategy,
                    auxiliaryTypeNamingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry.prepend(methodMatcher, handler, attributeAppenderFactory, methodTransformer),
                    modifiers,
                    typeAttributeAppender,
                    methodGraphCompiler,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory
            );
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null || getClass() != other.getClass())
                return false;
            if (!super.equals(other))
                return false;
            MethodAnnotationTarget that = (MethodAnnotationTarget) other;
            return attributeAppenderFactory.equals(that.attributeAppenderFactory)
                    && handler.equals(that.handler)
                    && methodMatcher.equals(that.methodMatcher)
                    && methodTransformer.equals(that.methodTransformer);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + methodMatcher.hashCode();
            result = 31 * result + handler.hashCode();
            result = 31 * result + attributeAppenderFactory.hashCode();
            result = 31 * result + methodTransformer.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ByteBuddy.MethodAnnotationTarget{" +
                    "classFileVersion=" + classFileVersion +
                    ", namingStrategy=" + namingStrategy +
                    ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                    ", interfaceTypes=" + interfaceTypes +
                    ", ignoredMethods=" + ignoredMethods +
                    ", classVisitorWrapperChain=" + classVisitorWrapperChain +
                    ", methodRegistry=" + methodRegistry +
                    ", modifiers=" + modifiers +
                    ", methodGraphCompiler=" + methodGraphCompiler +
                    ", typeAttributeAppender=" + typeAttributeAppender +
                    ", defaultFieldAttributeAppenderFactory=" + defaultFieldAttributeAppenderFactory +
                    ", defaultMethodAttributeAppenderFactory=" + defaultMethodAttributeAppenderFactory +
                    ", methodMatcher=" + methodMatcher +
                    ", handler=" + handler +
                    ", attributeAppenderFactory=" + attributeAppenderFactory +
                    ", methodTransformer=" + methodTransformer +
                    '}';
        }
    }

    /**
     * An optional method interception that allows to intercept a method selection only if this is needed.
     */
    public static class OptionalMethodInterception extends ByteBuddy implements MethodInterceptable {

        /**
         * The method matcher that defines the selected that is represented by this instance.
         */
        protected final LatentMethodMatcher methodMatcher;

        /**
         * Creates a new optional method interception.
         *
         * @param classFileVersion                      The currently defined class file version.
         * @param namingStrategy                        The currently defined naming strategy.
         * @param auxiliaryTypeNamingStrategy           The currently defined naming strategy for auxiliary types.
         * @param interfaceTypes                        The currently defined collection of interfaces to be implemented
         *                                              by any dynamically created type.
         * @param ignoredMethods                        The methods to always be ignored.
         *                                              process.
         * @param classVisitorWrapperChain              The class visitor wrapper chain to be applied to any implementation
         *                                              process.
         * @param methodRegistry                        The currently valid method registry.
         * @param modifiers                             The modifiers to define for any implementation process.
         * @param typeAttributeAppender                 The type attribute appender to apply to any implementation process.
         * @param methodGraphCompiler                   The method graph compiler to use.
         * @param defaultFieldAttributeAppenderFactory  The field attribute appender to apply as a default for any field
         *                                              definition.
         * @param defaultMethodAttributeAppenderFactory The method attribute appender to apply as a default for any
         *                                              method definition or implementation.
         * @param methodMatcher                         The method matcher representing the current method selection.
         */
        protected OptionalMethodInterception(ClassFileVersion classFileVersion,
                                             NamingStrategy.Unbound namingStrategy,
                                             AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                             List<TypeDescription> interfaceTypes,
                                             ElementMatcher<? super MethodDescription> ignoredMethods,
                                             ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                             MethodRegistry methodRegistry,
                                             Definable<Integer> modifiers,
                                             TypeAttributeAppender typeAttributeAppender,
                                             MethodGraph.Compiler methodGraphCompiler,
                                             FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                             MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                             LatentMethodMatcher methodMatcher) {
            super(classFileVersion,
                    namingStrategy,
                    auxiliaryTypeNamingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    methodGraphCompiler,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory);
            this.methodMatcher = methodMatcher;
        }

        @Override
        public MethodAnnotationTarget intercept(Implementation implementation) {
            return new MatchedMethodInterception(methodMatcher).intercept(implementation);
        }

        @Override
        public MethodAnnotationTarget withoutCode() {
            return new MatchedMethodInterception(methodMatcher).withoutCode();
        }

        @Override
        public MethodAnnotationTarget withDefaultValue(Object value, Class<?> type) {
            return new MatchedMethodInterception(methodMatcher).withDefaultValue(value, type);
        }

        @Override
        public MethodAnnotationTarget withDefaultValue(Object value) {
            return new MatchedMethodInterception(methodMatcher).withDefaultValue(value);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass()) && super.equals(other)
                    && methodMatcher.equals(((OptionalMethodInterception) other).methodMatcher);
        }

        @Override
        public int hashCode() {
            return 31 * methodMatcher.hashCode() + super.hashCode();
        }

        @Override
        public String toString() {
            return "ByteBuddy.OptionalMethodInterception{" +
                    "classFileVersion=" + classFileVersion +
                    ", namingStrategy=" + namingStrategy +
                    ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                    ", interfaceTypes=" + interfaceTypes +
                    ", ignoredMethods=" + ignoredMethods +
                    ", classVisitorWrapperChain=" + classVisitorWrapperChain +
                    ", methodRegistry=" + methodRegistry +
                    ", modifiers=" + modifiers +
                    ", methodGraphCompiler=" + methodGraphCompiler +
                    ", typeAttributeAppender=" + typeAttributeAppender +
                    ", defaultFieldAttributeAppenderFactory=" + defaultFieldAttributeAppenderFactory +
                    ", defaultMethodAttributeAppenderFactory=" + defaultMethodAttributeAppenderFactory +
                    ", methodMatcher=" + methodMatcher +
                    '}';
        }
    }

    /**
     * A proxy implementation for extending Byte Buddy while allowing for enhancing a {@link net.bytebuddy.ByteBuddy}
     * configuration.
     */
    protected abstract static class Proxy extends ByteBuddy {

        /**
         * Defines a new proxy configuration for {@code ByteBuddy}.
         *
         * @param classFileVersion                      The currently defined class file version.
         * @param namingStrategy                        The currently defined naming strategy.
         * @param auxiliaryTypeNamingStrategy           The currently defined naming strategy for auxiliary types.
         * @param interfaceTypes                        The currently defined collection of interfaces to be
         *                                              implemented by any dynamically created type.
         * @param ignoredMethods                        The methods to always be ignored.
         *                                              implementation process.
         * @param classVisitorWrapperChain              The class visitor wrapper chain to be applied to any
         *                                              implementation process.
         * @param methodRegistry                        The currently valid method registry.
         * @param modifiers                             The modifiers to define for any implementation process.
         * @param typeAttributeAppender                 The type attribute appender to apply to any implementation
         *                                              process.
         * @param methodGraphCompiler                   The method graph compiler to use.
         * @param defaultFieldAttributeAppenderFactory  The field attribute appender to apply as a default for any
         *                                              field definition.
         * @param defaultMethodAttributeAppenderFactory The method attribute appender to apply as a default for any
         *                                              method definition or implementation.
         */
        protected Proxy(ClassFileVersion classFileVersion,
                        NamingStrategy.Unbound namingStrategy,
                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                        List<TypeDescription> interfaceTypes,
                        ElementMatcher<? super MethodDescription> ignoredMethods,
                        ClassVisitorWrapper.Chain classVisitorWrapperChain,
                        MethodRegistry methodRegistry,
                        Definable<Integer> modifiers,
                        TypeAttributeAppender typeAttributeAppender,
                        MethodGraph.Compiler methodGraphCompiler,
                        FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                        MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory) {
            super(classFileVersion,
                    namingStrategy,
                    auxiliaryTypeNamingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    methodGraphCompiler,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory);
        }

        @Override
        public ClassFileVersion getClassFileVersion() {
            return materialize().getClassFileVersion();
        }

        @Override
        public NamingStrategy.Unbound getNamingStrategy() {
            return materialize().getNamingStrategy();
        }

        @Override
        public List<TypeDescription> getInterfaceTypes() {
            return materialize().getInterfaceTypes();
        }

        @Override
        public ElementMatcher<? super MethodDescription> getIgnoredMethods() {
            return materialize().getIgnoredMethods();
        }

        @Override
        public ClassVisitorWrapper.Chain getClassVisitorWrapperChain() {
            return materialize().getClassVisitorWrapperChain();
        }

        @Override
        public MethodRegistry getMethodRegistry() {
            return materialize().getMethodRegistry();
        }

        @Override
        public Definable<Integer> getModifiers() {
            return materialize().getModifiers();
        }

        @Override
        public MethodGraph.Compiler getMethodGraphCompiler() {
            return materialize().getMethodGraphCompiler();
        }

        @Override
        public TypeAttributeAppender getTypeAttributeAppender() {
            return materialize().getTypeAttributeAppender();
        }

        @Override
        public FieldAttributeAppender.Factory getDefaultFieldAttributeAppenderFactory() {
            return materialize().getDefaultFieldAttributeAppenderFactory();
        }

        @Override
        public MethodAttributeAppender.Factory getDefaultMethodAttributeAppenderFactory() {
            return materialize().getDefaultMethodAttributeAppenderFactory();
        }

        @Override
        public AuxiliaryType.NamingStrategy getAuxiliaryTypeNamingStrategy() {
            return materialize().getAuxiliaryTypeNamingStrategy();
        }

        @Override
        public <T> DynamicType.Builder<T> subclass(Class<T> superType) {
            return materialize().subclass(superType);
        }

        @Override
        public <T> DynamicType.Builder<T> subclass(Class<T> superType, ConstructorStrategy constructorStrategy) {
            return materialize().subclass(superType, constructorStrategy);
        }

        @Override
        public <T> DynamicType.Builder<T> subclass(TypeDescription superType) {
            return materialize().subclass(superType);
        }

        @Override
        public <T> DynamicType.Builder<T> subclass(TypeDescription superType,
                                                   ConstructorStrategy constructorStrategy) {
            return materialize().subclass(superType, constructorStrategy);
        }

        @Override
        public <T> DynamicType.Builder<T> redefine(Class<T> levelType) {
            return materialize().redefine(levelType);
        }

        @Override
        public <T> DynamicType.Builder<T> redefine(Class<T> levelType,
                                                   ClassFileLocator classFileLocator) {
            return materialize().redefine(levelType, classFileLocator);
        }

        @Override
        public <T> DynamicType.Builder<T> redefine(TypeDescription levelType,
                                                   ClassFileLocator classFileLocator) {
            return materialize().redefine(levelType, classFileLocator);
        }

        @Override
        public <T> DynamicType.Builder<T> rebase(Class<T> levelType) {
            return materialize().rebase(levelType);
        }

        @Override
        public <T> DynamicType.Builder<T> rebase(Class<T> levelType,
                                                 ClassFileLocator classFileLocator) {
            return materialize().rebase(levelType, classFileLocator);
        }

        @Override
        public <T> DynamicType.Builder<T> rebase(Class<T> levelType,
                                                 ClassFileLocator classFileLocator,
                                                 MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
            return materialize().rebase(levelType, classFileLocator, methodNameTransformer);
        }

        @Override
        public <T> DynamicType.Builder<T> rebase(TypeDescription levelType,
                                                 ClassFileLocator classFileLocator) {
            return materialize().rebase(levelType, classFileLocator);
        }

        @Override
        public <T> DynamicType.Builder<T> rebase(TypeDescription levelType,
                                                 ClassFileLocator classFileLocator,
                                                 MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
            return materialize().rebase(levelType, classFileLocator, methodNameTransformer);
        }

        @Override
        public ByteBuddy withClassFileVersion(ClassFileVersion classFileVersion) {
            return materialize().withClassFileVersion(classFileVersion);
        }

        @Override
        public ByteBuddy withNamingStrategy(NamingStrategy.Unbound namingStrategy) {
            return materialize().withNamingStrategy(namingStrategy);
        }

        @Override
        public ByteBuddy withNamingStrategy(NamingStrategy namingStrategy) {
            return materialize().withNamingStrategy(namingStrategy);
        }

        @Override
        public ByteBuddy withNamingStrategy(AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy) {
            return materialize().withNamingStrategy(auxiliaryTypeNamingStrategy);
        }

        @Override
        public ByteBuddy withModifiers(ModifierContributor.ForType... modifierContributor) {
            return materialize().withModifiers(modifierContributor);
        }

        @Override
        public ByteBuddy withAttribute(TypeAttributeAppender typeAttributeAppender) {
            return materialize().withAttribute(typeAttributeAppender);
        }

        @Override
        public ByteBuddy withTypeAnnotation(Annotation... annotation) {
            return materialize().withTypeAnnotation(annotation);
        }

        @Override
        public ByteBuddy withTypeAnnotation(Iterable<? extends Annotation> annotations) {
            return materialize().withTypeAnnotation(annotations);
        }

        @Override
        public ByteBuddy withTypeAnnotation(Collection<? extends AnnotationDescription> annotations) {
            return materialize().withTypeAnnotation(annotations);
        }

        @Override
        public OptionalMethodInterception withImplementing(Class<?>... type) {
            return materialize().withImplementing(type);
        }

        @Override
        public OptionalMethodInterception withImplementing(Iterable<? extends Class<?>> types) {
            return materialize().withImplementing(types);
        }

        @Override
        public OptionalMethodInterception withImplementing(TypeDescription... type) {
            return materialize().withImplementing(type);
        }

        @Override
        public OptionalMethodInterception withImplementing(Collection<? extends TypeDescription> types) {
            return materialize().withImplementing(types);
        }

        @Override
        public ByteBuddy withIgnoredMethods(ElementMatcher<? super MethodDescription> ignoredMethods) {
            return materialize().withIgnoredMethods(ignoredMethods);
        }

        @Override
        public ByteBuddy withClassVisitor(ClassVisitorWrapper classVisitorWrapper) {
            return materialize().withClassVisitor(classVisitorWrapper);
        }

        @Override
        public ByteBuddy withMethodGraphCompiler(MethodGraph.Compiler methodGraphCompiler) {
            return materialize().withMethodGraphCompiler(methodGraphCompiler);
        }

        @Override
        public ByteBuddy withDefaultFieldAttributeAppender(FieldAttributeAppender.Factory attributeAppenderFactory) {
            return materialize().withDefaultFieldAttributeAppender(attributeAppenderFactory);
        }

        @Override
        public ByteBuddy withDefaultMethodAttributeAppender(MethodAttributeAppender.Factory attributeAppenderFactory) {
            return materialize().withDefaultMethodAttributeAppender(attributeAppenderFactory);
        }

        @Override
        public MatchedMethodInterception invokable(ElementMatcher<? super MethodDescription> methodMatcher) {
            return materialize().invokable(methodMatcher);
        }

        @Override
        public DynamicType.Builder<?> makeInterface() {
            return materialize().makeInterface();
        }

        @Override
        public <T> DynamicType.Builder<T> makeInterface(Class<T> type) {
            return materialize().makeInterface(type);
        }

        @Override
        public DynamicType.Builder<?> makeInterface(Class<?>... type) {
            return materialize().makeInterface(type);
        }

        @Override
        public DynamicType.Builder<?> makeInterface(TypeDescription... typeDescription) {
            return materialize().makeInterface(typeDescription);
        }

        @Override
        public DynamicType.Builder<?> makeInterface(Iterable<? extends Class<?>> types) {
            return materialize().makeInterface(types);
        }

        @Override
        public DynamicType.Builder<?> makeInterface(Collection<? extends TypeDescription> typeDescriptions) {
            return materialize().makeInterface(typeDescriptions);
        }

        @Override
        public DynamicType.Builder<? extends Annotation> makeAnnotation() {
            return materialize().makeAnnotation();
        }

        @Override
        public DynamicType.Builder<? extends Enum<?>> makeEnumeration(String... value) {
            return materialize().makeEnumeration(value);
        }

        @Override
        public DynamicType.Builder<? extends Enum<?>> makeEnumeration(Collection<? extends String> values) {
            return materialize().makeEnumeration(values);
        }

        @Override
        public DynamicType.Builder<?> makePackage(String name) {
            return materialize().makePackage(name);
        }

        @Override
        public DynamicType.Builder<?> rebase(Package aPackage, ClassFileLocator classFileLocator) {
            return materialize().rebase(aPackage, classFileLocator);
        }

        @Override
        public DynamicType.Builder<?> rebase(PackageDescription packageDescription, ClassFileLocator classFileLocator) {
            return materialize().rebase(packageDescription, classFileLocator);
        }

        @Override
        public ByteBuddy withTypeAnnotation(AnnotationDescription... annotation) {
            return materialize().withTypeAnnotation(annotation);
        }

        @Override
        public MatchedMethodInterception invokable(LatentMethodMatcher methodMatcher) {
            return materialize().invokable(methodMatcher);
        }

        @Override
        public MatchedMethodInterception method(ElementMatcher<? super MethodDescription> methodMatcher) {
            return materialize().method(methodMatcher);
        }

        @Override
        public MatchedMethodInterception constructor(ElementMatcher<? super MethodDescription> methodMatcher) {
            return materialize().constructor(methodMatcher);
        }

        /**
         * Materializes the current extended configuration.
         *
         * @return The materialized Byte Buddy configuration.
         */
        protected abstract ByteBuddy materialize();
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
                        TargetType.DESCRIPTION));
            }
            return instrumentedType
                    .withField(new FieldDescription.Token(ENUM_VALUES,
                            ENUM_FIELD_MODIFIERS | Opcodes.ACC_SYNTHETIC,
                            TypeDescription.ArrayProjection.of(TargetType.DESCRIPTION, 1)))
                    .withInitializer(new InitializationAppender(values));
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new ValuesMethodAppender(implementationTarget.getTypeDescription());
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
                MethodDescription cloneMethod = TypeDescription.OBJECT.getDeclaredMethods().filter(named(CLONE_METHOD_NAME)).getOnly();
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

    /**
     * A matched method interception for a non-optional method definition.
     */
    public class MatchedMethodInterception implements MethodInterceptable {

        /**
         * A method matcher that represents the current method selection.
         */
        protected final LatentMethodMatcher methodMatcher;

        /**
         * Creates a new matched method interception.
         *
         * @param methodMatcher The method matcher representing the current method selection.
         */
        protected MatchedMethodInterception(LatentMethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public MethodAnnotationTarget intercept(Implementation implementation) {
            return new MethodAnnotationTarget(classFileVersion,
                    namingStrategy,
                    auxiliaryTypeNamingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    methodGraphCompiler,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    methodMatcher,
                    new MethodRegistry.Handler.ForImplementation(nonNull(implementation)),
                    MethodAttributeAppender.NoOp.INSTANCE,
                    MethodTransformer.NoOp.INSTANCE);
        }

        @Override
        public MethodAnnotationTarget withoutCode() {
            return new MethodAnnotationTarget(classFileVersion,
                    namingStrategy,
                    auxiliaryTypeNamingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    methodGraphCompiler,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    methodMatcher,
                    MethodRegistry.Handler.ForAbstractMethod.INSTANCE,
                    MethodAttributeAppender.NoOp.INSTANCE,
                    MethodTransformer.NoOp.INSTANCE);
        }

        @Override
        public MethodAnnotationTarget withDefaultValue(Object value, Class<?> type) {
            return withDefaultValue(AnnotationDescription.ForLoadedAnnotation.describe(nonNull(value), new TypeDescription.ForLoadedType(nonNull(type))));
        }

        @Override
        public MethodAnnotationTarget withDefaultValue(Object value) {
            return new MethodAnnotationTarget(classFileVersion,
                    namingStrategy,
                    auxiliaryTypeNamingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    methodGraphCompiler,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    methodMatcher,
                    MethodRegistry.Handler.ForAnnotationValue.of(value),
                    MethodAttributeAppender.NoOp.INSTANCE,
                    MethodTransformer.NoOp.INSTANCE);
        }

        /**
         * Returns the outer class instance of this instance.
         *
         * @return The outer class instance.
         */
        private ByteBuddy getByteBuddy() {
            return ByteBuddy.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && ByteBuddy.this.equals(((MatchedMethodInterception) other).getByteBuddy())
                    && methodMatcher.equals(((MatchedMethodInterception) other).methodMatcher);
        }

        @Override
        public int hashCode() {
            return 31 * methodMatcher.hashCode() + ByteBuddy.this.hashCode();
        }

        @Override
        public String toString() {
            return "ByteBuddy.MatchedMethodInterception{" +
                    "methodMatcher=" + methodMatcher +
                    "byteBuddy=" + ByteBuddy.this.toString() +
                    '}';
        }
    }
}

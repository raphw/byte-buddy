/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
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
import net.bytebuddy.dynamic.scaffold.ClassWriterStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.inline.DecoratingDynamicTypeBuilder;
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
 * Instead, the following method chain is correct use of the API:
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
@HashCodeAndEqualsPlugin.Enhance
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
     * The instrumented type factory to use.
     */
    protected final InstrumentedType.Factory instrumentedTypeFactory;

    /**
     * A matcher for identifying methods that should be excluded from instrumentation.
     */
    protected final LatentMatcher<? super MethodDescription> ignoredMethods;

    /**
     * Determines if a type should be explicitly validated.
     */
    protected final TypeValidation typeValidation;

    /**
     * The class writer strategy to use.
     */
    protected final ClassWriterStrategy classWriterStrategy;

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
                InstrumentedType.Factory.Default.MODIFIABLE,
                TypeValidation.ENABLED,
                ClassWriterStrategy.Default.CONSTANT_POOL_RETAINING,
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
     * @param instrumentedTypeFactory      The instrumented type factory to use.
     * @param typeValidation               Determines if a type should be explicitly validated.
     * @param classWriterStrategy          The class writer strategy to use.
     * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
     */
    protected ByteBuddy(ClassFileVersion classFileVersion,
                        NamingStrategy namingStrategy,
                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                        AnnotationValueFilter.Factory annotationValueFilterFactory,
                        AnnotationRetention annotationRetention,
                        Implementation.Context.Factory implementationContextFactory,
                        MethodGraph.Compiler methodGraphCompiler,
                        InstrumentedType.Factory instrumentedTypeFactory,
                        TypeValidation typeValidation,
                        ClassWriterStrategy classWriterStrategy,
                        LatentMatcher<? super MethodDescription> ignoredMethods) {
        this.classFileVersion = classFileVersion;
        this.namingStrategy = namingStrategy;
        this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
        this.annotationValueFilterFactory = annotationValueFilterFactory;
        this.annotationRetention = annotationRetention;
        this.implementationContextFactory = implementationContextFactory;
        this.methodGraphCompiler = methodGraphCompiler;
        this.instrumentedTypeFactory = instrumentedTypeFactory;
        this.typeValidation = typeValidation;
        this.classWriterStrategy = classWriterStrategy;
        this.ignoredMethods = ignoredMethods;
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
     * <p>
     * <b>Note</b>: This methods implements the supplied types in a generified state if they declare type variables or an owner type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param superType The super class or interface type to extend.
     * @param <T>       A loaded type that the generated class is guaranteed to inherit.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    @SuppressWarnings("unchecked")
    public <T> DynamicType.Builder<T> subclass(Class<T> superType) {
        return (DynamicType.Builder<T>) subclass(TypeDescription.ForLoadedType.of(superType));
    }

    /**
     * <p>
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types in a generified state if they declare type variables or an owner type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param superType           The super class or interface type to extend.
     * @param constructorStrategy A constructor strategy that determines the
     * @param <T>                 A loaded type that the generated class is guaranteed to inherit.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    @SuppressWarnings("unchecked")
    public <T> DynamicType.Builder<T> subclass(Class<T> superType, ConstructorStrategy constructorStrategy) {
        return (DynamicType.Builder<T>) subclass(TypeDescription.ForLoadedType.of(superType), constructorStrategy);
    }

    /**
     * <p>
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     * </p>
     * <p>
     * When extending a class, Byte Buddy imitates all visible constructors of the subclassed type. Any constructor is implemented
     * to only invoke its super type constructor of equal signature. Another behavior can be specified by supplying an explicit
     * {@link ConstructorStrategy} by {@link ByteBuddy#subclass(Type, ConstructorStrategy)}.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link Class} values are implemented
     * as raw types if they declare type variables.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param superType The super class or interface type to extend. The type must be a raw type or parameterized type. All type
     *                  variables that are referenced by the generic type must be declared by the generated subclass before creating
     *                  the type.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public DynamicType.Builder<?> subclass(Type superType) {
        return subclass(TypeDefinition.Sort.describe(superType));
    }

    /**
     * <p>
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link Class} values are implemented
     * as raw types if they declare type variables.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param superType           The super class or interface type to extend. The type must be a raw type or parameterized
     *                            type. All type variables that are referenced by the generic type must be declared by the
     *                            generated subclass before creating the type.
     * @param constructorStrategy A constructor strategy that determines the
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public DynamicType.Builder<?> subclass(Type superType, ConstructorStrategy constructorStrategy) {
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
     * <p>
     * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link TypeDescription} values are implemented
     * as raw types if they declare type variables.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param superType The super class or interface type to extend. The type must be a raw type or parameterized type. All type
     *                  variables that are referenced by the generic type must be declared by the generated subclass before creating
     *                  the type.
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public DynamicType.Builder<?> subclass(TypeDefinition superType) {
        return subclass(superType, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING);
    }

    /**
     * <p>
     * Creates a new builder for subclassing the provided type. If the provided type is an interface, a new class implementing
     * this interface type is created.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link TypeDescription} values are implemented
     * as raw types if they declare type variables.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param superType           The super class or interface type to extend. The type must be a raw type or parameterized
     *                            type. All type variables that are referenced by the generic type must be declared by the
     *                            generated subclass before creating the type.
     * @param constructorStrategy A constructor strategy that determines the
     * @return A type builder for creating a new class extending the provided class or interface.
     */
    public DynamicType.Builder<?> subclass(TypeDefinition superType, ConstructorStrategy constructorStrategy) {
        TypeDescription.Generic actualSuperType;
        TypeList.Generic interfaceTypes;
        if (superType.isPrimitive() || superType.isArray() || superType.isFinal()) {
            throw new IllegalArgumentException("Cannot subclass primitive, array or final types: " + superType);
        } else if (superType.isInterface()) {
            actualSuperType = TypeDescription.Generic.OBJECT;
            interfaceTypes = new TypeList.Generic.Explicit(superType);
        } else {
            actualSuperType = superType.asGenericType();
            interfaceTypes = new TypeList.Generic.Empty();
        }
        return new SubclassDynamicTypeBuilder<Object>(instrumentedTypeFactory.subclass(namingStrategy.subclass(superType.asGenericType()),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.PLAIN).resolve(superType.getModifiers()),
                actualSuperType).withInterfaces(interfaceTypes),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                ignoredMethods,
                constructorStrategy);
    }

    /**
     * <p>
     * Creates a new, plain interface type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @return A type builder that creates a new interface type.
     */
    public DynamicType.Builder<?> makeInterface() {
        return makeInterface(Collections.<TypeDescription>emptyList());
    }

    /**
     * <p>
     * Creates a new interface type that extends the provided interface.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types in a generified state if they declare type variables or an owner type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
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
     * <p>
     * Creates a new interface type that extends the provided interface.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link Class} values are implemented
     * as raw types if they declare type variables or an owner type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param interfaceType The interface types to implement. The types must be raw or parameterized types. All type
     *                      variables that are referenced by a parameterized type must be declared by the generated
     *                      subclass before creating the type.
     * @return A type builder that creates a new interface type.
     */
    public DynamicType.Builder<?> makeInterface(Type... interfaceType) {
        return makeInterface(Arrays.asList(interfaceType));
    }

    /**
     * <p>
     * Creates a new interface type that extends the provided interface.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link Class} values are implemented
     * as raw types if they declare type variables or an owner type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
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
     * <p>
     * Creates a new interface type that extends the provided interface.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link TypeDescription} values are implemented
     * as raw types if they declare type variables or an owner type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
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
     * <p>
     * Creates a new interface type that extends the provided interface.
     * </p>
     * <p>
     * <b>Note</b>: This methods implements the supplied types <i>as is</i>, i.e. any {@link TypeDescription} values are implemented
     * as raw types if they declare type variables or an owner type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
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
     * <p>
     * Creates a new package definition. Package definitions are defined by classes named {@code package-info}
     * without any methods or fields but permit annotations. Any field or method definition will cause an
     * {@link IllegalStateException} to be thrown when the type is created.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param name The fully qualified name of the package.
     * @return A type builder that creates a {@code package-info} class file.
     */
    public DynamicType.Builder<?> makePackage(String name) {
        return new SubclassDynamicTypeBuilder<Object>(instrumentedTypeFactory.subclass(name + "." + PackageDescription.PACKAGE_CLASS_NAME,
                PackageDescription.PACKAGE_MODIFIERS,
                TypeDescription.Generic.OBJECT),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                ignoredMethods,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    /**
     * <p>
     * Creates a new {@link Annotation} type. Annotation properties are implemented as non-static, public methods with the
     * property type being defined as the return type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @return A type builder that creates a new {@link Annotation} type.
     */
    public DynamicType.Builder<? extends Annotation> makeAnnotation() {
        return new SubclassDynamicTypeBuilder<Annotation>(instrumentedTypeFactory.subclass(namingStrategy.subclass(TypeDescription.Generic.ANNOTATION),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.ANNOTATION).resolve(),
                TypeDescription.Generic.OBJECT).withInterfaces(new TypeList.Generic.Explicit(TypeDescription.Generic.ANNOTATION)),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                ignoredMethods,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    /**
     * <p>
     * Creates a new {@link Enum} type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param value The names of the type's enumeration constants
     * @return A type builder for creating an enumeration type.
     */
    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(String... value) {
        return makeEnumeration(Arrays.asList(value));
    }

    /**
     * <p>
     * Creates a new {@link Enum} type.
     * </p>
     * <p>
     * <b>Note</b>: Byte Buddy does not cache previous subclasses but will attempt the generation of a new subclass. For caching
     * types, a external cache or {@link TypeCache} should be used.
     * </p>
     *
     * @param values The names of the type's enumeration constants
     * @return A type builder for creating an enumeration type.
     */
    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(Collection<? extends String> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Require at least one enumeration constant");
        }
        TypeDescription.Generic enumType = TypeDescription.Generic.Builder.parameterizedType(Enum.class, TargetType.class).build();
        return new SubclassDynamicTypeBuilder<Enum<?>>(instrumentedTypeFactory.subclass(namingStrategy.subclass(enumType),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL, EnumerationState.ENUMERATION).resolve(),
                enumType),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
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
        return redefine(TypeDescription.ForLoadedType.of(type), classFileLocator);
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
        if (type.isArray() || type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot redefine array or primitive type: " + type);
        }
        return new RedefinitionDynamicTypeBuilder<T>(instrumentedTypeFactory.represent(type),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
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
     * @param type The type that is being rebased.
     * @param <T>  The loaded type of the rebased type.
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
        return rebase(TypeDescription.ForLoadedType.of(type), classFileLocator);
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
        return rebase(TypeDescription.ForLoadedType.of(type), classFileLocator, methodNameTransformer);
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
        if (type.isArray() || type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot rebase array or primitive type: " + type);
        }
        return new RebaseDynamicTypeBuilder<T>(instrumentedTypeFactory.represent(type),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
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
     * <p>
     * Decorates a type with {@link net.bytebuddy.asm.AsmVisitorWrapper} and allows adding attributes and annotations. A decoration does
     * not allow for any standard transformations but can be used as a performance optimization compared to a redefinition, especially
     * when implementing a Java agent that only applies ASM-based code changes.
     * </p>
     * <p>
     * <b>Important</b>: Only use this mode to improve performance in a narrowly defined transformation. Using other features as those mentioned
     * might result in an unexpected outcome of the transformation or error. Using decoration also requires the configuration of an
     * {@link Implementation.Context.Factory} that does not attempt any type transformation.
     * </p>
     *
     * @param type The type to decorate.
     * @param <T>  The loaded type of the decorated type.
     * @return A type builder for decorating the provided type.
     */
    public <T> DynamicType.Builder<T> decorate(Class<T> type) {
        return decorate(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    /**
     * <p>
     * Decorates a type with {@link net.bytebuddy.asm.AsmVisitorWrapper} and allows adding attributes and annotations. A decoration does
     * not allow for any standard transformations but can be used as a performance optimization compared to a redefinition, especially
     * when implementing a Java agent that only applies ASM-based code changes.
     * </p>
     * <p>
     * <b>Important</b>: Only use this mode to improve performance in a narrowly defined transformation. Using other features as those mentioned
     * might result in an unexpected outcome of the transformation or error. Using decoration also requires the configuration of an
     * {@link Implementation.Context.Factory} that does not attempt any type transformation.
     * </p>
     *
     * @param type             The type to decorate.
     * @param classFileLocator The class file locator to use.
     * @param <T>              The loaded type of the decorated type.
     * @return A type builder for decorating the provided type.
     */
    public <T> DynamicType.Builder<T> decorate(Class<T> type, ClassFileLocator classFileLocator) {
        return decorate(TypeDescription.ForLoadedType.of(type), classFileLocator);
    }

    /**
     * <p>
     * Decorates a type with {@link net.bytebuddy.asm.AsmVisitorWrapper} and allows adding attributes and annotations. A decoration does
     * not allow for any standard transformations but can be used as a performance optimization compared to a redefinition, especially
     * when implementing a Java agent that only applies ASM-based code changes.
     * </p>
     * <p>
     * <b>Important</b>: Only use this mode to improve performance in a narrowly defined transformation. Using other features as those mentioned
     * might result in an unexpected outcome of the transformation or error. Using decoration also requires the configuration of an
     * {@link Implementation.Context.Factory} that does not attempt any type transformation.
     * </p>
     *
     * @param type             The type to decorate.
     * @param classFileLocator The class file locator to use.
     * @param <T>              The loaded type of the decorated type.
     * @return A type builder for decorating the provided type.
     */
    public <T> DynamicType.Builder<T> decorate(TypeDescription type, ClassFileLocator classFileLocator) {
        if (type.isArray() || type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot decorate array or primitive type: " + type);
        }
        return new DecoratingDynamicTypeBuilder<T>(type,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                ignoredMethods,
                classFileLocator);
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
                ignoredMethods);
    }

    /**
     * Configures Byte Buddy to use the specified factory for creating {@link InstrumentedType}s. Doing so, more efficient
     * representations can be chosen when only certain operations are required. By default, all operations are supported.
     *
     * @param instrumentedTypeFactory The factory to use when creating instrumented types.
     * @return A new Byte Buddy instance that uses the supplied factory for creating instrumented types.
     */
    public ByteBuddy with(InstrumentedType.Factory instrumentedTypeFactory) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
                ignoredMethods);
    }

    /**
     * Creates a new configuration that applies the supplied class writer strategy. By default, the constant pool of redefined and retransformed
     * classes is retained as most changes are additive and this retention improves performance.
     *
     * @param classWriterStrategy The class writer strategy to apply during type creation.
     * @return A new Byte Buddy instance that applies the supplied class writer strategy.
     */
    public ByteBuddy with(ClassWriterStrategy classWriterStrategy) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
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
                instrumentedTypeFactory,
                typeValidation,
                classWriterStrategy,
                ignoredMethods);
    }

    /**
     * An implementation fo the {@code values} method of an enumeration type.
     */
    @HashCodeAndEqualsPlugin.Enhance
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

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return new ValuesMethodAppender(implementationTarget.getInstrumentedType());
        }

        /**
         * A byte code appender for the {@code values} method of any enumeration type.
         */
        @HashCodeAndEqualsPlugin.Enhance
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

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                FieldDescription valuesField = instrumentedType.getDeclaredFields().filter(named(ENUM_VALUES)).getOnly();
                MethodDescription cloneMethod = TypeDescription.Generic.OBJECT.getDeclaredMethods().filter(named(CLONE_METHOD_NAME)).getOnly();
                return new Size(new StackManipulation.Compound(
                        FieldAccess.forField(valuesField).read(),
                        MethodInvocation.invoke(cloneMethod).virtual(valuesField.getType().asErasure()),
                        TypeCasting.to(valuesField.getType().asErasure()),
                        MethodReturn.REFERENCE
                ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }

        /**
         * A byte code appender for the type initializer of any enumeration type.
         */
        @HashCodeAndEqualsPlugin.Enhance
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

            /**
             * {@inheritDoc}
             */
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
                            FieldAccess.forField(fieldDescription).write());
                    enumerationFields.add(fieldDescription);
                }
                List<StackManipulation> fieldGetters = new ArrayList<StackManipulation>(values.size());
                for (FieldDescription fieldDescription : enumerationFields) {
                    fieldGetters.add(FieldAccess.forField(fieldDescription).read());
                }
                stackManipulation = new StackManipulation.Compound(
                        stackManipulation,
                        ArrayFactory.forType(instrumentedType.asGenericType()).withValues(fieldGetters),
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(ENUM_VALUES)).getOnly()).write()
                );
                return new Size(stackManipulation.apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }
}

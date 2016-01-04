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

    protected final ClassFileVersion classFileVersion;

    protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

    protected final AnnotationRetention annotationRetention;

    protected final NamingStrategy namingStrategy;

    protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    protected final Implementation.Context.Factory implementationContextFactory;

    protected final MethodGraph.Compiler methodGraphCompiler;

    protected final ElementMatcher<? super MethodDescription> ignoredMethods;

    public ByteBuddy() {
        this(ClassFileVersion.forCurrentJavaVersion());
    }

    public ByteBuddy(ClassFileVersion classFileVersion) {
        this(classFileVersion,
                new NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_PREFIX),
                new AuxiliaryType.NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_SUFFIX),
                AnnotationValueFilter.Default.APPEND_DEFAULTS,
                AnnotationRetention.ENABLED,
                Implementation.Context.Default.Factory.INSTANCE,
                MethodGraph.Compiler.DEFAULT,
                isSynthetic().or(isDefaultFinalizer()));
    }

    protected ByteBuddy(ClassFileVersion classFileVersion,
                        NamingStrategy namingStrategy,
                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                        AnnotationValueFilter.Factory annotationValueFilterFactory,
                        AnnotationRetention annotationRetention,
                        Implementation.Context.Factory implementationContextFactory,
                        MethodGraph.Compiler methodGraphCompiler,
                        ElementMatcher<? super MethodDescription> ignoredMethods) {
        this.classFileVersion = classFileVersion;
        this.namingStrategy = namingStrategy;
        this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
        this.annotationValueFilterFactory = annotationValueFilterFactory;
        this.annotationRetention = annotationRetention;
        this.implementationContextFactory = implementationContextFactory;
        this.ignoredMethods = ignoredMethods;
        this.methodGraphCompiler = methodGraphCompiler;
    }

    public <T> DynamicType.Builder<T> subclass(Class<T> superType) {
        return subclass(new TypeDescription.ForLoadedType(superType));
    }

    public <T> DynamicType.Builder<T> subclass(Class<T> superType, ConstructorStrategy constructorStrategy) {
        return subclass(new TypeDescription.ForLoadedType(superType), constructorStrategy);
    }

    public <T> DynamicType.Builder<T> subclass(Type superType) {
        return subclass(TypeDefinition.Sort.describe(superType));
    }

    public <T> DynamicType.Builder<T> subclass(Type superType, ConstructorStrategy constructorStrategy) {
        return subclass(TypeDefinition.Sort.describe(superType), constructorStrategy);
    }

    public <T> DynamicType.Builder<T> subclass(TypeDefinition superType) {
        return subclass(superType, ConstructorStrategy.Default.IMITATE_SUPER_TYPE);
    }

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
        return new SubclassDynamicTypeBuilder<T>(InstrumentedType.Default.subclass(namingStrategy.subclass(superType.asGenericType()),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.PLAIN).resolve(superType.asErasure().getModifiers()),
                actualSuperType).withInterfaces(interfaceTypes),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods,
                constructorStrategy);
    }

    public DynamicType.Builder<?> makeInterface() {
        return makeInterface(Collections.<TypeDescription>emptyList());
    }

    @SuppressWarnings("unchecked")
    public <T> DynamicType.Builder<T> makeInterface(Class<T> type) {
        return (DynamicType.Builder<T>) makeInterface(Collections.<TypeDescription>singletonList(new TypeDescription.ForLoadedType(type)));
    }

    public DynamicType.Builder<?> makeInterface(Type... type) {
        return makeInterface(new TypeList.Generic.ForLoadedTypes(type));
    }

    public DynamicType.Builder<?> makeInterface(Class<?>... type) {
        return makeInterface(new TypeList.ForLoadedTypes(type));
    }

    public DynamicType.Builder<?> makeInterface(List<? extends Type> types) {
        return makeInterface(new TypeList.Generic.ForLoadedTypes(types));
    }

    public DynamicType.Builder<?> makeInterface(TypeDefinition... typeDefinition) {
        return makeInterface(Arrays.asList(typeDefinition));
    }

    public DynamicType.Builder<?> makeInterface(Collection<? extends TypeDefinition> typeDefinitions) {
        return subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS).implement(typeDefinitions).modifiers(TypeManifestation.INTERFACE);
    }

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
                ignoredMethods,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    public DynamicType.Builder<?> rebase(Package aPackage, ClassFileLocator classFileLocator) {
        return rebase(new PackageDescription.ForLoadedPackage(aPackage), classFileLocator);
    }

    public DynamicType.Builder<?> rebase(PackageDescription packageDescription, ClassFileLocator classFileLocator) {
        return rebase(new TypeDescription.ForPackageDescription(packageDescription), classFileLocator);
    }

    public DynamicType.Builder<? extends Annotation> makeAnnotation() {
        return new SubclassDynamicTypeBuilder<Annotation>(InstrumentedType.Default.subclass(namingStrategy.subclass(TypeDescription.Generic.ANNOTATION),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.ANNOTATION).resolve(),
                TypeDescription.Generic.OBJECT).withInterfaces(Collections.singletonList(TypeDescription.Generic.ANNOTATION)),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods,
                ConstructorStrategy.Default.NO_CONSTRUCTORS);
    }

    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(String... value) {
        return makeEnumeration(Arrays.asList(value));
    }

    public DynamicType.Builder<? extends Enum<?>> makeEnumeration(Collection<? extends String> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Require at least one enumeration constant");
        }
        TypeDescription.Generic enumType = TypeDescription.Generic.Builder.parameterizedType(Enum.class, TargetType.class).asType();
        return new SubclassDynamicTypeBuilder<Enum<?>>(InstrumentedType.Default.subclass(namingStrategy.subclass(enumType),
                ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL, EnumerationState.ENUMERATION).resolve(),
                enumType),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods,
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

    public <T> DynamicType.Builder<T> redefine(Class<T> type) {
        return redefine(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    public <T> DynamicType.Builder<T> redefine(Class<T> type, ClassFileLocator classFileLocator) {
        return redefine(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    public <T> DynamicType.Builder<T> redefine(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return new RedefinitionDynamicTypeBuilder<T>(InstrumentedType.Default.represent(typeDescription),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods,
                typeDescription,
                classFileLocator);
    }

    public <T> DynamicType.Builder<T> rebase(Class<T> type) {
        return rebase(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    public <T> DynamicType.Builder<T> rebase(Class<T> type, ClassFileLocator classFileLocator) {
        return rebase(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    public <T> DynamicType.Builder<T> rebase(Class<T> type,
                                             ClassFileLocator classFileLocator,
                                             MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        return rebase(new TypeDescription.ForLoadedType(type), classFileLocator, methodNameTransformer);
    }

    public <T> DynamicType.Builder<T> rebase(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return rebase(typeDescription, classFileLocator, MethodRebaseResolver.MethodNameTransformer.Suffixing.withRandomSuffix());
    }

    public <T> DynamicType.Builder<T> rebase(TypeDescription typeDescription,
                                             ClassFileLocator classFileLocator,
                                             MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        return new RebaseDynamicTypeBuilder<T>(InstrumentedType.Default.represent(typeDescription),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods,
                typeDescription,
                classFileLocator,
                methodNameTransformer);
    }

    public ByteBuddy with(ClassFileVersion classFileVersion) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods);
    }

    public ByteBuddy with(NamingStrategy namingStrategy) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods);
    }

    public ByteBuddy with(AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods);
    }

    public ByteBuddy with(AnnotationValueFilter.Factory annotationValueFilterFactory) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods);
    }

    public ByteBuddy with(AnnotationRetention annotationRetention) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods);
    }

    public ByteBuddy with(Implementation.Context.Factory implementationContextFactory) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods);
    }

    public ByteBuddy with(MethodGraph.Compiler methodGraphCompiler) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods);
    }

    public ByteBuddy ignore(ElementMatcher<? super MethodDescription> ignoredMethods) {
        return new ByteBuddy(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
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

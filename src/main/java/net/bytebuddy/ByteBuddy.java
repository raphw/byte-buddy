package net.bytebuddy;

import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.lang.annotation.Annotation;
import java.util.List;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDefaultFinalizer;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isSynthetic;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

public class ByteBuddy {

    // TODO: create abstract delegation instead of using "extends ByteBuddy"
    // TODO: Allow optional method interception also for the defaults.

    private static final String BYTE_BUDDY_DEFAULT_PREFIX = "ByteBuddy";

    protected static interface Definable<T> {

        static class Undefined<T> implements Definable<T> {

            @Override
            public T resolve(T defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean equals(Object other) {
                return other != null && other instanceof Undefined;
            }

            @Override
            public int hashCode() {
                return 31;
            }
        }

        static class Defined<T> implements Definable<T> {

            private final T value;

            public Defined(T value) {
                this.value = value;
            }

            @Override
            public T resolve(T defaultValue) {
                return value;
            }

            @Override
            public boolean equals(Object o) {
                return this == o || !(o == null || getClass() != o.getClass())
                        && value.equals(((Defined) o).value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }

            @Override
            public String toString() {
                return "Defined{value=" + value + '}';
            }
        }

        T resolve(T defaultValue);
    }

    public static class MethodAnnotationTarget extends ByteBuddy {

        protected final MethodMatcher methodMatcher;
        protected final Instrumentation instrumentation;
        protected final MethodAttributeAppender.Factory attributeAppenderFactory;

        protected MethodAnnotationTarget(ClassFormatVersion classFormatVersion,
                                         NamingStrategy namingStrategy,
                                         List<TypeDescription> interfaceTypes,
                                         MethodMatcher ignoredMethods,
                                         BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                         ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                         MethodRegistry methodRegistry,
                                         Definable<Integer> modifiers,
                                         Definable<TypeAttributeAppender> typeAttributeAppender,
                                         FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                         MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                         MethodMatcher methodMatcher,
                                         Instrumentation instrumentation,
                                         MethodAttributeAppender.Factory attributeAppenderFactory) {
            super(classFormatVersion,
                    namingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    bridgeMethodResolverFactory,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory);
            this.methodMatcher = methodMatcher;
            this.instrumentation = instrumentation;
            this.attributeAppenderFactory = attributeAppenderFactory;
        }

        public MethodAnnotationTarget attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new MethodAnnotationTarget(classFormatVersion,
                    namingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    bridgeMethodResolverFactory,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    methodMatcher,
                    instrumentation,
                    new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory, attributeAppenderFactory));
        }

        public MethodAnnotationTarget annotateMethod(Annotation... annotation) {
            return attribute(new MethodAttributeAppender.ForAnnotation(annotation));
        }

        public MethodAnnotationTarget annotateParameter(int parameterIndex, Annotation... annotation) {
            return attribute(new MethodAttributeAppender.ForAnnotation(parameterIndex, annotation));
        }

        @Override
        public ClassFormatVersion getClassFormatVersion() {
            return materialize().getClassFormatVersion();
        }

        @Override
        public NamingStrategy getNamingStrategy() {
            return materialize().getNamingStrategy();
        }

        @Override
        public List<TypeDescription> getInterfaceTypes() {
            return materialize().getInterfaceTypes();
        }

        @Override
        public MethodMatcher getIgnoredMethods() {
            return materialize().getIgnoredMethods();
        }

        @Override
        public ClassVisitorWrapper.Chain getClassVisitorWrapperChain() {
            return materialize().getClassVisitorWrapperChain();
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
        public <T> DynamicType.Builder<T> subclass(TypeDescription superType, ConstructorStrategy constructorStrategy) {
            return materialize().subclass(superType, constructorStrategy);
        }

        @Override
        public ByteBuddy modifiers(ModifierContributor.ForType... modifierContributor) {
            return materialize().modifiers(modifierContributor);
        }

        @Override
        public ByteBuddy attribute(TypeAttributeAppender typeAttributeAppender) {
            return materialize().attribute(typeAttributeAppender);
        }

        @Override
        public ByteBuddy annotateType(Annotation... annotation) {
            return materialize().annotateType(annotation);
        }

        @Override
        public ByteBuddy withClassFormatVersion(ClassFormatVersion classFormatVersion) {
            return materialize().withClassFormatVersion(classFormatVersion);
        }

        @Override
        public ByteBuddy withNamingStrategy(NamingStrategy namingStrategy) {
            return materialize().withNamingStrategy(namingStrategy);
        }

        @Override
        public ByteBuddy implement(Class<?> type) {
            return materialize().implement(type);
        }

        @Override
        public ByteBuddy implement(TypeDescription type) {
            return materialize().implement(type);
        }

        @Override
        public ByteBuddy ignoreMethods(MethodMatcher ignoredMethods) {
            return materialize().ignoreMethods(ignoredMethods);
        }

        @Override
        public ByteBuddy withClassVisitor(ClassVisitorWrapper classVisitorWrapper) {
            return materialize().withClassVisitor(classVisitorWrapper);
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
        public MatchedMethodInterception intercept(MethodMatcher methodMatcher) {
            return materialize().intercept(methodMatcher);
        }

        protected ByteBuddy materialize() {
            return new ByteBuddy(classFormatVersion,
                    namingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    bridgeMethodResolverFactory,
                    classVisitorWrapperChain,
                    methodRegistry.prepend(new MethodRegistry.LatentMethodMatcher.Simple(methodMatcher),
                            instrumentation,
                            attributeAppenderFactory),
                    modifiers,
                    typeAttributeAppender,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory
            );
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (!super.equals(other)) return false;
            MethodAnnotationTarget that = (MethodAnnotationTarget) other;
            return attributeAppenderFactory.equals(that.attributeAppenderFactory)
                    && instrumentation.equals(that.instrumentation)
                    && methodMatcher.equals(that.methodMatcher);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + methodMatcher.hashCode();
            result = 31 * result + instrumentation.hashCode();
            result = 31 * result + attributeAppenderFactory.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodAnnotationTarget{" +
                    "base=" + super.toString() +
                    ", methodMatcher=" + methodMatcher +
                    ", instrumentation=" + instrumentation +
                    ", attributeAppenderFactory=" + attributeAppenderFactory +
                    '}';
        }
    }

    public class MatchedMethodInterception {

        protected final MethodMatcher methodMatcher;

        public MatchedMethodInterception(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        public MethodAnnotationTarget intercept(Instrumentation instrumentation) {
            return new MethodAnnotationTarget(classFormatVersion,
                    namingStrategy,
                    interfaceTypes,
                    ignoredMethods,
                    bridgeMethodResolverFactory,
                    classVisitorWrapperChain,
                    methodRegistry,
                    modifiers,
                    typeAttributeAppender,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    methodMatcher,
                    instrumentation,
                    MethodAttributeAppender.NoOp.INSTANCE);
        }

        public MethodAnnotationTarget withoutCode() {
            return intercept(Instrumentation.ForAbstractMethod.INSTANCE);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && getByteBuddy().equals(((MatchedMethodInterception) o).getByteBuddy())
                    && methodMatcher.equals(((MatchedMethodInterception) o).methodMatcher);
        }

        @Override
        public int hashCode() {
            return 31 * methodMatcher.hashCode() + getByteBuddy().hashCode();
        }

        @Override
        public String toString() {
            return "MatchedMethodInterception{" +
                    "methodMatcher=" + methodMatcher +
                    "byteBuddy=" + ByteBuddy.this.toString() +
                    '}';
        }

        private ByteBuddy getByteBuddy() {
            return ByteBuddy.this;
        }
    }

    protected final ClassFormatVersion classFormatVersion;
    protected final NamingStrategy namingStrategy;
    protected final List<TypeDescription> interfaceTypes;
    protected final MethodMatcher ignoredMethods;
    protected final BridgeMethodResolver.Factory bridgeMethodResolverFactory;
    protected final ClassVisitorWrapper.Chain classVisitorWrapperChain;
    protected final MethodRegistry methodRegistry;
    protected final Definable<Integer> modifiers;
    protected final Definable<TypeAttributeAppender> typeAttributeAppender;
    protected final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;
    protected final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;


    public ByteBuddy() {
        this(ClassFormatVersion.forCurrentJavaVersion());
    }

    public ByteBuddy(ClassFormatVersion classFormatVersion) {
        this(classFormatVersion,
                new NamingStrategy.SuffixingRandom(BYTE_BUDDY_DEFAULT_PREFIX),
                new TypeList.Empty(),
                isDefaultFinalizer().or(isSynthetic()),
                BridgeMethodResolver.Simple.Factory.FAIL_ON_REQUEST,
                new ClassVisitorWrapper.Chain(),
                new MethodRegistry.Default(),
                new Definable.Undefined<Integer>(),
                new Definable.Undefined<TypeAttributeAppender>(),
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE);
    }

    protected ByteBuddy(ClassFormatVersion classFormatVersion,
                        NamingStrategy namingStrategy,
                        List<TypeDescription> interfaceTypes,
                        MethodMatcher ignoredMethods,
                        BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                        ClassVisitorWrapper.Chain classVisitorWrapperChain,
                        MethodRegistry methodRegistry,
                        Definable<Integer> modifiers,
                        Definable<TypeAttributeAppender> typeAttributeAppender,
                        FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                        MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory) {
        this.classFormatVersion = classFormatVersion;
        this.namingStrategy = namingStrategy;
        this.interfaceTypes = interfaceTypes;
        this.ignoredMethods = ignoredMethods;
        this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.methodRegistry = methodRegistry;
        this.modifiers = modifiers;
        this.typeAttributeAppender = typeAttributeAppender;
        this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
        this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
    }

    public ClassFormatVersion getClassFormatVersion() {
        return classFormatVersion;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public List<TypeDescription> getInterfaceTypes() {
        return interfaceTypes;
    }

    public MethodMatcher getIgnoredMethods() {
        return ignoredMethods;
    }

    public ClassVisitorWrapper.Chain getClassVisitorWrapperChain() {
        return classVisitorWrapperChain;
    }

    public FieldAttributeAppender.Factory getDefaultFieldAttributeAppenderFactory() {
        return defaultFieldAttributeAppenderFactory;
    }

    public MethodAttributeAppender.Factory getDefaultMethodAttributeAppenderFactory() {
        return defaultMethodAttributeAppenderFactory;
    }

    public <T> DynamicType.Builder<T> subclass(Class<T> superType) {
        return subclass(superType, ConstructorStrategy.Default.IMITATE_SUPER_TYPE);
    }

    public <T> DynamicType.Builder<T> subclass(Class<T> superType, ConstructorStrategy constructorStrategy) {
        return subclass(new TypeDescription.ForLoadedType(superType), constructorStrategy);
    }

    public <T> DynamicType.Builder<T> subclass(TypeDescription superType) {
        return subclass(superType, ConstructorStrategy.Default.IMITATE_SUPER_TYPE);
    }

    public <T> DynamicType.Builder<T> subclass(TypeDescription superType, ConstructorStrategy constructorStrategy) {
        TypeDescription actualSuperType = isImplementable(superType);
        List<TypeDescription> interfaceTypes = this.interfaceTypes;
        if (nonNull(superType).isInterface()) {
            actualSuperType = new TypeDescription.ForLoadedType(Object.class);
            interfaceTypes = join(superType, interfaceTypes);
        }
        return new SubclassDynamicTypeBuilder<T>(classFormatVersion,
                namingStrategy,
                actualSuperType,
                interfaceTypes,
                modifiers.resolve(superType.getModifiers()),
                typeAttributeAppender.resolve(TypeAttributeAppender.NoOp.INSTANCE),
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                new FieldRegistry.Default(),
                methodRegistry,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                nonNull(constructorStrategy));
    }

    public ByteBuddy withClassFormatVersion(ClassFormatVersion classFormatVersion) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy withNamingStrategy(NamingStrategy namingStrategy) {
        return new ByteBuddy(classFormatVersion,
                nonNull(namingStrategy),
                interfaceTypes,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy modifiers(ModifierContributor.ForType... modifierContributor) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                new Definable.Defined<Integer>(resolveModifierContributors(TYPE_MODIFIER_MASK, modifierContributor)),
                typeAttributeAppender,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy attribute(TypeAttributeAppender typeAttributeAppender) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                new Definable.Defined<TypeAttributeAppender>(typeAttributeAppender),
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy annotateType(Annotation... annotation) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                new Definable.Defined<TypeAttributeAppender>(new TypeAttributeAppender.ForAnnotation(annotation)),
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy implement(Class<?> type) {
        return implement(new TypeDescription.ForLoadedType(type));
    }

    public ByteBuddy implement(TypeDescription type) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                join(interfaceTypes, isInterface(type)),
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy ignoreMethods(MethodMatcher ignoredMethods) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                nonNull(ignoredMethods),
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy withClassVisitor(ClassVisitorWrapper classVisitorWrapper) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain.append(nonNull(classVisitorWrapper)),
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy withDefaultFieldAttributeAppender(FieldAttributeAppender.Factory attributeAppenderFactory) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                nonNull(attributeAppenderFactory),
                defaultMethodAttributeAppenderFactory);
    }

    public ByteBuddy withDefaultMethodAttributeAppender(MethodAttributeAppender.Factory attributeAppenderFactory) {
        return new ByteBuddy(classFormatVersion,
                namingStrategy,
                interfaceTypes,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                methodRegistry,
                modifiers,
                typeAttributeAppender,
                defaultFieldAttributeAppenderFactory,
                nonNull(attributeAppenderFactory));
    }

    public MatchedMethodInterception intercept(MethodMatcher methodMatcher) {
        return new MatchedMethodInterception(methodMatcher);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ByteBuddy byteBuddy = (ByteBuddy) other;
        return classFormatVersion.equals(byteBuddy.classFormatVersion)
                && classVisitorWrapperChain.equals(byteBuddy.classVisitorWrapperChain)
                && defaultFieldAttributeAppenderFactory.equals(byteBuddy.defaultFieldAttributeAppenderFactory)
                && defaultMethodAttributeAppenderFactory.equals(byteBuddy.defaultMethodAttributeAppenderFactory)
                && ignoredMethods.equals(byteBuddy.ignoredMethods)
                && bridgeMethodResolverFactory.equals(byteBuddy.bridgeMethodResolverFactory)
                && interfaceTypes.equals(byteBuddy.interfaceTypes)
                && methodRegistry.equals(byteBuddy.methodRegistry)
                && modifiers.equals(byteBuddy.modifiers)
                && namingStrategy.equals(byteBuddy.namingStrategy)
                && typeAttributeAppender.equals(byteBuddy.typeAttributeAppender);
    }

    @Override
    public int hashCode() {
        int result = classFormatVersion.hashCode();
        result = 31 * result + namingStrategy.hashCode();
        result = 31 * result + interfaceTypes.hashCode();
        result = 31 * result + ignoredMethods.hashCode();
        result = 31 * result + bridgeMethodResolverFactory.hashCode();
        result = 31 * result + classVisitorWrapperChain.hashCode();
        result = 31 * result + methodRegistry.hashCode();
        result = 31 * result + modifiers.hashCode();
        result = 31 * result + typeAttributeAppender.hashCode();
        result = 31 * result + defaultFieldAttributeAppenderFactory.hashCode();
        result = 31 * result + defaultMethodAttributeAppenderFactory.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ByteBuddy{" +
                "classFormatVersion=" + classFormatVersion +
                ", namingStrategy=" + namingStrategy +
                ", interfaceTypes=" + interfaceTypes +
                ", ignoredMethods=" + ignoredMethods +
                ", bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                ", classVisitorWrapperChain=" + classVisitorWrapperChain +
                ", methodRegistry=" + methodRegistry +
                ", modifiers=" + modifiers +
                ", typeAttributeAppender=" + typeAttributeAppender +
                ", defaultFieldAttributeAppenderFactory=" + defaultFieldAttributeAppenderFactory +
                ", defaultMethodAttributeAppenderFactory=" + defaultMethodAttributeAppenderFactory +
                '}';
    }
}

package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.matcher.JunctionMethodMatcher;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.lang.annotation.Annotation;
import java.util.*;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

/**
 * Creates a dynamic type on basis of loaded types where the dynamic type extends the loaded types.
 *
 * @param <T> The best known loaded type representing the built dynamic type.
 */
public class SubclassDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    private final ClassFileVersion classFileVersion;
    private final NamingStrategy namingStrategy;
    private final TypeDescription superType;
    private final List<TypeDescription> interfaceTypes;
    private final int modifiers;
    private final TypeAttributeAppender attributeAppender;
    private final MethodMatcher ignoredMethods;
    private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;
    private final ClassVisitorWrapper.Chain classVisitorWrapperChain;
    private final FieldRegistry fieldRegistry;
    private final MethodRegistry methodRegistry;
    private final MethodLookupEngine.Factory methodLookupEngineFactory;
    private final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;
    private final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;
    private final ConstructorStrategy constructorStrategy;

    /**
     * Creates a new immutable type builder for a subclassing a loaded class.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param superType                             The loaded super type the dynamic type should extend.
     * @param interfaceTypes                        A list of interfaces that should be implemented by the created dynamic type.
     * @param modifiers                             The modifiers to be represented by the dynamic type.
     * @param attributeAppender                     The attribute appender to apply onto the dynamic type that is created.
     * @param ignoredMethods                        A matcher for determining methods that are to be ignored for instrumentation.
     * @param bridgeMethodResolverFactory           A factory for creating a bridge method resolver.
     * @param classVisitorWrapperChain              A chain of ASM class visitors to apply to the writing process.
     * @param fieldRegistry                         The field registry to apply to the dynamic type creation.
     * @param methodRegistry                        The method registry to apply to the dynamic type creation.
     * @param methodLookupEngineFactory             The method lookup engine factory to apply to the dynamic type creation.
     * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
     *                                              no specific appender was specified for a given field.
     * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
     *                                              if no specific appender was specified for a given method.
     * @param constructorStrategy                   The strategy for creating constructors when defining this dynamic type.
     */
    public SubclassDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                      NamingStrategy namingStrategy,
                                      TypeDescription superType,
                                      List<? extends TypeDescription> interfaceTypes,
                                      int modifiers,
                                      TypeAttributeAppender attributeAppender,
                                      MethodMatcher ignoredMethods,
                                      BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                      ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                      FieldRegistry fieldRegistry,
                                      MethodRegistry methodRegistry,
                                      MethodLookupEngine.Factory methodLookupEngineFactory,
                                      FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                      MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                      ConstructorStrategy constructorStrategy) {
        super(Collections.<FieldToken>emptyList(), Collections.<MethodToken>emptyList());
        this.classFileVersion = classFileVersion;
        this.namingStrategy = namingStrategy;
        this.superType = superType;
        this.interfaceTypes = new ArrayList<TypeDescription>(interfaceTypes);
        this.modifiers = modifiers;
        this.attributeAppender = attributeAppender;
        this.ignoredMethods = ignoredMethods;
        this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.fieldRegistry = fieldRegistry;
        this.methodLookupEngineFactory = methodLookupEngineFactory;
        this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
        this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
        this.methodRegistry = methodRegistry;
        this.constructorStrategy = constructorStrategy;
    }

    /**
     * Creates a new immutable type builder for a subclassing a loaded class.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param superType                             The loaded super type the dynamic type should extend.
     * @param interfaceTypes                        A list of interfaces that should be implemented by the created dynamic type.
     * @param modifiers                             The modifiers to be represented by the dynamic type.
     * @param attributeAppender                     The attribute appender to apply onto the dynamic type that is created.
     * @param ignoredMethods                        A matcher for determining methods that are to be ignored for instrumentation.
     * @param bridgeMethodResolverFactory           A factory for creating a bridge method resolver.
     * @param classVisitorWrapperChain              A chain of ASM class visitors to apply to the writing process.
     * @param fieldRegistry                         The field registry to apply to the dynamic type creation.
     * @param methodRegistry                        The method registry to apply to the dynamic type creation.
     * @param methodLookupEngineFactory             The method lookup engine factory to apply to the dynamic type creation.
     * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
     *                                              no specific appender was specified for a given field.
     * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
     *                                              if no specific appender was specified for a given method.
     * @param fieldTokens                           A list of field representations that were added explicitly to this
     *                                              dynamic type.
     * @param methodTokens                          A list of method representations that were added explicitly to this
     *                                              dynamic type.
     */
    protected SubclassDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                         NamingStrategy namingStrategy,
                                         TypeDescription superType,
                                         List<TypeDescription> interfaceTypes,
                                         int modifiers,
                                         TypeAttributeAppender attributeAppender,
                                         MethodMatcher ignoredMethods,
                                         BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                         ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                         FieldRegistry fieldRegistry,
                                         MethodRegistry methodRegistry,
                                         MethodLookupEngine.Factory methodLookupEngineFactory,
                                         FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                         MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                         List<FieldToken> fieldTokens,
                                         List<MethodToken> methodTokens,
                                         ConstructorStrategy constructorStrategy) {
        super(fieldTokens, methodTokens);
        this.classFileVersion = classFileVersion;
        this.namingStrategy = namingStrategy;
        this.superType = superType;
        this.interfaceTypes = interfaceTypes;
        this.modifiers = modifiers;
        this.attributeAppender = attributeAppender;
        this.ignoredMethods = ignoredMethods;
        this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.fieldRegistry = fieldRegistry;
        this.methodRegistry = methodRegistry;
        this.methodLookupEngineFactory = methodLookupEngineFactory;
        this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
        this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
        this.constructorStrategy = constructorStrategy;
    }

    @Override
    public DynamicType.Builder<T> classFileVersion(ClassFileVersion classFileVersion) {
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                constructorStrategy);
    }

    @Override
    public OptionalMatchedMethodInterception<T> implement(TypeDescription interfaceType) {
        return new SubclassOptionalMatchedMethodInterception<T>(isInterface(interfaceType));
    }

    @Override
    public DynamicType.Builder<T> name(String name) {
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                new NamingStrategy.Fixed(isValidTypeName(name)),
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                constructorStrategy);
    }

    @Override
    public DynamicType.Builder<T> modifiers(ModifierContributor.ForType... modifier) {
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                resolveModifierContributors(TYPE_MODIFIER_MASK, modifier),
                attributeAppender,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                constructorStrategy);
    }

    @Override
    public DynamicType.Builder<T> ignoreMethods(MethodMatcher ignoredMethods) {
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                new JunctionMethodMatcher.Conjunction(this.ignoredMethods, nonNull(ignoredMethods)),
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                constructorStrategy);
    }

    @Override
    public DynamicType.Builder<T> attribute(TypeAttributeAppender attributeAppender) {
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                new TypeAttributeAppender.Compound(this.attributeAppender, nonNull(attributeAppender)),
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                constructorStrategy);
    }

    @Override
    public DynamicType.Builder<T> annotateType(Annotation... annotation) {
        return attribute(new TypeAttributeAppender.ForAnnotation(annotation));
    }

    @Override
    public DynamicType.Builder<T> classVisitor(ClassVisitorWrapper classVisitorWrapper) {
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain.append(nonNull(classVisitorWrapper)),
                fieldRegistry,
                methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                constructorStrategy);
    }

    @Override
    public DynamicType.Builder<T> methodLookupEngine(MethodLookupEngine.Factory methodLookupEngineFactory) {
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                constructorStrategy);
    }

    @Override
    public FieldAnnotationTarget<T> defineField(String name,
                                                TypeDescription fieldType,
                                                ModifierContributor.ForField... modifier) {
        FieldToken fieldToken = new FieldToken(isValidIdentifier(name),
                nonNull(fieldType),
                resolveModifierContributors(FIELD_MODIFIER_MASK, nonNull(modifier)));
        return new SubclassFieldAnnotationTarget<T>(fieldToken, defaultFieldAttributeAppenderFactory);
    }

    @Override
    public ExceptionDeclarableMethodInterception<T> defineMethod(String name,
                                                                 TypeDescription returnType,
                                                                 List<? extends TypeDescription> parameterTypes,
                                                                 ModifierContributor.ForMethod... modifier) {
        MethodToken methodToken = new MethodToken(isValidIdentifier(name),
                nonNull(returnType),
                nonNull(parameterTypes),
                Collections.<TypeDescription>emptyList(),
                resolveModifierContributors(METHOD_MODIFIER_MASK, nonNull(modifier)));
        return new SubclassExceptionDeclarableMethodInterception<T>(methodToken);
    }

    @Override
    public ExceptionDeclarableMethodInterception<T> defineConstructor(List<? extends TypeDescription> parameterTypes,
                                                                      ModifierContributor.ForMethod... modifier) {
        MethodToken methodToken = new MethodToken(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                new TypeDescription.ForLoadedType(void.class),
                nonNull(parameterTypes),
                Collections.<TypeDescription>emptyList(),
                resolveModifierContributors(METHOD_MODIFIER_MASK, nonNull(modifier)));
        return new SubclassExceptionDeclarableMethodInterception<T>(methodToken);
    }

    @Override
    public MatchedMethodInterception<T> method(MethodMatcher methodMatcher) {
        return invokable(isMethod().and(methodMatcher));
    }

    @Override
    public MatchedMethodInterception<T> constructor(MethodMatcher methodMatcher) {
        return invokable(isConstructor().and(methodMatcher));
    }

    @Override
    public MatchedMethodInterception<T> invokable(MethodMatcher methodMatcher) {
        return new SubclassMatchedMethodInterception<T>(new MethodRegistry.LatentMethodMatcher.Simple(methodMatcher), methodTokens);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = constructorStrategy
                .inject(methodRegistry, defaultMethodAttributeAppenderFactory)
                .compile(
                        applyConstructorStrategy(
                                constructorStrategy,
                                applyRecordedMembersTo(new SubclassInstrumentedType(classFileVersion,
                                        superType,
                                        interfaceTypes,
                                        modifiers,
                                        namingStrategy))),
                        methodLookupEngineFactory.make(classFileVersion),
                        new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory),
                        MethodRegistry.Compiled.Entry.Skip.INSTANCE
                );
        MethodLookupEngine.Finding finding = compiledMethodRegistry.getFinding();
        TypeExtensionDelegate typeExtensionDelegate = new TypeExtensionDelegate(finding.getTypeDescription(), classFileVersion);
        return new TypeWriter.Builder<T>(finding.getTypeDescription(), compiledMethodRegistry.getTypeInitializer(), typeExtensionDelegate, classFileVersion)
                .build(classVisitorWrapperChain)
                .attributeType(attributeAppender)
                .fields()
                .write(finding.getTypeDescription().getDeclaredFields(),
                        fieldRegistry.compile(finding.getTypeDescription(), TypeWriter.FieldPool.Entry.NoOp.INSTANCE))
                .methods()
                .write(finding.getInvokableMethods().filter(isOverridable().and(not(ignoredMethods)).or(isDeclaredBy(finding.getTypeDescription()))),
                        compiledMethodRegistry)
                .write(typeExtensionDelegate.getRegisteredAccessors(), typeExtensionDelegate)
                .make();
    }

    private InstrumentedType applyConstructorStrategy(ConstructorStrategy constructorStrategy, InstrumentedType instrumentedType) {
        for (MethodDescription methodDescription : constructorStrategy.extractConstructors(instrumentedType)) {
            instrumentedType = instrumentedType.withMethod(methodDescription.getInternalName(),
                    methodDescription.getReturnType(),
                    methodDescription.getParameterTypes(),
                    methodDescription.getExceptionTypes(),
                    methodDescription.getModifiers());
        }
        return instrumentedType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        SubclassDynamicTypeBuilder that = (SubclassDynamicTypeBuilder) other;
        return modifiers == that.modifiers
                && attributeAppender.equals(that.attributeAppender)
                && bridgeMethodResolverFactory.equals(that.bridgeMethodResolverFactory)
                && classFileVersion.equals(that.classFileVersion)
                && classVisitorWrapperChain.equals(that.classVisitorWrapperChain)
                && defaultFieldAttributeAppenderFactory.equals(that.defaultFieldAttributeAppenderFactory)
                && defaultMethodAttributeAppenderFactory.equals(that.defaultMethodAttributeAppenderFactory)
                && fieldRegistry.equals(that.fieldRegistry)
                && ignoredMethods.equals(that.ignoredMethods)
                && interfaceTypes.equals(that.interfaceTypes)
                && methodLookupEngineFactory.equals(that.methodLookupEngineFactory)
                && methodRegistry.equals(that.methodRegistry)
                && namingStrategy.equals(that.namingStrategy)
                && superType.equals(that.superType);
    }

    @Override
    public int hashCode() {
        int result = classFileVersion.hashCode();
        result = 31 * result + namingStrategy.hashCode();
        result = 31 * result + superType.hashCode();
        result = 31 * result + interfaceTypes.hashCode();
        result = 31 * result + modifiers;
        result = 31 * result + attributeAppender.hashCode();
        result = 31 * result + ignoredMethods.hashCode();
        result = 31 * result + bridgeMethodResolverFactory.hashCode();
        result = 31 * result + classVisitorWrapperChain.hashCode();
        result = 31 * result + fieldRegistry.hashCode();
        result = 31 * result + methodRegistry.hashCode();
        result = 31 * result + methodLookupEngineFactory.hashCode();
        result = 31 * result + defaultFieldAttributeAppenderFactory.hashCode();
        result = 31 * result + defaultMethodAttributeAppenderFactory.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SubclassDynamicTypeBuilder{" +
                "classFileVersion=" + classFileVersion +
                ", namingStrategy=" + namingStrategy +
                ", superType=" + superType +
                ", interfaceTypes=" + interfaceTypes +
                ", modifiers=" + modifiers +
                ", attributeAppender=" + attributeAppender +
                ", ignoredMethods=" + ignoredMethods +
                ", bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                ", classVisitorWrapperChain=" + classVisitorWrapperChain +
                ", fieldRegistry=" + fieldRegistry +
                ", methodRegistry=" + methodRegistry +
                ", methodLookupEngineFactory=" + methodLookupEngineFactory +
                ", defaultFieldAttributeAppenderFactory=" + defaultFieldAttributeAppenderFactory +
                ", defaultMethodAttributeAppenderFactory=" + defaultMethodAttributeAppenderFactory +
                '}';
    }

    private class SubclassFieldAnnotationTarget<S> extends AbstractDelegatingBuilder<S> implements FieldAnnotationTarget<S> {

        private final FieldToken fieldToken;
        private final FieldAttributeAppender.Factory attributeAppenderFactory;

        private SubclassFieldAnnotationTarget(FieldToken fieldToken, FieldAttributeAppender.Factory attributeAppenderFactory) {
            this.fieldToken = fieldToken;
            this.attributeAppenderFactory = attributeAppenderFactory;
        }

        @Override
        protected DynamicType.Builder<S> materialize() {
            return new SubclassDynamicTypeBuilder<S>(classFileVersion,
                    namingStrategy,
                    superType,
                    interfaceTypes,
                    modifiers,
                    attributeAppender,
                    ignoredMethods,
                    bridgeMethodResolverFactory,
                    classVisitorWrapperChain,
                    fieldRegistry.include(fieldToken, attributeAppenderFactory),
                    methodRegistry,
                    methodLookupEngineFactory,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    join(fieldTokens, fieldToken),
                    methodTokens,
                    constructorStrategy);
        }

        @Override
        public FieldAnnotationTarget<S> attribute(FieldAttributeAppender.Factory attributeAppenderFactory) {
            return new SubclassFieldAnnotationTarget<S>(fieldToken,
                    new FieldAttributeAppender.Factory.Compound(this.attributeAppenderFactory, attributeAppenderFactory));
        }

        @Override
        public FieldAnnotationTarget<S> annotateField(Annotation... annotation) {
            return attribute(new FieldAttributeAppender.ForAnnotation(annotation));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SubclassFieldAnnotationTarget that = (SubclassFieldAnnotationTarget) other;
            return attributeAppenderFactory.equals(that.attributeAppenderFactory)
                    && fieldToken.equals(that.fieldToken)
                    && SubclassDynamicTypeBuilder.this.equals(that.getSubclassDynamicTypeBuilder());
        }

        @Override
        public int hashCode() {
            int result = fieldToken.hashCode();
            result = 31 * result + attributeAppenderFactory.hashCode();
            result = 31 * result + SubclassDynamicTypeBuilder.this.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "SubclassFieldAnnotationTarget{" +
                    "base=" + SubclassDynamicTypeBuilder.this +
                    ", fieldToken=" + fieldToken +
                    ", attributeAppenderFactory=" + attributeAppenderFactory +
                    '}';
        }

        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }
    }

    private class SubclassMatchedMethodInterception<S> implements MatchedMethodInterception<S> {

        private final MethodRegistry.LatentMethodMatcher latentMethodMatcher;
        private final List<MethodToken> methodTokens;

        private SubclassMatchedMethodInterception(MethodRegistry.LatentMethodMatcher latentMethodMatcher,
                                                  List<MethodToken> methodTokens) {
            this.latentMethodMatcher = latentMethodMatcher;
            this.methodTokens = methodTokens;
        }

        @Override
        public MethodAnnotationTarget<S> intercept(Instrumentation instrumentation) {
            return new SubclassMethodAnnotationTarget<S>(methodTokens,
                    latentMethodMatcher,
                    instrumentation,
                    defaultMethodAttributeAppenderFactory);
        }

        @Override
        public MethodAnnotationTarget<S> withoutCode() {
            return intercept(Instrumentation.ForAbstractMethod.INSTANCE);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SubclassMatchedMethodInterception<?> that = (SubclassMatchedMethodInterception<?>) other;
            return latentMethodMatcher.equals(that.latentMethodMatcher)
                    && methodTokens.equals(that.methodTokens)
                    && SubclassDynamicTypeBuilder.this.equals(that.getSubclassDynamicTypeBuilder());
        }

        @Override
        public int hashCode() {
            int result = latentMethodMatcher.hashCode();
            result = 31 * result + methodTokens.hashCode();
            result = 31 * result + SubclassDynamicTypeBuilder.this.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "SubclassMatchedMethodInterception{" +
                    "base=" + SubclassDynamicTypeBuilder.this +
                    ", latentMethodMatcher=" + latentMethodMatcher +
                    ", methodTokens=" + methodTokens +
                    '}';
        }

        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }
    }

    private class SubclassExceptionDeclarableMethodInterception<S> implements ExceptionDeclarableMethodInterception<S> {

        private final MethodToken methodToken;

        private SubclassExceptionDeclarableMethodInterception(MethodToken methodToken) {
            this.methodToken = methodToken;
        }

        @Override
        public MatchedMethodInterception<S> throwing(Class<?>... type) {
            return throwing(new TypeList.ForLoadedType(nonNull(type)).toArray(new TypeDescription[type.length]));
        }

        @Override
        public MatchedMethodInterception<S> throwing(TypeDescription... type) {
            return materialize(new MethodToken(methodToken.getInternalName(),
                    methodToken.getReturnType(),
                    methodToken.getParameterTypes(),
                    uniqueTypes(isThrowable(Arrays.asList(nonNull(type)))),
                    methodToken.getModifiers()));
        }

        @Override
        public MethodAnnotationTarget<S> intercept(Instrumentation instrumentation) {
            return materialize(methodToken).intercept(instrumentation);
        }

        @Override
        public MethodAnnotationTarget<S> withoutCode() {
            return materialize(methodToken).withoutCode();
        }

        private SubclassMatchedMethodInterception<S> materialize(MethodToken methodToken) {
            return new SubclassMatchedMethodInterception<S>(methodToken, join(methodTokens, methodToken));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodToken.equals(((SubclassExceptionDeclarableMethodInterception<?>) other).methodToken)
                    && SubclassDynamicTypeBuilder.this.equals(((SubclassExceptionDeclarableMethodInterception<?>) other).getSubclassDynamicTypeBuilder());
        }

        @Override
        public int hashCode() {
            return 31 * SubclassDynamicTypeBuilder.this.hashCode() + methodToken.hashCode();
        }

        @Override
        public String toString() {
            return "SubclassExceptionDeclarableMethodInterception{" +
                    "base=" + SubclassDynamicTypeBuilder.this +
                    "methodToken=" + methodToken +
                    '}';
        }

        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }
    }

    private class SubclassMethodAnnotationTarget<S> extends AbstractDelegatingBuilder<S> implements MethodAnnotationTarget<S> {

        private final List<MethodToken> methodTokens;
        private final MethodRegistry.LatentMethodMatcher latentMethodMatcher;
        private final Instrumentation instrumentation;
        private final MethodAttributeAppender.Factory attributeAppenderFactory;

        private SubclassMethodAnnotationTarget(List<MethodToken> methodTokens,
                                               MethodRegistry.LatentMethodMatcher latentMethodMatcher,
                                               Instrumentation instrumentation,
                                               MethodAttributeAppender.Factory attributeAppenderFactory) {
            this.latentMethodMatcher = latentMethodMatcher;
            this.methodTokens = methodTokens;
            this.instrumentation = instrumentation;
            this.attributeAppenderFactory = attributeAppenderFactory;
        }

        @Override
        protected DynamicType.Builder<S> materialize() {
            return new SubclassDynamicTypeBuilder<S>(classFileVersion,
                    namingStrategy,
                    superType,
                    interfaceTypes,
                    modifiers,
                    attributeAppender,
                    ignoredMethods,
                    bridgeMethodResolverFactory,
                    classVisitorWrapperChain,
                    fieldRegistry,
                    methodRegistry.prepend(latentMethodMatcher, instrumentation, attributeAppenderFactory),
                    methodLookupEngineFactory,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    fieldTokens,
                    methodTokens,
                    constructorStrategy);
        }

        @Override
        public MethodAnnotationTarget<S> attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new SubclassMethodAnnotationTarget<S>(
                    methodTokens,
                    latentMethodMatcher,
                    instrumentation,
                    new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory, attributeAppenderFactory));
        }

        @Override
        public MethodAnnotationTarget<S> annotateMethod(Annotation... annotation) {
            return attribute(new MethodAttributeAppender.ForAnnotation(annotation));
        }

        @Override
        public MethodAnnotationTarget<S> annotateParameter(int parameterIndex, Annotation... annotation) {
            return attribute(new MethodAttributeAppender.ForAnnotation(parameterIndex, annotation));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SubclassMethodAnnotationTarget that = (SubclassMethodAnnotationTarget<?>) other;
            return attributeAppenderFactory.equals(that.attributeAppenderFactory)
                    && instrumentation.equals(that.instrumentation)
                    && latentMethodMatcher.equals(that.latentMethodMatcher)
                    && methodTokens.equals(that.methodTokens)
                    && SubclassDynamicTypeBuilder.this.equals(that.getSubclassDynamicTypeBuilder());
        }

        @Override
        public int hashCode() {
            int result = methodTokens.hashCode();
            result = 31 * result + latentMethodMatcher.hashCode();
            result = 31 * result + instrumentation.hashCode();
            result = 31 * result + attributeAppenderFactory.hashCode();
            result = 31 * result + SubclassDynamicTypeBuilder.this.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "SubclassMethodAnnotationTarget{" +
                    "base=" + SubclassDynamicTypeBuilder.this +
                    ", methodTokens=" + methodTokens +
                    ", latentMethodMatcher=" + latentMethodMatcher +
                    ", instrumentation=" + instrumentation +
                    ", attributeAppenderFactory=" + attributeAppenderFactory +
                    '}';
        }

        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }
    }

    private class SubclassOptionalMatchedMethodInterception<S> extends AbstractDelegatingBuilder<S> implements OptionalMatchedMethodInterception<S> {

        private TypeDescription interfaceType;

        private SubclassOptionalMatchedMethodInterception(TypeDescription interfaceType) {
            this.interfaceType = interfaceType;
        }

        @Override
        public MethodAnnotationTarget<S> intercept(Instrumentation instrumentation) {
            return materialize().method(isDeclaredBy(interfaceType)).intercept(instrumentation);
        }

        @Override
        public MethodAnnotationTarget<S> withoutCode() {
            return materialize().method(isDeclaredBy(interfaceType)).withoutCode();
        }

        @Override
        protected DynamicType.Builder<S> materialize() {
            return new SubclassDynamicTypeBuilder<S>(classFileVersion,
                    namingStrategy,
                    superType,
                    join(interfaceTypes, isInterface(interfaceType)),
                    modifiers,
                    attributeAppender,
                    ignoredMethods,
                    bridgeMethodResolverFactory,
                    classVisitorWrapperChain,
                    fieldRegistry,
                    methodRegistry,
                    methodLookupEngineFactory,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    fieldTokens,
                    methodTokens,
                    constructorStrategy);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SubclassOptionalMatchedMethodInterception<?> that = (SubclassOptionalMatchedMethodInterception<?>) other;
            return interfaceType.equals(that.interfaceType)
                    && SubclassDynamicTypeBuilder.this.equals(that.getSubclassDynamicTypeBuilder());
        }

        @Override
        public int hashCode() {
            return 31 * SubclassDynamicTypeBuilder.this.hashCode() + interfaceType.hashCode();
        }

        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }

        @Override
        public String toString() {
            return "SubclassOptionalMatchedMethodInterception{" +
                    "base=" + SubclassDynamicTypeBuilder.this +
                    "interfaceType=" + interfaceType +
                    '}';
        }
    }
}

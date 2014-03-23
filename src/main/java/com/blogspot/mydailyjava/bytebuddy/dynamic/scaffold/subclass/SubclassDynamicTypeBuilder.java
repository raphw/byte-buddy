package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.NamingStrategy;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.JunctionMethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static com.blogspot.mydailyjava.bytebuddy.utility.ByteBuddyCommons.*;

/**
 * Creates a dynamic type on basis of loaded types where the dynamic type extends the loaded types.
 *
 * @param <T> The best known loaded type representing the built dynamic type.
 */
public class SubclassDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    private class SubclassFieldAnnotationTarget<S> extends AbstractDelegatingBuilder<S> implements FieldAnnotationTarget<S> {

        private final FieldToken fieldToken;
        private final FieldAttributeAppender.Factory attributeAppenderFactory;

        private SubclassFieldAnnotationTarget(FieldToken fieldToken, FieldAttributeAppender.Factory attributeAppenderFactory) {
            this.fieldToken = fieldToken;
            this.attributeAppenderFactory = attributeAppenderFactory;
        }

        @Override
        protected DynamicType.Builder<S> materialize() {
            return new SubclassDynamicTypeBuilder<S>(classFormatVersion,
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
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    join(fieldTokens, fieldToken),
                    methodTokens);
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

        private SubclassMatchedMethodInterception(MethodRegistry.LatentMethodMatcher latentMethodMatcher, List<MethodToken> methodTokens) {
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
            return new SubclassDynamicTypeBuilder<S>(classFormatVersion,
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
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    fieldTokens,
                    methodTokens);
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
            return new SubclassDynamicTypeBuilder<S>(classFormatVersion,
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
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    fieldTokens,
                    methodTokens);
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

    private static class MethodTokenListForConstructors extends AbstractList<MethodToken> {

        private final List<? extends MethodDescription> constructor;

        public MethodTokenListForConstructors(List<? extends MethodDescription> constructor) {
            this.constructor = constructor;
        }

        @Override
        public MethodToken get(int index) {
            return new MethodToken(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    new TypeDescription.ForLoadedType(void.class),
                    constructor.get(index).getParameterTypes(),
                    constructor.get(index).getModifiers());
        }

        @Override
        public int size() {
            return constructor.size();
        }

        @Override
        public String toString() {
            return "MethodTokenListForConstructors{constructor=" + constructor + '}';
        }
    }

    private final ClassFormatVersion classFormatVersion;
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
    private final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;
    private final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;

    /**
     * Creates a new immutable type builder for a subclassing a loaded class.
     *
     * @param classFormatVersion                    The class format version for the created dynamic type.
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
     * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
     *                                              no specific appender was specified for a given field.
     * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
     *                                              if no specific appender was specified for a given method.
     * @param constructorStrategy                   The strategy for creating constructors when defining this dynamic type.
     */
    public SubclassDynamicTypeBuilder(ClassFormatVersion classFormatVersion,
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
                                      FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                      MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                      ConstructorStrategy constructorStrategy) {
        super(Collections.<FieldToken>emptyList(),
                new MethodTokenListForConstructors(constructorStrategy.extractConstructors(superType)));
        this.classFormatVersion = classFormatVersion;
        this.namingStrategy = namingStrategy;
        this.superType = superType;
        this.interfaceTypes = new ArrayList<TypeDescription>(interfaceTypes);
        this.modifiers = modifiers;
        this.attributeAppender = attributeAppender;
        this.ignoredMethods = ignoredMethods;
        this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.fieldRegistry = fieldRegistry;
        this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
        this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
        this.methodRegistry = constructorStrategy.inject(methodRegistry, defaultMethodAttributeAppenderFactory);
    }

    /**
     * Creates a new immutable type builder for a subclassing a loaded class.
     *
     * @param classFormatVersion                    The class format version for the created dynamic type.
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
     * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
     *                                              no specific appender was specified for a given field.
     * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
     *                                              if no specific appender was specified for a given method.
     * @param fieldTokens                           A list of field representations that were added explicitly to this
     *                                              dynamic type.
     * @param methodTokens                          A list of method representations that were added explicitly to this
     *                                              dynamic type.
     */
    protected SubclassDynamicTypeBuilder(ClassFormatVersion classFormatVersion,
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
                                         FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                         MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                         List<FieldToken> fieldTokens,
                                         List<MethodToken> methodTokens) {
        super(fieldTokens, methodTokens);
        this.classFormatVersion = classFormatVersion;
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
        this.defaultFieldAttributeAppenderFactory = defaultFieldAttributeAppenderFactory;
        this.defaultMethodAttributeAppenderFactory = defaultMethodAttributeAppenderFactory;
    }

    @Override
    public DynamicType.Builder<T> classFormatVersion(ClassFormatVersion classFormatVersion) {
        return new SubclassDynamicTypeBuilder<T>(classFormatVersion,
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
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens);
    }

    @Override
    public OptionalMatchedMethodInterception<T> implement(TypeDescription interfaceType) {
        return new SubclassOptionalMatchedMethodInterception<T>(isInterface(interfaceType));
    }

    @Override
    public DynamicType.Builder<T> name(String name) {
        return new SubclassDynamicTypeBuilder<T>(classFormatVersion,
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
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> modifiers(ModifierContributor.ForType... modifier) {
        return new SubclassDynamicTypeBuilder<T>(classFormatVersion,
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
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> ignoreMethods(MethodMatcher ignoredMethods) {
        return new SubclassDynamicTypeBuilder<T>(classFormatVersion,
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
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> attribute(TypeAttributeAppender attributeAppender) {
        return new SubclassDynamicTypeBuilder<T>(classFormatVersion,
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
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> annotateType(Annotation... annotation) {
        return attribute(new TypeAttributeAppender.ForAnnotation(annotation));
    }

    @Override
    public DynamicType.Builder<T> classVisitor(ClassVisitorWrapper classVisitorWrapper) {
        return new SubclassDynamicTypeBuilder<T>(classFormatVersion,
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
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens);
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
    public MatchedMethodInterception<T> defineMethod(String name,
                                                     TypeDescription returnType,
                                                     List<? extends TypeDescription> parameterTypes,
                                                     ModifierContributor.ForMethod... modifier) {
        MethodToken methodToken = new MethodToken(isValidIdentifier(name),
                nonNull(returnType),
                nonNull(parameterTypes),
                resolveModifierContributors(METHOD_MODIFIER_MASK, nonNull(modifier)));
        return new SubclassMatchedMethodInterception<T>(methodToken, join(methodTokens, methodToken));
    }

    @Override
    public MatchedMethodInterception<T> defineConstructorDescriptive(List<? extends TypeDescription> parameterTypes,
                                                                     ModifierContributor.ForMethod... modifier) {
        MethodToken methodToken = new MethodToken(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                new TypeDescription.ForLoadedType(void.class),
                nonNull(parameterTypes),
                resolveModifierContributors(METHOD_MODIFIER_MASK, nonNull(modifier)));
        return new SubclassMatchedMethodInterception<T>(methodToken, join(methodTokens, methodToken));
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
        InstrumentedType instrumentedType = applyRecordedMembersTo(new SubclassInstumentedType(classFormatVersion,
                superType,
                interfaceTypes,
                modifiers,
                namingStrategy));
        SubclassInstrumentationContextDelegate contextDelegate = new SubclassInstrumentationContextDelegate(instrumentedType, bridgeMethodResolverFactory);
        Instrumentation.Context instrumentationContext = new Instrumentation.Context.Default(classFormatVersion, contextDelegate, contextDelegate);
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.compile(instrumentedType, MethodRegistry.Compiled.Entry.Skip.INSTANCE);
        instrumentedType = compiledMethodRegistry.getInstrumentedType();
        return new TypeWriter.Builder<T>(instrumentedType, instrumentationContext, classFormatVersion)
                .build(classVisitorWrapperChain)
                .attributeType(attributeAppender)
                .fields()
                .write(instrumentedType.getDeclaredFields(),
                        fieldRegistry.compile(instrumentedType, TypeWriter.FieldPool.Entry.NoOp.INSTANCE))
                .methods()
                .write(instrumentedType.getReachableMethods()
                                .filter(isOverridable().and(not(ignoredMethods)).or(isDeclaredBy(instrumentedType))),
                        compiledMethodRegistry
                )
                .write(contextDelegate.getProxiedMethods(), contextDelegate)
                .make();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        SubclassDynamicTypeBuilder that = (SubclassDynamicTypeBuilder) other;
        return modifiers == that.modifiers && attributeAppender.equals(that.attributeAppender)
                && bridgeMethodResolverFactory.equals(that.bridgeMethodResolverFactory)
                && classFormatVersion.equals(that.classFormatVersion)
                && classVisitorWrapperChain.equals(that.classVisitorWrapperChain)
                && defaultFieldAttributeAppenderFactory.equals(that.defaultFieldAttributeAppenderFactory)
                && defaultMethodAttributeAppenderFactory.equals(that.defaultMethodAttributeAppenderFactory)
                && fieldRegistry.equals(that.fieldRegistry) && ignoredMethods.equals(that.ignoredMethods)
                && interfaceTypes.equals(that.interfaceTypes) && methodRegistry.equals(that.methodRegistry)
                && namingStrategy.equals(that.namingStrategy)
                && superType.equals(that.superType);
    }

    @Override
    public int hashCode() {
        int result = classFormatVersion.hashCode();
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
        result = 31 * result + defaultFieldAttributeAppenderFactory.hashCode();
        result = 31 * result + defaultMethodAttributeAppenderFactory.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SubclassDynamicTypeBuilder{" +
                "classFormatVersion=" + classFormatVersion +
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
                ", defaultFieldAttributeAppenderFactory=" + defaultFieldAttributeAppenderFactory +
                ", defaultMethodAttributeAppenderFactory=" + defaultMethodAttributeAppenderFactory +
                '}';
    }
}

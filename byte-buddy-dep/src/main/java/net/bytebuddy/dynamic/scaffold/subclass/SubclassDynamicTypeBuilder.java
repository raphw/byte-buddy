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
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

/**
 * Creates a dynamic type on basis of loaded types where the dynamic type extends the loaded types.
 *
 * @param <T> The best known loaded type representing the built dynamic type.
 */
public class SubclassDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    /**
     * The class file version specified for this builder.
     */
    private final ClassFileVersion classFileVersion;

    /**
     * The naming strategy specified for this builder.
     */
    private final NamingStrategy namingStrategy;

    /**
     * The type description specified for this builder.
     */
    private final TypeDescription superType;

    /**
     * The interface types to implement as specified for this builder.
     */
    private final List<TypeDescription> interfaceTypes;

    /**
     * The modifiers specified for this builder.
     */
    private final int modifiers;

    /**
     * The type attribute appender specified for this builder.
     */
    private final TypeAttributeAppender attributeAppender;

    /**
     * The method matcher for ignored method specified for this builder.
     */
    private final MethodMatcher ignoredMethods;

    /**
     * The bridge method resolver factory specified for this builder.
     */
    private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

    /**
     * The class visitor wrapper chain that is applied on created types by this builder.
     */
    private final ClassVisitorWrapper.Chain classVisitorWrapperChain;

    /**
     * The field registry of this builder.
     */
    private final FieldRegistry fieldRegistry;

    /**
     * The method registry of this builder.
     */
    private final MethodRegistry methodRegistry;

    /**
     * The method lookup engine factory to be used by this builder.
     */
    private final MethodLookupEngine.Factory methodLookupEngineFactory;

    /**
     * The default field attribute appender factory that is automatically added to any field that is
     * registered on this builder.
     */
    private final FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory;

    /**
     * The default method attribute appender factory that is automatically added to any field method is
     * registered on this builder.
     */
    private final MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory;

    /**
     * The constructor strategy that is applied by this builder.
     */
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
     * @param constructorStrategy                   The strategy for creating constructors during the final definition
     *                                              phase of this dynamic type.
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
    public OptionalMatchedMethodInterception<T> implement(TypeDescription... interfaceType) {
        return new SubclassOptionalMatchedMethodInterception<T>(interfaceType);
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
    public DynamicType.Builder<T> bridgeMethodResolverFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                nonNull(bridgeMethodResolverFactory),
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
    public FieldValueTarget<T> defineField(String name,
                                           TypeDescription fieldType,
                                           ModifierContributor.ForField... modifier) {
        FieldToken fieldToken = new FieldToken(isValidIdentifier(name),
                nonNull(fieldType),
                resolveModifierContributors(FIELD_MODIFIER_MASK, nonNull(modifier)));
        return new SubclassFieldValueTarget<T>(fieldToken, defaultFieldAttributeAppenderFactory);
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

    /**
     * Applies this builder's constructor strategy to the given instrumented type.
     *
     * @param instrumentedType The instrumented type to apply the constructor onto.
     * @return The instrumented type with the constructor strategy applied onto.
     */
    private InstrumentedType applyConstructorStrategy(InstrumentedType instrumentedType) {
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
                && superType.equals(that.superType)
                && constructorStrategy.equals(that.constructorStrategy);
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
        result = 31 * result + constructorStrategy.hashCode();
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
                ", constructorStrategy=" + constructorStrategy +
                '}';
    }

    /**
     * A {@link net.bytebuddy.dynamic.DynamicType.Builder} for which a field was recently defined such that attributes
     * can be added to this recently defined field.
     *
     * @param <S> The best known loaded type representing the built dynamic type.
     */
    private class SubclassFieldValueTarget<S> extends AbstractDelegatingBuilder<S> implements FieldValueTarget<S> {

        /**
         * Representations of {@code boolean} values as JVM integers.
         */
        private static final int NUMERIC_BOOLEAN_TRUE = 1, NUMERIC_BOOLEAN_FALSE = 0;

        /**
         * A token representing the field that was recently defined.
         */
        private final FieldToken fieldToken;

        /**
         * The attribute appender factory that was defined for this field token.
         */
        private final FieldAttributeAppender.Factory attributeAppenderFactory;

        /**
         * The default value that is to be defined for the recently defined field or {@code null} if no such
         * value is to be defined. Default values must only be defined for {@code static} fields of primitive types
         * or of the {@link java.lang.String} type.
         */
        private final Object defaultValue;

        /**
         * Creates a new subclass field annotation target for a field without a default value.
         *
         * @param fieldToken               A token representing the field that was recently defined.
         * @param attributeAppenderFactory The attribute appender factory that was defined for this field token.
         */
        private SubclassFieldValueTarget(FieldToken fieldToken,
                                         FieldAttributeAppender.Factory attributeAppenderFactory) {
            this(fieldToken, attributeAppenderFactory, null);
        }

        /**
         * Creates a new subclass field annotation target.
         *
         * @param fieldToken               A token representing the field that was recently defined.
         * @param attributeAppenderFactory The attribute appender factory that was defined for this field token.
         * @param defaultValue             The default value to define for the recently defined field.
         */
        private SubclassFieldValueTarget(FieldToken fieldToken,
                                         FieldAttributeAppender.Factory attributeAppenderFactory,
                                         Object defaultValue) {
            this.fieldToken = fieldToken;
            this.attributeAppenderFactory = attributeAppenderFactory;
            this.defaultValue = defaultValue;
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
                    fieldRegistry.include(fieldToken, attributeAppenderFactory, defaultValue),
                    methodRegistry,
                    methodLookupEngineFactory,
                    defaultFieldAttributeAppenderFactory,
                    defaultMethodAttributeAppenderFactory,
                    join(fieldTokens, fieldToken),
                    methodTokens,
                    constructorStrategy);
        }

        @Override
        public FieldAnnotationTarget<S> value(boolean value) {
            return value(value ? NUMERIC_BOOLEAN_TRUE : NUMERIC_BOOLEAN_FALSE);
        }

        @Override
        public FieldAnnotationTarget<S> value(int value) {
            return makeFieldAnnotationTarget(isValid(value,
                    boolean.class,
                    byte.class,
                    short.class,
                    char.class,
                    int.class));
        }

        @Override
        public FieldAnnotationTarget<S> value(long value) {
            return makeFieldAnnotationTarget(isValid(value, long.class));
        }

        @Override
        public FieldAnnotationTarget<S> value(float value) {
            return makeFieldAnnotationTarget(isValid(value, float.class));
        }

        @Override
        public FieldAnnotationTarget<S> value(double value) {
            return makeFieldAnnotationTarget(isValid(value, double.class));
        }

        @Override
        public FieldAnnotationTarget<S> value(String value) {
            return makeFieldAnnotationTarget(isValid(value, String.class));
        }

        /**
         * Asserts the field's type to be one of the given legal types.
         *
         * @param defaultValue The default value to define for the recently defined field.
         * @param legalType    The types of which at least one should be considered to be legal for the field
         *                     that is represented by this instance.
         * @return The given default value.
         */
        private Object isValid(Object defaultValue, Class<?>... legalType) {
            for (Class<?> type : legalType) {
                if (fieldToken.getFieldType().represents(type)) {
                    return defaultValue;
                }
            }
            throw new IllegalStateException("The default");
        }

        /**
         * Creates a field annotation target for the given default value.
         *
         * @param defaultValue The default value to define for the recently defined field.
         * @return The resulting field annotation target.
         */
        private FieldAnnotationTarget<S> makeFieldAnnotationTarget(Object defaultValue) {
            if ((fieldToken.getModifiers() & Opcodes.ACC_STATIC) == 0) {
                throw new IllegalStateException("Default field values can only be set for static fields");
            }
            return new SubclassFieldValueTarget<S>(fieldToken, attributeAppenderFactory, defaultValue);
        }

        @Override
        public FieldAnnotationTarget<S> attribute(FieldAttributeAppender.Factory attributeAppenderFactory) {
            return new SubclassFieldValueTarget<S>(fieldToken,
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
            SubclassFieldValueTarget that = (SubclassFieldValueTarget<?>) other;
            return attributeAppenderFactory.equals(that.attributeAppenderFactory)
                    && !(defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null)
                    && fieldToken.equals(that.fieldToken)
                    && SubclassDynamicTypeBuilder.this.equals(that.getSubclassDynamicTypeBuilder());
        }

        @Override
        public int hashCode() {
            int result = fieldToken.hashCode();
            result = 31 * result + attributeAppenderFactory.hashCode();
            result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
            result = 31 * result + SubclassDynamicTypeBuilder.this.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "SubclassFieldAnnotationTarget{" +
                    "base=" + SubclassDynamicTypeBuilder.this +
                    ", fieldToken=" + fieldToken +
                    ", attributeAppenderFactory=" + attributeAppenderFactory +
                    ", defaultValue=" + defaultValue +
                    '}';
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }
    }

    /**
     * A {@link net.bytebuddy.dynamic.DynamicType.Builder.MatchedMethodInterception} for which a method was recently
     * identified or defined such that an {@link net.bytebuddy.instrumentation.Instrumentation} for these methods can
     * now be defined.
     *
     * @param <S> The best known loaded type representing the built dynamic type.
     */
    private class SubclassMatchedMethodInterception<S> implements MatchedMethodInterception<S> {

        /**
         * A latent method matcher that identifies the methods that are supposed to be intercepted by the later
         * defined instrumentation.
         */
        private final MethodRegistry.LatentMethodMatcher latentMethodMatcher;

        /**
         * A list of all method tokens that were previously defined.
         */
        private final List<MethodToken> methodTokens;

        /**
         * Creates a new subclass matched method interception.
         *
         * @param latentMethodMatcher A latent method matcher that identifies the methods that are supposed to be
         *                            intercepted by the later defined instrumentation.
         * @param methodTokens        A list of all method tokens that were previously defined.
         */
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

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }
    }

    /**
     * A {@link net.bytebuddy.dynamic.DynamicType.Builder.ExceptionDeclarableMethodInterception} which allows the
     * definition of exceptions for a recently defined method.
     *
     * @param <S> The best known loaded type representing the built dynamic type.
     */
    private class SubclassExceptionDeclarableMethodInterception<S> implements ExceptionDeclarableMethodInterception<S> {

        /**
         * The method token for which exceptions can be defined additionally.
         */
        private final MethodToken methodToken;

        /**
         * Creates a new subclass exception declarable method interception.
         *
         * @param methodToken The method token to define on the currently constructed method.
         */
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

        /**
         * Materializes the given method definition and returns an instance for defining an implementation.
         *
         * @param methodToken The method token to define on the currently constructed type.
         * @return A subclass matched method interception that represents the materialized method.
         */
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

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }
    }

    /**
     * A {@link net.bytebuddy.dynamic.DynamicType.Builder.MethodAnnotationTarget} which allows the definition of
     * annotations for a recently identified method.
     *
     * @param <S> The best known loaded type representing the built dynamic type.
     */
    private class SubclassMethodAnnotationTarget<S> extends AbstractDelegatingBuilder<S> implements MethodAnnotationTarget<S> {

        /**
         * A latent method matcher that identifies the methods that are supposed to be intercepted by the later
         * defined instrumentation.
         */
        private final MethodRegistry.LatentMethodMatcher latentMethodMatcher;

        /**
         * A list of all method tokens that were previously defined.
         */
        private final List<MethodToken> methodTokens;

        /**
         * The instrumentation that is to be applied to the matched methods.
         */
        private final Instrumentation instrumentation;

        /**
         * The method attribute appender factory to be applied to the matched methods.
         */
        private final MethodAttributeAppender.Factory attributeAppenderFactory;

        /**
         * Creates a new subclass method annotation target.
         *
         * @param methodTokens             A latent method matcher that identifies the methods that are supposed to be
         *                                 intercepted by the later defined instrumentation.
         * @param latentMethodMatcher      A list of all method tokens that were previously defined.
         * @param instrumentation          The instrumentation that is to be applied to the matched methods.
         * @param attributeAppenderFactory The method attribute appender factory to be applied to the matched methods.
         */
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

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }
    }

    /**
     * Allows for the direct implementation of an interface after its implementation was specified.
     *
     * @param <S> The best known loaded type representing the built dynamic type.
     */
    private class SubclassOptionalMatchedMethodInterception<S> extends AbstractDelegatingBuilder<S> implements OptionalMatchedMethodInterception<S> {

        /**
         * A list of all interfaces to implement.
         */
        private TypeDescription[] interfaceType;

        /**
         * Creates a new subclass optional matched method interception.
         *
         * @param interfaceType An array of all interfaces to implement.
         */
        private SubclassOptionalMatchedMethodInterception(TypeDescription[] interfaceType) {
            this.interfaceType = interfaceType;
        }

        @Override
        public MethodAnnotationTarget<S> intercept(Instrumentation instrumentation) {
            return materialize().method(isDeclaredByAny(interfaceType)).intercept(instrumentation);
        }

        @Override
        public MethodAnnotationTarget<S> withoutCode() {
            return materialize().method(isDeclaredByAny(interfaceType)).withoutCode();
        }

        @Override
        protected DynamicType.Builder<S> materialize() {
            return new SubclassDynamicTypeBuilder<S>(classFileVersion,
                    namingStrategy,
                    superType,
                    join(interfaceTypes, isInterface(Arrays.asList(interfaceType))),
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
            return Arrays.equals(interfaceType, that.interfaceType)
                    && SubclassDynamicTypeBuilder.this.equals(that.getSubclassDynamicTypeBuilder());
        }

        @Override
        public int hashCode() {
            return 31 * SubclassDynamicTypeBuilder.this.hashCode() + Arrays.hashCode(interfaceType);
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private SubclassDynamicTypeBuilder<?> getSubclassDynamicTypeBuilder() {
            return SubclassDynamicTypeBuilder.this;
        }

        @Override
        public String toString() {
            return "SubclassOptionalMatchedMethodInterception{" +
                    "base=" + SubclassDynamicTypeBuilder.this +
                    "interfaceType=" + Arrays.toString(interfaceType) +
                    '}';
        }
    }
}

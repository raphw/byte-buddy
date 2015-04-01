package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.instrumentation.type.auxiliary.TrivialType;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;


/**
 * A dynamic type builder which enhances a given type without creating a subclass.
 *
 * @param <T> The most specific type that is known to be represented by the enhanced type.
 */
public class InlineDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    /**
     * A locator for finding a class file.
     */
    private final ClassFileLocator classFileLocator;

    /**
     * The target handler to be used by this type builder.
     */
    private final TargetHandler targetHandler;

    /**
     * Creates a new immutable type builder for enhancing a given class.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param levelType                             A description of the enhanced type.
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
     * @param classFileLocator                      A locator for finding a class file.
     * @param targetHandler                         The target handler to be used by this type builder.
     */
    public InlineDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                    NamingStrategy namingStrategy,
                                    TypeDescription levelType,
                                    List<? extends TypeDescription> interfaceTypes,
                                    int modifiers,
                                    TypeAttributeAppender attributeAppender,
                                    ElementMatcher<? super MethodDescription> ignoredMethods,
                                    BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                    ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                    FieldRegistry fieldRegistry,
                                    MethodRegistry methodRegistry,
                                    MethodLookupEngine.Factory methodLookupEngineFactory,
                                    FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                    MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                    ClassFileLocator classFileLocator,
                                    TargetHandler targetHandler) {
        this(classFileVersion,
                namingStrategy,
                levelType,
                new ArrayList<TypeDescription>(interfaceTypes),
                modifiers,
                attributeAppender,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                fieldRegistry, methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                Collections.<FieldToken>emptyList(),
                Collections.<MethodToken>emptyList(),
                classFileLocator,
                targetHandler);
    }

    /**
     * Creates a new immutable type builder for enhancing a given class.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param levelType                             A description of the enhanced type.
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
     * @param classFileLocator                      A locator for finding a class file.
     * @param targetHandler                         The target handler to be used by this type builder.
     */
    protected InlineDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                       NamingStrategy namingStrategy,
                                       TypeDescription levelType,
                                       List<TypeDescription> interfaceTypes,
                                       int modifiers,
                                       TypeAttributeAppender attributeAppender,
                                       ElementMatcher<? super MethodDescription> ignoredMethods,
                                       BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                       ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                       FieldRegistry fieldRegistry,
                                       MethodRegistry methodRegistry,
                                       MethodLookupEngine.Factory methodLookupEngineFactory,
                                       FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                       MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                       List<FieldToken> fieldTokens,
                                       List<MethodToken> methodTokens,
                                       ClassFileLocator classFileLocator,
                                       TargetHandler targetHandler) {
        super(classFileVersion,
                namingStrategy,
                levelType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                bridgeMethodResolverFactory,
                classVisitorWrapperChain,
                fieldRegistry, methodRegistry,
                methodLookupEngineFactory,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens);
        this.classFileLocator = classFileLocator;
        this.targetHandler = targetHandler;
    }

    @Override
    protected DynamicType.Builder<T> materialize(ClassFileVersion classFileVersion,
                                                 NamingStrategy namingStrategy,
                                                 TypeDescription levelType,
                                                 List<TypeDescription> interfaceTypes,
                                                 int modifiers,
                                                 TypeAttributeAppender attributeAppender,
                                                 ElementMatcher<? super MethodDescription> ignoredMethods,
                                                 BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                                 ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                                 FieldRegistry fieldRegistry,
                                                 MethodRegistry methodRegistry,
                                                 MethodLookupEngine.Factory methodLookupEngineFactory,
                                                 FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                                 MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                                 List<FieldToken> fieldTokens,
                                                 List<MethodToken> methodTokens) {
        return new InlineDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                levelType,
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
                classFileLocator,
                targetHandler);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.prepare(
                applyRecordedMembersTo(new InlineInstrumentedType(classFileVersion,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        namingStrategy)), new LevelTypeDefinitionMatcher(targetType, ignoredMethods))
                .compile(targetHandler.makeInstrumentationTargetFactory(bridgeMethodResolverFactory),
                        methodLookupEngineFactory.make(classFileVersion.isSupportsDefaultMethods()));
        TargetHandler.Compiled compiledTargetHandler = targetHandler.compile(compiledMethodRegistry.getInstrumentedMethods(),
                compiledMethodRegistry.getInstrumentedType(),
                classFileVersion,
                new Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy.SuffixingRandom("placeholder"));
        return new TypeWriter.Default<T>(compiledMethodRegistry.getInstrumentedType(),
                compiledMethodRegistry.getLoadedTypeInitializer(),
                compiledMethodRegistry.getTypeInitializer(),
                compiledTargetHandler.getAuxiliaryTypes(),
                classFileVersion,
                new TypeWriter.Engine.ForRedefinition(compiledMethodRegistry.getInstrumentedType(),
                        targetType,
                        classFileVersion,
                        compiledMethodRegistry.getInstrumentedMethods(),
                        classVisitorWrapperChain,
                        attributeAppender,
                        fieldRegistry.prepare(compiledMethodRegistry.getInstrumentedType()).compile(TypeWriter.FieldPool.Entry.NoOp.INSTANCE),
                        compiledMethodRegistry,
                        classFileLocator,
                        compiledTargetHandler.getMethodRebaseResolver()))
                .make();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && classFileLocator.equals(((InlineDynamicTypeBuilder<?>) other).classFileLocator)
                && targetHandler.equals(((InlineDynamicTypeBuilder<?>) other).targetHandler);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * super.hashCode() + classFileLocator.hashCode()) + targetHandler.hashCode();
    }

    @Override
    public String toString() {
        return "InlineDynamicTypeBuilder{" +
                "classFileVersion=" + classFileVersion +
                ", namingStrategy=" + namingStrategy +
                ", levelType=" + targetType +
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
                ", classFileLocator=" + classFileLocator +
                ", targetHandler=" + targetHandler +
                '}';
    }

    protected static class LevelTypeDefinitionMatcher implements MethodRegistry.LatentMethodMatcher {

        private final TypeDescription targetType;

        private final ElementMatcher<? super MethodDescription> ignoredMethods;

        protected LevelTypeDefinitionMatcher(TypeDescription targetType, ElementMatcher<? super MethodDescription> ignoredMethods) {
            this.targetType = targetType;
            this.ignoredMethods = ignoredMethods;
        }

        @Override
        public ElementMatcher<? super MethodDescription> manifest(TypeDescription typeDescription) {
            return isDeclaredBy(typeDescription).and(not(anyOf(targetType.getDeclaredMethods())))
                    .or(isOverridable().or(isDeclaredBy(typeDescription)).and(not(ignoredMethods)));
        }
    }

    /**
     * An inline dynamic type builder's target handler is responsible to proving any information that is required
     * for defining the type.
     */
    public interface TargetHandler {

        Instrumentation.Target.Factory makeInstrumentationTargetFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory);

        Compiled compile(MethodList instrumentedMethods,
                         TypeDescription instrumentedType,
                         ClassFileVersion classFileVersion,
                         Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy);

        interface Compiled {

            List<DynamicType> getAuxiliaryTypes();

            MethodRebaseResolver getMethodRebaseResolver();

            enum NoOp implements Compiled {

                INSTANCE;

                @Override
                public List<DynamicType> getAuxiliaryTypes() {
                    return Collections.emptyList();
                }

                @Override
                public MethodRebaseResolver getMethodRebaseResolver() {
                    return MethodRebaseResolver.Forbidden.INSTANCE;
                }
            }

            class ForRebasement implements Compiled {

                protected static Compiled of(MethodList instrumentedMethods,
                                             MethodRebaseResolver.MethodNameTransformer methodNameTransformer,
                                             TypeDescription instrumentedType,
                                             ClassFileVersion classFileVersion,
                                             Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy) {
                    if (instrumentedMethods.filter(isConstructor()).isEmpty()) {
                        return new ForRebasement(Collections.<DynamicType>emptyList(),
                                MethodRebaseResolver.MethodsOnly.of(instrumentedMethods, methodNameTransformer));
                    } else {
                        DynamicType placeholderType = TrivialType.INSTANCE.make(auxiliaryTypeNamingStrategy.name(TrivialType.INSTANCE, instrumentedType),
                                classFileVersion,
                                AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE);
                        return new ForRebasement(Collections.singletonList(placeholderType),
                                MethodRebaseResolver.Enabled.of(instrumentedMethods, placeholderType.getTypeDescription(), methodNameTransformer));
                    }
                }

                private final List<DynamicType> dynamicTypes;

                private final MethodRebaseResolver methodNameTransformer;

                public ForRebasement(List<DynamicType> dynamicTypes, MethodRebaseResolver methodNameTransformer) {
                    this.dynamicTypes = dynamicTypes;
                    this.methodNameTransformer = methodNameTransformer;
                }

                @Override
                public List<DynamicType> getAuxiliaryTypes() {
                    return dynamicTypes;
                }

                @Override
                public MethodRebaseResolver getMethodRebaseResolver() {
                    return methodNameTransformer;
                }
            }
        }

        /**
         * Performs a subclass instrumentation which creates a redefinition of the given type by invoking the
         * actual super method when redefining a method.
         */
        enum ForRedefinitionInstrumentation implements TargetHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Instrumentation.Target.Factory makeInstrumentationTargetFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                return new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory, SubclassInstrumentationTarget.OriginTypeIdentifier.LEVEL_TYPE);
            }

            @Override
            public Compiled compile(MethodList instrumentedMethods,
                                    TypeDescription instrumentedType,
                                    ClassFileVersion classFileVersion,
                                    Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy) {
                return Compiled.NoOp.INSTANCE;
            }

            @Override
            public String toString() {
                return "InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation." + name();
            }
        }

        /**
         * Performs a rebase instrumentation which creates a redefinition of the given type by rebasing the original
         * code of redefined method and by invoking these methods when a super method should be invoked.
         */
        class ForRebaseInstrumentation implements TargetHandler {

            /**
             * The method name transformer to apply during instrumentation.
             */
            private final MethodRebaseResolver.MethodNameTransformer methodNameTransformer;

            /**
             * Creates a new rebase instrumentation target handler.
             *
             * @param methodNameTransformer The method name transformer to apply during instrumentation.
             */
            public ForRebaseInstrumentation(MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
                this.methodNameTransformer = methodNameTransformer;
            }

            @Override
            public Instrumentation.Target.Factory makeInstrumentationTargetFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                return new RebaseInstrumentationTarget.Factory(bridgeMethodResolverFactory, methodNameTransformer);
            }

            @Override
            public Compiled compile(MethodList instrumentedMethods,
                                    TypeDescription instrumentedType,
                                    ClassFileVersion classFileVersion,
                                    Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy) {
                return Compiled.ForRebasement.of(instrumentedMethods,
                        methodNameTransformer,
                        instrumentedType,
                        classFileVersion,
                        auxiliaryTypeNamingStrategy);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodNameTransformer.equals(((ForRebaseInstrumentation) other).methodNameTransformer);
            }

            @Override
            public int hashCode() {
                return methodNameTransformer.hashCode();
            }

            @Override
            public String toString() {
                return "InlineDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation{" +
                        "methodNameTransformer=" + methodNameTransformer +
                        '}';
            }
        }
    }
}

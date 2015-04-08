package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMethodMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isOverridable;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Creates a dynamic type on basis of loaded types where the dynamic type extends the loaded types.
 *
 * @param <T> The best known loaded type representing the built dynamic type.
 */
public class SubclassDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    private final ConstructorStrategy constructorStrategy;

    public SubclassDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                      NamingStrategy namingStrategy,
                                      AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                      TypeDescription superType,
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
                                      ConstructorStrategy constructorStrategy) {
        this(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                superType,
                new ArrayList<TypeDescription>(interfaceTypes),
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
                Collections.<FieldToken>emptyList(),
                Collections.<MethodToken>emptyList(),
                constructorStrategy);
    }

    /**
     * Creates a new immutable type builder for a subclassing a given class.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param superType                             The super class that the dynamic type should extend.
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
                                         AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                         TypeDescription superType,
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
                                         ConstructorStrategy constructorStrategy) {
        super(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
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
                methodTokens);
        this.constructorStrategy = constructorStrategy;
    }

    @Override
    protected DynamicType.Builder<T> materialize(ClassFileVersion classFileVersion,
                                                 NamingStrategy namingStrategy,
                                                 AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                 TypeDescription targetType,
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
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                targetType,
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
    public DynamicType.Unloaded<T> make() {
        InstrumentedType instrumentedType = new SubclassInstrumentedType(classFileVersion, targetType, interfaceTypes, modifiers, namingStrategy);
        MethodRegistry.Compiled compiledMethodRegistry = constructorStrategy
                .inject(methodRegistry, defaultMethodAttributeAppenderFactory)
                .prepare(applyConstructorStrategy(applyRecordedMembersTo(instrumentedType)),
                        methodLookupEngineFactory.make(classFileVersion.isSupportsDefaultMethods()),
                        new InstrumentableMatcher(ignoredMethods))
                .compile(new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory,
                        SubclassInstrumentationTarget.OriginTypeIdentifier.SUPER_TYPE));
        return TypeWriter.Default.<T>forCreation(compiledMethodRegistry,
                fieldRegistry.prepare(compiledMethodRegistry.getInstrumentedType()).compile(TypeWriter.FieldPool.Entry.NoOp.INSTANCE),
                auxiliaryTypeNamingStrategy,
                classVisitorWrapperChain,
                attributeAppender,
                classFileVersion).make();
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
                    methodDescription.getParameters().asTypeList(),
                    methodDescription.getExceptionTypes(),
                    methodDescription.getModifiers());
        }
        return instrumentedType;
    }

    protected static class InstrumentableMatcher implements LatentMethodMatcher {

        private final ElementMatcher<? super MethodDescription> ignoredMethods;

        public InstrumentableMatcher(ElementMatcher<? super MethodDescription> ignoredMethods) {
            this.ignoredMethods = ignoredMethods;
        }

        @Override
        public ElementMatcher<? super MethodDescription> manifest(TypeDescription typeDescription) {
            return isOverridable().and(not(ignoredMethods)).or(isDeclaredBy(typeDescription));
        }
    }
}

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
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A dynamic type builder that rebases a given type, i.e. it behaves like if a subclass was defined where any methods
 * are later inlined into the rebased type and the original methods are copied while using a different name space.
 *
 * @param <T> The actual type of the rebased type.
 */
public class RebaseDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    /**
     * A locator for finding a class file to a given type.
     */
    private final ClassFileLocator classFileLocator;

    /**
     * A name transformer that transforms names of any rebased method.
     */
    private final MethodRebaseResolver.MethodNameTransformer methodNameTransformer;

    /**
     * Creates a new rebase dynamic type builder.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param auxiliaryTypeNamingStrategy           The naming strategy for naming auxiliary types of the dynamic type.
     * @param levelType                             The type that is to be rebased.
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
     * @param classFileLocator                      A locator for finding a class file to a given type.
     * @param methodNameTransformer                 A name transformer that transforms names of any rebased method.
     */
    public RebaseDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                    NamingStrategy namingStrategy,
                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
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
                                    MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        this(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
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
                methodNameTransformer);
    }

    /**
     * Creates a new rebase dynamic type builder.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param auxiliaryTypeNamingStrategy           The naming strategy for naming auxiliary types of the dynamic type.
     * @param levelType                             The type that is to be rebased.
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
     * @param classFileLocator                      A locator for finding a class file to a given type.
     * @param methodNameTransformer                 A name transformer that transforms names of any rebased method.
     */
    protected RebaseDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                       NamingStrategy namingStrategy,
                                       AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
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
                                       MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        super(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
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
        this.methodNameTransformer = methodNameTransformer;
    }

    @Override
    protected DynamicType.Builder<T> materialize(ClassFileVersion classFileVersion,
                                                 NamingStrategy namingStrategy,
                                                 AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
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
        return new RebaseDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
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
                methodNameTransformer);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Prepared preparedMethodRegistry = methodRegistry.prepare(applyRecordedMembersTo(new InlineInstrumentedType(classFileVersion,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        namingStrategy)),
                methodLookupEngineFactory.make(classFileVersion.isSupportsDefaultMethods()),
                InlineInstrumentationMatcher.of(ignoredMethods, targetType));
        MethodRebaseResolver methodRebaseResolver = MethodRebaseResolver.Enabled.make(preparedMethodRegistry.getInstrumentedMethods(),
                preparedMethodRegistry.getInstrumentedType(),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                methodNameTransformer);
        MethodRegistry.Compiled compiledMethodRegistry = preparedMethodRegistry.compile(new RebaseInstrumentationTarget.Factory(bridgeMethodResolverFactory,
                methodRebaseResolver));
        return TypeWriter.Default.<T>forRebasing(compiledMethodRegistry,
                fieldRegistry.prepare(compiledMethodRegistry.getInstrumentedType()).compile(TypeWriter.FieldPool.Entry.NoOp.INSTANCE),
                auxiliaryTypeNamingStrategy,
                classVisitorWrapperChain,
                attributeAppender,
                classFileVersion,
                classFileLocator,
                targetType,
                methodRebaseResolver).make();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && classFileLocator.equals(((RebaseDynamicTypeBuilder<?>) other).classFileLocator)
                && methodNameTransformer.equals(((RebaseDynamicTypeBuilder<?>) other).methodNameTransformer);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + classFileLocator.hashCode();
        result = 31 * result + methodNameTransformer.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RebaseDynamicTypeBuilder{" +
                "classFileVersion=" + classFileVersion +
                ", namingStrategy=" + namingStrategy +
                ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                ", targetType=" + targetType +
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
                ", fieldTokens=" + fieldTokens +
                ", methodTokens=" + methodTokens +
                ", classFileLocator=" + classFileLocator +
                ", methodNameTransformer=" + methodNameTransformer +
                '}';
    }
}

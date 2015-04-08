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
import net.bytebuddy.instrumentation.Instrumentation;
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

import static net.bytebuddy.matcher.ElementMatchers.*;


public class RebaseDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    /**
     * A locator for finding a class file.
     */
    private final ClassFileLocator classFileLocator;

    private final MethodRebaseResolver.MethodNameTransformer methodNameTransformer;

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
        InstrumentedType instrumentedType = new InlineInstrumentedType(classFileVersion, targetType, interfaceTypes, modifiers, namingStrategy);
        MethodRegistry.Prepared preparedMethodRegistry = methodRegistry.prepare(applyRecordedMembersTo(instrumentedType),
                methodLookupEngineFactory.make(classFileVersion.isSupportsDefaultMethods()),
                isOverridable().and(not(ignoredMethods)).or(isDeclaredBy(instrumentedType).and(not(anyOf(targetType.getDeclaredMethods())))));
        MethodRebaseResolver methodRebaseResolver = MethodRebaseResolver.Enabled.make(preparedMethodRegistry.getInstrumentedMethods(),
                instrumentedType,
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
}

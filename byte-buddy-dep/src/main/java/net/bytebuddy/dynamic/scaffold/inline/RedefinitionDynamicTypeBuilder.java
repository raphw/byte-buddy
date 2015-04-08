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

import static net.bytebuddy.matcher.ElementMatchers.*;

public class RedefinitionDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    private final ClassFileLocator classFileLocator;

    public RedefinitionDynamicTypeBuilder(ClassFileVersion classFileVersion,
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
                                          ClassFileLocator classFileLocator) {
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
                classFileLocator);
    }

    protected RedefinitionDynamicTypeBuilder(ClassFileVersion classFileVersion,
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
                                             List<FieldToken> fieldTokens,
                                             List<MethodToken> methodTokens,
                                             ClassFileLocator classFileLocator) {
        super(classFileVersion,
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
                fieldTokens,
                methodTokens);
        this.classFileLocator = classFileLocator;
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
        return new RedefinitionDynamicTypeBuilder<T>(classFileVersion,
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
                classFileLocator);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        InstrumentedType instrumentedType = new InlineInstrumentedType(classFileVersion, targetType, interfaceTypes, modifiers, namingStrategy);
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.prepare(applyRecordedMembersTo(instrumentedType),
                methodLookupEngineFactory.make(classFileVersion.isSupportsDefaultMethods()),
                InlineInstrumentationMatcher.of(ignoredMethods, targetType))
                .compile(new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory, SubclassInstrumentationTarget.OriginTypeIdentifier.LEVEL_TYPE));
        return TypeWriter.Default.<T>forRedefinition(compiledMethodRegistry,
                fieldRegistry.prepare(instrumentedType).compile(TypeWriter.FieldPool.Entry.NoOp.INSTANCE),
                auxiliaryTypeNamingStrategy,
                classVisitorWrapperChain,
                attributeAppender,
                classFileVersion,
                classFileLocator,
                targetType).make();
    }
}

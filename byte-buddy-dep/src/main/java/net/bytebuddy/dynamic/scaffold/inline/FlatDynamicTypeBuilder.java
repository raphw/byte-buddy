package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlatDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    private final ClassFileLocator classFileLocator;

    public FlatDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                  NamingStrategy namingStrategy,
                                  TypeDescription levelType,
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
                                  ClassFileLocator classFileLocator) {
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
                classFileLocator);
    }

    public FlatDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                  NamingStrategy namingStrategy,
                                  TypeDescription levelType,
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
                                  ClassFileLocator classFileLocator) {
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
    }

    @Override
    protected DynamicType.Builder<T> materialize(ClassFileVersion classFileVersion,
                                                 NamingStrategy namingStrategy,
                                                 TypeDescription levelType,
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
                                                 List<MethodToken> methodTokens) {
        return new FlatDynamicTypeBuilder<T>(classFileVersion,
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
                classFileLocator);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.compile(applyRecordedMembersTo(new FlatInstrumentedType(classFileVersion,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        namingStrategy)),
                methodLookupEngineFactory.make(classFileVersion),
                new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory),
                MethodRegistry.Compiled.Entry.Skip.INSTANCE);


        return null; // TODO: Implement the type's creation.
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && classFileLocator.equals(((FlatDynamicTypeBuilder<?>) other).classFileLocator);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + classFileLocator.hashCode();
    }

    @Override
    public String toString() {
        return "FlatDynamicTypeBuilder{" +
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
                '}';
    }
}

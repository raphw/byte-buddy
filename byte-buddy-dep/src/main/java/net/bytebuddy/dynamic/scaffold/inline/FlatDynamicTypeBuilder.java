package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.method.matcher.JunctionMethodMatcher;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.instrumentation.type.auxiliary.TrivialType;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * A dynamic type builder which enhances a given type without creating a subclass.
 *
 * @param <T> The most specific type that is known to be represented by the enhanced type.
 */
public class FlatDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

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
    protected FlatDynamicTypeBuilder(ClassFileVersion classFileVersion,
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
                classFileLocator,
                targetHandler);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Prepared preparedMethodRegistry = methodRegistry.prepare(
                applyRecordedMembersTo(new FlatInstrumentedType(classFileVersion,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        namingStrategy)));
        TargetHandler.Prepared preparedTargetHandler = targetHandler.prepare(ignoredMethods,
                classFileVersion,
                preparedMethodRegistry.getInstrumentedType());
        MethodRegistry.Compiled compiledMethodRegistry = preparedMethodRegistry.compile(preparedTargetHandler.factory(bridgeMethodResolverFactory),
                methodLookupEngineFactory.make(classFileVersion),
                preparedTargetHandler.getMethodPoolEntryDefault());
        return new TypeWriter.Default<T>(compiledMethodRegistry.getInstrumentedType(),
                compiledMethodRegistry.getLoadedTypeInitializer(),
                preparedTargetHandler.getAuxiliaryTypes(),
                new TypeWriter.Engine.ForRedefinition(compiledMethodRegistry.getInstrumentedType(),
                        targetType,
                        classFileVersion,
                        compiledMethodRegistry.getInvokableMethods().filter(isOverridable()
                                .or(isDeclaredBy(compiledMethodRegistry.getInstrumentedType()))
                                .and(not(ignoredMethods).or(isDeclaredBy(compiledMethodRegistry.getInstrumentedType())
                                        .and(not(anyOf(targetType.getDeclaredMethods())))))),
                        classVisitorWrapperChain,
                        attributeAppender,
                        fieldRegistry.prepare(compiledMethodRegistry.getInstrumentedType()).compile(TypeWriter.FieldPool.Entry.NoOp.INSTANCE),
                        compiledMethodRegistry,
                        new TypeWriter.Engine.ForRedefinition.InputStreamProvider.ForClassFileLocator(targetType, classFileLocator),
                        preparedTargetHandler.getMethodFlatteningResolver()))
                .make(new TypeExtensionDelegate(compiledMethodRegistry.getInstrumentedType(), classFileVersion));
    }

    /**
     * Matches any method description of the list of given methods.
     *
     * @param methodDescriptions A list of method descriptions to match.
     * @return A method matcher that matches the list of methods.
     */
    private static MethodMatcher anyOf(List<MethodDescription> methodDescriptions) {
        JunctionMethodMatcher methodMatcher = none();
        for (MethodDescription methodDescription : methodDescriptions) {
            methodMatcher = methodMatcher.or(is(methodDescription));
        }
        return methodMatcher;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && classFileLocator.equals(((FlatDynamicTypeBuilder<?>) other).classFileLocator)
                && targetHandler.equals(((FlatDynamicTypeBuilder<?>) other).targetHandler);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * super.hashCode() + classFileLocator.hashCode()) + targetHandler.hashCode();
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
                ", targetHandler=" + targetHandler +
                '}';
    }

    public static interface TargetHandler {

        static enum ForRebaseInstrumentation implements TargetHandler {

            INSTANCE;

            @Override
            public Prepared prepare(MethodMatcher ignoredMethods,
                                    ClassFileVersion classFileVersion,
                                    TypeDescription instrumentedType) {
                return new Prepared.ForRebaseInstrumentation(ignoredMethods,
                        classFileVersion,
                        instrumentedType);
            }
        }

        static enum ForSubclassInstrumentation implements TargetHandler {

            INSTANCE;

            @Override
            public Prepared prepare(MethodMatcher ignoredMethods,
                                    ClassFileVersion classFileVersion,
                                    TypeDescription instrumentedType) {
                return Prepared.ForSubclassInstrumentation.INSTANCE;
            }
        }

        Prepared prepare(MethodMatcher ignoredMethods,
                         ClassFileVersion classFileVersion,
                         TypeDescription instrumentedType);

        static interface Prepared {

            MethodFlatteningResolver getMethodFlatteningResolver();

            TypeWriter.MethodPool.Entry.Factory getMethodPoolEntryDefault();

            static class ForRebaseInstrumentation implements Prepared {

                private static final String SUFFIX = "trivial";

                private final MethodFlatteningResolver methodFlatteningResolver;

                private final DynamicType placeholderType;

                public ForRebaseInstrumentation(MethodMatcher ignoredMethods,
                                                ClassFileVersion classFileVersion,
                                                TypeDescription instrumentedType) {
                    placeholderType = TrivialType.INSTANCE.make(trivialTypeNameFor(instrumentedType),
                            classFileVersion,
                            AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE);
                    this.methodFlatteningResolver = new MethodFlatteningResolver.Default(ignoredMethods,
                            placeholderType.getDescription(),
                            new MethodFlatteningResolver.MethodNameTransformer.Suffixing(new RandomString()));
                }

                private static String trivialTypeNameFor(TypeDescription rawInstrumentedType) {
                    return String.format("%s$%s$%s",
                            rawInstrumentedType.getInternalName(),
                            SUFFIX,
                            RandomString.make());
                }

                @Override
                public Instrumentation.Target.Factory factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                    return new RebaseInstrumentationTarget.Factory(bridgeMethodResolverFactory,
                            methodFlatteningResolver);
                }

                @Override
                public List<DynamicType> getAuxiliaryTypes() {
                    return Collections.singletonList(placeholderType);
                }

                @Override
                public MethodFlatteningResolver getMethodFlatteningResolver() {
                    return methodFlatteningResolver;
                }

                @Override
                public TypeWriter.MethodPool.Entry.Factory getMethodPoolEntryDefault() {
                    return MethodFlatteningDelegation.Factory.INSTANCE;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && placeholderType.equals(((ForRebaseInstrumentation) other).placeholderType)
                            && methodFlatteningResolver.equals(((ForRebaseInstrumentation) other).methodFlatteningResolver);
                }

                @Override
                public int hashCode() {
                    return 31 * methodFlatteningResolver.hashCode() + placeholderType.hashCode();
                }

                @Override
                public String toString() {
                    return "FlatDynamicTypeBuilder.TargetHandler.Prepared.ForRebaseInstrumentation{" +
                            "methodFlatteningResolver=" + methodFlatteningResolver +
                            ", placeholderType=" + placeholderType +
                            '}';
                }

                private static class MethodFlatteningDelegation implements TypeWriter.MethodPool.Entry, ByteCodeAppender {

                    private static enum Factory implements TypeWriter.MethodPool.Entry.Factory {

                        INSTANCE;

                        @Override
                        public TypeWriter.MethodPool.Entry compile(Instrumentation.Target instrumentationTarget) {
                            return new MethodFlatteningDelegation(instrumentationTarget);
                        }
                    }

                    private final Instrumentation.Target instrumentationTarget;

                    private MethodFlatteningDelegation(Instrumentation.Target instrumentationTarget) {
                        this.instrumentationTarget = instrumentationTarget;
                    }

                    @Override
                    public boolean isDefineMethod() {
                        return true;
                    }

                    @Override
                    public ByteCodeAppender getByteCodeAppender() {
                        return this;
                    }

                    @Override
                    public MethodAttributeAppender getAttributeAppender() {
                        return MethodAttributeAppender.NoOp.INSTANCE;
                    }

                    @Override
                    public boolean appendsCode() {
                        return true;
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor,
                                      Instrumentation.Context instrumentationContext,
                                      MethodDescription instrumentedMethod) {
                        return new Size(new StackManipulation.Compound(
                                MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                                instrumentedMethod.isTypeInitializer()
                                        ? StackManipulation.LegalTrivial.INSTANCE
                                        : instrumentationTarget.invokeSuper(instrumentedMethod, Instrumentation.Target.MethodLookup.Default.EXACT),
                                MethodReturn.returning(instrumentedMethod.getReturnType())
                        ).apply(methodVisitor, instrumentationContext).getMaximalSize(), instrumentedMethod.getStackSize());
                    }

                    @Override
                    public void apply(ClassVisitor classVisitor,
                                      Instrumentation.Context instrumentationContext,
                                      MethodDescription methodDescription) {
                        MethodVisitor methodVisitor = classVisitor.visitMethod(methodDescription.getAdjustedModifiers(true),
                                methodDescription.getInternalName(),
                                methodDescription.getDescriptor(),
                                methodDescription.getGenericSignature(),
                                methodDescription.getExceptionTypes().toInternalNames());
                        methodVisitor.visitCode();
                        Size size = apply(methodVisitor, instrumentationContext, methodDescription);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                        methodVisitor.visitEnd();
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        MethodFlatteningDelegation that = (MethodFlatteningDelegation) other;
                        return instrumentationTarget.equals(that.instrumentationTarget);
                    }

                    @Override
                    public int hashCode() {
                        return instrumentationTarget.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "FlatDynamicTypeBuilder.MethodFlatteningDelegation{instrumentationTarget=" + instrumentationTarget + '}';
                    }
                }
            }

            static enum ForSubclassInstrumentation implements Prepared {

                INSTANCE;

                @Override
                public MethodFlatteningResolver getMethodFlatteningResolver() {
                    return MethodFlatteningResolver.NoOp.INSTANCE;
                }

                @Override
                public TypeWriter.MethodPool.Entry.Factory getMethodPoolEntryDefault() {
                    return TypeWriter.MethodPool.Entry.Skip.INSTANCE;
                }

                @Override
                public Instrumentation.Target.Factory factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                    return new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory);
                }

                @Override
                public List<DynamicType> getAuxiliaryTypes() {
                    return Collections.emptyList();
                }
            }

            Instrumentation.Target.Factory factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory);

            List<DynamicType> getAuxiliaryTypes();
        }
    }
}

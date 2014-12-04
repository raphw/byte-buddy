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
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.instrumentation.type.auxiliary.TrivialType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

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
        MethodRegistry.Prepared preparedMethodRegistry = methodRegistry.prepare(
                applyRecordedMembersTo(new InlineInstrumentedType(classFileVersion,
                        targetType,
                        interfaceTypes,
                        modifiers,
                        namingStrategy)));
        TargetHandler.Prepared preparedTargetHandler = targetHandler.prepare(ignoredMethods,
                classFileVersion,
                preparedMethodRegistry.getInstrumentedType());
        MethodRegistry.Compiled compiledMethodRegistry = preparedMethodRegistry.compile(preparedTargetHandler.factory(bridgeMethodResolverFactory),
                methodLookupEngineFactory.make(classFileVersion.isSupportsDefaultMethods()),
                preparedTargetHandler.getMethodPoolEntryDefault());
        return new TypeWriter.Default<T>(compiledMethodRegistry.getInstrumentedType(),
                compiledMethodRegistry.getLoadedTypeInitializer(),
                compiledMethodRegistry.getTypeInitializer(),
                preparedTargetHandler.getAuxiliaryTypes(),
                classFileVersion,
                new TypeWriter.Engine.ForRedefinition(compiledMethodRegistry.getInstrumentedType(),
                        targetType,
                        classFileVersion,
                        compiledMethodRegistry.getInvokableMethods().filter(isOverridable()
                                .<MethodDescription>or(isDeclaredBy(compiledMethodRegistry.getInstrumentedType()))
                                .and(not(ignoredMethods).or(isDeclaredBy(compiledMethodRegistry.getInstrumentedType())
                                        .<MethodDescription>and(not(anyOf(targetType.getDeclaredMethods())))))),
                        classVisitorWrapperChain,
                        attributeAppender,
                        fieldRegistry.prepare(compiledMethodRegistry.getInstrumentedType()).compile(TypeWriter.FieldPool.Entry.NoOp.INSTANCE),
                        compiledMethodRegistry,
                        classFileLocator,
                        preparedTargetHandler.getMethodRebaseResolver()))
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

    /**
     * An inline dynamic type builder's target handler is responsible to proving any information that is required
     * for defining the type.
     */
    public static interface TargetHandler {

        /**
         * Prepares this target handler to a given set of type creation properties.
         *
         * @param ignoredMethods   A matcher for determining methods that are to be ignored for instrumentation.
         * @param classFileVersion The class file version for the created dynamic type.
         * @param instrumentedType The instrumented type.
         * @return A prepared target handler.
         */
        Prepared prepare(ElementMatcher<? super MethodDescription> ignoredMethods,
                         ClassFileVersion classFileVersion,
                         TypeDescription instrumentedType);

        /**
         * Performs a subclass instrumentation which creates a redefinition of the given type by invoking the
         * actual super method when redefining a method.
         */
        static enum ForRedefinitionInstrumentation implements TargetHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Prepared prepare(ElementMatcher<? super MethodDescription> ignoredMethods,
                                    ClassFileVersion classFileVersion,
                                    TypeDescription instrumentedType) {
                return Prepared.ForRedefinitionInstrumentation.INSTANCE;
            }
        }

        /**
         * A prepared {@link InlineDynamicTypeBuilder.TargetHandler}.
         */
        static interface Prepared {

            /**
             * Returns the method rebase resolver to be used when creating the dynamic type.
             *
             * @return The method rebase resolver to be used when creating the dynamic type.
             */
            MethodRebaseResolver getMethodRebaseResolver();

            /**
             * Returns the method pool entry that should be used as a default.
             *
             * @return The method pool entry that should be used as a default.
             */
            TypeWriter.MethodPool.Entry.Factory getMethodPoolEntryDefault();

            /**
             * Creates an instrumentation target factory that is to be used when creating the type.
             *
             * @param bridgeMethodResolverFactory A bridge method resolver factory.
             * @return An instrumentation target factory that is to be used when creating the type.
             */
            Instrumentation.Target.Factory factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory);

            /**
             * Returns a list of explicitly registered auxiliary types.
             *
             * @return A list of explicitly registered auxiliary types.
             */
            List<DynamicType> getAuxiliaryTypes();

            /**
             * A prepared target handler for an instrumentation that creates a redefinition of the given type by
             * invoking the actual super method when redefining a method.
             */
            static enum ForRedefinitionInstrumentation implements Prepared {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public MethodRebaseResolver getMethodRebaseResolver() {
                    return MethodRebaseResolver.NoOp.INSTANCE;
                }

                @Override
                public TypeWriter.MethodPool.Entry.Factory getMethodPoolEntryDefault() {
                    return TypeWriter.MethodPool.Entry.Skip.INSTANCE;
                }

                @Override
                public Instrumentation.Target.Factory factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                    return new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory,
                            SubclassInstrumentationTarget.OriginTypeIdentifier.LEVEL_TYPE);
                }

                @Override
                public List<DynamicType> getAuxiliaryTypes() {
                    return Collections.emptyList();
                }
            }

            /**
             * A prepared target handler for an instrumentation that creates a redefinition of the given type by
             * rebasing the original code of redefined method and by invoking these methods when a super method
             * should be invoked.
             */
            static class ForRebaseInstrumentation implements Prepared {

                /**
                 * The suffix that is to be used when creating a placeholder type.
                 */
                private static final String SUFFIX = "placeholder";

                /**
                 * The method rebase resolver to use.
                 */
                private final MethodRebaseResolver methodRebaseResolver;

                /**
                 * The placeholder type to use.
                 */
                private final DynamicType placeholderType;

                /**
                 * Creates a new prepared target handler for a rebase instrumentation.
                 *
                 * @param placeholderType       The placeholder type to use for rebasing constructors.
                 * @param ignoredMethods        The methods that should be ignored for rebasing.
                 * @param methodNameTransformer The method name transformer to be applied by the created
                 *                              method rebase resolver.
                 */
                protected ForRebaseInstrumentation(DynamicType placeholderType,
                                                   ElementMatcher<? super MethodDescription> ignoredMethods,
                                                   MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
                    this.placeholderType = placeholderType;
                    methodRebaseResolver = new MethodRebaseResolver.Default(ignoredMethods,
                            placeholderType.getTypeDescription(),
                            methodNameTransformer);
                }

                /**
                 * Creates a target handler for a rebase instrumentation.
                 *
                 * @param ignoredMethods        The methods that should be ignored for rebasing.
                 * @param classFileVersion      The class file version for the created dynamic type.
                 * @param instrumentedType      The instrumented type.
                 * @param methodNameTransformer The method name transformer to be applied by the created
                 *                              method rebase resolver.
                 * @return A prepared target handler.
                 */
                public static Prepared of(ElementMatcher<? super MethodDescription> ignoredMethods,
                                          ClassFileVersion classFileVersion,
                                          TypeDescription instrumentedType,
                                          MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
                    RandomString randomString = new RandomString();
                    return new ForRebaseInstrumentation(TrivialType.INSTANCE
                            .make(trivialTypeNameFor(instrumentedType, randomString),
                                    classFileVersion,
                                    AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE),
                            ignoredMethods,
                            methodNameTransformer);
                }

                /**
                 * Creates a trivial name for the instrumented type.
                 *
                 * @param instrumentedType The instrumented type.
                 * @param randomString     A random string supplier.
                 * @return A trivial name that is derived from the supplied instrumented type.
                 */
                private static String trivialTypeNameFor(TypeDescription instrumentedType, RandomString randomString) {
                    return String.format("%s$%s$%s",
                            instrumentedType.getName(),
                            SUFFIX,
                            randomString.nextString());
                }

                @Override
                public Instrumentation.Target.Factory factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                    return new RebaseInstrumentationTarget.Factory(bridgeMethodResolverFactory,
                            methodRebaseResolver);
                }

                @Override
                public List<DynamicType> getAuxiliaryTypes() {
                    return Collections.singletonList(placeholderType);
                }

                @Override
                public MethodRebaseResolver getMethodRebaseResolver() {
                    return methodRebaseResolver;
                }

                @Override
                public TypeWriter.MethodPool.Entry.Factory getMethodPoolEntryDefault() {
                    return MethodRebaseDelegation.Factory.INSTANCE;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForRebaseInstrumentation that = (ForRebaseInstrumentation) other;
                    return methodRebaseResolver.equals(that.methodRebaseResolver)
                            && placeholderType.equals(that.placeholderType);
                }

                @Override
                public int hashCode() {
                    int result = methodRebaseResolver.hashCode();
                    result = 31 * result + placeholderType.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "InlineDynamicTypeBuilder.TargetHandler.Prepared.ForRebaseInstrumentation{" +
                            "methodRebaseResolver=" + methodRebaseResolver +
                            ", placeholderType=" + placeholderType +
                            '}';
                }

                /**
                 * A method pool entry with a rebase implementation as its default behavior.
                 */
                protected static class MethodRebaseDelegation implements TypeWriter.MethodPool.Entry, ByteCodeAppender {

                    /**
                     * The instrumentation target.
                     */
                    private final Instrumentation.Target instrumentationTarget;

                    /**
                     * Creates a new method rebase delegation.
                     *
                     * @param instrumentationTarget The instrumentation target.
                     */
                    protected MethodRebaseDelegation(Instrumentation.Target instrumentationTarget) {
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
                        MethodRebaseDelegation that = (MethodRebaseDelegation) other;
                        return instrumentationTarget.equals(that.instrumentationTarget);
                    }

                    @Override
                    public int hashCode() {
                        return instrumentationTarget.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "InlineDynamicTypeBuilder.TargetHandler.Prepared.ForRebaseInstrumentation.MethodRebaseDelegation{" +
                                "instrumentationTarget=" + instrumentationTarget +
                                '}';
                    }

                    /**
                     * A factory for creating a method rebase delegation entry.
                     */
                    protected static enum Factory implements TypeWriter.MethodPool.Entry.Factory {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        @Override
                        public TypeWriter.MethodPool.Entry compile(Instrumentation.Target instrumentationTarget) {
                            return new MethodRebaseDelegation(instrumentationTarget);
                        }
                    }
                }
            }
        }

        /**
         * Performs a rebase instrumentation which creates a redefinition of the given type by rebasing the original
         * code of redefined method and by invoking these methods when a super method should be invoked.
         */
        static class ForRebaseInstrumentation implements TargetHandler {

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
            public Prepared prepare(ElementMatcher<? super MethodDescription> ignoredMethods,
                                    ClassFileVersion classFileVersion,
                                    TypeDescription instrumentedType) {
                return Prepared.ForRebaseInstrumentation.of(ignoredMethods,
                        classFileVersion,
                        instrumentedType,
                        methodNameTransformer);
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

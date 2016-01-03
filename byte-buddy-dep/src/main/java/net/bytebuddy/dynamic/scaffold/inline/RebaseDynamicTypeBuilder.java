package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.AnnotationAppender;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A dynamic type builder that rebases a given type, i.e. it behaves like if a subclass was defined where any methods
 * are later inlined into the rebased type and the original methods are copied while using a different name space.
 *
 * @param <T> The actual type of the rebased type.
 */
public class RebaseDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    private final TypeDescription originalType;

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
     * @param implementationContextFactory          The implementation context factory to use.
     * @param interfaceTypes                        A list of interfaces that should be implemented by the created dynamic type.
     * @param modifiers                             The modifiers to be represented by the dynamic type.
     * @param attributeAppender                     The attribute appender to apply onto the dynamic type that is created.
     * @param ignoredMethods                        A matcher for determining methods that are to be ignored for instrumentation.
     * @param classVisitorWrapper                   A ASM class visitor to apply to the writing process.
     * @param fieldRegistry                         The field registry to apply to the dynamic type creation.
     * @param methodRegistry                        The method registry to apply to the dynamic type creation.
     * @param methodGraphCompiler                   The method graph compiler to be used.
     * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
     *                                              no specific appender was specified for a given field.
     * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
     *                                              if no specific appender was specified for a given method.
     * @param originalType                          The type that is to be rebased.
     * @param classFileLocator                      A locator for finding a class file to a given type.
     * @param methodNameTransformer                 A name transformer that transforms names of any rebased method.
     */
    public RebaseDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                    NamingStrategy namingStrategy,
                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                    Implementation.Context.Factory implementationContextFactory,
                                    List<TypeDescription.Generic> interfaceTypes,
                                    int modifiers,
                                    TypeAttributeAppender attributeAppender,
                                    ElementMatcher<? super MethodDescription> ignoredMethods,
                                    ClassVisitorWrapper classVisitorWrapper,
                                    FieldRegistry fieldRegistry,
                                    MethodRegistry methodRegistry,
                                    MethodGraph.Compiler methodGraphCompiler,
                                    FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                    MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                    TypeDescription originalType,
                                    ClassFileLocator classFileLocator,
                                    MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        this(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                InstrumentedType.TypeInitializer.None.INSTANCE,
                CompoundList.of(interfaceTypes, originalType.getInterfaces()),
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapper,
                fieldRegistry,
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                originalType.getDeclaredFields().asTokenList(is(originalType)),
                originalType.getDeclaredMethods().asTokenList(is(originalType)),
                originalType,
                classFileLocator,
                methodNameTransformer);
    }

    /**
     * Creates a new rebase dynamic type builder.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param auxiliaryTypeNamingStrategy           The naming strategy for naming auxiliary types of the dynamic type.
     * @param implementationContextFactory          The implementation context factory to use.
     * @param typeInitializer                       The type initializer to use.
     * @param interfaceTypes                        A list of interfaces that should be implemented by the created dynamic type.
     * @param modifiers                             The modifiers to be represented by the dynamic type.
     * @param attributeAppender                     The attribute appender to apply onto the dynamic type that is created.
     * @param ignoredMethods                        A matcher for determining methods that are to be ignored for instrumentation.
     * @param classVisitorWrapper                   A ASM class visitor to apply to the writing process.
     * @param fieldRegistry                         The field registry to apply to the dynamic type creation.
     * @param methodRegistry                        The method registry to apply to the dynamic type creation.
     * @param methodGraphCompiler                   The method graph compiler to be used.
     * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
     *                                              no specific appender was specified for a given field.
     * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
     *                                              if no specific appender was specified for a given method.
     * @param fieldTokens                           A list of field representations that were added explicitly to this
     *                                              dynamic type.
     * @param methodTokens                          A list of method representations that were added explicitly to this
     *                                              dynamic type.
     * @param originalType                          The type that is to be rebased.
     * @param classFileLocator                      A locator for finding a class file to a given type.
     * @param methodNameTransformer                 A name transformer that transforms names of any rebased method.
     */
    protected RebaseDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                       NamingStrategy namingStrategy,
                                       AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                       Implementation.Context.Factory implementationContextFactory,
                                       InstrumentedType.TypeInitializer typeInitializer,
                                       List<TypeDescription.Generic> interfaceTypes,
                                       int modifiers,
                                       TypeAttributeAppender attributeAppender,
                                       ElementMatcher<? super MethodDescription> ignoredMethods,
                                       ClassVisitorWrapper classVisitorWrapper,
                                       FieldRegistry fieldRegistry,
                                       MethodRegistry methodRegistry,
                                       MethodGraph.Compiler methodGraphCompiler,
                                       FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                       MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                       List<FieldDescription.Token> fieldTokens,
                                       List<MethodDescription.Token> methodTokens,
                                       TypeDescription originalType,
                                       ClassFileLocator classFileLocator,
                                       MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
        super(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                typeInitializer,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapper,
                fieldRegistry,
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens);
        this.originalType = originalType;
        this.classFileLocator = classFileLocator;
        this.methodNameTransformer = methodNameTransformer;
    }

    @Override
    protected DynamicType.Builder<T> materialize(ClassFileVersion classFileVersion,
                                                 NamingStrategy namingStrategy,
                                                 AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                 Implementation.Context.Factory implementationContextFactory,
                                                 InstrumentedType.TypeInitializer typeInitializer,
                                                 List<TypeDescription.Generic> interfaceTypes,
                                                 int modifiers,
                                                 TypeAttributeAppender attributeAppender,
                                                 ElementMatcher<? super MethodDescription> ignoredMethods,
                                                 ClassVisitorWrapper classVisitorWrapper,
                                                 FieldRegistry fieldRegistry,
                                                 MethodRegistry methodRegistry,
                                                 MethodGraph.Compiler methodGraphCompiler,
                                                 FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                                 MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                                 List<FieldDescription.Token> fieldTokens,
                                                 List<MethodDescription.Token> methodTokens) {
        return new RebaseDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                typeInitializer,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapper,
                fieldRegistry,
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                originalType,
                classFileLocator,
                methodNameTransformer);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Prepared preparedMethodRegistry = methodRegistry.prepare(new InstrumentedType.Default(namingStrategy.name(new NamingStrategy
                        .UnnamedType.Default(originalType.getSuperType(), interfaceTypes, modifiers, classFileVersion)),
                        modifiers,
                        originalType.getTypeVariables().asSymbols(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(is(originalType))),
                        originalType.getSuperType(),
                        interfaceTypes,
                        fieldTokens,
                        methodTokens,
                        originalType.getDeclaredAnnotations(),
                        typeInitializer,
                        LoadedTypeInitializer.NoOp.INSTANCE,
                        originalType.getDeclaringType(),
                        originalType.getEnclosingMethod(),
                        originalType.getEnclosingType(),
                        originalType.getDeclaredTypes(),
                        originalType.isMemberClass(),
                        originalType.isAnonymousClass(),
                        originalType.isLocalClass()),
                methodGraphCompiler,
                InliningImplementationMatcher.of(ignoredMethods, originalType));
        MethodList<MethodDescription.InDefinedShape> rebaseableMethods = preparedMethodRegistry.getInstrumentedMethods()
                .asDefined()
                .filter(methodRepresentedBy(anyOf(originalType.getDeclaredMethods().asTokenList())));
        MethodRebaseResolver methodRebaseResolver = MethodRebaseResolver.Default.make(preparedMethodRegistry.getInstrumentedType(),
                rebaseableMethods,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                methodNameTransformer);
        MethodRegistry.Compiled compiledMethodRegistry = preparedMethodRegistry.compile(new RebaseImplementationTarget.Factory(rebaseableMethods, methodRebaseResolver));
        return TypeWriter.Default.<T>forRebasing(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                classVisitorWrapper,
                attributeAppender,
                classFileVersion,
                classFileLocator,
                originalType,
                methodRebaseResolver,
                AnnotationAppender.ValueFilter.Default.APPEND_DEFAULTS).make();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && originalType.equals(((RebaseDynamicTypeBuilder<?>) other).originalType)
                && classFileLocator.equals(((RebaseDynamicTypeBuilder<?>) other).classFileLocator)
                && methodNameTransformer.equals(((RebaseDynamicTypeBuilder<?>) other).methodNameTransformer);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + originalType.hashCode();
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
                ", implementationContextFactory=" + implementationContextFactory +
                ", typeInitializer=" + typeInitializer +
                ", originalType=" + originalType +
                ", interfaceTypes=" + interfaceTypes +
                ", modifiers=" + modifiers +
                ", attributeAppender=" + attributeAppender +
                ", ignoredMethods=" + ignoredMethods +
                ", classVisitorWrapper=" + classVisitorWrapper +
                ", fieldRegistry=" + fieldRegistry +
                ", methodRegistry=" + methodRegistry +
                ", methodGraphCompiler=" + methodGraphCompiler +
                ", defaultFieldAttributeAppenderFactory=" + defaultFieldAttributeAppenderFactory +
                ", defaultMethodAttributeAppenderFactory=" + defaultMethodAttributeAppenderFactory +
                ", fieldTokens=" + fieldTokens +
                ", methodTokens=" + methodTokens +
                ", classFileLocator=" + classFileLocator +
                ", methodNameTransformer=" + methodNameTransformer +
                '}';
    }
}

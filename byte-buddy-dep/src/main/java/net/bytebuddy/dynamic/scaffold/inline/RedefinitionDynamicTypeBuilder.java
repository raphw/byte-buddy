package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * A dynamic type builder that redefines a given type, i.e. it replaces any redefined method with another implementation.
 *
 * @param <T> The actual type of the rebased type.
 */
public class RedefinitionDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    private final TypeDescription originalType;

    /**
     * A locator for finding a class file to a given type.
     */
    private final ClassFileLocator classFileLocator;

    /**
     * Creates a new redefinition dynamic type builder.
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
     * @param originalType                          The type that is to be redefined.
     * @param classFileLocator                      A locator for finding a class file to a given type.
     */
    public RedefinitionDynamicTypeBuilder(ClassFileVersion classFileVersion,
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
                                          ClassFileLocator classFileLocator) {
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
                classFileLocator);
    }

    /**
     * Creates a new redefinition dynamic type builder.
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
     * @param originalType                          The type that is to be redefined.
     * @param classFileLocator                      A locator for finding a class file.
     */
    protected RedefinitionDynamicTypeBuilder(ClassFileVersion classFileVersion,
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
                                             ClassFileLocator classFileLocator) {
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
        return new RedefinitionDynamicTypeBuilder<T>(classFileVersion,
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
                classFileLocator);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.prepare(new InstrumentedType.Default(namingStrategy
                        .name(new NamingStrategy.UnnamedType.Default(originalType.getSuperType(), interfaceTypes, modifiers, classFileVersion)),
                        modifiers,
                        originalType.getTypeVariables().accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(is(originalType))),
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
                InliningImplementationMatcher.of(ignoredMethods, originalType))
                .compile(new SubclassImplementationTarget.Factory(SubclassImplementationTarget.OriginTypeResolver.LEVEL_TYPE));
        return TypeWriter.Default.<T>forRedefinition(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                classVisitorWrapper,
                attributeAppender,
                classFileVersion,
                classFileLocator,
                originalType).make();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && originalType.equals(((RedefinitionDynamicTypeBuilder<?>) other).originalType)
                && classFileLocator.equals(((RedefinitionDynamicTypeBuilder<?>) other).classFileLocator);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * super.hashCode() + originalType.hashCode()) + classFileLocator.hashCode();
    }

    @Override
    public String toString() {
        return "RedefinitionDynamicTypeBuilder{" +
                "classFileVersion=" + classFileVersion +
                ", namingStrategy=" + namingStrategy +
                ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                ", implementationContextFactory=" + implementationContextFactory +
                ", typeInitializer=" + typeInitializer +
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
                ", originalType=" + originalType +
                ", classFileLocator=" + classFileLocator +
                '}';
    }
}

package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.utility.ByteBuddyCommons.joinUniqueRaw;

/**
 * A dynamic type builder that redefines a given type, i.e. it replaces any redefined method with another implementation.
 *
 * @param <T> The actual type of the rebased type.
 */
public class RedefinitionDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

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
     * @param levelType                             The type that is to be redefined.
     * @param interfaceTypes                        A list of interfaces that should be implemented by the created dynamic type.
     * @param modifiers                             The modifiers to be represented by the dynamic type.
     * @param attributeAppender                     The attribute appender to apply onto the dynamic type that is created.
     * @param ignoredMethods                        A matcher for determining methods that are to be ignored for instrumentation.
     * @param classVisitorWrapperChain              A chain of ASM class visitors to apply to the writing process.
     * @param fieldRegistry                         The field registry to apply to the dynamic type creation.
     * @param methodRegistry                        The method registry to apply to the dynamic type creation.
     * @param methodGraphCompiler                   The method graph compiler to be used.
     * @param defaultFieldAttributeAppenderFactory  The field attribute appender factory that should be applied by default if
     *                                              no specific appender was specified for a given field.
     * @param defaultMethodAttributeAppenderFactory The method attribute appender factory that should be applied by default
     *                                              if no specific appender was specified for a given method.
     * @param classFileLocator                      A locator for finding a class file to a given type.
     */
    public RedefinitionDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                          NamingStrategy namingStrategy,
                                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                          TypeDescription levelType,
                                          List<TypeDescription> interfaceTypes,
                                          int modifiers,
                                          TypeAttributeAppender attributeAppender,
                                          ElementMatcher<? super MethodDescription> ignoredMethods,
                                          ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                          FieldRegistry fieldRegistry,
                                          MethodRegistry methodRegistry,
                                          MethodGraph.Compiler methodGraphCompiler,
                                          FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                          MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                          ClassFileLocator classFileLocator) {
        this(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                levelType,
                joinUniqueRaw(interfaceTypes, levelType.getInterfaces()),
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                levelType.getDeclaredFields().asTokenList(is(levelType)),
                levelType.getDeclaredMethods().asTokenList(is(levelType)),
                classFileLocator);
    }

    /**
     * Creates a new redefinition dynamic type builder.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param auxiliaryTypeNamingStrategy           The naming strategy for naming auxiliary types of the dynamic type.
     * @param levelType                             The type that is to be redefined.
     * @param interfaceTypes                        A list of interfaces that should be implemented by the created dynamic type.
     * @param modifiers                             The modifiers to be represented by the dynamic type.
     * @param attributeAppender                     The attribute appender to apply onto the dynamic type that is created.
     * @param ignoredMethods                        A matcher for determining methods that are to be ignored for instrumentation.
     * @param classVisitorWrapperChain              A chain of ASM class visitors to apply to the writing process.
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
     * @param classFileLocator                      A locator for finding a class file.
     */
    protected RedefinitionDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                             NamingStrategy namingStrategy,
                                             AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                             TypeDescription levelType,
                                             List<GenericTypeDescription> interfaceTypes,
                                             int modifiers,
                                             TypeAttributeAppender attributeAppender,
                                             ElementMatcher<? super MethodDescription> ignoredMethods,
                                             ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                             FieldRegistry fieldRegistry,
                                             MethodRegistry methodRegistry,
                                             MethodGraph.Compiler methodGraphCompiler,
                                             FieldAttributeAppender.Factory defaultFieldAttributeAppenderFactory,
                                             MethodAttributeAppender.Factory defaultMethodAttributeAppenderFactory,
                                             List<FieldDescription.Token> fieldTokens,
                                             List<MethodDescription.Token> methodTokens,
                                             ClassFileLocator classFileLocator) {
        super(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                levelType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodGraphCompiler,
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
                                                 List<GenericTypeDescription> interfaceTypes,
                                                 int modifiers,
                                                 TypeAttributeAppender attributeAppender,
                                                 ElementMatcher<? super MethodDescription> ignoredMethods,
                                                 ClassVisitorWrapper.Chain classVisitorWrapperChain,
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
                levelType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                fieldTokens,
                methodTokens,
                classFileLocator);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.prepare(new InstrumentedType.Default(namingStrategy
                        .name(new NamingStrategy.UnnamedType.Default(targetType.getSuperType(), interfaceTypes, modifiers, classFileVersion)),
                        modifiers,
                        targetType.getTypeVariables().accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(is(targetType))),
                        targetType.getSuperType(),
                        interfaceTypes,
                        fieldTokens,
                        methodTokens,
                        targetType.getDeclaredAnnotations(),
                        InstrumentedType.TypeInitializer.None.INSTANCE,
                        LoadedTypeInitializer.NoOp.INSTANCE,
                        targetType.getDeclaringType(),
                        targetType.getEnclosingMethod(),
                        targetType.getEnclosingType(),
                        targetType.isMemberClass(),
                        targetType.isAnonymousClass(),
                        targetType.isLocalClass()),
                methodGraphCompiler,
                InliningImplementationMatcher.of(ignoredMethods, targetType))
                .compile(new SubclassImplementationTarget.Factory(SubclassImplementationTarget.OriginTypeResolver.LEVEL_TYPE));
        return TypeWriter.Default.<T>forRedefinition(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
                auxiliaryTypeNamingStrategy,
                classVisitorWrapperChain,
                attributeAppender,
                classFileVersion,
                classFileLocator,
                targetType).make();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && classFileLocator.equals(((RedefinitionDynamicTypeBuilder<?>) other).classFileLocator);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + classFileLocator.hashCode();
    }

    @Override
    public String toString() {
        return "RedefinitionDynamicTypeBuilder{" +
                "classFileVersion=" + classFileVersion +
                ", namingStrategy=" + namingStrategy +
                ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                ", targetType=" + targetType +
                ", interfaceTypes=" + interfaceTypes +
                ", modifiers=" + modifiers +
                ", attributeAppender=" + attributeAppender +
                ", ignoredMethods=" + ignoredMethods +
                ", classVisitorWrapperChain=" + classVisitorWrapperChain +
                ", fieldRegistry=" + fieldRegistry +
                ", methodRegistry=" + methodRegistry +
                ", methodGraphCompiler=" + methodGraphCompiler +
                ", defaultFieldAttributeAppenderFactory=" + defaultFieldAttributeAppenderFactory +
                ", defaultMethodAttributeAppenderFactory=" + defaultMethodAttributeAppenderFactory +
                ", fieldTokens=" + fieldTokens +
                ", methodTokens=" + methodTokens +
                ", classFileLocator=" + classFileLocator +
                '}';
    }
}

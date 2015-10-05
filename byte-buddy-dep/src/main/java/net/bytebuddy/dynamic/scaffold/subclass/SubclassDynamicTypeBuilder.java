package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMethodMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Creates a dynamic type on basis of loaded types where the dynamic type extends a given type.
 *
 * @param <T> The best known loaded type representing the built dynamic type.
 */
public class SubclassDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    /**
     * A strategy that is used to define and implement constructors based on the subclassed type.
     */
    private final ConstructorStrategy constructorStrategy;

    /**
     * Creates a new immutable type builder for a subclassing a given class.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param auxiliaryTypeNamingStrategy           The naming strategy to apply to auxiliary types.
     * @param superType                             The super class that the dynamic type should extend.
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
     * @param constructorStrategy                   The strategy for creating constructors during the final definition
     *                                              phase of this dynamic type.
     */
    public SubclassDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                      NamingStrategy namingStrategy,
                                      AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                      TypeDescription superType,
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
                                      ConstructorStrategy constructorStrategy) {
        this(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                superType,
                new ArrayList<GenericTypeDescription>(interfaceTypes),
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                fieldRegistry,
                methodRegistry,
                methodGraphCompiler,
                defaultFieldAttributeAppenderFactory,
                defaultMethodAttributeAppenderFactory,
                Collections.<FieldDescription.Token>emptyList(),
                Collections.<MethodDescription.Token>emptyList(),
                constructorStrategy);
    }

    /**
     * Creates a new immutable type builder for a subclassing a given class.
     *
     * @param classFileVersion                      The class file version for the created dynamic type.
     * @param namingStrategy                        The naming strategy for naming the dynamic type.
     * @param auxiliaryTypeNamingStrategy           The naming strategy to apply to auxiliary types.
     * @param superType                             The super class that the dynamic type should extend.
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
     * @param constructorStrategy                   The strategy for creating constructors during the final definition
     *                                              phase of this dynamic type.
     */
    protected SubclassDynamicTypeBuilder(ClassFileVersion classFileVersion,
                                         NamingStrategy namingStrategy,
                                         AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                         TypeDescription superType,
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
                                         ConstructorStrategy constructorStrategy) {
        super(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                superType,
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
        this.constructorStrategy = constructorStrategy;
    }

    @Override
    protected DynamicType.Builder<T> materialize(ClassFileVersion classFileVersion,
                                                 NamingStrategy namingStrategy,
                                                 AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                 TypeDescription targetType,
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
        return new SubclassDynamicTypeBuilder<T>(classFileVersion,
                namingStrategy,
                auxiliaryTypeNamingStrategy,
                targetType,
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
                constructorStrategy);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = constructorStrategy
                .inject(methodRegistry, defaultMethodAttributeAppenderFactory)
                .prepare(applyConstructorStrategy(new InstrumentedType.Default(namingStrategy.name(new NamingStrategy.UnnamedType.Default(targetType,
                                interfaceTypes,
                                modifiers,
                                classFileVersion)),
                                modifiers,
                                Collections.<GenericTypeDescription>emptyList(),
                                targetType,
                                interfaceTypes,
                                fieldTokens,
                                methodTokens,
                                Collections.<AnnotationDescription>emptyList(),
                                InstrumentedType.TypeInitializer.None.INSTANCE,
                                LoadedTypeInitializer.NoOp.INSTANCE)),
                        methodGraphCompiler,
                        new InstrumentableMatcher(ignoredMethods))
                .compile(new SubclassImplementationTarget.Factory(SubclassImplementationTarget.OriginTypeResolver.SUPER_TYPE));
        return TypeWriter.Default.<T>forCreation(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
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
        if (!instrumentedType.isClassType()) {
            return instrumentedType;
        }
        for (MethodDescription.Token methodToken : constructorStrategy.extractConstructors(instrumentedType)) {
            instrumentedType = instrumentedType.withMethod(methodToken);
        }
        return instrumentedType;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && constructorStrategy.equals(((SubclassDynamicTypeBuilder<?>) other).constructorStrategy);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + constructorStrategy.hashCode();
    }

    @Override
    public String toString() {
        return "SubclassDynamicTypeBuilder{" +
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
                ", constructorStrategy=" + constructorStrategy +
                '}';
    }

    /**
     * A matcher that locates all methods that are overridable and not ignored or that are directly defined on the instrumented type.
     */
    protected static class InstrumentableMatcher implements LatentMethodMatcher {

        /**
         * A matcher for the ignored methods.
         */
        private final ElementMatcher<? super MethodDescription> ignoredMethods;

        /**
         * Creates a latent method matcher that matches all methods that are to be instrumented by a {@link SubclassDynamicTypeBuilder}.
         *
         * @param ignoredMethods A matcher for the ignored methods.
         */
        protected InstrumentableMatcher(ElementMatcher<? super MethodDescription> ignoredMethods) {
            this.ignoredMethods = ignoredMethods;
        }

        @Override
        public ElementMatcher<? super MethodDescription> resolve(TypeDescription instrumentedType) {
            // Casting is required by JDK 6.
            return (ElementMatcher<? super MethodDescription>) isVirtual().and(not(isFinal()))
                    .and(isVisibleTo(instrumentedType))
                    .and(not(ignoredMethods))
                    .or(isDeclaredBy(instrumentedType));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && ignoredMethods.equals(((InstrumentableMatcher) other).ignoredMethods);
        }

        @Override
        public int hashCode() {
            return ignoredMethods.hashCode();
        }

        @Override
        public String toString() {
            return "SubclassDynamicTypeBuilder.InstrumentableMatcher{" +
                    "ignoredMethods=" + ignoredMethods +
                    '}';
        }
    }
}

/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.scaffold.ClassWriterStrategy;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * A type builder that decorates a type by allowing for the application of attribute changes and ASM visitor wrappers.
 *
 * @param <T> A loaded type that the built type is guaranteed to be a subclass of.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DecoratingDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    /**
     * The instrumented type to decorate.
     */
    private final TypeDescription instrumentedType;

    /**
     * The type attribute appender to apply onto the instrumented type.
     */
    private final TypeAttributeAppender typeAttributeAppender;

    /**
     * The ASM visitor wrapper to apply onto the class writer.
     */
    private final AsmVisitorWrapper asmVisitorWrapper;

    /**
     * The class file version to define auxiliary types in.
     */
    private final ClassFileVersion classFileVersion;

    /**
     * The naming strategy for auxiliary types to apply.
     */
    private final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    /**
     * The annotation value filter factory to apply.
     */
    private final AnnotationValueFilter.Factory annotationValueFilterFactory;

    /**
     * The annotation retention to apply.
     */
    private final AnnotationRetention annotationRetention;

    /**
     * The implementation context factory to apply.
     */
    private final Implementation.Context.Factory implementationContextFactory;

    /**
     * The method graph compiler to use.
     */
    private final MethodGraph.Compiler methodGraphCompiler;

    /**
     * Determines if a type should be explicitly validated.
     */
    private final TypeValidation typeValidation;

    /**
     * The class writer strategy to use.
     */
    private final ClassWriterStrategy classWriterStrategy;

    /**
     * A matcher for identifying methods that should be excluded from instrumentation.
     */
    private final LatentMatcher<? super MethodDescription> ignoredMethods;

    /**
     * A list of explicitly required auxiliary types.
     */
    private final List<DynamicType> auxiliaryTypes;

    /**
     * The class file locator for locating the original type's class file.
     */
    private final ClassFileLocator classFileLocator;

    /**
     * Creates a new decorating dynamic type builder.
     *
     * @param instrumentedType             The instrumented type to decorate.
     * @param classFileVersion             The class file version to define auxiliary types in.
     * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
     * @param annotationValueFilterFactory The annotation value filter factory to apply.
     * @param annotationRetention          The annotation retention to apply.
     * @param implementationContextFactory The implementation context factory to apply.
     * @param methodGraphCompiler          The method graph compiler to use.
     * @param typeValidation               Determines if a type should be explicitly validated.
     * @param classWriterStrategy          The class writer strategy to use.
     * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
     * @param classFileLocator             The class file locator for locating the original type's class file.
     */
    public DecoratingDynamicTypeBuilder(TypeDescription instrumentedType,
                                        ClassFileVersion classFileVersion,
                                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                        AnnotationValueFilter.Factory annotationValueFilterFactory,
                                        AnnotationRetention annotationRetention,
                                        Implementation.Context.Factory implementationContextFactory,
                                        MethodGraph.Compiler methodGraphCompiler,
                                        TypeValidation typeValidation,
                                        ClassWriterStrategy classWriterStrategy,
                                        LatentMatcher<? super MethodDescription> ignoredMethods,
                                        ClassFileLocator classFileLocator) {
        this(instrumentedType,
                annotationRetention.isEnabled()
                        ? new TypeAttributeAppender.ForInstrumentedType.Differentiating(instrumentedType)
                        : TypeAttributeAppender.ForInstrumentedType.INSTANCE,
                AsmVisitorWrapper.NoOp.INSTANCE,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                ignoredMethods,
                Collections.<DynamicType>emptyList(),
                classFileLocator);
    }

    /**
     * Creates a new decorating dynamic type builder.
     *
     * @param instrumentedType             The instrumented type to decorate.
     * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
     * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
     * @param classFileVersion             The class file version to define auxiliary types in.
     * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
     * @param annotationValueFilterFactory The annotation value filter factory to apply.
     * @param annotationRetention          The annotation retention to apply.
     * @param implementationContextFactory The implementation context factory to apply.
     * @param methodGraphCompiler          The method graph compiler to use.
     * @param typeValidation               Determines if a type should be explicitly validated.
     * @param classWriterStrategy          The class writer strategy to use.
     * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
     * @param auxiliaryTypes               A list of explicitly required auxiliary types.
     * @param classFileLocator             The class file locator for locating the original type's class file.
     */
    protected DecoratingDynamicTypeBuilder(TypeDescription instrumentedType,
                                           TypeAttributeAppender typeAttributeAppender,
                                           AsmVisitorWrapper asmVisitorWrapper,
                                           ClassFileVersion classFileVersion,
                                           AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                           AnnotationValueFilter.Factory annotationValueFilterFactory,
                                           AnnotationRetention annotationRetention,
                                           Implementation.Context.Factory implementationContextFactory,
                                           MethodGraph.Compiler methodGraphCompiler,
                                           TypeValidation typeValidation,
                                           ClassWriterStrategy classWriterStrategy,
                                           LatentMatcher<? super MethodDescription> ignoredMethods,
                                           List<DynamicType> auxiliaryTypes,
                                           ClassFileLocator classFileLocator) {
        this.instrumentedType = instrumentedType;
        this.typeAttributeAppender = typeAttributeAppender;
        this.asmVisitorWrapper = asmVisitorWrapper;
        this.classFileVersion = classFileVersion;
        this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
        this.annotationValueFilterFactory = annotationValueFilterFactory;
        this.annotationRetention = annotationRetention;
        this.implementationContextFactory = implementationContextFactory;
        this.methodGraphCompiler = methodGraphCompiler;
        this.typeValidation = typeValidation;
        this.classWriterStrategy = classWriterStrategy;
        this.ignoredMethods = ignoredMethods;
        this.auxiliaryTypes = auxiliaryTypes;
        this.classFileLocator = classFileLocator;
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> visit(AsmVisitorWrapper asmVisitorWrapper) {
        return new DecoratingDynamicTypeBuilder<T>(instrumentedType,
                typeAttributeAppender,
                new AsmVisitorWrapper.Compound(this.asmVisitorWrapper, asmVisitorWrapper),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                ignoredMethods,
                auxiliaryTypes,
                classFileLocator);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> name(String name) {
        throw new UnsupportedOperationException("Cannot change name of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> modifiers(int modifiers) {
        throw new UnsupportedOperationException("Cannot change modifiers of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> merge(Collection<? extends ModifierContributor.ForType> modifierContributors) {
        throw new UnsupportedOperationException("Cannot change modifiers of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> topLevelType() {
        throw new UnsupportedOperationException("Cannot change type declaration of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public InnerTypeDefinition.ForType<T> innerTypeOf(TypeDescription type) {
        throw new UnsupportedOperationException("Cannot change type declaration of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public InnerTypeDefinition<T> innerTypeOf(MethodDescription.InDefinedShape methodDescription) {
        throw new UnsupportedOperationException("Cannot change type declaration of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> declaredTypes(Collection<? extends TypeDescription> types) {
        throw new UnsupportedOperationException("Cannot change type declaration of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> nestHost(TypeDescription type) {
        throw new UnsupportedOperationException("Cannot change type declaration of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> nestMembers(Collection<? extends TypeDescription> types) {
        throw new UnsupportedOperationException("Cannot change type declaration of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> attribute(TypeAttributeAppender typeAttributeAppender) {
        return new DecoratingDynamicTypeBuilder<T>(instrumentedType,
                new TypeAttributeAppender.Compound(this.typeAttributeAppender, typeAttributeAppender),
                asmVisitorWrapper,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                ignoredMethods,
                auxiliaryTypes,
                classFileLocator);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> annotateType(Collection<? extends AnnotationDescription> annotations) {
        return attribute(new TypeAttributeAppender.Explicit(new ArrayList<AnnotationDescription>(annotations)));
    }

    /**
     * {@inheritDoc}
     */
    public MethodDefinition.ImplementationDefinition.Optional<T> implement(Collection<? extends TypeDefinition> interfaceTypes) {
        throw new UnsupportedOperationException("Cannot implement interface for decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> initializer(ByteCodeAppender byteCodeAppender) {
        throw new UnsupportedOperationException("Cannot add initializer of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> initializer(LoadedTypeInitializer loadedTypeInitializer) {
        throw new UnsupportedOperationException("Cannot add initializer of decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public TypeVariableDefinition<T> typeVariable(String symbol, Collection<? extends TypeDefinition> bounds) {
        throw new UnsupportedOperationException("Cannot add type variable to decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> transform(ElementMatcher<? super TypeDescription.Generic> matcher, Transformer<TypeVariableToken> transformer) {
        throw new UnsupportedOperationException("Cannot transform decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public FieldDefinition.Optional.Valuable<T> defineField(String name, TypeDefinition type, int modifiers) {
        throw new UnsupportedOperationException("Cannot define field for decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public FieldDefinition.Valuable<T> field(LatentMatcher<? super FieldDescription> matcher) {
        throw new UnsupportedOperationException("Cannot change field for decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public DynamicType.Builder<T> ignoreAlso(LatentMatcher<? super MethodDescription> ignoredMethods) {
        return new DecoratingDynamicTypeBuilder<T>(instrumentedType,
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                new LatentMatcher.Disjunction<MethodDescription>(this.ignoredMethods, ignoredMethods),
                auxiliaryTypes,
                classFileLocator);
    }

    /**
     * {@inheritDoc}
     */
    public MethodDefinition.ParameterDefinition.Initial<T> defineMethod(String name, TypeDefinition returnType, int modifiers) {
        throw new UnsupportedOperationException("Cannot define method for decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public MethodDefinition.ParameterDefinition.Initial<T> defineConstructor(int modifiers) {
        throw new UnsupportedOperationException("Cannot define constructor for decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public MethodDefinition.ImplementationDefinition<T> invokable(LatentMatcher<? super MethodDescription> matcher) {
        throw new UnsupportedOperationException("Cannot intercept method for decorated type: " + instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<T> require(Collection<DynamicType> auxiliaryTypes) {
        return new DecoratingDynamicTypeBuilder<T>(instrumentedType,
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                classWriterStrategy,
                ignoredMethods,
                CompoundList.of(this.auxiliaryTypes, new ArrayList<DynamicType>(auxiliaryTypes)),
                classFileLocator);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Unloaded<T> make(TypeResolutionStrategy typeResolutionStrategy) {
        return make(typeResolutionStrategy, TypePool.Empty.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Unloaded<T> make(TypeResolutionStrategy typeResolutionStrategy, TypePool typePool) {
        return TypeWriter.Default.<T>forDecoration(instrumentedType,
                classFileVersion,
                auxiliaryTypes,
                CompoundList.of(methodGraphCompiler.compile(instrumentedType)
                        .listNodes()
                        .asMethodList()
                        .filter(not(ignoredMethods.resolve(instrumentedType))), instrumentedType.getDeclaredMethods().filter(not(isVirtual()))),
                typeAttributeAppender,
                asmVisitorWrapper,
                annotationValueFilterFactory,
                annotationRetention,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                typeValidation,
                classWriterStrategy,
                typePool,
                classFileLocator).make(typeResolutionStrategy.resolve());
    }
}

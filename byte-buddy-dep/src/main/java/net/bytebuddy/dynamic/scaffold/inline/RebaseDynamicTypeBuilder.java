/*
 * Copyright 2014 - 2020 Rafael Winterhalter
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
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.VisibilityBridgeStrategy;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.pool.TypePool;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * A type builder that rebases an instrumented type.
 *
 * @param <T> A loaded type that the dynamic type is guaranteed to be a subtype of.
 */
@HashCodeAndEqualsPlugin.Enhance
public class RebaseDynamicTypeBuilder<T> extends AbstractInliningDynamicTypeBuilder<T> {

    /**
     * The method rebase resolver to use for determining the name of a rebased method.
     */
    private final MethodNameTransformer methodNameTransformer;

    /**
     * Creates a rebase dynamic type builder.
     *
     * @param instrumentedType             An instrumented type representing the subclass.
     * @param classFileVersion             The class file version to use for types that are not based on an existing class file.
     * @param auxiliaryTypeNamingStrategy  The naming strategy to use for naming auxiliary types.
     * @param annotationValueFilterFactory The annotation value filter factory to use.
     * @param annotationRetention          The annotation retention strategy to use.
     * @param implementationContextFactory The implementation context factory to use.
     * @param methodGraphCompiler          The method graph compiler to use.
     * @param typeValidation               Determines if a type should be explicitly validated.
     * @param visibilityBridgeStrategy     The visibility bridge strategy to apply.
     * @param classWriterStrategy          The class writer strategy to use.
     * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
     * @param originalType                 The original type that is being redefined or rebased.
     * @param classFileLocator             The class file locator for locating the original type's class file.
     * @param methodNameTransformer        The method rebase resolver to use for determining the name of a rebased method.
     */
    public RebaseDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                    ClassFileVersion classFileVersion,
                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                    AnnotationValueFilter.Factory annotationValueFilterFactory,
                                    AnnotationRetention annotationRetention,
                                    Implementation.Context.Factory implementationContextFactory,
                                    MethodGraph.Compiler methodGraphCompiler,
                                    TypeValidation typeValidation,
                                    VisibilityBridgeStrategy visibilityBridgeStrategy,
                                    ClassWriterStrategy classWriterStrategy,
                                    LatentMatcher<? super MethodDescription> ignoredMethods,
                                    TypeDescription originalType,
                                    ClassFileLocator classFileLocator,
                                    MethodNameTransformer methodNameTransformer) {
        this(instrumentedType,
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                new RecordComponentRegistry.Default(),
                annotationRetention.isEnabled()
                        ? new TypeAttributeAppender.ForInstrumentedType.Differentiating(originalType)
                        : TypeAttributeAppender.ForInstrumentedType.INSTANCE,
                AsmVisitorWrapper.NoOp.INSTANCE,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                visibilityBridgeStrategy,
                classWriterStrategy,
                ignoredMethods,
                Collections.<DynamicType>emptyList(),
                originalType,
                classFileLocator,
                methodNameTransformer);
    }

    /**
     * Creates a rebase dynamic type builder.
     *
     * @param instrumentedType             An instrumented type representing the subclass.
     * @param fieldRegistry                The field pool to use.
     * @param methodRegistry               The method pool to use.
     * @param recordComponentRegistry      The record component pool to use.
     * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
     * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
     * @param classFileVersion             The class file version to use for types that are not based on an existing class file.
     * @param auxiliaryTypeNamingStrategy  The naming strategy to use for naming auxiliary types.
     * @param annotationValueFilterFactory The annotation value filter factory to use.
     * @param annotationRetention          The annotation retention strategy to use.
     * @param implementationContextFactory The implementation context factory to use.
     * @param methodGraphCompiler          The method graph compiler to use.
     * @param typeValidation               Determines if a type should be explicitly validated.
     * @param visibilityBridgeStrategy     The visibility bridge strategy to apply.
     * @param classWriterStrategy          The class writer strategy to use.
     * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
     * @param auxiliaryTypes               A list of explicitly required auxiliary types.
     * @param originalType                 The original type that is being redefined or rebased.
     * @param classFileLocator             The class file locator for locating the original type's class file.
     * @param methodNameTransformer        The method rebase resolver to use for determining the name of a rebased method.
     */
    protected RebaseDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                       FieldRegistry fieldRegistry,
                                       MethodRegistry methodRegistry,
                                       RecordComponentRegistry recordComponentRegistry,
                                       TypeAttributeAppender typeAttributeAppender,
                                       AsmVisitorWrapper asmVisitorWrapper,
                                       ClassFileVersion classFileVersion,
                                       AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                       AnnotationValueFilter.Factory annotationValueFilterFactory,
                                       AnnotationRetention annotationRetention,
                                       Implementation.Context.Factory implementationContextFactory,
                                       MethodGraph.Compiler methodGraphCompiler,
                                       TypeValidation typeValidation,
                                       VisibilityBridgeStrategy visibilityBridgeStrategy,
                                       ClassWriterStrategy classWriterStrategy,
                                       LatentMatcher<? super MethodDescription> ignoredMethods,
                                       List<? extends DynamicType> auxiliaryTypes,
                                       TypeDescription originalType,
                                       ClassFileLocator classFileLocator,
                                       MethodNameTransformer methodNameTransformer) {
        super(instrumentedType,
                fieldRegistry,
                methodRegistry,
                recordComponentRegistry,
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                visibilityBridgeStrategy,
                classWriterStrategy,
                ignoredMethods,
                auxiliaryTypes,
                originalType,
                classFileLocator);
        this.methodNameTransformer = methodNameTransformer;
    }

    @Override
    protected DynamicType.Builder<T> materialize(InstrumentedType.WithFlexibleName instrumentedType,
                                                 FieldRegistry fieldRegistry,
                                                 MethodRegistry methodRegistry,
                                                 RecordComponentRegistry recordComponentRegistry,
                                                 TypeAttributeAppender typeAttributeAppender,
                                                 AsmVisitorWrapper asmVisitorWrapper,
                                                 ClassFileVersion classFileVersion,
                                                 AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                 AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                 AnnotationRetention annotationRetention,
                                                 Implementation.Context.Factory implementationContextFactory,
                                                 MethodGraph.Compiler methodGraphCompiler,
                                                 TypeValidation typeValidation,
                                                 VisibilityBridgeStrategy visibilityBridgeStrategy,
                                                 ClassWriterStrategy classWriterStrategy,
                                                 LatentMatcher<? super MethodDescription> ignoredMethods,
                                                 List<? extends DynamicType> auxiliaryTypes) {
        return new RebaseDynamicTypeBuilder<T>(instrumentedType,
                fieldRegistry,
                methodRegistry,
                recordComponentRegistry,
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                visibilityBridgeStrategy,
                classWriterStrategy,
                ignoredMethods,
                auxiliaryTypes,
                originalType,
                classFileLocator,
                methodNameTransformer);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Unloaded<T> make(TypeResolutionStrategy typeResolutionStrategy, TypePool typePool) {
        MethodRegistry.Prepared methodRegistry = this.methodRegistry.prepare(instrumentedType,
                methodGraphCompiler,
                typeValidation,
                visibilityBridgeStrategy,
                InliningImplementationMatcher.of(ignoredMethods, originalType));
        HashSet<MethodDescription.SignatureToken> rebaseables = new HashSet<MethodDescription.SignatureToken>(
                originalType.getDeclaredMethods().asSignatureTokenList(is(originalType), instrumentedType));
        rebaseables.retainAll(methodRegistry.getInstrumentedMethods().asSignatureTokenList());
        MethodRebaseResolver methodRebaseResolver = MethodRebaseResolver.Default.make(methodRegistry.getInstrumentedType(),
                rebaseables,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                methodNameTransformer);
        return TypeWriter.Default.<T>forRebasing(methodRegistry,
                auxiliaryTypes,
                fieldRegistry.compile(methodRegistry.getInstrumentedType()),
                recordComponentRegistry.compile(methodRegistry.getInstrumentedType()),
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                annotationValueFilterFactory,
                annotationRetention,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                typeValidation,
                classWriterStrategy,
                typePool,
                originalType,
                classFileLocator,
                methodRebaseResolver).make(typeResolutionStrategy.resolve());
    }
}

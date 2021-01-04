/*
 * Copyright 2014 - Present Rafael Winterhalter
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

import java.util.List;

/**
 * An abstract base implementation of a dynamic type builder that alters an existing type.
 *
 * @param <T> A loaded type that the dynamic type is guaranteed to be a subtype of.
 */
@HashCodeAndEqualsPlugin.Enhance
public abstract class AbstractInliningDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase.Adapter<T> {

    /**
     * The original type that is being redefined or rebased.
     */
    protected final TypeDescription originalType;

    /**
     * The class file locator for locating the original type's class file.
     */
    protected final ClassFileLocator classFileLocator;

    /**
     * Creates an inlining dynamic type builder.
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
     * @param auxiliaryTypes               A list of explicitly defined auxiliary types.
     * @param originalType                 The original type that is being redefined or rebased.
     * @param classFileLocator             The class file locator for locating the original type's class file.
     */
    protected AbstractInliningDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
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
                                                 ClassFileLocator classFileLocator) {
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
                auxiliaryTypes);
        this.originalType = originalType;
        this.classFileLocator = classFileLocator;
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Unloaded<T> make(TypeResolutionStrategy typeResolutionStrategy) {
        return make(typeResolutionStrategy, TypePool.Default.of(classFileLocator));
    }
}

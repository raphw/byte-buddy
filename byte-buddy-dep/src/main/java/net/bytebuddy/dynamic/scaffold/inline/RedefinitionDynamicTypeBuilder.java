package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.pool.TypePool;

/**
 * A type builder that redefines an instrumented type.
 *
 * @param <T> A loaded type that the dynamic type is guaranteed to be a subtype of.
 */
public class RedefinitionDynamicTypeBuilder<T> extends AbstractInliningDynamicTypeBuilder<T> {

    /**
     * Creates a redefinition dynamic type builder.
     *
     * @param instrumentedType             An instrumented type representing the subclass.
     * @param classFileVersion             The class file version to use for types that are not based on an existing class file.
     * @param auxiliaryTypeNamingStrategy  The naming strategy to use for naming auxiliary types.
     * @param annotationValueFilterFactory The annotation value filter factory to use.
     * @param annotationRetention          The annotation retention strategy to use.
     * @param implementationContextFactory The implementation context factory to use.
     * @param typeValidation               Determines if a type should be explicitly validated.
     * @param methodGraphCompiler          The method graph compiler to use.
     * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
     * @param originalType                 The original type that is being redefined or rebased.
     * @param classFileLocator             The class file locator for locating the original type's class file.
     */
    public RedefinitionDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                          ClassFileVersion classFileVersion,
                                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                          AnnotationValueFilter.Factory annotationValueFilterFactory,
                                          AnnotationRetention annotationRetention,
                                          Implementation.Context.Factory implementationContextFactory,
                                          MethodGraph.Compiler methodGraphCompiler,
                                          TypeValidation typeValidation,
                                          LatentMatcher<? super MethodDescription> ignoredMethods,
                                          TypeDescription originalType,
                                          ClassFileLocator classFileLocator) {
        this(instrumentedType,
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
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
                ignoredMethods,
                originalType,
                classFileLocator);
    }

    /**
     * Creates a redefinition dynamic type builder.
     *
     * @param instrumentedType             An instrumented type representing the subclass.
     * @param fieldRegistry                The field pool to use.
     * @param methodRegistry               The method pool to use.
     * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
     * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
     * @param classFileVersion             The class file version to use for types that are not based on an existing class file.
     * @param auxiliaryTypeNamingStrategy  The naming strategy to use for naming auxiliary types.
     * @param annotationValueFilterFactory The annotation value filter factory to use.
     * @param annotationRetention          The annotation retention strategy to use.
     * @param implementationContextFactory The implementation context factory to use.
     * @param methodGraphCompiler          The method graph compiler to use.
     * @param typeValidation               Determines if a type should be explicitly validated.
     * @param ignoredMethods               A matcher for identifying methods that should be excluded from instrumentation.
     * @param originalType                 The original type that is being redefined or rebased.
     * @param classFileLocator             The class file locator for locating the original type's class file.
     */
    protected RedefinitionDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                             FieldRegistry fieldRegistry,
                                             MethodRegistry methodRegistry,
                                             TypeAttributeAppender typeAttributeAppender,
                                             AsmVisitorWrapper asmVisitorWrapper,
                                             ClassFileVersion classFileVersion,
                                             AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                             AnnotationValueFilter.Factory annotationValueFilterFactory,
                                             AnnotationRetention annotationRetention,
                                             Implementation.Context.Factory implementationContextFactory,
                                             MethodGraph.Compiler methodGraphCompiler,
                                             TypeValidation typeValidation,
                                             LatentMatcher<? super MethodDescription> ignoredMethods,
                                             TypeDescription originalType,
                                             ClassFileLocator classFileLocator) {
        super(instrumentedType,
                fieldRegistry,
                methodRegistry,
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                originalType,
                classFileLocator);
    }

    @Override
    protected DynamicType.Builder<T> materialize(InstrumentedType.WithFlexibleName instrumentedType,
                                                 FieldRegistry fieldRegistry,
                                                 MethodRegistry methodRegistry,
                                                 TypeAttributeAppender typeAttributeAppender,
                                                 AsmVisitorWrapper asmVisitorWrapper,
                                                 ClassFileVersion classFileVersion,
                                                 AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                 AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                 AnnotationRetention annotationRetention,
                                                 Implementation.Context.Factory implementationContextFactory,
                                                 MethodGraph.Compiler methodGraphCompiler,
                                                 TypeValidation typeValidation,
                                                 LatentMatcher<? super MethodDescription> ignoredMethods) {
        return new RedefinitionDynamicTypeBuilder<T>(instrumentedType,
                fieldRegistry,
                methodRegistry,
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                originalType,
                classFileLocator);
    }

    @Override
    public DynamicType.Unloaded<T> make(TypePool typePool) {
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.prepare(instrumentedType,
                methodGraphCompiler,
                typeValidation,
                InliningImplementationMatcher.of(ignoredMethods, originalType)).compile(SubclassImplementationTarget.Factory.LEVEL_TYPE);
        return TypeWriter.Default.<T>forRedefinition(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                annotationValueFilterFactory,
                annotationRetention,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                typeValidation,
                typePool,
                originalType,
                classFileLocator).make();
    }

    @Override
    public String toString() {
        return "RedefinitionDynamicTypeBuilder{" +
                "instrumentedType=" + instrumentedType +
                ", fieldRegistry=" + fieldRegistry +
                ", methodRegistry=" + methodRegistry +
                ", typeAttributeAppender=" + typeAttributeAppender +
                ", asmVisitorWrapper=" + asmVisitorWrapper +
                ", classFileVersion=" + classFileVersion +
                ", annotationValueFilterFactory=" + annotationValueFilterFactory +
                ", annotationRetention=" + annotationRetention +
                ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                ", implementationContextFactory=" + implementationContextFactory +
                ", methodGraphCompiler=" + methodGraphCompiler +
                ", typeValidation=" + typeValidation +
                ", ignoredMethods=" + ignoredMethods +
                ", originalType=" + originalType +
                ", classFileLocator=" + classFileLocator +
                '}';
    }
}

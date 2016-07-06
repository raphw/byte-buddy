package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.pool.TypePool;

/**
 * An abstract base implementation of a dynamic type builder that alters an existing type.
 *
 * @param <T> A loaded type that the dynamic type is guaranteed to be a subtype of.
 */
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
    protected AbstractInliningDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
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
                ignoredMethods);
        this.originalType = originalType;
        this.classFileLocator = classFileLocator;
    }

    @Override
    public DynamicType.Unloaded<T> make(TypeResolutionStrategy typeResolutionStrategy) {
        return make(typeResolutionStrategy, TypePool.Default.of(classFileLocator));
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        if (!super.equals(other)) return false;
        AbstractInliningDynamicTypeBuilder<?> that = (AbstractInliningDynamicTypeBuilder<?>) other;
        return originalType.equals(that.originalType)
                && classFileLocator.equals(that.classFileLocator);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + originalType.hashCode();
        result = 31 * result + classFileLocator.hashCode();
        return result;
    }
}

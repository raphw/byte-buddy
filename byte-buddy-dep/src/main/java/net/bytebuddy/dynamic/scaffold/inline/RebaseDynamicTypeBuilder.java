package net.bytebuddy.dynamic.scaffold.inline;

import com.google.auto.value.AutoValue;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
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
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.pool.TypePool;

import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * A type builder that rebases an instrumented type.
 *
 * @param <T> A loaded type that the dynamic type is guaranteed to be a subtype of.
 */
@AutoValue
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
                                    LatentMatcher<? super MethodDescription> ignoredMethods,
                                    TypeDescription originalType,
                                    ClassFileLocator classFileLocator,
                                    MethodNameTransformer methodNameTransformer) {
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
                classFileLocator,
                methodNameTransformer);
    }

    /**
     * Creates a rebase dynamic type builder.
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
     * @param methodNameTransformer        The method rebase resolver to use for determining the name of a rebased method.
     */
    protected RebaseDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
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
                                       ClassFileLocator classFileLocator,
                                       MethodNameTransformer methodNameTransformer) {
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
        this.methodNameTransformer = methodNameTransformer;
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
        return new RebaseDynamicTypeBuilder<T>(instrumentedType,
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
                classFileLocator,
                methodNameTransformer);
    }

    @Override
    public DynamicType.Unloaded<T> make(TypeResolutionStrategy typeResolutionStrategy, TypePool typePool) {
        MethodRegistry.Prepared methodRegistry = this.methodRegistry.prepare(instrumentedType,
                methodGraphCompiler,
                typeValidation,
                InliningImplementationMatcher.of(ignoredMethods, originalType));
        MethodRebaseResolver methodRebaseResolver = MethodRebaseResolver.Default.make(methodRegistry.getInstrumentedType(),
                new HashSet<MethodDescription.Token>(originalType.getDeclaredMethods()
                        .asTokenList(is(originalType))
                        .filter(RebaseableMatcher.of(methodRegistry.getInstrumentedType(), methodRegistry.getInstrumentedMethods()))),
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                methodNameTransformer);
        return TypeWriter.Default.<T>forRebasing(methodRegistry,
                fieldRegistry.compile(methodRegistry.getInstrumentedType()),
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
                classFileLocator,
                methodRebaseResolver).make(typeResolutionStrategy.resolve());
    }

    /**
     * A matcher that filters any method that should not be rebased, i.e. that is not already defined by the original type.
     */
    @AutoValue
    protected static class RebaseableMatcher implements ElementMatcher<MethodDescription.Token> {

        /**
         * A set of method tokens representing all instrumented methods.
         */
        private final Set<MethodDescription.Token> tokens;

        /**
         * Creates a new matcher for identifying rebasable methods.
         *
         * @param tokens A set of method tokens representing all instrumented methods.
         */
        protected RebaseableMatcher(Set<MethodDescription.Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns a matcher that filters any method that should not be rebased.
         *
         * @param instrumentedType    The instrumented type.
         * @param instrumentedMethods All instrumented methods.
         * @return A suitable matcher that filters all methods that should not be rebased.
         */
        protected static ElementMatcher<MethodDescription.Token> of(TypeDescription instrumentedType, MethodList<?> instrumentedMethods) {
            return new RebaseableMatcher(new HashSet<MethodDescription.Token>(instrumentedMethods.asTokenList(is(instrumentedType))));
        }

        @Override
        public boolean matches(MethodDescription.Token target) {
            return tokens.contains(target);
        }
    }
}

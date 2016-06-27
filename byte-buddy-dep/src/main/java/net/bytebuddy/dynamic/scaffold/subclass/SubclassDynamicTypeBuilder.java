package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.pool.TypePool;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A type builder that creates an instrumented type as a subclass, i.e. a type that is not based on an existing class file.
 *
 * @param <T> A loaded type that the dynamic type is guaranteed to be a subtype of.
 */
public class SubclassDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase.Adapter<T> {

    /**
     * The constructor strategy to apply onto the instrumented type.
     */
    private final ConstructorStrategy constructorStrategy;

    /**
     * Creates a new type builder for creating a subclass.
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
     * @param constructorStrategy          The constructor strategy to apply onto the instrumented type.
     */
    public SubclassDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                      ClassFileVersion classFileVersion,
                                      AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                      AnnotationValueFilter.Factory annotationValueFilterFactory,
                                      AnnotationRetention annotationRetention,
                                      Implementation.Context.Factory implementationContextFactory,
                                      MethodGraph.Compiler methodGraphCompiler,
                                      TypeValidation typeValidation,
                                      LatentMatcher<? super MethodDescription> ignoredMethods,
                                      ConstructorStrategy constructorStrategy) {
        this(instrumentedType,
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                TypeAttributeAppender.ForInstrumentedType.INSTANCE,
                AsmVisitorWrapper.NoOp.INSTANCE,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                typeValidation,
                ignoredMethods,
                constructorStrategy);
    }

    /**
     * Creates a new type builder for creating a subclass.
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
     * @param constructorStrategy          The constructor strategy to apply onto the instrumented type.
     */
    protected SubclassDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
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
                                         ConstructorStrategy constructorStrategy) {
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
        this.constructorStrategy = constructorStrategy;
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
        return new SubclassDynamicTypeBuilder<T>(instrumentedType,
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
                constructorStrategy);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        return make(TypePool.ClassLoading.ofClassPath()); // Mimics the default behavior of ASM for least surprise.
    }

    @Override
    public DynamicType.Unloaded<T> make(TypePool typePool) {
        MethodRegistry.Compiled compiledMethodRegistry = constructorStrategy
                .inject(methodRegistry)
                .prepare(applyConstructorStrategy(instrumentedType), methodGraphCompiler, typeValidation, new InstrumentableMatcher(ignoredMethods))
                .compile(SubclassImplementationTarget.Factory.SUPER_CLASS);
        return TypeWriter.Default.<T>forCreation(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
                typeAttributeAppender,
                asmVisitorWrapper,
                classFileVersion,
                annotationValueFilterFactory,
                annotationRetention,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                typeValidation,
                typePool).make();
    }

    /**
     * Applies this builder's constructor strategy to the given instrumented type.
     *
     * @param instrumentedType The instrumented type to apply the constructor onto.
     * @return The instrumented type with the constructor strategy applied onto.
     */
    private InstrumentedType applyConstructorStrategy(InstrumentedType instrumentedType) {
        if (!instrumentedType.isInterface()) {
            for (MethodDescription.Token token : constructorStrategy.extractConstructors(instrumentedType)) {
                instrumentedType = instrumentedType.withMethod(token);
            }
        }
        return instrumentedType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        if (!super.equals(other)) return false;
        SubclassDynamicTypeBuilder<?> that = (SubclassDynamicTypeBuilder<?>) other;
        return constructorStrategy.equals(that.constructorStrategy);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + constructorStrategy.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SubclassDynamicTypeBuilder{" +
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
                ", constructorStrategy=" + constructorStrategy +
                '}';
    }

    /**
     * A matcher that locates all methods that are overridable and not ignored or that are directly defined on the instrumented type.
     */
    protected static class InstrumentableMatcher implements LatentMatcher<MethodDescription> {

        /**
         * A matcher for the ignored methods.
         */
        private final LatentMatcher<? super MethodDescription> ignoredMethods;

        /**
         * Creates a latent method matcher that matches all methods that are to be instrumented by a {@link SubclassDynamicTypeBuilder}.
         *
         * @param ignoredMethods A matcher for the ignored methods.
         */
        protected InstrumentableMatcher(LatentMatcher<? super MethodDescription> ignoredMethods) {
            this.ignoredMethods = ignoredMethods;
        }

        @Override
        public ElementMatcher<? super MethodDescription> resolve(TypeDescription typeDescription) {
            // Casting is required by JDK 6.
            return (ElementMatcher<? super MethodDescription>) isVirtual().and(not(isFinal()))
                    .and(isVisibleTo(typeDescription))
                    .and(not(ignoredMethods.resolve(typeDescription)))
                    .or(isDeclaredBy(typeDescription));
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

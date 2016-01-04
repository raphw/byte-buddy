package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Creates a dynamic type on basis of loaded types where the dynamic type extends a given type.
 *
 * @param <T> The best known loaded type representing the built dynamic type.
 */
public class SubclassDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase.Adapter<T> {

    private final ConstructorStrategy constructorStrategy;

    public SubclassDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                      ElementMatcher<? super MethodDescription> ignored,
                                      AnnotationValueFilter.Factory annotationValueFilterFactory,
                                      ClassFileVersion classFileVersion,
                                      MethodGraph.Compiler methodGraphCompiler,
                                      AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                      Implementation.Context.Factory implementationContextFactory,
                                      ConstructorStrategy constructorStrategy) {
        this(instrumentedType,
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                ignored,
                TypeAttributeAppender.NoOp.INSTANCE,
                AsmVisitorWrapper.NoOp.INSTANCE,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                constructorStrategy);
    }

    protected SubclassDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                         FieldRegistry fieldRegistry,
                                         MethodRegistry methodRegistry,
                                         ElementMatcher<? super MethodDescription> ignored,
                                         TypeAttributeAppender typeAttributeAppender,
                                         AsmVisitorWrapper asmVisitorWrapper,
                                         AnnotationValueFilter.Factory annotationValueFilterFactory,
                                         ClassFileVersion classFileVersion,
                                         MethodGraph.Compiler methodGraphCompiler,
                                         AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                         Implementation.Context.Factory implementationContextFactory,
                                         ConstructorStrategy constructorStrategy) {
        super(instrumentedType,
                fieldRegistry,
                methodRegistry,
                ignored,
                typeAttributeAppender,
                asmVisitorWrapper,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory);
        this.constructorStrategy = constructorStrategy;
    }

    @Override
    protected DynamicType.Builder<T> materialize(InstrumentedType.WithFlexibleName instrumentedType,
                                                 FieldRegistry fieldRegistry,
                                                 MethodRegistry methodRegistry,
                                                 ElementMatcher<? super MethodDescription> ignored,
                                                 TypeAttributeAppender typeAttributeAppender,
                                                 AsmVisitorWrapper asmVisitorWrapper,
                                                 AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                 ClassFileVersion classFileVersion,
                                                 MethodGraph.Compiler methodGraphCompiler,
                                                 AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                 Implementation.Context.Factory implementationContextFactory) {
        return new SubclassDynamicTypeBuilder<T>(instrumentedType,
                fieldRegistry,
                methodRegistry,
                ignored,
                typeAttributeAppender,
                asmVisitorWrapper,
                annotationValueFilterFactory,
                classFileVersion,
                methodGraphCompiler,
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                constructorStrategy);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = constructorStrategy
                .inject(methodRegistry)
                .prepare(applyConstructorStrategy(instrumentedType), methodGraphCompiler, new InstrumentableMatcher(ignored))
                .compile(SubclassImplementationTarget.Factory.SUPER_TYPE);
        return TypeWriter.Default.<T>forCreation(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                asmVisitorWrapper,
                typeAttributeAppender,
                classFileVersion,
                annotationValueFilterFactory).make();
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
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && constructorStrategy.equals(((SubclassDynamicTypeBuilder<?>) other).constructorStrategy);
    }

    /**
     * A matcher that locates all methods that are overridable and not ignored or that are directly defined on the instrumented type.
     */
    protected static class InstrumentableMatcher implements LatentMatcher<MethodDescription> {

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

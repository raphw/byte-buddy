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
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * A dynamic type builder that redefines a given type, i.e. it replaces any redefined method with another implementation.
 *
 * @param <T> The actual type of the rebased type.
 */
public class RedefinitionDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase.Adapter<T> {

    private final TypeDescription originalType;

    private final ClassFileLocator classFileLocator;

    public RedefinitionDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                          ElementMatcher<? super MethodDescription> ignored,
                                          AnnotationValueFilter.Factory annotationValueFilterFactory,
                                          ClassFileVersion classFileVersion,
                                          MethodGraph.Compiler methodGraphCompiler,
                                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                          Implementation.Context.Factory implementationContextFactory,
                                          TypeDescription originalType,
                                          ClassFileLocator classFileLocator) {
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
                originalType,
                classFileLocator);
    }

    protected RedefinitionDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
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
                                             TypeDescription originalType,
                                             ClassFileLocator classFileLocator) {
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
        this.originalType = originalType;
        this.classFileLocator = classFileLocator;
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
        return new RedefinitionDynamicTypeBuilder<T>(instrumentedType,
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
                originalType,
                classFileLocator);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.prepare(instrumentedType,
                methodGraphCompiler,
                InliningImplementationMatcher.of(ignored, originalType)).compile(SubclassImplementationTarget.Factory.LEVEL_TYPE);
        return TypeWriter.Default.<T>forRedefinition(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                asmVisitorWrapper,
                typeAttributeAppender,
                classFileVersion,
                classFileLocator,
                originalType,
                annotationValueFilterFactory).make();
    }
}

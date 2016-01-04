package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.*;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static net.bytebuddy.matcher.ElementMatchers.methodRepresentedBy;

public class RebaseDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase.Adapter<T> {

    private final TypeDescription originalType;

    private final ClassFileLocator classFileLocator;

    private final MethodRebaseResolver.MethodNameTransformer methodNameTransformer;

    public RebaseDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                    ElementMatcher<? super MethodDescription> ignored,
                                    AnnotationValueFilter.Factory annotationValueFilterFactory,
                                    ClassFileVersion classFileVersion,
                                    MethodGraph.Compiler methodGraphCompiler,
                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                    Implementation.Context.Factory implementationContextFactory,
                                    TypeDescription originalType,
                                    ClassFileLocator classFileLocator,
                                    MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
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
                classFileLocator,
                methodNameTransformer);
    }

    protected RebaseDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
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
                                       ClassFileLocator classFileLocator,
                                       MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
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
        this.methodNameTransformer = methodNameTransformer;
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
        return new RebaseDynamicTypeBuilder<T>(instrumentedType,
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
                classFileLocator,
                methodNameTransformer);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Prepared preparedMethodRegistry = methodRegistry.prepare(instrumentedType,
                methodGraphCompiler,
                InliningImplementationMatcher.of(ignored, originalType));
        MethodList<MethodDescription.InDefinedShape> rebaseableMethods = preparedMethodRegistry.getInstrumentedMethods()
                .asDefined()
                .filter(methodRepresentedBy(anyOf(originalType.getDeclaredMethods().asTokenList())));
        MethodRebaseResolver methodRebaseResolver = MethodRebaseResolver.Default.make(preparedMethodRegistry.getInstrumentedType(),
                rebaseableMethods,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                methodNameTransformer);
        MethodRegistry.Compiled compiledMethodRegistry = preparedMethodRegistry.compile(new RebaseImplementationTarget.Factory(rebaseableMethods, methodRebaseResolver));
        return TypeWriter.Default.<T>forRebasing(compiledMethodRegistry,
                fieldRegistry.compile(compiledMethodRegistry.getInstrumentedType()),
                auxiliaryTypeNamingStrategy,
                implementationContextFactory,
                asmVisitorWrapper,
                typeAttributeAppender,
                classFileVersion,
                classFileLocator,
                originalType,
                methodRebaseResolver,
                annotationValueFilterFactory).make();
    }
}

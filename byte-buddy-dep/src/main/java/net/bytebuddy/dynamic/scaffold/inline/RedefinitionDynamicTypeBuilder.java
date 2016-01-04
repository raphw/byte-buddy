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
import net.bytebuddy.matcher.ElementMatcher;

public class RedefinitionDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase.Adapter<T> {

    private final TypeDescription originalType;

    private final ClassFileLocator classFileLocator;

    public RedefinitionDynamicTypeBuilder(InstrumentedType.WithFlexibleName instrumentedType,
                                          ClassFileVersion classFileVersion,
                                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                          AnnotationValueFilter.Factory annotationValueFilterFactory,
                                          AnnotationRetention annotationRetention,
                                          Implementation.Context.Factory implementationContextFactory,
                                          MethodGraph.Compiler methodGraphCompiler,
                                          ElementMatcher<? super MethodDescription> ignoredMethods,
                                          TypeDescription originalType,
                                          ClassFileLocator classFileLocator) {
        this(instrumentedType,
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                new TypeAttributeAppender.ForInstrumentedType.Excluding(originalType),
                AsmVisitorWrapper.NoOp.INSTANCE,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                annotationValueFilterFactory,
                annotationRetention,
                implementationContextFactory,
                methodGraphCompiler,
                ignoredMethods,
                originalType,
                classFileLocator);
    }

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
                                             ElementMatcher<? super MethodDescription> ignoredMethods,
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
                ignoredMethods);
        this.originalType = originalType;
        this.classFileLocator = classFileLocator;
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
                                                 ElementMatcher<? super MethodDescription> ignoredMethods) {
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
                ignoredMethods,
                originalType,
                classFileLocator);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        MethodRegistry.Compiled compiledMethodRegistry = methodRegistry.prepare(instrumentedType,
                methodGraphCompiler,
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
                originalType,
                classFileLocator).make();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        if (!super.equals(other)) return false;
        RedefinitionDynamicTypeBuilder<?> that = (RedefinitionDynamicTypeBuilder<?>) other;
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
                ", ignoredMethods=" + ignoredMethods +
                ", originalType=" + originalType +
                ", classFileLocator=" + classFileLocator +
                '}';
    }
}

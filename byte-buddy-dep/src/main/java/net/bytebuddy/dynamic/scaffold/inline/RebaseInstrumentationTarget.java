package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.NullConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

public class RebaseInstrumentationTarget extends SubclassInstrumentationTarget {

    protected final MethodFlatteningResolver methodFlatteningResolver;

    protected RebaseInstrumentationTarget(MethodLookupEngine.Finding finding,
                                          BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                          MethodFlatteningResolver methodFlatteningResolver) {
        super(finding, bridgeMethodResolverFactory);
        this.methodFlatteningResolver = methodFlatteningResolver;
    }

    @Override
    protected Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        return methodDescription.getDeclaringType().equals(typeDescription)
                ? invocationOf(methodFlatteningResolver.resolve(methodDescription))
                : super.invokeSuper(methodDescription);
    }

    private Instrumentation.SpecialMethodInvocation invocationOf(MethodFlatteningResolver.Resolution resolution) {
        if (!resolution.isRedefined()) {
            throw new IllegalArgumentException("Cannot invoke non-redefined method " + resolution.getResolvedMethod());
        }
        return resolution.getResolvedMethod().isConstructor()
                ? new RedefinedConstructorInvocation(resolution.getResolvedMethod(), typeDescription)
                : Instrumentation.SpecialMethodInvocation.Simple.of(resolution.getResolvedMethod(), typeDescription);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && methodFlatteningResolver.equals(((RebaseInstrumentationTarget) other).methodFlatteningResolver);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + methodFlatteningResolver.hashCode();
    }

    @Override
    public String toString() {
        return "RebaseInstrumentationTarget{" +
                "typeDescription=" + typeDescription +
                ", defaultMethods=" + defaultMethods +
                ", bridgeMethodResolver=" + bridgeMethodResolver +
                ", methodRedefinitionResolver=" + methodFlatteningResolver +
                '}';
    }

    protected static class RedefinedConstructorInvocation implements Instrumentation.SpecialMethodInvocation {

        private final MethodDescription methodDescription;

        private final TypeDescription typeDescription;

        private final StackManipulation stackManipulation;

        public RedefinedConstructorInvocation(MethodDescription methodDescription, TypeDescription typeDescription) {
            this.methodDescription = methodDescription;
            this.typeDescription = typeDescription;
            stackManipulation = new Compound(NullConstant.INSTANCE, MethodInvocation.invoke(methodDescription));
        }

        @Override
        public MethodDescription getMethodDescription() {
            return methodDescription;
        }

        @Override
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }

        @Override
        public boolean isValid() {
            return stackManipulation.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            return stackManipulation.apply(methodVisitor, instrumentationContext);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Instrumentation.SpecialMethodInvocation specialMethodInvocation = (Instrumentation.SpecialMethodInvocation) other;
            return isValid() == specialMethodInvocation.isValid()
                    && typeDescription.equals(specialMethodInvocation.getTypeDescription())
                    && methodDescription.getInternalName().equals(specialMethodInvocation.getMethodDescription().getInternalName())
                    && methodDescription.getParameterTypes().equals(specialMethodInvocation.getMethodDescription().getParameterTypes())
                    && methodDescription.getReturnType().equals(specialMethodInvocation.getMethodDescription().getReturnType());
        }

        @Override
        public int hashCode() {
            int result = methodDescription.getInternalName().hashCode();
            result = 31 * result + methodDescription.getParameterTypes().hashCode();
            result = 31 * result + methodDescription.getReturnType().hashCode();
            result = 31 * result + typeDescription.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "RebaseInstrumentationTarget.RedefinedConstructorInvocation{" +
                    "typeDescription=" + typeDescription +
                    ", methodDescription=" + methodDescription +
                    '}';
        }
    }
}

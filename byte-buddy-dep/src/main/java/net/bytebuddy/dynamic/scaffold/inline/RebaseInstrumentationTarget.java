package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

/**
 * An instrumentation target for redefining a given type while preserving the original methods within the
 * instrumented type.
 * <p>&nbsp;</p>
 * Super method calls are merely emulated by this {@link net.bytebuddy.instrumentation.Instrumentation.Target} in order
 * to preserve Java's super call semantics a user would expect when invoking a {@code super}-prefixed method. This
 * means that original methods are either moved to renamed {@code private} methods which are never dispatched
 * virtually or they are invoked directly via the {@code INVOKESPECIAL} invocation to explicitly forbid a virtual
 * dispatch.
 */
public class RebaseInstrumentationTarget extends Instrumentation.Target.AbstractBase {

    /**
     * A method rebase resolver to be used when calling a rebased method.
     */
    protected final MethodRebaseResolver methodRebaseResolver;

    /**
     * Creates a rebase instrumentation target.
     *
     * @param finding                     The lookup of the instrumented type this instance should represent.
     * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver.
     * @param methodRebaseResolver        A method rebase resolver to be used when calling a rebased method.
     */
    protected RebaseInstrumentationTarget(MethodLookupEngine.Finding finding,
                                          BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                          MethodRebaseResolver methodRebaseResolver) {
        super(finding, bridgeMethodResolverFactory);
        this.methodRebaseResolver = methodRebaseResolver;
    }

    @Override
    protected Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        return methodDescription.getDeclaringType().equals(typeDescription)
                ? invokeSuper(methodRebaseResolver.resolve(methodDescription))
                : Instrumentation.SpecialMethodInvocation.Simple.of(methodDescription, typeDescription.getSupertype());
    }

    /**
     * Defines a special method invocation on type level. This means that invoke super instructions are not explicitly
     * dispatched on the super type but on the instrumented type. This allows to call methods non-virtually even though
     * they are not defined on the super type. Redefined constructors are not renamed by are added an additional
     * parameter of a type which is only used for this purpose. Additionally, a {@code null} value is loaded onto the
     * stack when the special method invocation is applied in order to fill the operand stack with an additional caller
     * argument. Non-constructor methods are renamed.
     *
     * @param resolution A proxied super method invocation on the instrumented type.
     * @return A special method invocation on this proxied super method.
     */
    private Instrumentation.SpecialMethodInvocation invokeSuper(MethodRebaseResolver.Resolution resolution) {
        return resolution.isRebased()
                ? RebasedMethodSpecialMethodInvocation.of(resolution, typeDescription)
                : Instrumentation.SpecialMethodInvocation.Simple.of(resolution.getResolvedMethod(), typeDescription);
    }

    @Override
    public TypeDescription getOriginType() {
        return typeDescription;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && methodRebaseResolver.equals(((RebaseInstrumentationTarget) other).methodRebaseResolver);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + methodRebaseResolver.hashCode();
    }

    @Override
    public String toString() {
        return "RebaseInstrumentationTarget{" +
                "typeDescription=" + typeDescription +
                ", defaultMethods=" + defaultMethods +
                ", bridgeMethodResolver=" + bridgeMethodResolver +
                ", methodRebaseResolver=" + methodRebaseResolver +
                '}';
    }

    /**
     * A {@link net.bytebuddy.instrumentation.Instrumentation.SpecialMethodInvocation} which invokes a rebased method
     * as given by a {@link MethodRebaseResolver}.
     */
    protected static class RebasedMethodSpecialMethodInvocation implements Instrumentation.SpecialMethodInvocation {

        /**
         * The method to invoke via a special method invocation.
         */
        private final MethodDescription methodDescription;

        /**
         * The instrumented type on which the method should be invoked on.
         */
        private final TypeDescription instrumentedType;

        /**
         * The stack manipulation to execute in order to invoke the rebased method.
         */
        private final StackManipulation stackManipulation;

        /**
         * Creates a special method invocation for a rebased method.
         *
         * @param resolution       The resolution of the rebased method.
         * @param instrumentedType The instrumented type on which this method is to be invoked.
         */
        protected RebasedMethodSpecialMethodInvocation(MethodRebaseResolver.Resolution resolution,
                                                       TypeDescription instrumentedType) {
            this.instrumentedType = instrumentedType;
            methodDescription = resolution.getResolvedMethod();
            stackManipulation = new Compound(resolution.getAdditionalArguments(), resolution.getResolvedMethod().isStatic()
                    ? MethodInvocation.invoke(resolution.getResolvedMethod())
                    : MethodInvocation.invoke(resolution.getResolvedMethod()).special(instrumentedType));
        }

        /**
         * Creates a special method invocation for a rebased method if such an invocation is possible or otherwise
         * returns an illegal special method invocation.
         *
         * @param resolution       The resolution of the rebased method.
         * @param instrumentedType The instrumented type on which this method is to be invoked.
         * @return A special method invocation for the given method.
         */
        public static Instrumentation.SpecialMethodInvocation of(MethodRebaseResolver.Resolution resolution,
                                                                 TypeDescription instrumentedType) {
            return resolution.getResolvedMethod().isAbstract()
                    ? Illegal.INSTANCE
                    : new RebasedMethodSpecialMethodInvocation(resolution, instrumentedType);
        }

        @Override
        public MethodDescription getMethodDescription() {
            return methodDescription;
        }

        @Override
        public TypeDescription getTypeDescription() {
            return instrumentedType;
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
            if (!(other instanceof Instrumentation.SpecialMethodInvocation)) return false;
            Instrumentation.SpecialMethodInvocation specialMethodInvocation = (Instrumentation.SpecialMethodInvocation) other;
            return isValid() == specialMethodInvocation.isValid()
                    && instrumentedType.equals(specialMethodInvocation.getTypeDescription())
                    && methodDescription.getInternalName().equals(specialMethodInvocation.getMethodDescription().getInternalName())
                    && methodDescription.getParameterTypes().equals(specialMethodInvocation.getMethodDescription().getParameterTypes())
                    && methodDescription.getReturnType().equals(specialMethodInvocation.getMethodDescription().getReturnType());
        }

        @Override
        public int hashCode() {
            int result = methodDescription.getInternalName().hashCode();
            result = 31 * result + methodDescription.getParameterTypes().hashCode();
            result = 31 * result + methodDescription.getReturnType().hashCode();
            result = 31 * result + instrumentedType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "RebaseInstrumentationTarget.RedefinedConstructorInvocation{" +
                    "instrumentedType=" + instrumentedType +
                    ", methodDescription=" + methodDescription +
                    '}';
        }
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.inline.RebaseInstrumentationTarget}.
     */
    public static class Factory implements Instrumentation.Target.Factory {

        /**
         * A factory for creating a bridge method resolver.
         */
        private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

        /**
         * A method rebase resolver to be used when calling a rebased method.
         */
        private final MethodRebaseResolver methodRebaseResolver;

        /**
         * Creates a new factory for creating a {@link net.bytebuddy.dynamic.scaffold.inline.RebaseInstrumentationTarget}.
         *
         * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver.
         * @param methodRebaseResolver        A method rebase resolver to be used when calling a rebased method.
         */
        public Factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                       MethodRebaseResolver methodRebaseResolver) {
            this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
            this.methodRebaseResolver = methodRebaseResolver;
        }

        @Override
        public Instrumentation.Target make(MethodLookupEngine.Finding finding) {
            return new RebaseInstrumentationTarget(finding,
                    bridgeMethodResolverFactory,
                    methodRebaseResolver);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && bridgeMethodResolverFactory.equals(((Factory) other).bridgeMethodResolverFactory)
                    && methodRebaseResolver.equals(((Factory) other).methodRebaseResolver);
        }

        @Override
        public int hashCode() {
            return 31 * bridgeMethodResolverFactory.hashCode() + methodRebaseResolver.hashCode();
        }

        @Override
        public String toString() {
            return "RebaseInstrumentationTarget.Factory{" +
                    "bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                    ", methodRebaseResolver=" + methodRebaseResolver +
                    '}';
        }
    }
}

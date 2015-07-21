package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.MethodLookupEngine;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

/**
 * An implementation target for redefining a given type while preserving the original methods within the
 * instrumented type.
 * <p>&nbsp;</p>
 * Super method calls are merely emulated by this {@link Implementation.Target} in order
 * to preserve Java's super call semantics a user would expect when invoking a {@code super}-prefixed method. This
 * means that original methods are either moved to renamed {@code private} methods which are never dispatched
 * virtually or they are invoked directly via the {@code INVOKESPECIAL} invocation to explicitly forbid a virtual
 * dispatch.
 */
public class RebaseImplementationTarget extends Implementation.Target.AbstractBase {

    /**
     * A method rebase resolver to be used when calling a rebased method.
     */
    protected final MethodRebaseResolver methodRebaseResolver;

    /**
     * Creates a rebase implementation target.
     *
     * @param finding                     The lookup of the instrumented type this instance should represent.
     * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver.
     * @param methodRebaseResolver        A method rebase resolver to be used when calling a rebased method.
     */
    protected RebaseImplementationTarget(MethodLookupEngine.Finding finding,
                                         BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                         MethodRebaseResolver methodRebaseResolver) {
        super(finding, bridgeMethodResolverFactory);
        this.methodRebaseResolver = methodRebaseResolver;
    }

    @Override
    protected Implementation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        return methodDescription.getDeclaringType().equals(typeDescription)
                ? invokeSuper(methodRebaseResolver.resolve(methodDescription.asDeclared()))
                : Implementation.SpecialMethodInvocation.Simple.of(methodDescription, typeDescription.getSuperType().asRawType());
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
    private Implementation.SpecialMethodInvocation invokeSuper(MethodRebaseResolver.Resolution resolution) {
        return resolution.isRebased()
                ? RebasedMethodInvocation.of(resolution, typeDescription)
                : Implementation.SpecialMethodInvocation.Simple.of(resolution.getResolvedMethod(), typeDescription);
    }

    @Override
    public TypeDescription getOriginType() {
        return typeDescription;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && methodRebaseResolver.equals(((RebaseImplementationTarget) other).methodRebaseResolver);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + methodRebaseResolver.hashCode();
    }

    @Override
    public String toString() {
        return "RebaseImplementationTarget{" +
                "typeDescription=" + typeDescription +
                ", defaultMethods=" + defaultMethods +
                ", bridgeMethodResolver=" + bridgeMethodResolver +
                ", methodRebaseResolver=" + methodRebaseResolver +
                '}';
    }

    /**
     * A {@link Implementation.SpecialMethodInvocation} which invokes a rebased method
     * as given by a {@link MethodRebaseResolver}.
     */
    protected static class RebasedMethodInvocation implements Implementation.SpecialMethodInvocation {

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
        protected RebasedMethodInvocation(MethodRebaseResolver.Resolution resolution, TypeDescription instrumentedType) {
            this.instrumentedType = instrumentedType;
            methodDescription = resolution.getResolvedMethod();
            stackManipulation = new Compound(resolution.getAdditionalArguments(), resolution.getResolvedMethod().isStatic()
                    ? MethodInvocation.invoke(methodDescription)
                    : MethodInvocation.invoke(methodDescription).special(instrumentedType));
        }

        /**
         * Creates a special method invocation for a rebased method if such an invocation is possible or otherwise
         * returns an illegal special method invocation.
         *
         * @param resolution       The resolution of the rebased method.
         * @param instrumentedType The instrumented type on which this method is to be invoked.
         * @return A special method invocation for the given method.
         */
        public static Implementation.SpecialMethodInvocation of(MethodRebaseResolver.Resolution resolution, TypeDescription instrumentedType) {
            return resolution.getResolvedMethod().isAbstract()
                    ? Illegal.INSTANCE
                    : new RebasedMethodInvocation(resolution, instrumentedType);
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
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return stackManipulation.apply(methodVisitor, implementationContext);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Implementation.SpecialMethodInvocation)) return false;
            Implementation.SpecialMethodInvocation specialMethodInvocation = (Implementation.SpecialMethodInvocation) other;
            return isValid() == specialMethodInvocation.isValid()
                    && instrumentedType.equals(specialMethodInvocation.getTypeDescription())
                    && methodDescription.getInternalName().equals(specialMethodInvocation.getMethodDescription().getInternalName())
                    && methodDescription.getParameters().asTypeList().asRawTypes().equals(specialMethodInvocation.getMethodDescription().getParameters().asTypeList().asRawTypes())
                    && methodDescription.getReturnType().asRawType().equals(specialMethodInvocation.getMethodDescription().getReturnType().asRawType());
        }

        @Override
        public int hashCode() {
            int result = methodDescription.getInternalName().hashCode();
            result = 31 * result + methodDescription.getParameters().asTypeList().asRawTypes().hashCode();
            result = 31 * result + methodDescription.getReturnType().asRawType().hashCode();
            result = 31 * result + instrumentedType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "RebaseimplementationTarget.RebasedMethodInvocation{" +
                    "instrumentedType=" + instrumentedType +
                    ", methodDescription=" + methodDescription +
                    '}';
        }
    }

    /**
     * A factory for creating a {@link RebaseImplementationTarget}.
     */
    public static class Factory implements Implementation.Target.Factory {

        /**
         * The bridge method resolver factory to use.
         */
        private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

        /**
         * The method rebase resolver to use.
         */
        private final MethodRebaseResolver methodRebaseResolver;

        /**
         * Creates a new factory for a rebase implementation target.
         *
         * @param bridgeMethodResolverFactory The bridge method resolver factory to use.
         * @param methodRebaseResolver        The method rebase resolver to use.
         */
        public Factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory, MethodRebaseResolver methodRebaseResolver) {
            this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
            this.methodRebaseResolver = methodRebaseResolver;
        }

        @Override
        public Implementation.Target make(MethodLookupEngine.Finding finding, List<? extends MethodDescription> instrumentedMethods) {
            return new RebaseImplementationTarget(finding, bridgeMethodResolverFactory, methodRebaseResolver);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && bridgeMethodResolverFactory.equals(((Factory) other).bridgeMethodResolverFactory)
                    && methodRebaseResolver.equals(((Factory) other).methodRebaseResolver);
        }

        @Override
        public int hashCode() {
            int result = bridgeMethodResolverFactory.hashCode();
            result = 31 * result + methodRebaseResolver.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "RebaseImplementationTarget.Factory{" +
                    "bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                    ", methodRebaseResolver=" + methodRebaseResolver +
                    '}';
        }
    }
}

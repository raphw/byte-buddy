package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

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
@HashCodeAndEqualsPlugin.Enhance
public class RebaseImplementationTarget extends Implementation.Target.AbstractBase {

    /**
     * A mapping of the instrumented type's declared methods by each method's token.
     */
    private final Map<MethodDescription.SignatureToken, MethodRebaseResolver.Resolution> rebaseableMethods;

    /**
     * Creates a rebase implementation target.
     *
     * @param instrumentedType        The instrumented type.
     * @param methodGraph             A method graph of the instrumented type.
     * @param defaultMethodInvocation The default method invocation mode to apply.
     * @param rebaseableMethods       A mapping of the instrumented type's declared methods by each method's token.
     */
    protected RebaseImplementationTarget(TypeDescription instrumentedType,
                                         MethodGraph.Linked methodGraph,
                                         DefaultMethodInvocation defaultMethodInvocation,
                                         Map<MethodDescription.SignatureToken, MethodRebaseResolver.Resolution> rebaseableMethods) {
        super(instrumentedType, methodGraph, defaultMethodInvocation);
        this.rebaseableMethods = rebaseableMethods;
    }

    /**
     * Creates a new rebase implementation target.
     *
     * @param instrumentedType     The instrumented type.
     * @param methodGraph          A method graph of the instrumented type.
     * @param classFileVersion     The type's class file version.
     * @param methodRebaseResolver A method rebase resolver to be used when calling a rebased method.
     * @return An implementation target for the given input.
     */
    protected static Implementation.Target of(TypeDescription instrumentedType,
                                              MethodGraph.Linked methodGraph,
                                              ClassFileVersion classFileVersion,
                                              MethodRebaseResolver methodRebaseResolver) {
        return new RebaseImplementationTarget(instrumentedType, methodGraph, DefaultMethodInvocation.of(classFileVersion), methodRebaseResolver.asTokenMap());
    }

    /**
     * {@inheritDoc}
     */
    public Implementation.SpecialMethodInvocation invokeSuper(MethodDescription.SignatureToken token) {
        MethodRebaseResolver.Resolution resolution = rebaseableMethods.get(token);
        return resolution == null
                ? invokeSuper(methodGraph.getSuperClassGraph().locate(token))
                : invokeSuper(resolution);
    }

    /**
     * Creates a special method invocation for the given node.
     *
     * @param node The node for which a special method invocation is to be created.
     * @return A special method invocation for the provided node.
     */
    private Implementation.SpecialMethodInvocation invokeSuper(MethodGraph.Node node) {
        return node.getSort().isResolved()
                ? Implementation.SpecialMethodInvocation.Simple.of(node.getRepresentative(), instrumentedType.getSuperClass().asErasure())
                : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
    }

    /**
     * Creates a special method invocation for the given rebase resolution.
     *
     * @param resolution The resolution for which a special method invocation is to be created.
     * @return A special method invocation for the provided resolution.
     */
    private Implementation.SpecialMethodInvocation invokeSuper(MethodRebaseResolver.Resolution resolution) {
        return resolution.isRebased()
                ? RebasedMethodInvocation.of(resolution.getResolvedMethod(), instrumentedType, resolution.getAdditionalArguments())
                : Implementation.SpecialMethodInvocation.Simple.of(resolution.getResolvedMethod(), instrumentedType);
    }

    /**
     * {@inheritDoc}
     */
    public TypeDescription getOriginType() {
        return instrumentedType;
    }

    /**
     * A {@link Implementation.SpecialMethodInvocation} which invokes a rebased method
     * as given by a {@link MethodRebaseResolver}.
     */
    protected static class RebasedMethodInvocation extends Implementation.SpecialMethodInvocation.AbstractBase {

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
         * Creates a new rebased method invocation.
         *
         * @param methodDescription The method to invoke via a special method invocation.
         * @param instrumentedType  The instrumented type on which the method should be invoked on.
         * @param stackManipulation The stack manipulation to execute in order to invoke the rebased method.
         */
        protected RebasedMethodInvocation(MethodDescription methodDescription, TypeDescription instrumentedType, StackManipulation stackManipulation) {
            this.methodDescription = methodDescription;
            this.instrumentedType = instrumentedType;
            this.stackManipulation = stackManipulation;
        }

        /**
         * Creates a special method invocation for the given method.
         *
         * @param resolvedMethod      The rebased method to be invoked.
         * @param instrumentedType    The instrumented type on which the method is to be invoked if it is non-static.
         * @param additionalArguments Any additional arguments that are to be provided to the rebased method.
         * @return A special method invocation of the rebased method.
         */
        protected static Implementation.SpecialMethodInvocation of(MethodDescription resolvedMethod,
                                                                   TypeDescription instrumentedType,
                                                                   StackManipulation additionalArguments) {
            StackManipulation stackManipulation = resolvedMethod.isStatic()
                    ? MethodInvocation.invoke(resolvedMethod)
                    : MethodInvocation.invoke(resolvedMethod).special(instrumentedType);
            return stackManipulation.isValid()
                    ? new RebasedMethodInvocation(resolvedMethod, instrumentedType, new Compound(additionalArguments, stackManipulation))
                    : Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription getMethodDescription() {
            return methodDescription;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return instrumentedType;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return stackManipulation.apply(methodVisitor, implementationContext);
        }
    }

    /**
     * A factory for creating a {@link RebaseImplementationTarget}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class Factory implements Implementation.Target.Factory {

        /**
         * The method rebase resolver to use.
         */
        private final MethodRebaseResolver methodRebaseResolver;

        /**
         * Creates a new factory for a rebase implementation target.
         *
         * @param methodRebaseResolver The method rebase resolver to use.
         */
        public Factory(MethodRebaseResolver methodRebaseResolver) {
            this.methodRebaseResolver = methodRebaseResolver;
        }

        /**
         * {@inheritDoc}
         */
        public Implementation.Target make(TypeDescription instrumentedType, MethodGraph.Linked methodGraph, ClassFileVersion classFileVersion) {
            return RebaseImplementationTarget.of(instrumentedType, methodGraph, classFileVersion, methodRebaseResolver);
        }
    }
}

package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static net.bytebuddy.utility.ByteBuddyCommons.isInterface;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * This {@link net.bytebuddy.instrumentation.Instrumentation} invokes a default method for the methods it instruments.
 * A default method is potentially ambiguous if a method of identical signature is defined in several interfaces.
 * For this reason, this instrumentation allows for the specification of <i>prioritized interfaces</i> whose default
 * methods, if a method of equivalent signature is defined for a specific interface. All prioritized interfaces are
 * searched for default methods in the order of their specification. If no prioritized interface defines a default method
 * of equivalent signature to the given instrumented method, any non-prioritized interface is searched for a suitable
 * default method. If exactly one of those interfaces defines a suitable default method, this method is invoked.
 * If no method or more than one method is identified as a suitable default method, an exception is thrown.
 * <p>&nbsp;</p>
 * When it comes to default methods, the Java programming language specifies stronger requirements for the
 * legitimacy of invoking a default method than the Java runtime. The Java compiler requires a method to be
 * the most specific method in its defining type's type hierarchy, i.e. the method must not be overridden by another
 * interface or class type. Additionally, the method must be invokable from an interface type which is directly
 * implemented by the instrumented type. The Java runtime only requires the second condition to be fulfilled which
 * is why this instrumentation only checks the later condition, as well.
 */
public class DefaultMethodCall implements Instrumentation {

    /**
     * A list of prioritized interfaces in the order in which a method should be attempted to be called.
     */
    private final List<TypeDescription> prioritizedInterfaces;

    /**
     * Creates a new {@link net.bytebuddy.instrumentation.DefaultMethodCall} instrumentation for a given list of
     * prioritized interfaces.
     *
     * @param prioritizedInterfaces A list of prioritized interfaces in the order in which a method should be attempted to
     *                              be called.
     */
    protected DefaultMethodCall(List<TypeDescription> prioritizedInterfaces) {
        for (TypeDescription typeDescription : prioritizedInterfaces) {
            isInterface(typeDescription);
        }
        this.prioritizedInterfaces = prioritizedInterfaces;
    }

    /**
     * Creates a {@link net.bytebuddy.instrumentation.DefaultMethodCall} instrumentation which searches the given list
     * of interface types for a suitable default method in their order. If no such prioritized interface is suitable,
     * because it is either not defined on the instrumented type or because it does not define a suitable default method,
     * any remaining interface is searched for a suitable default method. If no or more than one method defines a
     * suitable default method, an exception is thrown.
     *
     * @param prioritizedInterface A list of prioritized default method interfaces in their prioritization order.
     * @return An instrumentation which calls an instrumented method's compatible default method that considers the given
     * interfaces to be prioritized in their order.
     */
    public static Instrumentation prioritize(Class<?>... prioritizedInterface) {
        return new DefaultMethodCall(new TypeList.ForLoadedType(nonNull(prioritizedInterface)));
    }

    public static Instrumentation prioritize(TypeDescription... prioritizedInterface) {
        return new DefaultMethodCall(new TypeList.Explicit(Arrays.asList(nonNull(prioritizedInterface))));
    }

    /**
     * Creates a {@link net.bytebuddy.instrumentation.DefaultMethodCall} instrumentation without prioritizing any
     * interface. Instead, any interface that is defined for a given type is searched for a suitable default method. If
     * exactly one interface defines a suitable default method, this method is invoked from the instrumented method.
     * Otherwise, an exception is thrown.
     *
     * @return An instrumentation which calls an instrumented method's compatible default method if such a method
     * is unambiguous.
     */
    public static Instrumentation unambiguousOnly() {
        return new DefaultMethodCall(new TypeList.Empty());
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(Target instrumentationTarget) {
        return new Appender(instrumentationTarget, filterRelevant(instrumentationTarget.getTypeDescription()));
    }

    /**
     * Filters the relevant prioritized interfaces for a given type, i.e. finds the types that are
     * directly declared on a given instrumented type.
     *
     * @param typeDescription The instrumented type for which the prioritized interfaces are to be filtered.
     * @return A list of prioritized interfaces that are additionally implemented by the given type.
     */
    private List<TypeDescription> filterRelevant(TypeDescription typeDescription) {
        List<TypeDescription> filtered = new ArrayList<TypeDescription>(prioritizedInterfaces.size());
        Set<TypeDescription> relevant = new HashSet<TypeDescription>(typeDescription.getInterfaces());
        for (TypeDescription prioritizedInterface : prioritizedInterfaces) {
            if (relevant.remove(prioritizedInterface)) {
                filtered.add(prioritizedInterface);
            }
        }
        return filtered;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && prioritizedInterfaces.equals(((DefaultMethodCall) other).prioritizedInterfaces);
    }

    @Override
    public int hashCode() {
        return prioritizedInterfaces.hashCode();
    }

    @Override
    public String toString() {
        return "DefaultMethodCall{prioritizedInterfaces=" + prioritizedInterfaces + '}';
    }

    /**
     * The appender for implementing a {@link net.bytebuddy.instrumentation.DefaultMethodCall}.
     */
    protected static class Appender implements ByteCodeAppender {

        /**
         * The instrumentation target of this appender.
         */
        private final Target instrumentationTarget;

        /**
         * The relevant prioritized interfaces to be considered by this appender.
         */
        private final List<TypeDescription> prioritizedInterfaces;

        /**
         * The relevant non-prioritized interfaces to be considered by this appender.
         */
        private final Set<TypeDescription> nonPrioritizedInterfaces;

        /**
         * Creates a new appender for implementing a {@link net.bytebuddy.instrumentation.DefaultMethodCall}.
         *
         * @param instrumentationTarget The instrumentation target of this appender.
         * @param prioritizedInterfaces The relevant prioritized interfaces to be considered by this appender.
         */
        protected Appender(Target instrumentationTarget, List<TypeDescription> prioritizedInterfaces) {
            this.instrumentationTarget = instrumentationTarget;
            this.prioritizedInterfaces = prioritizedInterfaces;
            this.nonPrioritizedInterfaces = new HashSet<TypeDescription>(instrumentationTarget.getTypeDescription().getInterfaces());
            nonPrioritizedInterfaces.removeAll(prioritizedInterfaces);
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
            StackManipulation defaultMethodInvocation = locateDefault(instrumentedMethod);
            if (!defaultMethodInvocation.isValid()) {
                throw new IllegalArgumentException("Cannot invoke default method on " + instrumentedMethod);
            }
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                    defaultMethodInvocation,
                    MethodReturn.returning(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        /**
         * Locates a special method invocation to be invoked from a given method.
         *
         * @param methodDescription The method that is currently instrumented.
         * @return A potentially illegal stack manipulation representing the default method invocation for the
         * given method.
         */
        private StackManipulation locateDefault(MethodDescription methodDescription) {
            String uniqueMethodSignature = methodDescription.getUniqueSignature();
            SpecialMethodInvocation specialMethodInvocation = SpecialMethodInvocation.Illegal.INSTANCE;
            for (TypeDescription typeDescription : prioritizedInterfaces) {
                specialMethodInvocation = instrumentationTarget.invokeDefault(typeDescription, uniqueMethodSignature);
                if (specialMethodInvocation.isValid()) {
                    return specialMethodInvocation;
                }
            }
            for (TypeDescription typeDescription : nonPrioritizedInterfaces) {
                SpecialMethodInvocation other = instrumentationTarget.invokeDefault(typeDescription, uniqueMethodSignature);
                if (specialMethodInvocation.isValid() && other.isValid()) {
                    throw new IllegalArgumentException(methodDescription + " has an ambiguous default method with "
                            + other.getMethodDescription() + " and " + specialMethodInvocation.getMethodDescription());
                }
                specialMethodInvocation = other;
            }
            return specialMethodInvocation;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && prioritizedInterfaces.equals(((Appender) other).prioritizedInterfaces)
                    && instrumentationTarget.equals(((Appender) other).instrumentationTarget);
        }

        @Override
        public int hashCode() {
            return 31 * instrumentationTarget.hashCode() + prioritizedInterfaces.hashCode();
        }

        @Override
        public String toString() {
            return "DefaultMethodCall.Appender{" +
                    "instrumentationTarget=" + instrumentationTarget +
                    ", prioritizedInterfaces=" + prioritizedInterfaces +
                    ", nonPrioritizedInterfaces=" + nonPrioritizedInterfaces +
                    '}';
        }
    }
}

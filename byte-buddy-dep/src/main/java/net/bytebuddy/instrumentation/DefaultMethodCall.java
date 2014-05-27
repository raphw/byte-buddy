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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.utility.ByteBuddyCommons.isInterface;

/**
 * A
 */
public class DefaultMethodCall implements Instrumentation {

    private final List<TypeDescription> preferredInterfaces;

    protected DefaultMethodCall(List<TypeDescription> preferredInterfaces) {
        for (TypeDescription typeDescription : preferredInterfaces) {
            isInterface(typeDescription);
        }
        this.preferredInterfaces = preferredInterfaces;
    }

    public static Instrumentation preferring(Class<?>... prioritizedInterface) {
        return new DefaultMethodCall(new TypeList.ForLoadedType(prioritizedInterface));
    }

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

    private List<TypeDescription> filterRelevant(TypeDescription typeDescription) {
        List<TypeDescription> filtered = new ArrayList<TypeDescription>(preferredInterfaces.size());
        Set<TypeDescription> relevant = new HashSet<TypeDescription>(typeDescription.getInterfaces());
        for (TypeDescription preferredInterface : preferredInterfaces) {
            if (relevant.remove(preferredInterface)) {
                filtered.add(preferredInterface);
            }
        }
        return filtered;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && preferredInterfaces.equals(((DefaultMethodCall) other).preferredInterfaces);
    }

    @Override
    public int hashCode() {
        return preferredInterfaces.hashCode();
    }

    @Override
    public String toString() {
        return "DefaultMethodCall{preferredInterfaces=" + preferredInterfaces + '}';
    }

    private static class Appender implements ByteCodeAppender {

        private final Target instrumentationTarget;
        private final List<TypeDescription> explicitInterfaces;
        private final Set<TypeDescription> implicitInterfaces;

        public Appender(Target instrumentationTarget, List<TypeDescription> explicitInterfaces) {
            this.instrumentationTarget = instrumentationTarget;
            this.explicitInterfaces = explicitInterfaces;
            this.implicitInterfaces = new HashSet<TypeDescription>(instrumentationTarget.getTypeDescription().getInterfaces());
            implicitInterfaces.removeAll(explicitInterfaces);
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

        private StackManipulation locateDefault(MethodDescription methodDescription) {
            String uniqueMethodSignature = methodDescription.getUniqueSignature();
            SpecialMethodInvocation specialMethodInvocation = SpecialMethodInvocation.Illegal.INSTANCE;
            for (TypeDescription typeDescription : explicitInterfaces) {
                specialMethodInvocation = instrumentationTarget.invokeDefault(typeDescription, uniqueMethodSignature);
                if (specialMethodInvocation.isValid()) {
                    return specialMethodInvocation;
                }
            }
            for (TypeDescription typeDescription : implicitInterfaces) {
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
                    && explicitInterfaces.equals(((Appender) other).explicitInterfaces)
                    && instrumentationTarget.equals(((Appender) other).instrumentationTarget);
        }

        @Override
        public int hashCode() {
            return 31 * instrumentationTarget.hashCode() + explicitInterfaces.hashCode();
        }

        @Override
        public String toString() {
            return "DefaultMethodCall.Appender{" +
                    "instrumentationTarget=" + instrumentationTarget +
                    ", explicitInterfaces=" + explicitInterfaces +
                    ", implicitInterfaces=" + implicitInterfaces +
                    '}';
        }
    }
}

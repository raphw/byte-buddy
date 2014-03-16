package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * A delegate class that represents a method accessor factory for an instrumentation where an instrumentation is
 * conducted by creating a subclass of a given type. This delegate represents a mutable data structure.
 */
public class SubclassInstrumentationContextDelegate
        implements AuxiliaryType.MethodAccessorFactory,
        Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy,
        TypeWriter.MethodPool {

    private static final String DEFAULT_PREFIX = "delegate";

    private final String prefix;
    private final Random random;

    private final InstrumentedType instrumentedType;
    private final List<MethodDescription> orderedAccessorMethods;
    private final Map<MethodDescription, MethodDescription> knownTargetMethodsToAccessorMethod;
    private final Map<MethodDescription, MethodDescription> registeredAccessorMethodToTargetMethod;

    /**
     * Creates a new delegate with a default prefix.
     *
     * @param instrumentedType The instrumented type that is subject of the instrumentation.
     */
    public SubclassInstrumentationContextDelegate(InstrumentedType instrumentedType) {
        this(instrumentedType, DEFAULT_PREFIX);
    }

    /**
     * Creates a new delegate.
     *
     * @param instrumentedType The instrumented type that is subject of the instrumentation.
     * @param prefix           The prefix to be used for the delegation methods.
     */
    public SubclassInstrumentationContextDelegate(InstrumentedType instrumentedType, String prefix) {
        this.prefix = prefix;
        this.instrumentedType = instrumentedType;
        this.random = new Random();
        orderedAccessorMethods = new ArrayList<MethodDescription>();
        knownTargetMethodsToAccessorMethod = new HashMap<MethodDescription, MethodDescription>();
        registeredAccessorMethodToTargetMethod = new HashMap<MethodDescription, MethodDescription>();
    }

    @Override
    public String name(AuxiliaryType auxiliaryType) {
        return String.format("%s$%s$%d", instrumentedType.getName(), DEFAULT_PREFIX, Math.abs(random.nextInt()));
    }

    @Override
    public MethodDescription requireAccessorMethodFor(MethodDescription targetMethod) {
        MethodDescription accessorMethod = knownTargetMethodsToAccessorMethod.get(targetMethod);
        if (accessorMethod != null) {
            return accessorMethod;
        }
        String name = String.format("%s$%s$%d", targetMethod.getInternalName(), prefix, Math.abs(random.nextInt()));
        accessorMethod = new MethodDescription.Latent(name,
                instrumentedType,
                targetMethod.getReturnType(),
                targetMethod.getParameterTypes(),
                (targetMethod.isStatic() ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL);
        knownTargetMethodsToAccessorMethod.put(targetMethod, accessorMethod);
        registeredAccessorMethodToTargetMethod.put(accessorMethod, targetMethod);
        orderedAccessorMethods.add(accessorMethod);
        return accessorMethod;
    }

    /**
     * Returns an iterable containing all accessor methods that were registered with this delegate. The iterable can
     * safely be co-modified in the same thread in order to allow the registration of additional accessor methods with
     * this delegate while other accessors are already created for the instrumented type.
     *
     * @return An co-modifiable iterable of all accessors method that were registered with this delegate.
     */
    public Iterable<MethodDescription> getProxiedMethods() {
        return new TypeWriter.SameThreadCoModifiableIterable<MethodDescription>(orderedAccessorMethods);
    }

    private class SameSignatureMethodCall implements Entry, ByteCodeAppender {

        private final MethodDescription targetDescription;

        private SameSignatureMethodCall(MethodDescription targetDescription) {
            if (targetDescription == null) {
                throw new IllegalArgumentException("Unknown method: " + targetDescription);
            }
            this.targetDescription = targetDescription;
        }

        @Override
        public ByteCodeAppender getByteCodeAppender() {
            return this;
        }

        @Override
        public boolean isDefineMethod() {
            return true;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    MethodVariableAccess.loadThisAndArguments(instrumentedMethod),
                    MethodInvocation.invoke(targetDescription).special(instrumentedType.getSupertype()),
                    MethodReturn.returning(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public MethodAttributeAppender getAttributeAppender() {
            return MethodAttributeAppender.NoOp.INSTANCE;
        }
    }

    @Override
    public Entry target(MethodDescription methodDescription) {
        return new SameSignatureMethodCall(registeredAccessorMethodToTargetMethod.get(methodDescription));
    }

    @Override
    public String toString() {
        return "SubclassInstrumentationContextDelegate{" +
                "instrumentedType=" + instrumentedType +
                ", prefix='" + prefix + '\'' +
                ", random=" + random +
                ", knownTargetMethodsToAccessorMethod=" + knownTargetMethodsToAccessorMethod +
                '}';
    }
}

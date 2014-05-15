package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.Throw;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.TextConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.takesArguments;

/**
 * A delegate class that represents a method accessor factory for an instrumentation where an instrumentation is
 * conducted by creating a subclass of a given type. This delegate represents a mutable data structure.
 */
public class SubclassInstrumentationContextDelegate
        implements AuxiliaryType.MethodAccessorFactory,
        Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy,
        TypeWriter.MethodPool {

    /**
     * The prefix for exception messages that are thrown when calling invoking a super proxy's method for an abstract
     * method of the instrumented type.
     */
    public static final String ABSTRACT_METHOD_WARNING_PREFIX = "There is no super implementation for: ";

    private static final String DEFAULT_PREFIX = "delegate";
    private final String prefix;
    private final Random random;
    private final BridgeMethodResolver bridgeMethodResolver;
    private final InstrumentedType instrumentedType;
    private final List<MethodDescription> orderedAccessorMethods;
    private final Map<MethodDescription, MethodDescription> knownTargetMethodsToAccessorMethod;
    private final Map<MethodDescription, Entry> registeredAccessorMethodToTargetMethodCall;
    private final Map<String, MethodDescription> reachableMethods;

    /**
     * Creates a new delegate with a default prefix.
     *
     * @param instrumentedType            The instrumented type that is subject of the instrumentation.
     * @param methodLookupEngine          The method lookup engine to query for reachable methods.
     * @param bridgeMethodResolverFactory A factory that is used for creating a bridge method resolver for the given
     *                                    instrumented type.
     */
    public SubclassInstrumentationContextDelegate(InstrumentedType instrumentedType,
                                                  MethodLookupEngine methodLookupEngine,
                                                  BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
        this(instrumentedType, methodLookupEngine, bridgeMethodResolverFactory, DEFAULT_PREFIX);
    }

    /**
     * Creates a new delegate.
     *
     * @param instrumentedType            The instrumented type that is subject of the instrumentation.
     * @param methodLookupEngine          The method lookup engine to query for reachable methods.
     * @param bridgeMethodResolverFactory A factory that is used for creating a bridge method resolver for the given
     *                                    instrumented type.
     * @param prefix                      The prefix to be used for the delegation methods.
     */
    public SubclassInstrumentationContextDelegate(InstrumentedType instrumentedType,
                                                  MethodLookupEngine methodLookupEngine,
                                                  BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                                  String prefix) {
        this.prefix = prefix;
        this.instrumentedType = instrumentedType;
        random = new Random();
        MethodList reachableMethods = methodLookupEngine.getReachableMethods(instrumentedType);
        bridgeMethodResolver = bridgeMethodResolverFactory.make(reachableMethods);
        this.reachableMethods = new HashMap<String, MethodDescription>(reachableMethods.size());
        for (MethodDescription methodDescription : reachableMethods) {
            this.reachableMethods.put(methodDescription.getUniqueSignature(), methodDescription);
        }
        orderedAccessorMethods = new ArrayList<MethodDescription>();
        knownTargetMethodsToAccessorMethod = new HashMap<MethodDescription, MethodDescription>();
        registeredAccessorMethodToTargetMethodCall = new HashMap<MethodDescription, Entry>();
    }

    @Override
    public String name(AuxiliaryType auxiliaryType) {
        return String.format("%s$%s$%d", instrumentedType.getName(), DEFAULT_PREFIX, Math.abs(random.nextInt()));
    }

    @Override
    public MethodDescription requireAccessorMethodFor(MethodDescription targetMethod) {
        targetMethod = reachableMethods.get(targetMethod.getUniqueSignature());
        if (targetMethod == null) {
            throw new IllegalArgumentException("Method is not reachable from instrumented type");
        }
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
        Entry methodCall = targetMethod.isSpecializableFor(instrumentedType.getSupertype())
                ? new SameSignatureMethodCall(bridgeMethodResolver.resolve(targetMethod), instrumentedType)
                : AbstractMethodCall.INSTANCE;
        registeredAccessorMethodToTargetMethodCall.put(accessorMethod, methodCall);
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

    @Override
    public Entry target(MethodDescription methodDescription) {
        Entry targetMethodCall = registeredAccessorMethodToTargetMethodCall.get(methodDescription);
        if (targetMethodCall == null) {
            throw new IllegalArgumentException("Unknown method: " + methodDescription);
        }
        return targetMethodCall;
    }

    @Override
    public String toString() {
        return "SubclassInstrumentationContextDelegate{" +
                "instrumentedType=" + instrumentedType +
                ", bridgeMethodResolver=" + bridgeMethodResolver +
                ", prefix='" + prefix + '\'' +
                ", random=" + random +
                ", knownTargetMethodsToAccessorMethod=" + knownTargetMethodsToAccessorMethod +
                '}';
    }

    private static enum AbstractMethodCall implements Entry, ByteCodeAppender {

        INSTANCE;

        private final TypeDescription exceptionType;
        private final MethodDescription constructor;

        private AbstractMethodCall() {
            exceptionType = new TypeDescription.ForLoadedType(RuntimeException.class);
            constructor = exceptionType.getDeclaredMethods()
                    .filter(isConstructor().and(takesArguments(String.class))).getOnly();
        }

        @Override
        public boolean isDefineMethod() {
            return true;
        }

        @Override
        public ByteCodeAppender getByteCodeAppender() {
            return this;
        }

        @Override
        public MethodAttributeAppender getAttributeAppender() {
            return MethodAttributeAppender.NoOp.INSTANCE;
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
                    TypeCreation.forType(exceptionType),
                    Duplication.SINGLE,
                    new TextConstant(ABSTRACT_METHOD_WARNING_PREFIX + instrumentedMethod),
                    MethodInvocation.invoke(constructor),
                    Throw.INSTANCE
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    private static class SameSignatureMethodCall implements Entry, ByteCodeAppender {

        private final StackManipulation targetMethodCall;

        private SameSignatureMethodCall(MethodDescription accessorMethod, TypeDescription instrumentedType) {
            this.targetMethodCall = MethodInvocation.invoke(accessorMethod).special(instrumentedType.getSupertype());
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
                    MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                    targetMethodCall,
                    MethodReturn.returning(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public MethodAttributeAppender getAttributeAppender() {
            return MethodAttributeAppender.NoOp.INSTANCE;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && targetMethodCall.equals(((SameSignatureMethodCall) other).targetMethodCall);
        }

        @Override
        public int hashCode() {
            return targetMethodCall.hashCode();
        }

        @Override
        public String toString() {
            return "SameSignatureMethodCall{targetMethodCall=" + targetMethodCall + '}';
        }
    }
}

package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class TypeExtensionDelegate implements Instrumentation.Context.ExtractableView,
        AuxiliaryType.MethodAccessorFactory,
        TypeWriter.MethodPool {

    /**
     * An iterable view of a list that can be modified within the same thread without breaking
     * the iterator. Instead, the iterator will continue its iteration over the additional entries
     * that were prepended to the list.
     *
     * @param <S> The type of the list elements.
     */
    public static class SameThreadCoModifiableIterable<S> implements Iterable<S> {

        private final List<? extends S> elements;

        /**
         * Creates a new iterable view.
         *
         * @param elements The elements to be represented by this view.
         */
        public SameThreadCoModifiableIterable(List<? extends S> elements) {
            this.elements = elements;
        }

        @Override
        public Iterator<S> iterator() {
            return new SameThreadCoModifiableIterator();
        }

        private class SameThreadCoModifiableIterator implements Iterator<S> {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < elements.size();
            }

            @Override
            public S next() {
                return elements.get(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static final String DEFAULT_ACCESSOR_METHOD_PREFIX = "accessor";

    private final TypeDescription instrumentedType;
    private final ClassFileVersion classFileVersion;
    private final String accessorMethodPrefix;
    private final AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy;

    private final Map<Instrumentation.SpecialMethodInvocation, MethodDescription> registeredAccessorMethods;
    private final List<MethodDescription> orderedAccessorMethods;
    private final Map<MethodDescription, TypeWriter.MethodPool.Entry> accessorMethodEntries;

    private final Map<AuxiliaryType, DynamicType> auxiliaryTypes;

    private final Random random;

    public TypeExtensionDelegate(TypeDescription typeDescription, ClassFileVersion classFileVersion) {
        this(typeDescription,
                classFileVersion,
                DEFAULT_ACCESSOR_METHOD_PREFIX,
                new AuxiliaryTypeNamingStrategy.SuffixingRandom(DEFAULT_ACCESSOR_METHOD_PREFIX));
    }

    public TypeExtensionDelegate(TypeDescription instrumentedType,
                                 ClassFileVersion classFileVersion,
                                 String accessorMethodPrefix,
                                 AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy) {
        this.instrumentedType = instrumentedType;
        this.classFileVersion = classFileVersion;
        this.accessorMethodPrefix = accessorMethodPrefix;
        this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
        registeredAccessorMethods = new HashMap<Instrumentation.SpecialMethodInvocation, MethodDescription>();
        orderedAccessorMethods = new LinkedList<MethodDescription>();
        accessorMethodEntries = new HashMap<MethodDescription, TypeWriter.MethodPool.Entry>();
        auxiliaryTypes = new HashMap<AuxiliaryType, DynamicType>();
        random = new Random();
    }

    @Override
    public MethodDescription registerAccessorFor(Instrumentation.SpecialMethodInvocation specialMethodInvocation) {
        MethodDescription accessorMethod = registeredAccessorMethods.get(specialMethodInvocation);
        if (accessorMethod == null) {
            String name = String.format("%s$%s$%d", specialMethodInvocation.getMethodDescription().getName(),
                    accessorMethodPrefix,
                    Math.abs(random.nextInt()));
            accessorMethod = new MethodDescription.Latent(name,
                    instrumentedType,
                    specialMethodInvocation.getMethodDescription().getReturnType(),
                    specialMethodInvocation.getMethodDescription().getParameterTypes(),
                    Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL);
            registerAccessor(specialMethodInvocation, accessorMethod);
        }
        return accessorMethod;
    }

    private void registerAccessor(Instrumentation.SpecialMethodInvocation specialMethodInvocation,
                                  MethodDescription accessorMethod) {
        registeredAccessorMethods.put(specialMethodInvocation, accessorMethod);
        orderedAccessorMethods.add(accessorMethod);
        accessorMethodEntries.put(accessorMethod, new AccessorMethodDelegation(specialMethodInvocation));
    }

    public Iterable<MethodDescription> getRegisteredAccessors() {
        return new SameThreadCoModifiableIterable<MethodDescription>(orderedAccessorMethods);
    }

    @Override
    public TypeWriter.MethodPool.Entry target(MethodDescription methodDescription) {
        TypeWriter.MethodPool.Entry targetMethodCall = accessorMethodEntries.get(methodDescription);
        if (targetMethodCall == null) {
            throw new IllegalArgumentException("Unknown accessor method: " + methodDescription);
        }
        return targetMethodCall;
    }

    @Override
    public TypeDescription register(AuxiliaryType auxiliaryType) {
        DynamicType dynamicType = auxiliaryTypes.get(auxiliaryType);
        if (dynamicType == null) {
            dynamicType = auxiliaryType.make(auxiliaryTypeNamingStrategy.name(auxiliaryType, instrumentedType),
                    classFileVersion,
                    this);
            auxiliaryTypes.put(auxiliaryType, dynamicType);
        }
        return dynamicType.getDescription();
    }

    @Override
    public List<DynamicType> getRegisteredAuxiliaryTypes() {
        return new ArrayList<DynamicType>(auxiliaryTypes.values());
    }

    /**
     * Representation of a naming strategy for an auxiliary type.
     */
    public static interface AuxiliaryTypeNamingStrategy {

        static class SuffixingRandom implements AuxiliaryTypeNamingStrategy {

            private final String suffix;
            private final Random random;

            public SuffixingRandom(String suffix) {
                this.suffix = suffix;
                random = new Random();
            }

            @Override
            public String name(AuxiliaryType auxiliaryType, TypeDescription instrumentedType) {
                return String.format("%s$%s$%d", instrumentedType.getName(), suffix, Math.abs(random.nextInt()));
            }
        }

        /**
         * NAmes an auxiliary type.
         *
         * @param auxiliaryType The auxiliary type to name.
         * @return The fully qualified name for the given auxiliary type.
         */
        String name(AuxiliaryType auxiliaryType, TypeDescription instrumentedType);
    }

    private static class AccessorMethodDelegation implements TypeWriter.MethodPool.Entry, ByteCodeAppender {

        private final StackManipulation accessorMethodInvocation;

        private AccessorMethodDelegation(StackManipulation accessorMethodInvocation) {
            this.accessorMethodInvocation = accessorMethodInvocation;
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
                    accessorMethodInvocation,
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
                    && accessorMethodInvocation.equals(((AccessorMethodDelegation) other).accessorMethodInvocation);
        }

        @Override
        public int hashCode() {
            return accessorMethodInvocation.hashCode();
        }

        @Override
        public String toString() {
            return "AccessorMethodDelegation{accessorMethodInvocation=" + accessorMethodInvocation + '}';
        }
    }
}

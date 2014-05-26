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

/**
 * This delegate offers a default implementation of an instrumentation context and a method accessor factory.
 * For convenience, all extension information is stored in a format that allows using this class as a method pool
 * for defining any accessor methods on the accessed type. Note that this delegate represents a mutable structure
 * where registrations cannot be revoked.
 */
public class TypeExtensionDelegate implements Instrumentation.Context.ExtractableView,
        AuxiliaryType.MethodAccessorFactory,
        TypeWriter.MethodPool {

    private static final String DEFAULT_ACCESSOR_METHOD_SUFFIX = "accessor";
    private final TypeDescription instrumentedType;
    private final ClassFileVersion classFileVersion;
    private final String accessorMethodSuffix;
    private final AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy;
    private final Map<Instrumentation.SpecialMethodInvocation, MethodDescription> registeredAccessorMethods;
    private final List<MethodDescription> orderedAccessorMethods;
    private final Map<MethodDescription, TypeWriter.MethodPool.Entry> accessorMethodEntries;
    private final Map<AuxiliaryType, DynamicType> auxiliaryTypes;
    private final Random random;

    /**
     * Creates a new delegate. This constructor implicitly defines default naming strategies for created accessor
     * method and registered auxiliary types.
     *
     * @param instrumentedType The description of the type that is currently subject of creation.
     * @param classFileVersion The class file version of the created class.
     */
    public TypeExtensionDelegate(TypeDescription instrumentedType, ClassFileVersion classFileVersion) {
        this(instrumentedType,
                classFileVersion,
                DEFAULT_ACCESSOR_METHOD_SUFFIX,
                new AuxiliaryTypeNamingStrategy.SuffixingRandom(DEFAULT_ACCESSOR_METHOD_SUFFIX));
    }

    /**
     * Creates a new delegate.
     *
     * @param instrumentedType            The description of the type that is currently subject of creation.
     * @param classFileVersion            The class file version of the created class.
     * @param accessorMethodSuffix        A suffix that is added to any accessor method where the method name is prefixed by
     *                                    the accessed method's name.
     * @param auxiliaryTypeNamingStrategy The naming strategy for naming an auxiliary type.
     */
    public TypeExtensionDelegate(TypeDescription instrumentedType,
                                 ClassFileVersion classFileVersion,
                                 String accessorMethodSuffix,
                                 AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy) {
        this.instrumentedType = instrumentedType;
        this.classFileVersion = classFileVersion;
        this.accessorMethodSuffix = accessorMethodSuffix;
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
                    accessorMethodSuffix,
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

    /**
     * Returns an iterable of the registered accessors. The returned iterator can be modified during a running
     * iteration. This way, this instance can be used safely as an instrumentation context even for implementing
     * the accessor methods.
     *
     * @return A co-modifiable iterable of all accessor methods that were registered on this instance.
     */
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

        /**
         * NAmes an auxiliary type.
         *
         * @param auxiliaryType    The auxiliary type to name.
         * @param instrumentedType The instrumented type for which an auxiliary type is registered.
         * @return The fully qualified name for the given auxiliary type.
         */
        String name(AuxiliaryType auxiliaryType, TypeDescription instrumentedType);

        /**
         * A naming strategy for an auxiliary type which returns the instrumented type's name with a fixed extension
         * and a random number as a suffix. All generated names will be in the same package as the instrumented type.
         */
        static class SuffixingRandom implements AuxiliaryTypeNamingStrategy {

            private final String suffix;
            private final Random random;

            /**
             * Creates a new suffixing random naming strategy.
             *
             * @param suffix The suffix to extend to the instrumented type.
             */
            public SuffixingRandom(String suffix) {
                this.suffix = suffix;
                random = new Random();
            }

            @Override
            public String name(AuxiliaryType auxiliaryType, TypeDescription instrumentedType) {
                return String.format("%s$%s$%d", instrumentedType.getName(), suffix, Math.abs(random.nextInt()));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && suffix.equals(((SuffixingRandom) other).suffix);
            }

            @Override
            public int hashCode() {
                return suffix.hashCode();
            }

            @Override
            public String toString() {
                return "TypeExtensionDelegate.AuxiliaryTypeNamingStrategySuffixingRandom{" +
                        "suffix='" + suffix + '\'' +
                        '}';
            }
        }
    }

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

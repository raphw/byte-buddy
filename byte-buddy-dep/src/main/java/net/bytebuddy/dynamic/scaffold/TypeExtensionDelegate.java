package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
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
        TypeWriter.MethodPool,
        TypeWriter.FieldPool {

    /**
     * The default name suffix to be appended to an accessor method.
     */
    private static final String DEFAULT_ACCESSOR_METHOD_SUFFIX = "accessor";

    /**
     * The default name suffix to be prepended to a cache field.
     */
    private static final String DEFAULT_FIELD_CACHE_PREFIX = "cachedValue";

    /**
     * The instrumented type that this instance represents.
     */
    private final TypeDescription instrumentedType;

    /**
     * The class file version that the instrumented type is written in.
     */
    private final ClassFileVersion classFileVersion;

    /**
     * The name suffix to be appended to an accessor method.
     */
    private final String accessorMethodSuffix;

    /**
     * The name prefix to be prepended to a cache field.
     */
    private final String fieldCachePrefix;

    /**
     * The naming strategy for naming auxiliary types that are registered.
     */
    private final AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy;

    /**
     * A mapping of special method invocations to their accessor methods that each invoke their mapped invocation.
     */
    private final Map<Instrumentation.SpecialMethodInvocation, MethodDescription> registeredAccessorMethods;

    /**
     * An list of accessor methods in the order of their registration.
     */
    private final List<MethodDescription> orderedAccessorMethods;

    /**
     * A map of accessor methods to a method pool entry that represents their implementation.
     */
    private final Map<MethodDescription, TypeWriter.MethodPool.Entry> accessorMethodEntries;

    /**
     * A map of registered auxiliary types to their dynamic type representation.
     */
    private final Map<AuxiliaryType, DynamicType> auxiliaryTypes;

    /**
     * A map of already registered field caches to their field representation.
     */
    private final Map<FieldCacheEntry, FieldDescription> registeredFieldCacheEntries;

    /**
     * An instance for supporting the creation of random values.
     */
    private final Random random;

    /**
     * A marker that determines if this instance is still capable of defining field caches. Field caches need to
     * be initiated explicitly in a class's type initializer. As a consequence, no field caches can be defined during
     * or after the type initializer ({@code &gt;clinit&lt;} has been written to a given class. This marker is
     * set to {@code false} once the
     * {@link net.bytebuddy.dynamic.scaffold.TypeExtensionDelegate#wrapForTypeInitializerInterception(net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool)}
     * is called. It is the responsibility of this instance's user to write the type initializer as late as possibly.
     * <p>&nbsp;</p>
     * This might appear to be a limitation of the library at first look. However, caching values in a type initializer
     * does not make sense. The type initializer is invokes once at most once which makes field caching of values that
     * are only used in this initializer unnecessary. Instead, field caches should only be defined in other methods
     * or constructors.
     */
    private boolean canRegisterFieldCache;

    /**
     * Creates a new delegate. This constructor implicitly defines default naming strategies for created accessor
     * method and registered auxiliary types.
     *
     * @param instrumentedType The description of the type that is currently subject of creation.
     * @param classFileVersion The class file version of the created class.
     */
    public TypeExtensionDelegate(TypeDescription instrumentedType,
                                 ClassFileVersion classFileVersion) {
        this(instrumentedType,
                classFileVersion,
                DEFAULT_ACCESSOR_METHOD_SUFFIX,
                DEFAULT_FIELD_CACHE_PREFIX,
                new AuxiliaryTypeNamingStrategy.SuffixingRandom(DEFAULT_ACCESSOR_METHOD_SUFFIX));
    }

    /**
     * Creates a new delegate.
     *
     * @param instrumentedType            The description of the type that is currently subject of creation.
     * @param classFileVersion            The class file version of the created class.
     * @param accessorMethodSuffix        A suffix that is added to any accessor method where the method name is
     *                                    prefixed by the accessed method's name.
     * @param fieldCachePrefix            A prefix that is added to any field cache.
     * @param auxiliaryTypeNamingStrategy The naming strategy for naming an auxiliary type.
     */
    public TypeExtensionDelegate(TypeDescription instrumentedType,
                                 ClassFileVersion classFileVersion,
                                 String accessorMethodSuffix,
                                 String fieldCachePrefix,
                                 AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy) {
        this.instrumentedType = instrumentedType;
        this.classFileVersion = classFileVersion;
        this.accessorMethodSuffix = accessorMethodSuffix;
        this.fieldCachePrefix = fieldCachePrefix;
        this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
        registeredAccessorMethods = new HashMap<Instrumentation.SpecialMethodInvocation, MethodDescription>();
        orderedAccessorMethods = new LinkedList<MethodDescription>();
        accessorMethodEntries = new HashMap<MethodDescription, TypeWriter.MethodPool.Entry>();
        auxiliaryTypes = new HashMap<AuxiliaryType, DynamicType>();
        registeredFieldCacheEntries = new HashMap<FieldCacheEntry, FieldDescription>();
        random = new Random();
        canRegisterFieldCache = true;
    }

    @Override
    public MethodDescription registerAccessorFor(Instrumentation.SpecialMethodInvocation specialMethodInvocation) {
        MethodDescription accessorMethod = registeredAccessorMethods.get(specialMethodInvocation);
        if (accessorMethod == null) {
            String name = String.format("%s$%s$%d", specialMethodInvocation.getMethodDescription().getInternalName(),
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

    /**
     * Registers a new accessor method.
     *
     * @param specialMethodInvocation The special method invocation that the accessor method should invoke.
     * @param accessorMethod          The accessor method for this invocation.
     */
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

    @Override
    public FieldDescription cache(StackManipulation fieldValue, TypeDescription fieldType) {
        FieldCacheEntry fieldCacheEntry = new FieldCacheEntry(fieldValue, fieldType);
        FieldDescription fieldCache = registeredFieldCacheEntries.get(fieldCacheEntry);
        if (fieldCache != null) {
            return fieldCache;
        }
        validateFieldCacheAccessibility();
        fieldCache = new FieldDescription.Latent(String.format("%s$%d", fieldCachePrefix, Math.abs(random.nextInt())),
                instrumentedType,
                fieldType,
                Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC);
        registeredFieldCacheEntries.put(fieldCacheEntry, fieldCache);
        return fieldCache;
    }

    /**
     * Validates that the field cache is still accessible. Once the type initializer of a class is written, no
     * additional field caches can be defined. See
     * {@link net.bytebuddy.dynamic.scaffold.TypeExtensionDelegate#canRegisterFieldCache} for a more detailed
     * explanation of this validation.
     */
    private void validateFieldCacheAccessibility() {
        if (!canRegisterFieldCache) {
            throw new IllegalStateException("A field cache cannot be registered during or after the creation of a " +
                    "type initializer - instead, the field cache should be registered in the method that requires " +
                    "the cached value");
        }
    }

    @Override
    public List<FieldDescription> getRegisteredFieldCaches() {
        return new ArrayList<FieldDescription>(registeredFieldCacheEntries.values());
    }

    @Override
    public TypeWriter.FieldPool.Entry target(FieldDescription fieldDescription) {
        return TypeWriter.FieldPool.Entry.NoOp.INSTANCE;
    }

    /**
     * Wraps the given method pool in order to prepend any initializations of field caches. At the same time,
     * invoking this method disables the field cache for further use as it is not possible to define further
     * cached values from a type initializer once this initializer is written.
     *
     * @param methodPool The method pool that contains user interceptions which needs to be wrapped.
     * @return A method pool that represents this user method pool but is capable of defining field caches.
     */
    public TypeWriter.MethodPool wrapForTypeInitializerInterception(TypeWriter.MethodPool methodPool) {
        canRegisterFieldCache = false;
        return new FieldCacheWritingMethodPool(methodPool);
    }

    @Override
    public String toString() {
        return "TypeExtensionDelegate{" +
                "instrumentedType=" + instrumentedType +
                ", classFileVersion=" + classFileVersion +
                ", accessorMethodSuffix='" + accessorMethodSuffix + '\'' +
                ", fieldCachePrefix='" + fieldCachePrefix + '\'' +
                ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                ", registeredAccessorMethods=" + registeredAccessorMethods +
                ", orderedAccessorMethods=" + orderedAccessorMethods +
                ", accessorMethodEntries=" + accessorMethodEntries +
                ", auxiliaryTypes=" + auxiliaryTypes +
                ", registeredFieldCacheEntries=" + registeredFieldCacheEntries +
                ", random=" + random +
                ", canRegisterFieldCache=" + canRegisterFieldCache +
                '}';
    }

    /**
     * A {@link net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender} for returning from the type
     * initializer. This byte code appender is used as a default appender for the type initializer if no user-defined
     * type initializer was defined.
     */
    private static enum TypeInitializerReturn implements ByteCodeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            return new Size(MethodReturn.VOID.apply(methodVisitor, instrumentationContext).getMaximalSize(),
                    instrumentedMethod.getStackSize());
        }

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

            /**
             * The suffix to append to the instrumented type for creating names for the auxiliary types.
             */
            private final String suffix;

            /**
             * An instance for creating random values.
             */
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
                return "TypeExtensionDelegate.AuxiliaryTypeNamingStrategySuffixingRandom{suffix='" + suffix + '\'' + '}';
            }
        }
    }

    /**
     * A field cache entry for uniquely identifying a cached field. A cached field is described by the stack
     * manipulation that loads the field's value onto the operand stack and the type of the field.
     */
    private static class FieldCacheEntry {

        /**
         * The field value that is represented by this field cache entry.
         */
        private final StackManipulation fieldValue;

        /**
         * The field type that is represented by this field cache entry.
         */
        private final TypeDescription fieldType;

        /**
         * Creates a new field cache entry.
         *
         * @param fieldValue The field value that is represented by this field cache entry.
         * @param fieldType  The field type that is represented by this field cache entry.
         */
        private FieldCacheEntry(StackManipulation fieldValue, TypeDescription fieldType) {
            this.fieldValue = fieldValue;
            this.fieldType = fieldType;
        }

        /**
         * Returns the field value that is represented by this field cache entry.
         *
         * @return The field value that is represented by this field cache entry.
         */
        public StackManipulation getFieldValue() {
            return fieldValue;
        }

        /**
         * Returns the field type that is represented by this field cache entry.
         *
         * @return The field type that is represented by this field cache entry.
         */
        public TypeDescription getFieldType() {
            return fieldType;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && fieldType.equals(((FieldCacheEntry) other).fieldType)
                    && fieldValue.equals(((FieldCacheEntry) other).fieldValue);
        }

        @Override
        public int hashCode() {
            return 31 * fieldValue.hashCode() + fieldType.hashCode();
        }

        @Override
        public String toString() {
            return "TypeExtensionDelegate.FieldCacheEntry{" +
                    "fieldValue=" + fieldValue +
                    ", fieldType=" + fieldType +
                    '}';
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

        /**
         * A possibly mutable list of the elements this iterable represents.
         */
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
        public java.util.Iterator<S> iterator() {
            return new Iterator();
        }

        @Override
        public String toString() {
            return "TypeExtensionDelegate.SameThreadCoModifiableIterable{elements=" + elements + '}';
        }

        /**
         * The iterator over a {@link net.bytebuddy.dynamic.scaffold.TypeExtensionDelegate.SameThreadCoModifiableIterable}.
         */
        private class Iterator implements java.util.Iterator<S> {

            /**
             * The current index of this iteration.
             */
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

            @Override
            public String toString() {
                return "TypeExtensionDelegate.SameThreadCoModifiableIterable.Iterator{" +
                        "iterable=" + SameThreadCoModifiableIterable.this +
                        ", index=" + index +
                        '}';
            }
        }
    }

    /**
     * An implementation of a {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry} for implementing
     * an accessor method.
     */
    private static class AccessorMethodDelegation implements TypeWriter.MethodPool.Entry, ByteCodeAppender {

        /**
         * The stack manipulation that represents the requested special method invocation.
         */
        private final StackManipulation accessorMethodInvocation;

        /**
         * Creates a new accessor method delegation.
         *
         * @param accessorMethodInvocation The stack manipulation that represents the requested special method
         *                                 invocation.
         */
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
            return "TypeExtensionDelegate.AccessorMethodDelegation{accessorMethodInvocation=" + accessorMethodInvocation + '}';
        }
    }

    /**
     * A method pool that prepends the initialization of any field caches to the actual instrumentation of a dynamic
     * type's type initializer, if any. If the wrapped method pool does not define an instrumentation for the type
     * initializer, this method pool adds an appropriate return statement to the end of the type initializer.
     */
    private class FieldCacheWritingMethodPool implements TypeWriter.MethodPool, ByteCodeAppender {

        /**
         * The method pool that is wrapped by this instance.
         */
        private final TypeWriter.MethodPool wrappedMethodPool;

        /**
         * Creates a new field cache writing method pool that is to be applied only for type initializers.
         *
         * @param wrappedMethodPool The method pool that is wrapped by this instance.
         */
        private FieldCacheWritingMethodPool(TypeWriter.MethodPool wrappedMethodPool) {
            this.wrappedMethodPool = wrappedMethodPool;
        }

        @Override
        public Entry target(MethodDescription methodDescription) {
            if (!methodDescription.isTypeInitializer()) {
                throw new IllegalArgumentException(this + " must only be applied to a type initializer");
            }
            Entry wrappedEntry = wrappedMethodPool.target(methodDescription);
            if (registeredFieldCacheEntries.isEmpty()) {
                return wrappedEntry;
            } else if (!wrappedEntry.isDefineMethod()) {
                return new Entry.Simple(new ByteCodeAppender.Compound(this, TypeInitializerReturn.INSTANCE),
                        MethodAttributeAppender.NoOp.INSTANCE);
            } else {
                return new Entry.Simple(new ByteCodeAppender.Compound(this, wrappedEntry.getByteCodeAppender()),
                        wrappedEntry.getAttributeAppender());
            }
        }

        @Override
        public boolean appendsCode() {
            return !registeredFieldCacheEntries.isEmpty();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            StackManipulation[] fieldInitialization = new StackManipulation[registeredFieldCacheEntries.size()];
            int currentIndex = 0;
            for (Map.Entry<FieldCacheEntry, FieldDescription> entry : registeredFieldCacheEntries.entrySet()) {
                fieldInitialization[currentIndex++] = new StackManipulation.Compound(entry.getKey().getFieldValue(),
                        FieldAccess.forField(entry.getValue()).putter());
            }
            return new Size(new StackManipulation.Compound(fieldInitialization)
                    .apply(methodVisitor, instrumentationContext).getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public String toString() {
            return "TypeExtensionDelegate.FieldCacheWritingMethodPool{" +
                    "typeExtensionDelegate=" + TypeExtensionDelegate.this +
                    "wrappedMethodPool=" + wrappedMethodPool +
                    '}';
        }
    }
}

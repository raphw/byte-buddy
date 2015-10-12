package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * An implementation is responsible for implementing methods of a dynamically created type as byte code. An
 * implementation is applied in two stages:
 * <ol>
 * <li>The implementation is able to prepare an instrumented type by adding fields and/or helper methods that are
 * required for the methods implemented by this implementation. Furthermore,
 * {@link LoadedTypeInitializer}s  and byte code for the type initializer can be registered for the instrumented
 * type.</li>
 * <li>Any implementation is required to supply a byte code appender that is responsible for providing the byte code
 * to the instrumented methods that were delegated to this implementation. This byte code appender is also
 * be responsible for providing implementations for the methods added in step <i>1</i>.</li>
 * </ol>
 * <p>&nbsp;</p>
 * An implementation should provide meaningful implementations of both {@link java.lang.Object#equals(Object)}
 * and {@link Object#hashCode()} if it wants to avoid to be used twice within the creation of a dynamic type. For two
 * equal implementations only one will be applied on the creation of a dynamic type.
 */
public interface Implementation {

    /**
     * During the preparation phase of an implementation, implementations are eligible to adding fields or methods
     * to the currently instrumented type. All methods that are added by this implementation are required to be
     * implemented by the {@link net.bytebuddy.implementation.bytecode.ByteCodeAppender} that is emitted
     * on the call to
     * {@link Implementation#appender(Implementation.Target)}
     * call. On this method call, loaded type initializers can also be added to the instrumented type.
     *
     * @param instrumentedType The instrumented type that is the basis of the ongoing instrumentation.
     * @return The instrumented type with any applied changes, if any.
     */
    InstrumentedType prepare(InstrumentedType instrumentedType);

    /**
     * Creates a byte code appender that determines the implementation of the instrumented type's methods.
     *
     * @param implementationTarget The target of the current implementation.
     * @return A byte code appender for implementing methods delegated to this implementation. This byte code appender
     * is also responsible for handling methods that were added by this implementation on the call to
     * {@link Implementation#prepare(InstrumentedType)}.
     */
    ByteCodeAppender appender(Target implementationTarget);

    /**
     * Represents an type-specific method invocation on the current instrumented type which is not legal from outside
     * the type such as a super method or default method invocation. Legal instances of special method invocations must
     * be equal to one another if they represent the same invocation target.
     */
    interface SpecialMethodInvocation extends StackManipulation {

        /**
         * Returns the method that represents this special method invocation. This method can be different even for
         * equal special method invocations, dependant on the method that was used to request such an invocation by the
         * means of a {@link Implementation.Target}.
         *
         * @return The method description that describes this instances invocation target.
         */
        MethodDescription getMethodDescription();

        /**
         * Returns the target type the represented method is invoked on.
         *
         * @return The type the represented method is invoked on.
         */
        TypeDescription getTypeDescription();

        /**
         * A canonical implementation of an illegal {@link Implementation.SpecialMethodInvocation}.
         */
        enum Illegal implements SpecialMethodInvocation {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                throw new IllegalStateException("Cannot implement an undefined method");
            }

            @Override
            public MethodDescription getMethodDescription() {
                throw new IllegalStateException("An illegal special method invocation must not be applied");
            }

            @Override
            public TypeDescription getTypeDescription() {
                throw new IllegalStateException("An illegal special method invocation must not be applied");
            }

            @Override
            public String toString() {
                return "Implementation.SpecialMethodInvocation.Illegal." + name();
            }
        }

        /**
         * An abstract base implementation of a valid special method invocation.
         */
        abstract class AbstractBase implements SpecialMethodInvocation {

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public int hashCode() {
                return 31 * getMethodDescription().asToken().hashCode() + getTypeDescription().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof SpecialMethodInvocation)) return false;
                SpecialMethodInvocation specialMethodInvocation = (SpecialMethodInvocation) other;
                return getMethodDescription().asToken().equals(specialMethodInvocation.getMethodDescription().asToken())
                        && getTypeDescription().equals(((SpecialMethodInvocation) other).getTypeDescription());
            }
        }

        /**
         * A canonical implementation of a {@link Implementation.SpecialMethodInvocation}.
         */
        class Simple extends SpecialMethodInvocation.AbstractBase {

            /**
             * The method description that is represented by this legal special method invocation.
             */
            private final MethodDescription methodDescription;

            /**
             * The type description that is represented by this legal special method invocation.
             */
            private final TypeDescription typeDescription;

            /**
             * A stack manipulation representing the method's invocation on the type description.
             */
            private final StackManipulation stackManipulation;

            /**
             * Creates a new legal special method invocation.
             *
             * @param methodDescription The method that represents the special method invocation.
             * @param typeDescription   The type on which the method should be invoked on by an {@code INVOKESPECIAL}
             *                          invocation.
             * @param stackManipulation The stack manipulation that represents this special method invocation.
             */
            protected Simple(MethodDescription methodDescription, TypeDescription typeDescription, StackManipulation stackManipulation) {
                this.methodDescription = methodDescription;
                this.typeDescription = typeDescription;
                this.stackManipulation = stackManipulation;
            }

            /**
             * Creates a special method invocation for a given invocation target.
             *
             * @param methodDescription The method that represents the special method invocation.
             * @param typeDescription   The type on which the method should be invoked on by an {@code INVOKESPECIAL}
             *                          invocation.
             * @return A special method invocation representing a legal invocation if the method can be invoked
             * specially on the target type or an illegal invocation if this is not possible.
             */
            public static SpecialMethodInvocation of(MethodDescription methodDescription, TypeDescription typeDescription) {
                StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).special(typeDescription);
                return stackManipulation.isValid()
                        ? new Simple(methodDescription, typeDescription, stackManipulation)
                        : SpecialMethodInvocation.Illegal.INSTANCE;
            }

            @Override
            public MethodDescription getMethodDescription() {
                return methodDescription;
            }

            @Override
            public TypeDescription getTypeDescription() {
                return typeDescription;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                return stackManipulation.apply(methodVisitor, implementationContext);
            }

            @Override
            public String toString() {
                return "Instrumentation.SpecialMethodInvocation.Simple{" +
                        "typeDescription=" + typeDescription +
                        ", methodDescription=" + methodDescription +
                        '}';
            }
        }
    }

    /**
     * The target of an implementation. Implementation targets must be immutable and can be queried without altering
     * the implementation result. An implementation target provides information on the type that is to be created
     * where it is the implementation's responsibility to cache expensive computations, especially such computations
     * that require reflective look-up.
     */
    interface Target {

        /**
         * Returns a description of the instrumented type.
         *
         * @return A description of the instrumented type.
         */
        TypeDescription getTypeDescription();

        /**
         * Identifies the origin type of an implementation. The origin type describes the type that is subject to
         * any form of enhancement. If a subclass of a given type is generated, the base type of this subclass
         * describes the origin type. If a given type is redefined or rebased, the origin type is described by the
         * instrumented type itself.
         *
         * @return The origin type of this implementation.
         */
        TypeDescription getOriginType();

        /**
         * Creates a special method invocation for invoking the super method of the given method.
         *
         * @param methodToken A token of the method that is to be invoked as a super method.
         * @return The corresponding special method invocation which might be illegal if the requested invocation is
         * not legal.
         */
        SpecialMethodInvocation invokeSuper(MethodDescription.Token methodToken);

        /**
         * Creates a special method invocation for invoking a default method.
         *
         * @param targetType  The interface on which the default method is to be invoked.
         * @param methodToken A token that uniquely describes the method to invoke.
         * @return The corresponding special method invocation which might be illegal if the requested invocation is
         * not legal.
         */
        SpecialMethodInvocation invokeDefault(TypeDescription targetType, MethodDescription.Token methodToken);

        /**
         * Invokes a dominant method, i.e. if the method token can be invoked as a super method invocation, this invocation is considered dominant.
         * Alternatively, a method invocation is attempted on an interface type as a default method invocation only if this invocation is not ambiguous
         * for several interfaces.
         *
         * @param methodToken The method token representing the method to be invoked.
         * @return A special method invocation for a method representing the method token.
         */
        SpecialMethodInvocation invokeDominant(MethodDescription.Token methodToken);

        /**
         * A factory for creating an {@link Implementation.Target}.
         */
        interface Factory {

            /**
             * Creates an implementation target.
             *
             * @param instrumentedType The instrumented type.
             * @param methodGraph      A method graph of the instrumented type.
             * @return An implementation target for the instrumented type.
             */
            Target make(TypeDescription instrumentedType, MethodGraph.Linked methodGraph);
        }

        /**
         * An abstract base implementation for an {@link Implementation.Target}.
         */
        abstract class AbstractBase implements Target {

            /**
             * The instrumented type.
             */
            protected final TypeDescription instrumentedType;

            /**
             * The instrumented type's method graph.
             */
            protected final MethodGraph.Linked methodGraph;

            /**
             * Creates a new implementation target.
             *
             * @param instrumentedType The instrumented type.
             * @param methodGraph      The instrumented type's method graph.
             */
            protected AbstractBase(TypeDescription instrumentedType, MethodGraph.Linked methodGraph) {
                this.instrumentedType = instrumentedType;
                this.methodGraph = methodGraph;
            }

            @Override
            public TypeDescription getTypeDescription() {
                return instrumentedType;
            }

            @Override
            public Implementation.SpecialMethodInvocation invokeDefault(TypeDescription targetType, MethodDescription.Token methodToken) {
                MethodGraph.Node node = methodGraph.getInterfaceGraph(targetType).locate(methodToken);
                return node.getSort().isUnique()
                        ? SpecialMethodInvocation.Simple.of(node.getRepresentative(), targetType)
                        : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
            }

            @Override
            public SpecialMethodInvocation invokeDominant(MethodDescription.Token methodToken) {
                SpecialMethodInvocation specialMethodInvocation = invokeSuper(methodToken);
                if (!specialMethodInvocation.isValid()) {
                    Iterator<TypeDescription> iterator = instrumentedType.getInterfaces().asErasures().iterator();
                    while (!specialMethodInvocation.isValid() && iterator.hasNext()) {
                        specialMethodInvocation = invokeDefault(iterator.next(), methodToken);
                    }
                    while (iterator.hasNext()) {
                        if (invokeDefault(iterator.next(), methodToken).isValid()) {
                            return SpecialMethodInvocation.Illegal.INSTANCE;
                        }
                    }
                }
                return specialMethodInvocation;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                AbstractBase that = (AbstractBase) other;
                return instrumentedType.equals(that.instrumentedType)
                        && methodGraph.equals(that.methodGraph);
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + methodGraph.hashCode();
                return result;
            }
        }
    }

    /**
     * The context for an implementation application. An implementation context represents a mutable data structure
     * where any registration is irrevocable. Calling methods on an implementation context should be considered equally
     * sensitive as calling a {@link org.objectweb.asm.MethodVisitor}. As such, an implementation context and a
     * {@link org.objectweb.asm.MethodVisitor} are complementary for creating an new Java type.
     */
    interface Context {

        /**
         * Registers an auxiliary type as required for the current implementation. Registering a type will cause the
         * creation of this type even if this type is not effectively used for the current implementation.
         *
         * @param auxiliaryType The auxiliary type that is required for the current implementation.
         * @return A description of the registered auxiliary type.
         */
        TypeDescription register(AuxiliaryType auxiliaryType);

        /**
         * Caches a single value by storing it in form of a {@code private}, {@code final} and {@code static} field.
         * By caching values, expensive instance creations can be avoided and object identity can be preserved.
         * The field is initiated in a generated class's static initializer.
         *
         * @param fieldValue A stack manipulation for creating the value that is to be cached in a {@code static} field.
         *                   After executing the stack manipulation, exactly one value must be put onto the operand
         *                   stack which is assignable to the given {@code fieldType}.
         * @param fieldType  The type of the field for storing the cached value. This field's type determines the value
         *                   that is put onto the operand stack by this method's returned stack manipulation.
         * @return A description of a field that was defined on the instrumented type which contains the given value.
         */
        FieldDescription cache(StackManipulation fieldValue, TypeDescription fieldType);

        /**
         * Represents an extractable view of an {@link Implementation.Context} which
         * allows the retrieval of any registered auxiliary type.
         */
        interface ExtractableView extends Context {

            /**
             * Returns any {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType} that was registered
             * with this {@link Implementation.Context}.
             *
             * @return A list of all manifested registered auxiliary types.
             */
            List<DynamicType> getRegisteredAuxiliaryTypes();

            /**
             * Writes any information that was registered with an {@link Implementation.Context}
             * to the provided class visitor. This contains any fields for value caching, any accessor method and it
             * writes the type initializer. The type initializer must therefore never be written manually.
             *
             * @param classVisitor The class visitor to which the extractable view is to be written.
             * @param methodPool   A method pool which is queried for any user code to add to the type initializer.
             * @param injectedCode Potential code that is to be injected into the type initializer.
             */
            void drain(ClassVisitor classVisitor, TypeWriter.MethodPool methodPool, InjectedCode injectedCode);

            /**
             * When draining an implementation context, a type initializer might be written to the created class
             * file. If any code must be explicitly invoked from within the type initializer, this can be achieved
             * by providing a code injection by this instance. The injected code is added after the class is set up but
             * before any user code is run from within the type initializer.
             */
            interface InjectedCode {

                /**
                 * Returns a byte code appender for appending the injected code.
                 *
                 * @return A byte code appender for appending the injected code.
                 */
                ByteCodeAppender getByteCodeAppender();

                /**
                 * Checks if there is actually code defined to be injected.
                 *
                 * @return {@code true} if code is to be injected.
                 */
                boolean isDefined();

                /**
                 * A canonical implementation of non-applicable injected code.
                 */
                enum None implements InjectedCode {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public ByteCodeAppender getByteCodeAppender() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public boolean isDefined() {
                        return false;
                    }

                    @Override
                    public String toString() {
                        return "Implementation.Context.ExtractableView.InjectedCode.None." + name();
                    }
                }
            }
        }

        /**
         * A default implementation of an {@link Implementation.Context.ExtractableView}
         * which serves as its own {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType.MethodAccessorFactory}.
         */
        class Default implements Implementation.Context.ExtractableView, AuxiliaryType.MethodAccessorFactory {

            /**
             * The name suffix to be appended to an accessor method.
             */
            public static final String ACCESSOR_METHOD_SUFFIX = "accessor";

            /**
             * The name prefix to be prepended to a field storing a cached value.
             */
            public static final String FIELD_CACHE_PREFIX = "cachedValue";

            /**
             * The instrumented type that this instance represents.
             */
            private final TypeDescription instrumentedType;

            /**
             * The type initializer of the created instrumented type.
             */
            private final InstrumentedType.TypeInitializer typeInitializer;

            /**
             * The class file version that the instrumented type is written in.
             */
            private final ClassFileVersion classFileVersion;

            /**
             * The naming strategy for naming auxiliary types that are registered.
             */
            private final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

            /**
             * A mapping of special method invocations to their accessor methods that each invoke their mapped invocation.
             */
            private final Map<Implementation.SpecialMethodInvocation, MethodDescription.InDefinedShape> registeredAccessorMethods;

            /**
             * The registered getters.
             */
            private final Map<FieldDescription, MethodDescription.InDefinedShape> registeredGetters;

            /**
             * The registered setters.
             */
            private final Map<FieldDescription, MethodDescription.InDefinedShape> registeredSetters;

            /**
             * A map of accessor methods to a method pool entry that represents their implementation.
             */
            private final List<TypeWriter.MethodPool.Record> accessorMethods;

            /**
             * A map of registered auxiliary types to their dynamic type representation.
             */
            private final Map<AuxiliaryType, DynamicType> auxiliaryTypes;

            /**
             * A map of already registered field caches to their field representation.
             */
            private final Map<FieldCacheEntry, FieldDescription> registeredFieldCacheEntries;

            /**
             * A random suffix to append to the names of accessor methods.
             */
            private final String suffix;

            /**
             * Signals if this type extension delegate is still capable of registering field cache entries. Such entries
             * must be explicitly initialized in the instrumented type's type initializer such that no entries can be
             * registered after the type initializer was written.
             */
            private boolean canRegisterFieldCache;

            /**
             * Creates a new implementation context.
             *
             * @param instrumentedType            The description of the type that is currently subject of creation.
             * @param auxiliaryTypeNamingStrategy The naming strategy for naming an auxiliary type.
             * @param typeInitializer             The type initializer of the created instrumented type.
             * @param classFileVersion            The class file version of the created class.
             */
            public Default(TypeDescription instrumentedType,
                           AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                           InstrumentedType.TypeInitializer typeInitializer,
                           ClassFileVersion classFileVersion) {
                this.instrumentedType = instrumentedType;
                this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
                this.typeInitializer = typeInitializer;
                this.classFileVersion = classFileVersion;
                registeredAccessorMethods = new HashMap<Implementation.SpecialMethodInvocation, MethodDescription.InDefinedShape>();
                registeredGetters = new HashMap<FieldDescription, MethodDescription.InDefinedShape>();
                registeredSetters = new HashMap<FieldDescription, MethodDescription.InDefinedShape>();
                accessorMethods = new LinkedList<TypeWriter.MethodPool.Record>();
                auxiliaryTypes = new HashMap<AuxiliaryType, DynamicType>();
                registeredFieldCacheEntries = new HashMap<FieldCacheEntry, FieldDescription>();
                suffix = RandomString.make();
                canRegisterFieldCache = true;
            }

            @Override
            public MethodDescription.InDefinedShape registerAccessorFor(Implementation.SpecialMethodInvocation specialMethodInvocation) {
                MethodDescription.InDefinedShape accessorMethod = registeredAccessorMethods.get(specialMethodInvocation);
                if (accessorMethod == null) {
                    accessorMethod = new AccessorMethod(instrumentedType, specialMethodInvocation.getMethodDescription(), suffix);
                    registeredAccessorMethods.put(specialMethodInvocation, accessorMethod);
                    accessorMethods.add(new AccessorMethodDelegation(accessorMethod, specialMethodInvocation));
                }
                return accessorMethod;
            }

            @Override
            public MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription) {
                MethodDescription.InDefinedShape accessorMethod = registeredGetters.get(fieldDescription);
                if (accessorMethod == null) {
                    accessorMethod = new FieldGetter(instrumentedType, fieldDescription, suffix);
                    registeredGetters.put(fieldDescription, accessorMethod);
                    accessorMethods.add(new FieldGetterDelegation(accessorMethod, fieldDescription));
                }
                return accessorMethod;
            }

            @Override
            public MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription) {
                MethodDescription.InDefinedShape accessorMethod = registeredSetters.get(fieldDescription);
                if (accessorMethod == null) {
                    accessorMethod = new FieldSetter(instrumentedType, fieldDescription, suffix);
                    registeredSetters.put(fieldDescription, accessorMethod);
                    accessorMethods.add(new FieldSetterDelegation(accessorMethod, fieldDescription));
                }
                return accessorMethod;
            }

            @Override
            public TypeDescription register(AuxiliaryType auxiliaryType) {
                DynamicType dynamicType = auxiliaryTypes.get(auxiliaryType);
                if (dynamicType == null) {
                    dynamicType = auxiliaryType.make(auxiliaryTypeNamingStrategy.name(instrumentedType), classFileVersion, this);
                    auxiliaryTypes.put(auxiliaryType, dynamicType);
                }
                return dynamicType.getTypeDescription();
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
                fieldCache = new CacheValueField(instrumentedType, fieldType, suffix, fieldValue.hashCode());
                registeredFieldCacheEntries.put(fieldCacheEntry, fieldCache);
                return fieldCache;
            }

            /**
             * Validates that the field cache is still accessible. Once the type initializer of a class is written, no
             * additional field caches can be defined. See
             * {@link Implementation.Context.Default#canRegisterFieldCache} for a more
             * detailed explanation of this validation.
             */
            private void validateFieldCacheAccessibility() {
                if (!canRegisterFieldCache) {
                    throw new IllegalStateException("A field cache cannot be registered during or after the creation of a " +
                            "type initializer - instead, the field cache should be registered in the method that requires " +
                            "the cached value");
                }
            }

            @Override
            public void drain(ClassVisitor classVisitor, TypeWriter.MethodPool methodPool, InjectedCode injectedCode) {
                canRegisterFieldCache = false;
                InstrumentedType.TypeInitializer typeInitializer = this.typeInitializer;
                for (Map.Entry<FieldCacheEntry, FieldDescription> entry : registeredFieldCacheEntries.entrySet()) {
                    classVisitor.visitField(entry.getValue().getModifiers(),
                            entry.getValue().getInternalName(),
                            entry.getValue().getDescriptor(),
                            entry.getValue().getGenericSignature(),
                            FieldDescription.NO_DEFAULT_VALUE).visitEnd();
                    typeInitializer = typeInitializer.expandWith(new ByteCodeAppender.Simple(entry.getKey().storeIn(entry.getValue())));
                }
                if (injectedCode.isDefined()) {
                    typeInitializer = typeInitializer.expandWith(injectedCode.getByteCodeAppender());
                }
                MethodDescription typeInitializerMethod = new MethodDescription.Latent.TypeInitializer(instrumentedType);
                TypeWriter.MethodPool.Record initializerRecord = methodPool.target(typeInitializerMethod);
                if (initializerRecord.getSort().isImplemented() && typeInitializer.isDefined()) {
                    initializerRecord = initializerRecord.prepend(typeInitializer);
                } else if (typeInitializer.isDefined()) {
                    initializerRecord = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody(typeInitializerMethod, typeInitializer.withReturn());
                }
                initializerRecord.apply(classVisitor, this);
                for (TypeWriter.MethodPool.Record record : accessorMethods) {
                    record.apply(classVisitor, this);
                }
            }

            @Override
            public String toString() {
                return "Implementation.Context.Default{" +
                        "instrumentedType=" + instrumentedType +
                        ", typeInitializer=" + typeInitializer +
                        ", classFileVersion=" + classFileVersion +
                        ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                        ", registeredAccessorMethods=" + registeredAccessorMethods +
                        ", registeredGetters=" + registeredGetters +
                        ", registeredSetters=" + registeredSetters +
                        ", accessorMethods=" + accessorMethods +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        ", registeredFieldCacheEntries=" + registeredFieldCacheEntries +
                        ", suffix=" + suffix +
                        ", canRegisterFieldCache=" + canRegisterFieldCache +
                        '}';
            }

            /**
             * A description of a field that stores a cached value.
             */
            protected static class CacheValueField extends FieldDescription.InDefinedShape.AbstractBase {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The type of the cache's field.
                 */
                private final TypeDescription fieldType;

                /**
                 * The suffix to use for the cache field's name.
                 */
                private final String suffix;

                /**
                 * The hash value of the field's value for creating a unique field name.
                 */
                private final int valueHashCode;

                /**
                 * Creates a new cache value field.
                 *
                 * @param instrumentedType The instrumented type.
                 * @param fieldType        The type of the cache's field.
                 * @param suffix           The suffix to use for the cache field's name.
                 * @param valueHashCode    The hash value of the field's value for creating a unique field name.
                 */
                protected CacheValueField(TypeDescription instrumentedType, TypeDescription fieldType, String suffix, int valueHashCode) {
                    this.instrumentedType = instrumentedType;
                    this.fieldType = fieldType;
                    this.suffix = suffix;
                    this.valueHashCode = valueHashCode;
                }

                @Override
                public GenericTypeDescription getType() {
                    return fieldType;
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return instrumentedType;
                }

                @Override
                public int getModifiers() {
                    return Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | (instrumentedType.isClassType()
                            ? Opcodes.ACC_PRIVATE
                            : Opcodes.ACC_PUBLIC);
                }

                @Override
                public String getName() {
                    return String.format("%s$%s$%d", FIELD_CACHE_PREFIX, suffix, Math.abs(valueHashCode % Integer.MAX_VALUE));
                }
            }

            /**
             * A field cache entry for uniquely identifying a cached field. A cached field is described by the stack
             * manipulation that loads the field's value onto the operand stack and the type of the field.
             */
            protected static class FieldCacheEntry implements StackManipulation {

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
                protected FieldCacheEntry(StackManipulation fieldValue, TypeDescription fieldType) {
                    this.fieldValue = fieldValue;
                    this.fieldType = fieldType;
                }

                /**
                 * Returns a stack manipulation where the represented value is stored in the given field.
                 *
                 * @param fieldDescription A static field in which the value is to be stored.
                 * @return A stack manipulation that represents this storage.
                 */
                public StackManipulation storeIn(FieldDescription fieldDescription) {
                    return new Compound(this, FieldAccess.forField(fieldDescription).putter());
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
                public boolean isValid() {
                    return fieldValue.isValid();
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                    return fieldValue.apply(methodVisitor, implementationContext);
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
                    return "Implementation.Context.Default.FieldCacheEntry{" +
                            "fieldValue=" + fieldValue +
                            ", fieldType=" + fieldType +
                            '}';
                }
            }

            /**
             * A base implementation of a method that accesses a property of an instrumented type.
             */
            protected abstract static class AbstractPropertyAccessorMethod extends MethodDescription.InDefinedShape.AbstractBase {

                @Override
                public int getModifiers() {
                    return Opcodes.ACC_SYNTHETIC | getBaseModifiers() | (getDeclaringType().isClassType()
                            ? Opcodes.ACC_FINAL
                            : Opcodes.ACC_PUBLIC);
                }

                /**
                 * Returns the base modifiers, i.e. the modifiers that define the accessed property's features.
                 *
                 * @return Returns the base modifiers of the represented methods.
                 */
                protected abstract int getBaseModifiers();
            }

            /**
             * A description of an accessor method to access another method from outside the instrumented type.
             */
            protected static class AccessorMethod extends AbstractPropertyAccessorMethod {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The method that is being accessed.
                 */
                private final MethodDescription methodDescription;

                /**
                 * The suffix to append to the accessor method's name.
                 */
                private final String suffix;

                /**
                 * Creates a new accessor method.
                 *
                 * @param instrumentedType  The instrumented type.
                 * @param methodDescription The method that is being accessed.
                 * @param suffix            The suffix to append to the accessor method's name.
                 */
                protected AccessorMethod(TypeDescription instrumentedType, MethodDescription methodDescription, String suffix) {
                    this.instrumentedType = instrumentedType;
                    this.methodDescription = methodDescription;
                    this.suffix = suffix;
                }

                @Override
                public GenericTypeDescription getReturnType() {
                    return methodDescription.getReturnType().asErasure();
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Explicit.ForTypes(this, methodDescription.getParameters().asTypeList().asErasures());
                }

                @Override
                public GenericTypeList getExceptionTypes() {
                    return methodDescription.getExceptionTypes().asErasures().asGenericTypes();
                }

                @Override
                public Object getDefaultValue() {
                    return MethodDescription.NO_DEFAULT_VALUE;
                }

                @Override
                public GenericTypeList getTypeVariables() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return instrumentedType;
                }

                @Override
                public int getBaseModifiers() {
                    return methodDescription.isStatic()
                            ? Opcodes.ACC_STATIC
                            : EMPTY_MASK;
                }

                @Override
                public String getInternalName() {
                    return String.format("%s$%s$%s", methodDescription.getInternalName(), ACCESSOR_METHOD_SUFFIX, suffix);
                }
            }

            /**
             * A description of a field getter method.
             */
            protected static class FieldGetter extends AbstractPropertyAccessorMethod {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The field for which a getter is described.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * The name suffix for the field getter method.
                 */
                private final String suffix;

                /**
                 * Creates a new field getter.
                 *
                 * @param instrumentedType The instrumented type.
                 * @param fieldDescription The field for which a getter is described.
                 * @param suffix           The name suffix for the field getter method.
                 */
                protected FieldGetter(TypeDescription instrumentedType, FieldDescription fieldDescription, String suffix) {
                    this.instrumentedType = instrumentedType;
                    this.fieldDescription = fieldDescription;
                    this.suffix = suffix;
                }

                @Override
                public GenericTypeDescription getReturnType() {
                    return fieldDescription.getType().asErasure();
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Empty();
                }

                @Override
                public GenericTypeList getExceptionTypes() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public Object getDefaultValue() {
                    return MethodDescription.NO_DEFAULT_VALUE;
                }

                @Override
                public GenericTypeList getTypeVariables() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return instrumentedType;
                }

                @Override
                protected int getBaseModifiers() {
                    return fieldDescription.isStatic()
                            ? Opcodes.ACC_STATIC
                            : EMPTY_MASK;
                }

                @Override
                public String getInternalName() {
                    return String.format("%s$%s$%s", fieldDescription.getName(), ACCESSOR_METHOD_SUFFIX, suffix);
                }
            }

            /**
             * A description of a field setter method.
             */
            protected static class FieldSetter extends AbstractPropertyAccessorMethod {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The field for which a setter is described.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * The name suffix for the field setter method.
                 */
                private final String suffix;

                /**
                 * Creates a new field setter.
                 *
                 * @param instrumentedType The instrumented type.
                 * @param fieldDescription The field for which a setter is described.
                 * @param suffix           The name suffix for the field setter method.
                 */
                protected FieldSetter(TypeDescription instrumentedType, FieldDescription fieldDescription, String suffix) {
                    this.instrumentedType = instrumentedType;
                    this.fieldDescription = fieldDescription;
                    this.suffix = suffix;
                }

                @Override
                public GenericTypeDescription getReturnType() {
                    return TypeDescription.VOID;
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Explicit.ForTypes(this, Collections.singletonList(fieldDescription.getType().asErasure()));
                }

                @Override
                public GenericTypeList getExceptionTypes() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public Object getDefaultValue() {
                    return MethodDescription.NO_DEFAULT_VALUE;
                }

                @Override
                public GenericTypeList getTypeVariables() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return instrumentedType;
                }

                @Override
                protected int getBaseModifiers() {
                    return fieldDescription.isStatic()
                            ? Opcodes.ACC_STATIC
                            : EMPTY_MASK;
                }

                @Override
                public String getInternalName() {
                    return String.format("%s$%s$%s", fieldDescription.getName(), ACCESSOR_METHOD_SUFFIX, suffix);
                }
            }

            /**
             * An abstract method pool entry that delegates the implementation of a method to itself.
             */
            protected abstract static class AbstractDelegationRecord extends TypeWriter.MethodPool.Record.ForDefinedMethod implements ByteCodeAppender {

                /**
                 * The delegation method.
                 */
                protected final MethodDescription methodDescription;

                /**
                 * Creates a new delegation record.
                 *
                 * @param methodDescription The delegation method.
                 */
                protected AbstractDelegationRecord(MethodDescription methodDescription) {
                    this.methodDescription = methodDescription;
                }

                @Override
                public MethodDescription getImplementedMethod() {
                    return methodDescription;
                }

                @Override
                public Sort getSort() {
                    return Sort.IMPLEMENTED;
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor, Context implementationContext) {
                    methodVisitor.visitCode();
                    Size size = apply(methodVisitor, implementationContext, getImplementedMethod());
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                }

                @Override
                public TypeWriter.MethodPool.Record prepend(ByteCodeAppender byteCodeAppender) {
                    throw new UnsupportedOperationException("Cannot prepend code to a delegation");
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && methodDescription.equals(((AbstractDelegationRecord) other).methodDescription);
                }

                @Override
                public int hashCode() {
                    return methodDescription.hashCode();
                }
            }

            /**
             * An implementation of a {@link TypeWriter.MethodPool.Record} for implementing
             * an accessor method.
             */
            protected static class AccessorMethodDelegation extends AbstractDelegationRecord {

                /**
                 * The stack manipulation that represents the requested special method invocation.
                 */
                private final StackManipulation accessorMethodInvocation;

                /**
                 * Creates a new accessor method delegation.
                 *
                 * @param methodDescription        The accessor method.
                 * @param accessorMethodInvocation The stack manipulation that represents the requested special method invocation.
                 */
                protected AccessorMethodDelegation(MethodDescription methodDescription, StackManipulation accessorMethodInvocation) {
                    super(methodDescription);
                    this.accessorMethodInvocation = accessorMethodInvocation;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                            accessorMethodInvocation,
                            MethodReturn.returning(instrumentedMethod.getReturnType().asErasure())
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && super.equals(other)
                            && accessorMethodInvocation.equals(((AccessorMethodDelegation) other).accessorMethodInvocation);
                }

                @Override
                public int hashCode() {
                    return accessorMethodInvocation.hashCode() + 31 * super.hashCode();
                }

                @Override
                public String toString() {
                    return "Implementation.Context.Default.AccessorMethodDelegation{" +
                            "accessorMethodInvocation=" + accessorMethodInvocation +
                            ", methodDescription=" + methodDescription +
                            '}';
                }
            }

            /**
             * An implementation for a field getter.
             */
            protected static class FieldGetterDelegation extends AbstractDelegationRecord {

                /**
                 * The field to read from.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new field getter implementation.
                 *
                 * @param methodDescription The delegation method.
                 * @param fieldDescription  The field to read.
                 */
                protected FieldGetterDelegation(MethodDescription methodDescription, FieldDescription fieldDescription) {
                    super(methodDescription);
                    this.fieldDescription = fieldDescription;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.Trivial.INSTANCE
                                    : MethodVariableAccess.REFERENCE.loadOffset(0),
                            FieldAccess.forField(fieldDescription).getter(),
                            MethodReturn.returning(fieldDescription.getType().asErasure())
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }


                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && super.equals(other)
                            && fieldDescription.equals(((FieldGetterDelegation) other).fieldDescription);
                }

                @Override
                public int hashCode() {
                    return fieldDescription.hashCode() + 31 * super.hashCode();
                }

                @Override
                public String toString() {
                    return "Implementation.Context.Default.FieldGetterDelegation{" +
                            "fieldDescription=" + fieldDescription +
                            ", methodDescription=" + methodDescription +
                            '}';
                }
            }

            /**
             * An implementation for a field setter.
             */
            protected static class FieldSetterDelegation extends AbstractDelegationRecord {

                /**
                 * The field to write to.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new field setter.
                 *
                 * @param methodDescription The field accessor method.
                 * @param fieldDescription  The field to write to.
                 */
                protected FieldSetterDelegation(MethodDescription methodDescription, FieldDescription fieldDescription) {
                    super(methodDescription);
                    this.fieldDescription = fieldDescription;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                            FieldAccess.forField(fieldDescription).putter(),
                            MethodReturn.VOID
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && super.equals(other)
                            && fieldDescription.equals(((FieldSetterDelegation) other).fieldDescription);
                }

                @Override
                public int hashCode() {
                    return fieldDescription.hashCode() + 31 * super.hashCode();
                }

                @Override
                public String toString() {
                    return "Implementation.Context.Default.FieldSetterDelegation{" +
                            "fieldDescription=" + fieldDescription +
                            ", methodDescription=" + methodDescription +
                            '}';
                }
            }
        }
    }

    /**
     * A compound implementation that allows to combine several implementations.
     * <p>&nbsp;</p>
     * Note that the combination of two implementation might break the contract for implementing
     * {@link java.lang.Object#equals(Object)} and {@link Object#hashCode()} as described for
     * {@link Implementation}.
     *
     * @see Implementation
     */
    class Compound implements Implementation {

        /**
         * All implementation that are represented by this compound implementation.
         */
        private final Implementation[] implementation;

        /**
         * Creates a new immutable compound implementation.
         *
         * @param implementation The implementations to combine in their order.
         */
        public Compound(Implementation... implementation) {
            this.implementation = implementation;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            for (Implementation implementation : this.implementation) {
                instrumentedType = implementation.prepare(instrumentedType);
            }
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            ByteCodeAppender[] byteCodeAppender = new ByteCodeAppender[implementation.length];
            int index = 0;
            for (Implementation implementation : this.implementation) {
                byteCodeAppender[index++] = implementation.appender(implementationTarget);
            }
            return new ByteCodeAppender.Compound(byteCodeAppender);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(implementation, ((Compound) other).implementation);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(implementation);
        }

        @Override
        public String toString() {
            return "Implementation.Compound{implementation=" + Arrays.toString(implementation) + '}';
        }
    }

    /**
     * A simple implementation that does not register any members with the instrumented type.
     */
    class Simple implements Implementation {

        /**
         * The byte code appender to emmit.
         */
        private final ByteCodeAppender byteCodeAppender;

        /**
         * Creates a new simple implementation for the given byte code appenders.
         *
         * @param byteCodeAppender The byte code appenders to apply in their order of application.
         */
        public Simple(ByteCodeAppender... byteCodeAppender) {
            this.byteCodeAppender = new ByteCodeAppender.Compound(byteCodeAppender);
        }

        /**
         * Creates a new simple instrumentation for the given stack manipulations which are summarized in a
         * byte code appender that defines any requested method by these manipulations.
         *
         * @param stackManipulation The stack manipulation to apply in their order of application.
         */
        public Simple(StackManipulation... stackManipulation) {
            byteCodeAppender = new ByteCodeAppender.Simple(stackManipulation);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return byteCodeAppender;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && byteCodeAppender.equals(((Simple) other).byteCodeAppender);
        }

        @Override
        public int hashCode() {
            return byteCodeAppender.hashCode();
        }

        @Override
        public String toString() {
            return "Implementation.Simple{byteCodeAppender=" + byteCodeAppender + '}';
        }
    }
}

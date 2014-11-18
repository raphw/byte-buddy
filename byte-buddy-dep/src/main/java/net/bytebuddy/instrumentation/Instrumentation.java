package net.bytebuddy.instrumentation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * An instrumentation is responsible for implementing (or not implementing) methods of a dynamically created type. An
 * instrumentation is applied in two stages:
 * <ol>
 * <li>The instrumentation is able to prepare an instrumented type by adding fields and/or helper methods that are
 * required for the methods implemented by this instrumentation. Furthermore,
 * {@link LoadedTypeInitializer}s can be registered for an instrumented
 * type.</li>
 * <li>An instrumentation is required to supply a byte code appender that is responsible for providing the byte code
 * to the instrumented methods that were delegated to this instrumentation. This byte code appender will also
 * be responsible for providing implementations for the methods added in step <i>1</i>.</li>
 * </ol>
 * <p>&nbsp;</p>
 * An instrumentation implementation should provide meaningful implementations {@link java.lang.Object#equals(Object)}
 * and {@link Object#hashCode()} if it wants to avoid to be used twice within the creation of a dynamic type. For two
 * equal instrumentation implementations only one will be applied on the creation of a dynamic type.
 */
public interface Instrumentation {

    /**
     * During the preparation phase of an instrumentation, implementations are eligible to adding fields or methods
     * to the currently instrumented type. All methods that are added by this instrumentation are required to be
     * implemented by the {@link net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender} that is emitted
     * on the call to
     * {@link net.bytebuddy.instrumentation.Instrumentation#appender(net.bytebuddy.instrumentation.Instrumentation.Target)}
     * call. On this method call, loaded type initializers can also be added to the instrumented type.
     *
     * @param instrumentedType The instrumented type that is the basis of the ongoing instrumentation.
     * @return The instrumented type with any applied changes, if any.
     */
    InstrumentedType prepare(InstrumentedType instrumentedType);

    /**
     * Creates a byte code appender that determines the implementation of the instrumented type's methods.
     *
     * @param instrumentationTarget The target of the current instrumentation.
     * @return A byte code appender for implementing methods delegated to this instrumentation. This byte code appender
     * is also responsible for handling methods that were added by this instrumentation on the call to
     * {@link net.bytebuddy.instrumentation.Instrumentation#prepare(net.bytebuddy.instrumentation.type.InstrumentedType)}.
     */
    ByteCodeAppender appender(Target instrumentationTarget);

    /**
     * An instrumentation for an abstract method that does not append any code and will throw an exception if it is
     * attempted to be composed with other methods that do provide an implementation.
     */
    static enum ForAbstractMethod implements Instrumentation, ByteCodeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return this;
        }

        @Override
        public boolean appendsCode() {
            return false;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
            throw new IllegalStateException();
        }
    }

    /**
     * Represents an type-specific method invocation on the current instrumented type which is not legal from outside
     * the type such as a super method or default method invocation. Legal instances of special method invocations must
     * be equal to one another if they represent the same invocation target.
     */
    static interface SpecialMethodInvocation extends StackManipulation {

        /**
         * Returns the method that represents this special method invocation. This method can be different even for
         * equal special method invocations, dependant on the method that was used to request such an invocation by the
         * means of a {@link net.bytebuddy.instrumentation.Instrumentation.Target}.
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
         * A canonical implementation of an illegal {@link net.bytebuddy.instrumentation.Instrumentation.SpecialMethodInvocation}.
         */
        static enum Illegal implements SpecialMethodInvocation {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context instrumentationContext) {
                throw new IllegalStateException();
            }

            @Override
            public MethodDescription getMethodDescription() {
                throw new IllegalStateException("An illegal special method invocation must not be applied");
            }

            @Override
            public TypeDescription getTypeDescription() {
                throw new IllegalStateException("An illegal special method invocation must not be applied");
            }
        }

        /**
         * A canonical implementation of a {@link net.bytebuddy.instrumentation.Instrumentation.SpecialMethodInvocation}.
         */
        static class Simple implements SpecialMethodInvocation {

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
            protected Simple(MethodDescription methodDescription,
                             TypeDescription typeDescription,
                             StackManipulation stackManipulation) {
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
            public static SpecialMethodInvocation of(MethodDescription methodDescription,
                                                     TypeDescription typeDescription) {
                StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).special(typeDescription);
                return stackManipulation.isValid() && !methodDescription.isAbstract()
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
            public boolean isValid() {
                return stackManipulation.isValid();
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context instrumentationContext) {
                return stackManipulation.apply(methodVisitor, instrumentationContext);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof SpecialMethodInvocation)) return false;
                SpecialMethodInvocation specialMethodInvocation = (SpecialMethodInvocation) other;
                return isValid() == specialMethodInvocation.isValid()
                        && typeDescription.equals(specialMethodInvocation.getTypeDescription())
                        && methodDescription.getInternalName().equals(specialMethodInvocation.getMethodDescription().getInternalName())
                        && methodDescription.getParameterTypes().equals(specialMethodInvocation.getMethodDescription().getParameterTypes())
                        && methodDescription.getReturnType().equals(specialMethodInvocation.getMethodDescription().getReturnType());
            }

            @Override
            public int hashCode() {
                int result = methodDescription.getInternalName().hashCode();
                result = 31 * result + methodDescription.getParameterTypes().hashCode();
                result = 31 * result + methodDescription.getReturnType().hashCode();
                result = 31 * result + typeDescription.hashCode();
                return result;
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
     * The target of an instrumentation. Instrumentation targets must be immutable and can be queried without altering
     * the instrumentation result. An instrumentation target provides information on the type that is to be created
     * where it is the implementation's responsibility to cache expensive computations, especially such computations
     * that require reflective look-up.
     */
    static interface Target {

        /**
         * Returns a description of the instrumented type.
         *
         * @return A description of the instrumented type.
         */
        TypeDescription getTypeDescription();

        /**
         * Identifies the origin type of an instrumentation. The origin type describes the type that is subject to
         * any form of enhancement. If a subclass of a given type is generated, the base type of this subclass
         * describes the origin type. If a given type is redefined or rebased, the origin type is described by the
         * instrumented type itself.
         *
         * @return The origin type of this instrumentation.
         */
        TypeDescription getOriginType();

        /**
         * Creates a special method invocation for invoking the super method of the given method.
         *
         * @param methodDescription The method that is to be invoked specially.
         * @param methodLookup      The lookup for this method which mainly serves to avoid bridge method invocation.
         * @return The corresponding special method invocation which might be illegal if the requested invocation is
         * not legal.
         */
        SpecialMethodInvocation invokeSuper(MethodDescription methodDescription, MethodLookup methodLookup);

        /**
         * Creates a special method invocation for invoking a default method.
         *
         * @param targetType            The interface on which the default method is to be invoked.
         * @param uniqueMethodSignature The unique method signature as defined by
         *                              {@link net.bytebuddy.instrumentation.method.MethodDescription#getUniqueSignature()}
         *                              of the method that is to be invoked.
         * @return The corresponding special method invocation which might be illegal if the requested invocation is
         * not legal.
         */
        SpecialMethodInvocation invokeDefault(TypeDescription targetType, String uniqueMethodSignature);

        /**
         * A strategy for looking up a method.
         */
        static interface MethodLookup {

            /**
             * Resolves the target method that is actually invoked.
             *
             * @param methodDescription    The method that is to be invoked specially.
             * @param invokableMethods     A map of all invokable methods on the instrumented type.
             * @param bridgeMethodResolver The bridge method resolver for this type.
             * @return The target method that is actually invoked.
             */
            MethodDescription resolve(MethodDescription methodDescription,
                                      Map<String, MethodDescription> invokableMethods,
                                      BridgeMethodResolver bridgeMethodResolver);

            /**
             * Default implementations of a {@link net.bytebuddy.instrumentation.Instrumentation.Target.MethodLookup}.
             */
            static enum Default implements MethodLookup {

                /**
                 * An exact method lookup which directly invokes the given method.
                 */
                EXACT {
                    @Override
                    public MethodDescription resolve(MethodDescription methodDescription,
                                                     Map<String, MethodDescription> invokableMethods,
                                                     BridgeMethodResolver bridgeMethodResolver) {
                        return methodDescription;
                    }
                },

                /**
                 * Looks up a most specific method by a method signature. All bridge methods are resolved by this
                 * lookup.
                 */
                MOST_SPECIFIC {
                    @Override
                    public MethodDescription resolve(MethodDescription methodDescription,
                                                     Map<String, MethodDescription> invokableMethods,
                                                     BridgeMethodResolver bridgeMethodResolver) {
                        return bridgeMethodResolver.resolve(invokableMethods.get(methodDescription.getUniqueSignature()));
                    }
                }
            }
        }

        /**
         * A factory for creating an {@link net.bytebuddy.instrumentation.Instrumentation.Target}.
         */
        static interface Factory {

            /**
             * Creates an {@link net.bytebuddy.instrumentation.Instrumentation.Target} for the given instrumented
             * type's description.
             *
             * @param finding The finding of a method lookup engine on analyzing the instrumented type.
             * @return An {@link net.bytebuddy.instrumentation.Instrumentation.Target} for the given type description.
             */
            Target make(MethodLookupEngine.Finding finding);
        }

        /**
         * An abstract base implementation for an {@link net.bytebuddy.instrumentation.Instrumentation.Target}.
         */
        abstract static class AbstractBase implements Target {

            /**
             * The type that is subject to instrumentation.
             */
            protected final TypeDescription typeDescription;

            /**
             * A map of invokable methods by their unique signature.
             */
            protected final Map<String, MethodDescription> invokableMethods;

            /**
             * A map of default methods by their unique signature.
             */
            protected final Map<TypeDescription, Map<String, MethodDescription>> defaultMethods;

            /**
             * A bridge method resolver for the given instrumented type.
             */
            protected final BridgeMethodResolver bridgeMethodResolver;

            /**
             * Creates a new instrumentation target.
             *
             * @param finding                     A finding of a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}
             *                                    for the instrumented type.
             * @param bridgeMethodResolverFactory A factory for creating a
             *                                    {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver}.
             */
            protected AbstractBase(MethodLookupEngine.Finding finding,
                                   BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                bridgeMethodResolver = bridgeMethodResolverFactory.make(finding.getInvokableMethods());
                typeDescription = finding.getTypeDescription();
                invokableMethods = new HashMap<String, MethodDescription>(finding.getInvokableMethods().size());
                for (MethodDescription methodDescription : finding.getInvokableMethods()) {
                    invokableMethods.put(methodDescription.getUniqueSignature(), methodDescription);
                }
                defaultMethods = new HashMap<TypeDescription, Map<String, MethodDescription>>(finding.getInvokableDefaultMethods().size());
                for (Map.Entry<TypeDescription, Set<MethodDescription>> entry : finding.getInvokableDefaultMethods().entrySet()) {
                    Map<String, MethodDescription> defaultMethods = new HashMap<String, MethodDescription>(entry.getValue().size());
                    for (MethodDescription methodDescription : entry.getValue()) {
                        defaultMethods.put(methodDescription.getUniqueSignature(), methodDescription);
                    }
                    this.defaultMethods.put(entry.getKey(), defaultMethods);
                }
            }

            @Override
            public TypeDescription getTypeDescription() {
                return typeDescription;
            }

            @Override
            public Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription,
                                                                       MethodLookup methodLookup) {
                return invokeSuper(methodLookup.resolve(methodDescription, invokableMethods, bridgeMethodResolver));
            }

            /**
             * Invokes the fully resolved method to be invoked by a super method call.
             *
             * @param methodDescription The method that is to be invoked specially.
             * @return A special method invocation for calling the super method.
             */
            protected abstract Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription);

            @Override
            public Instrumentation.SpecialMethodInvocation invokeDefault(TypeDescription targetType,
                                                                         String uniqueMethodSignature) {
                Map<String, MethodDescription> defaultMethods = this.defaultMethods.get(targetType);
                if (defaultMethods != null) {
                    MethodDescription defaultMethod = defaultMethods.get(uniqueMethodSignature);
                    if (defaultMethod != null) {
                        return SpecialMethodInvocation.Simple.of(defaultMethod, targetType);
                    }
                }
                return Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                AbstractBase that = (AbstractBase) other;
                return bridgeMethodResolver.equals(that.bridgeMethodResolver)
                        && defaultMethods.equals(that.defaultMethods)
                        && typeDescription.equals(that.typeDescription);
            }

            @Override
            public int hashCode() {
                int result = typeDescription.hashCode();
                result = 31 * result + defaultMethods.hashCode();
                result = 31 * result + bridgeMethodResolver.hashCode();
                return result;
            }
        }
    }

    /**
     * The context for an instrumentation application. An instrumentation context represents a mutable data structure
     * and all queries are irrevocable. Calling methods on an instrumentation context should be considered equally
     * sensitive as calling a {@link org.objectweb.asm.MethodVisitor}. As such, an instrumentation context and a
     * {@link org.objectweb.asm.MethodVisitor} are complementary for creating an new Java type.
     */
    static interface Context {

        /**
         * Registers an auxiliary type as required for the current instrumentation. Registering a type will cause the
         * creation of this type even if this type is not effectively used for the current instrumentation.
         *
         * @param auxiliaryType The auxiliary type that is required for the current instrumentation.
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
         * Represents an extractable view of an {@link net.bytebuddy.instrumentation.Instrumentation.Context} which
         * allows the retrieval of any registered auxiliary type.
         */
        static interface ExtractableView extends Context {

            /**
             * A default modifier for a field that serves as a cache.
             */
            static final int FIELD_CACHE_MODIFIER = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC;

            /**
             * Returns any {@link net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType} that was registered
             * with this {@link net.bytebuddy.instrumentation.Instrumentation.Context}.
             *
             * @return A list of all manifested registered auxiliary types.
             */
            List<DynamicType> getRegisteredAuxiliaryTypes();

            /**
             * Writes any information that was registered with an {@link net.bytebuddy.instrumentation.Instrumentation.Context}
             * to the provided class visitor. This contains any fields for value caching, any accessor method and it
             * writes the type initializer. The type initializer must therefore never be written manually.
             *
             * @param classVisitor The class visitor to which the extractable view is to be written.
             * @param methodPool   A method pool which is queried for any user code to add to the type initializer.
             * @param injectedCode Potential code that is to be injected into the type initializer.
             */
            void drain(ClassVisitor classVisitor, TypeWriter.MethodPool methodPool, InjectedCode injectedCode);

            /**
             * When draining an instrumentation context, a type initializer might be written to the created class
             * file. If any code must be explicitly invoked from within the type initializer, this can be achieved
             * by providing a code injection by this instance. The injected code is added after the class is set up but
             * before any user code is run from within the type initializer.
             */
            static interface InjectedCode {

                /**
                 * Returns the injected code. This method must only be called if there is actual code injected as
                 * signaled by {@link net.bytebuddy.instrumentation.Instrumentation.Context.ExtractableView.InjectedCode#isInjected()}.
                 *
                 * @return A stack manipulation that represents the injected code.
                 */
                StackManipulation getInjectedCode();

                /**
                 * Checks if code is injected.
                 *
                 * @return {@code true} if code is injected.
                 */
                boolean isInjected();

                /**
                 * A canonical implementation of non-applicable injected code.
                 */
                static enum None implements InjectedCode {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public StackManipulation getInjectedCode() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public boolean isInjected() {
                        return false;
                    }
                }
            }
        }

        /**
         * A default implementation of an {@link net.bytebuddy.instrumentation.Instrumentation.Context.ExtractableView}
         * which serves as its own {@link net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType.MethodAccessorFactory}.
         */
        static class Default implements Instrumentation.Context.ExtractableView, AuxiliaryType.MethodAccessorFactory {

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
             * The registered getters.
             */
            private final Map<FieldDescription, MethodDescription> registeredGetters;

            /**
             * The registered setters.
             */
            private final Map<FieldDescription, MethodDescription> registeredSetters;

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
            private final RandomString randomString;

            /**
             * Signals if this type extension delegate is still capable of registering field cache entries. Such entries
             * must be explicitly initialized in the instrumented type's type initializer such that no entries can be
             * registered after the type initializer was written.
             */
            private boolean canRegisterFieldCache;

            /**
             * Creates a new delegate. This constructor implicitly defines default naming strategies for created accessor
             * method and registered auxiliary types.
             *
             * @param instrumentedType The description of the type that is currently subject of creation.
             * @param classFileVersion The class file version of the created class.
             */
            public Default(TypeDescription instrumentedType, ClassFileVersion classFileVersion) {
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
            public Default(TypeDescription instrumentedType,
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
                registeredGetters = new HashMap<FieldDescription, MethodDescription>();
                registeredSetters = new HashMap<FieldDescription, MethodDescription>();
                accessorMethodEntries = new HashMap<MethodDescription, TypeWriter.MethodPool.Entry>();
                auxiliaryTypes = new HashMap<AuxiliaryType, DynamicType>();
                registeredFieldCacheEntries = new HashMap<FieldCacheEntry, FieldDescription>();
                randomString = new RandomString();
                canRegisterFieldCache = true;
            }

            @Override
            public MethodDescription registerAccessorFor(Instrumentation.SpecialMethodInvocation specialMethodInvocation) {
                MethodDescription accessorMethod = registeredAccessorMethods.get(specialMethodInvocation);
                if (accessorMethod == null) {
                    String name = String.format("%s$%s$%s", specialMethodInvocation.getMethodDescription().getInternalName(),
                            accessorMethodSuffix,
                            randomString.nextString());
                    accessorMethod = new MethodDescription.Latent(name,
                            instrumentedType,
                            specialMethodInvocation.getMethodDescription().getReturnType(),
                            specialMethodInvocation.getMethodDescription().getParameterTypes(),
                            ACCESSOR_METHOD_MODIFIER | (specialMethodInvocation.getMethodDescription().isStatic()
                                    ? Opcodes.ACC_STATIC
                                    : 0),
                            specialMethodInvocation.getMethodDescription().getExceptionTypes());
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
                accessorMethodEntries.put(accessorMethod, new AccessorMethodDelegation(specialMethodInvocation));
            }

            @Override
            public MethodDescription registerGetterFor(FieldDescription fieldDescription) {
                MethodDescription accessorMethod = registeredGetters.get(fieldDescription);
                if (accessorMethod == null) {
                    String name = String.format("%s$%s$%s", fieldDescription.getName(),
                            accessorMethodSuffix,
                            randomString.nextString());
                    accessorMethod = new MethodDescription.Latent(name,
                            instrumentedType,
                            fieldDescription.getFieldType(),
                            Collections.<TypeDescription>emptyList(),
                            ACCESSOR_METHOD_MODIFIER | (fieldDescription.isStatic()
                                    ? Opcodes.ACC_STATIC
                                    : 0),
                            Collections.<TypeDescription>emptyList());
                    registerGetter(fieldDescription, accessorMethod);
                }
                return accessorMethod;
            }

            /**
             * Registers a new getter method.
             *
             * @param fieldDescription The field to read.
             * @param accessorMethod   The accessor method for this field.
             */
            private void registerGetter(FieldDescription fieldDescription,
                                        MethodDescription accessorMethod) {
                registeredGetters.put(fieldDescription, accessorMethod);
                accessorMethodEntries.put(accessorMethod, new FieldGetter(fieldDescription));
            }

            @Override
            public MethodDescription registerSetterFor(FieldDescription fieldDescription) {
                MethodDescription accessorMethod = registeredSetters.get(fieldDescription);
                if (accessorMethod == null) {
                    String name = String.format("%s$%s$%s", fieldDescription.getName(),
                            accessorMethodSuffix,
                            randomString.nextString());
                    accessorMethod = new MethodDescription.Latent(name,
                            instrumentedType,
                            new TypeDescription.ForLoadedType(void.class),
                            Collections.singletonList(fieldDescription.getFieldType()),
                            ACCESSOR_METHOD_MODIFIER | (fieldDescription.isStatic()
                                    ? Opcodes.ACC_STATIC
                                    : 0),
                            Collections.<TypeDescription>emptyList());
                    registerSetter(fieldDescription, accessorMethod);
                }
                return accessorMethod;
            }

            /**
             * Registers a new setter method.
             *
             * @param fieldDescription The field to write to.
             * @param accessorMethod   The accessor method for this field.
             */
            private void registerSetter(FieldDescription fieldDescription,
                                        MethodDescription accessorMethod) {
                registeredSetters.put(fieldDescription, accessorMethod);
                accessorMethodEntries.put(accessorMethod, new FieldSetter(fieldDescription));
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
                fieldCache = new FieldDescription.Latent(String.format("%s$%s", fieldCachePrefix, randomString.nextString()),
                        instrumentedType,
                        fieldType,
                        FIELD_CACHE_MODIFIER);
                registeredFieldCacheEntries.put(fieldCacheEntry, fieldCache);
                return fieldCache;
            }

            /**
             * Validates that the field cache is still accessible. Once the type initializer of a class is written, no
             * additional field caches can be defined. See
             * {@link net.bytebuddy.instrumentation.Instrumentation.Context.Default#canRegisterFieldCache} for a more
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
                MethodDescription typeInitializer = MethodDescription.Latent.typeInitializerOf(instrumentedType);
                FieldCacheAppender.resolve(methodPool.target(typeInitializer), registeredFieldCacheEntries, injectedCode)
                        .apply(classVisitor, this, typeInitializer);
                for (FieldDescription fieldDescription : registeredFieldCacheEntries.values()) {
                    classVisitor.visitField(fieldDescription.getModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            fieldDescription.getGenericSignature(),
                            null).visitEnd();
                }
                for (Map.Entry<MethodDescription, TypeWriter.MethodPool.Entry> entry : accessorMethodEntries.entrySet()) {
                    entry.getValue().apply(classVisitor, this, entry.getKey());
                }
            }

            @Override
            public String toString() {
                return "Instrumentation.Context.Default{" +
                        "instrumentedType=" + instrumentedType +
                        ", classFileVersion=" + classFileVersion +
                        ", accessorMethodSuffix='" + accessorMethodSuffix + '\'' +
                        ", fieldCachePrefix='" + fieldCachePrefix + '\'' +
                        ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                        ", registeredAccessorMethods=" + registeredAccessorMethods +
                        ", accessorMethodEntries=" + accessorMethodEntries +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        ", registeredFieldCacheEntries=" + registeredFieldCacheEntries +
                        ", randomString=" + randomString +
                        ", canRegisterFieldCache=" + canRegisterFieldCache +
                        '}';
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
                    private final RandomString randomString;

                    /**
                     * Creates a new suffixing random naming strategy.
                     *
                     * @param suffix The suffix to extend to the instrumented type.
                     */
                    public SuffixingRandom(String suffix) {
                        this.suffix = suffix;
                        randomString = new RandomString();
                    }

                    @Override
                    public String name(AuxiliaryType auxiliaryType, TypeDescription instrumentedType) {
                        return String.format("%s$%s$%s", instrumentedType.getName(), suffix, randomString.nextString());
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
                        return "Instrumentation.Context.Default.AuxiliaryTypeNamingStrategySuffixingRandom{suffix='" + suffix + '\'' + '}';
                    }
                }
            }

            /**
             * A byte code appender that writes the field cache entries to a given {@link org.objectweb.asm.MethodVisitor}.
             */
            protected static class FieldCacheAppender implements ByteCodeAppender {

                /**
                 * The map of registered field cache entries.
                 */
                private final Map<FieldCacheEntry, FieldDescription> registeredFieldCacheEntries;

                /**
                 * Creates a new field cache appender.
                 *
                 * @param registeredFieldCacheEntries A map of field cache entries to their field descriptions.
                 */
                private FieldCacheAppender(Map<FieldCacheEntry, FieldDescription> registeredFieldCacheEntries) {
                    this.registeredFieldCacheEntries = registeredFieldCacheEntries;
                }

                /**
                 * Resolves the actual method pool entry to be applied for a given set of field cache entries and the
                 * provided {@link net.bytebuddy.instrumentation.Instrumentation.Context.ExtractableView.InjectedCode}.
                 *
                 * @param originalEntry               The original entry that is provided by the user.
                 * @param registeredFieldCacheEntries A map of registered field cache entries.
                 * @param injectedCode                The explicitly supplied injected code.
                 * @return The entry to apply to the type initializer.
                 */
                protected static TypeWriter.MethodPool.Entry resolve(TypeWriter.MethodPool.Entry originalEntry,
                                                                     Map<FieldCacheEntry, FieldDescription> registeredFieldCacheEntries,
                                                                     InjectedCode injectedCode) {
                    boolean defineMethod = originalEntry.isDefineMethod();
                    boolean injectCode = injectedCode.isInjected();
                    return registeredFieldCacheEntries.size() == 0 && !injectCode
                            ? originalEntry
                            : new TypeWriter.MethodPool.Entry.Simple(new Compound(new FieldCacheAppender(registeredFieldCacheEntries),
                            new Simple(injectCode ? injectedCode.getInjectedCode() : StackManipulation.LegalTrivial.INSTANCE),
                            defineMethod && originalEntry.getByteCodeAppender().appendsCode()
                                    ? originalEntry.getByteCodeAppender()
                                    : new Simple(MethodReturn.VOID)),
                            defineMethod
                                    ? originalEntry.getAttributeAppender()
                                    : MethodAttributeAppender.NoOp.INSTANCE);
                }

                @Override
                public boolean appendsCode() {
                    return registeredFieldCacheEntries.size() > 0;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Instrumentation.Context instrumentationContext,
                                  MethodDescription instrumentedMethod) {
                    StackManipulation[] fieldInitialization = new StackManipulation[registeredFieldCacheEntries.size()];
                    int currentIndex = 0;
                    for (Map.Entry<FieldCacheEntry, FieldDescription> entry : registeredFieldCacheEntries.entrySet()) {
                        fieldInitialization[currentIndex++] = new StackManipulation
                                .Compound(entry.getKey().getFieldValue(), FieldAccess.forField(entry.getValue()).putter());
                    }
                    StackManipulation.Size stackSize = new StackManipulation.Compound(fieldInitialization)
                            .apply(methodVisitor, instrumentationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && registeredFieldCacheEntries.equals(((FieldCacheAppender) other).registeredFieldCacheEntries);
                }

                @Override
                public int hashCode() {
                    return registeredFieldCacheEntries.hashCode();
                }

                @Override
                public String toString() {
                    return "Instrumentation.Context.Default.FieldCacheAppender{registeredFieldCacheEntries=" + registeredFieldCacheEntries + '}';
                }
            }

            /**
             * A field cache entry for uniquely identifying a cached field. A cached field is described by the stack
             * manipulation that loads the field's value onto the operand stack and the type of the field.
             */
            protected static class FieldCacheEntry {

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
                    return "Instrumentation.Context.Default.FieldCacheEntry{" +
                            "fieldValue=" + fieldValue +
                            ", fieldType=" + fieldType +
                            '}';
                }
            }

            /**
             * An implementation of a {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry} for implementing
             * an accessor method.
             */
            protected static class AccessorMethodDelegation implements TypeWriter.MethodPool.Entry, ByteCodeAppender {

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
                protected AccessorMethodDelegation(StackManipulation accessorMethodInvocation) {
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
                public void apply(ClassVisitor classVisitor,
                                  Instrumentation.Context instrumentationContext,
                                  MethodDescription methodDescription) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(methodDescription.getModifiers(),
                            methodDescription.getInternalName(),
                            methodDescription.getDescriptor(),
                            methodDescription.getGenericSignature(),
                            methodDescription.getExceptionTypes().toInternalNames());
                    methodVisitor.visitCode();
                    Size size = apply(methodVisitor, instrumentationContext, methodDescription);
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    methodVisitor.visitEnd();
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
                    return "Instrumentation.Context.Default.AccessorMethodDelegation{accessorMethodInvocation=" + accessorMethodInvocation + '}';
                }
            }

            /**
             * An implementation for a field getter.
             */
            protected static class FieldGetter implements TypeWriter.MethodPool.Entry, ByteCodeAppender {

                /**
                 * The field to read from.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new field getter instrumentation.
                 *
                 * @param fieldDescription The field to read.
                 */
                protected FieldGetter(FieldDescription fieldDescription) {
                    this.fieldDescription = fieldDescription;
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
                public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.LegalTrivial.INSTANCE
                                    : MethodVariableAccess.REFERENCE.loadFromIndex(0),
                            FieldAccess.forField(fieldDescription).getter(),
                            MethodReturn.returning(fieldDescription.getFieldType())
                    ).apply(methodVisitor, instrumentationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                @Override
                public void apply(ClassVisitor classVisitor, Context instrumentationContext, MethodDescription methodDescription) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(methodDescription.getModifiers(),
                            methodDescription.getInternalName(),
                            methodDescription.getDescriptor(),
                            methodDescription.getGenericSignature(),
                            methodDescription.getExceptionTypes().toInternalNames());
                    methodVisitor.visitCode();
                    Size size = apply(methodVisitor, instrumentationContext, methodDescription);
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    methodVisitor.visitEnd();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldDescription.equals(((FieldGetter) other).fieldDescription);
                }

                @Override
                public int hashCode() {
                    return fieldDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "Instrumentation.Context.Default.FieldGetter{" +
                            "fieldDescription=" + fieldDescription +
                            '}';
                }
            }

            /**
             * An implementation for a field setter.
             */
            protected static class FieldSetter implements TypeWriter.MethodPool.Entry, ByteCodeAppender {

                /**
                 * The field to write to.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new field setter.
                 *
                 * @param fieldDescription The field to write to.
                 */
                protected FieldSetter(FieldDescription fieldDescription) {
                    this.fieldDescription = fieldDescription;
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
                public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                            FieldAccess.forField(fieldDescription).putter(),
                            MethodReturn.VOID
                    ).apply(methodVisitor, instrumentationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                @Override
                public void apply(ClassVisitor classVisitor, Context instrumentationContext, MethodDescription methodDescription) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(methodDescription.getModifiers(),
                            methodDescription.getInternalName(),
                            methodDescription.getDescriptor(),
                            methodDescription.getGenericSignature(),
                            methodDescription.getExceptionTypes().toInternalNames());
                    methodVisitor.visitCode();
                    Size size = apply(methodVisitor, instrumentationContext, methodDescription);
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    methodVisitor.visitEnd();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldDescription.equals(((FieldSetter) other).fieldDescription);
                }

                @Override
                public int hashCode() {
                    return fieldDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "Instrumentation.Context.Default.FieldSetter{" +
                            "fieldDescription=" + fieldDescription +
                            '}';
                }
            }
        }
    }

    /**
     * A compound instrumentation that allows to combine several instrumentations.
     * <p>&nbsp;</p>
     * Note that the combination of two instrumentations might break the contract for implementing
     * {@link java.lang.Object#equals(Object)} and {@link Object#hashCode()} as described for
     * {@link net.bytebuddy.instrumentation.Instrumentation}.
     *
     * @see net.bytebuddy.instrumentation.Instrumentation
     */
    static class Compound implements Instrumentation {

        /**
         * All instrumentations that are represented by this compound instrumentation.
         */
        private final Instrumentation[] instrumentation;

        /**
         * Creates a new immutable compound instrumentation.
         *
         * @param instrumentation The instrumentations to combine in their order.
         */
        public Compound(Instrumentation... instrumentation) {
            this.instrumentation = instrumentation;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            for (Instrumentation instrumentation : this.instrumentation) {
                instrumentedType = instrumentation.prepare(instrumentedType);
            }
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            ByteCodeAppender[] byteCodeAppender = new ByteCodeAppender[instrumentation.length];
            int index = 0;
            for (Instrumentation instrumentation : this.instrumentation) {
                byteCodeAppender[index++] = instrumentation.appender(instrumentationTarget);
            }
            return new ByteCodeAppender.Compound(byteCodeAppender);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && Arrays.equals(instrumentation, ((Compound) o).instrumentation);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(instrumentation);
        }

        @Override
        public String toString() {
            return "Instrumentation.Compound{instrumentation=" + Arrays.toString(instrumentation) + '}';
        }
    }

    /**
     * A simple implementation of an instrumentation that does not register any members with the instrumented type.
     */
    static class Simple implements Instrumentation {

        /**
         * The byte code appender to emmit.
         */
        private final ByteCodeAppender byteCodeAppender;

        /**
         * Creates a new simple instrumentation for the given byte code appenders.
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
        public ByteCodeAppender appender(Target instrumentationTarget) {
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
            return "Instrumentation.Simple{byteCodeAppender=" + byteCodeAppender + '}';
        }
    }
}

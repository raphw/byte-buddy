package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodLookupEngine;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
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
         * A canonical implementation of a {@link Implementation.SpecialMethodInvocation}.
         */
        class Simple implements SpecialMethodInvocation {

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
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                return stackManipulation.apply(methodVisitor, implementationContext);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof SpecialMethodInvocation)) return false;
                SpecialMethodInvocation specialMethodInvocation = (SpecialMethodInvocation) other;
                return isValid() == specialMethodInvocation.isValid()
                        && typeDescription.equals(specialMethodInvocation.getTypeDescription())
                        && methodDescription.getInternalName().equals(specialMethodInvocation.getMethodDescription().getInternalName())
                        && methodDescription.getParameters().asTypeList().asRawTypes().equals(specialMethodInvocation.getMethodDescription().getParameters().asTypeList().asRawTypes())
                        && methodDescription.getReturnType().asRawType().equals(specialMethodInvocation.getMethodDescription().getReturnType().asRawType());
            }

            @Override
            public int hashCode() {
                int result = methodDescription.getInternalName().hashCode();
                result = 31 * result + methodDescription.getParameters().asTypeList().asRawTypes().hashCode();
                result = 31 * result + methodDescription.getReturnType().asRawType().hashCode();
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
         *                              {@link MethodDescription#getUniqueSignature()}
         *                              of the method that is to be invoked.
         * @return The corresponding special method invocation which might be illegal if the requested invocation is
         * not legal.
         */
        SpecialMethodInvocation invokeDefault(TypeDescription targetType, String uniqueMethodSignature);

        /**
         * A strategy for looking up a method.
         */
        interface MethodLookup {

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
             * Default implementations of a {@link Implementation.Target.MethodLookup}.
             */
            enum Default implements MethodLookup {

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
                };

                @Override
                public String toString() {
                    return "Implementation.Target.MethodLookup.Default." + name();
                }
            }
        }

        /**
         * A factory for creating an {@link Implementation.Target}.
         */
        interface Factory {

            /**
             * Creates a new implementation target.
             *
             * @param finding             The analyzed instrumented type.
             * @param instrumentedMethods A list of all methods that are to be instrumented.
             * @return A suitable implementation target.
             */
            Target make(MethodLookupEngine.Finding finding, List<? extends MethodDescription> instrumentedMethods);
        }

        /**
         * An abstract base implementation for an {@link Implementation.Target}.
         */
        abstract class AbstractBase implements Target {

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
             * Creates a new implementation target.
             *
             * @param finding                     A finding of a {@link MethodLookupEngine}
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
            public Implementation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription,
                                                                      MethodLookup methodLookup) {
                return invokeSuper(methodLookup.resolve(methodDescription, invokableMethods, bridgeMethodResolver));
            }

            /**
             * Invokes the fully resolved method to be invoked by a super method call.
             *
             * @param methodDescription The method that is to be invoked specially.
             * @return A special method invocation for calling the super method.
             */
            protected abstract Implementation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription);

            @Override
            public Implementation.SpecialMethodInvocation invokeDefault(TypeDescription targetType,
                                                                        String uniqueMethodSignature) {
                Map<String, MethodDescription> defaultMethods = this.defaultMethods.get(targetType);
                if (defaultMethods != null) {
                    MethodDescription defaultMethod = defaultMethods.get(uniqueMethodSignature);
                    if (defaultMethod != null) {
                        return SpecialMethodInvocation.Simple.of(defaultMethod, targetType);
                    }
                }
                return Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
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
             * A default modifier for a field that serves as a cache.
             */
            int FIELD_CACHE_MODIFIER = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC;

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
             * Indicates that a field should be defined without a default value.
             */
            private static final Object NO_DEFAULT_VALUE = null;

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
            private final Map<Implementation.SpecialMethodInvocation, MethodDescription> registeredAccessorMethods;

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
                registeredAccessorMethods = new HashMap<Implementation.SpecialMethodInvocation, MethodDescription>();
                registeredGetters = new HashMap<FieldDescription, MethodDescription>();
                registeredSetters = new HashMap<FieldDescription, MethodDescription>();
                accessorMethodEntries = new HashMap<MethodDescription, TypeWriter.MethodPool.Entry>();
                auxiliaryTypes = new HashMap<AuxiliaryType, DynamicType>();
                registeredFieldCacheEntries = new HashMap<FieldCacheEntry, FieldDescription>();
                randomString = new RandomString();
                canRegisterFieldCache = true;
            }

            @Override
            public MethodDescription registerAccessorFor(Implementation.SpecialMethodInvocation specialMethodInvocation) {
                MethodDescription accessorMethod = registeredAccessorMethods.get(specialMethodInvocation);
                if (accessorMethod == null) {
                    accessorMethod = new MethodDescription.Latent(instrumentedType,
                            String.format("%s$%s$%s", specialMethodInvocation.getMethodDescription().getInternalName(),
                                    ACCESSOR_METHOD_SUFFIX,
                                    randomString.nextString()),
                            specialMethodInvocation.getMethodDescription().getReturnType().asRawType(),
                            specialMethodInvocation.getMethodDescription().getParameters().accept(GenericTypeDescription.Visitor.Erasing.INSTANCE),
                            resolveModifier(specialMethodInvocation.getMethodDescription().isStatic()),
                            specialMethodInvocation.getMethodDescription().getExceptionTypes().asRawTypes(),
                            Collections.<AnnotationDescription>emptyList());
                    registerAccessor(specialMethodInvocation, accessorMethod);
                }
                return accessorMethod;
            }

            /**
             * Resolves the modifier for an accessor method.
             *
             * @param isStatic {@code true} if the accessor method is supposed to be static.
             * @return The modifier for the method.
             */
            private int resolveModifier(boolean isStatic) {
                return ACCESSOR_METHOD_MODIFIER | (isStatic ? Opcodes.ACC_STATIC : 0);
            }

            /**
             * Registers a new accessor method.
             *
             * @param specialMethodInvocation The special method invocation that the accessor method should invoke.
             * @param accessorMethod          The accessor method for this invocation.
             */
            private void registerAccessor(Implementation.SpecialMethodInvocation specialMethodInvocation, MethodDescription accessorMethod) {
                registeredAccessorMethods.put(specialMethodInvocation, accessorMethod);
                accessorMethodEntries.put(accessorMethod, new AccessorMethodDelegation(specialMethodInvocation));
            }

            @Override
            public MethodDescription registerGetterFor(FieldDescription fieldDescription) {
                MethodDescription accessorMethod = registeredGetters.get(fieldDescription);
                if (accessorMethod == null) {
                    accessorMethod = new MethodDescription.Latent(instrumentedType,
                            String.format("%s$%s$%s", fieldDescription.getName(), ACCESSOR_METHOD_SUFFIX, randomString.nextString()),
                            fieldDescription.getType().asRawType(),
                            Collections.<ParameterDescription.Token>emptyList(),
                            resolveModifier(fieldDescription.isStatic()),
                            Collections.<GenericTypeDescription>emptyList(),
                            Collections.<AnnotationDescription>emptyList());
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
            private void registerGetter(FieldDescription fieldDescription, MethodDescription accessorMethod) {
                registeredGetters.put(fieldDescription, accessorMethod);
                accessorMethodEntries.put(accessorMethod, new FieldGetter(fieldDescription));
            }

            @Override
            public MethodDescription registerSetterFor(FieldDescription fieldDescription) {
                MethodDescription accessorMethod = registeredSetters.get(fieldDescription);
                if (accessorMethod == null) {
                    accessorMethod = new MethodDescription.Latent(instrumentedType,
                            String.format("%s$%s$%s", fieldDescription.getName(), ACCESSOR_METHOD_SUFFIX, randomString.nextString()),
                            TypeDescription.VOID,
                            Collections.singletonList(new ParameterDescription.Token(fieldDescription.getType().asRawType())),
                            resolveModifier(fieldDescription.isStatic()),
                            Collections.<GenericTypeDescription>emptyList(),
                            Collections.<AnnotationDescription>emptyList());
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
            private void registerSetter(FieldDescription fieldDescription, MethodDescription accessorMethod) {
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
                fieldCache = new FieldDescription.Latent(instrumentedType,
                        String.format("%s$%s", FIELD_CACHE_PREFIX, randomString.nextString()),
                        fieldType,
                        FIELD_CACHE_MODIFIER,
                        Collections.<AnnotationDescription>emptyList());
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
                            NO_DEFAULT_VALUE).visitEnd();
                    typeInitializer = typeInitializer.expandWith(new ByteCodeAppender.Simple(entry.getKey().storeIn(entry.getValue())));
                }
                if (injectedCode.isDefined()) {
                    typeInitializer = typeInitializer.expandWith(injectedCode.getByteCodeAppender());
                }
                MethodDescription typeInitializerMethod = MethodDescription.Latent.typeInitializerOf(instrumentedType);
                TypeWriter.MethodPool.Entry initializerEntry = methodPool.target(typeInitializerMethod);
                if (initializerEntry.getSort().isImplemented() && typeInitializer.isDefined()) {
                    initializerEntry = initializerEntry.prepend(typeInitializer);
                } else if (typeInitializer.isDefined()) {
                    initializerEntry = new TypeWriter.MethodPool.Entry.ForImplementation(typeInitializer.withReturn(), MethodAttributeAppender.NoOp.INSTANCE);
                }
                initializerEntry.apply(classVisitor, this, typeInitializerMethod);
                for (Map.Entry<MethodDescription, TypeWriter.MethodPool.Entry> entry : accessorMethodEntries.entrySet()) {
                    entry.getValue().apply(classVisitor, this, entry.getKey());
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
                        ", accessorMethodEntries=" + accessorMethodEntries +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        ", registeredFieldCacheEntries=" + registeredFieldCacheEntries +
                        ", randomString=" + randomString +
                        ", canRegisterFieldCache=" + canRegisterFieldCache +
                        '}';
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
             * An abstract method pool entry that delegates the implementation of a method to itself.
             */
            protected abstract static class AbstractDelegationEntry extends TypeWriter.MethodPool.Entry.AbstractDefiningEntry implements ByteCodeAppender {

                @Override
                public Sort getSort() {
                    return Sort.IMPLEMENT;
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor, Context implementationContext, MethodDescription methodDescription) {
                    methodVisitor.visitCode();
                    Size size = apply(methodVisitor, implementationContext, methodDescription);
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                }

                @Override
                public TypeWriter.MethodPool.Entry prepend(ByteCodeAppender byteCodeAppender) {
                    throw new UnsupportedOperationException("Cannot prepend code to a delegator");
                }
            }

            /**
             * An implementation of a {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry} for implementing
             * an accessor method.
             */
            protected static class AccessorMethodDelegation extends AbstractDelegationEntry {

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
                public Size apply(MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                            accessorMethodInvocation,
                            MethodReturn.returning(instrumentedMethod.getReturnType().asRawType())
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
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
                    return "Implementation.Context.Default.AccessorMethodDelegation{accessorMethodInvocation=" + accessorMethodInvocation + '}';
                }
            }

            /**
             * An implementation for a field getter.
             */
            protected static class FieldGetter extends AbstractDelegationEntry {

                /**
                 * The field to read from.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new field getter implementation.
                 *
                 * @param fieldDescription The field to read.
                 */
                protected FieldGetter(FieldDescription fieldDescription) {
                    this.fieldDescription = fieldDescription;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.LegalTrivial.INSTANCE
                                    : MethodVariableAccess.REFERENCE.loadOffset(0),
                            FieldAccess.forField(fieldDescription).getter(),
                            MethodReturn.returning(fieldDescription.getType().asRawType())
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
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
                    return "Implementation.Context.Default.FieldGetter{" +
                            "fieldDescription=" + fieldDescription +
                            '}';
                }
            }

            /**
             * An implementation for a field setter.
             */
            protected static class FieldSetter extends AbstractDelegationEntry {

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
                public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                            FieldAccess.forField(fieldDescription).putter(),
                            MethodReturn.VOID
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
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
                    return "Implementation.Context.Default.FieldSetter{" +
                            "fieldDescription=" + fieldDescription +
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
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && Arrays.equals(implementation, ((Compound) o).implementation);
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

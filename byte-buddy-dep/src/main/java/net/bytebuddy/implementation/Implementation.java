package net.bytebuddy.implementation;

import lombok.EqualsAndHashCode;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
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
public interface Implementation extends InstrumentedType.Prepareable {

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
     * Represents an implementation that can be chained together with another implementation.
     */
    interface Composable extends Implementation {

        /**
         * Appends the supplied implementation to this implementation.
         *
         * @param implementation The subsequent implementation.
         * @return An implementation that combines this implementation with the provided one.
         */
        Implementation andThen(Implementation implementation);
    }

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
                return 31 * getMethodDescription().asSignatureToken().hashCode() + getTypeDescription().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof SpecialMethodInvocation)) return false;
                SpecialMethodInvocation specialMethodInvocation = (SpecialMethodInvocation) other;
                return getMethodDescription().asSignatureToken().equals(specialMethodInvocation.getMethodDescription().asSignatureToken())
                        && getTypeDescription().equals(((SpecialMethodInvocation) other).getTypeDescription());
            }
        }

        /**
         * A canonical implementation of a {@link SpecialMethodInvocation}.
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
        TypeDescription getInstrumentedType();

        /**
         * Identifies the origin type of an implementation. The origin type describes the type that is subject to
         * any form of enhancement. If a subclass of a given type is generated, the base type of this subclass
         * describes the origin type. If a given type is redefined or rebased, the origin type is described by the
         * instrumented type itself.
         *
         * @return The origin type of this implementation.
         */
        TypeDefinition getOriginType();

        /**
         * Creates a special method invocation for invoking the super method of the given method.
         *
         * @param token A token of the method that is to be invoked as a super method.
         * @return The corresponding special method invocation which might be illegal if the requested invocation is not legal.
         */
        SpecialMethodInvocation invokeSuper(MethodDescription.SignatureToken token);

        /**
         * Creates a special method invocation for invoking a default method with the given token. The default method call must
         * not be ambiguous or an illegal special method invocation is returned.
         *
         * @param token A token of the method that is to be invoked as a default method.
         * @return The corresponding default method invocation which might be illegal if the requested invocation is not legal or ambiguous.
         */
        SpecialMethodInvocation invokeDefault(MethodDescription.SignatureToken token);

        /**
         * Creates a special method invocation for invoking a default method.
         *
         * @param targetType The interface on which the default method is to be invoked.
         * @param token      A token that uniquely describes the method to invoke.
         * @return The corresponding special method invocation which might be illegal if the requested invocation is
         * not legal.
         */
        SpecialMethodInvocation invokeDefault(MethodDescription.SignatureToken token, TypeDescription targetType);

        /**
         * Invokes a dominant method, i.e. if the method token can be invoked as a super method invocation, this invocation is considered dominant.
         * Alternatively, a method invocation is attempted on an interface type as a default method invocation only if this invocation is not ambiguous
         * for several interfaces.
         *
         * @param token The method token representing the method to be invoked.
         * @return A special method invocation for a method representing the method token.
         */
        SpecialMethodInvocation invokeDominant(MethodDescription.SignatureToken token);

        /**
         * A factory for creating an {@link Implementation.Target}.
         */
        interface Factory {

            /**
             * Creates an implementation target.
             *
             * @param instrumentedType The instrumented type.
             * @param methodGraph      A method graph of the instrumented type.
             * @param classFileVersion The type's class file version.
             * @return An implementation target for the instrumented type.
             */
            Target make(TypeDescription instrumentedType, MethodGraph.Linked methodGraph, ClassFileVersion classFileVersion);
        }

        /**
         * An abstract base implementation for an {@link Implementation.Target}.
         */
        @EqualsAndHashCode
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
             * The default method invocation mode to apply.
             */
            protected final DefaultMethodInvocation defaultMethodInvocation;

            /**
             * Creates a new implementation target.
             *
             * @param instrumentedType        The instrumented type.
             * @param methodGraph             The instrumented type's method graph.
             * @param defaultMethodInvocation The default method invocation mode to apply.
             */
            protected AbstractBase(TypeDescription instrumentedType, MethodGraph.Linked methodGraph, DefaultMethodInvocation defaultMethodInvocation) {
                this.instrumentedType = instrumentedType;
                this.methodGraph = methodGraph;
                this.defaultMethodInvocation = defaultMethodInvocation;
            }

            @Override
            public TypeDescription getInstrumentedType() {
                return instrumentedType;
            }

            @Override
            public SpecialMethodInvocation invokeDefault(MethodDescription.SignatureToken token) {
                SpecialMethodInvocation specialMethodInvocation = SpecialMethodInvocation.Illegal.INSTANCE;
                for (TypeDescription interfaceType : instrumentedType.getInterfaces().asErasures()) {
                    SpecialMethodInvocation invocation = invokeDefault(token, interfaceType);
                    if (invocation.isValid()) {
                        if (specialMethodInvocation.isValid()) {
                            return SpecialMethodInvocation.Illegal.INSTANCE;
                        } else {
                            specialMethodInvocation = invocation;
                        }
                    }
                }
                return specialMethodInvocation;
            }

            @Override
            public SpecialMethodInvocation invokeDefault(MethodDescription.SignatureToken token, TypeDescription targetType) {
                return defaultMethodInvocation.apply(methodGraph.getInterfaceGraph(targetType).locate(token), targetType);
            }

            @Override
            public SpecialMethodInvocation invokeDominant(MethodDescription.SignatureToken token) {
                SpecialMethodInvocation specialMethodInvocation = invokeSuper(token);
                return specialMethodInvocation.isValid()
                        ? specialMethodInvocation
                        : invokeDefault(token);
            }

            /**
             * Determines if default method invocations are possible.
             */
            protected enum DefaultMethodInvocation {

                /**
                 * Permits default method invocations, if an interface declaring a default method is possible.
                 */
                ENABLED {
                    @Override
                    protected SpecialMethodInvocation apply(MethodGraph.Node node, TypeDescription targetType) {
                        return node.getSort().isUnique()
                                ? SpecialMethodInvocation.Simple.of(node.getRepresentative(), targetType)
                                : SpecialMethodInvocation.Illegal.INSTANCE;
                    }
                },

                /**
                 * Does not permit default method invocations.
                 */
                DISABLED {
                    @Override
                    protected SpecialMethodInvocation apply(MethodGraph.Node node, TypeDescription targetType) {
                        return SpecialMethodInvocation.Illegal.INSTANCE;
                    }
                };

                /**
                 * Resolves a default method invocation depending on the class file version permitting such calls.
                 *
                 * @param classFileVersion The class file version to resolve for.
                 * @return A suitable default method invocation mode.
                 */
                public static DefaultMethodInvocation of(ClassFileVersion classFileVersion) {
                    return classFileVersion.isAtLeast(ClassFileVersion.JAVA_V8)
                            ? ENABLED
                            : DISABLED;
                }

                /**
                 * Resolves a default method invocation for a given node.
                 *
                 * @param node       The node representing the default method call.
                 * @param targetType The target type defining the default method.
                 * @return A suitable special method invocation.
                 */
                protected abstract SpecialMethodInvocation apply(MethodGraph.Node node, TypeDescription targetType);
            }
        }
    }

    /**
     * The context for an implementation application. An implementation context represents a mutable data structure
     * where any registration is irrevocable. Calling methods on an implementation context should be considered equally
     * sensitive as calling a {@link org.objectweb.asm.MethodVisitor}. As such, an implementation context and a
     * {@link org.objectweb.asm.MethodVisitor} are complementary for creating an new Java type.
     */
    interface Context extends MethodAccessorFactory {

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
        FieldDescription.InDefinedShape cache(StackManipulation fieldValue, TypeDescription fieldType);

        /**
         * Returns the instrumented type of the current implementation. The instrumented type is exposed with the intend of allowing optimal
         * byte code generation and not for implementing checks or changing the behavior of a {@link StackManipulation}.
         *
         * @return The instrumented type of the current implementation.
         */
        TypeDescription getInstrumentedType();

        /**
         * Returns the class file version of the currently created dynamic type.
         *
         * @return The class file version of the currently created dynamic type.
         */
        ClassFileVersion getClassFileVersion();

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
            List<DynamicType> getAuxiliaryTypes();

            /**
             * Writes any information that was registered with an {@link Implementation.Context}
             * to the provided class visitor. This contains any fields for value caching, any accessor method and it
             * writes the type initializer. The type initializer must therefore never be written manually.
             *
             * @param drain                        The drain to write the type initializer to.
             * @param classVisitor                 The class visitor to which the extractable view is to be written.
             * @param annotationValueFilterFactory The annotation value filter factory to apply when writing annotation.
             */
            void drain(TypeInitializer.Drain drain, ClassVisitor classVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * An abstract base implementation of an extractable view of an implementation context.
             */
            @EqualsAndHashCode
            abstract class AbstractBase implements ExtractableView {

                /**
                 * The instrumented type.
                 */
                protected final TypeDescription instrumentedType;

                /**
                 * The class file version of the dynamic type.
                 */
                protected final ClassFileVersion classFileVersion;

                /**
                 * Create a new extractable view.
                 *
                 * @param instrumentedType The instrumented type.
                 * @param classFileVersion The class file version of the dynamic type.
                 */
                protected AbstractBase(TypeDescription instrumentedType, ClassFileVersion classFileVersion) {
                    this.instrumentedType = instrumentedType;
                    this.classFileVersion = classFileVersion;
                }

                @Override
                public TypeDescription getInstrumentedType() {
                    return instrumentedType;
                }

                @Override
                public ClassFileVersion getClassFileVersion() {
                    return classFileVersion;
                }
            }
        }

        /**
         * A factory for creating a new implementation context.
         */
        interface Factory {

            /**
             * Creates a new implementation context.
             *
             * @param instrumentedType            The description of the type that is currently subject of creation.
             * @param auxiliaryTypeNamingStrategy The naming strategy for naming an auxiliary type.
             * @param typeInitializer             The type initializer of the created instrumented type.
             * @param classFileVersion            The class file version of the created class.
             * @param auxiliaryClassFileVersion   The class file version of any auxiliary classes.
             * @return An implementation context in its extractable view.
             */
            ExtractableView make(TypeDescription instrumentedType,
                                 AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                 TypeInitializer typeInitializer,
                                 ClassFileVersion classFileVersion,
                                 ClassFileVersion auxiliaryClassFileVersion);
        }

        /**
         * An implementation context that does not allow for any injections into the static initializer block. This can be useful when
         * redefining a class when it is not allowed to add methods to a class what is an implicit requirement when copying the static
         * initializer block into another method.
         */
        class Disabled extends ExtractableView.AbstractBase {

            /**
             * Creates a new disabled implementation context.
             *
             * @param instrumentedType The instrumented type.
             * @param classFileVersion The class file version to create the class in.
             */
            protected Disabled(TypeDescription instrumentedType, ClassFileVersion classFileVersion) {
                super(instrumentedType, classFileVersion);
            }

            @Override
            public List<DynamicType> getAuxiliaryTypes() {
                return Collections.emptyList();
            }

            @Override
            public void drain(TypeInitializer.Drain drain, ClassVisitor classVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                drain.apply(classVisitor, TypeInitializer.None.INSTANCE, this);
            }

            @Override
            public TypeDescription register(AuxiliaryType auxiliaryType) {
                throw new IllegalStateException("Registration of auxiliary types was disabled: " + auxiliaryType);
            }

            @Override
            public MethodDescription.InDefinedShape registerAccessorFor(SpecialMethodInvocation specialMethodInvocation, AccessType accessType) {
                throw new IllegalStateException("Registration of method accessors was disabled: " + specialMethodInvocation.getMethodDescription());
            }

            @Override
            public MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription, AccessType accessType) {
                throw new IllegalStateException("Registration of field accessor was disabled: " + fieldDescription);
            }

            @Override
            public MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription, AccessType accessType) {
                throw new IllegalStateException("Registration of field accessor was disabled: " + fieldDescription);
            }

            @Override
            public FieldDescription.InDefinedShape cache(StackManipulation fieldValue, TypeDescription fieldType) {
                throw new IllegalStateException("Field values caching was disabled: " + fieldType);
            }

            /**
             * A factory for creating a {@link net.bytebuddy.implementation.Implementation.Context.Disabled}.
             */
            public enum Factory implements Context.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public ExtractableView make(TypeDescription instrumentedType,
                                            AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                            TypeInitializer typeInitializer,
                                            ClassFileVersion classFileVersion,
                                            ClassFileVersion auxiliaryClassFileVersion) {
                    if (typeInitializer.isDefined()) {
                        throw new IllegalStateException("Cannot define type initializer which was explicitly disabled: " + typeInitializer);
                    }
                    return new Disabled(instrumentedType, classFileVersion);
                }
            }
        }

        /**
         * A default implementation of an {@link Implementation.Context.ExtractableView}
         * which serves as its own {@link MethodAccessorFactory}.
         */
        class Default extends ExtractableView.AbstractBase {

            /**
             * The name suffix to be appended to an accessor method.
             */
            public static final String ACCESSOR_METHOD_SUFFIX = "accessor";

            /**
             * The name prefix to be prepended to a field storing a cached value.
             */
            public static final String FIELD_CACHE_PREFIX = "cachedValue";

            /**
             * The naming strategy for naming auxiliary types that are registered.
             */
            private final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

            /**
             * The type initializer of the created instrumented type.
             */
            private final TypeInitializer typeInitializer;

            /**
             * The class file version to use for auxiliary classes.
             */
            private final ClassFileVersion auxiliaryClassFileVersion;

            /**
             * A mapping of special method invocations to their accessor methods that each invoke their mapped invocation.
             */
            private final Map<SpecialMethodInvocation, DelegationRecord> registeredAccessorMethods;

            /**
             * The registered getters.
             */
            private final Map<FieldDescription, DelegationRecord> registeredGetters;

            /**
             * The registered setters.
             */
            private final Map<FieldDescription, DelegationRecord> registeredSetters;

            /**
             * A map of registered auxiliary types to their dynamic type representation.
             */
            private final Map<AuxiliaryType, DynamicType> auxiliaryTypes;

            /**
             * A map of already registered field caches to their field representation.
             */
            private final Map<FieldCacheEntry, FieldDescription.InDefinedShape> registeredFieldCacheEntries;

            /**
             * A random suffix to append to the names of accessor methods.
             */
            private final String suffix;

            /**
             * If {@code false}, the type initializer for this instance was already drained what prohibits the registration of additional cached field values.
             */
            private boolean fieldCacheCanAppendEntries;

            /**
             * Creates a new default implementation context.
             *
             * @param instrumentedType            The description of the type that is currently subject of creation.
             * @param classFileVersion            The class file version of the created class.
             * @param auxiliaryTypeNamingStrategy The naming strategy for naming an auxiliary type.
             * @param typeInitializer             The type initializer of the created instrumented type.
             * @param auxiliaryClassFileVersion   The class file version to use for auxiliary classes.
             */
            protected Default(TypeDescription instrumentedType,
                              ClassFileVersion classFileVersion,
                              AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                              TypeInitializer typeInitializer,
                              ClassFileVersion auxiliaryClassFileVersion) {
                super(instrumentedType, classFileVersion);
                this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
                this.typeInitializer = typeInitializer;
                this.auxiliaryClassFileVersion = auxiliaryClassFileVersion;
                registeredAccessorMethods = new HashMap<SpecialMethodInvocation, DelegationRecord>();
                registeredGetters = new HashMap<FieldDescription, DelegationRecord>();
                registeredSetters = new HashMap<FieldDescription, DelegationRecord>();
                auxiliaryTypes = new HashMap<AuxiliaryType, DynamicType>();
                registeredFieldCacheEntries = new HashMap<FieldCacheEntry, FieldDescription.InDefinedShape>();
                suffix = RandomString.make();
                fieldCacheCanAppendEntries = true;
            }

            @Override
            public MethodDescription.InDefinedShape registerAccessorFor(SpecialMethodInvocation specialMethodInvocation, AccessType accessType) {
                DelegationRecord record = registeredAccessorMethods.get(specialMethodInvocation);
                record = record == null
                        ? new AccessorMethodDelegation(instrumentedType, suffix, accessType, specialMethodInvocation)
                        : record.with(accessType);
                registeredAccessorMethods.put(specialMethodInvocation, record);
                return record.getMethod();
            }

            @Override
            public MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription, AccessType accessType) {
                DelegationRecord record = registeredGetters.get(fieldDescription);
                record = record == null
                        ? new FieldGetterDelegation(instrumentedType, suffix, accessType, fieldDescription)
                        : record.with(accessType);
                registeredGetters.put(fieldDescription, record);
                return record.getMethod();
            }

            @Override
            public MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription, AccessType accessType) {
                DelegationRecord record = registeredSetters.get(fieldDescription);
                record = record == null
                        ? new FieldSetterDelegation(instrumentedType, suffix, accessType, fieldDescription)
                        : record.with(accessType);
                registeredSetters.put(fieldDescription, record);
                return record.getMethod();
            }

            @Override
            public TypeDescription register(AuxiliaryType auxiliaryType) {
                DynamicType dynamicType = auxiliaryTypes.get(auxiliaryType);
                if (dynamicType == null) {
                    dynamicType = auxiliaryType.make(auxiliaryTypeNamingStrategy.name(instrumentedType), auxiliaryClassFileVersion, this);
                    auxiliaryTypes.put(auxiliaryType, dynamicType);
                }
                return dynamicType.getTypeDescription();
            }

            @Override
            public List<DynamicType> getAuxiliaryTypes() {
                return new ArrayList<DynamicType>(auxiliaryTypes.values());
            }

            @Override
            public FieldDescription.InDefinedShape cache(StackManipulation fieldValue, TypeDescription fieldType) {
                FieldCacheEntry fieldCacheEntry = new FieldCacheEntry(fieldValue, fieldType);
                FieldDescription.InDefinedShape fieldCache = registeredFieldCacheEntries.get(fieldCacheEntry);
                if (fieldCache != null) {
                    return fieldCache;
                }
                if (!fieldCacheCanAppendEntries) {
                    throw new IllegalStateException("Cached values cannot be registered after defining the type initializer for " + instrumentedType);
                }
                fieldCache = new CacheValueField(instrumentedType, fieldType.asGenericType(), suffix, fieldValue.hashCode());
                registeredFieldCacheEntries.put(fieldCacheEntry, fieldCache);
                return fieldCache;
            }

            @Override
            public void drain(TypeInitializer.Drain drain,
                              ClassVisitor classVisitor,
                              AnnotationValueFilter.Factory annotationValueFilterFactory) {
                fieldCacheCanAppendEntries = false;
                TypeInitializer typeInitializer = this.typeInitializer;
                for (Map.Entry<FieldCacheEntry, FieldDescription.InDefinedShape> entry : registeredFieldCacheEntries.entrySet()) {
                    classVisitor.visitField(entry.getValue().getModifiers(),
                            entry.getValue().getInternalName(),
                            entry.getValue().getDescriptor(),
                            entry.getValue().getGenericSignature(),
                            FieldDescription.NO_DEFAULT_VALUE).visitEnd();
                    typeInitializer = typeInitializer.expandWith(entry.getKey().storeIn(entry.getValue()));
                }
                drain.apply(classVisitor, typeInitializer, this);
                for (TypeWriter.MethodPool.Record record : registeredAccessorMethods.values()) {
                    record.apply(classVisitor, this, annotationValueFilterFactory);
                }
                for (TypeWriter.MethodPool.Record record : registeredGetters.values()) {
                    record.apply(classVisitor, this, annotationValueFilterFactory);
                }
                for (TypeWriter.MethodPool.Record record : registeredSetters.values()) {
                    record.apply(classVisitor, this, annotationValueFilterFactory);
                }
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
                private final TypeDescription.Generic fieldType;

                /**
                 * The suffix to use for the cache field's name.
                 */
                private final String suffix;

                /**
                 * The hash value of the field's value for creating a unique field name.
                 */
                private final int hashCode;

                /**
                 * Creates a new cache value field.
                 *
                 * @param instrumentedType The instrumented type.
                 * @param fieldType        The type of the cache's field.
                 * @param suffix           The suffix to use for the cache field's name.
                 * @param hashCode         The hash value of the field's value for creating a unique field name.
                 */
                protected CacheValueField(TypeDescription instrumentedType, TypeDescription.Generic fieldType, String suffix, int hashCode) {
                    this.instrumentedType = instrumentedType;
                    this.fieldType = fieldType;
                    this.suffix = suffix;
                    this.hashCode = hashCode;
                }

                @Override
                public TypeDescription.Generic getType() {
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
                    return Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | (instrumentedType.isInterface()
                            ? Opcodes.ACC_PUBLIC
                            : Opcodes.ACC_PRIVATE);
                }

                @Override
                public String getName() {
                    return String.format("%s$%s$%s", FIELD_CACHE_PREFIX, suffix, RandomString.hashOf(hashCode));
                }
            }

            /**
             * A field cache entry for uniquely identifying a cached field. A cached field is described by the stack
             * manipulation that loads the field's value onto the operand stack and the type of the field.
             */
            @EqualsAndHashCode
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
                 * @return A byte code appender that represents this storage.
                 */
                protected ByteCodeAppender storeIn(FieldDescription fieldDescription) {
                    return new ByteCodeAppender.Simple(this, FieldAccess.forField(fieldDescription).write());
                }

                /**
                 * Returns the field type that is represented by this field cache entry.
                 *
                 * @return The field type that is represented by this field cache entry.
                 */
                protected TypeDescription getFieldType() {
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
            }

            /**
             * A base implementation of a method that accesses a property of an instrumented type.
             */
            protected abstract static class AbstractPropertyAccessorMethod extends MethodDescription.InDefinedShape.AbstractBase {

                @Override
                public int getModifiers() {
                    return Opcodes.ACC_SYNTHETIC | getBaseModifiers() | (getDeclaringType().isInterface()
                            ? Opcodes.ACC_PUBLIC
                            : Opcodes.ACC_FINAL);
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
                public TypeDescription.Generic getReturnType() {
                    return methodDescription.getReturnType().asRawType();
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Explicit.ForTypes(this, methodDescription.getParameters().asTypeList().asRawTypes());
                }

                @Override
                public TypeList.Generic getExceptionTypes() {
                    return methodDescription.getExceptionTypes().asRawTypes();
                }

                @Override
                public AnnotationValue<?, ?> getDefaultValue() {
                    return AnnotationValue.UNDEFINED;
                }

                @Override
                public TypeList.Generic getTypeVariables() {
                    return new TypeList.Generic.Empty();
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
                public TypeDescription.Generic getReturnType() {
                    return fieldDescription.getType().asRawType();
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Empty<ParameterDescription.InDefinedShape>();
                }

                @Override
                public TypeList.Generic getExceptionTypes() {
                    return new TypeList.Generic.Empty();
                }

                @Override
                public AnnotationValue<?, ?> getDefaultValue() {
                    return AnnotationValue.UNDEFINED;
                }

                @Override
                public TypeList.Generic getTypeVariables() {
                    return new TypeList.Generic.Empty();
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
                public TypeDescription.Generic getReturnType() {
                    return TypeDescription.Generic.VOID;
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Explicit.ForTypes(this, Collections.singletonList(fieldDescription.getType().asRawType()));
                }

                @Override
                public TypeList.Generic getExceptionTypes() {
                    return new TypeList.Generic.Empty();
                }

                @Override
                public AnnotationValue<?, ?> getDefaultValue() {
                    return AnnotationValue.UNDEFINED;
                }

                @Override
                public TypeList.Generic getTypeVariables() {
                    return new TypeList.Generic.Empty();
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
            @EqualsAndHashCode(callSuper = false)
            protected abstract static class DelegationRecord extends TypeWriter.MethodPool.Record.ForDefinedMethod implements ByteCodeAppender {

                /**
                 * The delegation method.
                 */
                protected final MethodDescription.InDefinedShape methodDescription;

                /**
                 * The record's visibility.
                 */
                protected final Visibility visibility;

                /**
                 * Creates a new delegation record.
                 *
                 * @param methodDescription The delegation method.
                 * @param visibility        The method's actual visibility.
                 */
                protected DelegationRecord(MethodDescription.InDefinedShape methodDescription, Visibility visibility) {
                    this.methodDescription = methodDescription;
                    this.visibility = visibility;
                }

                /**
                 * Returns this delegation record with the minimal visibility represented by the supplied access type.
                 *
                 * @param accessType The access type to enforce.
                 * @return A new version of this delegation record with the minimal implied visibility.
                 */
                protected abstract DelegationRecord with(AccessType accessType);

                @Override
                public MethodDescription.InDefinedShape getMethod() {
                    return methodDescription;
                }

                @Override
                public Sort getSort() {
                    return Sort.IMPLEMENTED;
                }

                @Override
                public Visibility getVisibility() {
                    return visibility;
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor, Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    methodVisitor.visitCode();
                    Size size = applyCode(methodVisitor, implementationContext);
                    methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                }

                @Override
                public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    /* do nothing */
                }

                @Override
                public Size applyCode(MethodVisitor methodVisitor, Context implementationContext) {
                    return apply(methodVisitor, implementationContext, getMethod());
                }

                @Override
                public TypeWriter.MethodPool.Record prepend(ByteCodeAppender byteCodeAppender) {
                    throw new UnsupportedOperationException("Cannot prepend code to a delegation for " + methodDescription);
                }
            }

            /**
             * An implementation of a {@link TypeWriter.MethodPool.Record} for implementing
             * an accessor method.
             */
            @EqualsAndHashCode(callSuper = true)
            protected static class AccessorMethodDelegation extends DelegationRecord {

                /**
                 * The stack manipulation that represents the requested special method invocation.
                 */
                private final StackManipulation accessorMethodInvocation;

                /**
                 * Creates a delegation to an accessor method.
                 *
                 * @param instrumentedType        The instrumented type.
                 * @param suffix                  The suffix to append to the method.
                 * @param accessType              The access type.
                 * @param specialMethodInvocation The actual method's invocation.
                 */
                protected AccessorMethodDelegation(TypeDescription instrumentedType,
                                                   String suffix,
                                                   AccessType accessType,
                                                   SpecialMethodInvocation specialMethodInvocation) {
                    this(new AccessorMethod(instrumentedType, specialMethodInvocation.getMethodDescription(), suffix),
                            accessType.getVisibility(),
                            specialMethodInvocation);
                }

                /**
                 * Creates a delegation to an accessor method.
                 *
                 * @param methodDescription        The accessor method.
                 * @param visibility               The method's visibility.
                 * @param accessorMethodInvocation The actual method's invocation.
                 */
                private AccessorMethodDelegation(MethodDescription.InDefinedShape methodDescription,
                                                 Visibility visibility,
                                                 StackManipulation accessorMethodInvocation) {
                    super(methodDescription, visibility);
                    this.accessorMethodInvocation = accessorMethodInvocation;
                }

                @Override
                protected DelegationRecord with(AccessType accessType) {
                    return new AccessorMethodDelegation(methodDescription, visibility.expandTo(accessType.getVisibility()), accessorMethodInvocation);
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                            accessorMethodInvocation,
                            MethodReturn.of(instrumentedMethod.getReturnType())
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }
            }

            /**
             * An implementation for a field getter.
             */
            @EqualsAndHashCode(callSuper = true)
            protected static class FieldGetterDelegation extends DelegationRecord {

                /**
                 * The field to read from.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new field getter implementation.
                 *
                 * @param instrumentedType The instrumented type.
                 * @param suffix           The suffix to use for the setter method.
                 * @param accessType       The method's access type.
                 * @param fieldDescription The field to write to.
                 */
                protected FieldGetterDelegation(TypeDescription instrumentedType, String suffix, AccessType accessType, FieldDescription fieldDescription) {
                    this(new FieldGetter(instrumentedType, fieldDescription, suffix), accessType.getVisibility(), fieldDescription);
                }

                /**
                 * Creates a new field getter implementation.
                 *
                 * @param methodDescription The delegation method.
                 * @param visibility        The delegation method's visibility.
                 * @param fieldDescription  The field to read.
                 */
                private FieldGetterDelegation(MethodDescription.InDefinedShape methodDescription, Visibility visibility, FieldDescription fieldDescription) {
                    super(methodDescription, visibility);
                    this.fieldDescription = fieldDescription;
                }

                @Override
                protected DelegationRecord with(AccessType accessType) {
                    return new FieldGetterDelegation(methodDescription, visibility.expandTo(accessType.getVisibility()), fieldDescription);
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.Trivial.INSTANCE
                                    : MethodVariableAccess.loadThis(),
                            FieldAccess.forField(fieldDescription).read(),
                            MethodReturn.of(fieldDescription.getType())
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }
            }

            /**
             * An implementation for a field setter.
             */
            @EqualsAndHashCode(callSuper = true)
            protected static class FieldSetterDelegation extends DelegationRecord {

                /**
                 * The field to write to.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new field setter implementation.
                 *
                 * @param instrumentedType The instrumented type.
                 * @param suffix           The suffix to use for the setter method.
                 * @param accessType       The method's access type.
                 * @param fieldDescription The field to write to.
                 */
                protected FieldSetterDelegation(TypeDescription instrumentedType, String suffix, AccessType accessType, FieldDescription fieldDescription) {
                    this(new FieldSetter(instrumentedType, fieldDescription, suffix), accessType.getVisibility(), fieldDescription);
                }

                /**
                 * Creates a new field setter.
                 *
                 * @param methodDescription The field accessor method.
                 * @param visibility        The delegation method's visibility.
                 * @param fieldDescription  The field to write to.
                 */
                private FieldSetterDelegation(MethodDescription.InDefinedShape methodDescription, Visibility visibility, FieldDescription fieldDescription) {
                    super(methodDescription, visibility);
                    this.fieldDescription = fieldDescription;
                }

                @Override
                protected DelegationRecord with(AccessType accessType) {
                    return new FieldSetterDelegation(methodDescription, visibility.expandTo(accessType.getVisibility()), fieldDescription);
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                            FieldAccess.forField(fieldDescription).write(),
                            MethodReturn.VOID
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }
            }

            /**
             * A factory for creating a {@link net.bytebuddy.implementation.Implementation.Context.Default}.
             */
            public enum Factory implements ExtractableView.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public ExtractableView make(TypeDescription instrumentedType,
                                            AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                            TypeInitializer typeInitializer,
                                            ClassFileVersion classFileVersion,
                                            ClassFileVersion auxiliaryClassFileVersion) {
                    return new Default(instrumentedType, classFileVersion, auxiliaryTypeNamingStrategy, typeInitializer, auxiliaryClassFileVersion);
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
    @EqualsAndHashCode
    class Compound implements Implementation {

        /**
         * All implementation that are represented by this compound implementation.
         */
        private final List<Implementation> implementations;

        /**
         * Creates a new immutable compound implementation.
         *
         * @param implementation The implementations to combine in their order.
         */
        public Compound(Implementation... implementation) {
            this(Arrays.asList(implementation));
        }

        /**
         * Creates a new immutable compound implementation.
         *
         * @param implementations The implementations to combine in their order.
         */
        public Compound(List<? extends Implementation> implementations) {
            this.implementations = new ArrayList<Implementation>();
            for (Implementation implementation : implementations) {
                if (implementation instanceof Compound) {
                    this.implementations.addAll(((Compound) implementation).implementations);
                } else {
                    this.implementations.add(implementation);
                }
            }
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            for (Implementation implementation : implementations) {
                instrumentedType = implementation.prepare(instrumentedType);
            }
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            ByteCodeAppender[] byteCodeAppender = new ByteCodeAppender[implementations.size()];
            int index = 0;
            for (Implementation implementation : implementations) {
                byteCodeAppender[index++] = implementation.appender(implementationTarget);
            }
            return new ByteCodeAppender.Compound(byteCodeAppender);
        }
    }

    /**
     * A simple implementation that does not register any members with the instrumented type.
     */
    @EqualsAndHashCode
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
    }
}

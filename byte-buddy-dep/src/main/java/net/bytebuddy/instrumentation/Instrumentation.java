package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import org.objectweb.asm.MethodVisitor;

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
     * call. On this method call, type initializers can also be added to the instrumented type.
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
                throw new IllegalStateException();
            }

            @Override
            public TypeDescription getTypeDescription() {
                throw new IllegalStateException();
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
            private Simple(MethodDescription methodDescription,
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
                if (other == null || getClass() != other.getClass()) return false;
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
                return "Instrumentation.SpecialMethodInvocation.Legal{" +
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
             * @param methodLookupEngineFinding The finding of a method lookup engine on analyzing the
             *                                  instrumented type.
             * @return An {@link net.bytebuddy.instrumentation.Instrumentation.Target} for the given type description.
             */
            Target make(MethodLookupEngine.Finding methodLookupEngineFinding);
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
             * Returns any {@link net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType} that was registered
             * with this {@link net.bytebuddy.instrumentation.Instrumentation.Context}.
             *
             * @return A list of all manifested registered auxiliary types.
             */
            List<DynamicType> getRegisteredAuxiliaryTypes();

            /**
             * Returns a list of the descriptions of all fields of registered field caches.
             *
             * @return A list of the descriptions of all fields of registered field caches.
             */
            List<FieldDescription> getRegisteredFieldCaches();
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
            return "Compound{" + Arrays.toString(instrumentation) + '}';
        }
    }
}

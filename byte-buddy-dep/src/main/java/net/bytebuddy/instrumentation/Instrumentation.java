package net.bytebuddy.instrumentation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
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
 * {@link net.bytebuddy.instrumentation.TypeInitializer}s can be registered for an instrumented
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
         * A factory for creating an {@link net.bytebuddy.instrumentation.Instrumentation.Target}.
         */
        static interface Factory {

            /**
             * Creates an {@link net.bytebuddy.instrumentation.Instrumentation.Target} for the given instrumented
             * type's description.
             *
             * @param typeDescription The type description for which the instrumentation target should be created.
             * @return An {@link net.bytebuddy.instrumentation.Instrumentation.Target} for the given type description.
             */
            Target make(TypeDescription typeDescription);
        }

        /**
         * A default implementation of a {@link net.bytebuddy.instrumentation.Instrumentation.Target}.
         */
        static class Default implements Target {

            private final TypeDescription typeDescription;

            /**
             * Creates a new default {@link net.bytebuddy.instrumentation.Instrumentation.Target}.
             *
             * @param typeDescription A description of the instrumented type.
             */
            public Default(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            @Override
            public TypeDescription getTypeDescription() {
                return typeDescription;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && typeDescription.equals(((Default) other).typeDescription);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            public String toString() {
                return "Instrumentation.Target.Default{" +
                        "typeDescription=" + typeDescription +
                        '}';
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
         * Returns a list of auxiliary types that are currently registered for the instrumentation for this context.
         *
         * @return A list containing all auxiliary types currently registered.
         */
        List<DynamicType> getRegisteredAuxiliaryTypes();

        /**
         * A convenience implementation of an instrumentation context that allows for a better composition
         * of the instrumentation context implementation.
         */
        static class Default implements Context, AuxiliaryType.MethodAccessorFactory {

            private final ClassFileVersion classFileVersion;
            private final AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy;
            private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;
            private final Map<AuxiliaryType, DynamicType> auxiliaryTypes;
            private final Map<MethodDescription, MethodDescription> registeredAccessorMethods;

            /**
             * Creates a new default instrumentation context.
             *
             * @param classFileVersion            The class file version for auxiliary types.
             * @param auxiliaryTypeNamingStrategy The naming strategy for auxiliary types that are registered.
             * @param methodAccessorFactory       A factory for creating method proxies for the currently instrumented
             *                                    type.
             */
            public Default(ClassFileVersion classFileVersion,
                           AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy,
                           AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                this.classFileVersion = classFileVersion;
                this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
                this.methodAccessorFactory = methodAccessorFactory;
                auxiliaryTypes = new HashMap<AuxiliaryType, DynamicType>();
                registeredAccessorMethods = new HashMap<MethodDescription, MethodDescription>();
            }

            @Override
            public TypeDescription register(AuxiliaryType auxiliaryType) {
                DynamicType dynamicType = auxiliaryTypes.get(auxiliaryType);
                if (dynamicType == null) {
                    dynamicType = auxiliaryType.make(auxiliaryTypeNamingStrategy.name(auxiliaryType), classFileVersion, this);
                    auxiliaryTypes.put(auxiliaryType, dynamicType);
                }
                return dynamicType.getDescription();
            }

            @Override
            public MethodDescription requireAccessorMethodFor(MethodDescription targetMethod, LookupMode lookupMode) {
                MethodDescription accessorMethod = registeredAccessorMethods.get(targetMethod);
                if (accessorMethod == null) {
                    accessorMethod = methodAccessorFactory.requireAccessorMethodFor(targetMethod, lookupMode);
                    registeredAccessorMethods.put(targetMethod, accessorMethod);
                }
                return accessorMethod;
            }

            @Override
            public List<DynamicType> getRegisteredAuxiliaryTypes() {
                return Collections.unmodifiableList(new ArrayList<DynamicType>(auxiliaryTypes.values()));
            }

            @Override
            public String toString() {
                return "Default{" +
                        "classFileVersion=" + classFileVersion +
                        ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                        ", methodAccessorFactory=" + methodAccessorFactory +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        ", registeredAccessorMethods=" + registeredAccessorMethods +
                        '}';
            }

            /**
             * Representation of a naming strategy for an auxiliary type.
             */
            public static interface AuxiliaryTypeNamingStrategy {

                /**
                 * NAmes an auxiliary type.
                 *
                 * @param auxiliaryType The auxiliary type to name.
                 * @return The fully qualified name for the given auxiliary type.
                 */
                String name(AuxiliaryType auxiliaryType);
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

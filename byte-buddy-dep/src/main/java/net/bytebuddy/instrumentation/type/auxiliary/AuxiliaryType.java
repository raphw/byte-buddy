package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.modifier.SyntheticState;
import org.objectweb.asm.Opcodes;

/**
 * An auxiliary type that provides services to the instrumentation of another type. Implementations should provide
 * meaningful {@code equals(Object)} and {@code hashCode()} implementations in order to avoid multiple creations
 * of this type.
 */
public interface AuxiliaryType {

    /**
     * The default type access of an auxiliary type. This array must not be mutated.
     */
    static final ModifierContributor.ForType[] DEFAULT_TYPE_MODIFIER = {SyntheticState.SYNTHETIC};

    /**
     * Creates a new auxiliary type.
     *
     * @param auxiliaryTypeName     The fully qualified non-internal name for this auxiliary type. The type should be in
     *                              the same package than the instrumented type this auxiliary type is providing services
     *                              to in order to allow package-private access.
     * @param classFileVersion      The class file version the auxiliary class should be written in.
     * @param methodAccessorFactory A factory for accessor methods.
     * @return A dynamically created type representing this auxiliary type.
     */
    DynamicType make(String auxiliaryTypeName,
                     ClassFileVersion classFileVersion,
                     MethodAccessorFactory methodAccessorFactory);

    /**
     * A factory for creating method proxies for an auxiliary type. Such proxies are required to allow a type to
     * call methods of a second type that are usually not accessible for the first type. This strategy is also adapted
     * by the Java compiler that creates accessor methods for example to implement inner classes.
     */
    static interface MethodAccessorFactory {

        /**
         * The modifier for accessor methods. Accessor methods might additionally be {@code static}.
         */
        static final int ACCESSOR_METHOD_MODIFIER = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL;

        /**
         * Registers an accessor method for a
         * {@link net.bytebuddy.instrumentation.Instrumentation.SpecialMethodInvocation} which cannot itself be
         * triggered invoked directly from outside a type. The method is registered on the instrumented type
         * with package-private visibility, similarly to a Java compiler's accessor methods.
         *
         * @param specialMethodInvocation The special method invocation.
         * @return The accessor method for invoking the special method invocation.
         */
        MethodDescription registerAccessorFor(Instrumentation.SpecialMethodInvocation specialMethodInvocation);

        /**
         * Registers a getter for the given {@link net.bytebuddy.instrumentation.field.FieldDescription} which might
         * itself not be accessible from outside the class. The returned getter method defines the field type as
         * its return type, does not take any arguments and is of package-private visibility, similarly to the Java
         * compiler's accessor methods. If the field is {@code static}, this accessor method is also {@code static}.
         *
         * @param fieldDescription The field which is to be accessed.
         * @return A getter method for the given field.
         */
        MethodDescription registerGetterFor(FieldDescription fieldDescription);

        /**
         * Registers a setter for the given {@link net.bytebuddy.instrumentation.field.FieldDescription} which might
         * itself not be accessible from outside the class. The returned setter method defines the field type as
         * its only argument type, returns {@code void} and is of package-private visibility, similarly to the Java
         * compiler's accessor methods. If the field is {@code static}, this accessor method is also {@code static}.
         *
         * @param fieldDescription The field which is to be accessed.
         * @return A setter method for the given field.
         */
        MethodDescription registerSetterFor(FieldDescription fieldDescription);

        /**
         * A method accessor factory that forbids any accessor registration.
         */
        static enum Illegal implements MethodAccessorFactory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodDescription registerAccessorFor(Instrumentation.SpecialMethodInvocation specialMethodInvocation) {
                throw new IllegalStateException("It is illegal to register an accessor for this type");
            }

            @Override
            public MethodDescription registerGetterFor(FieldDescription fieldDescription) {
                throw new IllegalStateException("It is illegal to register a field getter for this type");
            }

            @Override
            public MethodDescription registerSetterFor(FieldDescription fieldDescription) {
                throw new IllegalStateException("It is illegal to register a field setter for this type");
            }
        }
    }
}

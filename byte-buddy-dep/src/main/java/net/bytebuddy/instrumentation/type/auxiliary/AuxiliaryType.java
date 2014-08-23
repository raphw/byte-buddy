package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.modifier.SyntheticState;
import net.bytebuddy.modifier.Visibility;

/**
 * An auxiliary type that provides services to the instrumentation of another type. Implementations should provide
 * meaningful {@code equals(Object)} and {@code hashCode()} implementations in order to avoid multiple creations
 * of this type.
 */
public interface AuxiliaryType {

    /**
     * The default type access of an auxiliary type. This array must not be mutated.
     */
    static final ModifierContributor.ForType[] DEFAULT_TYPE_MODIFIER = {Visibility.PACKAGE_PRIVATE, SyntheticState.SYNTHETIC};

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
         * Registers an accessor method for a
         * {@link net.bytebuddy.instrumentation.Instrumentation.SpecialMethodInvocation} which cannot be triggered
         * invoked directly from outside a type. The method is registered on the instrumented type with package-private
         * visibility, similarly to a Java compiler accessor method.
         *
         * @param specialMethodInvocation The special method invocation.
         * @return The accessor method for invoking the special method invocation.
         */
        MethodDescription registerAccessorFor(Instrumentation.SpecialMethodInvocation specialMethodInvocation);

        static enum Illegal implements MethodAccessorFactory {

            INSTANCE;

            @Override
            public MethodDescription registerAccessorFor(Instrumentation.SpecialMethodInvocation specialMethodInvocation) {
                throw new IllegalStateException("It is illegal to register an accessor for this type");
            }
        }
    }
}

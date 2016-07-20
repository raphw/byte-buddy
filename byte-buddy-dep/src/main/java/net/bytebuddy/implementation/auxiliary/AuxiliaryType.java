package net.bytebuddy.implementation.auxiliary;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.utility.RandomString;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An auxiliary type that provides services to the instrumentation of another type. Implementations should provide
 * meaningful {@code equals(Object)} and {@code hashCode()} implementations in order to avoid multiple creations
 * of this type.
 */
public interface AuxiliaryType {

    /**
     * The default type access of an auxiliary type. <b>This array must not be mutated</b>.
     */
    @SuppressFBWarnings(value = {"MS_MUTABLE_ARRAY", "MS_OOI_PKGPROTECT"}, justification = "The array is not to be modified by contract")
    ModifierContributor.ForType[] DEFAULT_TYPE_MODIFIER = {SyntheticState.SYNTHETIC};

    /**
     * Creates a new auxiliary type.
     *
     * @param auxiliaryTypeName     The fully qualified binary name for this auxiliary type. The type should be in
     *                              the same package than the instrumented type this auxiliary type is providing services
     *                              to in order to allow package-private access.
     * @param classFileVersion      The class file version the auxiliary class should be written in.
     * @param methodAccessorFactory A factory for accessor methods.
     * @return A dynamically created type representing this auxiliary type.
     */
    DynamicType make(String auxiliaryTypeName, ClassFileVersion classFileVersion, MethodAccessorFactory methodAccessorFactory);

    /**
     * A factory for creating method proxies for an auxiliary type. Such proxies are required to allow a type to
     * call methods of a second type that are usually not accessible for the first type. This strategy is also adapted
     * by the Java compiler that creates accessor methods for example to implement inner classes.
     */
    interface MethodAccessorFactory {

        /**
         * Registers an accessor method for a
         * {@link Implementation.SpecialMethodInvocation} which cannot itself be
         * triggered invoked directly from outside a type. The method is registered on the instrumented type
         * with package-private visibility, similarly to a Java compiler's accessor methods.
         *
         * @param specialMethodInvocation The special method invocation.
         * @return The accessor method for invoking the special method invocation.
         */
        MethodDescription.InDefinedShape registerAccessorFor(Implementation.SpecialMethodInvocation specialMethodInvocation);

        /**
         * Registers a getter for the given {@link net.bytebuddy.description.field.FieldDescription} which might
         * itself not be accessible from outside the class. The returned getter method defines the field type as
         * its return type, does not take any arguments and is of package-private visibility, similarly to the Java
         * compiler's accessor methods. If the field is {@code static}, this accessor method is also {@code static}.
         *
         * @param fieldDescription The field which is to be accessed.
         * @return A getter method for the given field.
         */
        MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription);

        /**
         * Registers a setter for the given {@link net.bytebuddy.description.field.FieldDescription} which might
         * itself not be accessible from outside the class. The returned setter method defines the field type as
         * its only argument type, returns {@code void} and is of package-private visibility, similarly to the Java
         * compiler's accessor methods. If the field is {@code static}, this accessor method is also {@code static}.
         *
         * @param fieldDescription The field which is to be accessed.
         * @return A setter method for the given field.
         */
        MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription);

        /**
         * A method accessor factory that forbids any accessor registration.
         */
        enum Illegal implements MethodAccessorFactory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodDescription.InDefinedShape registerAccessorFor(Implementation.SpecialMethodInvocation specialMethodInvocation) {
                throw new IllegalStateException("It is illegal to register an accessor for this type");
            }

            @Override
            public MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription) {
                throw new IllegalStateException("It is illegal to register a field getter for this type");
            }

            @Override
            public MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription) {
                throw new IllegalStateException("It is illegal to register a field setter for this type");
            }

            @Override
            public String toString() {
                return "AuxiliaryType.MethodAccessorFactory.Illegal." + name();
            }
        }
    }

    /**
     * Representation of a naming strategy for an auxiliary type.
     */
    interface NamingStrategy {

        /**
         * Names an auxiliary type.
         *
         * @param instrumentedType The instrumented type for which an auxiliary type is registered.
         * @return The fully qualified name for the given auxiliary type.
         */
        String name(TypeDescription instrumentedType);

        /**
         * A naming strategy for an auxiliary type which returns the instrumented type's name with a fixed extension
         * and a random number as a suffix. All generated names will be in the same package as the instrumented type.
         */
        class SuffixingRandom implements NamingStrategy {

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
            public String name(TypeDescription instrumentedType) {
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
     * A marker to indicate that an auxiliary type is part of the instrumented types signature. This information can be used to load a type before
     * the instrumented type such that reflection on the instrumented type does not cause a {@link NoClassDefFoundError}.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface SignatureRelevant {
        /* empty */
    }
}

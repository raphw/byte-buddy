package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.modifier.SyntheticState;
import com.blogspot.mydailyjava.bytebuddy.modifier.Visibility;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.javaSignatureCompatibleTo;

/**
 * An auxiliary type that provides services to the instrumentation of another type. Implementations should provide
 * meaningful {@code equals(Object)} and {@code hashCode()} implementations in order to avoid multiple creations
 * of this type.
 */
public interface AuxiliaryType {

    /**
     * The default type access of an auxiliary type.
     */
    static final List<ModifierContributor.ForType> DEFAULT_TYPE_MODIFIER = Collections.unmodifiableList(
            Arrays.<ModifierContributor.ForType>asList(Visibility.PACKAGE_PRIVATE, SyntheticState.SYNTHETIC));


    /**
     * A factory for creating method proxies for an auxiliary type. Such proxies are required to allow a type to
     * call methods of a second type that are usually not accessible for the first type. This strategy is also adapted
     * by the Java compiler that creates accessor methods for example to implement inner classes.
     */
    static interface MethodAccessorFactory {

        static interface BridgeMethodResolver {

            static class Default implements BridgeMethodResolver {

                private final MethodList availableMethods;

                public Default(MethodList availableMethods) {
                    this.availableMethods = availableMethods;
                }

                @Override
                public MethodDescription resolveCallTo(MethodDescription methodDescription) {
                    MethodList relevant = availableMethods.filter(javaSignatureCompatibleTo(methodDescription));
                    for (MethodDescription alternative : relevant) {
                        if (methodDescription.getReturnType().isAssignableFrom(alternative.getReturnType())) {
                            methodDescription = alternative;
                        }
                    }
                    return methodDescription;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && availableMethods.equals(((Default) other).availableMethods);
                }

                @Override
                public int hashCode() {
                    return availableMethods.hashCode();
                }

                @Override
                public String toString() {
                    return "BridgeMethodResolver.Default{availableMethods=" + availableMethods + '}';
                }
            }

            MethodDescription resolveCallTo(MethodDescription methodDescription);
        }

        /**
         * Requests a new accessor method for the requested method. If such a method cannot be created, an exception
         * will be thrown.
         *
         * @param targetMethod The target method for which an accessor method is required.
         * @return A new accessor method.
         */
        MethodDescription requireAccessorMethodFor(MethodDescription targetMethod);
    }

    /**
     * Creates a new auxiliary type.
     *
     * @param auxiliaryTypeName     The fully qualified non-internal name for this auxiliary type. The type should be in
     *                              the same package than the instrumented type this auxiliary type is providing services
     *                              to in order to allow package-private access.
     * @param classFormatVersion    The class format version the auxiliary class should be written in.
     * @param methodAccessorFactory A factory for accessor methods.
     * @return A dynamically created type representing this auxiliary type.
     */
    DynamicType make(String auxiliaryTypeName,
                     ClassFormatVersion classFormatVersion,
                     MethodAccessorFactory methodAccessorFactory);
}

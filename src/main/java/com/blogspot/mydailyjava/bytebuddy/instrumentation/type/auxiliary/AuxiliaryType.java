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

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

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

        /**
         * Implementations of this interface serve as resolvers for bridge methods. For the Java compiler, a method
         * signature does not include any information on a method's return type. However, within Java byte code, a
         * distinction is made such that the Java compiler needs to include bridge methods where the method with the more
         * specific return type is called from the method with the less specific return type when a method is
         * overridden to return a more specific value. This resolution is important when auxiliary types are called since
         * an accessor required for {@code Foo#qux} on some type {@code Bar} with an overriden method {@code qux} that was
         * defined with a more specific return type would call the bridge method internally and not the intended method.
         * This can be problematic if the following chain is the result of an instrumentation:
         * <ol>
         * <li>A {@code super} method accessor is registered for {@code Foo#qux} for some auxiliary type {@code Baz}.</li>
         * <li>The accessor is a bridging method which calls {@code Bar#qux} with the more specific return type.</li>
         * <li>The method {@code Bar#qux} is intercepted by an instrumentation.</li>
         * <li>Within the instrumented implementation, the auxiliary type {@code Baz} is used to invoke {@code Foo#qux}.</li>
         * <li>The {@code super} method invocation hits the bridge which delegates to the intercepted implementation what
         * results in endless recursion.</li>
         * </ol>
         */
        static interface BridgeMethodResolver {

            /**
             * A default implementation of a bridge method resolver.
             */
            static class Default implements BridgeMethodResolver {

                private final MethodList availableMethods;

                /**
                 * Creates a new default bridge method resolver.
                 *
                 * @param availableMethods The method which should be considered as bridge methods.
                 */
                public Default(MethodList availableMethods) {
                    this.availableMethods = availableMethods.filter(not(isConstructor()));
                }

                @Override
                public MethodDescription resolveCallTo(MethodDescription methodDescription) {
                    MethodList relevant = availableMethods.filter(hasSameJavaCompilerSignatureAs(methodDescription));
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

            /**
             * Resolves a call to a given method that might be a bridge method. Methods that are passed to
             * this method will in general not be checked for their type ownership.
             *
             * @param methodDescription The method which is intended to be called.
             * @return The unbridge method for the given method description which in the most trivial case
             * is simply the method itself.
             */
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

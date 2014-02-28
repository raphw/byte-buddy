package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import org.objectweb.asm.Opcodes;

/**
 * An auxiliary type that provides services to the instrumentation of another type. Implementations should provide
 * meaningful {@code equals(Object)} and {@code hashCode()} implementations in order to avoid multiple creations
 * of this type.
 */
public interface AuxiliaryType {

    /**
     * The default type access of an auxiliary type.
     */
    static final int DEFAULT_TYPE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;

    /**
     * A factory for creating method proxies for an auxiliary type. Such proxies are required to allow a type to
     * call methods of a second type that are usually not accessible for the first type. This strategy is also adapted
     * by the Java compiler that creates accessor methods for example to implement inner classes.
     */
    static interface MethodProxyFactory {

        /**
         * Requests a new accessor method for the requested method. If such a method cannot be created, an exception
         * will be thrown.
         *
         * @param targetMethod The target method for which an accessor method is required.
         * @return A new accessor method.
         */
        MethodDescription requireProxyMethodFor(MethodDescription targetMethod);
    }

    /**
     * Creates a new auxiliary type.
     *
     * @param auxiliaryTypeName  The fully qualified internalName for this auxiliary type. The type should be in the same
     *                           package than the instrumented type this auxiliary type is providing services to.
     * @param methodProxyFactory A factory for accessor methods.
     * @return A dynamically created type representing this auxiliary type.
     */
    DynamicType<?> make(String auxiliaryTypeName, MethodProxyFactory methodProxyFactory);
}

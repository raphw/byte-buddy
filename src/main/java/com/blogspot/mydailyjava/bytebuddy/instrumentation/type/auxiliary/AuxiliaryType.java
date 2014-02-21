package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import org.objectweb.asm.Opcodes;

public interface AuxiliaryType {

    static final int DEFAULT_TYPE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;

    static interface MethodProxyFactory {

        MethodDescription requireProxyMethodFor(MethodDescription targetMethod);
    }

    DynamicType<?> make(String auxiliaryTypeName, MethodProxyFactory methodProxyFactory);
}

package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import org.objectweb.asm.Opcodes;

public interface AuxiliaryType {

    static final int DEFAULT_TYPE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;

    DynamicType<?> make(String proxyTypeName, ClassVersion classVersion);
}

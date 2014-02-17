package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.proxy.DynamicType;
import org.objectweb.asm.Opcodes;

public interface AuxiliaryType {

    static final int DEFAULT_TYPE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;

    static interface Named {

        DynamicType<?> make();
    }

    Named name(String proxyTypeName, ClassVersion classVersion);
}

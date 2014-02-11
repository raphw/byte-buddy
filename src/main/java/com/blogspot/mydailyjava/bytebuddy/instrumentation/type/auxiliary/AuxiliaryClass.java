package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import org.objectweb.asm.Opcodes;

public interface AuxiliaryClass {

    static final int DEFAULT_TYPE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;

    static interface Named extends Assignment {

        String getProxyTypeInternalName();

        byte[] make();
    }

    Named name(String proxyTypeName, ClassVersion classVersion);
}

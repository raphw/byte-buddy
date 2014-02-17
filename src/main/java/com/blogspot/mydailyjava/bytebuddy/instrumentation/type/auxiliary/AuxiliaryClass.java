package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import org.objectweb.asm.Opcodes;

public interface AuxiliaryClass {

    static final int DEFAULT_TYPE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;

    static interface Named extends StackManipulation {

        String getProxyTypeInternalName();

        byte[] make();
    }

    Named name(String proxyTypeName, ClassVersion classVersion);
}

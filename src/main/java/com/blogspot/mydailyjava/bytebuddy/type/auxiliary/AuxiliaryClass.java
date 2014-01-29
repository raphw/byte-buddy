package com.blogspot.mydailyjava.bytebuddy.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;

public interface AuxiliaryClass {

    static interface Named extends Assignment {

        String getProxyTypeInternalName();

        byte[] make();
    }

    Named name(String proxyTypeName, ClassVersion classVersion);
}

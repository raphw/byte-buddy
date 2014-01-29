package com.blogspot.mydailyjava.bytebuddy.method;

public interface ByteCodeMethod {

    String getInternalName();

    String getDescriptor();

    String[] getExceptionTypesInternalNames();

    String getUniqueSignature();

    String getDeclaringClassInternalName();

    String getDeclaringClassTypeDescriptor();

    String getDeclaringSuperClassInternalName();

    String getDeclaringSuperClassTypeDescriptor();
}

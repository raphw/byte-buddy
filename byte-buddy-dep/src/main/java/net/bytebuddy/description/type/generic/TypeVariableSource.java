package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.ByteCodeElement;

public interface TypeVariableSource extends ByteCodeElement {

    GenericTypeList getTypeVariables();

    TypeVariableSource getEnclosingSource();

    // TODO: TypeVariableSource find(String symbol);
}

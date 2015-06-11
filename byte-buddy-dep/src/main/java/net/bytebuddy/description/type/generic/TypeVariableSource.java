package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

public interface TypeVariableSource extends ByteCodeElement {

    GenericTypeList getTypeVariables();

    TypeVariableSource getEnclosingSource();

    GenericTypeDescription findVariable(String symbol);

    <T> T accept(Visitor<T> visitor);

    interface Visitor<T> {

        T onType(TypeDescription typeDescription);

        T onMethod(MethodDescription methodDescription);

        enum NoOp implements Visitor<TypeVariableSource> {

            INSTANCE;

            @Override
            public TypeVariableSource onType(TypeDescription typeDescription) {
                return typeDescription;
            }

            @Override
            public TypeVariableSource onMethod(MethodDescription methodDescription) {
                return methodDescription;
            }
        }
    }
}

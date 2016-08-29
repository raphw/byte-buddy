package net.bytebuddy.test;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;

public class SimpleEntryPoint implements EntryPoint {

    @Override
    public ByteBuddy getByteBuddy() {
        return new ByteBuddy();
    }

    @Override
    public DynamicType.Builder<?> transform(TypeDescription typeDescription,
                                            ByteBuddy byteBuddy,
                                            ClassFileLocator classFileLocator,
                                            MethodNameTransformer methodNameTransformer) {
        return byteBuddy.rebase(typeDescription, classFileLocator, methodNameTransformer);
    }
}

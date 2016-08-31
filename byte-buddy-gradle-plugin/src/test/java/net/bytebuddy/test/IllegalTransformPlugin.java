package net.bytebuddy.test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

public class IllegalTransformPlugin implements Plugin {

    public IllegalTransformPlugin() {
        throw new RuntimeException();
    }

    @Override
    public boolean matches(TypeDescription target) {
        throw new AssertionError();
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
        throw new AssertionError();
    }
}

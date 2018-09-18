package net.bytebuddy.test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

import java.io.IOException;

public class IllegalTransformPlugin implements Plugin {

    public IllegalTransformPlugin() {
        throw new RuntimeException();
    }

    public boolean matches(TypeDescription target) {
        throw new AssertionError();
    }

    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        throw new AssertionError();
    }

    public void close() {
        /* do nothing */
    }
}

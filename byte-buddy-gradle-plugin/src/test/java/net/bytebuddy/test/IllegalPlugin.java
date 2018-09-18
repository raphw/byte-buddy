package net.bytebuddy.test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

public class IllegalPlugin implements Plugin {

    public boolean matches(TypeDescription target) {
        throw new RuntimeException();
    }

    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        throw new RuntimeException();
    }

    public void close() {
        /* do nothing */
    }
}

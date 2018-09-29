package net.bytebuddy.test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class ArgumentPlugin implements Plugin {

    private final int value;

    public ArgumentPlugin(int value) {
        this.value = value;
    }

    public boolean matches(TypeDescription target) {
        return target.getName().equals("foo.Bar");
    }

    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.method(named("foo")).intercept(FixedValue.value(String.valueOf(value)));
    }

    public void close() {
        /* do nothing */
    }
}

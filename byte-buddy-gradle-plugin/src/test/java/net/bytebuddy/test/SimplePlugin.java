package net.bytebuddy.test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class SimplePlugin implements Plugin {

    public boolean matches(TypeDescription target) {
        return target.getName().equals("foo.Bar");
    }

    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.method(named("foo")).intercept(FixedValue.value("qux"));
    }

    public void close() {
        /* do nothing */
    }
}

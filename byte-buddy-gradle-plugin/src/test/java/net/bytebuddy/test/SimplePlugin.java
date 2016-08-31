package net.bytebuddy.test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class SimplePlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return target.getName().equals("foo.Bar");
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
        return builder.method(named("foo")).intercept(FixedValue.value("qux"));
    }
}

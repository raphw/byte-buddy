package com.transformations;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;

public class BasicLibTransformation implements Plugin {

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                        TypeDescription typeDescription,
                                        ClassFileLocator classFileLocator) {
        return builder.visit(Advice.to(BasicAdvice.class)
                .on(ElementMatchers.named("getMessage"))
        );
    }

    @Override
    public boolean matches(TypeDescription target) {
        return target.getName().contains("Some");
    }

    @Override
    public void close() throws IOException {

    }
}

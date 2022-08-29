package net.bytebuddy.plugin.test.aar;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;

public class AarPlugin implements Plugin {

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.visit(
                Advice.to(AarAdvice.class).on(ElementMatchers.named("someMethod"))
        );
    }

    @Override
    public void close() throws IOException {
        //NoOp
    }

    @Override
    public boolean matches(TypeDescription typeDefinitions) {
        return typeDefinitions.getTypeName().contains("Another");
    }
}
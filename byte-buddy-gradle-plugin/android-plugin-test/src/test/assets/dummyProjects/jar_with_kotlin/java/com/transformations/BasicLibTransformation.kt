package com.transformations;

import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.description.method.MethodDescription

class BasicLibTransformation : Plugin {

    override fun matches(target: TypeDescription): Boolean {
        return target.getName().contains("Some")
    }

    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator
    ): DynamicType.Builder<*> {
        return builder.visit(
            Advice.to(BasicAdvice::class.java)
                .on(ElementMatchers.named("getMessage"))
        )
    }

    override fun close() {
        // Nothing to close
    }
}
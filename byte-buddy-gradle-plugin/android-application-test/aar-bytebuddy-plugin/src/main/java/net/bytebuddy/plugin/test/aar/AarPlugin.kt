package net.bytebuddy.plugin.test.aar

import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers

class AarPlugin : Plugin {

    override fun matches(description: TypeDescription): Boolean {
        return description.typeName.contains("Another")
    }

    override fun close() {
        //NoOp
    }

    override fun apply(
        builder: DynamicType.Builder<*>,
        type: TypeDescription,
        locator: ClassFileLocator
    ): DynamicType.Builder<*> {
        return builder.visit(
            Advice.to(AarAdvice::class.java).on(ElementMatchers.named("someMethod"))
        )
    }
}
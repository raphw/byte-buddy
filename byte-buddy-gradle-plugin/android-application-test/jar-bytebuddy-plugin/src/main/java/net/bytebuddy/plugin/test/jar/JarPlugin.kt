package net.bytebuddy.plugin.test.jar

import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers

class JarPlugin : Plugin {

    override fun matches(description: TypeDescription): Boolean {
        return description.simpleName.contains("Some")
    }

    override fun close() {
        //NoOp
    }

    override fun apply(
        builder: DynamicType.Builder<*>,
        type: TypeDescription,
        locator: ClassFileLocator
    ): DynamicType.Builder<*> {
        println("Adding advice to: ${type.typeName}")//todo delete
        return builder.visit(
            Advice.to(JarAdvice::class.java).on(ElementMatchers.named("someMethod"))
        )
    }
}
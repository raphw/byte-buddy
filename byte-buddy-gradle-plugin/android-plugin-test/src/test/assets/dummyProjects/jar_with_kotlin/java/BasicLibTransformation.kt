import net.bytebuddy.build.Plugin
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.description.method.MethodDescription

class BasicLibTransformation : Plugin {

    override fun matches(target: TypeDescription?): Boolean {
        return target?.getName()?.contains("SomeClass") == true || target?.getName()
            ?.contains("SomeKotlinClass") == true
    }

    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator
    ): DynamicType.Builder<*> {
//        val assignableTo: Boolean = typeDescription.isAssignableTo(String::class.java)
//        println("Is $typeDescription assignable2: $assignableTo")
        return builder.method(
            ElementMatchers.named<MethodDescription>(
                "getMessage"
            )
        ).intercept(FixedValue.value("Instrumented message in lib"))
    }

    override fun close() {
        // Nothing to close
    }
}
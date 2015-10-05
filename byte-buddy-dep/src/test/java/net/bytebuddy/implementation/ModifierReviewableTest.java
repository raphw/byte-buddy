package net.bytebuddy.implementation;

import net.bytebuddy.description.ModifierReviewable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ModifierReviewableTest {

    private final SimpleModifierReviewable simpleModifierReviewable;

    private final Method method;

    public ModifierReviewableTest(int modifiers, String methodName) throws Exception {
        simpleModifierReviewable = new SimpleModifierReviewable(modifiers);
        method = ModifierReviewable.class.getDeclaredMethod(methodName);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Opcodes.ACC_ABSTRACT, "isAbstract"},
                {Opcodes.ACC_ANNOTATION, "isAnnotation"},
                {Opcodes.ACC_BRIDGE, "isBridge"},
                {Opcodes.ACC_DEPRECATED, "isDeprecated"},
                {Opcodes.ACC_ENUM, "isEnum"},
                {Opcodes.ACC_FINAL, "isFinal"},
                {Opcodes.ACC_INTERFACE, "isInterface"},
                {Opcodes.ACC_MANDATED, "isMandated"},
                {Opcodes.ACC_NATIVE, "isNative"},
                {Opcodes.ACC_PRIVATE, "isPrivate"},
                {Opcodes.ACC_PROTECTED, "isProtected"},
                {Opcodes.ACC_PUBLIC, "isPublic"},
                {Opcodes.ACC_STATIC, "isStatic"},
                {Opcodes.ACC_STRICT, "isStrict"},
                {Opcodes.ACC_SUPER, "isSuper"},
                {Opcodes.ACC_SYNCHRONIZED, "isSynchronized"},
                {Opcodes.ACC_SYNTHETIC, "isSynthetic"},
                {Opcodes.ACC_TRANSIENT, "isTransient"},
                {Opcodes.ACC_VARARGS, "isVarArgs"},
                {Opcodes.ACC_VOLATILE, "isVolatile"},
                {0, "isClassType"},
                {0, "isPackagePrivate"}
        });
    }

    @Test
    public void testModifierProperty() throws Exception {
        assertThat((Boolean) method.invoke(simpleModifierReviewable), is(true));
    }

    private static class SimpleModifierReviewable extends ModifierReviewable.AbstractBase {

        private final int modifiers;

        private SimpleModifierReviewable(int modifiers) {
            this.modifiers = modifiers;
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }
    }
}

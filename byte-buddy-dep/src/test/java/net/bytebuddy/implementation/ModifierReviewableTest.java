package net.bytebuddy.implementation;

import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.modifier.*;
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

    private final Object expected;

    public ModifierReviewableTest(int modifiers, String methodName, Object expected) throws Exception {
        simpleModifierReviewable = new SimpleModifierReviewable(modifiers);
        method = ModifierReviewable.AbstractBase.class.getMethod(methodName);
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Opcodes.ACC_ABSTRACT, "isAbstract", true},
                {Opcodes.ACC_ANNOTATION, "isAnnotation", true},
                {Opcodes.ACC_BRIDGE, "isBridge", true},
                {Opcodes.ACC_DEPRECATED, "isDeprecated", true},
                {Opcodes.ACC_ENUM, "isEnum", true},
                {Opcodes.ACC_FINAL, "isFinal", true},
                {Opcodes.ACC_INTERFACE, "isInterface", true},
                {Opcodes.ACC_MANDATED, "isMandated", true},
                {Opcodes.ACC_NATIVE, "isNative", true},
                {Opcodes.ACC_PRIVATE, "isPrivate", true},
                {Opcodes.ACC_PROTECTED, "isProtected", true},
                {Opcodes.ACC_PUBLIC, "isPublic", true},
                {Opcodes.ACC_STATIC, "isStatic", true},
                {Opcodes.ACC_STRICT, "isStrict", true},
                {Opcodes.ACC_SYNCHRONIZED, "isSynchronized", true},
                {Opcodes.ACC_SYNTHETIC, "isSynthetic", true},
                {Opcodes.ACC_TRANSIENT, "isTransient", true},
                {Opcodes.ACC_VARARGS, "isVarArgs", true},
                {Opcodes.ACC_VOLATILE, "isVolatile", true},
                {ModifierReviewable.EMPTY_MASK, "isPackagePrivate", true},
                {Opcodes.ACC_SYNTHETIC, "getSyntheticState", SyntheticState.SYNTHETIC},
                {ModifierReviewable.EMPTY_MASK, "getSyntheticState", SyntheticState.PLAIN},
                {Opcodes.ACC_PUBLIC, "getVisibility", Visibility.PUBLIC},
                {ModifierReviewable.EMPTY_MASK, "getVisibility", Visibility.PACKAGE_PRIVATE},
                {Opcodes.ACC_PROTECTED, "getVisibility", Visibility.PROTECTED},
                {Opcodes.ACC_PRIVATE, "getVisibility", Visibility.PRIVATE},
                {Opcodes.ACC_STATIC, "getOwnership", Ownership.STATIC},
                {ModifierReviewable.EMPTY_MASK, "getOwnership", Ownership.MEMBER},
                {Opcodes.ACC_ENUM, "getEnumerationState", EnumerationState.ENUMERATION},
                {ModifierReviewable.EMPTY_MASK, "getEnumerationState", EnumerationState.PLAIN},
                {Opcodes.ACC_ABSTRACT, "getTypeManifestation", TypeManifestation.ABSTRACT},
                {Opcodes.ACC_FINAL, "getTypeManifestation", TypeManifestation.FINAL},
                {Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE, "getTypeManifestation", TypeManifestation.INTERFACE},
                {Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION, "getTypeManifestation", TypeManifestation.ANNOTATION},
                {ModifierReviewable.EMPTY_MASK, "getTypeManifestation", TypeManifestation.PLAIN},
                {Opcodes.ACC_FINAL, "getFieldManifestation", FieldManifestation.FINAL},
                {Opcodes.ACC_VOLATILE, "getFieldManifestation", FieldManifestation.VOLATILE},
                {Opcodes.ACC_TRANSIENT, "getFieldPersistence", FieldPersistence.TRANSIENT},
                {ModifierReviewable.EMPTY_MASK, "getFieldPersistence", FieldPersistence.PLAIN},
                {Opcodes.ACC_SYNCHRONIZED, "getSynchronizationState", SynchronizationState.SYNCHRONIZED},
                {ModifierReviewable.EMPTY_MASK, "getSynchronizationState", SynchronizationState.PLAIN},
                {Opcodes.ACC_FINAL, "getParameterManifestation", ParameterManifestation.FINAL},
                {ModifierReviewable.EMPTY_MASK, "getParameterManifestation", ParameterManifestation.PLAIN},
                {Opcodes.ACC_MANDATED, "getProvisioningState", ProvisioningState.MANDATED},
                {ModifierReviewable.EMPTY_MASK, "getProvisioningState", ProvisioningState.PLAIN},
                {Opcodes.ACC_BRIDGE, "getMethodManifestation", MethodManifestation.BRIDGE},
                {Opcodes.ACC_ABSTRACT, "getMethodManifestation", MethodManifestation.ABSTRACT},
                {Opcodes.ACC_FINAL, "getMethodManifestation", MethodManifestation.FINAL},
                {Opcodes.ACC_NATIVE, "getMethodManifestation", MethodManifestation.NATIVE},
                {Opcodes.ACC_NATIVE | Opcodes.ACC_FINAL, "getMethodManifestation", MethodManifestation.FINAL_NATIVE},
                {Opcodes.ACC_BRIDGE | Opcodes.ACC_FINAL, "getMethodManifestation", MethodManifestation.FINAL_BRIDGE},
                {ModifierReviewable.EMPTY_MASK, "getMethodManifestation", MethodManifestation.PLAIN},
                {Opcodes.ACC_STRICT, "getMethodStrictness", MethodStrictness.STRICT},
                {ModifierReviewable.EMPTY_MASK, "getMethodStrictness", MethodStrictness.PLAIN}
        });
    }

    @Test
    public void testModifierProperty() throws Exception {
        assertThat(method.invoke(simpleModifierReviewable), is(expected));
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

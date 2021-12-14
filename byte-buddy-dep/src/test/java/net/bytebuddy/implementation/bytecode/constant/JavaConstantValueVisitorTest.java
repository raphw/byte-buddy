package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class JavaConstantValueVisitorTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return Arrays.asList(new Object[][]{
                {JavaConstant.Simple.ofLoaded(FOO), FOO},
                {JavaConstant.Simple.ofLoaded(Object.class), Type.getType(Object.class)},
                {JavaConstant.MethodType.ofConstant(Object.class), Type.getMethodType(Type.getType(Object.class))},
                {JavaConstant.MethodHandle.of(Object.class.getMethod("toString")), new Handle(Opcodes.H_INVOKEVIRTUAL,
                        Type.getInternalName(Object.class),
                        "toString",
                        Type.getMethodDescriptor(Object.class.getMethod("toString")),
                        false)},
                {JavaConstant.Dynamic.ofNullConstant(), new ConstantDynamic(JavaConstant.Dynamic.DEFAULT_NAME,
                        Type.getDescriptor(Object.class),
                        new Handle(Opcodes.H_INVOKESTATIC,
                                JavaType.CONSTANT_BOOTSTRAPS.getTypeStub().getInternalName(),
                                "nullConstant",
                                Type.getMethodDescriptor(Type.getType(Object.class),
                                        Type.getType(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().getDescriptor()),
                                        Type.getType(String.class),
                                        Type.getType(Class.class)),
                                false))}
        });
    }

    private final JavaConstant constant;

    private final Object value;

    public JavaConstantValueVisitorTest(JavaConstant constant, Object value) {
        this.constant = constant;
        this.value = value;
    }

    @Test
    public void testVisit() throws Exception {
        assertThat(constant.accept(JavaConstantValue.Visitor.INSTANCE), is(value));
    }
}

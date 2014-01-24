package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class PrimitiveWideningDelegateIllegalTest {

    @Parameterized.Parameters
    public static Collection<Object[]> illegalPrimitiveAssignments() {
        return Arrays.asList(new Object[][]{
                {boolean.class, byte.class},
                {boolean.class, short.class},
                {boolean.class, char.class},
                {boolean.class, int.class},
                {boolean.class, long.class},
                {boolean.class, float.class},
                {boolean.class, double.class},
                {byte.class, boolean.class},
                {byte.class, char.class},
                {short.class, boolean.class},
                {short.class, byte.class},
                {short.class, char.class},
                {char.class, boolean.class},
                {char.class, byte.class},
                {char.class, short.class},
                {int.class, boolean.class},
                {int.class, byte.class},
                {int.class, short.class},
                {long.class, boolean.class},
                {long.class, byte.class},
                {long.class, short.class},
                {long.class, char.class},
                {long.class, int.class},
                {float.class, boolean.class},
                {float.class, byte.class},
                {float.class, short.class},
                {float.class, char.class},
                {float.class, int.class},
                {float.class, long.class},
                {double.class, boolean.class},
                {double.class, byte.class},
                {double.class, short.class},
                {double.class, char.class},
                {double.class, int.class},
                {double.class, long.class},
                {double.class, float.class},
        });
    }

    private final Class<?> source;
    private final Class<?> target;

    public PrimitiveWideningDelegateIllegalTest(Class<?> source, Class<?> target) {
        this.source = source;
        this.target = target;
    }

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBoolean() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(source).widenTo(target);
        assertThat(assignment.isValid(), is(false));
        assignment.apply(methodVisitor);
    }
}

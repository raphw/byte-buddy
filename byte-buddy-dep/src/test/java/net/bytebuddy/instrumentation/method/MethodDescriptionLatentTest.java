package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class MethodDescriptionLatentTest {

    private static final String FOO = "foo", BAR = "bar", BARS = "bars", QUX = "qux", BAZ = "baz";
    private static final String DESCRIPTOR = "(" + BAZ + ")" + QUX;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription declaringType, returnType, parameterType;

    private MethodDescription latentMethod, latentConstructor;

    @Before
    public void setUp() throws Exception {
        when(declaringType.getName()).thenReturn(BAR);
        when(declaringType.getInternalName()).thenReturn(BARS);
        when(returnType.getDescriptor()).thenReturn(QUX);
        when(parameterType.getDescriptor()).thenReturn(BAZ);
        when(parameterType.getStackSize()).thenReturn(StackSize.DOUBLE);
        latentMethod = new MethodDescription.Latent(FOO,
                declaringType,
                returnType,
                Arrays.asList(parameterType),
                Opcodes.ACC_PUBLIC);
        latentConstructor = new MethodDescription.Latent(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                declaringType,
                returnType,
                Arrays.asList(parameterType),
                Opcodes.ACC_PUBLIC);
    }

    @Test
    public void testGetInternalName() throws Exception {
        assertThat(latentMethod.getInternalName(), is(FOO));
        assertThat(latentConstructor.getInternalName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
    }

    @Test
    public void testGetName() throws Exception {
        assertThat(latentMethod.getName(), is(FOO));
        assertThat(latentConstructor.getName(), is(BAR));
    }

    @Test
    public void testGetDescriptor() throws Exception {
        assertThat(latentMethod.getDescriptor(), is(DESCRIPTOR));
        assertThat(latentConstructor.getDescriptor(), is(DESCRIPTOR));
    }

    @Test
    public void testIsOverridable() throws Exception {
        assertThat(latentMethod.isOverridable(), is(true));
        assertThat(latentConstructor.isOverridable(), is(false));
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(latentMethod.getStackSize(), is(3));
        assertThat(latentConstructor.getStackSize(), is(3));
    }

    @Test
    public void testGetParameterOffset() throws Exception {
        assertThat(latentMethod.getParameterOffset(0), is(1));
        assertThat(latentConstructor.getParameterOffset(0), is(1));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(latentMethod.hashCode(), is((BARS + "." + FOO + DESCRIPTOR).hashCode()));
        assertThat(latentConstructor.hashCode(), is((BARS + "." + MethodDescription.CONSTRUCTOR_INTERNAL_NAME + DESCRIPTOR).hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        assertThat(latentMethod.equals(latentConstructor), is(false));
        assertThat(latentMethod.equals(latentMethod), is(true));
        assertThat(latentConstructor.equals(latentConstructor), is(true));
    }
}

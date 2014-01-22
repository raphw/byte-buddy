package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class PrimitiveTypeAwareAssignerTest {

    private Assigner chainedAssigner;
    private Assigner primitiveAssigner;

    @Before
    public void setUp() throws Exception {
        chainedAssigner = mock(Assigner.class);
        when(chainedAssigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        primitiveAssigner = new PrimitiveTypeAwareAssigner(chainedAssigner);
    }

    @Test
    public void testPrimitiveToPrimitive() throws Exception {
        assertThat(primitiveAssigner.assign(boolean.class, boolean.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(boolean.class, byte.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(boolean.class, short.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(boolean.class, char.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(boolean.class, int.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(boolean.class, long.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(boolean.class, float.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(boolean.class, double.class, false).isAssignable(), is(false));

        assertThat(primitiveAssigner.assign(byte.class, boolean.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(byte.class, byte.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(byte.class, short.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(byte.class, char.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(byte.class, int.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(byte.class, long.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(byte.class, float.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(byte.class, double.class, false).isAssignable(), is(true));

        assertThat(primitiveAssigner.assign(short.class, boolean.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(short.class, byte.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(short.class, short.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(short.class, char.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(short.class, int.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(short.class, long.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(short.class, float.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(short.class, double.class, false).isAssignable(), is(true));

        assertThat(primitiveAssigner.assign(char.class, boolean.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(char.class, byte.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(char.class, short.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(char.class, char.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(char.class, int.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(char.class, long.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(char.class, float.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(char.class, double.class, false).isAssignable(), is(true));

        assertThat(primitiveAssigner.assign(int.class, boolean.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(int.class, byte.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(int.class, short.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(int.class, char.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(int.class, int.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(int.class, long.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(int.class, float.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(int.class, double.class, false).isAssignable(), is(true));

        assertThat(primitiveAssigner.assign(long.class, boolean.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(long.class, byte.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(long.class, short.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(long.class, char.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(long.class, int.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(long.class, long.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(long.class, float.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(long.class, double.class, false).isAssignable(), is(true));

        assertThat(primitiveAssigner.assign(float.class, boolean.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(float.class, byte.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(float.class, short.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(float.class, char.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(float.class, int.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(float.class, long.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(float.class, float.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(float.class, double.class, false).isAssignable(), is(true));

        assertThat(primitiveAssigner.assign(double.class, boolean.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(double.class, byte.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(double.class, short.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(double.class, char.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(double.class, int.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(double.class, long.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(double.class, float.class, false).isAssignable(), is(false));
        assertThat(primitiveAssigner.assign(double.class, double.class, false).isAssignable(), is(true));

        verifyZeroInteractions(chainedAssigner);
    }

    @Test
    public void testBoxing() throws Exception {
        assertThat(primitiveAssigner.assign(boolean.class, Boolean.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Boolean.class, Boolean.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(byte.class, Byte.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Byte.class, Byte.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(short.class, Short.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Short.class, Short.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(char.class, Character.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Character.class, Character.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(int.class, Integer.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Integer.class, Integer.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(long.class, Long.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Long.class, Long.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(float.class, Float.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Float.class, Float.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(double.class, Double.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Double.class, Double.class, false);
        verifyNoMoreInteractions(chainedAssigner);
    }

    @Test
    public void testUnboxing() throws Exception {
        assertThat(primitiveAssigner.assign(Boolean.class, boolean.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(Byte.class, byte.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(Short.class, short.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(Character.class, char.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(Integer.class, int.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(Long.class, long.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(Float.class, float.class, false).isAssignable(), is(true));
        assertThat(primitiveAssigner.assign(Double.class, double.class, false).isAssignable(), is(true));

        verifyZeroInteractions(chainedAssigner);
    }

    @Test
    public void testImplicitUnboxing() throws Exception {
        assertThat(primitiveAssigner.assign(Object.class, boolean.class, true).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Boolean.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(Object.class, byte.class, true).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Byte.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(Object.class, short.class, true).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Short.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(Object.class, char.class, true).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Character.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(Object.class, int.class, true).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Integer.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(Object.class, long.class, true).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Long.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(Object.class, float.class, true).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Float.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertThat(primitiveAssigner.assign(Object.class, double.class, true).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Double.class, true);
        verifyNoMoreInteractions(chainedAssigner);
    }

    @Test
    public void testDelegation() throws Exception {
        assertThat(primitiveAssigner.assign(Object.class, Object.class, false).isAssignable(), is(true));
        verify(chainedAssigner).assign(Object.class, Object.class, false);
        verifyNoMoreInteractions(chainedAssigner);
    }
}

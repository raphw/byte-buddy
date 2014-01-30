package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.LegalTrivialAssignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.hamcrest.Matcher;
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
        when(chainedAssigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean()))
                .thenReturn(LegalTrivialAssignment.INSTANCE);
        primitiveAssigner = new PrimitiveTypeAwareAssigner(chainedAssigner);
    }

    @Test
    public void testPrimitiveToPrimitive() throws Exception {
        assertAssignment(boolean.class, boolean.class, false, is(true));
        assertAssignment(boolean.class, byte.class, false, is(false));
        assertAssignment(boolean.class, short.class, false, is(false));
        assertAssignment(boolean.class, char.class, false, is(false));
        assertAssignment(boolean.class, int.class, false, is(false));
        assertAssignment(boolean.class, long.class, false, is(false));
        assertAssignment(boolean.class, float.class, false, is(false));
        assertAssignment(boolean.class, double.class, false, is(false));

        assertAssignment(byte.class, boolean.class, false, is(false));
        assertAssignment(byte.class, byte.class, false, is(true));
        assertAssignment(byte.class, short.class, false, is(true));
        assertAssignment(byte.class, char.class, false, is(false));
        assertAssignment(byte.class, int.class, false, is(true));
        assertAssignment(byte.class, long.class, false, is(true));
        assertAssignment(byte.class, float.class, false, is(true));
        assertAssignment(byte.class, double.class, false, is(true));

        assertAssignment(short.class, boolean.class, false, is(false));
        assertAssignment(short.class, byte.class, false, is(false));
        assertAssignment(short.class, short.class, false, is(true));
        assertAssignment(short.class, char.class, false, is(false));
        assertAssignment(short.class, int.class, false, is(true));
        assertAssignment(short.class, long.class, false, is(true));
        assertAssignment(short.class, float.class, false, is(true));
        assertAssignment(short.class, double.class, false, is(true));

        assertAssignment(char.class, boolean.class, false, is(false));
        assertAssignment(char.class, byte.class, false, is(false));
        assertAssignment(char.class, short.class, false, is(false));
        assertAssignment(char.class, char.class, false, is(true));
        assertAssignment(char.class, int.class, false, is(true));
        assertAssignment(char.class, long.class, false, is(true));
        assertAssignment(char.class, float.class, false, is(true));
        assertAssignment(char.class, double.class, false, is(true));

        assertAssignment(int.class, boolean.class, false, is(false));
        assertAssignment(int.class, byte.class, false, is(false));
        assertAssignment(int.class, short.class, false, is(false));
        assertAssignment(int.class, char.class, false, is(false));
        assertAssignment(int.class, int.class, false, is(true));
        assertAssignment(int.class, long.class, false, is(true));
        assertAssignment(int.class, float.class, false, is(true));
        assertAssignment(int.class, double.class, false, is(true));

        assertAssignment(long.class, boolean.class, false, is(false));
        assertAssignment(long.class, byte.class, false, is(false));
        assertAssignment(long.class, short.class, false, is(false));
        assertAssignment(long.class, char.class, false, is(false));
        assertAssignment(long.class, int.class, false, is(false));
        assertAssignment(long.class, long.class, false, is(true));
        assertAssignment(long.class, float.class, false, is(true));
        assertAssignment(long.class, double.class, false, is(true));

        assertAssignment(float.class, boolean.class, false, is(false));
        assertAssignment(float.class, byte.class, false, is(false));
        assertAssignment(float.class, short.class, false, is(false));
        assertAssignment(float.class, char.class, false, is(false));
        assertAssignment(float.class, int.class, false, is(false));
        assertAssignment(float.class, long.class, false, is(false));
        assertAssignment(float.class, float.class, false, is(true));
        assertAssignment(float.class, double.class, false, is(true));

        assertAssignment(double.class, boolean.class, false, is(false));
        assertAssignment(double.class, byte.class, false, is(false));
        assertAssignment(double.class, short.class, false, is(false));
        assertAssignment(double.class, char.class, false, is(false));
        assertAssignment(double.class, int.class, false, is(false));
        assertAssignment(double.class, long.class, false, is(false));
        assertAssignment(double.class, float.class, false, is(false));
        assertAssignment(double.class, double.class, false, is(true));

        verifyZeroInteractions(chainedAssigner);
    }

    @Test
    public void testBoxing() throws Exception {
        assertAssignment(boolean.class, Boolean.class, false, is(true));
        verifyChainedAssignment(Boolean.class, Boolean.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(byte.class, Byte.class, false, is(true));
        verifyChainedAssignment(Byte.class, Byte.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(short.class, Short.class, false, is(true));
        verifyChainedAssignment(Short.class, Short.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(char.class, Character.class, false, is(true));
        verifyChainedAssignment(Character.class, Character.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(int.class, Integer.class, false, is(true));
        verifyChainedAssignment(Integer.class, Integer.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(long.class, Long.class, false, is(true));
        verifyChainedAssignment(Long.class, Long.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(float.class, Float.class, false, is(true));
        verifyChainedAssignment(Float.class, Float.class, false);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(double.class, Double.class, false, is(true));
        verifyChainedAssignment(Double.class, Double.class, false);
        verifyNoMoreInteractions(chainedAssigner);
    }

    @Test
    public void testUnboxing() throws Exception {
        assertAssignment(Boolean.class, boolean.class, false, is(true));
        assertAssignment(Byte.class, byte.class, false, is(true));
        assertAssignment(Short.class, short.class, false, is(true));
        assertAssignment(Character.class, char.class, false, is(true));
        assertAssignment(Integer.class, int.class, false, is(true));
        assertAssignment(Long.class, long.class, false, is(true));
        assertAssignment(Float.class, float.class, false, is(true));
        assertAssignment(Double.class, double.class, false, is(true));
        verifyZeroInteractions(chainedAssigner);
    }

    @Test
    public void testImplicitUnboxing() throws Exception {
        assertAssignment(Object.class, boolean.class, true, is(true));
        verifyChainedAssignment(Object.class, Boolean.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(Object.class, byte.class, true, is(true));
        verifyChainedAssignment(Object.class, Byte.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(Object.class, short.class, true, is(true));
        verifyChainedAssignment(Object.class, Short.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(Object.class, char.class, true, is(true));
        verifyChainedAssignment(Object.class, Character.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(Object.class, int.class, true, is(true));
        verifyChainedAssignment(Object.class, Integer.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(Object.class, long.class, true, is(true));
        verifyChainedAssignment(Object.class, Long.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(Object.class, float.class, true, is(true));
        verifyChainedAssignment(Object.class, Float.class, true);
        verifyNoMoreInteractions(chainedAssigner);

        assertAssignment(Object.class, double.class, true, is(true));
        verifyChainedAssignment(Object.class, Double.class, true);
        verifyNoMoreInteractions(chainedAssigner);
    }

    @Test
    public void testDelegation() throws Exception {
        assertAssignment(Object.class, Object.class, false, is(true));
        verifyChainedAssignment(Object.class, Object.class, false);
        verifyNoMoreInteractions(chainedAssigner);
    }

    private void assertAssignment(Class<?> sourceType,
                                  Class<?> targetType,
                                  boolean considerRuntimeType,
                                  Matcher<Boolean> matcher) {
        assertThat(primitiveAssigner.assign(new TypeDescription.ForLoadedType(sourceType),
                new TypeDescription.ForLoadedType(targetType),
                considerRuntimeType).isValid(),
                matcher);

    }

    private void verifyChainedAssignment(Class<?> sourceType,
                                         Class<?> targetType,
                                         boolean considerRuntimeType) {
        verify(chainedAssigner).assign(new TypeDescription.ForLoadedType(sourceType),
                new TypeDescription.ForLoadedType(targetType),
                considerRuntimeType);
    }
}

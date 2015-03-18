package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RuntimeTypeVerifierTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;
    @Mock
    private ParameterDescription parameterDescription;

    @Mock
    private RuntimeType runtimeType;

    @Before
    public void setUp() throws Exception {
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
    }

    @Test
    public void testCheckMethodValid() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(runtimeType));
        assertThat(RuntimeType.Verifier.check(methodDescription), is(true));
        verify(methodDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testCheckMethodInvalid() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation());
        assertThat(RuntimeType.Verifier.check(methodDescription), is(false));
        verify(methodDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testCheckMethodParameterValid() throws Exception {
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(runtimeType));
        assertThat(RuntimeType.Verifier.check(parameterDescription), is(true));
        verify(parameterDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(parameterDescription);
    }

    @Test
    public void testCheckMethodParameterInvalid() throws Exception {
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation());
        assertThat(RuntimeType.Verifier.check(parameterDescription), is(false));
        verify(parameterDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(parameterDescription);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInstantiation() throws Exception {
        Constructor<?> constructor = RuntimeType.Verifier.class.getDeclaredConstructor();
        assertThat(constructor.getModifiers(), is(Opcodes.ACC_PRIVATE));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException e) {
            throw (UnsupportedOperationException) e.getCause();
        }
    }
}

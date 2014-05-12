package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class RuntimeTypeVerifierTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;
    @Mock
    private RuntimeType runtimeType;

    @Before
    public void setUp() throws Exception {
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
    }

    @Test
    public void testCheckMethodValid() throws Exception {
        when(methodDescription.getAnnotations()).thenReturn(new Annotation[]{runtimeType});
        assertThat(RuntimeType.Verifier.check(methodDescription), is(true));
        verify(methodDescription).getAnnotations();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testCheckMethodInvalid() throws Exception {
        when(methodDescription.getAnnotations()).thenReturn(new Annotation[0]);
        assertThat(RuntimeType.Verifier.check(methodDescription), is(false));
        verify(methodDescription).getAnnotations();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testCheckMethodParameterValid() throws Exception {
        when(methodDescription.getParameterAnnotations()).thenReturn(new Annotation[][]{{runtimeType}});
        assertThat(RuntimeType.Verifier.check(methodDescription, 0), is(true));
        verify(methodDescription).getParameterAnnotations();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testCheckMethodParameterInvalid() throws Exception {
        when(methodDescription.getParameterAnnotations()).thenReturn(new Annotation[1][0]);
        assertThat(RuntimeType.Verifier.check(methodDescription, 0), is(false));
        verify(methodDescription).getParameterAnnotations();
        verifyNoMoreInteractions(methodDescription);
    }
}

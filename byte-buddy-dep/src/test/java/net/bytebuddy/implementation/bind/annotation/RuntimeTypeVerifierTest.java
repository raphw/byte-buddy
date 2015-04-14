package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotatedElement;
import net.bytebuddy.description.annotation.AnnotationList;
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
    private AnnotatedElement annotatedElement;

    @Mock
    private RuntimeType runtimeType;

    @Before
    public void setUp() throws Exception {
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
    }

    @Test
    public void testCheckElementValid() throws Exception {
        when(annotatedElement.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(runtimeType));
        assertThat(RuntimeType.Verifier.check(annotatedElement), is(true));
        verify(annotatedElement).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotatedElement);
    }

    @Test
    public void testCheckElementInvalid() throws Exception {
        when(annotatedElement.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation());
        assertThat(RuntimeType.Verifier.check(annotatedElement), is(false));
        verify(annotatedElement).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotatedElement);
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

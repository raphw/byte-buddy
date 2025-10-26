package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IgnoreForBindingVerifierTest extends AbstractAnnotationTest<IgnoreForBinding> {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private AnnotationList annotationList;

    public IgnoreForBindingVerifierTest() {
        super(IgnoreForBinding.class);
    }

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(annotationList);
    }

    @Test
    public void testIsPresent() throws Exception {
        when(annotationList.isAnnotationPresent(IgnoreForBinding.class)).thenReturn(true);
        assertThat(IgnoreForBinding.Verifier.check(methodDescription), is(true));
        verify(methodDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(methodDescription);
        verify(annotationList).isAnnotationPresent(IgnoreForBinding.class);
        verifyNoMoreInteractions(annotationList);
    }

    @Test
    public void testIsNotPresent() throws Exception {
        assertThat(IgnoreForBinding.Verifier.check(methodDescription), is(false));
        verify(methodDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(methodDescription);
        verify(annotationList).isAnnotationPresent(IgnoreForBinding.class);
        verifyNoMoreInteractions(annotationList);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInstantiation() throws Exception {
        Constructor<?> constructor = IgnoreForBinding.Verifier.class.getDeclaredConstructor();
        assertThat(constructor.getModifiers(), is(Opcodes.ACC_PRIVATE));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (UnsupportedOperationException) exception.getTargetException();
        }
    }
}

package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotatedCodeElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RuntimeTypeVerifierTest extends AbstractAnnotationTest<RuntimeType> {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AnnotatedCodeElement annotatedCodeElement;

    @Mock
    private RuntimeType runtimeType;

    public RuntimeTypeVerifierTest() {
        super(RuntimeType.class);
    }

    @Before
    public void setUp() throws Exception {
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
    }

    @Test
    public void testCheckElementValid() throws Exception {
        when(annotatedCodeElement.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(runtimeType));
        assertThat(RuntimeType.Verifier.check(annotatedCodeElement), is(Assigner.Typing.DYNAMIC));
        verify(annotatedCodeElement).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotatedCodeElement);
    }

    @Test
    public void testCheckElementInvalid() throws Exception {
        when(annotatedCodeElement.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations());
        assertThat(RuntimeType.Verifier.check(annotatedCodeElement), is(Assigner.Typing.STATIC));
        verify(annotatedCodeElement).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotatedCodeElement);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInstantiation() throws Exception {
        Constructor<?> constructor = RuntimeType.Verifier.class.getDeclaredConstructor();
        assertThat(constructor.getModifiers(), is(Opcodes.ACC_PRIVATE));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (UnsupportedOperationException) exception.getCause();
        }
    }
}

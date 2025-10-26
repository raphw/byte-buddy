package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RuntimeTypeVerifierTest extends AbstractAnnotationTest<RuntimeType> {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private AnnotationSource annotationSource;

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
        when(annotationSource.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(runtimeType));
        assertThat(RuntimeType.Verifier.check(annotationSource), is(Assigner.Typing.DYNAMIC));
        verify(annotationSource).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotationSource);
    }

    @Test
    public void testCheckElementInvalid() throws Exception {
        when(annotationSource.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations());
        assertThat(RuntimeType.Verifier.check(annotationSource), is(Assigner.Typing.STATIC));
        verify(annotationSource).getDeclaredAnnotations();
        verifyNoMoreInteractions(annotationSource);
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
            throw (UnsupportedOperationException) exception.getTargetException();
        }
    }
}

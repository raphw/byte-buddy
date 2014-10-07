package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class IgnoreForBindingVerifierTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    private AnnotationList annotationList;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(annotationList);
    }

    @Test
    public void testIsPresent() throws Exception {
        when(annotationList.isAnnotationPresent(IgnoreForBinding.class)).thenReturn(true);
        assertThat(IgnoreForBinding.Verifier.check(methodDescription), is(true));
        verify(annotationList).isAnnotationPresent(IgnoreForBinding.class);
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testIsNotPresent() throws Exception {
        assertThat(IgnoreForBinding.Verifier.check(methodDescription), is(false));
        verify(annotationList).isAnnotationPresent(IgnoreForBinding.class);
        verifyNoMoreInteractions(methodDescription);
    }
}

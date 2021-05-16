package net.bytebuddy.utility;

import org.junit.Test;

import java.lang.reflect.AnnotatedElement;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class JavaModuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void testExtractModule() throws Exception {
        JavaModule.of(mock(Object.class));
    }

    @Test
    public void testUnwrap() throws Exception {
        AnnotatedElement object = mock(AnnotatedElement.class);
        JavaModule module = new JavaModule(object);
        assertThat(module.unwrap(), sameInstance((Object) object));
    }
}
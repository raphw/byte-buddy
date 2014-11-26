package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ClassFileLocatorForClassLoaderTest {

    private static final String FOOBAR = "foo/bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoader classLoader;

    @Mock
    private TypeDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getInternalName()).thenReturn(FOOBAR);
    }

    @Test
    public void testLocatable() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(classLoader.getResourceAsStream(FOOBAR + ".class")).thenReturn(inputStream);
        TypeDescription.BinaryRepresentation binaryRepresentation = new ClassFileLocator.ForClassLoader(classLoader)
                .classFileFor(typeDescription);
        assertThat(binaryRepresentation.isValid(), is(true));
        assertThat(binaryRepresentation.getData(), is(new byte[]{1, 2, 3}));
        verify(classLoader).getResourceAsStream(FOOBAR + ".class");
        verifyNoMoreInteractions(classLoader);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonLocatable() throws Exception {
        TypeDescription.BinaryRepresentation binaryRepresentation = new ClassFileLocator.ForClassLoader(classLoader)
                .classFileFor(typeDescription);
        assertThat(binaryRepresentation.isValid(), is(false));
        verify(classLoader).getResourceAsStream(FOOBAR + ".class");
        verifyNoMoreInteractions(classLoader);
        binaryRepresentation.getData();
        fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBootstrapThrowsException() throws Exception {
        new ClassFileLocator.ForClassLoader(null);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.ForClassLoader.class).apply();
    }
}

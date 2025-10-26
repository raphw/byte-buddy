package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.io.ByteArrayInputStream;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ClassFileLocatorForModuleTest {

    private static final String FOOBAR = "foo/bar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private JavaModule module;

    @Mock
    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        when(module.isNamed()).thenReturn(true);
        when(module.getClassLoader()).thenReturn(classLoader);
    }

    @Test
    public void testCreationNamed() throws Exception {
        when(module.isNamed()).thenReturn(true);
        assertThat(ClassFileLocator.ForModule.of(module), hasPrototype((ClassFileLocator) new ClassFileLocator.ForModule(module)));
    }

    @Test
    public void testCreationUnnamed() throws Exception {
        when(module.isNamed()).thenReturn(false);
        assertThat(ClassFileLocator.ForModule.of(module), hasPrototype((ClassFileLocator) new ClassFileLocator.ForClassLoader(classLoader)));
    }

    @Test
    public void testLocatable() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(module.getResourceAsStream(FOOBAR + ClassFileLocator.CLASS_FILE_EXTENSION)).thenReturn(inputStream);
        ClassFileLocator.Resolution resolution = new ClassFileLocator.ForModule(module)
                .locate(FOOBAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(new byte[]{1, 2, 3}));
        verify(module).getResourceAsStream(FOOBAR + ClassFileLocator.CLASS_FILE_EXTENSION);
        verifyNoMoreInteractions(module);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonLocatable() throws Exception {
        ClassFileLocator.Resolution resolution = new ClassFileLocator.ForModule(module)
                .locate(FOOBAR);
        assertThat(resolution.isResolved(), is(false));
        verify(module).getResourceAsStream(FOOBAR + ClassFileLocator.CLASS_FILE_EXTENSION);
        verifyNoMoreInteractions(module);
        resolution.resolve();
        fail();
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testBootPath() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForModule.ofBootLayer();
        assertThat(classFileLocator.locate(Object.class.getName()).isResolved(), is(true));
        assertThat(classFileLocator.locate(getClass().getName()).isResolved(), is(false));
    }

    @Test
    public void testClose() throws Exception {
        new ClassFileLocator.ForModule(module).close();
        verifyNoMoreInteractions(module);
    }
}

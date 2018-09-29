package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ClassFileLocatorForModuleWeaklyReferencedTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private JavaModule module;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private Object unwrapped;

    @Before
    public void setUp() throws Exception {
        when(module.isNamed()).thenReturn(true);
        when(module.getClassLoader()).thenReturn(classLoader);
        when(module.unwrap()).thenReturn(unwrapped);
    }

    @Test
    public void testCreationNamed() throws Exception {
        when(module.isNamed()).thenReturn(true);
        assertThat(ClassFileLocator.ForModule.WeaklyReferenced.of(module), hasPrototype((ClassFileLocator) new ClassFileLocator.ForModule.WeaklyReferenced(unwrapped)));
    }

    @Test
    public void testCreationUnnamed() throws Exception {
        when(module.isNamed()).thenReturn(false);
        assertThat(ClassFileLocator.ForModule.WeaklyReferenced.of(module), hasPrototype((ClassFileLocator) new ClassFileLocator.ForClassLoader.WeaklyReferenced(classLoader)));
    }

    @Test
    public void testCreationNamedSystem() throws Exception {
        when(module.isNamed()).thenReturn(true);
        when(module.getClassLoader()).thenReturn(ClassLoader.getSystemClassLoader());
        assertThat(ClassFileLocator.ForModule.WeaklyReferenced.of(module), hasPrototype((ClassFileLocator) new ClassFileLocator.ForModule(module)));
    }

    @Test
    public void testCreationUnnamedSystem() throws Exception {
        when(module.isNamed()).thenReturn(false);
        when(module.getClassLoader()).thenReturn(ClassLoader.getSystemClassLoader());
        assertThat(ClassFileLocator.ForModule.of(module), hasPrototype(ClassFileLocator.ForClassLoader.ofSystemLoader()));
    }

    @Test
    public void testCreationNamedPlatform() throws Exception {
        when(module.isNamed()).thenReturn(true);
        when(module.getClassLoader()).thenReturn(ClassLoader.getSystemClassLoader().getParent());
        assertThat(ClassFileLocator.ForModule.WeaklyReferenced.of(module), hasPrototype((ClassFileLocator) new ClassFileLocator.ForModule(module)));
    }

    @Test
    public void testCreationUnnamedPlatform() throws Exception {
        when(module.isNamed()).thenReturn(false);
        when(module.getClassLoader()).thenReturn(ClassLoader.getSystemClassLoader().getParent());
        assertThat(ClassFileLocator.ForModule.of(module),
                hasPrototype((ClassFileLocator) new ClassFileLocator.ForClassLoader(ClassLoader.getSystemClassLoader().getParent())));
    }

    @Test
    public void testCreationNamedBoot() throws Exception {
        when(module.isNamed()).thenReturn(true);
        when(module.getClassLoader()).thenReturn(null);
        assertThat(ClassFileLocator.ForModule.WeaklyReferenced.of(module), hasPrototype((ClassFileLocator) new ClassFileLocator.ForModule(module)));
    }

    @Test
    public void testCreationUnnamedBoot() throws Exception {
        when(module.isNamed()).thenReturn(false);
        when(module.getClassLoader()).thenReturn(null);
        assertThat(ClassFileLocator.ForModule.of(module), hasPrototype(ClassFileLocator.ForClassLoader.ofBootLoader()));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testLocateModules() throws Exception {
        ClassFileLocator classFileLocator = new ClassFileLocator.ForModule.WeaklyReferenced(JavaModule.ofType(Object.class).unwrap());
        assertThat(classFileLocator.locate(Object.class.getName()).isResolved(), is(true));
        assertThat(classFileLocator.locate(getClass().getName()).isResolved(), is(false));
    }

    @Test
    public void testClose() throws Exception {
        new ClassFileLocator.ForModule.WeaklyReferenced(module).close();
        verifyZeroInteractions(module);
    }
}

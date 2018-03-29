package net.bytebuddy.dynamic;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassFileLocatorPackageDiscriminatingTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private ClassFileLocator classFileLocator;

    @Mock
    private ClassFileLocator foo, bar;

    @Mock
    private ClassFileLocator.Resolution fooResulution, barResolution;

    @Before
    public void setUp() throws Exception {
        when(foo.locate(FOO + "." + BAR)).thenReturn(fooResulution);
        when(bar.locate(BAR)).thenReturn(barResolution);
        Map<String, ClassFileLocator> map = new HashMap<String, ClassFileLocator>();
        map.put(FOO, foo);
        map.put(NamedElement.EMPTY_NAME, bar);
        classFileLocator = new ClassFileLocator.PackageDiscriminating(map);
    }

    @Test
    public void testValidLocation() throws Exception {
        assertThat(classFileLocator.locate(FOO + "." + BAR), is(fooResulution));
    }

    @Test
    public void testValidLocationDefaultPackage() throws Exception {
        assertThat(classFileLocator.locate(BAR), is(barResolution));
    }

    @Test
    public void testInvalidLocation() throws Exception {
        assertThat(classFileLocator.locate(BAR + "." + FOO).isResolved(), is(false));
    }

    @Test
    public void testClose() throws Exception {
        classFileLocator.close();
        verify(foo).close();
        verifyNoMoreInteractions(foo);
        verify(bar).close();
        verifyNoMoreInteractions(bar);
    }
}

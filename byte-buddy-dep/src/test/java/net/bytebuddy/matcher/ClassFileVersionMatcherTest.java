package net.bytebuddy.matcher;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassFileVersionMatcherTest extends AbstractElementMatcherTest<ClassFileVersionMatcher<?>> {

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassFileVersion left, right;

    @SuppressWarnings("unchecked")
    public ClassFileVersionMatcherTest() {
        super((Class<ClassFileVersionMatcher<?>>) (Object) ClassFileVersionMatcher.class, "hasClassFileVersion");
    }

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getClassFileVersion()).thenReturn(left);
    }

    @Test
    public void testNoClassFileVersion() {
        assertThat(new ClassFileVersionMatcher<TypeDescription>(mock(ClassFileVersion.class), false).matches(mock(TypeDescription.class)), is(false));
    }

    @Test
    public void testAtLeastMatcherMatches() {
        when(left.isAtLeast(right)).thenReturn(true);
        assertThat(new ClassFileVersionMatcher<TypeDescription>(right, false).matches(typeDescription), is(true));
    }

    @Test
    public void testAtLeastMatcherDoesNotMatch() {
        assertThat(new ClassFileVersionMatcher<TypeDescription>(right, false).matches(typeDescription), is(false));
    }

    @Test
    public void testAtMostMatcherMatches() {
        when(left.isAtMost(right)).thenReturn(true);
        assertThat(new ClassFileVersionMatcher<TypeDescription>(right, true).matches(typeDescription), is(true));
    }

    @Test
    public void testAtMostMatcherDoesNotMatch() {
        assertThat(new ClassFileVersionMatcher<TypeDescription>(right, true).matches(typeDescription), is(false));
    }
}

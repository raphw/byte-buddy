package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class InstanceTypeMatcherTest extends AbstractElementMatcherTest<InstanceTypeMatcher<?>> {

    @Mock
    private Object object;

    @Mock
    private ElementMatcher<? super TypeDescription> matcher;

    @SuppressWarnings("unchecked")
    public InstanceTypeMatcherTest() {
        super((Class<InstanceTypeMatcher<?>>) (Object) InstanceTypeMatcher.class, "ofType");
    }

    @Test
    public void testMatch() throws Exception {
        when(matcher.matches(new TypeDescription.ForLoadedType(object.getClass()))).thenReturn(true);
        assertThat(new InstanceTypeMatcher<Object>(matcher).matches(object), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        when(matcher.matches(new TypeDescription.ForLoadedType(object.getClass()))).thenReturn(false);
        assertThat(new InstanceTypeMatcher<Object>(matcher).matches(object), is(false));
    }

    @Test
    public void testNoMatchNull() throws Exception {
        assertThat(new InstanceTypeMatcher<Object>(matcher).matches(null), is(false));
    }
}

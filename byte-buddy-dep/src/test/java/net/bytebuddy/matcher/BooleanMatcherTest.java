package net.bytebuddy.matcher;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class BooleanMatcherTest extends AbstractElementMatcherTest<BooleanMatcher<?>> {

    @SuppressWarnings("unchecked")
    public BooleanMatcherTest() {
        super((Class<BooleanMatcher<?>>) (Object) BooleanMatcher.class, "");
    }

    @Test
    public void testMatch() throws Exception {
        Object target = mock(Object.class);
        assertThat(new BooleanMatcher<Object>(true).matches(target), is(true));
        verifyZeroInteractions(target);
    }

    @Test
    public void testNoMatch() throws Exception {
        Object target = mock(Object.class);
        assertThat(new BooleanMatcher<Object>(false).matches(target), is(false));
        verifyZeroInteractions(target);
    }

    @Test
    public void testToString() throws Exception {
        assertThat(new BooleanMatcher<Object>(true).toString(), is("true"));
        assertThat(new BooleanMatcher<Object>(false).toString(), is("false"));
    }

    @Test
    public void testSingletonEquivalentToNewInstance() {
        assertThat(BooleanMatcher.of(true), is((ElementMatcher.Junction<Object>) new BooleanMatcher<Object>(true)));
        assertThat(BooleanMatcher.of(false), is((ElementMatcher.Junction<Object>) new BooleanMatcher<Object>(false)));
    }
}

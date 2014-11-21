package net.bytebuddy.matcher;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EqualityMatcherTest extends AbstractElementMatcherTest<EqualityMatcher<?>> {

    @SuppressWarnings("unchecked")
    public EqualityMatcherTest() {
        super((Class<EqualityMatcher<?>>) (Object) EqualityMatcher.class, "is");
    }

    @Test
    public void testMatch() throws Exception {
        Object target = new Object();
        assertThat(new EqualityMatcher<Object>(target).matches(target), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new EqualityMatcher<Object>(new Object()).matches(new Object()), is(false));
    }
}

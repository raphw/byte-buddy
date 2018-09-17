package net.bytebuddy.matcher;

import net.bytebuddy.description.NamedElement;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class IsNamedMatcherTest extends AbstractElementMatcherTest<IsNamedMatcher<?>> {

    @Mock
    private NamedElement.WithOptionalName namedElement;

    @SuppressWarnings("unchecked")
    public IsNamedMatcherTest() {
        super((Class<IsNamedMatcher<?>>) (Object) IsNamedMatcher.class, "isNamed");
    }

    @Test
    public void testMatch() throws Exception {
        when(namedElement.isNamed()).thenReturn(true);
        assertThat(new IsNamedMatcher<NamedElement.WithOptionalName>().matches(namedElement), is(true));
    }

    @Test
    public void testPositiveToNegative() throws Exception {
        assertThat(new IsNamedMatcher<NamedElement.WithOptionalName>().matches(namedElement), is(false));
    }
}

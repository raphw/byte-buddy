package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class LatentMatcherAccessorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ElementMatcher<? super Object> matcher;

    @Mock
    private TypeDescription typeDescription;

    @Test
    @SuppressWarnings("unchecked")
    public void testManifestation() throws Exception {
        LatentMatcher<Object> matcher = new LatentMatcher.Resolved<Object>(this.matcher);
        assertThat(matcher.resolve(typeDescription), is((ElementMatcher) this.matcher));
        verifyZeroInteractions(this.matcher);
        verifyZeroInteractions(typeDescription);
    }
}

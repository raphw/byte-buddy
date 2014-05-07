package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodAttributeAppenderCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodAttributeAppender first, second;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new MethodAttributeAppender.Compound(first, second).hashCode(),
                is(new MethodAttributeAppender.Compound(first, second).hashCode()));
        assertThat(new MethodAttributeAppender.Compound(first, second),
                is(new MethodAttributeAppender.Compound(first, second)));
        assertThat(new MethodAttributeAppender.Compound(first, second).hashCode(),
                not(is(new MethodAttributeAppender.Compound(second, first).hashCode())));
        assertThat(new MethodAttributeAppender.Compound(first, second),
                not(is(new MethodAttributeAppender.Compound(second, first))));
    }
}

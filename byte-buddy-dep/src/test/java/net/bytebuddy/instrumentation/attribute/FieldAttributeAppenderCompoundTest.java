package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldAttributeAppenderCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldAttributeAppender first, second;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new FieldAttributeAppender.Compound(first, second).hashCode(),
                is(new FieldAttributeAppender.Compound(first, second).hashCode()));
        assertThat(new FieldAttributeAppender.Compound(first, second),
                is(new FieldAttributeAppender.Compound(first, second)));
        assertThat(new FieldAttributeAppender.Compound(first, second).hashCode(),
                not(is(new FieldAttributeAppender.Compound(second, first).hashCode())));
        assertThat(new FieldAttributeAppender.Compound(first, second),
                not(is(new FieldAttributeAppender.Compound(second, first))));
    }
}

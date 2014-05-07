package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldAttributeAppenderFactoryCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldAttributeAppender.Factory first, second;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new FieldAttributeAppender.Factory.Compound(first, second).hashCode(),
                is(new FieldAttributeAppender.Factory.Compound(first, second).hashCode()));
        assertThat(new FieldAttributeAppender.Factory.Compound(first, second),
                is(new FieldAttributeAppender.Factory.Compound(first, second)));
        assertThat(new FieldAttributeAppender.Factory.Compound(first, second).hashCode(),
                not(is(new FieldAttributeAppender.Factory.Compound(second, first).hashCode())));
        assertThat(new FieldAttributeAppender.Factory.Compound(first, second),
                not(is(new FieldAttributeAppender.Factory.Compound(second, first))));
    }
}

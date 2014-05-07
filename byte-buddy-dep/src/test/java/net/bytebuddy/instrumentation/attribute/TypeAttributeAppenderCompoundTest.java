package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeAttributeAppenderCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeAttributeAppender first, second;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new TypeAttributeAppender.Compound(first, second).hashCode(),
                is(new TypeAttributeAppender.Compound(first, second).hashCode()));
        assertThat(new TypeAttributeAppender.Compound(first, second),
                is(new TypeAttributeAppender.Compound(first, second)));
        assertThat(new TypeAttributeAppender.Compound(first, second).hashCode(),
                not(is(new TypeAttributeAppender.Compound(second, first).hashCode())));
        assertThat(new TypeAttributeAppender.Compound(first, second),
                not(is(new TypeAttributeAppender.Compound(second, first))));
    }
}

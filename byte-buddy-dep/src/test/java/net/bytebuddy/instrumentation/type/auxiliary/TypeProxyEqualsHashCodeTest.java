package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class TypeProxyEqualsHashCodeTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription first, second;

    @Test
    public void testEqualsHashCode() throws Exception {
        assertThat(new TypeProxy(first, second, true).hashCode(), is(new TypeProxy(first, second, true).hashCode()));
        assertThat(new TypeProxy(first, second, true), is(new TypeProxy(first, second, true)));
        assertThat(new TypeProxy(first, second, true).hashCode(), not(is(new TypeProxy(first, second, false).hashCode())));
        assertThat(new TypeProxy(first, second, true), not(is(new TypeProxy(first, second, false))));
        assertThat(new TypeProxy(first, second, true).hashCode(), not(is(new TypeProxy(second, first, true).hashCode())));
        assertThat(new TypeProxy(first, second, true), not(is(new TypeProxy(second, first, true))));
    }
}

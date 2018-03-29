package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RebaseDynamicTypeBuilderRebaseableMatcherTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.Token targetToken, otherToken;

    @Test
    public void testMatchToken() throws Exception {
        assertThat(new RebaseDynamicTypeBuilder.RebaseableMatcher(Collections.singleton(targetToken)).matches(targetToken), is(true));
    }

    @Test
    public void testNoMatchToken() throws Exception {
        assertThat(new RebaseDynamicTypeBuilder.RebaseableMatcher(Collections.singleton(otherToken)).matches(targetToken), is(false));
    }
}

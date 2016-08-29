package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class RebaseDynamicTypeBuilderRebaseableMatcherTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private MethodDescription target;

    @Mock
    private MethodDescription.Token targetToken, otherToken;

    @Before
    public void setUp() throws Exception {
        when(target.asToken(ElementMatchers.is(instrumentedType))).thenReturn(targetToken);
    }

    @Test
    public void testMatchToken() throws Exception {
        assertThat(new RebaseDynamicTypeBuilder.RebaseableMatcher(instrumentedType, Collections.singleton(targetToken)).matches(target), is(true));
    }

    @Test
    public void testNoMatchToken() throws Exception {
        assertThat(new RebaseDynamicTypeBuilder.RebaseableMatcher(instrumentedType, Collections.singleton(otherToken)).matches(target), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(RebaseDynamicTypeBuilder.RebaseableMatcher.class).apply();
    }
}

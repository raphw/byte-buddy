package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransformerCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Transformer<Object> first, second;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private Object firstTarget, secondTarget, finalTarget;

    @Before
    public void setUp() throws Exception {
        when(first.transform(typeDescription, firstTarget)).thenReturn(secondTarget);
        when(second.transform(typeDescription, secondTarget)).thenReturn(finalTarget);
    }

    @Test
    @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
    public void testTransformation() throws Exception {
        assertThat(new Transformer.Compound<Object>(first, second).transform(typeDescription, firstTarget), is(finalTarget));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Transformer.Compound.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(mock(Transformer.class));
            }
        }).apply();
    }
}

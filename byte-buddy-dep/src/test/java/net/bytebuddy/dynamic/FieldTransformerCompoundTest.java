package net.bytebuddy.dynamic;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class FieldTransformerCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldTransformer first, second;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private FieldDescription firstMethod, secondMethod, finalMethod;

    @Before
    public void setUp() throws Exception {
        when(first.transform(typeDescription, firstMethod)).thenReturn(secondMethod);
        when(second.transform(typeDescription, secondMethod)).thenReturn(finalMethod);
    }

    @Test
    public void testTransformation() throws Exception {
        assertThat(new FieldTransformer.Compound(first, second).transform(typeDescription, firstMethod), is(finalMethod));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldTransformer.Compound.class).apply();
    }
}

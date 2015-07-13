package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FieldRegistryLatentFieldMatcherForTokenTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldDescription.Token fieldToken;

    @Test
    public void testFieldName() throws Exception {
        when(fieldToken.getName()).thenReturn(FOO);
        assertThat(new FieldRegistry.LatentFieldMatcher.ForToken(fieldToken).getFieldName(), is(FOO));
        verify(fieldToken).getName();
        verifyNoMoreInteractions(fieldToken);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldRegistry.LatentFieldMatcher.ForToken.class).apply();
    }
}
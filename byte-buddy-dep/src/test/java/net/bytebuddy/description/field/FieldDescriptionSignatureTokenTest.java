package net.bytebuddy.description.field;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldDescriptionSignatureTokenTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription type;

    @Test
    public void testProperties() throws Exception {
        FieldDescription.SignatureToken token = new FieldDescription.SignatureToken(FOO, type);
        assertThat(token.getName(), is(FOO));
        assertThat(token.getType(), is(type));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldDescription.SignatureToken.class).apply();
    }
}

package net.bytebuddy.description.field;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldDescriptionSignatureTokenTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription type;

    @Test
    public void testProperties() throws Exception {
        FieldDescription.SignatureToken token = new FieldDescription.SignatureToken(FOO, type);
        assertThat(token.getName(), is(FOO));
        assertThat(token.getType(), is(type));
    }
}

package net.bytebuddy.instrumentation.field;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldDescriptionLatentTest {

    private static final String FOO = "foo";
    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription declaringType, fieldType;

    @Test
    public void testFieldDefinition() throws Exception {
        FieldDescription fieldDescription = new FieldDescription.Latent(FOO, declaringType, fieldType, MODIFIERS);
        assertThat(fieldDescription.getName(), is(FOO));
        assertThat(fieldDescription.getInternalName(), is(FOO));
        assertThat(fieldDescription.getModifiers(), is(MODIFIERS));
        assertThat(fieldDescription.getFieldType(), is(fieldType));
        assertThat(fieldDescription.getDeclaringType(), is(declaringType));
    }
}

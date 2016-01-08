package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionGenericVisitorValidatorTest {

    // TODO

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic typeDescription;

    @Test
    public void testWildcardNotValidated() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.TYPE_VARIABLE.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.EXCEPTION.onWildcard(typeDescription), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Validator.class).apply();
    }
}

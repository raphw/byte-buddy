package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class TypeVariableTokenTest {

    // TODO: Test visitors and getters, all tokens.

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeVariableToken.class).apply();
    }
}

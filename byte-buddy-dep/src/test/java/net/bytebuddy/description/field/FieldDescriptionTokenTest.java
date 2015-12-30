package net.bytebuddy.description.field;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class FieldDescriptionTokenTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldDescription.Token.class).apply();
    }
}

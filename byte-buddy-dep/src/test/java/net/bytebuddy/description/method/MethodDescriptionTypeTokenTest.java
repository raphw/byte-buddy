package net.bytebuddy.description.method;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodDescriptionTypeTokenTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDescription.TypeToken.class).apply();
    }
}

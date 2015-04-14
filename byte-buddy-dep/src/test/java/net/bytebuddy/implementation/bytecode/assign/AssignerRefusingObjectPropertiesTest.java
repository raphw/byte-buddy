package net.bytebuddy.implementation.bytecode.assign;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class AssignerRefusingObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Assigner.Refusing.class).apply();
    }
}

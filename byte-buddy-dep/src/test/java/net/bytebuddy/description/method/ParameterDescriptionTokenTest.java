package net.bytebuddy.description.method;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Random;

public class ParameterDescriptionTokenTest {

    // TODO

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ParameterDescription.Token.class).create(new ObjectPropertyAssertion.Creator<Integer>() {
            @Override
            public Integer create() {
                return new Random().nextInt();
            }
        }).apply();
    }
}

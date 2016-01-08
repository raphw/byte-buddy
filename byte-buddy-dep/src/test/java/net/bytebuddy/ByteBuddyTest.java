package net.bytebuddy;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ByteBuddyTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddy.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.EnumerationImplementation.class).apply();
    }
}

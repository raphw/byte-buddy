package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class TypeWriterDefaultTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.Default.ForCreation.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.Default.ForInlining.class).apply();
    }
}

package net.bytebuddy.asm;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ClassVisitorWrapperChainObjectPropertyTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassVisitorWrapper.Chain.class).apply();
    }
}

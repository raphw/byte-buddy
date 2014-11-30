package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class InlineDynamicTypeBuilderTargetHandlerForRebaseInstrumentationTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InlineDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation.class).apply();
    }
}

package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class InlineDynamicTypeBuilderTargetHandlerForRebaseInstrumentationTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InlineDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation.class).apply();
    }
}

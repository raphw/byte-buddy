package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.Java8Rule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class DefaultMethodCallTest extends AbstractInstrumentationTest {

    @Rule
    public MethodRule java8Rule = new Java8Rule();

    @Test
    @Java8Rule.Enforce
    @Ignore
    public void testName() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(null, DefaultMethodCall.unambiguousOnly());
    }
}

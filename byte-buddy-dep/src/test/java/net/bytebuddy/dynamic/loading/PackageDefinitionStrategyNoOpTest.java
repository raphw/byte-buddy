package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PackageDefinitionStrategyNoOpTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testPackageNotDefined() throws Exception {
        assertThat(PackageDefinitionStrategy.NoOp.INSTANCE.define(getClass().getClassLoader(), FOO, BAR).isDefined(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PackageDefinitionStrategy.NoOp.class).apply();
    }
}

package net.bytebuddy.dynamic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class VisibilityBridgeStrategyDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testAlwaysStrategy() {
        assertThat(VisibilityBridgeStrategy.Default.ALWAYS.generateVisibilityBridge(methodDescription), is(true));
    }

    @Test
    public void testNonGenericStrategyOnNonGeneric() {
        when(methodDescription.isGenerified()).thenReturn(false);
        assertThat(VisibilityBridgeStrategy.Default.ON_NON_GENERIC_METHOD.generateVisibilityBridge(methodDescription), is(true));
    }

    @Test
    public void testNonGenericStrategyOnGeneric() {
        when(methodDescription.isGenerified()).thenReturn(true);
        assertThat(VisibilityBridgeStrategy.Default.ON_NON_GENERIC_METHOD.generateVisibilityBridge(methodDescription), is(false));
    }

    @Test
    public void testNeverStrategy() {
        assertThat(VisibilityBridgeStrategy.Default.NEVER.generateVisibilityBridge(methodDescription), is(false));
    }
}
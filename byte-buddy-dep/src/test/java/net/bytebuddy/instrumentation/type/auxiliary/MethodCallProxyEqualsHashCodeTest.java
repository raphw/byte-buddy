package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodCallProxyEqualsHashCodeTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription first, second;

    @Test
    public void testEqualsHashCode() throws Exception {
        assertThat(new MethodCallProxy(first).hashCode(), is(new MethodCallProxy(first).hashCode()));
        assertThat(new MethodCallProxy(first), is(new MethodCallProxy(first)));
        assertThat(new MethodCallProxy(first).hashCode(), not(is(new MethodCallProxy(second).hashCode())));
        assertThat(new MethodCallProxy(first), not(is(new MethodCallProxy(second))));
    }

    @Test
    public void testAssignableSignatureCallEqualsHashCode() throws Exception {
        assertThat(new MethodCallProxy.AssignableSignatureCall(first).hashCode(), is(new MethodCallProxy.AssignableSignatureCall(first).hashCode()));
        assertThat(new MethodCallProxy.AssignableSignatureCall(first), is(new MethodCallProxy.AssignableSignatureCall(first)));
        assertThat(new MethodCallProxy.AssignableSignatureCall(first).hashCode(), not(is(new MethodCallProxy.AssignableSignatureCall(second).hashCode())));
        assertThat(new MethodCallProxy.AssignableSignatureCall(first), not(is(new MethodCallProxy.AssignableSignatureCall(second))));
    }
}

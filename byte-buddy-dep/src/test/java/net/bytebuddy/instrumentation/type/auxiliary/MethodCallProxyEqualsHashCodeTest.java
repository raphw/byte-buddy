package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.Instrumentation;
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
    private Instrumentation.SpecialMethodInvocation first, second;

    @Test
    public void testEqualsHashCode() throws Exception {
        assertThat(new MethodCallProxy(first, false).hashCode(), is(new MethodCallProxy(first, false).hashCode()));
        assertThat(new MethodCallProxy(first, false), is(new MethodCallProxy(first, false)));
        assertThat(new MethodCallProxy(first, false).hashCode(), not(is(new MethodCallProxy(second, false).hashCode())));
        assertThat(new MethodCallProxy(first, false), not(is(new MethodCallProxy(second, false))));
    }

    @Test
    public void testAssignableSignatureCallEqualsHashCode() throws Exception {
        assertThat(new MethodCallProxy.AssignableSignatureCall(first, false).hashCode(),
                is(new MethodCallProxy.AssignableSignatureCall(first, false).hashCode()));
        assertThat(new MethodCallProxy.AssignableSignatureCall(first, false),
                is(new MethodCallProxy.AssignableSignatureCall(first, false)));
        assertThat(new MethodCallProxy.AssignableSignatureCall(first, false).hashCode(),
                not(is(new MethodCallProxy.AssignableSignatureCall(second, false).hashCode())));
        assertThat(new MethodCallProxy.AssignableSignatureCall(first, false),
                not(is(new MethodCallProxy.AssignableSignatureCall(second, false))));
    }
}

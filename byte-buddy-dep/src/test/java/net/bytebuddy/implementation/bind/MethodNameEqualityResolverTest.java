package net.bytebuddy.implementation.bind;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodNameEqualityResolverTest extends AbstractAmbiguityResolverTest {

    private static final String FOO = "foo";

    private static final String BAR = "bar";

    @Test
    public void testBothEqual() throws Exception {
        test(FOO, FOO, FOO, MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
    }

    @Test
    public void testLeftEqual() throws Exception {
        test(FOO, BAR, FOO, MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
    }

    @Test
    public void testRightEqual() throws Exception {
        test(BAR, FOO, FOO, MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
    }

    @Test
    public void testNoneEqual() throws Exception {
        test(BAR, BAR, FOO, MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
    }

    private void test(String leftName,
                      String rightName,
                      String resultName,
                      MethodDelegationBinder.AmbiguityResolver.Resolution expected) throws Exception {
        when(leftMethod.getName()).thenReturn(leftName);
        when(rightMethod.getName()).thenReturn(rightName);
        when(source.getName()).thenReturn(resultName);
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MethodNameEqualityResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(expected));
        verify(left, atLeast(1)).getTarget();
        verify(leftMethod, atLeast(1)).getName();
        verify(right, atLeast(1)).getTarget();
        verify(rightMethod, atLeast(1)).getName();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodNameEqualityResolver.class).apply();
    }
}
